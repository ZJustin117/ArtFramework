package artframework.vfx;

import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.EcsSystem;

/** Idempotent ART VFX system registration for the shared pack schedule. */
public final class VfxSystems {
    public static final String SIMULATION_ID = "art.vfx.simulation";
    public static final String PROJECTION_ID = "art.vfx.render-projection";
    private VfxSystems() {}

    public static synchronized void enable() {
        if (!contains(PackSystemPhase.EFFECTS, SimulationSystem.class)) {
            PackSystems.enable(PackSystemPhase.EFFECTS, SIMULATION_ID,
                    new SimulationSystem());
        }
        if (!contains(PackSystemPhase.RENDER_PROJECTION, ParticleRenderProjectionSystem.class)) {
            PackSystems.enable(PackSystemPhase.RENDER_PROJECTION, PROJECTION_ID,
                    new ParticleRenderProjectionSystem());
        }
        // Re-assert the aggregator at the end of RENDER_PROJECTION: this producer was just enabled
        // (possibly appending after a previously-installed aggregator), and any other producer may
        // have registered since. The aggregator must always run last or contributions land a frame
        // late.
        artframework.render.ArtRenderFrameAggregationSystem.install();
    }

    public static synchronized void disable() {
        PackSystems.disable(PackSystemPhase.RENDER_PROJECTION, PROJECTION_ID);
        PackSystems.disable(PackSystemPhase.EFFECTS, SIMULATION_ID);
    }

    private static boolean contains(PackSystemPhase phase, Class<?> type) {
        for (EcsSystem system : PackSystems.systemsFor(phase)) {
            if (system != null && type.isInstance(system)) return true;
        }
        return false;
    }

    private static final class SimulationSystem implements EcsSystem {
        private final ParticleSpawnSystem spawn = new ParticleSpawnSystem();
        private final ParticleIntegrateSystem integrate = new ParticleIntegrateSystem();
        private final ParticleCurveSystem curves = new ParticleCurveSystem();
        private final VfxLifecycleSystem lifecycle = new VfxLifecycleSystem();
        @Override public void run(artframework.ecs.PresentationWorld world, artframework.ecs.EcsTick tick) {
            spawn.run(world, tick); integrate.run(world, tick); curves.run(world, tick); lifecycle.run(world, tick);
        }
    }
}
