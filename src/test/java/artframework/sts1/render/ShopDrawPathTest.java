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
import artframework.sts1.PresentSafety;
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
        assertEquals(ResourceIds.cardArt("Strike_R"),
                ShopDrawPath.buildFromProjection().get(0).resourceId);
        assertEquals(ResourceIds.UI_REWARD_RELIC,
                ShopDrawPath.buildFromProjection().get(1).resourceId);
        Map<String, Object> probe = ShopDrawPath.probeSlice();
        assertEquals(Integer.valueOf(150), probe.get("gold"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeShop"));
    }

    @Test
    public void shopEntriesUseKnownResourcesAndSoldOutFallback() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        ShopView shop = ShopView.of(77, Arrays.asList(
                ShopView.ShopEntryView.of(0, "card", "Strike_R", 50, false, ResourceIds.cardArt("Strike_R")),
                ShopView.ShopEntryView.of(1, "relic", "Sold Relic", 99, true, ResourceIds.UI_REWARD_RELIC),
                ShopView.ShopEntryView.of(2, "potion", "Potion", 25)),
                true, 75);
        backend.publish(ContextFrame.ofFull(
                1L, 1L, "shop", null, ControlsView.empty(), MapView.empty(), EventView.empty(),
                SelectView.empty(), RewardView.empty(), RestView.empty(), TreasureView.empty(), shop,
                TopPanelView.empty(), MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());

        assertEquals(ResourceIds.cardArt("Strike_R"), ShopDrawPath.buildFromProjection().get(0).resourceId);
        assertEquals(ResourceIds.UI_SHOP_SOLD_OUT, ShopDrawPath.buildFromProjection().get(1).resourceId);
        assertEquals(ResourceIds.UI_SHOP_ENTRY_PANEL, ShopDrawPath.buildFromProjection().get(2).resourceId);
        assertEquals(ResourceIds.UI_SHOP_MERCHANT, ShopDrawPath.chromeLines().get(0).resourceId);
        assertEquals(ResourceIds.UI_SHOP_GOLD, ShopDrawPath.chromeLines().get(1).resourceId);
        assertEquals(ResourceIds.UI_SHOP_PURGE, ShopDrawPath.chromeLines().get(5).resourceId);
    }

    @Test
    public void shopChromeRowsSyncResourceGeometryAndStateToC2() throws Exception {
        publishShopFrame();
        FullPresentMode.setShopLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SHOP).mount();

        invokePrepareShop(Sts1RenderPipeline.plan());

        List<RoomChromeLine> lines = ShopDrawPath.chromeLines();
        assertC2Line(SurfaceIds.SHOP, lines.get(0));
        assertC2Line(SurfaceIds.SHOP, lines.get(1));
        assertC2Line(SurfaceIds.SHOP, lines.get(2));
        assertC2Line(SurfaceIds.SHOP, lines.get(3));
    }

    @Test
    public void shopVisualsCleanupStaleRowsAndEvidenceTracksChromeRows() throws Exception {
        publishShopFrame();
        FullPresentMode.setShopLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SHOP).mount();
        invokePrepareShop(Sts1RenderPipeline.plan());
        assertTrue(hasC2Line(SurfaceIds.SHOP, "entry:1"));
        assertTrue(hasC2Line(SurfaceIds.SHOP, "purge"));

        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        ShopView shop = ShopView.of(10, Arrays.asList(
                ShopView.ShopEntryView.of(0, "potion", "Fire Potion", 40)), false, 0);
        backend.publish(ContextFrame.ofFull(
                2L, 2L, "shop", null, ControlsView.empty(), MapView.empty(), EventView.empty(),
                SelectView.empty(), RewardView.empty(), RestView.empty(), TreasureView.empty(), shop,
                TopPanelView.empty(), MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        invokePrepareShop(Sts1RenderPipeline.plan());

        assertTrue(hasC2Line(SurfaceIds.SHOP, "entry:0"));
        assertFalse(hasC2Line(SurfaceIds.SHOP, "entry:1"));
        assertFalse(hasC2Line(SurfaceIds.SHOP, "purge"));

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.SHOP, "native.Shop", "render", "test");
        int chromeRows = ShopDrawPath.chromeLines().size();
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.SHOP, chromeRows);
        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertEquals(chromeRows, evidence.drawCount);
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

    private static void invokePrepareShop(SurfaceDrawPlan plan) throws Exception {
        java.lang.reflect.Method prepare = Sts1SurfaceRenderer.class.getDeclaredMethod(
                "prepareShopVisuals", SurfaceDrawPlan.class);
        prepare.setAccessible(true);
        prepare.invoke(null, plan);
    }

    private static void assertC2Line(String surfaceId, RoomChromeLine line) {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        EntityId entity = entityFor(context, surfaceId, line.id);
        assertTrue("missing C2 visual " + line.id, entity != null);
        DrawComponent draw = context.world().get(entity, DrawComponent.class);
        assertEquals(line.role, draw.role);
        assertEquals(line.resourceId, draw.resourceId);
        assertEquals(line.text, draw.text);
        BoundsComponent bounds = context.world().get(entity, BoundsComponent.class);
        assertEquals(line.x, bounds.rect.x, 0.01f);
        assertEquals(line.y, bounds.rect.y, 0.01f);
        assertEquals(line.w, bounds.rect.width, 0.01f);
        assertEquals(line.h, bounds.rect.height, 0.01f);
        VisibilityComponent visibility = context.world().get(entity, VisibilityComponent.class);
        assertTrue(visibility.visible);
    }

    private static boolean hasC2Line(String surfaceId, String localId) {
        return entityFor(PresentationRegistry.context("c2-surfaces"), surfaceId, localId) != null;
    }

    private static EntityId entityFor(PresentationContext context, String surfaceId, String localId) {
        String scope = "sts1.visual." + surfaceId;
        for (EntityId candidate : context.entities()) {
            NodeIdentityComponent identity = context.world().get(candidate, NodeIdentityComponent.class);
            if (identity != null && scope.equals(identity.key.scope)
                    && localId.equals(identity.key.localId)) {
                return candidate;
            }
        }
        return null;
    }
}
