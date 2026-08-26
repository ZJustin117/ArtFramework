package artframework.sts1.backend;

import artframework.context.RestView;
import artframework.context.TreasureView;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure JUnit for the Slice C G5/G6 backend soft reads. The live host objects are stubbed with
 * reflection-compatible field names so the soft-read seams stay testable without GL or a
 * running dungeon.
 */
public class Sts1PresentationBackendRoomViewsTest {

    // --- Rest (G5) stubs: class simple names drive option identity (XxxOption -> xxx). ---

    public static class StubRestRoom {
        public Object campfireUI;
    }

    public static class StubCampfireUI {
        public final List<Object> buttons = new ArrayList<Object>();
    }

    public static class RestOption {
        public String label = "";
        public boolean usable = true;
    }

    public static class SmithOption {
        public String label = "";
        public boolean usable = true;
    }

    public static class RecallOption {
        public String label = "";
        public boolean usable = true;
    }

    // --- Treasure (G6) stubs ---

    public enum StubRewardType { RELIC, GOLD }

    public static class StubRelic {
        public String name = "";
        public String relicId = "";
    }

    public static class StubRewardItem {
        public StubRewardType type;
        public StubRelic relic;
    }

    public static class StubChest {
        public boolean isOpen;
    }

    public static class StubTreasureRoom {
        public Object chest;
        public final List<Object> rewards = new ArrayList<Object>();
    }

    @Test
    public void liveRestViewProjectsLiveButtonsWithAvailability() {
        StubCampfireUI ui = new StubCampfireUI();
        RestOption rest = new RestOption();
        rest.label = "Rest";
        SmithOption smith = new SmithOption();
        smith.label = "Smith";
        RecallOption recall = new RecallOption();
        recall.label = "Recall";
        recall.usable = false;
        ui.buttons.addAll(Arrays.<Object>asList(rest, smith, recall));
        StubRestRoom room = new StubRestRoom();
        room.campfireUI = ui;

        RestView view = Sts1PresentationBackend.liveRestView(room);

        assertTrue(view.available);
        assertEquals(3, view.optionCount());
        assertEquals("rest", view.options.get(0).id);
        assertEquals("Rest", view.options.get(0).label);
        assertTrue(view.options.get(0).enabled);
        assertEquals("smith", view.options.get(1).id);
        assertEquals("Smith", view.options.get(1).label);
        assertTrue(view.options.get(1).enabled);
        assertEquals("recall", view.options.get(2).id);
        assertEquals("Recall", view.options.get(2).label);
        assertFalse("unusable native button must project disabled",
                view.options.get(2).enabled);
        assertTrue(view.options.get(0).visible);
    }

    @Test
    public void liveRestViewSkipsNullButtonsAndToleratesMissingFields() {
        StubCampfireUI ui = new StubCampfireUI();
        ui.buttons.add(null);
        RestOption rest = new RestOption(); // no label set on purpose
        ui.buttons.add(rest);
        StubRestRoom room = new StubRestRoom();
        room.campfireUI = ui;

        RestView view = Sts1PresentationBackend.liveRestView(room);

        assertTrue(view.available);
        assertEquals(1, view.optionCount());
        assertEquals("rest", view.options.get(0).id);
        assertEquals("", view.options.get(0).label);
        assertTrue(view.options.get(0).enabled);
    }

    @Test
    public void liveRestViewFallsBackWhenUiOrButtonsMissing() {
        assertNull(Sts1PresentationBackend.liveRestView(null));

        StubRestRoom room = new StubRestRoom();
        assertNull(Sts1PresentationBackend.liveRestView(room));

        room.campfireUI = new StubCampfireUI();
        assertNull("empty button list must fall back to the caller's default",
                Sts1PresentationBackend.liveRestView(room));
    }

    @Test
    public void liveTreasureViewReportsClosedChest() {
        StubTreasureRoom room = new StubTreasureRoom();
        StubChest chest = new StubChest();
        chest.isOpen = false;
        room.chest = chest;

        TreasureView view = Sts1PresentationBackend.liveTreasureView(room);

        assertEquals(TreasureView.closed().chestOpen, view.chestOpen);
        assertFalse(view.chestOpen);
        assertTrue(view.canOpen);
        assertTrue(view.available);
        assertEquals("", view.relicLabel);
    }

    @Test
    public void liveTreasureViewProjectsOpenedChestRelic() {
        StubTreasureRoom room = new StubTreasureRoom();
        StubChest chest = new StubChest();
        chest.isOpen = true;
        room.chest = chest;
        StubRewardItem gold = new StubRewardItem();
        gold.type = StubRewardType.GOLD;
        StubRelic relic = new StubRelic();
        relic.name = "Bag of Marbles";
        relic.relicId = "Bag of Marbles";
        StubRewardItem relicReward = new StubRewardItem();
        relicReward.type = StubRewardType.RELIC;
        relicReward.relic = relic;
        room.rewards.addAll(Arrays.<Object>asList(gold, relicReward));

        TreasureView view = Sts1PresentationBackend.liveTreasureView(room);

        assertTrue(view.chestOpen);
        assertFalse(view.canOpen);
        assertTrue(view.available);
        assertEquals("Bag of Marbles", view.relicLabel);
        assertEquals("relic.Bag of Marbles", view.relicResourceId);
    }

    @Test
    public void liveTreasureViewOpenedWithoutReadableRelicKeepsEmptyLabels() {
        StubTreasureRoom room = new StubTreasureRoom();
        StubChest chest = new StubChest();
        chest.isOpen = true;
        room.chest = chest;

        TreasureView view = Sts1PresentationBackend.liveTreasureView(room);

        assertTrue(view.chestOpen);
        assertEquals("", view.relicLabel);
        assertEquals("", view.relicResourceId);
    }

    @Test
    public void liveTreasureViewFallsBackWhenChestMissing() {
        assertNull(Sts1PresentationBackend.liveTreasureView(null));

        StubTreasureRoom room = new StubTreasureRoom();
        assertNull(Sts1PresentationBackend.liveTreasureView(room));

        // A chest object without a readable isOpen field degrades to a closed chest.
        room.chest = new Object();
        TreasureView degraded = Sts1PresentationBackend.liveTreasureView(room);
        assertFalse(degraded.chestOpen);
        assertTrue(degraded.available);
    }

    @Test
    public void singletonInstanceSharesTheSoftReadSeams() {
        // The backend is a stateless read adapter for these seams; guard against accidental
        // per-instance state creeping into the projections.
        StubRestRoom room = new StubRestRoom();
        StubCampfireUI ui = new StubCampfireUI();
        RestOption rest = new RestOption();
        rest.label = "Rest";
        ui.buttons.addAll(Collections.<Object>singletonList(rest));
        room.campfireUI = ui;
        RestView first = Sts1PresentationBackend.liveRestView(room);
        RestView second = Sts1PresentationBackend.liveRestView(room);
        assertEquals(first.optionCount(), second.optionCount());
    }
}
