package artframework.core;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;

/** Advances the configured host backend at the final production ECS schedule phase. */
public final class HostBackendTickSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        HostBackends.get().tick(tick.deltaSeconds);
    }
}
