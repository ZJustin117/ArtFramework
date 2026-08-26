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

public class TopPanelDrawPathTest {

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
        ArtFramework.component(SurfaceIds.TOP_PANEL).mount();
    }

    @Test
    public void suppressesNativeTopPanelOnlyWhenFullReady() {
        mountedCombat();
        assertFalse("OFF keeps native renderer", TopPanelDrawPath.shouldSuppressNativeTopPanel());

        FullPresentMode.setTopPanelLevel(PresentLevel.FULL);
        assertFalse("FULL without executor stays native", TopPanelDrawPath.shouldSuppressNativeTopPanel());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue("FULL + mounted + combat + ready executor suppresses native",
                TopPanelDrawPath.shouldSuppressNativeTopPanel());

        Sts1RenderPipeline.setOverlayObserve(true);
        assertFalse("overlay-observe forces native visibility", TopPanelDrawPath.shouldSuppressNativeTopPanel());
    }

    @Test
    public void surfaceDrawPlanSuppressesNativeTopPanelWhenFullMountedCombat() {
        mountedCombat();
        FullPresentMode.setTopPanelLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldSuppressNative(SurfaceIds.TOP_PANEL));
    }

    @Test
    public void probeSlice() {
        mountedCombat();
        FullPresentMode.setTopPanelLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = TopPanelDrawPath.probeSlice();
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals(Boolean.FALSE, m.get("suppressNativeTopPanel"));
    }
}
