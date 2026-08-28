package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.context.CardPose;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapNodeView;
import artframework.context.MapView;
import artframework.context.MonsterIntentView;
import artframework.context.RestView;
import artframework.context.RewardItemView;
import artframework.context.RewardView;
import artframework.context.SelectView;
import artframework.context.ShopView;
import artframework.context.SurfaceIds;
import artframework.context.TopPanelView;
import artframework.context.TreasureView;
import artframework.ecs.EntityId;
import artframework.presentation.DrawComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.PresentationVisuals;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.PresentSafety;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Slice 8 lifecycle matrix for whole-surface native render ledger and C2 visual cleanup. */
public class NativeRenderLifecycleCleanupMatrixTest {

    private static final SurfaceCase[] SURFACES = new SurfaceCase[] {
            c(SurfaceIds.COMBAT_HAND, "combat", false),
            c(SurfaceIds.COMBAT_CONTROLS, "combat", true),
            c(SurfaceIds.COMBAT_ENERGY, "combat", false),
            c(SurfaceIds.COMBAT_INTENTS, "combat", true),
            c(SurfaceIds.COMBAT_PROCEED, "combat", false),
            c(SurfaceIds.TOP_PANEL, "combat", true),
            c(SurfaceIds.MAP, "map", false),
            c(SurfaceIds.EVENT, "event", true),
            c(SurfaceIds.SELECT_GRID, "select", false),
            c(SurfaceIds.SELECT_HAND, "select", true),
            c(SurfaceIds.REWARD_COMBAT, "reward", false),
            c(SurfaceIds.REWARD_CARD, "reward", true),
            c(SurfaceIds.REWARD_BOSS_RELIC, "reward", false),
            c(SurfaceIds.REST, "rest", true),
            c(SurfaceIds.SHOP, "shop", false),
            c(SurfaceIds.TREASURE, "treasure", true)
    };

    @After
    public void tearDown() {
        resetRuntime();
    }

    @Test
    public void hostRecreationClearsLedgerAndC2VisualsForAllDelegatedSurfaces() {
        for (SurfaceCase sc : SURFACES) {
            resetRuntime();
            publishFrame(sc.scene);
            armFullReady(sc.surfaceId);
            PresentationVisuals.syncC2Item(sc.surfaceId, "stale", Rect.ZERO, 1f,
                    "test", "", sc.surfaceId, true);

            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    sc.surfaceId, "native." + sc.surfaceId, "render", "matrix");
            assertEquals(sc.surfaceId + " must create a delegated invocation",
                    RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
            assertFalse(disposition.nativeContinuation);

            if (sc.recordEvidence) {
                NativeRenderBridge.recordSurfaceDraw(disposition.invocationId, 1);
                assertNotNull(sc.surfaceId + " must record correlated evidence",
                        NativeRenderBridge.ledger().evidence(disposition.invocationId));
                assertEquals(Integer.valueOf(0),
                        NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
            } else {
                assertEquals(sc.surfaceId + " pending delegation must be visible pre-cleanup",
                        Integer.valueOf(1), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
            }
            assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
            assertFalse(sc.surfaceId + " fixture must have a stale C2 visual",
                    c2Draws(sc.surfaceId).isEmpty());

            PresentSafety.onHostRecreated();

            assertLedgerReset(sc.surfaceId);
            assertTrue(sc.surfaceId + " C2 visuals must not survive host recreation",
                    c2Draws(sc.surfaceId).isEmpty());
        }
    }

    @Test
    public void sceneTransitionRemovesVisualsForEverySurfaceInUnifiedCleanupList() {
        for (SurfaceCase sc : SURFACES) {
            resetRuntime();
            publishFrame(sc.scene);
            armFullReady(sc.surfaceId);
            PresentationVisuals.syncC2Item(sc.surfaceId, "stale", Rect.ZERO, 1f,
                    "test", "", sc.surfaceId, true);
            assertFalse(c2Draws(sc.surfaceId).isEmpty());

            publishFrame("unknown");
            invokeDisableInactiveSurfaceEffects(Sts1RenderPipeline.plan());

            assertTrue(sc.surfaceId + " stale C2 visuals must clear on scene transition",
                    c2Draws(sc.surfaceId).isEmpty());
        }
    }

    @Test
    public void panicCleanupClearsVisualsAndOldSurfaceQueueDoesNotPolluteNextEvidence() {
        resetRuntime();
        publishFrame("combat");
        armFullReady(SurfaceIds.COMBAT_CONTROLS);
        PresentationVisuals.syncC2Item(SurfaceIds.COMBAT_CONTROLS, "stale", Rect.ZERO, 1f,
                "test", "", "old", true);
        RenderDisposition old = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_CONTROLS, "native.Controls", "render", "old");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, old.mode);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));

        PresentSafety.panic("matrix-panic");

        assertTrue(PresentSafety.isPanic());
        assertTrue("panic must clear C2 visuals for delegated surfaces",
                c2Draws(SurfaceIds.COMBAT_CONTROLS).isEmpty());
        PresentSafety.clearPanic();
        publishFrame("combat");
        armFullReady(SurfaceIds.COMBAT_CONTROLS);
        RenderDisposition fresh = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_CONTROLS, "native.Controls", "render", "fresh");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, fresh.mode);

        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_CONTROLS, 2);

        assertNotNull("fresh evidence must correlate to the fresh invocation",
                NativeRenderBridge.ledger().evidence(fresh.invocationId));
        assertEquals(2, NativeRenderBridge.ledger().evidence(fresh.invocationId).drawCount);
        assertTrue("old pending invocation must not receive fresh surface evidence",
                NativeRenderBridge.ledger().evidence(old.invocationId) == null);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    private static void assertLedgerReset(String surfaceId) {
        Map<String, Object> probe = NativeRenderBridge.probeSlice();
        assertEquals(surfaceId + " invocationCount", Integer.valueOf(0), probe.get("invocationCount"));
        assertEquals(surfaceId + " evidenceCount", Integer.valueOf(0), probe.get("evidenceCount"));
        assertEquals(surfaceId + " delegatedWithoutEvidence", Integer.valueOf(0),
                probe.get("delegatedWithoutEvidence"));
        assertEquals(surfaceId + " orphanArtOutput", Integer.valueOf(0), probe.get("orphanArtOutput"));
        Map<String, Object> strict = NativeRenderBridge.strictReport();
        assertEquals(surfaceId + " runtimeUNKNOWN", Integer.valueOf(0), strict.get("runtimeUNKNOWN"));
        assertEquals(surfaceId + " openInvocation", Integer.valueOf(0), strict.get("openInvocation"));
        assertEquals(surfaceId + " dispositionMismatch", Integer.valueOf(0), strict.get("dispositionMismatch"));
        assertEquals(surfaceId + " recoveryFailOpen", Integer.valueOf(0), strict.get("recoveryFailOpen"));
        assertEquals(surfaceId + " unrecordedFAIL_OPEN", Integer.valueOf(0), strict.get("unrecordedFAIL_OPEN"));
    }

    private static void armFullReady(String surfaceId) {
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
    }

    private static void publishFrame(String scene) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        List<CardView> cards = Collections.singletonList(
                CardView.builder(new CardRef("h1", "Strike_R"))
                        .zone(CardZone.HAND)
                        .pose(CardPose.at(300f, 200f))
                        .build());
        MapView map = new MapView(Collections.singletonList(
                new MapNodeView(1, 1, 100f, 200f, false, true,
                        "M", "monster", artframework.assets.ResourceIds.MAP_NODE_MONSTER)),
                1920, 1080);
        EventView event = EventView.of("Neow", Arrays.asList(
                EventOptionView.of(0, "Talk", true),
                EventOptionView.of(1, "Leave", true)));
        SelectView select = SelectView.grid(
                Collections.singletonList(
                        CardView.builder(new CardRef("g1", "Strike_R"))
                                .zone(CardZone.SELECT)
                                .slot(0)
                                .build()),
                Collections.singletonList("g1"), true, true);
        RewardView reward = RewardView.of("combat", "Victory!", Arrays.asList(
                new RewardItemView(0, "gold", "30 Gold", "ui.reward.gold",
                        true, true, 410f, 520f, 180f, 44f),
                RewardItemView.of(1, "card", "Strike_R")));
        RestView rest = RestView.of(Collections.singletonList(
                new RestView.RestOptionView("rest", "Rest", true, true)));
        ShopView shop = ShopView.of(99, Collections.singletonList(
                ShopView.ShopEntryView.of(0, "card", "Strike_R", 50)), false, 0);
        backend.publish(ContextFrame.ofFull(
                1L, 1L, scene, cards,
                ControlsView.combatWithProceed(3, 2, 12, 5, 1,
                        true, true, true, true, false, true,
                        111f, 222f, 333f, 44f, "End Turn"),
                map, event, select, reward, rest, TreasureView.closed(), shop,
                TopPanelView.of(66, 80, 123, 9, 17, "Ironclad"),
                MonsterIntentView.of(Collections.singletonList(
                        new MonsterIntentView.IntentEntry("m1", "Cultist", "ATTACK",
                                "ui.intent.attack.attack_intent_3", 3, 200f, 300f))),
                null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private static void invokeDisableInactiveSurfaceEffects(SurfaceDrawPlan plan) {
        try {
            Method method = Sts1SurfaceRenderer.class.getDeclaredMethod(
                    "disableInactiveSurfaceEffects", SurfaceDrawPlan.class);
            method.setAccessible(true);
            method.invoke(null, plan);
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

    private static void resetRuntime() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private static SurfaceCase c(String surfaceId, String scene, boolean evidence) {
        return new SurfaceCase(surfaceId, scene, evidence);
    }

    private static final class SurfaceCase {
        final String surfaceId;
        final String scene;
        final boolean recordEvidence;

        SurfaceCase(String surfaceId, String scene, boolean recordEvidence) {
            this.surfaceId = surfaceId;
            this.scene = scene;
            this.recordEvidence = recordEvidence;
        }
    }
}
