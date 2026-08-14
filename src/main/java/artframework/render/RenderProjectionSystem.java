package artframework.render;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;

/** Flushes deferred ECS-to-host render-plan projections in the production schedule. */
public final class RenderProjectionSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        RenderProjectionQueue.flush();
    }
}
