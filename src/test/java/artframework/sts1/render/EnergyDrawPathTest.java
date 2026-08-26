package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnergyDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void mountedCombat() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
    }

    @Test
    public void suppressesNativeEnergyOnlyWhenFullReady() {
        mountedCombat();
        assertFalse("OFF keeps native renderer", EnergyDrawPath.shouldSuppressNativeEnergy());

        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        assertFalse("FULL without executor stays native", EnergyDrawPath.shouldSuppressNativeEnergy());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue("FULL + mounted + combat + ready executor suppresses native",
                EnergyDrawPath.shouldSuppressNativeEnergy());

        Sts1RenderPipeline.setOverlayObserve(true);
        assertFalse("overlay-observe forces native visibility", EnergyDrawPath.shouldSuppressNativeEnergy());
    }

    @Test
    public void surfaceDrawPlanSuppressesNativeEnergyWhenFullMountedCombat() {
        mountedCombat();
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldSuppressNative(SurfaceIds.COMBAT_ENERGY));
    }

    @Test
    public void probeSlice() {
        mountedCombat();
        FullPresentMode.setEnergyLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = EnergyDrawPath.probeSlice();
        assertEquals(Integer.valueOf(3), m.get("energy"));
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals(Boolean.FALSE, m.get("suppressNativeEnergy"));
    }
}
