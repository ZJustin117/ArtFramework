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
    private static final PresentationContext CONTEXT = PresentationRegistry.context("sts1-input");
    private static final PresentationWorld WORLD = CONTEXT.world();

    private NativeInputRecords() {}

    public static void input(String name, String surfaceId) {
        WORLD.put(entity(surfaceId), NativeInputComponent.class,
                new NativeInputComponent(name, surfaceId));
    }

    public static void intercept(String surfaceId, boolean processed, boolean suppressNative,
            String reason) {
        WORLD.put(entity(surfaceId), NativeInterceptComponent.class,
                new NativeInterceptComponent(processed, suppressNative, reason));
    }

    public static void intent(String surfaceId, String name,
            NativeIntentLifecycleComponent.State state, String message) {
        WORLD.put(entity(surfaceId), NativeIntentLifecycleComponent.class,
                new NativeIntentLifecycleComponent(name, state, message));
    }

    public static void observe(String surfaceId, long frameId, long sceneEpoch, String scene) {
        EntityId entity = entity(surfaceId);
        WORLD.put(entity, NativeIntentObservationComponent.class,
                new NativeIntentObservationComponent(frameId, sceneEpoch, scene));
        NativeIntentLifecycleComponent lifecycle = WORLD.get(entity, NativeIntentLifecycleComponent.class);
        if (lifecycle != null && lifecycle.state == NativeIntentLifecycleComponent.State.EXECUTED) {
            WORLD.put(entity, NativeIntentLifecycleComponent.class,
                    new NativeIntentLifecycleComponent(
                            lifecycle.name, NativeIntentLifecycleComponent.State.CONFIRMED,
                            "authority frame observed"));
        }
    }

    /** Mark an executed intent failed when its next authority frame is unavailable. */
    public static void failed(String surfaceId, String message) {
        EntityId entity = entity(surfaceId);
        NativeIntentLifecycleComponent lifecycle = WORLD.get(entity, NativeIntentLifecycleComponent.class);
        if (lifecycle != null && lifecycle.state == NativeIntentLifecycleComponent.State.EXECUTED) {
            WORLD.put(entity, NativeIntentLifecycleComponent.class,
                    new NativeIntentLifecycleComponent(
                            lifecycle.name, NativeIntentLifecycleComponent.State.FAILED, message));
        }
    }

    public static void resetForTests() {
        for (EntityId entity : new java.util.ArrayList<EntityId>(CONTEXT.entities())) {
            if (WORLD.get(entity, NativeInputComponent.class) != null
                    || WORLD.get(entity, NativeInterceptComponent.class) != null
                    || WORLD.get(entity, NativeIntentLifecycleComponent.class) != null
                    || WORLD.get(entity, NativeIntentObservationComponent.class) != null) {
                CONTEXT.destroy(entity);
            }
        }
    }

    private static EntityId entity(String surfaceId) {
        String id = SurfaceIds.canonicalize(surfaceId);
        PresentationKey key = new PresentationKey("sts1.input", id);
        EntityId entity = CONTEXT.entity(key);
        return entity != null ? entity : CONTEXT.create(key, id, "input", "sts1");
    }
}
