package artframework.render;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;

/** Advances the host-only render clock during the ordered production ECS schedule. */
public final class RenderClockSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        RenderHosts.get().tick(tick.deltaSeconds);
    }
}
