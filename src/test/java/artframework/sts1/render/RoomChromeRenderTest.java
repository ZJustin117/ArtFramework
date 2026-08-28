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
import artframework.ecs.EntityId;
import artframework.presentation.DrawComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Slice C phase 2: rest/shop/treasure minimal text chrome. Verifies the pure projection
 * (chromeLines), the C2 visual item materialization (prepare*Visuals), and that the recorded
 * draw evidence equals the real painted row count. Font rendering itself needs GL and is
 * covered by the D1 device smoke, not this suite.
 */
public class RoomChromeRenderTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void publishFrame(String scene, RestView rest, TreasureView treasure, ShopView shop) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, scene, null, ControlsView.empty(), MapView.empty(),
                        EventView.empty(), SelectView.empty(), RewardView.empty(),
                        rest, treasure, shop, TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private void fullReady(String surfaceId) {
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
    }

    @Test
    public void restChromeLinesProjectTitleAndOptions() {
        publishFrame("rest",
                RestView.of(Arrays.asList(
                        new RestView.RestOptionView("rest", "Rest", true, true),
                        new RestView.RestOptionView("smith", "Smith", false, true))),
                TreasureView.empty(), ShopView.empty());

        List<RoomChromeLine> lines = RestDrawPath.chromeLines();

        assertEquals(3, lines.size());
        assertEquals("title", lines.get(0).id);
        assertEquals("Campfire", lines.get(0).text);
        assertTrue(lines.get(0).enabled);
        assertEquals("option:rest", lines.get(1).id);
        assertEquals("Rest", lines.get(1).text);
        assertTrue(lines.get(1).enabled);
        assertEquals("option:smith", lines.get(2).id);
        assertEquals("Smith", lines.get(2).text);
        assertTrue("projected availability must reach the chrome row", !lines.get(2).enabled);
        assertEquals(Integer.valueOf(lines.size()), RestDrawPath.probeSlice().get("chromeLineCount"));
    }

    @Test
    public void shopChromeLinesProjectGoldEntriesAndPurge() {
        publishFrame("shop", RestView.empty(), TreasureView.empty(),
                ShopView.of(150, Arrays.asList(
                        ShopView.ShopEntryView.of(0, "card", "Strike_R", 50),
                        new ShopView.ShopEntryView(1, "relic", "Bag of Marbles", 150, true, ""),
                        ShopView.ShopEntryView.of(2, "potion", "Fire Potion", 75)),
                        true, 75));

        List<RoomChromeLine> lines = ShopDrawPath.chromeLines();

        assertEquals(6, lines.size());
        assertEquals("Merchant", lines.get(0).text);
        assertEquals("gold", lines.get(1).id);
        assertEquals("Gold 150", lines.get(1).text);
        assertEquals("entry:0", lines.get(2).id);
        assertEquals("Strike_R  50G", lines.get(2).text);
        assertTrue(lines.get(2).enabled);
        assertEquals("entry:1", lines.get(3).id);
        assertEquals("Bag of Marbles  150G", lines.get(3).text);
        assertTrue("sold-out entry must render disabled", !lines.get(3).enabled);
        assertEquals("entry:2", lines.get(4).id);
        assertEquals("Fire Potion  75G", lines.get(4).text);
        assertTrue(lines.get(4).enabled);
        assertEquals("purge", lines.get(5).id);
        assertEquals("Card Removal  75G", lines.get(5).text);
        assertEquals(Integer.valueOf(lines.size()), ShopDrawPath.probeSlice().get("chromeLineCount"));
        assertEquals("minimal text chrome exposes each projected entry plus title/gold/purge",
                Integer.valueOf(3), ShopDrawPath.probeSlice().get("drawCount"));
    }

    @Test
    public void treasureChromeLinesReflectChestState() {
        publishFrame("treasure", RestView.empty(), TreasureView.closed(), ShopView.empty());
        List<RoomChromeLine> closedLines = TreasureDrawPath.chromeLines();
        assertEquals(2, closedLines.size());
        assertEquals("Treasure", closedLines.get(0).text);
        assertEquals("chest", closedLines.get(1).id);
        assertEquals("Chest closed", closedLines.get(1).text);
        assertTrue(closedLines.get(1).enabled);

        publishFrame("treasure", RestView.empty(),
                TreasureView.opened("Bag of Marbles", "relic.Bag of Marbles"), ShopView.empty());
        List<RoomChromeLine> openLines = TreasureDrawPath.chromeLines();
        assertEquals(2, openLines.size());
        assertEquals("relic", openLines.get(1).id);
        assertEquals("Bag of Marbles", openLines.get(1).text);
        assertEquals(Integer.valueOf(openLines.size()),
                TreasureDrawPath.probeSlice().get("chromeLineCount"));
    }

    @Test
    public void chromeLinesStayEmptyWhenViewUnavailable() {
        publishFrame("rest", RestView.empty(), TreasureView.empty(), ShopView.empty());
        assertTrue(RestDrawPath.chromeLines().isEmpty());
        assertEquals(Integer.valueOf(0), RestDrawPath.probeSlice().get("chromeLineCount"));

        publishFrame("shop", RestView.empty(), TreasureView.empty(), ShopView.empty());
        assertTrue(ShopDrawPath.chromeLines().isEmpty());
        assertEquals(Integer.valueOf(0), ShopDrawPath.probeSlice().get("chromeLineCount"));

        publishFrame("treasure", RestView.empty(), TreasureView.empty(), ShopView.empty());
        assertTrue(TreasureDrawPath.chromeLines().isEmpty());
        assertEquals(Integer.valueOf(0), TreasureDrawPath.probeSlice().get("chromeLineCount"));
    }

    @Test
    public void prepareRestVisualsMaterializesC2ItemsMatchingChromeLines() {
        publishFrame("rest",
                RestView.of(Arrays.asList(
                        new RestView.RestOptionView("rest", "Rest", true, true),
                        new RestView.RestOptionView("smith", "Smith", false, true))),
                TreasureView.empty(), ShopView.empty());
        fullReady(SurfaceIds.REST);
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.REST));

        Sts1SurfaceRenderer.prepareRestVisuals(plan);

        assertC2ItemsMatch(SurfaceIds.REST, RestDrawPath.chromeLines());
    }

    @Test
    public void prepareShopVisualsMaterializesC2ItemsMatchingChromeLines() {
        publishFrame("shop", RestView.empty(), TreasureView.empty(),
                ShopView.of(99, java.util.Collections.singletonList(
                        ShopView.ShopEntryView.of(0, "card", "Strike_R", 50)),
                        false, 0));
        fullReady(SurfaceIds.SHOP);
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.SHOP));

        Sts1SurfaceRenderer.prepareShopVisuals(plan);

        assertC2ItemsMatch(SurfaceIds.SHOP, ShopDrawPath.chromeLines());
    }

    @Test
    public void prepareShopVisualsClearsC2ItemsWhenNotFullReady() {
        publishFrame("shop", RestView.empty(), TreasureView.empty(),
                ShopView.of(99, java.util.Collections.singletonList(
                        ShopView.ShopEntryView.of(0, "card", "Strike_R", 50)),
                        false, 0));
        fullReady(SurfaceIds.SHOP);
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        Sts1SurfaceRenderer.prepareShopVisuals(plan);
        assertC2ItemsMatch(SurfaceIds.SHOP, ShopDrawPath.chromeLines());

        CombatInputRouter.resetForTests();
        Sts1SurfaceRenderer.prepareShopVisuals(Sts1RenderPipeline.plan());

        assertC2ItemsMatch(SurfaceIds.SHOP, java.util.Collections.<RoomChromeLine>emptyList());
    }

    @Test
    public void prepareTreasureVisualsMaterializesC2ItemsMatchingChromeLines() {
        publishFrame("treasure", RestView.empty(), TreasureView.closed(), ShopView.empty());
        fullReady(SurfaceIds.TREASURE);
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.TREASURE));

        Sts1SurfaceRenderer.prepareTreasureVisuals(plan);

        assertC2ItemsMatch(SurfaceIds.TREASURE, TreasureDrawPath.chromeLines());
    }

    @Test
    public void restEvidenceRecordsRealRowCount() {
        publishFrame("rest",
                RestView.of(java.util.Collections.singletonList(
                        new RestView.RestOptionView("rest", "Rest", true, true))),
                TreasureView.empty(), ShopView.empty());
        fullReady(SurfaceIds.REST);

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.REST, "com.megacrit.cardcrawl.rooms.CampfireUI", "render", "test");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);

        int rows = RestDrawPath.chromeLines().size();
        assertTrue(rows > 0);
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.REST, rows);

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(rows, evidence.drawCount);
        assertEquals(Integer.valueOf(0),
                NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void shopEvidenceRecordsRealRowCount() {
        publishFrame("shop", RestView.empty(), TreasureView.empty(),
                ShopView.of(150, Arrays.asList(
                        ShopView.ShopEntryView.of(0, "card", "Strike_R", 50),
                        ShopView.ShopEntryView.of(1, "relic", "Bag of Marbles", 150)),
                        true, 75));
        fullReady(SurfaceIds.SHOP);

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.SHOP, "com.megacrit.cardcrawl.shop.ShopScreen", "render", "test");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);

        int rows = ShopDrawPath.chromeLines().size();
        assertTrue(rows > 0);
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.SHOP, rows);

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(rows, evidence.drawCount);
        assertEquals("evidence count is one invocation, not a hard-coded row count",
                Integer.valueOf(1), NativeRenderBridge.probeSlice().get("evidenceCount"));
        assertEquals("draw evidence must carry title/gold/entries/purge real row count",
                5, evidence.drawCount);
        assertEquals(Integer.valueOf(0),
                NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void treasureEvidenceRecordsRealRowCount() {
        publishFrame("treasure", RestView.empty(), TreasureView.closed(), ShopView.empty());
        fullReady(SurfaceIds.TREASURE);

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.TREASURE, "com.megacrit.cardcrawl.rooms.TreasureRoom", "render", "test");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);

        int rows = TreasureDrawPath.chromeLines().size();
        assertTrue(rows > 0);
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.TREASURE, rows);

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(rows, evidence.drawCount);
        assertEquals(Integer.valueOf(0),
                NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    private static void assertC2ItemsMatch(String surfaceId, List<RoomChromeLine> lines) {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        String scope = "sts1.visual." + surfaceId;
        Set<String> ids = new LinkedHashSet<String>();
        Map<String, String> texts = new LinkedHashMap<String, String>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            if (identity == null || !scope.equals(identity.key.scope)) continue;
            ids.add(identity.key.localId);
            DrawComponent draw = context.world().get(entity, DrawComponent.class);
            texts.put(identity.key.localId, draw != null ? draw.text : null);
        }
        assertEquals("C2 items must mirror the projected chrome rows exactly",
                ids.size(), lines.size());
        for (RoomChromeLine line : lines) {
            assertTrue("missing C2 item " + line.id + " for " + surfaceId, ids.contains(line.id));
            assertEquals(line.text, texts.get(line.id));
        }
    }
}
