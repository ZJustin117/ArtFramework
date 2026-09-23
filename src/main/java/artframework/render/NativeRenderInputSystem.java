package artframework.render;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

/** Stateless validation pass for native render input components. */
public final class NativeRenderInputSystem implements EcsSystem {
    @Override public void run(PresentationWorld world, EcsTick tick) {
        if (world == null || tick == null) throw new IllegalArgumentException("world and tick required");
        for (EntityId id : world.query(NativeRenderInputComponent.class)) {
            NativeRenderInputComponent input = world.get(id, NativeRenderInputComponent.class);
            new RenderOrder(input.phase(), input.z(), input.stableKey());
            if (input.nativeRenderFamily() == null || input.ownerId() == null
                    || input.bounds() == null || input.ownership() == null) {
                throw new IllegalStateException("invalid native render input");
            }
        }
    }
}
