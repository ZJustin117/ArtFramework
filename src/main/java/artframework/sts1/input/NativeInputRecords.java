package artframework.sts1.input;

import artframework.context.NativeInputComponent;
import artframework.context.NativeInterceptComponent;
import artframework.context.NativeIntentLifecycleComponent;
import artframework.context.NativeIntentObservationComponent;
import artframework.context.SurfaceIds;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;

/** Stateless writer for native input records owned by the shared ART ECS world. */
public final class NativeInputRecords {
    private NativeInputRecords() {}

    public static void input(String name, String surfaceId) {
        world().put(entity(surfaceId), NativeInputComponent.class,
                new NativeInputComponent(name, surfaceId));
    }

    public static void intercept(String surfaceId, boolean processed, boolean suppressNative,
            String reason) {
        world().put(entity(surfaceId), NativeInterceptComponent.class,
                new NativeInterceptComponent(processed, suppressNative, reason));
    }

    public static void intent(String surfaceId, String name,
            NativeIntentLifecycleComponent.State state, String message) {
        entity(surfaceId);
        artframework.api.ArtFramework.dispatchNativeIntentLifecycle(
                SurfaceIds.canonicalize(surfaceId), name, state, message);
    }

    public static void observe(String surfaceId, long frameId, long sceneEpoch, String scene) {
        EntityId entity = entity(surfaceId);
        world().put(entity, NativeIntentObservationComponent.class,
                NativeIntentObservationComponent.available(frameId, sceneEpoch, scene));
        runIntentSystem();
    }

    /** Mark an executed intent failed when its next authority frame is unavailable. */
    public static void failed(String surfaceId, long frameId, String message) {
        EntityId entity = entity(surfaceId);
        world().put(entity, NativeIntentObservationComponent.class,
                NativeIntentObservationComponent.unavailable(frameId, message));
        runIntentSystem();
    }

    public static void resetForTests() {
        PresentationContext context = context();
        for (EntityId entity : new java.util.ArrayList<EntityId>(context.entities())) {
            context.destroy(entity);
        }
    }

    private static EntityId entity(String surfaceId) {
        String id = SurfaceIds.canonicalize(surfaceId);
        PresentationKey key = new PresentationKey("sts1.input", id);
        PresentationContext context = context();
        EntityId entity = context.entity(key);
        return entity != null ? entity : context.create(key, id, "input", "sts1");
    }

    private static PresentationContext context() { return PresentationRegistry.context("sts1-input"); }

    private static PresentationWorld world() { return context().world(); }

    private static void runIntentSystem() {
        artframework.api.ArtFramework.processNativeIntentLifecycle();
    }
}
