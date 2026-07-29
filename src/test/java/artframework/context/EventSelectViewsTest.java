package artframework.context;

import artframework.api.ArtFramework;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Milestone 22.1: EventView / SelectView on ContextFrame + projection. */
public class EventSelectViewsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void emptyViewsDefaultOnFrame() {
        ContextFrame frame = ContextFrame.of(1L, "combat", null);
        assertFalse(frame.eventView.available);
        assertEquals(0, frame.eventView.optionCount());
        assertFalse(frame.selectView.available);
        assertEquals(0, frame.selectView.poolCount());
    }

    @Test
    public void eventAndSelectApplyToProjection() {
        EventView event =
                EventView.of(
                        "Neow",
                        Arrays.asList(
                                EventOptionView.of(0, "Talk", true),
                                EventOptionView.of(1, "Leave", true)));
        SelectView select =
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("s1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .selected(true)
                                        .build()),
                        Collections.singletonList("s1"),
                        true,
                        true);
        ContextFrame frame =
                ContextFrame.of(
                        2L,
                        1L,
                        "event",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        event,
                        select,
                        null);
        FrameDiff diff = ArtFramework.publishFrame(frame);
        assertTrue(diff.applied);
        assertEquals(2, ArtFramework.projection().event().optionCount());
        assertEquals("Leave", ArtFramework.projection().event().find(1).label);
        assertEquals(SelectView.KIND_GRID, ArtFramework.projection().select().kind);
        assertEquals(1, ArtFramework.projection().select().poolCount());
        assertTrue(ArtFramework.projection().select().isSelected("s1"));
        Map<String, Object> probe = ArtFramework.projection().probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("eventOptionCount"));
        assertEquals(Integer.valueOf(1), probe.get("selectPoolCount"));
        assertEquals(SelectView.KIND_GRID, probe.get("selectKind"));
        assertNotNull(event.toMap().get("options"));
        assertNotNull(select.toMap().get("pool"));
    }
}
