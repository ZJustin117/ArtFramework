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
    public void mountingCombatRootAlsoMountsEnergySurface() {
        combatFrameMounted();
        assertTrue(ArtFramework.component(SurfaceIds.COMBAT_ENERGY).isMounted());
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_ENERGY));
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
    public void fullMountedSkeletonDoesNotRequireInputExecutor() {
        combatFrameMounted();
        ArtFramework.component(SurfaceIds.SKELETON).mount();
        FullPresentMode.setSkeletonLevel(PresentLevel.FULL);

        SurfaceDrawPlan.Entry skeleton = Sts1RenderPipeline.plan().find(SurfaceIds.SKELETON);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, skeleton.mode);
        assertEquals("FULL_READY", skeleton.effectiveState.name());
        assertEquals("ready", skeleton.reason);
    }

    @Test
    public void fullMountedIntentsDrawsAndSuppressesNative() {
        combatFrameMounted();
        ArtFramework.component(SurfaceIds.COMBAT_INTENTS).mount();
        FullPresentMode.setIntentsLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry intents = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_INTENTS);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, intents.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_INTENTS));
        assertTrue(intents.suppressNative);
        assertTrue(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_INTENTS));
    }

    @Test
    public void fullMountedTargetingObservesWithoutSuppressingNative() {
        combatFrameMounted();
        ArtFramework.component(SurfaceIds.COMBAT_TARGETING).mount();
        FullPresentMode.setTargetingLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry targeting = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_TARGETING);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, targeting.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_TARGETING));
        // Targeting is observe-first: the patch always continues native rendering.
        assertFalse("targeting must not suppress native render", targeting.suppressNative);
        assertFalse(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_TARGETING));
    }

    @Test
    public void targetingRequiresCombatSceneAndMountForOptionalFullSelfDraw() {
        combatFrameMounted();
        FullPresentMode.setTargetingLevel(PresentLevel.FULL);
        SurfaceDrawPlan.Entry targeting = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_TARGETING);
        assertEquals(SurfaceDrawPlan.DrawMode.SKIP, targeting.mode);
        assertFalse(targeting.suppressNative);

        ArtFramework.component(SurfaceIds.COMBAT_TARGETING).mount();
        targeting = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_TARGETING);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, targeting.mode);
        assertFalse(targeting.suppressNative);

        ArtFramework.publishFrame(ContextFrame.of(9L, "map", java.util.Collections.<CardView>emptyList()));
        targeting = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_TARGETING);
        assertEquals(SurfaceDrawPlan.DrawMode.SKIP, targeting.mode);
        assertFalse(targeting.suppressNative);
    }

    @Test
    public void offSkipsTargetingDraw() {
        combatFrameMounted();
        ArtFramework.component(SurfaceIds.COMBAT_TARGETING).mount();
        SurfaceDrawPlan.Entry targeting = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_TARGETING);
        assertEquals(SurfaceDrawPlan.DrawMode.SKIP, targeting.mode);
        assertFalse(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_TARGETING));
    }

    @Test
    public void fullMountedControlsDrawsAndSuppressesNative() {
        combatFrameMounted();
        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry controls = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_CONTROLS);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, controls.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_CONTROLS));
        // Controls are now ART-owned; native EndTurnButton.render is suppressed.
        assertTrue(controls.suppressNative);
        assertTrue(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_CONTROLS));
    }

    @Test
    public void fullMountedEnergyDrawsAndSuppressesNative() {
        combatFrameMounted();
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry energy = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_ENERGY);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, energy.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_ENERGY));
        assertTrue(energy.suppressNative);
        assertTrue(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_ENERGY));
    }

    @Test
    public void fullMountedTopPanelDrawsAndSuppressesNative() {
        combatFrameMounted();
        ArtFramework.component(SurfaceIds.TOP_PANEL).mount();
        FullPresentMode.setTopPanelLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry topPanel = Sts1RenderPipeline.plan().find(SurfaceIds.TOP_PANEL);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, topPanel.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.TOP_PANEL));
        assertTrue(topPanel.suppressNative);
        assertTrue(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.TOP_PANEL));
    }

    @Test
    public void fullMountedProceedDrawsAndSuppressesNative() {
        combatFrameMounted();
        ArtFramework.component(SurfaceIds.COMBAT_PROCEED).mount();
        FullPresentMode.setProceedLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry proceed = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_PROCEED);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, proceed.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_PROCEED));
        assertTrue(proceed.suppressNative);
        assertTrue(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_PROCEED));
    }

    @Test
    public void roomAndOverlaySurfacesSuppressNativeWhenFullMountedAndSceneMatches() {
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        String[] scenes = {"map", "event", "select", "reward", "rest", "shop", "treasure"};
        String[] surfaces = {
            SurfaceIds.MAP,
            SurfaceIds.EVENT,
            SurfaceIds.SELECT_GRID,
            SurfaceIds.SELECT_HAND,
            SurfaceIds.REWARD_COMBAT,
            SurfaceIds.REWARD_CARD,
            SurfaceIds.REWARD_BOSS_RELIC,
            SurfaceIds.REST,
            SurfaceIds.SHOP,
            SurfaceIds.TREASURE
        };
        for (String scene : scenes) {
            publishMinimalScene(scene);
            for (String surfaceId : surfaces) {
                SurfaceDrawPlan.Entry entry = Sts1RenderPipeline.plan().find(surfaceId);
                if (entry == null) {
                    continue;
                }
                if (entry.mode == SurfaceDrawPlan.DrawMode.DRAW) {
                    assertTrue("surface " + surfaceId + " in scene " + scene + " must suppress native",
                            entry.suppressNative);
                }
            }
        }
    }

    private void publishMinimalScene(String scene) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L, 1L, scene, null, ControlsView.empty(), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        FullPresentMode.setLevel(SurfaceIds.MAP, "map".equals(scene) ? PresentLevel.FULL : PresentLevel.OFF);
        FullPresentMode.setLevel(SurfaceIds.EVENT, "event".equals(scene) ? PresentLevel.FULL : PresentLevel.OFF);
        FullPresentMode.setLevel(SurfaceIds.SELECT_GRID, "select".equals(scene) ? PresentLevel.FULL : PresentLevel.OFF);
        FullPresentMode.setLevel(SurfaceIds.REWARD_COMBAT, "reward".equals(scene) ? PresentLevel.FULL : PresentLevel.OFF);
        FullPresentMode.setLevel(SurfaceIds.REST, "rest".equals(scene) ? PresentLevel.FULL : PresentLevel.OFF);
        FullPresentMode.setLevel(SurfaceIds.SHOP, "shop".equals(scene) ? PresentLevel.FULL : PresentLevel.OFF);
        FullPresentMode.setLevel(SurfaceIds.TREASURE, "treasure".equals(scene) ? PresentLevel.FULL : PresentLevel.OFF);
        if ("map".equals(scene)) {
            ArtFramework.component(SurfaceIds.MAP).mount();
        } else if ("event".equals(scene)) {
            ArtFramework.component(SurfaceIds.EVENT).mount();
        } else if ("select".equals(scene)) {
            ArtFramework.component(SurfaceIds.SELECT_GRID).mount();
            ArtFramework.component(SurfaceIds.SELECT_HAND).mount();
        } else if ("reward".equals(scene)) {
            ArtFramework.component(SurfaceIds.REWARD_COMBAT).mount();
        } else if ("rest".equals(scene)) {
            ArtFramework.component(SurfaceIds.REST).mount();
        } else if ("shop".equals(scene)) {
            ArtFramework.component(SurfaceIds.SHOP).mount();
        } else if ("treasure".equals(scene)) {
            ArtFramework.component(SurfaceIds.TREASURE).mount();
        }
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
    public void probeSliceHasPlan() {
        combatFrameMounted();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        Map<String, Object> m = Sts1RenderPipeline.probeSlice();
        assertEquals("combat", m.get("scene"));
        assertEquals(Integer.valueOf(2), m.get("drawCount"));
        assertEquals(Boolean.FALSE, m.get("overlayObserve"));
        assertFalse("batchArmed probe field removed in Slice E1", m.containsKey("batchArmed"));
        assertFalse("clipEmpty probe field removed in Slice E1", m.containsKey("clipEmpty"));
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

    @Test
    public void fullMountedSkeletonDrawsAndSuppressesNative() {
        combatFrameMounted();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SKELETON).mount();
        FullPresentMode.setSkeletonLevel(PresentLevel.FULL);
        SurfaceDrawPlan.Entry skeleton = Sts1RenderPipeline.plan().find(SurfaceIds.SKELETON);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, skeleton.mode);
        // Skeleton surface is now a normal C2 full-present surface. Per-instance suppression is
        // still handled by SkeletonRenderPatches for claimed skeletons.
        assertTrue(skeleton.suppressNative);
        assertTrue(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.SKELETON));
    }

    @Test
    public void fullMountedEventDrawsAndSuppressesNative() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(1L, 1L, "event", null, ControlsView.empty(), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.EVENT).action("mount_event");
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, plan.find(SurfaceIds.EVENT).mode);
        assertTrue(plan.shouldSuppressNative(SurfaceIds.EVENT));
    }
}
