package artframework.sts1.render;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;
import java.util.List;

/**
 * Drains pending transient-effect projections into nrcc-native presentation entities.
 * The only {@link Sts1NativePresentationAdapter} caller in the transient-effect chain;
 * the registry reference is the host-side pending-event source, not system-owned state.
 */
public final class TransientEffectProjectionSystem implements EcsSystem {
    private final TransientEffectRegistry registry;

    public TransientEffectProjectionSystem(TransientEffectRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry required");
        this.registry = registry;
    }

    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        drain();
    }

    /** Shared drain for the scheduled run and synchronous compatibility callers. */
    public void drain() {
        List<TransientEffectRegistry.PendingProjection> events =
                registry.drainPendingProjections();
        for (TransientEffectRegistry.PendingProjection event : events) {
            if (event.removal) {
                Sts1NativePresentationAdapter.remove(event.ownerId);
            } else {
                String entityId = Sts1NativePresentationAdapter.present(event.invocation);
                registry.recordProjected(event.instanceId, entityId);
            }
        }
    }
}
