package artframework.sts1.input;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.RewardItemView;
import artframework.context.RewardView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentCapability;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.render.RewardDrawPath;
import artframework.sts1.render.Sts1RenderPipeline;
import artframework.sts1.render.SurfaceDrawPlan;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Milestone 26: room surfaces need matching scene for FULL_READY (not mount-only). */
public class RoomPresentCapabilityTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        CombatInputRouter.resetForTests();
        FullPresentMode.resetForTests();
        Sts1RenderPipeline.resetForTests();
    }

    private void publishRewardScene() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        List<RewardItemView> items = new ArrayList<RewardItemView>();
        items.add(RewardItemView.of(0, "gold", "Gold 25"));
        backend.publish(
                ContextFrame.ofFull(
                        1L,
                        1L,
                        "reward",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        artframework.context.EventView.empty(),
                        artframework.context.SelectView.empty(),
                        RewardView.of("combat", "Rewards", items),
                        artframework.context.RestView.empty(),
                        artframework.context.TreasureView.empty(),
                        artframework.context.ShopView.empty(),
                        artframework.context.TopPanelView.empty(),
                        artframework.context.MonsterIntentView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private void publishCombatScene() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(1L, 1L, "combat", null, ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void rewardMountWithoutRewardSceneFallsBack() {
        publishCombatScene();
        FullPresentMode.setRewardLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.REWARD_COMBAT).action("mount_reward");
        FullPresentCapability cap = CombatInputRouter.capability(SurfaceIds.REWARD_COMBAT);
        assertEquals(FullPresentCapability.State.FULL_FALLBACK_NATIVE, cap.state);
        assertEquals("scene_unavailable", cap.reason);
        assertFalse(RewardDrawPath.shouldSuppressNativeReward());
        assertFalse(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.REWARD_COMBAT));
    }

    @Test
    public void rewardFullReadyWhenSceneMountedExecutor() {
        publishRewardScene();
        FullPresentMode.setRewardLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.REWARD_COMBAT).action("mount_reward");
        FullPresentCapability cap = CombatInputRouter.capability(SurfaceIds.REWARD_COMBAT);
        assertEquals(FullPresentCapability.State.FULL_READY, cap.state);
        assertTrue(cap.shouldSuppressNative());
        assertTrue(RewardDrawPath.shouldSuppressNativeReward());
        assertEquals(SurfaceDrawPlan.DrawMode.DRAW, Sts1RenderPipeline.plan().find(SurfaceIds.REWARD_COMBAT).mode);
        Map<String, Object> probe = RewardDrawPath.probeSlice();
        assertEquals("FULL_READY", probe.get("capability"));
        assertEquals(Boolean.TRUE, probe.get("suppressNativeReward"));
        Map<String, Object> input = CombatInputRouter.probeSlice();
        assertEquals("FULL_READY", input.get("rewardState"));
    }

    @Test
    public void rewardWithoutExecutorFallsBack() {
        publishRewardScene();
        FullPresentMode.setRewardLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.REWARD_COMBAT).action("mount_reward");
        FullPresentCapability cap = CombatInputRouter.capability(SurfaceIds.REWARD_COMBAT);
        assertEquals(FullPresentCapability.State.FULL_FALLBACK_NATIVE, cap.state);
        assertEquals("executor_unavailable", cap.reason);
    }

    @Test
    public void restRequiresRestScene() {
        publishRewardScene();
        FullPresentMode.setRestLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.REST).action("mount_rest");
        assertEquals(
                FullPresentCapability.State.FULL_FALLBACK_NATIVE,
                CombatInputRouter.capability(SurfaceIds.REST).state);
    }

    @Test
    public void energyRequiresCombatScene() {
        publishRewardScene();
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.COMBAT_ENERGY).action("mount_energy");
        assertEquals(
                FullPresentCapability.State.FULL_FALLBACK_NATIVE,
                CombatInputRouter.capability(SurfaceIds.COMBAT_ENERGY).state);
        assertEquals("scene_unavailable", CombatInputRouter.capability(SurfaceIds.COMBAT_ENERGY).reason);
    }

    @Test
    public void energyFullReadyInCombat() {
        publishCombatScene();
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.COMBAT_ENERGY).action("mount_energy");
        assertEquals(
                FullPresentCapability.State.FULL_READY,
                CombatInputRouter.capability(SurfaceIds.COMBAT_ENERGY).state);
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_ENERGY));
    }

    @Test
    public void shopFullReadyInShopScene() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.ofFull(
                        1L,
                        1L,
                        "shop",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        artframework.context.EventView.empty(),
                        artframework.context.SelectView.empty(),
                        artframework.context.RewardView.empty(),
                        artframework.context.RestView.empty(),
                        artframework.context.TreasureView.empty(),
                        artframework.context.ShopView.of(
                                99,
                                java.util.Arrays.asList(
                                        artframework.context.ShopView.ShopEntryView.of(
                                                0, "card", "Strike", 50)),
                                true,
                                75),
                        artframework.context.TopPanelView.empty(),
                        artframework.context.MonsterIntentView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
        FullPresentMode.setShopLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SHOP).action("mount_shop");
        assertEquals(
                FullPresentCapability.State.FULL_READY,
                CombatInputRouter.capability(SurfaceIds.SHOP).state);
        assertTrue(artframework.sts1.render.ShopDrawPath.shouldSuppressNativeShop());
    }
}
