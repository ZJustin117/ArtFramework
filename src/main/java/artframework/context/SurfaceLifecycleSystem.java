package artframework.context;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

/** Applies surface lifecycle commands to ECS data and consumes the command. */
public final class SurfaceLifecycleSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(SurfaceLifecycleRequestComponent.class)) {
            SurfaceLifecycleRequestComponent request =
                    world.get(entity, SurfaceLifecycleRequestComponent.class);
            world.put(entity, SurfaceLifecycleComponent.class,
                    new SurfaceLifecycleComponent(request.mounted));
            world.remove(entity, SurfaceLifecycleRequestComponent.class);
        }
    }
}
