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

    // ---- NRO-04 D05: shop rug background ------------------------------------------------

    @Test
    public void rugResourceIdSelectsLanguageSpecificTexture() {
        setLanguage("DEU");
        assertEquals(ResourceIds.shopRug("deu"), ShopDrawPath.rugResourceId());
        assertEquals("ui.shop.rug.deu", ShopDrawPath.rugResourceId());

        setLanguage("ZHS");
        assertEquals(ResourceIds.shopRug("zhs"), ShopDrawPath.rugResourceId());

        // A language without a native rug file (DUT) must fall back to the eng default.
        setLanguage("DUT");
        assertEquals(ResourceIds.shopRug("eng"), ShopDrawPath.rugResourceId());

        // Absent/unknown language also falls back to eng.
        setLanguage(null);
        assertEquals("ui.shop.rug.eng", ShopDrawPath.rugResourceId());
    }

    @Test
    public void rugCatalogMapsResolveToExistingJarFiles() throws Exception {
        java.util.Map<String, String> catalog =
                artframework.sts1.assets.Sts1VanillaCatalog.catalog();
        for (String lang : new String[] {"eng", "deu", "epo", "fin", "fra", "ita", "jpn",
                "kor", "rus", "tha", "ukr", "zhs"}) {
            String id = ResourceIds.shopRug(lang);
            assertEquals("sts1:images/npcs/rug/" + lang + ".png", catalog.get(id));
            assertTrue("catalog must know " + id,
                    artframework.sts1.assets.Sts1VanillaCatalog.isKnown(id));
            java.net.URL url = ShopDrawPathTest.class.getClassLoader()
                    .getResource("images/npcs/rug/" + lang + ".png");
            assertTrue(lang + ".png must exist in the host jar", url != null);
        }
        // The full 1920x1136 native size, pinned for the eng default.
        java.net.URL eng = ShopDrawPathTest.class.getClassLoader()
                .getResource("images/npcs/rug/eng.png");
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(eng);
        assertEquals(1920, img.getWidth());
        assertEquals(1136, img.getHeight());
    }

    @Test
    public void rugItemGeometryUsesSettledNativePosition() throws Exception {
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float yScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setStaticField("WIDTH", Integer.valueOf(1920));
            setStaticField("HEIGHT", Integer.valueOf(1080));
            setStaticField("yScale", Float.valueOf(1f));
            ShopDrawPath.RugItem rug = ShopDrawPath.rugItem();
            assertTrue(rug != null);
            assertEquals(0f, rug.x, 0.01f);
            assertEquals(0f, rug.y, 0.01f); // HEIGHT/2 - 540*1 = 0 at 1080p
            assertEquals(1920f, rug.w, 0.01f);
            assertEquals(1080f, rug.h, 0.01f);

            // Non-default yScale keeps the pinned settled formula HEIGHT/2 - 540*yScale.
            setStaticField("yScale", Float.valueOf(1.5f));
            ShopDrawPath.RugItem scaled = ShopDrawPath.rugItem();
            assertEquals(540f - 810f, scaled.y, 0.01f);

            // Uninitialized Settings (WIDTH <= 0) fails open to no rug item.
            setStaticField("WIDTH", Integer.valueOf(0));
            assertTrue(ShopDrawPath.rugItem() == null);
        } finally {
            setStaticField("WIDTH", Integer.valueOf(w));
            setStaticField("HEIGHT", Integer.valueOf(h));
            setStaticField("yScale", Float.valueOf(yScale));
        }
    }

    @Test
    public void probeSliceExposesRugAndKeepsRowList() throws Exception {
        publishShopFrame();
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float yScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setStaticField("WIDTH", Integer.valueOf(1920));
            setStaticField("HEIGHT", Integer.valueOf(1080));
            setStaticField("yScale", Float.valueOf(1f));
            Map<String, Object> probe = ShopDrawPath.probeSlice();
            @SuppressWarnings("unchecked")
            Map<String, Object> rug = (Map<String, Object>) probe.get("rug");
            assertTrue("probe exposes the rug sub-map", rug != null);
            assertTrue("rug resourceId must be a shop.rug.* id",
                    String.valueOf(rug.get("resourceId")).startsWith("ui.shop.rug."));
            assertEquals(Float.valueOf(0f), rug.get("x"));
            assertEquals(Float.valueOf(0f), rug.get("y"));
            assertEquals(Float.valueOf(1920f), rug.get("w"));
            assertEquals(Float.valueOf(1080f), rug.get("h"));
            assertEquals(Integer.valueOf(1), probe.get("rugCount"));
            // The rug is kept out of the public row list.
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) probe.get("items");
            assertEquals(2, items.size());
        } finally {
            setStaticField("WIDTH", Integer.valueOf(w));
            setStaticField("HEIGHT", Integer.valueOf(h));
            setStaticField("yScale", Float.valueOf(yScale));
        }
    }

    @Test
    public void rugSyncsBelowRowsAndIsDrawnFirst() throws Exception {
        publishShopFrame();
        FullPresentMode.setShopLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SHOP).mount();
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float yScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setStaticField("WIDTH", Integer.valueOf(1920));
            setStaticField("HEIGHT", Integer.valueOf(1080));
            setStaticField("yScale", Float.valueOf(1f));
            invokePrepareShop(Sts1RenderPipeline.plan());

            PresentationContext context = PresentationRegistry.context("c2-surfaces");
            EntityId rugEntity = entityFor(context, SurfaceIds.SHOP, ShopDrawPath.RUG_ITEM_ID);
            assertTrue("missing C2 rug item", rugEntity != null);
            DrawComponent draw = context.world().get(rugEntity, DrawComponent.class);
            assertEquals(ShopDrawPath.RUG_ROLE, draw.role);
            assertTrue(draw.resourceId.startsWith("ui.shop.rug."));
            BoundsComponent rugBounds = context.world().get(rugEntity, BoundsComponent.class);
            assertEquals(0f, rugBounds.rect.x, 0.01f);
            assertEquals(1920f, rugBounds.rect.width, 0.01f);

            // The rug z must sit BELOW every chrome row so it paints behind them.
            EntityId title = entityFor(context, SurfaceIds.SHOP, "title");
            BoundsComponent titleBounds = context.world().get(title, BoundsComponent.class);
            assertTrue("rug must sort behind the chrome rows",
                    rugBounds.z < titleBounds.z);
        } finally {
            setStaticField("WIDTH", Integer.valueOf(w));
            setStaticField("HEIGHT", Integer.valueOf(h));
            setStaticField("yScale", Float.valueOf(yScale));
        }
    }

    private static void setLanguage(String enumName) {
        try {
            Class<?> langClass = Class.forName(
                    "com.megacrit.cardcrawl.core.Settings$GameLanguage");
            Object value = null;
            if (enumName != null) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object resolved = Enum.valueOf((Class<? extends Enum>) langClass, enumName);
                value = resolved;
            }
            java.lang.reflect.Field field =
                    com.megacrit.cardcrawl.core.Settings.class.getDeclaredField("language");
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set Settings.language=" + enumName, failure);
        }
    }

    private static void setStaticField(String name, Object value) {
        try {
            java.lang.reflect.Field field =
                    com.megacrit.cardcrawl.core.Settings.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set Settings." + name, failure);
        }
    }
}
