package artframework.context;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.HostBindingComponent;
import artframework.presentation.NodePropertiesComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.VisibilityComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ART-owned hard-sync projection of card entities from context frames.
 */
public final class PresentProjection {

    private final PresentationKey metadataKey = new PresentationKey("sts1.projection", "context");

    private static final ProjectionFrameComponent EMPTY_METADATA =
            new ProjectionFrameComponent(-1L, -1L, "", false, false);
    private static final ProjectionInteractionComponent EMPTY_INTERACTION =
            new ProjectionInteractionComponent(null);
    private static final ProjectionFrameSnapshotComponent EMPTY_SNAPSHOT =
            new ProjectionFrameSnapshotComponent(ContextFrame.unavailable(0L));

    public FrameDiff applyFrame(ContextFrame frame) {
        return applyFrame(frame, true);
    }

    FrameDiff applyFrame(ContextFrame frame, boolean runConfirmation) {
        if (frame == null) {
            return FrameDiff.skipped("frame required");
        }
        ProjectionFrameComponent metadata = metadataForWrite();
        ContextFrame previousFrame = snapshotForWrite().frame;
        if (!frame.available) {
            recordBusinessConfirmationFrame(previousFrame, frame);
            observeNativeIntents(frame);
            putMetadata(metadata.frameId, metadata.sceneEpoch, metadata.scene, false,
                    metadata.frameId >= 0);
            putSnapshot(frame);
            return FrameDiff.skipped("frame unavailable");
        }
        if (metadata.sceneEpoch >= 0L && frame.sceneEpoch != metadata.sceneEpoch) {
            // Epoch is Backend authority. A new scene must never inherit card/drag state.
            clearCards();
            clearDrag();
            metadata = new ProjectionFrameComponent(-1L, metadata.sceneEpoch, metadata.scene,
                    metadata.available, metadata.stale);
        }
        if (frame.frameId < metadata.frameId) {
            return FrameDiff.skipped("stale frameId " + frame.frameId + " < " + metadata.frameId);
        }
        if (frame.frameId == metadata.frameId && metadata.frameId >= 0) {
            // same-frame reapply: last wins (replace projection from frame)
        }

        List<String> added = new ArrayList<String>();
        List<String> updated = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();

        for (CardView view : frame.cards) {
            String id = view.ref.instanceId;
            seen.add(id);
            EntityId presentationEntity = cardEntity(id);
            CardEntity entity = presentationEntity == null ? null : cardView(presentationEntity);
            if (entity == null) {
                entity = new CardEntity(id);
                PresentationKey key = new PresentationKey("sts1.card", id);
                presentationEntity = contextForWrite().entity(key);
                if (presentationEntity == null) {
                    presentationEntity = contextForWrite().create(key, id, "card", "c2-projection");
                }
                int changes = CardEntity.IDENTITY_CHANGED
                        | CardEntity.PLACEMENT_CHANGED
                        | CardEntity.INTERACTION_CHANGED
                        | CardEntity.ASSETS_CHANGED;
                entity.apply(view);
                applyComponents(presentationEntity, view, changes);
                added.add(id);
            } else {
                int changes = entity.apply(view);
                if (changes != 0) {
                    applyComponents(cardEntity(id), view, changes);
                    updated.add(id);
                }
            }
        }

        List<String> removed = new ArrayList<String>();
        List<String> toRemove = new ArrayList<String>();
        for (EntityId entity : contextForWrite().entities()) {
            CardIdentityComponent identity = worldForWrite().get(entity, CardIdentityComponent.class);
            if (identity != null && !seen.contains(identity.instanceId)) {
                toRemove.add(identity.instanceId);
            }
        }
        for (String id : toRemove) {
            contextForWrite().destroy(cardEntity(id));
            removed.add(id);
            if (id.equals(dragInstanceId())) {
                clearDrag();
            }
        }

        putMetadata(frame.frameId, frame.sceneEpoch, frame.scene, true, false);
        putSnapshot(frame);
        recordBusinessConfirmationFrame(previousFrame, frame);
        observeNativeIntents(frame);
        return new FrameDiff(added, removed, updated, true, "");
    }

    private void recordBusinessConfirmationFrame(ContextFrame before, ContextFrame after) {
        EntityId metadata = ensureMetadataEntity();
        worldForWrite().put(metadata, BusinessConfirmationFrameComponent.class,
                new BusinessConfirmationFrameComponent(before, after));
    }

    public long lastFrameId() {
        return metadata().frameId;
    }

    public String scene() {
        return metadata().scene;
    }

    public long sceneEpoch() {
        return metadata().sceneEpoch;
    }

    public boolean isAvailable() {
        return metadata().available;
    }

    public boolean isStale() {
        return metadata().stale;
    }

    public ContextFrame lastFrame() {
        return snapshot().frame;
    }

    public CardEntity get(String instanceId) {
        EntityId entity = cardEntity(instanceId);
        return entity == null ? null : cardView(entity);
    }

    public List<CardEntity> list() {
        List<CardEntity> result = new ArrayList<CardEntity>();
        PresentationContext context = existingContext();
        if (context == null) return Collections.emptyList();
        for (EntityId entity : context.entities()) {
            if (context.world().get(entity, CardIdentityComponent.class) != null) result.add(cardView(entity));
        }
        return Collections.unmodifiableList(result);
    }

    public List<CardEntity> listZone(CardZone zone) {
        List<CardEntity> out = new ArrayList<CardEntity>();
        for (CardEntity e : list()) {
            if (e.zone == zone) {
                out.add(e);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public int size() {
        return list().size();
    }

    public String dragInstanceId() {
        ProjectionInteractionComponent interaction = interaction();
        return interaction.dragInstanceId;
    }

    public void setDragInstanceId(String instanceId) {
        putInteraction(instanceId);
    }

    public void clearDrag() {
        putInteraction(null);
    }

    public void reset() {
        EntityId metadata = ensureMetadataEntity();
        for (EntityId entity : new ArrayList<EntityId>(contextForWrite().entities())) {
            if (!metadata.equals(entity)) contextForWrite().destroy(entity);
        }
        clearDrag();
        putSnapshot(ContextFrame.unavailable(0L));
        putMetadata(-1L, -1L, "", false, false);
    }

    private EntityId ensureMetadataEntity() {
        PresentationContext context = contextForWrite();
        EntityId entity = context.entity(metadataKey);
        return entity != null ? entity : context.create(metadataKey, "context", "projection", "c2");
    }

    private ProjectionFrameComponent metadata() {
        PresentationContext context = existingContext();
        if (context == null) return EMPTY_METADATA;
        EntityId entity = context.entity(metadataKey);
        ProjectionFrameComponent component = entity != null
                ? context.world().get(entity, ProjectionFrameComponent.class) : null;
        return component != null ? component : EMPTY_METADATA;
    }

    private ProjectionFrameComponent metadataForWrite() {
        EntityId entity = ensureMetadataEntity();
        ProjectionFrameComponent component = worldForWrite().get(entity, ProjectionFrameComponent.class);
        if (component == null) {
            component = EMPTY_METADATA;
            worldForWrite().put(entity, ProjectionFrameComponent.class, component);
        }
        return component;
    }

    private void putMetadata(long frameId, long sceneEpoch, String scene,
            boolean available, boolean stale) {
        worldForWrite().put(ensureMetadataEntity(), ProjectionFrameComponent.class,
                new ProjectionFrameComponent(frameId, sceneEpoch, scene, available, stale));
    }

    private ProjectionInteractionComponent interaction() {
        PresentationContext context = existingContext();
        if (context == null) return EMPTY_INTERACTION;
        EntityId entity = context.entity(metadataKey);
        ProjectionInteractionComponent component = entity != null
                ? context.world().get(entity, ProjectionInteractionComponent.class) : null;
        return component != null ? component : EMPTY_INTERACTION;
    }

    private void putInteraction(String dragInstanceId) {
        worldForWrite().put(ensureMetadataEntity(), ProjectionInteractionComponent.class,
                new ProjectionInteractionComponent(dragInstanceId));
    }

    private ProjectionFrameSnapshotComponent snapshot() {
        PresentationContext context = existingContext();
        if (context == null) return EMPTY_SNAPSHOT;
        EntityId entity = context.entity(metadataKey);
        ProjectionFrameSnapshotComponent component = entity != null
                ? context.world().get(entity, ProjectionFrameSnapshotComponent.class) : null;
        return component != null ? component : EMPTY_SNAPSHOT;
    }

    private ProjectionFrameSnapshotComponent snapshotForWrite() {
        EntityId entity = ensureMetadataEntity();
        ProjectionFrameSnapshotComponent component =
                worldForWrite().get(entity, ProjectionFrameSnapshotComponent.class);
        if (component == null) {
            component = EMPTY_SNAPSHOT;
            worldForWrite().put(entity, ProjectionFrameSnapshotComponent.class, component);
        }
        return component;
    }

    private void putSnapshot(ContextFrame frame) {
        worldForWrite().put(ensureMetadataEntity(), ProjectionFrameSnapshotComponent.class,
                new ProjectionFrameSnapshotComponent(frame));
    }

    private void clearCards() {
        for (EntityId entity : new ArrayList<EntityId>(contextForWrite().entities())) {
            if (worldForWrite().get(entity, CardIdentityComponent.class) != null) contextForWrite().destroy(entity);
        }
    }

    private void observeNativeIntents(ContextFrame frame) {
        for (EntityId entity : artframework.presentation.PresentationRegistry.context("sts1-input").entities()) {
            NativeIntentLifecycleComponent lifecycle = worldForWrite().get(entity, NativeIntentLifecycleComponent.class);
            if (lifecycle != null) {
                NativeInputComponent input = worldForWrite().get(entity, NativeInputComponent.class);
                if (input != null) {
                    if (frame.available) {
                        artframework.sts1.input.NativeInputRecords.observe(input.surfaceId,
                                frame.frameId, frame.sceneEpoch, frame.scene);
                    } else {
                        artframework.sts1.input.NativeInputRecords.failed(
                                input.surfaceId, frame.frameId, "authority frame unavailable");
                    }
                }
            }
        }
    }

    /** Internal presentation ECS world; consumers should continue using this projection facade. */
    public PresentationWorld world() {
        PresentationContext context = existingContext();
        return context != null ? context.world() : PresentationRegistry.world();
    }

    /** Returns the ECS identity for a projected card, or null when absent. */
    public EntityId entityId(String instanceId) {
        return cardEntity(instanceId);
    }

    private EntityId cardEntity(String instanceId) {
        if (instanceId == null) return null;
        PresentationContext context = existingContext();
        return context != null ? context.entity(new PresentationKey("sts1.card", instanceId)) : null;
    }

    private CardEntity cardView(EntityId entity) {
        PresentationWorld world = world();
        CardIdentityComponent identity = world.get(entity, CardIdentityComponent.class);
        CardPlacementComponent placement = world.get(entity, CardPlacementComponent.class);
        CardInteractionComponent interaction = world.get(entity, CardInteractionComponent.class);
        CardAssetsComponent assets = world.get(entity, CardAssetsComponent.class);
        CardEntity view = new CardEntity(identity.instanceId);
        view.cardId = identity.cardId;
        view.zone = placement != null ? placement.zone : CardZone.OTHER;
        view.slotIndex = placement != null ? placement.slotIndex : 0;
        view.pose = placement != null ? placement.pose : CardPose.at(0f, 0f);
        view.playable = interaction != null && interaction.playable;
        view.selected = interaction != null && interaction.selected;
        view.hovered = interaction != null && interaction.hovered;
        view.dragging = interaction != null && interaction.dragging;
        view.artResourceId = assets != null ? assets.artResourceId : "";
        view.frameResourceId = assets != null ? assets.frameResourceId : "";
        return view;
    }

    private void applyComponents(EntityId entity, CardView view, int changes) {
        if ((changes & CardEntity.IDENTITY_CHANGED) != 0) {
            worldForWrite().put(entity, CardIdentityComponent.class,
                    new CardIdentityComponent(view.ref.instanceId, view.ref.cardId));
        }
        if ((changes & CardEntity.PLACEMENT_CHANGED) != 0) {
            worldForWrite().put(entity, CardPlacementComponent.class,
                    new CardPlacementComponent(view.zone, view.slotIndex, view.pose));
        }
        if ((changes & CardEntity.INTERACTION_CHANGED) != 0) {
            worldForWrite().put(entity, CardInteractionComponent.class,
                    new CardInteractionComponent(view.playable, view.selected, view.hovered, view.dragging));
        }
        if ((changes & CardEntity.ASSETS_CHANGED) != 0) {
            worldForWrite().put(entity, CardAssetsComponent.class,
                    new CardAssetsComponent(view.artResourceId, view.frameResourceId));
        }
        worldForWrite().put(entity, HostBindingComponent.class,
                new HostBindingComponent("STS1_C2", view.ref.instanceId));
        worldForWrite().put(entity, DrawComponent.class,
                new DrawComponent("card", view.artResourceId, view.ref.cardId));
        worldForWrite().put(entity, VisibilityComponent.class, new VisibilityComponent(true, 1f));
        worldForWrite().put(entity, NodePropertiesComponent.class,
                new NodePropertiesComponent(java.util.Collections.<String, Object>singletonMap(
                        "instanceId", view.ref.instanceId)));
        if (view.pose != null) {
            worldForWrite().put(entity, BoundsComponent.class,
                    new BoundsComponent(new artframework.component.Rect(
                            view.pose.x, view.pose.y, 0f, 0f), view.pose.z));
        }
    }

    public ControlsView controls() {
        return snapshot().frame.controlsView;
    }

    public MapView map() {
        return snapshot().frame.mapView;
    }

    public EventView event() {
        return snapshot().frame.eventView;
    }

    public SelectView select() {
        return snapshot().frame.selectView;
    }

    public RewardView reward() {
        return snapshot().frame.rewardView;
    }

    public RestView rest() {
        return snapshot().frame.restView;
    }

    public TreasureView treasure() {
        return snapshot().frame.treasureView;
    }

    public ShopView shop() {
        return snapshot().frame.shopView;
    }

    public TopPanelView topPanel() {
        return snapshot().frame.topPanelView;
    }

    public MonsterIntentView intents() {
        return snapshot().frame.intentsView;
    }

    public ViewportView viewport() {
        return snapshot().frame.viewport;
    }

    public Map<String, Object> probeSlice() {
        ProjectionFrameComponent metadata = metadata();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("frameId", Long.valueOf(metadata.frameId));
        m.put("sceneEpoch", Long.valueOf(metadata.sceneEpoch));
        m.put("scene", metadata.scene);
        m.put("available", Boolean.valueOf(metadata.available));
        m.put("stale", Boolean.valueOf(metadata.stale));
        m.put("cardCount", Integer.valueOf(size()));
        m.put("handCount", Integer.valueOf(listZone(CardZone.HAND).size()));
        String dragInstanceId = interaction().dragInstanceId;
        m.put("dragInstanceId", dragInstanceId != null ? dragInstanceId : "");
        ContextFrame frame = snapshot().frame;
        m.put("endTurnEnabled", Boolean.valueOf(frame.controlsView.endTurnEnabled));
        m.put("mapNodeCount", Integer.valueOf(frame.mapView.nodeCount()));
        m.put("eventOptionCount", Integer.valueOf(frame.eventView.optionCount()));
        m.put("selectPoolCount", Integer.valueOf(frame.selectView.poolCount()));
        m.put("selectKind", frame.selectView.kind);
        m.put("rewardItemCount", Integer.valueOf(frame.rewardView.itemCount()));
        m.put("rewardKind", frame.rewardView.kind);
        m.put("restOptionCount", Integer.valueOf(frame.restView.optionCount()));
        m.put("treasureAvailable", Boolean.valueOf(frame.treasureView.available));
        m.put("shopEntryCount", Integer.valueOf(frame.shopView.entryCount()));
        m.put("topPanelAvailable", Boolean.valueOf(frame.topPanelView.available));
        m.put("intentCount", Integer.valueOf(frame.intentsView.entryCount()));
        m.put("viewportWidth", Integer.valueOf(frame.viewport.logicalWidth));
        m.put("viewportHeight", Integer.valueOf(frame.viewport.logicalHeight));
        return m;
    }

    private PresentationContext contextForWrite() {
        return PresentationRegistry.context("c2-projection");
    }

    private PresentationContext existingContext() {
        return PresentationRegistry.existingContext("c2-projection");
    }

    private PresentationWorld worldForWrite() {
        return contextForWrite().world();
    }
}
