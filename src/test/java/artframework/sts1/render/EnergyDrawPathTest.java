package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.assets.ResourceIds;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.component.Rect;
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
        EnergyDrawPath.resetAnimationForTests();
    }

    private void mountedCombat() {
        mountedCombat(3);
    }

    private void mountedCombat(int energy) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combat(energy, 1, 0, 0, 0, true, true), MapView.empty(), null));
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

    @Test
    public void projectionProvidesOrbResourceAndStableGeometry() {
        mountedCombat();
        EnergyDrawPath.DrawItem item = EnergyDrawPath.buildFromProjection().get(0);
        assertEquals(ResourceIds.energyOrbLayer("red", 1), item.resourceId);
        Rect expected = EnergyDrawPath.orbBounds(1f, 1f, 1f);
        assertEquals(expected.x, item.bounds.x, 0.01f);
        assertEquals(expected.y, item.bounds.y, 0.01f);
        assertEquals(expected.width, item.bounds.width, 0.01f);
        assertEquals(expected.height, item.bounds.height, 0.01f);
        assertEquals(item.bounds.width, item.bounds.height, 0.01f);
        assertEquals(6, item.layers.size());
        assertEquals(1, EnergyDrawPath.buildFromProjection().size());
    }

    @Test
    public void orbLayersMirrorNativeLayerStack() {
        mountedCombat(3);
        EnergyDrawPath.DrawItem lit = EnergyDrawPath.buildFromProjection().get(0);
        assertEquals(6, lit.layers.size());
        for (int i = 1; i <= 6; i++) {
            assertEquals(ResourceIds.energyOrbLayer("red", i), lit.layers.get(i - 1).resourceId);
        }

        mountedCombat(0);
        EnergyDrawPath.DrawItem dim = EnergyDrawPath.buildFromProjection().get(0);
        assertEquals(6, dim.layers.size());
        for (int i = 1; i <= 5; i++) {
            assertEquals(ResourceIds.energyOrbDimLayer("red", i), dim.layers.get(i - 1).resourceId);
        }
        assertEquals(ResourceIds.energyOrbLayer("red", 6), dim.layers.get(5).resourceId);

        assertEquals("red", EnergyDrawPath.orbColor());
        for (EnergyDrawPath.OrbLayer layer : lit.layers) {
            assertTrue(layer.resourceId, Sts1VanillaCatalog.isKnown(layer.resourceId));
        }
        for (EnergyDrawPath.OrbLayer layer : dim.layers) {
            assertTrue(layer.resourceId, Sts1VanillaCatalog.isKnown(layer.resourceId));
        }
    }
}
