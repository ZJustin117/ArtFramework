package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.MonsterIntentView;
import artframework.context.RestView;
import artframework.context.RewardView;
import artframework.context.SelectView;
import artframework.context.ShopView;
import artframework.context.SurfaceIds;
import artframework.context.TopPanelView;
import artframework.context.TreasureView;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.PresentSafety;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.patch.CombatControlsRenderPatches;
import artframework.sts1.patch.CombatEnergyRenderPatches;
import artframework.sts1.patch.CombatIntentRenderPatches;
import artframework.sts1.patch.ProceedButtonRenderPatches;
import artframework.sts1.patch.TopPanelRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Dynamic NRCC evidence matrix for delegated combat HUD C2 surfaces. */
public class CombatHudSurfaceCoverageTest {

    private static final String[] SURFACES = {
            SurfaceIds.COMBAT_CONTROLS,
            SurfaceIds.COMBAT_ENERGY,
            SurfaceIds.COMBAT_INTENTS,
            SurfaceIds.COMBAT_PROCEED,
            SurfaceIds.TOP_PANEL
    };

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    @Test
    public void fullReadySuppressesEveryCombatHudSurface() {
        for (String surfaceId : SURFACES) {
            resetRuntime();
            publishCombatFrame("combat");
            mount(surfaceId);
            setFull(surfaceId);
            CombatInputRouter.setExecutor(new RecordingIntentExecutor());

            SpireReturn<Void> result = prefix(surfaceId);

            assertTrue(surfaceId + " FULL_READY must suppress native render", result.isPresent());
            assertEquals(surfaceId + " must delegate exactly once",
                    Integer.valueOf(1), NativeRenderBridge.probeSlice().get("delegateToArt"));
        }
    }

    @Test
    public void nonReadyStatesContinueNativeAndDoNotCountDelegatedCoverage() {
        for (String surfaceId : SURFACES) {
            assertContinuesWithoutDelegation(surfaceId, Scenario.OFF);
            assertContinuesWithoutDelegation(surfaceId, Scenario.EXECUTORLESS);
            assertContinuesWithoutDelegation(surfaceId, Scenario.UNMOUNTED);
            assertContinuesWithoutDelegation(surfaceId, Scenario.SCENE_MISMATCH);
        }
    }

    @Test
    public void panicAndUnknownOwnerFailOpenWithoutDelegatedCoverage() {
        for (String surfaceId : SURFACES) {
            resetRuntime();
            publishCombatFrame("combat");
            mount(surfaceId);
            setFull(surfaceId);
            CombatInputRouter.setExecutor(new RecordingIntentExecutor());
            PresentSafety.panic("combat-hud-test");

            SpireReturn<Void> result = prefix(surfaceId);

            assertFalse(surfaceId + " panic must fail open to native", result.isPresent());
            assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("failOpen"));
            assertNoDelegatedCoverage(surfaceId);

            resetRuntime();
            publishCombatFrame("combat");
            mount(surfaceId);
            setFull(surfaceId);
            CombatInputRouter.setExecutor(new RecordingIntentExecutor());

            RenderDisposition unknown = NativeRenderBridge.beginSurface(
                    surfaceId + ".unknown", "native.Unknown", "render", "unknown-owner");

            assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
            assertTrue(unknown.nativeContinuation);
            assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("runtimeUNKNOWN"));
            assertNoDelegatedCoverage(surfaceId);
        }
    }

    @Test
    public void c2MaterializationUsesLiveProjectionFieldsForEveryCombatHudSurface() {
        publishCombatFrame("combat");
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        for (String surfaceId : SURFACES) {
            mount(surfaceId);
            setFull(surfaceId);
        }
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();

        invokePrepare("prepareControlsVisuals", plan);
        invokePrepare("prepareEnergyVisuals", plan);
        invokePrepare("prepareIntentVisuals", plan);
        invokePrepare("prepareProceedVisuals", plan);
        invokePrepare("prepareTopPanelVisuals", plan);

        Map<String, DrawComponent> controls = c2Draws(SurfaceIds.COMBAT_CONTROLS);
        assertEquals(1, controls.size());
        assertEquals("结束回合", controls.get(ControlsView.END_TURN_ID).text);
        Rect endTurn = c2Bounds(SurfaceIds.COMBAT_CONTROLS).get(ControlsView.END_TURN_ID).rect;
        assertEquals(111f, endTurn.x, 0.01f);
        assertEquals(222f, endTurn.y, 0.01f);
        assertEquals(333f, endTurn.width, 0.01f);
        assertEquals(44f, endTurn.height, 0.01f);

        Map<String, DrawComponent> energy = c2Draws(SurfaceIds.COMBAT_ENERGY);
        assertEquals(Integer.valueOf(EnergyDrawPath.buildFromProjection().size()),
                Integer.valueOf(energy.size()));
        assertEquals("7", energy.get("energy_orb").text);

        Map<String, DrawComponent> intents = c2Draws(SurfaceIds.COMBAT_INTENTS);
        assertEquals(Integer.valueOf(IntentDrawPath.buildFromProjection().size()),
                Integer.valueOf(intents.size()));
        assertEquals("3", intents.get("intent:m1").text);
        assertEquals("2", intents.get("intent:m2").text);
        assertEquals("ui.intent.attack.attack_intent_3", intents.get("intent:m1").resourceId);
        Rect intentBounds = c2Bounds(SurfaceIds.COMBAT_INTENTS).get("intent:m1").rect;
        assertEquals(168f, intentBounds.x, 0.01f);
        assertEquals(268f, intentBounds.y, 0.01f);

        Map<String, DrawComponent> proceed = c2Draws(SurfaceIds.COMBAT_PROCEED);
        assertEquals(Integer.valueOf(ProceedDrawPath.buildFromProjection().size()),
                Integer.valueOf(proceed.size()));
        assertEquals("Proceed", proceed.get(ControlsView.PROCEED_ID).text);
        assertEquals("Cancel", proceed.get(ControlsView.CANCEL_ID).text);

        Map<String, DrawComponent> topPanel = c2Draws(SurfaceIds.TOP_PANEL);
        assertEquals(Integer.valueOf(TopPanelDrawPath.buildFromProjection().size()),
                Integer.valueOf(topPanel.size()));
        assertEquals("Ironclad  HP 66/80  Gold 123  Floor 9  A17",
                topPanel.get("top_panel_hud").text);
    }

    @Test
    public void evidenceDrawCountsComeFromCurrentProjectionNotDelegatedConstants() {
        publishCombatFrame("combat");
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        for (String surfaceId : SURFACES) {
            mount(surfaceId);
            setFull(surfaceId);
        }

        assertEvidenceCount(SurfaceIds.COMBAT_CONTROLS,
                ControlsDrawPath.materializedDrawCount(), "renderControls");
        assertTrue("controls projection includes non-controls entries, proving coverage is not raw list size",
                ControlsDrawPath.buildFromProjection().size() > ControlsDrawPath.materializedDrawCount());
        assertEvidenceCount(SurfaceIds.COMBAT_ENERGY,
                EnergyDrawPath.buildFromProjection().size(), "renderEnergy");
        assertEvidenceCount(SurfaceIds.COMBAT_INTENTS,
                IntentDrawPath.buildFromProjection().size(), "renderIntents");
        assertEvidenceCount(SurfaceIds.COMBAT_PROCEED,
                ProceedDrawPath.buildFromProjection().size(), "renderProceed");
        assertTrue("proceed evidence must vary with projected proceed/cancel rows",
                ProceedDrawPath.buildFromProjection().size() > 1);
        assertEvidenceCount(SurfaceIds.TOP_PANEL,
                TopPanelDrawPath.buildFromProjection().size(), "renderTopPanel");
    }

    @Test
    public void sceneAndHostRecoveryClearStaleHudItemsAndEvidence() {
        publishCombatFrame("combat");
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        for (String surfaceId : SURFACES) {
            mount(surfaceId);
            setFull(surfaceId);
        }
        SurfaceDrawPlan combatPlan = Sts1RenderPipeline.plan();
        invokePrepare("prepareControlsVisuals", combatPlan);
        invokePrepare("prepareEnergyVisuals", combatPlan);
        invokePrepare("prepareIntentVisuals", combatPlan);
        invokePrepare("prepareProceedVisuals", combatPlan);
        invokePrepare("prepareTopPanelVisuals", combatPlan);
        assertFalse(c2Draws(SurfaceIds.COMBAT_CONTROLS).isEmpty());
        assertFalse(c2Draws(SurfaceIds.COMBAT_ENERGY).isEmpty());
        assertFalse(c2Draws(SurfaceIds.COMBAT_INTENTS).isEmpty());
        assertFalse(c2Draws(SurfaceIds.COMBAT_PROCEED).isEmpty());
        assertFalse(c2Draws(SurfaceIds.TOP_PANEL).isEmpty());

        publishCombatFrame("unknown");
        invokePrivate("disableInactiveSurfaceEffects",
                new Class<?>[] {SurfaceDrawPlan.class}, Sts1RenderPipeline.plan());
        for (String surfaceId : SURFACES) {
            assertTrue(surfaceId + " stale C2 items must be removed on scene transition",
                    c2Draws(surfaceId).isEmpty());
        }

        publishCombatFrame("combat");
        mount(SurfaceIds.COMBAT_INTENTS);
        setFull(SurfaceIds.COMBAT_INTENTS);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "native.Intent", "render", "intent");
        invokeRender("renderIntents");
        assertNotNull(NativeRenderBridge.ledger().evidence(disposition.invocationId));

        PresentSafety.onHostRecreated();
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("evidenceCount"));
        for (String surfaceId : SURFACES) {
            assertTrue(surfaceId + " stale C2 items must be removed on host recreation",
                    c2Draws(surfaceId).isEmpty());
        }
    }

    private static void assertContinuesWithoutDelegation(String surfaceId, Scenario scenario) {
        resetRuntime();
        publishCombatFrame(scenario == Scenario.SCENE_MISMATCH ? "unknown" : "combat");
        if (scenario != Scenario.UNMOUNTED) {
            mount(surfaceId);
        }
        if (scenario != Scenario.OFF) {
            setFull(surfaceId);
        }
        if (scenario != Scenario.EXECUTORLESS) {
            CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        }

        SpireReturn<Void> result = prefix(surfaceId);

        assertFalse(surfaceId + " " + scenario + " must continue native render", result.isPresent());
        assertNoDelegatedCoverage(surfaceId);
    }

    private static void assertEvidenceCount(String surfaceId, int expected, String renderMethod) {
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                surfaceId, "native." + surfaceId, "render", surfaceId);
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);

        invokeRender(renderMethod);

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(surfaceId + " must record evidence", evidence);
        assertEquals(surfaceId + " evidence count must come from live projection",
                expected, evidence.drawCount);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    private static void publishCombatFrame(String scene) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        ControlsView controls = ControlsView.combatWithProceed(
                7, 4, 12, 5, 1,
                true, true,
                true, true,
                false, true,
                111f, 222f, 333f, 44f,
                "结束回合");
        MonsterIntentView intents = MonsterIntentView.of(Arrays.asList(
                new MonsterIntentView.IntentEntry(
                        "m1", "Cultist", "ATTACK", "ui.intent.attack.attack_intent_3",
                        3, 200f, 300f),
                new MonsterIntentView.IntentEntry(
                        "m2", "Jaw Worm", "ATTACK_BUFF", "ui.intent.attack_buff.attack_buff_intent_2",
                        2, 420f, 340f)));
        backend.publish(ContextFrame.ofFull(
                1L, 1L, scene,
                Collections.<artframework.context.CardView>emptyList(),
                controls,
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                RewardView.empty(),
                RestView.empty(),
                TreasureView.empty(),
                ShopView.empty(),
                TopPanelView.of(66, 80, 123, 9, 17, "Ironclad"),
                intents,
                null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private static SpireReturn<Void> prefix(String surfaceId) {
        if (SurfaceIds.COMBAT_CONTROLS.equals(surfaceId)) {
            return CombatControlsRenderPatches.ObserveNativeEndTurnRender.Prefix(null, null);
        }
        if (SurfaceIds.COMBAT_ENERGY.equals(surfaceId)) {
            return CombatEnergyRenderPatches.ObserveNativeEnergyRender.Prefix(null, null);
        }
        if (SurfaceIds.COMBAT_INTENTS.equals(surfaceId)) {
            return CombatIntentRenderPatches.ObserveNativeIntent.Prefix(null, null);
        }
        if (SurfaceIds.COMBAT_PROCEED.equals(surfaceId)) {
            return ProceedButtonRenderPatches.ObserveNativeProceedButtonRender.Prefix(null, null);
        }
        if (SurfaceIds.TOP_PANEL.equals(surfaceId)) {
            return TopPanelRenderPatches.ObserveNativeTopPanelRender.Prefix(null, null);
        }
        throw new AssertionError("unsupported surface " + surfaceId);
    }

    private static void setFull(String surfaceId) {
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
    }

    private static void mount(String surfaceId) {
        ArtFramework.component(surfaceId).mount();
    }

    private static void resetRuntime() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private static void assertNoDelegatedCoverage(String surfaceId) {
        Map<String, Object> probe = NativeRenderBridge.probeSlice();
        assertEquals(surfaceId + " must not delegate", Integer.valueOf(0), probe.get("delegateToArt"));
        assertEquals(surfaceId + " must not record evidence", Integer.valueOf(0), probe.get("evidenceCount"));
        assertEquals(surfaceId + " must not leave delegated evidence gaps",
                Integer.valueOf(0), probe.get("delegatedWithoutEvidence"));
    }

    private static void invokePrepare(String name, SurfaceDrawPlan plan) {
        invokePrivate(name, new Class<?>[] {SurfaceDrawPlan.class}, plan);
    }

    private static void invokeRender(String name) {
        invokePrivate(name,
                new Class<?>[] {com.badlogic.gdx.graphics.g2d.SpriteBatch.class},
                new Object[] {null});
    }

    private static void invokePrivate(String name, Class<?>[] types, Object... args) {
        try {
            Method method = Sts1SurfaceRenderer.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(null, args);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Map<String, DrawComponent> c2Draws(String surfaceId) {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        String scope = "sts1.visual." + surfaceId;
        Set<String> duplicateGuard = new LinkedHashSet<String>();
        Map<String, DrawComponent> out = new LinkedHashMap<String, DrawComponent>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            if (identity == null || !scope.equals(identity.key.scope)) continue;
            assertTrue("duplicate C2 item id " + identity.key.localId,
                    duplicateGuard.add(identity.key.localId));
            out.put(identity.key.localId, context.world().get(entity, DrawComponent.class));
        }
        return out;
    }

    private static Map<String, BoundsComponent> c2Bounds(String surfaceId) {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        String scope = "sts1.visual." + surfaceId;
        Map<String, BoundsComponent> out = new LinkedHashMap<String, BoundsComponent>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            if (identity == null || !scope.equals(identity.key.scope)) continue;
            out.put(identity.key.localId, context.world().get(entity, BoundsComponent.class));
        }
        return out;
    }

    private enum Scenario {
        OFF,
        EXECUTORLESS,
        UNMOUNTED,
        SCENE_MISMATCH
    }
}
