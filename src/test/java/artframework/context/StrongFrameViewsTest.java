package artframework.context;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class StrongFrameViewsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void strongControlsAndMapApplyToProjection() {
        ControlsView controls =
                ControlsView.combat(3, 5, 10, 2, 1, true, true);
        MapNodeView node =
                new MapNodeView(1, 2, 10f, 20f, false, true, "M", "monster", ResourceIds.mapNode("monster"));
        MapView map = new MapView(Collections.singletonList(node), 1920, 1080);
        ContextFrame frame =
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        Arrays.asList(
                                CardView.builder(new CardRef("c1", "Strike_R"))
                                        .zone(CardZone.HAND)
                                        .slot(0)
                                        .build()),
                        controls,
                        map,
                        new ViewportView(1920, 1080, 1920, 1080));

        FrameDiff diff = ArtFramework.publishFrame(frame);
        assertTrue(diff.applied);
        assertEquals(3, ArtFramework.projection().controls().energy);
        assertTrue(ArtFramework.projection().controls().endTurnEnabled);
        assertNotNull(ArtFramework.projection().controls().find(ControlsView.END_TURN_ID));
        assertEquals(1, ArtFramework.projection().map().nodeCount());
        assertEquals("monster", ArtFramework.projection().map().find(1, 2).roomKind);
        assertEquals(1920, ArtFramework.projection().viewport().logicalWidth);
        assertEquals(Boolean.TRUE, ArtFramework.projection().probeSlice().get("endTurnEnabled"));
        assertEquals(Integer.valueOf(1), ArtFramework.projection().probeSlice().get("mapNodeCount"));
    }

    @Test
    public void reframeSharesImmutableDomainViews() {
        CardView card = CardView.builder(new CardRef("c1", "Strike_R")).build();
        ContextFrame original = ContextFrame.of(
                1L,
                4L,
                "combat",
                Collections.singletonList(card),
                ControlsView.empty(),
                MapView.empty(),
                new ViewportView(100, 100, 100, 100));
        ContextFrame reframed = original.reframe(2L);

        assertEquals(2L, reframed.frameId);
        assertTrue(original.sameDomains(reframed));
        assertSame(original.cards, reframed.cards);
        assertSame(original.controlsView, reframed.controlsView);
        assertSame(original.mapView, reframed.mapView);
        assertSame(original.controls, reframed.controls);
        assertSame(original.map, reframed.map);
    }

    @Test
    public void legacyMapControlsCoerced() {
        Map<String, Object> rawControls = new LinkedHashMap<String, Object>();
        rawControls.put("energy", Integer.valueOf(2));
        rawControls.put("handSize", Integer.valueOf(4));
        rawControls.put("endTurnEnabled", Boolean.TRUE);
        Map<String, Object> rawMap = new LinkedHashMap<String, Object>();
        rawMap.put("viewportWidth", Integer.valueOf(800));
        rawMap.put("viewportHeight", Integer.valueOf(600));
        List<Map<String, Object>> nodes = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> n = new LinkedHashMap<String, Object>();
        n.put("row", Integer.valueOf(0));
        n.put("col", Integer.valueOf(1));
        n.put("symbol", "R");
        n.put("roomKind", "rest");
        n.put("resourceId", ResourceIds.mapNode("rest"));
        nodes.add(n);
        rawMap.put("nodes", nodes);

        ContextFrame frame =
                new ContextFrame(9L, 3L, "map", null, rawControls, rawMap, true, null);
        assertEquals(2, frame.controlsView.energy);
        assertTrue(frame.controlsView.endTurnEnabled);
        assertEquals(1, frame.mapView.nodeCount());
        assertEquals("rest", frame.mapView.find(0, 1).roomKind);
        assertEquals(Integer.valueOf(2), frame.controls.get("energy"));
        assertEquals(Integer.valueOf(1), frame.map.get("nodeCount"));
    }

    @Test
    public void unavailableDoesNotClearCardsOrChangeEpoch() {
        ArtFramework.publishFrame(
                ContextFrame.of(
                        1L,
                        5L,
                        "combat",
                        Arrays.asList(CardView.builder(new CardRef("keep", "Defend_R")).build()),
                        ControlsView.combat(1, 1, 0, 0, 0, true, true),
                        MapView.empty(),
                        new ViewportView(100, 100, 100, 100)));
        assertEquals(5L, ArtFramework.projection().sceneEpoch());
        assertEquals(1, ArtFramework.projection().size());

        FrameDiff d = ArtFramework.publishFrame(ContextFrame.unavailable(2L));
        assertFalse(d.applied);
        assertTrue(ArtFramework.projection().isStale());
        assertEquals(1, ArtFramework.projection().size());
        assertEquals(5L, ArtFramework.projection().sceneEpoch());
        assertNotNull(ArtFramework.projection().get("keep"));
    }

    @Test
    public void sceneEpochClearsDragAndCards() {
        ArtFramework.publishFrame(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        Arrays.asList(CardView.builder(new CardRef("old", "Strike_R")).build()),
                        ControlsView.empty(),
                        MapView.empty(),
                        null));
        ArtFramework.projection().setDragInstanceId("old");

        FrameDiff diff =
                ArtFramework.publishFrame(
                        ContextFrame.of(
                                1L,
                                2L,
                                "map",
                                Arrays.<CardView>asList(),
                                ControlsView.empty(),
                                new MapView(
                                        Collections.singletonList(
                                                new MapNodeView(
                                                        0, 0, 0, 0, false, false, "M", "monster", "")),
                                        0,
                                        0),
                                null));
        assertTrue(diff.applied);
        assertEquals(0, ArtFramework.projection().size());
        assertNull(ArtFramework.projection().dragInstanceId());
        assertEquals(2L, ArtFramework.projection().sceneEpoch());
        assertEquals(1, ArtFramework.projection().map().nodeCount());
    }

    @Test
    public void sameEpochFrameIdMonotonic() {
        ArtFramework.publishFrame(
                ContextFrame.of(5L, 1L, "combat", Arrays.<CardView>asList(), ControlsView.empty(), MapView.empty(), null));
        FrameDiff stale =
                ArtFramework.publishFrame(
                        ContextFrame.of(
                                3L, 1L, "combat", Arrays.<CardView>asList(), ControlsView.empty(), MapView.empty(), null));
        assertFalse(stale.applied);
        assertTrue(stale.message.contains("stale"));
    }
}
