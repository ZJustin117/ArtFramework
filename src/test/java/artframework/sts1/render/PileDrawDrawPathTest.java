package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.PileSoulView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.backend.Sts1PileSoulProjection;
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

public class PileDrawDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        Sts1PileSoulProjection.resetForTests();
    }

    private void mountedCombat(int drawSize) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combat(3, 1, drawSize, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
    }

    private void publishDrawEntry(Rect bounds, int count, boolean visible) {
        Sts1PileSoulProjection.publish(new PileSoulView(
                Arrays.asList(new PileSoulView.Entry(
                        "pile:draw:Draw", "pile", "draw", count, bounds, visible,
                        ResourceIds.UI_PILE_DRAW, "Draw")),
                true));
    }

    @Test
    public void suppressesNativePileDrawOnlyWhenFullReady() {
        mountedCombat(7);
        assertFalse("OFF keeps native renderer", PileDrawDrawPath.shouldSuppressNativePileDraw());

        FullPresentMode.setPileDrawLevel(PresentLevel.FULL);
        assertFalse("FULL without executor stays native",
                PileDrawDrawPath.shouldSuppressNativePileDraw());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue("FULL + mounted + combat + ready executor suppresses native",
                PileDrawDrawPath.shouldSuppressNativePileDraw());

        Sts1RenderPipeline.setOverlayObserve(true);
        assertFalse("overlay-observe forces native visibility",
                PileDrawDrawPath.shouldSuppressNativePileDraw());
    }

    @Test
    public void surfaceDrawPlanSuppressesNativePileDrawWhenFullMountedCombat() {
        mountedCombat(7);
        FullPresentMode.setPileDrawLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldSuppressNative(SurfaceIds.COMBAT_PILE_DRAW));
    }

    @Test
    public void mountedCombatRootMountsPileDrawSurface() {
        mountedCombat(7);
        assertTrue(ArtFramework.component(SurfaceIds.COMBAT_PILE_DRAW).isMounted());
    }

    @Test
    public void probeSliceExposesStableShape() {
        mountedCombat(7);
        FullPresentMode.setPileDrawLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = PileDrawDrawPath.probeSlice();
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals(Boolean.FALSE, m.get("suppressNativePileDraw"));
        assertEquals(Integer.valueOf(7), m.get("drawCount"));
        assertEquals(Integer.valueOf(2), m.get("count"));
        assertEquals("OBSERVING", m.get("capability"));
        assertNotNull(m.get("capabilityReason"));
        assertTrue(m.get("items") instanceof List);
    }

    @Test
    public void twoItemsUseNativeTexturesAndGeometryNotProjectionBounds() {
        mountedCombat(12);
        // A deliberately wrong/observed projection: its bounds and label must NOT be used.
        Rect observed = new Rect(34f, 44f, 96f, 128f);
        publishDrawEntry(observed, 99, true);

        List<PileDrawDrawPath.DrawItem> items = PileDrawDrawPath.buildFromProjection();
        assertEquals(2, items.size());

        PileDrawDrawPath.DrawItem icon = items.get(0);
        assertEquals("pile:draw:icon", icon.id);
        assertEquals(ResourceIds.UI_PILE_DRAW, icon.resourceId);
        assertEquals("", icon.label);
        assertEquals(12, icon.count);
        Rect iconExpected = PileDrawDrawPath.iconBounds();
        assertEquals(PileDrawDrawPath.PANEL_SHOW_X + PileDrawDrawPath.deckX(),
                icon.bounds.x, 0.01f);
        assertEquals(PileDrawDrawPath.PANEL_SHOW_Y + PileDrawDrawPath.deckY(),
                icon.bounds.y, 0.01f);
        assertEquals(iconExpected.width, icon.bounds.width, 0.01f);
        assertEquals(iconExpected.height, icon.bounds.height, 0.01f);
        assertEquals(icon.bounds.width, icon.bounds.height, 0.01f);

        PileDrawDrawPath.DrawItem count = items.get(1);
        assertEquals("pile:draw:count", count.id);
        assertEquals(ResourceIds.UI_PILE_COUNT_CIRCLE, count.resourceId);
        assertEquals("12", count.label);
        assertEquals(12, count.count);
        assertEquals(PileDrawDrawPath.PANEL_SHOW_X + PileDrawDrawPath.countOffsetX(),
                count.bounds.x, 0.01f);
        assertEquals(PileDrawDrawPath.PANEL_SHOW_Y + PileDrawDrawPath.countOffsetY(),
                count.bounds.y, 0.01f);
        assertEquals(PileDrawDrawPath.countCircleBounds().width, count.bounds.width, 0.01f);
        assertEquals(PileDrawDrawPath.countCircleBounds().height, count.bounds.height, 0.01f);

        // Geometry is native, never the observed projection bounds.
        for (PileDrawDrawPath.DrawItem item : items) {
            assertFalse("observed projection x must not drive geometry",
                    Math.abs(item.bounds.x - observed.x) < 0.01f && item.bounds.width == observed.width);
        }
        assertEquals(Float.valueOf(PileDrawDrawPath.PANEL_SHOW_X + PileDrawDrawPath.deckX()),
                icon.toMap().get("x"));
    }

    @Test
    public void countFallsBackToProjectionDrawEntryWhenControlsDrawSizeAbsent() {
        mountedCombat(0);
        publishDrawEntry(new Rect(34f, 44f, 96f, 128f), 5, true);
        assertEquals(5, PileDrawDrawPath.drawCount());
        PileDrawDrawPath.DrawItem count = PileDrawDrawPath.buildFromProjection().get(1);
        assertEquals("5", count.label);
    }

    @Test
    public void geometryDoesNotComeFromProjection() {
        mountedCombat(9);
        // Even with a wildly different projection, native geometry is used.
        publishDrawEntry(new Rect(-999f, -999f, 12f, 12f), 9, true);
        PileDrawDrawPath.DrawItem icon = PileDrawDrawPath.buildFromProjection().get(0);
        assertEquals(PileDrawDrawPath.iconBounds().x, icon.bounds.x, 0.01f);
        assertEquals(PileDrawDrawPath.iconBounds().y, icon.bounds.y, 0.01f);
        PileDrawDrawPath.DrawItem count = PileDrawDrawPath.buildFromProjection().get(1);
        assertEquals(PileDrawDrawPath.countCircleBounds().x, count.bounds.x, 0.01f);
        assertEquals(PileDrawDrawPath.countCircleBounds().y, count.bounds.y, 0.01f);
        assertEquals("9", count.label);
    }

    @Test
    public void vanillaCatalogMapsPileDrawToExistingDeckButtonTexture() {
        // Regression guard: UI_PILE_DRAW must resolve to the existing native deckButton base,
        // never the non-existent images/ui/topPanel/cardPile.png.
        assertTrue("UI_PILE_DRAW must be known to the catalog",
                Sts1VanillaCatalog.isKnown(ResourceIds.UI_PILE_DRAW));
        String drawPath = Sts1VanillaCatalog.sourceFor(ResourceIds.UI_PILE_DRAW);
        assertTrue("UI_PILE_DRAW must map to deckButton/base.png, was " + drawPath,
                drawPath != null && drawPath.endsWith("images/ui/deckButton/base.png"));
        String circlePath = Sts1VanillaCatalog.sourceFor(ResourceIds.UI_PILE_COUNT_CIRCLE);
        assertTrue("UI_PILE_COUNT_CIRCLE must map to topPanel/countCircle.png, was " + circlePath,
                circlePath != null && circlePath.endsWith("images/ui/topPanel/countCircle.png"));
    }
}
