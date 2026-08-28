package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
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
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.VisibilityComponent;
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

public class RewardDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void publishRewardFrame() {
        publishRewardFrame("combat");
    }

    private void publishRewardFrame(String kind) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        RewardView reward = RewardView.of(kind, "Victory!",
                Arrays.asList(
                        new RewardItemView(0, "gold", "30 Gold", "ui.reward.gold",
                                true, true, 410f, 520f, 180f, 44f),
                        new RewardItemView(1, "relic", "Pen Nib", "relic.Pen Nib",
                                true, false, 410f, 462f, 220f, 48f)));
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "reward", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), reward, RestView.empty(),
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void buildFromProjectionListsRewardItems() {
        publishRewardFrame();
        assertEquals(2, RewardDrawPath.buildFromProjection().size());
        List<RewardDrawPath.DrawItem> items = RewardDrawPath.buildFromProjection();
        assertEquals(0, items.get(0).index);
        assertEquals("gold", items.get(0).kind);
        assertEquals("30 Gold", items.get(0).label);
        assertEquals(ResourceIds.UI_REWARD_GOLD, items.get(0).resourceId);
        assertTrue(items.get(0).enabled);
        assertEquals(320f, items.get(0).x - items.get(0).w / 2f, 0.01f);
        assertEquals(498f, items.get(0).y - items.get(0).h / 2f, 0.01f);
        assertEquals(1, items.get(1).index);
        assertEquals("relic", items.get(1).kind);
        assertEquals("Pen Nib", items.get(1).label);
        assertEquals(ResourceIds.UI_REWARD_DISABLED, items.get(1).resourceId);
        assertFalse("disabled projected rewards must keep enabled=false", items.get(1).enabled);
        Map<String, Object> probe = RewardDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("count"));
        assertEquals("Victory!", probe.get("title"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeReward"));
    }

    @Test
    public void rewardFallbacksCoverCommonKindsAndVisibilityEvidence() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        RewardView reward = RewardView.of("boss_relic", "Choose", Arrays.asList(
                RewardItemView.of(0, "gold", "Gold"),
                RewardItemView.of(1, "card", "Card"),
                RewardItemView.of(2, "relic", "Relic"),
                RewardItemView.of(3, "boss_relic", "Boss Relic"),
                new RewardItemView(4, "gold", "Hidden", "", false, true, 0f, 0f, 0f, 0f)));
        backend.publish(ContextFrame.ofFull(
                1L, 1L, "reward", null, ControlsView.empty(), MapView.empty(),
                EventView.empty(), SelectView.empty(), reward, RestView.empty(), TreasureView.empty(),
                ShopView.empty(), TopPanelView.empty(), MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());

        List<RewardDrawPath.DrawItem> items = RewardDrawPath.buildFromProjection();
        assertEquals(ResourceIds.UI_REWARD_GOLD, items.get(0).resourceId);
        assertEquals(ResourceIds.UI_REWARD_CARD, items.get(1).resourceId);
        assertEquals(ResourceIds.UI_REWARD_RELIC, items.get(2).resourceId);
        assertEquals(ResourceIds.UI_REWARD_BOSS_RELIC, items.get(3).resourceId);
        assertFalse(items.get(4).visible);
        assertEquals("probe count must come from visible draw items",
                Integer.valueOf(4), RewardDrawPath.probeSlice().get("drawCount"));
    }

    @Test
    public void fullMountedRewardDrawsAndSuppressesNative() {
        publishRewardFrame();
        FullPresentMode.setRewardLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.REWARD_COMBAT).mount();
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.REWARD_COMBAT));
        assertTrue(plan.shouldDraw(SurfaceIds.REWARD_CARD));
        assertTrue(plan.shouldDraw(SurfaceIds.REWARD_BOSS_RELIC));
        assertTrue(plan.shouldSuppressNative(SurfaceIds.REWARD_COMBAT));
        assertTrue(plan.shouldSuppressNative(SurfaceIds.REWARD_CARD));
        assertTrue(plan.shouldSuppressNative(SurfaceIds.REWARD_BOSS_RELIC));
        assertTrue(RewardDrawPath.shouldSuppressNativeReward());
        assertEquals(Boolean.TRUE, RewardDrawPath.probeSlice().get("suppressNativeReward"));
    }

    @Test
    public void rewardVisualsMaterializePerRewardSurfaceWithProjectedIdentityResourceAndBounds()
            throws Exception {
        assertRewardVisualsForSurface(SurfaceIds.REWARD_COMBAT, "combat");
        assertRewardVisualsForSurface(SurfaceIds.REWARD_CARD, "card");
        assertRewardVisualsForSurface(SurfaceIds.REWARD_BOSS_RELIC, "boss_relic");
    }

    @Test
    public void rewardEvidenceRecordsRealProjectedItemCountForEachRewardSurface() {
        publishRewardFrame("card");
        FullPresentMode.setRewardLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.REWARD_CARD).mount();

        assertRewardEvidenceCountComesFromProjection(SurfaceIds.REWARD_COMBAT);
        assertRewardEvidenceCountComesFromProjection(SurfaceIds.REWARD_CARD);
        assertRewardEvidenceCountComesFromProjection(SurfaceIds.REWARD_BOSS_RELIC);
    }

    @Test
    public void offDoesNotSuppress() {
        publishRewardFrame();
        ArtFramework.component(SurfaceIds.REWARD_COMBAT).mount();
        assertFalse(RewardDrawPath.shouldSuppressNativeReward());
    }

    @SuppressWarnings("unchecked")
    private static void assertRewardVisualsForSurface(String surfaceId, String kind) throws Exception {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        RewardDrawPathTest test = new RewardDrawPathTest();
        test.publishRewardFrame(kind);
        FullPresentMode.setRewardLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(surfaceId));

        java.lang.reflect.Method prepare = Sts1SurfaceRenderer.class.getDeclaredMethod(
                "prepareRewardVisuals", SurfaceDrawPlan.class);
        prepare.setAccessible(true);
        prepare.invoke(null, plan);

        Map<String, Object> probe = RewardDrawPath.probeSlice();
        assertEquals(kind, probe.get("kind"));
        assertEquals("projected drawCount must be item-derived, not a constant",
                probe.get("count"), probe.get("drawCount"));
        List<Map<String, Object>> projected = (List<Map<String, Object>>) probe.get("items");
        assertEquals(Integer.valueOf(2), probe.get("drawCount"));

        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        String scope = "sts1.visual." + surfaceId;
        for (Map<String, Object> expected : projected) {
            String localId = "reward:" + expected.get("index");
            EntityId entity = null;
            for (EntityId candidate : context.entities()) {
                NodeIdentityComponent identity = context.world().get(candidate, NodeIdentityComponent.class);
                if (identity != null && scope.equals(identity.key.scope)
                        && localId.equals(identity.key.localId)) {
                    entity = candidate;
                    break;
                }
            }
            assertTrue("missing reward C2 visual " + localId + " for " + surfaceId, entity != null);
            DrawComponent draw = context.world().get(entity, DrawComponent.class);
            assertEquals("reward-item", draw.role);
            assertEquals(expected.get("resourceId"), draw.resourceId);
            assertEquals(expected.get("label"), draw.text);
            BoundsComponent bounds = context.world().get(entity, BoundsComponent.class);
            assertEquals(((Float) expected.get("x")).floatValue()
                    - ((Float) expected.get("w")).floatValue() / 2f, bounds.rect.x, 0.01f);
            assertEquals(((Float) expected.get("y")).floatValue()
                    - ((Float) expected.get("h")).floatValue() / 2f, bounds.rect.y, 0.01f);
            assertEquals(((Float) expected.get("w")).floatValue(), bounds.rect.width, 0.01f);
            assertEquals(((Float) expected.get("h")).floatValue(), bounds.rect.height, 0.01f);
            VisibilityComponent visibility = context.world().get(entity, VisibilityComponent.class);
            assertTrue(visibility.visible);
        }
    }

    private static void assertRewardEvidenceCountComesFromProjection(String surfaceId) {
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                surfaceId, "native.Reward", "render", "test");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        int projectedItems = ((Integer) RewardDrawPath.probeSlice().get("drawCount")).intValue();
        assertTrue("test fixture must prove a non-constant projected item count", projectedItems > 1);
        NativeRenderBridge.recordSurfaceDraw(surfaceId, projectedItems);
        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertEquals(projectedItems, evidence.drawCount);
    }
}
