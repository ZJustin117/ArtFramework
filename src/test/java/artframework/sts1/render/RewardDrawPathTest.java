package artframework.sts1.render;

import artframework.api.ArtFramework;
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

public class RewardDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void publishRewardFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        RewardView reward = RewardView.of("combat", "Victory!",
                Arrays.asList(
                        RewardItemView.of(0, "gold", "30 Gold"),
                        RewardItemView.of(1, "relic", "Pen Nib")));
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
        Map<String, Object> probe = RewardDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("count"));
        assertEquals("Victory!", probe.get("title"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeReward"));
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
    public void offDoesNotSuppress() {
        publishRewardFrame();
        ArtFramework.component(SurfaceIds.REWARD_COMBAT).mount();
        assertFalse(RewardDrawPath.shouldSuppressNativeReward());
    }
}
