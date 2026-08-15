package artframework.context;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

/** Projects pending authority observations into ECS component data. */
public final class AuthorityProjectionSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(AuthorityFrameComponent.class)) {
            AuthorityFrameComponent pending = world.get(entity, AuthorityFrameComponent.class);
            PresentProjections.publishDirect(pending.frame);
            world.remove(entity, AuthorityFrameComponent.class);
        }
    }
}
