package artframework.render;

import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.EffectAttachment;
import artframework.presentation.EffectsComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stateless writer/query adapter for host render data stored in the shared ECS world. */
public final class RenderStateEcs {
    private static final String SCOPE = "render-state";
    private static final PresentationContext CONTEXT = PresentationRegistry.context(SCOPE);

    private RenderStateEcs() {}

    public static void surface(String surfaceId, float x, float y, float width, float height) {
        surface(surfaceId, x, y, width, height, true);
    }

    public static void surface(String surfaceId, float x, float y, float width, float height,
            boolean enabled) {
        EntityId entity = entity("surface", surfaceId);
        RenderSurfaceComponent current = CONTEXT.world().get(entity, RenderSurfaceComponent.class);
        List<EffectAttachment> effects = current != null ? current.effects() : Collections.<EffectAttachment>emptyList();
        CONTEXT.world().put(entity, RenderSurfaceComponent.class,
                new RenderSurfaceComponent(surfaceId, new Rect(x, y, width, height), enabled, -10f, effects));
    }

    public static void surfaceEffects(String surfaceId, List<EffectAttachment> effects) {
        EntityId entity = entity("surface", surfaceId);
        RenderSurfaceComponent current = CONTEXT.world().get(entity, RenderSurfaceComponent.class);
        Rect bounds = current != null ? current.bounds : new Rect(0f, 0f, 0f, 0f);
        boolean enabled = current == null || current.enabled;
        CONTEXT.world().put(entity, RenderSurfaceComponent.class,
                new RenderSurfaceComponent(surfaceId, bounds, enabled, -10f, effects));
    }

    public static void removeSurface(String surfaceId) {
        EntityId entity = CONTEXT.entity(new PresentationKey("render.surface", surfaceId));
        if (entity != null) CONTEXT.destroy(entity);
    }

    public static void fullFrame(float width, float height, boolean enabled,
            List<EffectAttachment> effects) {
        EntityId entity = entity("full-frame", "full_frame");
        CONTEXT.world().put(entity, FullFrameRenderComponent.class,
                new FullFrameRenderComponent(new Rect(0f, 0f, width, height), enabled, effects));
    }

    public static FullFrameRenderComponent fullFrameState() {
        EntityId entity = CONTEXT.entity(new PresentationKey("render.full-frame", "full_frame"));
        return entity == null ? null : CONTEXT.world().get(entity, FullFrameRenderComponent.class);
    }

    public static RenderSurfaceComponent surfaceState(String surfaceId) {
        if (surfaceId == null || surfaceId.trim().isEmpty()) return null;
        EntityId entity = CONTEXT.entity(new PresentationKey("render.surface", surfaceId));
        return entity == null ? null : CONTEXT.world().get(entity, RenderSurfaceComponent.class);
    }

    public static void fullFrameEffects(List<EffectAttachment> effects) {
        FullFrameRenderComponent current = fullFrameState();
        fullFrame(current != null ? current.bounds.width : 1920f,
                current != null ? current.bounds.height : 1080f,
                current != null && current.enabled, effects);
    }

    public static void removeFullFrame() {
        EntityId entity = CONTEXT.entity(new PresentationKey("render.full-frame", "full_frame"));
        if (entity != null) CONTEXT.destroy(entity);
    }

    public static void resetForTests() {
        for (EntityId entity : new ArrayList<EntityId>(CONTEXT.entities())) {
            CONTEXT.destroy(entity);
        }
    }

    public static PresentationContext context() { return CONTEXT; }

    private static EntityId entity(String scope, String id) {
        PresentationKey key = new PresentationKey("render." + scope, id);
        EntityId entity = CONTEXT.entity(key);
        return entity != null ? entity : CONTEXT.create(key, id, "render", "artframework");
    }
}
