package artframework.context;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

/** Consumes intent execution events and authority observations into lifecycle state. */
public final class NativeIntentLifecycleSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(NativeIntentLifecycleEventComponent.class)) {
            NativeIntentLifecycleEventComponent event =
                    world.get(entity, NativeIntentLifecycleEventComponent.class);
            if (event.state == NativeIntentLifecycleComponent.State.REQUESTED
                    || event.state == NativeIntentLifecycleComponent.State.SENT) {
                world.remove(entity, NativeIntentObservationComponent.class);
            }
            world.put(entity, NativeIntentLifecycleComponent.class,
                    new NativeIntentLifecycleComponent(event.name, event.state, event.message));
            world.remove(entity, NativeIntentLifecycleEventComponent.class);
        }

        for (EntityId entity : world.query(
                NativeIntentLifecycleComponent.class, NativeIntentObservationComponent.class)) {
            NativeIntentLifecycleComponent lifecycle =
                    world.get(entity, NativeIntentLifecycleComponent.class);
            NativeIntentObservationComponent observation =
                    world.get(entity, NativeIntentObservationComponent.class);
            if (lifecycle.state == NativeIntentLifecycleComponent.State.EXECUTED) {
                world.put(entity, NativeIntentLifecycleComponent.class,
                        new NativeIntentLifecycleComponent(lifecycle.name,
                                observation.available
                                        ? NativeIntentLifecycleComponent.State.CONFIRMED
                                        : NativeIntentLifecycleComponent.State.FAILED,
                                observation.available
                                        ? "authority frame observed" : observation.message));
            }
        }
    }
}
