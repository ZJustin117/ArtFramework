package artframework.render;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.PresentationWorld;
import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;

/** Stateless render-projection pass for native render input components. */
public final class NativeRenderInputSystem implements EcsSystem {
    public static final String SYSTEM_ID = "art.native-render-input";

    /** Installs this system into the existing schedule-owned render projection phase. */
    public static void install() {
        PackSystems.enable(PackSystemPhase.RENDER_PROJECTION, SYSTEM_ID,
                new NativeRenderInputSystem());
    }

    @Override public void run(PresentationWorld world, EcsTick tick) {
        if (world == null || tick == null) throw new IllegalArgumentException("world and tick required");
        new NativeRenderFrameExtractor().extract(world);
    }
}
