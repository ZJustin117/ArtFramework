package artframework.presentation;

import artframework.component.EffectDecl;
import artframework.component.Rect;
import artframework.core.PresentChromeStyle;
import artframework.core.PresentPack;
import artframework.core.PresentPacks;
import artframework.core.PresentResolve;
import artframework.ecs.EntityId;
import java.util.List;

/** Materializes exact C2 visual items below their policy surface entity. */
public final class PresentationVisuals {
    private static final String CONTEXT = "c2-surfaces";

    private PresentationVisuals() {}

    public static EntityId syncC2Item(
            String surfaceId,
            String itemId,
            Rect bounds,
            float z,
            String role,
            String resourceId,
            String text,
            boolean visible) {
        if (surfaceId == null || surfaceId.isEmpty() || itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("surfaceId and itemId required");
        }
        PresentationContext context = PresentationRegistry.context(CONTEXT);
        PresentationKey key = new PresentationKey("sts1.visual." + surfaceId, itemId);
        EntityId entity = context.entity(key);
        if (entity == null) {
            entity = context.create(key, itemId, role, "c2-visual");
            EntityId parent = context.entity(new PresentationKey("sts1.surface", surfaceId));
            if (parent != null) {
                NodeHierarchyComponent hierarchy = context.world().get(entity, NodeHierarchyComponent.class);
                if (hierarchy.parent == null) {
                    NodeHierarchyComponent parentHierarchy =
                            context.world().get(parent, NodeHierarchyComponent.class);
                    java.util.List<EntityId> children =
                            new java.util.ArrayList<EntityId>(parentHierarchy.children);
                    children.add(entity);
                    context.world().put(parent, NodeHierarchyComponent.class,
                            new NodeHierarchyComponent(parentHierarchy.parent, children));
                    context.world().put(entity, NodeHierarchyComponent.class,
                            new NodeHierarchyComponent(parent, hierarchy.children));
                }
            }
        }
        PresentChromeStyle style = PresentResolve.chromeForSurface(surfaceId);
        context.world().put(entity, BoundsComponent.class, new BoundsComponent(bounds, z));
        context.world().put(entity, VisibilityComponent.class, new VisibilityComponent(visible, 1f));
        context.world().put(entity, DrawComponent.class, new DrawComponent(role, resourceId, text));
        context.world().put(entity, HostBindingComponent.class,
                new HostBindingComponent("STS1_C2", surfaceId + ":" + itemId));
        context.world().put(entity, ChromeComponent.class,
                new ChromeComponent(style.panelR, style.panelG, style.panelB, style.panelAlpha,
                        style.borderR, style.borderG, style.borderB, style.borderA, style.borderWidth));
        EffectsComponent effects = context.world().get(entity, EffectsComponent.class);
        for (EffectDecl effect : effectsFor(surfaceId)) {
            effects.put(new EffectAttachment(effect.id, "ambient", effect.params));
        }
        return entity;
    }

    public static void removeC2Item(String surfaceId, String itemId) {
        PresentationContext context = PresentationRegistry.context(CONTEXT);
        EntityId entity = context.entity(new PresentationKey("sts1.visual." + surfaceId, itemId));
        if (entity != null) context.destroy(entity);
    }

    private static List<EffectDecl> effectsFor(String surfaceId) {
        PresentPack pack = PresentPacks.active();
        if (pack == null) return java.util.Collections.emptyList();
        List<EffectDecl> effects = pack.surfaceEffects.get(surfaceId);
        return effects != null ? effects : java.util.Collections.<EffectDecl>emptyList();
    }
}
