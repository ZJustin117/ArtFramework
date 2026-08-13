package artframework.ecs;

import java.util.List;

/** Executes an explicitly ordered list of stateless ECS systems. */
public final class EcsPipeline {
    private EcsPipeline() {}

    public static void run(PresentationWorld world, EcsTick tick, List<? extends EcsSystem> systems) {
        if (world == null) throw new IllegalArgumentException("world required");
        if (tick == null) throw new IllegalArgumentException("tick required");
        if (systems == null) throw new IllegalArgumentException("systems required");
        for (EcsSystem system : systems) {
            if (system == null) throw new IllegalArgumentException("system required");
            system.run(world, tick);
        }
    }
}
