package artframework.sts1.backend;

import artframework.assets.ResourceIds;
import artframework.context.BackendMode;
import artframework.context.CardPose;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.IntentResult;
import artframework.context.MapNodeView;
import artframework.context.MapView;
import artframework.context.MonsterIntentView;
import artframework.context.RestView;
import artframework.context.RewardItemView;
import artframework.context.RewardView;
import artframework.context.SelectView;
import artframework.context.ShopView;
import artframework.context.SignalBackend;
import artframework.context.TopPanelView;
import artframework.context.TreasureView;
import artframework.context.UiIntent;
import artframework.context.ViewportView;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * STS1 read-only authority adapter. Observation-only until the matching full-present surface owns
 * native input and has a tested intent executor ({@link FullPresentMode#mayOwnInput}).
 */
public final class Sts1PresentationBackend implements SignalBackend {

    public static final Sts1PresentationBackend INSTANCE = new Sts1PresentationBackend();

    private long frameId;
    private String scene = "";
    private long sceneEpoch;
    private artframework.core.SignalSubscription actionSubscription;
    private final Map<String, PlayabilityState> playability = new HashMap<String, PlayabilityState>();
    private final Map<String, CardView> cardViews = new HashMap<String, CardView>();
    private ContextFrame lastPublished;
    private long combatRevision = Long.MIN_VALUE;

    private Sts1PresentationBackend() {}

    public String id() { return "sts1"; }
    public BackendMode mode() { return BackendMode.READ_ONLY; }

    public ContextFrame publishFrame() {
        // Never throw from the host post-update path; embark / room transitions can leave
        // player non-null while currMapNode/room is still null.
        try {
            String nextScene = scene();
            if (!nextScene.equals(scene)) {
                scene = nextScene;
                // Unavailable / empty scene must not bump epoch (16.0 policy).
                if (!nextScene.isEmpty()) {
                    sceneEpoch++;
                }
            }
            frameId++;
            if ("combat".equals(scene)) {
                return publish(combatFrame());
            }
            if ("map".equals(scene)) {
                return publish(mapFrame());
            }
            if ("event".equals(scene)) {
                return publish(eventFrame());
            }
            if ("select".equals(scene)) {
                return publish(selectOnlyFrame());
            }
            if ("reward".equals(scene)) {
                return publish(rewardFrame());
            }
            if ("rest".equals(scene)) {
                return publish(restFrame());
            }
            if ("treasure".equals(scene)) {
                return publish(treasureFrame());
            }
            if ("shop".equals(scene)) {
                return publish(shopFrame());
            }
            return publish(ContextFrame.unavailable(frameId));
        } catch (Throwable t) {
            frameId++;
            return ContextFrame.unavailable(frameId);
        }
    }

    private ContextFrame publish(ContextFrame candidate) {
        if (lastPublished != null && lastPublished.sameDomains(candidate)) {
            lastPublished = lastPublished.reframe(candidate.frameId);
        } else {
            lastPublished = candidate;
        }
        return lastPublished;
    }

    @Override
    public void installSignals() {
        if (actionSubscription != null && actionSubscription.isConnected()) return;
        actionSubscription = artframework.core.SignalBuses.get().connect(
                java.util.regex.Pattern.compile("^ui/.+/.+$"),
                new artframework.core.SignalListener() {
                    @Override public artframework.core.SignalDecision onSignal(artframework.core.UiSignal signal) {
                        if (!(signal.payload instanceof UiIntent)) {
                            return artframework.core.SignalDecision.continueSignal();
                        }
                        IntentResult result = artframework.sts1.input.CombatInputRouter
                                .executeIfOwned((UiIntent) signal.payload);
                        // ACCEPTED and QUEUED both leave the action chain open; only REJECTED stops.
                        if (result != null && result.status == IntentResult.Status.REJECTED) {
                            return artframework.core.SignalDecision.stopRejected(result.message);
                        }
                        return artframework.core.SignalDecision.continueSignal();
                    }
                });
    }

    @Override
    public void uninstallSignals() {
        if (actionSubscription != null) actionSubscription.disconnect();
        actionSubscription = null;
    }

    public long sceneEpoch() {
        return sceneEpoch;
    }

    private ContextFrame combatFrame() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return ContextFrame.unavailable(frameId);
        }
        long nextCombatRevision = combatRevision(player);
        List<CardView> cards = new ArrayList<CardView>();
        Set<String> seenCards = new HashSet<String>();
        append(cards, seenCards, player.hand, CardZone.HAND, nextCombatRevision);
        append(cards, seenCards, player.drawPile, CardZone.DRAW, nextCombatRevision);
        append(cards, seenCards, player.discardPile, CardZone.DISCARD, nextCombatRevision);
        append(cards, seenCards, player.exhaustPile, CardZone.EXHAUST, nextCombatRevision);
        for (String id : new ArrayList<String>(cardViews.keySet())) {
            if (!seenCards.contains(id)) {
                cardViews.remove(id);
                playability.remove(id);
            }
        }

        boolean endTurnEnabled = AbstractDungeon.overlayMenu != null
                && AbstractDungeon.overlayMenu.endTurnButton != null
                && AbstractDungeon.overlayMenu.endTurnButton.enabled;
        boolean endTurnVisible = AbstractDungeon.overlayMenu != null
                && AbstractDungeon.overlayMenu.endTurnButton != null;
        boolean proceedVisible = false;
        boolean proceedEnabled = false;
        try {
            AbstractRoom curr = safeCurrRoom();
            if (curr != null && curr.isBattleOver) {
                proceedVisible = true;
                proceedEnabled = true;
            }
        } catch (Throwable ignored) {
        }
        ControlsView controls =
                ControlsView.combatWithProceed(
                        player.energy != null ? player.energy.energy : 0,
                        player.hand != null ? player.hand.size() : 0,
                        player.drawPile != null ? player.drawPile.size() : 0,
                        player.discardPile != null ? player.discardPile.size() : 0,
                        player.exhaustPile != null ? player.exhaustPile.size() : 0,
                        endTurnEnabled,
                        endTurnVisible,
                        proceedEnabled,
                        proceedVisible,
                        false,
                        false);
        ViewportView viewport =
                new ViewportView(
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT,
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT);
        publishSkeletonFrame();
        return ContextFrame.ofFull(
                frameId,
                sceneEpoch,
                "combat",
                cards,
                controls,
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                RewardView.empty(),
                RestView.empty(),
                TreasureView.empty(),
                ShopView.empty(),
                readTopPanelView(),
                readIntentsView(),
                viewport);
    }

    private void publishSkeletonFrame() {
        try {
            artframework.sts1.skeleton.Sts1CreatureSkeletonSnapshots.publish(frameId);
        } catch (Throwable ignored) {
        }
    }

    private ContextFrame mapFrame() {
        List<MapNodeView> nodes = new ArrayList<MapNodeView>();
        if (AbstractDungeon.map != null) {
            for (List<MapRoomNode> row : AbstractDungeon.map) {
                if (row == null) {
                    continue;
                }
                for (MapRoomNode node : row) {
                    if (node == null || !isPresentableMapNode(node)) {
                        continue;
                    }
                    String kind = roomKind(node);
                    float x = node.hb != null ? node.hb.cX : node.offsetX;
                    float y = node.hb != null ? node.hb.cY : node.offsetY;
                    nodes.add(
                            new MapNodeView(
                                    node.y,
                                    node.x,
                                    x,
                                    y,
                                    node.taken,
                                    node.highlighted,
                                    node.getRoomSymbol(Boolean.FALSE),
                                    kind,
                                    ResourceIds.mapNode(kind)));
                }
            }
        }
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        MapView map = new MapView(nodes, w, h);
        ViewportView viewport = new ViewportView(w, h, w, h);
        return new ContextFrame(
                frameId,
                sceneEpoch,
                "map",
                null,
                ControlsView.empty(),
                map,
                EventView.empty(),
                SelectView.empty(),
                true,
                viewport);
    }

    private ContextFrame eventFrame() {
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        ViewportView viewport = new ViewportView(w, h, w, h);
        return new ContextFrame(
                frameId,
                sceneEpoch,
                "event",
                null,
                ControlsView.empty(),
                MapView.empty(),
                readEventView(),
                SelectView.empty(),
                true,
                viewport);
    }

    private ContextFrame selectOnlyFrame() {
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        ViewportView viewport = new ViewportView(w, h, w, h);
        return new ContextFrame(
                frameId,
                sceneEpoch,
                "select",
                null,
                ControlsView.empty(),
                MapView.empty(),
                EventView.empty(),
                readSelectView(),
                true,
                viewport);
    }

    /** Attach live event/select/room views onto any available frame without changing scene. */
    private ContextFrame enrich(ContextFrame base) {
        if (base == null || !base.available) {
            return base;
        }
        // Scene samplers already own their live view. Only sample shared or transient domains
        // that are meaningful for this scene; do not probe every STS screen every post-update.
        EventView event = base.eventView;
        SelectView select = base.selectView;
        RewardView reward = base.rewardView;
        RestView rest = base.restView;
        TreasureView treasure = base.treasureView;
        ShopView shop = base.shopView;
        TopPanelView top = base.topPanelView;
        MonsterIntentView intents = base.intentsView;
        if ("combat".equals(base.scene)) {
            top = readTopPanelView();
            intents = readIntentsView();
        } else if ("map".equals(base.scene)) {
            top = readTopPanelView();
        } else if ("event".equals(base.scene)) {
            top = readTopPanelView();
        } else if ("select".equals(base.scene)) {
            select = readSelectView();
            top = readTopPanelView();
        } else if ("reward".equals(base.scene)) {
            reward = readRewardView();
            top = readTopPanelView();
        } else if ("rest".equals(base.scene)) {
            rest = readRestView();
            top = readTopPanelView();
        } else if ("treasure".equals(base.scene)) {
            treasure = readTreasureView();
            top = readTopPanelView();
        } else if ("shop".equals(base.scene)) {
            shop = readShopView();
            top = readTopPanelView();
        }
        if (sameView(base, event, select, reward, rest, treasure, shop, top, intents)) {
            return base;
        }
        return new ContextFrame(
                base.frameId,
                base.sceneEpoch,
                base.scene,
                base.cards,
                base.controlsView,
                base.mapView,
                event,
                select,
                reward,
                rest,
                treasure,
                shop,
                top,
                intents,
                true,
                base.viewport);
    }

    private static boolean sameView(
            ContextFrame base,
            EventView event,
            SelectView select,
            RewardView reward,
            RestView rest,
            TreasureView treasure,
            ShopView shop,
            TopPanelView top,
            MonsterIntentView intents) {
        return base.eventView == event
                && base.selectView == select
                && base.rewardView == reward
                && base.restView == rest
                && base.treasureView == treasure
                && base.shopView == shop
                && base.topPanelView == top
                && base.intentsView == intents;
    }

    private ContextFrame rewardFrame() {
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        return ContextFrame.ofFull(
                frameId,
                sceneEpoch,
                "reward",
                null,
                ControlsView.empty(),
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                readRewardView(),
                RestView.empty(),
                TreasureView.empty(),
                ShopView.empty(),
                readTopPanelView(),
                MonsterIntentView.empty(),
                new ViewportView(w, h, w, h));
    }

    private ContextFrame restFrame() {
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        return ContextFrame.ofFull(
                frameId,
                sceneEpoch,
                "rest",
                null,
                ControlsView.empty(),
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                RewardView.empty(),
                readRestView(),
                TreasureView.empty(),
                ShopView.empty(),
                readTopPanelView(),
                MonsterIntentView.empty(),
                new ViewportView(w, h, w, h));
    }

    private ContextFrame treasureFrame() {
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        return ContextFrame.ofFull(
                frameId,
                sceneEpoch,
                "treasure",
                null,
                ControlsView.empty(),
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                RewardView.empty(),
                RestView.empty(),
                readTreasureView(),
                ShopView.empty(),
                readTopPanelView(),
                MonsterIntentView.empty(),
                new ViewportView(w, h, w, h));
    }

    private ContextFrame shopFrame() {
        int w = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        return ContextFrame.ofFull(
                frameId,
                sceneEpoch,
                "shop",
                null,
                ControlsView.empty(),
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                RewardView.empty(),
                RestView.empty(),
                TreasureView.empty(),
                readShopView(),
                readTopPanelView(),
                MonsterIntentView.empty(),
                new ViewportView(w, h, w, h));
    }

    private static RewardView readRewardView() {
        try {
            if (AbstractDungeon.combatRewardScreen == null
                    || AbstractDungeon.combatRewardScreen.rewards == null
                    || AbstractDungeon.combatRewardScreen.rewards.isEmpty()) {
                return RewardView.empty();
            }
            List<RewardItemView> items = new ArrayList<RewardItemView>();
            int i = 0;
            for (Object raw : AbstractDungeon.combatRewardScreen.rewards) {
                String kind = "reward";
                String label = "Reward " + i;
                try {
                    kind = raw.getClass().getSimpleName();
                    java.lang.reflect.Field text = raw.getClass().getField("text");
                    Object t = text.get(raw);
                    if (t != null) {
                        label = String.valueOf(t);
                    }
                } catch (Throwable ignored) {
                }
                items.add(RewardItemView.of(i, kind, label));
                i++;
            }
            String kind = "combat";
            try {
                if (AbstractDungeon.cardRewardScreen != null
                        && AbstractDungeon.cardRewardScreen.rewardGroup != null
                        && !AbstractDungeon.cardRewardScreen.rewardGroup.isEmpty()) {
                    kind = "card";
                }
            } catch (Throwable ignored) {
            }
            return RewardView.of(kind, "Rewards", items);
        } catch (Throwable t) {
            return RewardView.empty();
        }
    }

    private static RestView readRestView() {
        try {
            AbstractRoom room = safeCurrRoom();
            if (room == null || !(room instanceof com.megacrit.cardcrawl.rooms.RestRoom)) {
                return RestView.empty();
            }
            List<RestView.RestOptionView> options = new ArrayList<RestView.RestOptionView>();
            options.add(RestView.RestOptionView.of("rest", "Rest"));
            options.add(RestView.RestOptionView.of("smith", "Smith"));
            options.add(RestView.RestOptionView.of("recall", "Recall"));
            return RestView.of(options);
        } catch (Throwable t) {
            return RestView.empty();
        }
    }

    private static TreasureView readTreasureView() {
        try {
            AbstractRoom room = safeCurrRoom();
            if (room == null || !(room instanceof com.megacrit.cardcrawl.rooms.TreasureRoom)) {
                return TreasureView.empty();
            }
            return TreasureView.closed();
        } catch (Throwable t) {
            return TreasureView.empty();
        }
    }

    private static ShopView readShopView() {
        try {
            AbstractRoom room = safeCurrRoom();
            if (room == null || !(room instanceof com.megacrit.cardcrawl.rooms.ShopRoom)) {
                return ShopView.empty();
            }
            int gold = AbstractDungeon.player != null ? AbstractDungeon.player.gold : 0;
            List<ShopView.ShopEntryView> entries = new ArrayList<ShopView.ShopEntryView>();
            entries.add(ShopView.ShopEntryView.of(0, "card", "Card", 50));
            entries.add(ShopView.ShopEntryView.of(1, "relic", "Relic", 150));
            return ShopView.of(gold, entries, true, 75);
        } catch (Throwable t) {
            return ShopView.empty();
        }
    }

    private static TopPanelView readTopPanelView() {
        try {
            if (AbstractDungeon.player == null) {
                return TopPanelView.empty();
            }
            AbstractPlayer p = AbstractDungeon.player;
            return TopPanelView.of(
                    p.currentHealth,
                    p.maxHealth,
                    p.gold,
                    AbstractDungeon.floorNum,
                    AbstractDungeon.ascensionLevel,
                    p.name != null ? p.name : p.getClass().getSimpleName());
        } catch (Throwable t) {
            return TopPanelView.empty();
        }
    }

    private static MonsterIntentView readIntentsView() {
        try {
            if (AbstractDungeon.getMonsters() == null
                    || AbstractDungeon.getMonsters().monsters == null) {
                return MonsterIntentView.empty();
            }
            List<MonsterIntentView.IntentEntry> entries = new ArrayList<MonsterIntentView.IntentEntry>();
            for (com.megacrit.cardcrawl.monsters.AbstractMonster m :
                    AbstractDungeon.getMonsters().monsters) {
                if (m == null || m.isDeadOrEscaped()) {
                    continue;
                }
                String intent = m.intent != null ? m.intent.name() : "";
                float x = m.hb != null ? m.hb.cX : m.drawX;
                float y = m.hb != null ? m.hb.cY + 80f : m.drawY + 80f;
                entries.add(
                        new MonsterIntentView.IntentEntry(
                                m.id != null ? m.id : m.name,
                                m.name != null ? m.name : "",
                                intent,
                                "",
                                0,
                                x,
                                y));
            }
            return MonsterIntentView.of(entries);
        } catch (Throwable t) {
            return MonsterIntentView.empty();
        }
    }

    private static EventView readEventView() {
        try {
            AbstractRoom room = safeCurrRoom();
            if (room == null || room.event == null) {
                return EventView.empty();
            }
            AbstractEvent ev = room.event;
            String title = ev.getClass().getSimpleName();
            List<EventOptionView> options = readEventOptions(ev);
            if (options.isEmpty()) {
                options.add(EventOptionView.of(0, "Continue", true));
            }
            return new EventView(title, options, true);
        } catch (Throwable t) {
            return EventView.empty();
        }
    }

    private static List<EventOptionView> readEventOptions(AbstractEvent ev) {
        List<EventOptionView> options = new ArrayList<EventOptionView>();
        try {
            com.megacrit.cardcrawl.events.GenericEventDialog dialog = ev.imageEventText;
            if (dialog != null && dialog.optionList != null && !dialog.optionList.isEmpty()) {
                for (int i = 0; i < dialog.optionList.size(); i++) {
                    Object btn = dialog.optionList.get(i);
                    String label = "Option " + i;
                    boolean enabled = true;
                    try {
                        java.lang.reflect.Field msg = btn.getClass().getDeclaredField("msg");
                        msg.setAccessible(true);
                        Object m = msg.get(btn);
                        if (m != null) {
                            label = String.valueOf(m);
                        }
                    } catch (Throwable ignored) {
                    }
                    try {
                        java.lang.reflect.Field dis = btn.getClass().getDeclaredField("isDisabled");
                        dis.setAccessible(true);
                        Object d = dis.get(btn);
                        if (d instanceof Boolean) {
                            enabled = !((Boolean) d).booleanValue();
                        }
                    } catch (Throwable ignored) {
                    }
                    options.add(EventOptionView.of(i, label, enabled));
                }
                return options;
            }
        } catch (Throwable ignored) {
        }
        if (options.isEmpty()) {
            options.add(EventOptionView.of(0, "Option 0", true));
            options.add(EventOptionView.of(1, "Option 1", true));
        }
        return options;
    }

    private static SelectView readSelectView() {
        try {
            if (AbstractDungeon.gridSelectScreen != null
                    && isSelectVisible(AbstractDungeon.gridSelectScreen)) {
                return readGridSelect(AbstractDungeon.gridSelectScreen);
            }
            if (AbstractDungeon.handCardSelectScreen != null
                    && isSelectVisible(AbstractDungeon.handCardSelectScreen)) {
                return readHandSelect(AbstractDungeon.handCardSelectScreen);
            }
        } catch (Throwable ignored) {
        }
        return SelectView.empty();
    }

    private static boolean isSelectVisible(Object screen) {
        try {
            Object forWhat = softField(screen.getClass(), screen, "forUpgrade");
            Object isShown = softField(screen.getClass(), screen, "isShown");
            if (isShown instanceof Boolean) {
                return ((Boolean) isShown).booleanValue();
            }
            Object target = softField(screen.getClass(), screen, "targetGroup");
            if (target instanceof CardGroup) {
                CardGroup g = (CardGroup) target;
                return g.group != null && !g.group.isEmpty();
            }
            Object num = softField(screen.getClass(), screen, "numCardsToSelect");
            if (num instanceof Number && ((Number) num).intValue() > 0) {
                return true;
            }
            return forWhat != null || target != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static SelectView readGridSelect(GridCardSelectScreen gcs) {
        List<CardView> pool = new ArrayList<CardView>();
        List<String> selected = new ArrayList<String>();
        if (gcs.targetGroup != null && gcs.targetGroup.group != null) {
            for (int i = 0; i < gcs.targetGroup.group.size(); i++) {
                AbstractCard card = gcs.targetGroup.group.get(i);
                if (card == null) {
                    continue;
                }
                String instance = card.uuid != null ? card.uuid.toString() : identity(card);
                boolean isSel = gcs.selectedCards != null && gcs.selectedCards.contains(card);
                if (isSel) {
                    selected.add(instance);
                }
                CardPose pose =
                        new CardPose(
                                card.current_x,
                                card.current_y,
                                card.angle,
                                card.drawScale,
                                i,
                                true);
                pool.add(
                        CardView.builder(new CardRef(instance, card.cardID))
                                .zone(CardZone.SELECT)
                                .slot(i)
                                .pose(pose)
                                .selected(isSel)
                                .art(ResourceIds.cardArt(card.cardID))
                                .build());
            }
        }
        boolean confirmVisible = gcs.confirmButton != null;
        boolean confirmEnabled =
                gcs.confirmButton != null && gcs.confirmButton.hb != null;
        return SelectView.grid(pool, selected, confirmEnabled, confirmVisible);
    }

    private static SelectView readHandSelect(HandCardSelectScreen hcs) {
        List<CardView> pool = new ArrayList<CardView>();
        List<String> selected = new ArrayList<String>();
        try {
            if (AbstractDungeon.player != null
                    && AbstractDungeon.player.hand != null
                    && AbstractDungeon.player.hand.group != null) {
                for (int i = 0; i < AbstractDungeon.player.hand.group.size(); i++) {
                    AbstractCard card = AbstractDungeon.player.hand.group.get(i);
                    if (card == null) {
                        continue;
                    }
                    String instance = card.uuid != null ? card.uuid.toString() : identity(card);
                    boolean isSel = card.isSelected;
                    if (isSel) {
                        selected.add(instance);
                    }
                    pool.add(
                            CardView.builder(new CardRef(instance, card.cardID))
                                    .zone(CardZone.SELECT)
                                    .slot(i)
                                    .selected(isSel)
                                    .art(ResourceIds.cardArt(card.cardID))
                                    .build());
                }
            }
        } catch (Throwable ignored) {
        }
        boolean confirmVisible = true;
        boolean confirmEnabled = !selected.isEmpty() || !pool.isEmpty();
        return SelectView.hand(pool, selected, confirmEnabled, confirmVisible);
    }

    private static Object softField(Class<?> clz, Object inst, String name) {
        try {
            java.lang.reflect.Field f = clz.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(inst);
        } catch (Throwable t) {
            try {
                if (clz.getSuperclass() != null) {
                    return softField(clz.getSuperclass(), inst, name);
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    private void append(
            List<CardView> out,
            Set<String> seenCards,
            CardGroup group,
            CardZone zone,
            long revision) {
        if (group == null || group.group == null) {
            return;
        }
        for (int i = 0; i < group.group.size(); i++) {
            AbstractCard card = group.group.get(i);
            if (card == null) {
                continue;
            }
            String instance = card.uuid != null ? card.uuid.toString() : identity(card);
            CardPose pose = new CardPose(
                    card.current_x,
                    card.current_y,
                    card.angle,
                    card.drawScale,
                    i,
                    !card.fadingOut);
            boolean playable = playable(card, zone, revision);
            CardView next = CardView.builder(new CardRef(instance, card.cardID))
                    .zone(zone)
                    .slot(i)
                    .pose(pose)
                    .playable(playable)
                    .selected(card.isSelected)
                    .hovered(card.hb != null && card.hb.hovered)
                    .dragging(AbstractDungeon.player != null && AbstractDungeon.player.isDraggingCard
                            && AbstractDungeon.player.hoveredCard == card)
                    .art(ResourceIds.cardArt(card.cardID))
                    .frame(ResourceIds.cardFrame(card.color != null ? card.color.name().toLowerCase() : "colorless"))
                    .build();
            CardView stable = cardViews.get(instance);
            if (stable == null || !sameCardView(stable, next)) {
                stable = next;
                cardViews.put(instance, stable);
            }
            seenCards.add(instance);
            out.add(stable);
        }
    }

    private static boolean sameCardView(CardView a, CardView b) {
        return a.ref.instanceId.equals(b.ref.instanceId)
                && a.ref.cardId.equals(b.ref.cardId)
                && a.zone == b.zone
                && a.slotIndex == b.slotIndex
                && a.pose.equals(b.pose)
                && a.playable == b.playable
                && a.selected == b.selected
                && a.hovered == b.hovered
                && a.dragging == b.dragging
                && a.artResourceId.equals(b.artResourceId)
                && a.frameResourceId.equals(b.frameResourceId);
    }

    private boolean playable(AbstractCard card, CardZone zone, long revision) {
        if (card == null || zone != CardZone.HAND) {
            return false;
        }
        String instance = card.uuid != null ? card.uuid.toString() : identity(card);
        PlayabilityState cached = playability.get(instance);
        if (cached != null && cached.revision == revision) {
            return cached.value;
        }
        boolean value = false;
        try {
            value = card.canUse(AbstractDungeon.player, null);
        } catch (Throwable ignored) {
        }
        playability.put(instance, new PlayabilityState(revision, value));
        return value;
    }

    private long combatRevision(AbstractPlayer player) {
        long revision = 17L;
        revision = 31L * revision + (player.energy != null ? player.energy.energy : 0);
        revision = 31L * revision + player.currentHealth;
        revision = 31L * revision + player.maxHealth;
        revision = 31L * revision + (player.hand != null ? player.hand.size() : 0);
        revision = 31L * revision + (player.drawPile != null ? player.drawPile.size() : 0);
        revision = 31L * revision + (player.discardPile != null ? player.discardPile.size() : 0);
        revision = 31L * revision + (player.exhaustPile != null ? player.exhaustPile.size() : 0);
        AbstractRoom room = safeCurrRoom();
        revision = 31L * revision + (room != null && room.isBattleOver ? 1 : 0);
        if (AbstractDungeon.getMonsters() != null && AbstractDungeon.getMonsters().monsters != null) {
            for (com.megacrit.cardcrawl.monsters.AbstractMonster monster
                    : AbstractDungeon.getMonsters().monsters) {
                if (monster == null) continue;
                revision = 31L * revision + monster.currentHealth;
                revision = 31L * revision + (monster.intent != null ? monster.intent.ordinal() : -1);
            }
        }
        if (revision != combatRevision) {
            combatRevision = revision;
        }
        return revision;
    }

    private static final class PlayabilityState {
        final long revision;
        final boolean value;

        PlayabilityState(long revision, boolean value) {
            this.revision = revision;
            this.value = value;
        }
    }

    private static String scene() {
        // The map screen can be opened during a room transition while player/dungeon guards are
        // temporarily unset. It is still a valid present surface and mapFrame() does not require
        // the player instance, so detect it before the transient player guard.
        if (AbstractDungeon.screen != null && "MAP".equals(AbstractDungeon.screen.name())) {
            return "map";
        }
        AbstractRoom room = safeCurrRoom();
        // Deterministic lab transitions can establish a valid event room before STS's coarse
        // player-in-dungeon guard reflects the new room. Prefer the live room over stale combat.
        if (room != null && room.event != null
                && room.phase != AbstractRoom.RoomPhase.COMBAT) {
            return "event";
        }
        if (!AbstractDungeon.isPlayerInDungeon() || AbstractDungeon.player == null) {
            return "";
        }
        if (room != null
                && room.phase == AbstractRoom.RoomPhase.COMBAT
                && !room.isBattleOver) {
            return "combat";
        }
        try {
            if (AbstractDungeon.gridSelectScreen != null
                    && isSelectVisible(AbstractDungeon.gridSelectScreen)) {
                return "select";
            }
            if (AbstractDungeon.handCardSelectScreen != null
                    && isSelectVisible(AbstractDungeon.handCardSelectScreen)) {
                return "select";
            }
        } catch (Throwable ignored) {
        }
        try {
            if (AbstractDungeon.screen != null) {
                String sn = AbstractDungeon.screen.name();
                if ("COMBAT_REWARD".equals(sn) || "CARD_REWARD".equals(sn) || "BOSS_REWARD".equals(sn)) {
                    return "reward";
                }
                if ("SHOP".equals(sn)) {
                    return "shop";
                }
            }
        } catch (Throwable ignored) {
        }
        if (room instanceof com.megacrit.cardcrawl.rooms.RestRoom) {
            return "rest";
        }
        if (room instanceof com.megacrit.cardcrawl.rooms.TreasureRoom
                || room instanceof com.megacrit.cardcrawl.rooms.TreasureRoomBoss) {
            return "treasure";
        }
        if (room instanceof com.megacrit.cardcrawl.rooms.ShopRoom) {
            return "shop";
        }
        try {
            if (AbstractDungeon.combatRewardScreen != null
                    && AbstractDungeon.combatRewardScreen.rewards != null
                    && !AbstractDungeon.combatRewardScreen.rewards.isEmpty()
                    && room != null
                    && room.isBattleOver) {
                return "reward";
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    /** {@link AbstractDungeon#getCurrRoom()} NPEs when {@code currMapNode} is null. */
    private static AbstractRoom safeCurrRoom() {
        try {
            if (AbstractDungeon.currMapNode == null) {
                return null;
            }
            return AbstractDungeon.currMapNode.getRoom();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Skip empty-grid placeholders whose hitboxes never leave the offscreen sentinel. */
    private static boolean isPresentableMapNode(MapRoomNode node) {
        if (node.hb != null) {
            float cx = node.hb.cX;
            float cy = node.hb.cY;
            // STS uses large negative sentinels for unused column slots.
            if (cx < -500f || cy < -500f) {
                return false;
            }
        }
        return true;
    }

    private static String roomKind(MapRoomNode node) {
        if (node.room instanceof com.megacrit.cardcrawl.rooms.EventRoom) {
            return "event";
        }
        if (node.room instanceof com.megacrit.cardcrawl.rooms.MonsterRoom) {
            return "monster";
        }
        if (node.room instanceof com.megacrit.cardcrawl.rooms.MonsterRoomElite) {
            return "elite";
        }
        if (node.room instanceof com.megacrit.cardcrawl.rooms.RestRoom) {
            return "rest";
        }
        if (node.room instanceof com.megacrit.cardcrawl.rooms.ShopRoom) {
            return "shop";
        }
        if (node.room instanceof com.megacrit.cardcrawl.rooms.TreasureRoom
                || node.room instanceof com.megacrit.cardcrawl.rooms.TreasureRoomBoss) {
            return "treasure";
        }
        String symbol = node.getRoomSymbol(Boolean.FALSE);
        if ("M".equals(symbol)) {
            return "monster";
        }
        if ("E".equals(symbol)) {
            return "elite";
        }
        if ("R".equals(symbol)) {
            return "rest";
        }
        if ("$".equals(symbol)) {
            return "shop";
        }
        if ("T".equals(symbol)) {
            return "treasure";
        }
        if ("B".equals(symbol)) {
            return "boss";
        }
        return "unknown";
    }

    private static String identity(AbstractCard card) {
        return card.cardID + "@" + Integer.toHexString(System.identityHashCode(card));
    }
}
