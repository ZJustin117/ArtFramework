package artframework.vfx;

import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.EcsSystem;
import artframework.render.ArtRenderFrameAggregationSystem;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VfxSystemsTest {
    @After public void cleanup() { PackSystems.resetForTests(); }

    @Test
    public void enableRemainsIdempotentAndWorksAfterPackReset() {
        VfxSystems.enable();
        VfxSystems.enable();
        assertEquals(1, PackSystems.systemsFor(PackSystemPhase.EFFECTS).size());
        assertRenderProjectionIsProducerThenAggregator();
        PackSystems.resetForTests();
        VfxSystems.enable();
        assertEquals(1, PackSystems.systemsFor(PackSystemPhase.EFFECTS).size());
        assertRenderProjectionIsProducerThenAggregator();
    }

    /**
     * Enabling VFX now also re-asserts the shared aggregation pass, so the phase holds exactly the
     * VFX producer followed by the aggregator (the aggregator must run last).
     */
    private static void assertRenderProjectionIsProducerThenAggregator() {
        List<EcsSystem> systems = PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION);
        assertEquals(2, systems.size());
        assertTrue(systems.get(0) instanceof ParticleRenderProjectionSystem);
        assertTrue(systems.get(1) instanceof ArtRenderFrameAggregationSystem);
    }
}
