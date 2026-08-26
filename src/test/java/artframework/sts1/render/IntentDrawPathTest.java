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

public class IntentDrawPathTest {

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
        ArtFramework.component(SurfaceIds.COMBAT_INTENTS).mount();
    }

    @Test
    public void suppressesNativeIntentsOnlyWhenFullReady() {
        mountedCombat();
        assertFalse("OFF keeps native renderer", IntentDrawPath.shouldSuppressNativeIntents());

        FullPresentMode.setIntentsLevel(PresentLevel.FULL);
        assertFalse("FULL without executor stays native", IntentDrawPath.shouldSuppressNativeIntents());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue("FULL + mounted + combat + ready executor suppresses native",
                IntentDrawPath.shouldSuppressNativeIntents());

        Sts1RenderPipeline.setOverlayObserve(true);
        assertFalse("overlay-observe forces native visibility", IntentDrawPath.shouldSuppressNativeIntents());
    }

    @Test
    public void surfaceDrawPlanSuppressesNativeIntentsWhenFullMountedCombat() {
        mountedCombat();
        FullPresentMode.setIntentsLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldSuppressNative(SurfaceIds.COMBAT_INTENTS));
    }

    @Test
    public void probeSlice() {
        mountedCombat();
        FullPresentMode.setIntentsLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = IntentDrawPath.probeSlice();
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals(Boolean.FALSE, m.get("suppressNativeIntents"));
    }
}
