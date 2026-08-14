package artframework.core;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;

/** Advances ECS-backed effect pulse envelopes in the ordered production schedule. */
public final class EffectPulseSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        EffectPulse.tick(tick.deltaSeconds);
    }
}
