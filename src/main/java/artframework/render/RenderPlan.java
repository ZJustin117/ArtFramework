package artframework.render;

import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.EffectAttachment;
import artframework.presentation.EffectsComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationDrawItem;
import artframework.presentation.PresentationFrame;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.VisibilityComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.HashMap;

/** Immutable render target description projected from ECS render state. */
public final class RenderPlan {
    public static final class Entry {
        public final String id;
        public final RenderTargetKind kind;
        public final Rect bounds;
        public final float z;
        public final RenderPhase phase;
        public final String stableKey;
        public final boolean enabled;
        public final List<EffectAttachment> effects;

        Entry(String id, RenderTargetKind kind, Rect bounds, float z, boolean enabled,
                List<EffectAttachment> effects) {
            this(id, kind, bounds, RenderPhase.C2_CONTENT, z, id, enabled, effects);
        }

        Entry(String id, RenderTargetKind kind, Rect bounds, RenderPhase phase, float z,
                String stableKey, boolean enabled, List<EffectAttachment> effects) {
            this.id = id;
            this.kind = kind;
            this.bounds = bounds;
            this.z = z;
            this.phase = phase;
            this.stableKey = stableKey;
            this.enabled = enabled;
            this.effects = effects == null || effects.isEmpty()
                    ? Collections.<EffectAttachment>emptyList()
                    : Collections.unmodifiableList(new ArrayList<EffectAttachment>(effects));
        }

        RenderOrder order() {
            return new RenderOrder(phase, z, stableKey);
        }
    }

    private final List<Entry> entries;

    private RenderPlan(List<Entry> entries) {
        List<Entry> ordered = new ArrayList<Entry>(entries);
        Map<String, String> stableKeyOwners = new HashMap<String, String>();
        for (Entry entry : ordered) {
            String priorOwner = stableKeyOwners.put(entry.stableKey, entry.id);
            if (priorOwner != null && !priorOwner.equals(entry.id)) {
                throw new IllegalArgumentException("duplicate render stable key: " + entry.stableKey);
            }
        }
        Collections.sort(ordered, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                return RenderOrder.COMPARATOR.compare(a.order(), b.order());
            }
        });
        this.entries = Collections.unmodifiableList(ordered);
    }

    private static Entry item(String id, RenderTargetKind kind, Rect bounds, RenderPhase phase,
            float z, boolean enabled, List<EffectAttachment> effects) {
        return new Entry(id, kind, bounds, phase, z, id, enabled, effects);
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Build a stable snapshot of all non-C1 host render state owned by ECS. */
    public static RenderPlan fromEcs(Set<String> activeSurfaceIds) {
        List<Entry> entries = new ArrayList<Entry>();
        FullFrameRenderComponent full = RenderStateEcs.fullFrameState();
        if (full != null && full.enabled) {
            entries.add(item(RenderHost.FULL_FRAME_ID, RenderTargetKind.FULL_FRAME,
                    full.bounds, RenderPhase.C2_CONTENT, 1000f, true, full.effects()));
        }

        appendC2SurfaceEntries(entries, activeSurfaceIds);
        return appendFullProjectionEntries(entries,
                PresentationRegistry.context("c2-entity-present"),
                PresentationRegistry.context("c2-surfaces"),
                PresentationRegistry.existingContext("nrcc-native"));
    }

    /** Build only the C2 surface targets needed during an STS render pass. */
    public static RenderPlan fromActiveC2Surfaces(Set<String> activeSurfaceIds) {
        List<Entry> entries = new ArrayList<Entry>();
        appendC2SurfaceEntries(entries, activeSurfaceIds);
        return new RenderPlan(entries);
    }

    private static void appendC2SurfaceEntries(List<Entry> entries, Set<String> activeSurfaceIds) {
        PresentationContext render = RenderStateEcs.context();
        Map<String, List<EffectAttachment>> surfaceEffects =
                new LinkedHashMap<String, List<EffectAttachment>>();
        for (EntityId entity : render.entities()) {
            RenderSurfaceComponent surface = render.world().get(entity, RenderSurfaceComponent.class);
            if (surface == null) continue;
            if (activeSurfaceIds != null && !activeSurfaceIds.contains(surface.surfaceId)) continue;
            surfaceEffects.put(surface.surfaceId, surface.effects());
            entries.add(item(RenderHost.c2SurfaceTargetId(surface.surfaceId),
                    RenderTargetKind.C2_SURFACE, surface.bounds, RenderPhase.C2_CONTENT,
                    surface.z, surface.enabled, surface.effects()));
        }

        PresentationContext visuals = PresentationRegistry.context("c2-surfaces");
        for (EntityId entity : visuals.entities()) {
            NodeIdentityComponent identity = visuals.world().get(entity, NodeIdentityComponent.class);
            if (identity == null || !identity.key.scope.startsWith("sts1.visual.")) continue;
            String surfaceId = identity.key.scope.substring("sts1.visual.".length());
            if (activeSurfaceIds != null && !activeSurfaceIds.contains(surfaceId)) continue;
            BoundsComponent bounds = visuals.world().get(entity, BoundsComponent.class);
            VisibilityComponent visibility = visuals.world().get(entity, VisibilityComponent.class);
            if (bounds == null || visibility == null) continue;
            EffectsComponent effects = visuals.world().get(entity, EffectsComponent.class);
            List<EffectAttachment> itemEffects = new ArrayList<EffectAttachment>();
            List<EffectAttachment> inherited = surfaceEffects.get(surfaceId);
            if (inherited != null) itemEffects.addAll(inherited);
            if (effects != null) itemEffects.addAll(effects.attachments());
            entries.add(item(RenderHost.c2ItemTargetId(surfaceId, identity.key.localId),
                    RenderTargetKind.C2_SURFACE, bounds.rect, RenderPhase.C2_CONTENT, bounds.z,
                    visibility.visible && bounds.rect.width > 0f && bounds.rect.height > 0f,
                    itemEffects));
        }
    }

    private static RenderPlan appendFullProjectionEntries(List<Entry> entries,
            PresentationContext entityContext, PresentationContext visuals,
            PresentationContext nativeContext) {
        for (EntityId entity : entityContext.entities()) {
            artframework.c2.EntitySlotIdentityComponent identity =
                    entityContext.world().get(entity, artframework.c2.EntitySlotIdentityComponent.class);
            artframework.c2.EntitySlotTransformComponent transform =
                    entityContext.world().get(entity, artframework.c2.EntitySlotTransformComponent.class);
            if (identity == null || transform == null) continue;
            float scale = transform.scale > 0f ? transform.scale : 1f;
            float[] size = artframework.c2.EntityDrawPath.defaultSize(identity.kind, scale);
            entries.add(item("c2:entity:" + identity.slotId, RenderTargetKind.ENTITY_SLOT,
                    new Rect(transform.x - size[0] * 0.5f, transform.y - size[1] * 0.5f,
                            size[0], size[1]), RenderPhase.ENTITY_CONTENT, 10f, transform.laidOut,
                    Collections.<EffectAttachment>emptyList()));
        }
        for (String scope : PresentationRegistry.scopes()) {
            if (!scope.startsWith("tree:")) continue;
            PresentationContext context = PresentationRegistry.existingContext(scope);
            if (context == null) continue;
            for (PresentationDrawItem item : PresentationFrame.from(context).items) {
                entries.add(item(c1TargetId(item),
                        item.root ? RenderTargetKind.SYNTHETIC_WINDOW
                                : RenderTargetKind.SYNTHETIC_WIDGET,
                        item.bounds, RenderPhase.C1_CONTENT, item.z, true, item.effects));
                if (item.root) {
                    List<EffectAttachment> titleEffects = new ArrayList<EffectAttachment>();
                    List<artframework.component.EffectDecl> defaults =
                            artframework.core.PackEffectDefaults.forNodeType(
                                    visuals.world(), artframework.component.UiTypes.LABEL);
                    for (artframework.component.EffectDecl effect : defaults) {
                        titleEffects.add(new EffectAttachment(effect.id, "ambient", effect.params));
                    }
                    entries.add(item(c1TitleTargetId(item),
                            RenderTargetKind.SYNTHETIC_WIDGET, item.bounds, RenderPhase.C1_CONTENT,
                            item.z + 0.001f,
                            true, titleEffects));
                }
            }
        }
        if (nativeContext != null) {
            for (PresentationDrawItem item : PresentationFrame.from(nativeContext).items) {
                entries.add(item("native:" + item.key.localId,
                        RenderTargetKind.SYNTHETIC_WIDGET, item.bounds, RenderPhase.NATIVE_RETAINED,
                        item.z, true, item.effects));
            }
        }
        return new RenderPlan(entries);
    }

    private static String c1TargetId(PresentationDrawItem item) {
        if (item.hostBinding != null && "SCENE2D_C1".equals(item.hostBinding.hostKind)) {
            String local = item.hostBinding.localKey;
            int separator = local.indexOf(':');
            if (separator > 0 && separator < local.length() - 1) {
                String window = local.substring(0, separator);
                String key = local.substring(separator + 1);
                return item.root ? "c1:" + window : "c1:" + window + ":" + key;
            }
        }
        return item.key.toString();
    }

    private static String c1TitleTargetId(PresentationDrawItem item) {
        if (item.hostBinding != null) {
            String local = item.hostBinding.localKey;
            int separator = local.indexOf(':');
            if (separator > 0) return "c1:" + local.substring(0, separator) + ":__art_title";
        }
        return c1TargetId(item) + ":__art_title";
    }
}
