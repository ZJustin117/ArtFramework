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
import artframework.sts1.PresentLevel;
import artframework.sts1.backend.Sts1PresentationBackend;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TreasureDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void publishTreasureFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        TreasureView treasure = TreasureView.closed();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "treasure", null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(),
                        RestView.empty(), treasure, ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void probeSliceReportsClosedChest() {
        publishTreasureFrame();
        Map<String, Object> probe = TreasureDrawPath.probeSlice();
        assertEquals(Boolean.FALSE, probe.get("chestOpen"));
        assertEquals(Boolean.TRUE, probe.get("canOpen"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeTreasure"));
        assertEquals(Integer.valueOf(2), probe.get("chromeLineCount"));
        assertEquals(Integer.valueOf(2), probe.get("drawCount"));
    }

    @Test
    public void fullMountedTreasureDrawsAndSuppressesNative() {
        publishTreasureFrame();
        FullPresentMode.setTreasureLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.TREASURE).mount();
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.TREASURE));
        // Slice C phase 2: minimal chest chrome supplies real pixels, so suppression is safe.
        assertTrue(plan.shouldSuppressNative(SurfaceIds.TREASURE));
        assertTrue(TreasureDrawPath.shouldSuppressNativeTreasure());
        assertEquals(Boolean.TRUE, TreasureDrawPath.probeSlice().get("suppressNativeTreasure"));
    }

    @Test
    public void offDoesNotSuppress() {
        publishTreasureFrame();
        ArtFramework.component(SurfaceIds.TREASURE).mount();
        assertFalse(TreasureDrawPath.shouldSuppressNativeTreasure());
    }

    @Test
    public void treasureRowsUseChestAndRelicResources() {
        publishTreasureFrame();
        assertEquals(ResourceIds.UI_TREASURE_PANEL, TreasureDrawPath.chromeLines().get(0).resourceId);
        assertEquals(ResourceIds.UI_TREASURE_CHEST_CLOSED,
                TreasureDrawPath.chromeLines().get(1).resourceId);

        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.ofFull(
                1L, 1L, "treasure", null, ControlsView.empty(), MapView.empty(), EventView.empty(),
                SelectView.empty(), RewardView.empty(), RestView.empty(),
                TreasureView.opened("Bag of Marbles", "relic.Bag of Marbles"), ShopView.empty(),
                TopPanelView.empty(), MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());

        assertEquals(3, TreasureDrawPath.chromeLines().size());
        assertEquals(ResourceIds.UI_TREASURE_CHEST_OPEN,
                TreasureDrawPath.chromeLines().get(1).resourceId);
        assertEquals(ResourceIds.UI_TREASURE_RELIC,
                TreasureDrawPath.chromeLines().get(2).resourceId);
    }

    @Test
    public void treasureChromeRowsSyncResourceGeometryAndStateToC2() throws Exception {
        publishOpenedTreasureFrame();
        FullPresentMode.setTreasureLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.TREASURE).mount();

        invokePrepareTreasure(Sts1RenderPipeline.plan());

        List<RoomChromeLine> lines = TreasureDrawPath.chromeLines();
        assertEquals(ResourceIds.UI_TREASURE_CHEST_OPEN, lines.get(1).resourceId);
        assertFalse(lines.get(1).enabled);
        assertC2Line(SurfaceIds.TREASURE, lines.get(0));
        assertC2Line(SurfaceIds.TREASURE, lines.get(1));
        assertC2Line(SurfaceIds.TREASURE, lines.get(2));
    }

    @Test
    public void treasureVisualsCleanupStaleRelicAndEvidenceTracksChromeRows() throws Exception {
        publishOpenedTreasureFrame();
        FullPresentMode.setTreasureLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.TREASURE).mount();
        invokePrepareTreasure(Sts1RenderPipeline.plan());
        assertTrue(hasC2Line(SurfaceIds.TREASURE, "relic"));

        publishTreasureFrame();
        invokePrepareTreasure(Sts1RenderPipeline.plan());
        assertTrue(hasC2Line(SurfaceIds.TREASURE, "chest"));
        assertFalse(hasC2Line(SurfaceIds.TREASURE, "relic"));

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.TREASURE, "native.Treasure", "render", "test");
        int chromeRows = TreasureDrawPath.chromeLines().size();
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.TREASURE, chromeRows);
        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertEquals(chromeRows, evidence.drawCount);
    }

    private void publishOpenedTreasureFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.ofFull(
                1L, 1L, "treasure", null, ControlsView.empty(), MapView.empty(), EventView.empty(),
                SelectView.empty(), RewardView.empty(), RestView.empty(),
                TreasureView.opened("Strike_R", ResourceIds.cardArt("Strike_R")), ShopView.empty(),
                TopPanelView.empty(), MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private static void invokePrepareTreasure(SurfaceDrawPlan plan) throws Exception {
        java.lang.reflect.Method prepare = Sts1SurfaceRenderer.class.getDeclaredMethod(
                "prepareTreasureVisuals", SurfaceDrawPlan.class);
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

    // ---- NRO-04 D08: treasure chest sprite (AbstractChest geometry + per-kind texture) ----------

    private static final String NATIVE_SMALL = ResourceIds.chestSprite("small", false);
    private static final String NATIVE_MEDIUM_OPEN = ResourceIds.chestSprite("medium", true);
    private static final String NATIVE_LARGE = ResourceIds.chestSprite("large", false);
    private static final String NATIVE_BOSS_OPEN = ResourceIds.chestSprite("boss", true);

    @Test
    public void chestSpriteGeometryMatchesNativeAbstractChestAtUnitScale() {
        TreasureDrawPath.ChestItem chest =
                TreasureDrawPath.chestItemAt(NATIVE_LARGE, 1f, 1920f, 500f);
        assertTrue(chest != null);
        // Native AbstractChest.render: center (WIDTH/2 + 348*scale, floorY + 192*scale), 512*scale.
        assertEquals(960f + 348f, chest.x, 0.01f);
        assertEquals(500f + 192f, chest.y, 0.01f);
        assertEquals(512f, chest.w, 0.01f);
        assertEquals(512f, chest.h, 0.01f);
        // The renderer draws bottom-left = center - size/2 (origin 256/256 native).
        artframework.component.Rect b = chest.bounds();
        assertEquals(960f + 348f - 256f, b.x, 0.01f);
        assertEquals(500f + 192f - 256f, b.y, 0.01f);
    }

    @Test
    public void chestSpriteGeometryScalesWithNonUnitScale() {
        float scale = 1.25f;
        TreasureDrawPath.ChestItem chest =
                TreasureDrawPath.chestItemAt(NATIVE_SMALL, scale, 1920f, 400f);
        assertTrue(chest != null);
        assertEquals(960f + 348f * scale, chest.x, 0.01f);
        assertEquals(400f + 192f * scale, chest.y, 0.01f);
        assertEquals(512f * scale, chest.w, 0.01f);
        assertEquals(512f * scale, chest.h, 0.01f);
    }

    @Test
    public void chestSpriteFailsOpenToMediumClosedWhenResourceEmpty() {
        TreasureDrawPath.ChestItem chest = TreasureDrawPath.chestItemAt("", 1f, 1920f, 500f);
        assertTrue(chest != null);
        assertEquals(ResourceIds.chestSprite("medium", false), chest.resourceId);
        assertTrue(TreasureDrawPath.chestItemAt(null, 0f, 1920f, 500f) == null);
        assertTrue(TreasureDrawPath.chestItemAt(NATIVE_SMALL, 1f, 0f, 500f) == null);
    }

    @Test
    public void chestResourceSelectsByKindAndOpenFlag() {
        assertEquals(NATIVE_SMALL,
                Sts1PresentationBackend.chestResource(new SmallChest(false)));
        assertEquals(NATIVE_MEDIUM_OPEN,
                Sts1PresentationBackend.chestResource(new MediumChest(true)));
        assertEquals(NATIVE_LARGE,
                Sts1PresentationBackend.chestResource(new LargeChest(false)));
        assertEquals(NATIVE_BOSS_OPEN,
                Sts1PresentationBackend.chestResource(new BossChest(true)));
        // An unknown class, an unreadable/non-boolean isOpen field, and a null chest fail open to a
        // medium CLOSED chest.
        assertEquals(ResourceIds.chestSprite("medium", false),
                Sts1PresentationBackend.chestResource(new Object()));
        assertEquals(ResourceIds.chestSprite("medium", false),
                Sts1PresentationBackend.chestResource(new UnknownChest()));
        assertEquals(ResourceIds.chestSprite("medium", false),
                Sts1PresentationBackend.chestResource(null));
    }

    @Test
    public void chestCatalogIdsResolveToExistingJarFiles() throws Exception {
        java.util.Map<String, String> catalog =
                artframework.sts1.assets.Sts1VanillaCatalog.catalog();
        for (String kind : new String[] {"small", "medium", "large", "boss"}) {
            String closedId = ResourceIds.chestSprite(kind, false);
            String openedId = ResourceIds.chestSprite(kind, true);
            assertEquals("sts1:images/npcs/" + kind + "Chest.png", catalog.get(closedId));
            assertEquals("sts1:images/npcs/" + kind + "ChestOpened.png", catalog.get(openedId));
            assertTrue(artframework.sts1.assets.Sts1VanillaCatalog.isKnown(closedId));
            assertTrue(artframework.sts1.assets.Sts1VanillaCatalog.isKnown(openedId));
            java.net.URL closed = TreasureDrawPathTest.class.getClassLoader()
                    .getResource("images/npcs/" + kind + "Chest.png");
            java.net.URL opened = TreasureDrawPathTest.class.getClassLoader()
                    .getResource("images/npcs/" + kind + "ChestOpened.png");
            assertTrue(kind + "Chest.png must exist in the host jar", closed != null);
            assertTrue(kind + "ChestOpened.png must exist in the host jar", opened != null);
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(closed);
            assertEquals(512, img.getWidth());
            assertEquals(512, img.getHeight());
        }
        // The text-row chest ids are a documented MIS-MAP to the map icons, NOT the sprite.
        assertEquals("sts1:images/ui/map/chest.png",
                catalog.get(ResourceIds.UI_TREASURE_CHEST_CLOSED));
        assertEquals("sts1:images/ui/map/chestOutline.png",
                catalog.get(ResourceIds.UI_TREASURE_CHEST_OPEN));
    }

    @Test
    public void probeSliceExposesChestSpriteBehindRows() {
        publishTreasureFrame();
        Map<String, Object> probe = TreasureDrawPath.probeSlice();
        // The chest sprite is its own sub-map, kept out of the chrome row list.
        assertTrue(probe.containsKey("chest"));
        int rows = TreasureDrawPath.chromeLines().size();
        assertEquals(Integer.valueOf(rows), probe.get("chromeLineCount"));
        assertEquals(Integer.valueOf(rows), probe.get("drawCount"));
        // submitCount is the chest sprite (when supplied) plus every chrome row.
        assertEquals(Integer.valueOf(((Integer) probe.get("chestCount")).intValue() + rows),
                probe.get("submitCount"));
    }

    /** Reflection-compatible chest stubs: the class simple name drives the projected kind. */
    public static final class SmallChest {
        public boolean isOpen;
        public SmallChest(boolean isOpen) { this.isOpen = isOpen; }
    }

    public static final class MediumChest {
        public boolean isOpen;
        public MediumChest(boolean isOpen) { this.isOpen = isOpen; }
    }

    public static final class LargeChest {
        public boolean isOpen;
        public LargeChest(boolean isOpen) { this.isOpen = isOpen; }
    }

    public static final class BossChest {
        public boolean isOpen;
        public BossChest(boolean isOpen) { this.isOpen = isOpen; }
    }

    /** Unknown class whose {@code isOpen}-named field is not a boolean. */
    public static final class UnknownChest {
        public String isOpen = "yes";
    }
}
