package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
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

public class Sts1RenderPipelineTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
    }

    private void combatFrameMounted() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        Arrays.asList(
                                CardView.builder(new CardRef("h1", "Strike_R"))
                                        .zone(CardZone.HAND)
                                        .build()),
                        ControlsView.combat(3, 1, 0, 0, 0, true, true),
                        MapView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
    }

    @Test
    public void offSkipsDrawAndSuppress() {
        combatFrameMounted();
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertFalse(plan.shouldDraw(SurfaceIds.COMBAT_HAND));
        assertFalse(plan.shouldSuppressNative(SurfaceIds.COMBAT_HAND));
        assertEquals(SurfaceDrawPlan.DrawMode.SKIP, plan.find(SurfaceIds.COMBAT_HAND).mode);
    }

    @Test
    public void observeNeverDrawsOrSuppresses() {
        combatFrameMounted();
        FullPresentMode.setCombatHandLevel(PresentLevel.OBSERVE);
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertEquals(SurfaceDrawPlan.DrawMode.OBSERVE, plan.find(SurfaceIds.COMBAT_HAND).mode);
        assertFalse(plan.shouldDraw(SurfaceIds.COMBAT_HAND));
        assertFalse(plan.shouldSuppressNative(SurfaceIds.COMBAT_HAND));
        assertFalse(Sts1SurfaceRenderer.shouldSuppressNativeHand());
    }

    @Test
    public void fullMountedCombatDrawsAndSuppresses() {
        combatFrameMounted();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, plan.find(SurfaceIds.COMBAT_HAND).mode);
        assertTrue(plan.shouldDraw(SurfaceIds.COMBAT_HAND));
        assertTrue(plan.shouldDraw(SurfaceIds.COMBAT_CARD_SLOTS));
        assertTrue(plan.shouldSuppressNative(SurfaceIds.COMBAT_HAND));
        assertTrue(Sts1SurfaceRenderer.shouldSuppressNativeHand());
        // hand level drives hand + slots; layer order: slots then hand
        assertEquals(2, plan.drawOrder().size());
        assertEquals(PresentLayer.COMBAT_SLOTS, plan.drawOrder().get(0).layer);
        assertEquals(PresentLayer.COMBAT_HAND, plan.drawOrder().get(1).layer);
    }

    @Test
    public void fullMountedCombatWithoutExecutorFallsBackToNative() {
        combatFrameMounted();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        SurfaceDrawPlan.Entry hand = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_HAND);
        assertEquals(SurfaceDrawPlan.DrawMode.SKIP, hand.mode);
        assertEquals("FULL_FALLBACK_NATIVE", hand.effectiveState.name());
        assertEquals("executor_unavailable", hand.reason);
        assertFalse(hand.suppressNative);
    }

    @Test
    public void overlayObserveDowngradesFullToObserve() {
        combatFrameMounted();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        Sts1RenderPipeline.setOverlayObserve(true);
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertEquals(SurfaceDrawPlan.DrawMode.OBSERVE, plan.find(SurfaceIds.COMBAT_HAND).mode);
        assertFalse(plan.shouldSuppressNative(SurfaceIds.COMBAT_HAND));
        assertFalse(Sts1SurfaceRenderer.shouldSuppressNativeHand());
    }

    @Test
    public void batchGuardNestAndRestoreFlags() {
        BatchStateGuard g = new BatchStateGuard();
        g.beginCapture(true);
        assertTrue(g.shouldEndBatch());
        assertTrue(g.shouldRestoreBegin());
        g.beginCapture(false);
        assertTrue(g.isArmed());
        g.endCapture();
        assertTrue(g.isArmed());
        g.endCapture();
        assertFalse(g.isArmed());
    }

    @Test
    public void clipRectContains() {
        ClipRect c = new ClipRect(10, 20, 100, 50);
        assertTrue(c.contains(10, 20));
        assertTrue(c.contains(50, 40));
        assertFalse(c.contains(9, 20));
        assertFalse(ClipRect.none().contains(0, 0));
    }

    @Test
    public void probeSliceHasPlan() {
        combatFrameMounted();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        Map<String, Object> m = Sts1RenderPipeline.probeSlice();
        assertEquals("combat", m.get("scene"));
        assertEquals(Integer.valueOf(2), m.get("drawCount"));
        assertEquals(Boolean.FALSE, m.get("overlayObserve"));
    }

    @Test
    public void wrongSceneNoHandDraw() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(1L, 1L, "map", null, ControlsView.empty(), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        assertFalse(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_HAND));
    }
}
