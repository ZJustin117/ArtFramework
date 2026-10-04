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
import static org.junit.Assert.assertNotNull;
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
    public void rewardRowGeometryTreatsViewXAsRowCenterNotBottomLeft() throws Exception {
        // Regression guard for the D04 D1 defect: the backend publishes the native RewardItem.hb
        // CENTER (native moves hb to (Settings.WIDTH/2, y)); RewardItemView.x/y are CENTER, so the
        // DrawPath bounds are (x - w/2, y - h/2). With the native center x = WIDTH/2 = 960 and the
        // native REWARD_SCREEN_ITEM width 464, bounds.x is 960 - 232 = 728 (the native panel origin
        // WIDTH/2 - 232). If x were mistakenly a hitbox BOTTOM-LEFT the row would shift ~half a panel
        // left (bounds.x 496), which this test rejects.
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(1920));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();

            FakeSignalBackend backend = new FakeSignalBackend();
            backend.installSignals();
            float nativeCenterX = com.megacrit.cardcrawl.core.Settings.WIDTH / 2f;
            assertEquals(960f, nativeCenterX, 0.01f);
            RewardView reward = RewardView.of("combat", "Victory!",
                    Arrays.asList(
                            new RewardItemView(0, "gold", "30 Gold", "ui.reward.gold",
                                    true, true, nativeCenterX, 540f, 464f, 98f)));
            backend.publish(ContextFrame.ofFull(
                    1L, 1L, "reward", null, ControlsView.empty(), MapView.empty(),
                    EventView.empty(), SelectView.empty(), reward, RestView.empty(),
                    TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                    MonsterIntentView.empty(), null));
            ArtFramework.publishFrame(backend.currentFrame());

            RewardDrawPath.DrawItem row = RewardDrawPath.buildFromProjection().get(0);
            assertEquals("view x carries the native row CENTER", 960f, row.x, 0.01f);
            assertEquals("bounds.x = center - w/2 = WIDTH/2 - 232", 728f, row.bounds().x, 0.01f);
            assertEquals("row center stays WIDTH/2 (rows are horizontally centered)",
                    960f, row.bounds().x + row.bounds().width / 2f, 0.01f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
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

    @Test
    public void rewardSheetUsesNativeGeometryAtUnitScaleAndSortsBeforeRows() throws Exception {
        // Native CombatRewardScreen.renderItemReward (verified bytecode) draws REWARD_SCREEN_SHEET at
        // bottom-left (WIDTH/2 - 306, HEIGHT/2 - 46*scale - 358), size (612*xScale, 716*scale).
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();
            publishRewardFrame();

            RewardDrawPath.DrawItem sheet = RewardDrawPath.sheetItem();
            assertTrue("sheet is supplied when Settings is initialized and reward available",
                    sheet != null);
            assertEquals(ResourceIds.UI_REWARD_SHEET, sheet.resourceId);
            assertEquals(RewardDrawPath.SHEET_KIND, sheet.kind);
            assertEquals("", sheet.label);
            assertTrue(sheet.visible);
            // DrawItem x/y is the CENTER; renderer applies x - w/2, y - h/2.
            assertEquals(960f, sheet.x, 0.01f);   // WIDTH/2 - 306 + 306*1
            assertEquals(494f, sheet.y, 0.01f);   // HEIGHT/2 - 46*1 - 358 + 358*1
            assertEquals(612f, sheet.w, 0.01f);
            assertEquals(716f, sheet.h, 0.01f);
            assertEquals("native bottom-left x = WIDTH/2 - 306", 654f, sheet.bounds().x, 0.01f);
            assertEquals("native bottom-left y = HEIGHT/2 - 46*scale - 358", 136f,
                    sheet.bounds().y, 0.01f);

            Map<String, Object> probe = RewardDrawPath.probeSlice();
            @SuppressWarnings("unchecked")
            Map<String, Object> sheetMap = (Map<String, Object>) probe.get("sheet");
            assertNotNull("probe exposes the sheet sub-map", sheetMap);
            assertEquals(ResourceIds.UI_REWARD_SHEET, sheetMap.get("resourceId"));
            assertEquals(Float.valueOf(960f), sheetMap.get("x"));
            assertEquals(Float.valueOf(494f), sheetMap.get("y"));
            assertEquals(Integer.valueOf(1), probe.get("sheetCount"));
            assertEquals("submitCount = sheet + visible rows", Integer.valueOf(3),
                    probe.get("submitCount"));

            List<RewardDrawPath.DrawItem> order = RewardDrawPath.drawOrder();
            assertEquals("sheet first, then rows", 3, order.size());
            assertEquals(RewardDrawPath.SHEET_KIND, order.get(0).kind);
            assertEquals(RewardDrawPath.SHEET_Z, order.get(0).z, 0.01f);
            assertTrue("sheet sorts before the rows", order.get(0).z < order.get(1).z);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale",
                    Float.valueOf(previousXScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
        }
    }

    @Test
    public void rewardSheetGeometryCatchesXScaleVersusScaleMixups() throws Exception {
        // Non-unit, xScale != scale: the bottom-left stays pinned at WIDTH/2-306 in x and
        // HEIGHT/2-46*scale-358 in y, while the SIZE carries 612*xScale / 716*scale.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1.25f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1.5f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();
            publishRewardFrame();

            RewardDrawPath.DrawItem sheet = RewardDrawPath.sheetItem();
            assertTrue(sheet != null);
            assertEquals(612f * 1.5f, sheet.w, 0.01f);
            assertEquals(716f * 1.25f, sheet.h, 0.01f);
            // center x carries +306*xScale; center y carries +358*scale
            assertEquals(960f - 306f + 306f * 1.5f, sheet.x, 0.01f);
            assertEquals(540f - 46f * 1.25f - 358f + 358f * 1.25f, sheet.y, 0.01f);
            // bottom-left x is invariant of xScale; bottom-left y is anchored to scale
            assertEquals(654f, sheet.bounds().x, 0.01f);
            assertEquals(540f - 46f * 1.25f - 358f, sheet.bounds().y, 0.01f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale",
                    Float.valueOf(previousXScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
        }
    }

    @Test
    public void rewardSheetResourceMapsToExistingNativeTexture() throws Exception {
        java.util.Map<String, String> catalog =
                artframework.sts1.assets.Sts1VanillaCatalog.catalog();
        assertEquals("sts1:images/ui/reward/rewardScreenSheet.png",
                catalog.get(ResourceIds.UI_REWARD_SHEET));
        assertTrue(artframework.sts1.assets.Sts1VanillaCatalog.isKnown(ResourceIds.UI_REWARD_SHEET));

        // The mapped texture must actually EXIST in the host jar at the native 612x716 size.
        java.net.URL url = RewardDrawPathTest.class.getClassLoader()
                .getResource("images/ui/reward/rewardScreenSheet.png");
        assertNotNull("rewardScreenSheet.png must exist in the host jar", url);
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(url);
        assertNotNull(img);
        assertEquals(612, img.getWidth());
        assertEquals(716, img.getHeight());
    }

    private static void setStaticField(Class<?> owner, String name, Object value) {
        try {
            java.lang.reflect.Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set static field " + owner + "." + name, failure);
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
