package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeBackend;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
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
        FakeBackend backend = new FakeBackend();
        ArtFramework.bindPresentationBackend(backend);
        backend.pushFrame(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        null,
                        ControlsView.combat(3, 5, 10, 0, 0, true, true),
                        MapView.empty(),
                        null));
        ArtFramework.frames().syncFromBackend();
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
    }

    @Test
    public void suppressOnlyWhenFullMountedCombat() {
        combatControlsFrame();
        assertFalse(ControlsDrawPath.shouldSuppressNativeEndTurn());
        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        assertTrue(ControlsDrawPath.shouldSuppressNativeEndTurn());
        Sts1RenderPipeline.setOverlayObserve(true);
        assertFalse(ControlsDrawPath.shouldSuppressNativeEndTurn());
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
}
