package artframework.core;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;
import artframework.presentation.PresentationRuntime;

/** Advances C1 animation-player host caches for ECS-mounted windows. */
public final class AnimationPlaybackSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (String windowId : PresentationRuntime.openWindowIds()) {
            AnimationPlayers.tick(windowId, tick.deltaSeconds);
        }
    }
}
