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

    private final Map<String, CardEntity> byInstance = new LinkedHashMap<String, CardEntity>();
    private final Map<String, EntityId> entityByInstance = new LinkedHashMap<String, EntityId>();
    private final PresentationContext context = PresentationRegistry.context("c2-projection");
    private final PresentationWorld world = context.world();
    private long lastFrameId = -1L;
    private long sceneEpoch = -1L;
    private String scene = "";
    private boolean available;
    private boolean stale;
    private String dragInstanceId;
    private ContextFrame lastFrame = ContextFrame.unavailable(0L);

    public FrameDiff applyFrame(ContextFrame frame) {
        if (frame == null) {
            return FrameDiff.skipped("frame required");
        }
        if (!frame.available) {
            available = false;
            stale = lastFrameId >= 0;
            lastFrame = frame;
            return FrameDiff.skipped("frame unavailable");
        }
        if (sceneEpoch >= 0L && frame.sceneEpoch != sceneEpoch) {
            // Epoch is Backend authority. A new scene must never inherit card/drag state.
            byInstance.clear();
            dragInstanceId = null;
            lastFrameId = -1L;
        }
        if (frame.frameId < lastFrameId) {
            return FrameDiff.skipped("stale frameId " + frame.frameId + " < " + lastFrameId);
        }
        if (frame.frameId == lastFrameId && lastFrameId >= 0) {
            // same-frame reapply: last wins (replace projection from frame)
        }

        List<String> added = new ArrayList<String>();
        List<String> updated = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();

        for (CardView view : frame.cards) {
            String id = view.ref.instanceId;
            seen.add(id);
            CardEntity entity = byInstance.get(id);
            if (entity == null) {
                entity = new CardEntity(id);
                byInstance.put(id, entity);
                PresentationKey key = new PresentationKey("sts1.card", id);
                EntityId presentationEntity = context.entity(key);
                if (presentationEntity != null && !world.contains(presentationEntity)) {
                    presentationEntity = null;
                }
                if (presentationEntity == null) {
                    presentationEntity = context.create(key, id, "card", "c2-projection");
                }
                entityByInstance.put(id, presentationEntity);
                int changes = CardEntity.IDENTITY_CHANGED
                        | CardEntity.PLACEMENT_CHANGED
                        | CardEntity.INTERACTION_CHANGED
                        | CardEntity.ASSETS_CHANGED;
                entity.apply(view);
                applyComponents(entityByInstance.get(id), view, changes);
                added.add(id);
            } else {
                int changes = entity.apply(view);
                if (changes != 0) {
                    applyComponents(entityByInstance.get(id), view, changes);
                    updated.add(id);
                }
            }
        }

        List<String> removed = new ArrayList<String>();
        List<String> toRemove = new ArrayList<String>();
        for (String id : byInstance.keySet()) {
            if (!seen.contains(id)) {
                toRemove.add(id);
            }
        }
        for (String id : toRemove) {
            byInstance.remove(id);
            world.destroyEntity(entityByInstance.remove(id));
            removed.add(id);
            if (id.equals(dragInstanceId)) {
                dragInstanceId = null;
            }
        }

        lastFrameId = frame.frameId;
        sceneEpoch = frame.sceneEpoch;
        scene = frame.scene;
        available = true;
        stale = false;
        lastFrame = frame;
        return new FrameDiff(added, removed, updated, true, "");
    }

    public long lastFrameId() {
        return lastFrameId;
    }

    public String scene() {
        return scene;
    }

    public long sceneEpoch() {
        return sceneEpoch;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isStale() {
        return stale;
    }

    public ContextFrame lastFrame() {
        return lastFrame;
    }

    public CardEntity get(String instanceId) {
        return byInstance.get(instanceId);
    }

    public List<CardEntity> list() {
        return Collections.unmodifiableList(new ArrayList<CardEntity>(byInstance.values()));
    }

    public List<CardEntity> listZone(CardZone zone) {
        List<CardEntity> out = new ArrayList<CardEntity>();
        for (CardEntity e : byInstance.values()) {
            if (e.zone == zone) {
                out.add(e);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public int size() {
        return byInstance.size();
    }

    public String dragInstanceId() {
        return dragInstanceId;
    }

    public void setDragInstanceId(String instanceId) {
        this.dragInstanceId = instanceId;
    }

    public void clearDrag() {
        this.dragInstanceId = null;
    }

    public void reset() {
        for (EntityId entity : new ArrayList<EntityId>(entityByInstance.values())) context.destroy(entity);
        byInstance.clear();
        entityByInstance.clear();
        lastFrameId = -1L;
        scene = "";
        sceneEpoch = -1L;
        available = false;
        stale = false;
        dragInstanceId = null;
        lastFrame = ContextFrame.unavailable(0L);
    }

    /** Internal presentation ECS world; consumers should continue using this projection facade. */
    public PresentationWorld world() {
        return world;
    }

    /** Returns the ECS identity for a projected card, or null when absent. */
    public EntityId entityId(String instanceId) {
        return entityByInstance.get(instanceId);
    }

    private void applyComponents(EntityId entity, CardView view, int changes) {
        if ((changes & CardEntity.IDENTITY_CHANGED) != 0) {
            world.put(entity, CardIdentityComponent.class,
                    new CardIdentityComponent(view.ref.instanceId, view.ref.cardId));
        }
        if ((changes & CardEntity.PLACEMENT_CHANGED) != 0) {
            world.put(entity, CardPlacementComponent.class,
                    new CardPlacementComponent(view.zone, view.slotIndex, view.pose));
        }
        if ((changes & CardEntity.INTERACTION_CHANGED) != 0) {
            world.put(entity, CardInteractionComponent.class,
                    new CardInteractionComponent(view.playable, view.selected, view.hovered, view.dragging));
        }
        if ((changes & CardEntity.ASSETS_CHANGED) != 0) {
            world.put(entity, CardAssetsComponent.class,
                    new CardAssetsComponent(view.artResourceId, view.frameResourceId));
        }
        world.put(entity, HostBindingComponent.class,
                new HostBindingComponent("STS1_C2", view.ref.instanceId));
        world.put(entity, DrawComponent.class,
                new DrawComponent("card", view.artResourceId, view.ref.cardId));
        world.put(entity, VisibilityComponent.class, new VisibilityComponent(true, 1f));
        world.put(entity, NodePropertiesComponent.class,
                new NodePropertiesComponent(java.util.Collections.<String, Object>singletonMap(
                        "instanceId", view.ref.instanceId)));
        if (view.pose != null) {
            world.put(entity, BoundsComponent.class,
                    new BoundsComponent(new artframework.component.Rect(
                            view.pose.x, view.pose.y, 0f, 0f), view.pose.z));
        }
    }

    public ControlsView controls() {
        return lastFrame.controlsView;
    }

    public MapView map() {
        return lastFrame.mapView;
    }

    public EventView event() {
        return lastFrame.eventView;
    }

    public SelectView select() {
        return lastFrame.selectView;
    }

    public RewardView reward() {
        return lastFrame.rewardView;
    }

    public RestView rest() {
        return lastFrame.restView;
    }

    public TreasureView treasure() {
        return lastFrame.treasureView;
    }

    public ShopView shop() {
        return lastFrame.shopView;
    }

    public TopPanelView topPanel() {
        return lastFrame.topPanelView;
    }

    public MonsterIntentView intents() {
        return lastFrame.intentsView;
    }

    public ViewportView viewport() {
        return lastFrame.viewport;
    }

    public Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("frameId", Long.valueOf(lastFrameId));
        m.put("sceneEpoch", Long.valueOf(sceneEpoch));
        m.put("scene", scene);
        m.put("available", Boolean.valueOf(available));
        m.put("stale", Boolean.valueOf(stale));
        m.put("cardCount", Integer.valueOf(byInstance.size()));
        m.put("handCount", Integer.valueOf(listZone(CardZone.HAND).size()));
        m.put("dragInstanceId", dragInstanceId != null ? dragInstanceId : "");
        m.put("endTurnEnabled", Boolean.valueOf(lastFrame.controlsView.endTurnEnabled));
        m.put("mapNodeCount", Integer.valueOf(lastFrame.mapView.nodeCount()));
        m.put("eventOptionCount", Integer.valueOf(lastFrame.eventView.optionCount()));
        m.put("selectPoolCount", Integer.valueOf(lastFrame.selectView.poolCount()));
        m.put("selectKind", lastFrame.selectView.kind);
        m.put("rewardItemCount", Integer.valueOf(lastFrame.rewardView.itemCount()));
        m.put("rewardKind", lastFrame.rewardView.kind);
        m.put("restOptionCount", Integer.valueOf(lastFrame.restView.optionCount()));
        m.put("treasureAvailable", Boolean.valueOf(lastFrame.treasureView.available));
        m.put("shopEntryCount", Integer.valueOf(lastFrame.shopView.entryCount()));
        m.put("topPanelAvailable", Boolean.valueOf(lastFrame.topPanelView.available));
        m.put("intentCount", Integer.valueOf(lastFrame.intentsView.entryCount()));
        m.put("viewportWidth", Integer.valueOf(lastFrame.viewport.logicalWidth));
        m.put("viewportHeight", Integer.valueOf(lastFrame.viewport.logicalHeight));
        return m;
    }
}
