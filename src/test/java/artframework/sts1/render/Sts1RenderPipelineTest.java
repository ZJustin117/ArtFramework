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
    public void fullMountedIntentsDrawsWithoutSuppressingNative() {
        combatFrameMounted();
        ArtFramework.component(SurfaceIds.COMBAT_INTENTS).mount();
        FullPresentMode.setIntentsLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry intents = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_INTENTS);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, intents.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_INTENTS));
        // NRO-02: intent pixels stay owned by native AbstractMonster.renderIntent.
        assertFalse(intents.suppressNative);
        assertFalse(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_INTENTS));
    }

    @Test
    public void fullMountedControlsDrawWithoutSuppressingNative() {
        combatFrameMounted();
        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry controls = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_CONTROLS);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, controls.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_CONTROLS));
        // NRO-02: end-turn pixels stay owned by native EndTurnButton.render.
        assertFalse(controls.suppressNative);
        assertFalse(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_CONTROLS));
    }

    @Test
    public void fullMountedEnergyDrawWithoutSuppressingNative() {
        combatFrameMounted();
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan.Entry energy = Sts1RenderPipeline.plan().find(SurfaceIds.COMBAT_ENERGY);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, energy.mode);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_ENERGY));
        // NRO-02: energy orb pixels stay owned by native EnergyPanel.render.
        assertFalse(energy.suppressNative);
        assertFalse(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_ENERGY));
    }

    @Test
    public void mapEventSelectRoomSurfacesDrawWithoutSuppressingNative() {
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
                    // NRO-03: these surfaces never suppress their native renderers.
                    assertFalse("surface " + surfaceId + " in scene " + scene + " must not suppress native",
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

    @Test
    public void fullMountedSkeletonDrawsWithoutSuppressingNative() {
        combatFrameMounted();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SKELETON).mount();
        FullPresentMode.setSkeletonLevel(PresentLevel.FULL);
        SurfaceDrawPlan.Entry skeleton = Sts1RenderPipeline.plan().find(SurfaceIds.SKELETON);
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, skeleton.mode);
        // NRO-04: skeleton suppression is per-instance via the patch; the surface plan never
        // claims wholesale native-pixel authority for the skeleton surface.
        assertFalse(skeleton.suppressNative);
        assertFalse(Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.SKELETON));
    }

    @Test
    public void fullMountedEventDrawsWithoutSuppressingNative() {
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
        // NRO-03: native GenericEventDialog.render stays the visual authority.
        assertFalse(plan.shouldSuppressNative(SurfaceIds.EVENT));
    }
}
