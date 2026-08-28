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
import artframework.sts1.PresentSafety;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RoomRenderPatchesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
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
    public void shopPatchContinuesForFailOpenAndIncompleteReadinessStates() {
        publishShopFrame();
        FullPresentMode.setShopLevel(PresentLevel.FULL);

        assertFalse("executor-less FULL shop must continue ShopScreen.render",
                RoomRenderPatches.ObserveNativeShopRender.Prefix(null, null).isPresent());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SHOP).unmount();
        assertFalse("unmounted FULL shop must continue ShopScreen.render",
                RoomRenderPatches.ObserveNativeShopRender.Prefix(null, null).isPresent());

        publishShopFrame();
        publishMismatchedShopOwnerFrame();
        assertFalse("scene mismatch must continue ShopScreen.render",
                RoomRenderPatches.ObserveNativeShopRender.Prefix(null, null).isPresent());

        publishShopFrame();
        PresentSafety.panic("shop-test");
        assertFalse("panic fail-open must continue ShopScreen.render",
                RoomRenderPatches.ObserveNativeShopRender.Prefix(null, null).isPresent());

        PresentSafety.resetForTests();
        NativeRenderBridge.resetForTests();
        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                "sts1.unknown-owner", "native.Unknown", "render", "unknown");
        assertTrue("unknown owner fail-opens to native", unknown.nativeContinuation);
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("runtimeUNKNOWN"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("delegateToArt"));
    }

    @Test
    public void roomSurfaceSuppressionRequiresFullMountedMatchingSceneAndExecutorReady() {
        assertSurfaceGate(SurfaceIds.REWARD_COMBAT, "reward");
        assertSurfaceGate(SurfaceIds.REWARD_CARD, "reward");
        assertSurfaceGate(SurfaceIds.REWARD_BOSS_RELIC, "reward");
        assertSurfaceGate(SurfaceIds.REST, "rest");
        assertSurfaceGate(SurfaceIds.TREASURE, "treasure");
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

    private void publishMismatchedShopOwnerFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "rest", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(), RestView.empty(),
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
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

    private void assertSurfaceGate(String surfaceId, String scene) {
        resetRoomState();
        publishFrame(scene);
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
        RenderDisposition suppress = NativeRenderBridge.beginSurface(surfaceId,
                "native." + surfaceId, "render", "ready");
        assertEquals(surfaceId + " FULL + mounted + matching scene + ready executor suppresses",
                RenderDisposition.Mode.DELEGATE_TO_ART, suppress.mode);
        assertFalse(suppress.nativeContinuation);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("delegateToArt"));

        resetRoomState();
        publishFrame(scene);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
        RenderDisposition off = NativeRenderBridge.beginSurface(surfaceId,
                "native." + surfaceId, "render", "off");
        assertTrue(surfaceId + " OFF continues native", off.nativeContinuation);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("delegateToArt"));

        resetRoomState();
        publishFrame(scene);
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        ArtFramework.component(surfaceId).mount();
        RenderDisposition executorless = NativeRenderBridge.beginSurface(surfaceId,
                "native." + surfaceId, "render", "executorless");
        assertTrue(surfaceId + " executor-less FULL continues native", executorless.nativeContinuation);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("delegateToArt"));

        resetRoomState();
        publishFrame(scene);
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        RenderDisposition unmounted = NativeRenderBridge.beginSurface(surfaceId,
                "native." + surfaceId, "render", "unmounted");
        assertTrue(surfaceId + " unmounted FULL continues native", unmounted.nativeContinuation);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("delegateToArt"));

        resetRoomState();
        publishFrame("combat");
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
        RenderDisposition mismatch = NativeRenderBridge.beginSurface(surfaceId,
                "native." + surfaceId, "render", "scene-mismatch");
        assertTrue(surfaceId + " scene mismatch continues native", mismatch.nativeContinuation);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("delegateToArt"));

        resetRoomState();
        publishFrame(scene);
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
        PresentSafety.panic("room-gate-test");
        RenderDisposition panic = NativeRenderBridge.beginSurface(surfaceId,
                "native." + surfaceId, "render", "panic");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, panic.mode);
        assertTrue(surfaceId + " panic fail-opens to native", panic.nativeContinuation);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("delegateToArt"));

        resetRoomState();
        publishFrame(scene);
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
        RenderDisposition unknown = NativeRenderBridge.beginSurface(surfaceId + ".unknown",
                "native.Unknown", "render", "unknown-owner");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertTrue(surfaceId + " unknown owner fail-opens to native", unknown.nativeContinuation);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("runtimeUNKNOWN"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("delegateToArt"));
    }

    private void resetRoomState() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private void publishFrame(String scene) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        RewardView reward = "reward".equals(scene)
                ? RewardView.of("combat", "Rewards", Arrays.asList(RewardItemView.of(0, "gold", "30 Gold")))
                : RewardView.empty();
        RestView rest = "rest".equals(scene)
                ? RestView.of(Collections.singletonList(RestView.RestOptionView.of("rest", "Rest")))
                : RestView.empty();
        TreasureView treasure = "treasure".equals(scene) ? TreasureView.closed() : TreasureView.empty();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, scene, null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), reward, rest,
                        treasure, ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }
}
