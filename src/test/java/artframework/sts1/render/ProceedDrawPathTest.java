package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
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
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProceedDrawPathTest {

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
        ArtFramework.component(SurfaceIds.COMBAT_PROCEED).mount();
    }

    @Test
    public void suppressesNativeProceedOnlyWhenFullReady() {
        mountedCombat();
        assertFalse("OFF keeps native renderer", ProceedDrawPath.shouldSuppressNativeProceed());

        FullPresentMode.setProceedLevel(PresentLevel.FULL);
        assertFalse("FULL without executor stays native", ProceedDrawPath.shouldSuppressNativeProceed());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue("FULL + mounted + combat + ready executor suppresses native",
                ProceedDrawPath.shouldSuppressNativeProceed());

        Sts1RenderPipeline.setOverlayObserve(true);
        assertFalse("overlay-observe forces native visibility", ProceedDrawPath.shouldSuppressNativeProceed());
    }

    @Test
    public void surfaceDrawPlanSuppressesNativeProceedWhenFullMountedCombat() {
        mountedCombat();
        FullPresentMode.setProceedLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldSuppressNative(SurfaceIds.COMBAT_PROCEED));
    }

    @Test
    public void probeSlice() {
        mountedCombat();
        FullPresentMode.setProceedLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = ProceedDrawPath.probeSlice();
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals(Boolean.FALSE, m.get("suppressNativeProceed"));
    }

    @Test
    public void buildsStableButtonItemsWithStateResourcesAndGeometry() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combatWithProceed(3, 1, 0, 0, 0,
                        true, true, true, true, false, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());

        List<ProceedDrawPath.DrawItem> items = ProceedDrawPath.buildFromProjection();

        assertEquals(2, items.size());
        assertEquals(ControlsView.PROCEED_ID, items.get(0).id);
        assertEquals("Proceed", items.get(0).text);
        assertEquals(ResourceIds.UI_BUTTON_PROCEED_ENABLED, items.get(0).resourceId);
        assertTrue(items.get(0).enabled);
        assertEquals(780f, items.get(0).bounds(1920f, 1080f).x, 0.01f);
        assertEquals(ControlsView.CANCEL_ID, items.get(1).id);
        assertEquals(ResourceIds.UI_BUTTON_CANCEL_DISABLED, items.get(1).resourceId);
        assertFalse(items.get(1).enabled);
        assertEquals(items.size(), ProceedDrawPath.materializedDrawCount());
    }
}
