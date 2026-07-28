package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeBackend;
import artframework.context.MapNodeView;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.assets.Sts1HostAssets;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MapDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1HostAssets.resetForTests();
    }

    private void mapFrame() {
        Sts1HostAssets.install();
        FakeBackend backend = new FakeBackend();
        ArtFramework.bindPresentationBackend(backend);
        MapNodeView n =
                new MapNodeView(
                        1, 2, 100f, 200f, false, true, "M", "monster", ResourceIds.MAP_NODE_MONSTER);
        backend.pushFrame(
                ContextFrame.of(
                        1L,
                        1L,
                        "map",
                        null,
                        ControlsView.empty(),
                        new MapView(Collections.singletonList(n), 1920, 1080),
                        null));
        ArtFramework.frames().syncFromBackend();
        ArtFramework.component(SurfaceIds.MAP).mount();
    }

    @Test
    public void buildProjectsNodesWithPanZoom() {
        mapFrame();
        MapDrawPath.panZoom().setPan(10f, 20f);
        MapDrawPath.panZoom().setZoom(2f);
        List<MapDrawPath.DrawItem> items = MapDrawPath.buildFromProjection();
        assertEquals(1, items.size());
        assertEquals(1, items.get(0).row);
        assertEquals(2, items.get(0).col);
        assertEquals(100f * 2f + 10f, items.get(0).screenX, 0.01f);
        assertEquals(200f * 2f + 20f, items.get(0).screenY, 0.01f);
        assertTrue(items.get(0).artFound);
    }

    @Test
    public void hitTestFindsNode() {
        mapFrame();
        MapDrawPath.DrawItem hit = MapDrawPath.hitTest(100f, 200f, 30f);
        assertNotNull(hit);
        assertEquals(1, hit.row);
        assertNull(MapDrawPath.hitTest(0f, 0f, 5f));
    }

    @Test
    public void suppressOnlyWhenFullMapScene() {
        mapFrame();
        assertFalse(MapDrawPath.shouldSuppressNativeMap());
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        assertTrue(MapDrawPath.shouldSuppressNativeMap());
        FullPresentMode.setMapLevel(PresentLevel.OBSERVE);
        assertFalse(MapDrawPath.shouldSuppressNativeMap());
    }

    @Test
    public void panZoomLimits() {
        MapPanZoom pz = new MapPanZoom();
        pz.setZoom(10f);
        assertEquals(2.5f, pz.zoom(), 0.01f);
        pz.zoomBy(0.1f);
        assertTrue(pz.zoom() >= 0.5f);
    }

    @Test
    public void probeSlice() {
        mapFrame();
        FullPresentMode.setMapLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = MapDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), m.get("count"));
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals("map", m.get("scene"));
    }
}
