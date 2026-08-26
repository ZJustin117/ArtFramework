package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.assets.Sts1HostAssets;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ControlsDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1HostAssets.resetForTests();
        Sts1RenderPipeline.resetForTests();
    }

    private void combatControlsFrame() {
        Sts1HostAssets.install();
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        null,
                        ControlsView.combat(3, 5, 10, 0, 0, true, true),
                        MapView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).mount();
    }

    @Test
    public void buildIncludesEndTurn() {
        combatControlsFrame();
        List<ControlsDrawPath.DrawItem> items = ControlsDrawPath.buildFromProjection();
        assertFalse(items.isEmpty());
        assertEquals(ControlsView.END_TURN_ID, items.get(0).id);
        assertTrue(items.get(0).enabled);
        assertTrue(items.get(0).visible);
        assertTrue(items.get(0).iconFound);
        assertTrue(items.get(0).iconSource.startsWith("sts1:images/"));
    }

    @Test
    public void endTurnBoundsMatchCombatControlPlacement() {
        artframework.component.Rect bounds = ControlsDrawPath.endTurnBounds(1920f, 1080f);
        assertEquals(1440f, bounds.x, 0.01f);
        assertEquals(54f, bounds.y, 0.01f);
        assertEquals(384f, bounds.width, 0.01f);
        assertEquals(151.2f, bounds.height, 0.01f);
    }

    @Test
    public void suppressesNativeEndTurnOnlyWhenFullReady() {
        combatControlsFrame();
        assertFalse("OFF keeps native renderer", ControlsDrawPath.shouldSuppressNativeEndTurn());

        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        assertFalse("FULL without executor stays native", ControlsDrawPath.shouldSuppressNativeEndTurn());

        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue("FULL + mounted + combat + ready executor suppresses native",
                ControlsDrawPath.shouldSuppressNativeEndTurn());

        Sts1RenderPipeline.setOverlayObserve(true);
        assertFalse("overlay-observe forces native visibility", ControlsDrawPath.shouldSuppressNativeEndTurn());
    }

    @Test
    public void surfaceDrawPlanSuppressesNativeControlsWhenFullMountedCombat() {
        combatControlsFrame();
        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldSuppressNative(SurfaceIds.COMBAT_CONTROLS));
    }

    @Test
    public void probeSlice() {
        combatControlsFrame();
        FullPresentMode.setCombatControlsLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = ControlsDrawPath.probeSlice();
        assertEquals(Integer.valueOf(3), m.get("energy"));
        assertEquals(Boolean.TRUE, m.get("endTurnEnabled"));
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals(Boolean.FALSE, m.get("suppressNativeEndTurn"));
    }

    private void projectedControlsFrame() {
        Sts1HostAssets.install();
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        null,
                        ControlsView.combat(3, 5, 10, 0, 0, true, true, 111f, 222f, 333f, 44f, "结束回合"),
                        MapView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).mount();
    }

    @Test
    public void projectedGeometryWinsOverConstantFallback() {
        projectedControlsFrame();
        artframework.component.Rect bounds = ControlsDrawPath.endTurnProjectedBounds(1920f, 1080f);
        assertEquals(111f, bounds.x, 0.01f);
        assertEquals(222f, bounds.y, 0.01f);
        assertEquals(333f, bounds.width, 0.01f);
        assertEquals(44f, bounds.height, 0.01f);
    }

    @Test
    public void missingProjectionFallsBackToConstantBounds() {
        combatControlsFrame();
        artframework.component.Rect projected =
                ControlsDrawPath.endTurnProjectedBounds(1920f, 1080f);
        artframework.component.Rect constant = ControlsDrawPath.endTurnBounds(1920f, 1080f);
        assertEquals(constant.x, projected.x, 0.01f);
        assertEquals(constant.y, projected.y, 0.01f);
        assertEquals(constant.width, projected.width, 0.01f);
        assertEquals(constant.height, projected.height, 0.01f);
    }

    @Test
    public void projectedLabelFlowsIntoEndTurnDrawItem() {
        projectedControlsFrame();
        List<ControlsDrawPath.DrawItem> items = ControlsDrawPath.buildFromProjection();
        assertFalse(items.isEmpty());
        assertEquals("结束回合", items.get(0).text);

        combatControlsFrame();
        List<ControlsDrawPath.DrawItem> fallbackItems = ControlsDrawPath.buildFromProjection();
        assertEquals("End Turn", fallbackItems.get(0).text);
    }

    @Test
    public void endTurnChromeReflectionBridgeRemoved() {
        try {
            Class.forName("artframework.sts1.render.Sts1EndTurnChrome");
            org.junit.Assert.fail("render path must not read native end-turn state directly");
        } catch (ClassNotFoundException expected) {
        }
    }
}
