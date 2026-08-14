package artframework.skeleton;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;

/** Advances provider-owned skeleton handles from the ECS-backed presentation snapshot. */
public final class SkeletonHostTickSystem implements EcsSystem {
    private final SkeletonPresentationSystem presentation;

    public SkeletonHostTickSystem(SkeletonPresentationSystem presentation) {
        if (presentation == null) throw new IllegalArgumentException("presentation required");
        this.presentation = presentation;
    }

    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        presentation.tick(tick.deltaSeconds);
    }
}
