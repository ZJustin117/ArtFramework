package artframework.sts1.backend;

import artframework.context.RestView;
import artframework.context.RewardItemView;
import artframework.context.RewardView;
import artframework.context.ShopView;
import artframework.context.TreasureView;
import artframework.context.RoomShellView;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import com.megacrit.cardcrawl.helpers.Hitbox;
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

    @Test
    public void roomShellViewIsImmutableAndDependencyNeutral() {
        RoomShellView view = new RoomShellView("generic", "room-1", "Room", "COMPLETE",
                true, new Rect(1f, 2f, 3f, 4f), ResourceIds.UI_ROOM_SHELL_GENERIC, true);
        assertEquals("generic", view.kind);
        assertEquals("room-1", view.id);
        assertEquals("COMPLETE", view.phase);
        assertEquals(3f, view.bounds.width, 0.01f);
        assertTrue(view.available);
        assertTrue(view.visible);
    }

    // --- Event (G2) stubs ---

    public static class StubEvent extends com.megacrit.cardcrawl.events.AbstractEvent {
        @Override
        protected void buttonEffect(int buttonPressed) {
        }
    }

    public static class StubGenericEventDialog extends com.megacrit.cardcrawl.events.GenericEventDialog {
        @SuppressWarnings("unchecked")
        public StubGenericEventDialog() {
            this.optionList = new java.util.ArrayList();
        }
    }

    public static class StubEventButton {
        public String msg;
        public boolean isDisabled;
        public com.megacrit.cardcrawl.helpers.Hitbox hb;
    }

    @Test
    public void readEventOptionsProjectsButtonGeometry() {
        StubEvent event = new StubEvent();
        StubGenericEventDialog dialog = new StubGenericEventDialog();
        StubEventButton btn = new StubEventButton();
        btn.msg = "Talk";
        btn.isDisabled = false;
        btn.hb = new com.megacrit.cardcrawl.helpers.Hitbox(10f, 20f, 100f, 30f);
        @SuppressWarnings("unchecked")
        java.util.List rawList = dialog.optionList;
        rawList.add(btn);
        event.imageEventText = dialog;

        java.util.List<artframework.context.EventOptionView> options =
                Sts1PresentationBackend.readEventOptions(event);

        assertEquals(1, options.size());
        artframework.context.EventOptionView opt = options.get(0);
        assertEquals(0, opt.index);
        assertEquals("Talk", opt.label);
        assertTrue(opt.enabled);
        assertEquals(10f, opt.x, 0.01f);
        assertEquals(20f, opt.y, 0.01f);
        assertEquals(100f, opt.w, 0.01f);
        assertEquals(30f, opt.h, 0.01f);
    }

    @Test
    public void readEventOptionsFallsBackWhenDialogIsUnreadable() {
        StubEvent event = new StubEvent();
        java.util.List<artframework.context.EventOptionView> options =
                Sts1PresentationBackend.readEventOptions(event);
        assertEquals(2, options.size());
        assertEquals(0f, options.get(0).x, 0.01f);
        assertTrue(options.get(0).enabled);
    }

    // --- Reward (G3) stubs ---

    public static class StubCombatRewardScreen {
        public final java.util.List<Object> rewards = new java.util.ArrayList<Object>();
    }

    public static class StubRewardScreenItem {
        public String text;
        public Hitbox hb;
    }

    @Test
    public void readRewardViewProjectsItemGeometry() {
        StubCombatRewardScreen screen = new StubCombatRewardScreen();
        StubRewardScreenItem gold = new StubRewardScreenItem();
        gold.text = "30 Gold";
        gold.hb = new Hitbox(5f, 6f, 100f, 30f);
        screen.rewards.add(gold);

        RewardView view = Sts1PresentationBackend.readRewardView(screen);

        assertEquals(1, view.items.size());
        RewardItemView item = view.items.get(0);
        assertEquals(0, item.index);
        assertEquals("30 Gold", item.label);
        assertEquals("StubRewardScreenItem", item.kind);
        assertEquals(5f, item.x, 0.01f);
        assertEquals(6f, item.y, 0.01f);
        assertEquals(100f, item.w, 0.01f);
        assertEquals(30f, item.h, 0.01f);
    }

    @Test
    public void readRewardViewFallsBackWhenScreenIsEmpty() {
        RewardView view = Sts1PresentationBackend.readRewardView(new StubCombatRewardScreen());
        assertFalse(view.available);
        assertEquals(0, view.itemCount());
    }

    // --- Shop (G4) stubs ---

    public static class StubShopCard {
        public String name = "Strike_R";
        public String cardID = "Strike_R";
        public int price = 50;
        public boolean isPurchased = false;
    }

    public static class StubRelicInfo {
        public String name = "Bag of Marbles";
        public String relicId = "Bag of Marbles";
    }

    public static class StubShopRelic {
        public Object relic;
        public int price = 150;
        public boolean isPurchased = false;
    }

    public static class StubPotionInfo {
        public String name = "Fire Potion";
        public String ID = "Fire Potion";
    }

    public static class StubShopPotion {
        public Object potion;
        public int price = 75;
        public boolean isPurchased = false;
    }

    public static class StubShopScreen {
        public final java.util.List<Object> coloredCards = new java.util.ArrayList<Object>();
        public final java.util.List<Object> colorlessCards = new java.util.ArrayList<Object>();
        public final java.util.List<Object> relics = new java.util.ArrayList<Object>();
        public final java.util.List<Object> potions = new java.util.ArrayList<Object>();
        public boolean purgeAvailable = true;
        public int actualPurgeCost = 125;
    }

    @Test
    public void readShopViewProjectsLiveInventoryAndPrices() {
        StubShopScreen screen = new StubShopScreen();
        screen.coloredCards.add(new StubShopCard());
        StubShopCard colorless = new StubShopCard();
        colorless.name = "Dramatic Entrance";
        colorless.cardID = "Dramatic Entrance";
        colorless.price = 60;
        screen.colorlessCards.add(colorless);
        StubShopRelic relic = new StubShopRelic();
        relic.relic = new StubRelicInfo();
        relic.isPurchased = true;
        screen.relics.add(relic);
        StubShopPotion potion = new StubShopPotion();
        potion.potion = new StubPotionInfo();
        screen.potions.add(potion);

        ShopView view = Sts1PresentationBackend.readShopView(screen, 200);

        assertTrue(view.available);
        assertEquals(200, view.gold);
        assertEquals(4, view.entryCount());
        assertEquals("card", view.entries.get(0).kind);
        assertEquals("Strike_R", view.entries.get(0).label);
        assertEquals(50, view.entries.get(0).cost);
        assertFalse("unpurchased card must project as enabled/not sold-out",
                view.entries.get(0).soldOut);
        assertEquals("card.art.Dramatic Entrance", view.entries.get(1).resourceId);
        assertEquals("relic", view.entries.get(2).kind);
        assertEquals("Bag of Marbles", view.entries.get(2).label);
        assertEquals(150, view.entries.get(2).cost);
        assertTrue("purchased relic must project sold-out", view.entries.get(2).soldOut);
        assertEquals("potion", view.entries.get(3).kind);
        assertEquals("Fire Potion", view.entries.get(3).label);
        assertEquals(75, view.entries.get(3).cost);
        assertEquals("potion.Fire Potion", view.entries.get(3).resourceId);
        assertEquals(125, view.purgeCost);
        assertTrue(view.purgeAvailable);
    }

    @Test
    public void readShopViewFallsBackWhenScreenIsNull() {
        ShopView view = Sts1PresentationBackend.readShopView(null, 50);
        assertFalse(view.available);
        assertEquals(0, view.entryCount());
    }
}
