package artframework.sts1.render;

import artframework.api.ArtFramework;
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
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentSafety;
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

public class ShopDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private void publishShopFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        ShopView shop = ShopView.of(150, Arrays.asList(
                ShopView.ShopEntryView.of(0, "card", "Strike_R", 50),
                ShopView.ShopEntryView.of(1, "relic", "Bag of Marbles", 150)),
                true, 75);
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "shop", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(),
                        RestView.empty(), TreasureView.empty(), shop, TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void buildFromProjectionListsEntries() {
        publishShopFrame();
        assertEquals(2, ShopDrawPath.buildFromProjection().size());
        Map<String, Object> probe = ShopDrawPath.probeSlice();
        assertEquals(Integer.valueOf(150), probe.get("gold"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeShop"));
    }

    @Test
    public void fullMountedShopDrawsAndSuppressesNative() {
        publishShopFrame();
        FullPresentMode.setShopLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SHOP).mount();
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.SHOP));
        // Slice C phase 2: minimal merchant chrome supplies real pixels, so suppression is safe.
        assertTrue(plan.shouldSuppressNative(SurfaceIds.SHOP));
        assertTrue(ShopDrawPath.shouldSuppressNativeShop());
        assertEquals(Boolean.TRUE, ShopDrawPath.probeSlice().get("suppressNativeShop"));
    }

    @Test
    public void shopSuppressesOnlyWhenFullReadySceneMountedAndExecutorReady() {
        publishShopFrame();
        ArtFramework.component(SurfaceIds.SHOP).mount();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        assertFalse("OFF is not delegated coverage", ShopDrawPath.shouldSuppressNativeShop());

        FullPresentMode.setShopLevel(PresentLevel.FULL);
        assertTrue("FULL + shop scene + mounted + executor-ready suppresses native shop",
                ShopDrawPath.shouldSuppressNativeShop());

        CombatInputRouter.resetForTests();
        assertFalse("executor-less FULL shop must continue native",
                ShopDrawPath.shouldSuppressNativeShop());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SHOP).unmount();
        assertFalse("unmounted FULL shop must continue native",
                ShopDrawPath.shouldSuppressNativeShop());

        ArtFramework.component(SurfaceIds.SHOP).mount();
        publishNonShopSceneFrame();
        assertFalse("scene mismatch must continue native",
                ShopDrawPath.shouldSuppressNativeShop());

        publishShopFrame();
        PresentSafety.panic("shop-test");
        assertFalse("panic fail-open must not be counted as coverage",
                ShopDrawPath.shouldSuppressNativeShop());
    }

    @Test
    public void offDoesNotSuppress() {
        publishShopFrame();
        ArtFramework.component(SurfaceIds.SHOP).mount();
        assertFalse(ShopDrawPath.shouldSuppressNativeShop());
    }

    private void publishNonShopSceneFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "event", null, ControlsView.empty(), MapView.empty(),
                        EventView.of("Other", java.util.Collections.<artframework.context.EventOptionView>emptyList()),
                        SelectView.empty(), RewardView.empty(), RestView.empty(), TreasureView.empty(),
                        ShopView.empty(), TopPanelView.empty(), MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }
}
