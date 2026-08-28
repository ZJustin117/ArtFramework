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
}
