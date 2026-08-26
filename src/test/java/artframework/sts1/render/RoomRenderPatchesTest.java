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
import artframework.sts1.patch.RoomRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RoomRenderPatchesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    @Test
    public void fullReadySuppressesNativeRewardRender() {
        publishRewardFrame();
        FullPresentMode.setRewardLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                RoomRenderPatches.ObserveNativeRewardRender.Prefix(null, null);

        assertTrue("FULL + mounted + reward + ready executor must suppress CombatRewardScreen.render",
                result.isPresent());
    }

    @Test
    public void offContinuesNativeRewardRender() {
        publishRewardFrame();

        SpireReturn<Void> result =
                RoomRenderPatches.ObserveNativeRewardRender.Prefix(null, null);

        assertFalse("OFF must let CombatRewardScreen.render continue", result.isPresent());
    }

    @Test
    public void fullReadySuppressesNativeRestRender() {
        publishRestFrame();
        FullPresentMode.setRestLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                RoomRenderPatches.ObserveNativeRestRender.Prefix(null, null);

        assertTrue("FULL + mounted + rest + ready executor must suppress CampfireUI.render",
                result.isPresent());
    }

    @Test
    public void offContinuesNativeRestRender() {
        publishRestFrame();

        SpireReturn<Void> result =
                RoomRenderPatches.ObserveNativeRestRender.Prefix(null, null);

        assertFalse("OFF must let CampfireUI.render continue", result.isPresent());
    }

    @Test
    public void fullReadySuppressesNativeShopRender() {
        publishShopFrame();
        FullPresentMode.setShopLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                RoomRenderPatches.ObserveNativeShopRender.Prefix(null, null);

        assertTrue("FULL + mounted + shop + ready executor must suppress ShopScreen.render",
                result.isPresent());
    }

    @Test
    public void offContinuesNativeShopRender() {
        publishShopFrame();

        SpireReturn<Void> result =
                RoomRenderPatches.ObserveNativeShopRender.Prefix(null, null);

        assertFalse("OFF must let ShopScreen.render continue", result.isPresent());
    }

    @Test
    public void fullReadySuppressesNativeTreasureRender() {
        publishTreasureFrame();
        FullPresentMode.setTreasureLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                RoomRenderPatches.ObserveNativeTreasureRender.Prefix(null, null);

        assertTrue("FULL + mounted + treasure + ready executor must suppress TreasureRoom.render",
                result.isPresent());
    }

    @Test
    public void offContinuesNativeTreasureRender() {
        publishTreasureFrame();

        SpireReturn<Void> result =
                RoomRenderPatches.ObserveNativeTreasureRender.Prefix(null, null);

        assertFalse("OFF must let TreasureRoom.render continue", result.isPresent());
    }

    private void publishRewardFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        RewardView reward = RewardView.of("combat", "Victory!",
                Arrays.asList(RewardItemView.of(0, "gold", "30 Gold")));
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "reward", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), reward, RestView.empty(),
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.REWARD_COMBAT).mount();
    }

    private void publishRestFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "rest", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(),
                        artframework.context.RestView.of(
                                Collections.singletonList(
                                        artframework.context.RestView.RestOptionView.of("rest", "Rest"))),
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.REST).mount();
    }

    private void publishShopFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        ShopView shop = ShopView.of(150,
                Collections.singletonList(ShopView.ShopEntryView.of(0, "card", "Strike_R", 50)),
                true, 75);
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "shop", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(),
                        RestView.empty(), TreasureView.empty(), shop, TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.SHOP).mount();
    }

    private void publishTreasureFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "treasure", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(),
                        RestView.empty(), TreasureView.closed(), ShopView.empty(),
                        TopPanelView.empty(), MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.TREASURE).mount();
    }
}
