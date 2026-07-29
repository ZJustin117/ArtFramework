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
import artframework.context.SelectView;
import artframework.context.SignalBackend;
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
import java.util.List;

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
                return enrich(combatFrame());
            }
            if ("map".equals(scene)) {
                return enrich(mapFrame());
            }
            if ("event".equals(scene)) {
                return enrich(eventFrame());
            }
            if ("select".equals(scene)) {
                return enrich(selectOnlyFrame());
            }
            return ContextFrame.unavailable(frameId);
        } catch (Throwable t) {
            frameId++;
            return ContextFrame.unavailable(frameId);
        }
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
        List<CardView> cards = new ArrayList<CardView>();
        append(cards, player.hand, CardZone.HAND);
        append(cards, player.drawPile, CardZone.DRAW);
        append(cards, player.discardPile, CardZone.DISCARD);
        append(cards, player.exhaustPile, CardZone.EXHAUST);

        boolean endTurnEnabled = AbstractDungeon.overlayMenu != null
                && AbstractDungeon.overlayMenu.endTurnButton != null
                && AbstractDungeon.overlayMenu.endTurnButton.enabled;
        boolean endTurnVisible = AbstractDungeon.overlayMenu != null
                && AbstractDungeon.overlayMenu.endTurnButton != null;
        ControlsView controls =
                ControlsView.combat(
                        player.energy != null ? player.energy.energy : 0,
                        player.hand != null ? player.hand.size() : 0,
                        player.drawPile != null ? player.drawPile.size() : 0,
                        player.discardPile != null ? player.discardPile.size() : 0,
                        player.exhaustPile != null ? player.exhaustPile.size() : 0,
                        endTurnEnabled,
                        endTurnVisible);
        ViewportView viewport =
                new ViewportView(
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT,
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT);
        return new ContextFrame(
                frameId,
                sceneEpoch,
                "combat",
                cards,
                controls,
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                true,
                viewport);
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

    /** Attach live event/select views onto any available frame without changing scene. */
    private ContextFrame enrich(ContextFrame base) {
        if (base == null || !base.available) {
            return base;
        }
        EventView event = readEventView();
        SelectView select = readSelectView();
        if (!event.available && !select.available) {
            return base;
        }
        if (!event.available) {
            event = base.eventView;
        }
        if (!select.available) {
            select = base.selectView;
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
                true,
                base.viewport);
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

    private static void append(List<CardView> out, CardGroup group, CardZone zone) {
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
            boolean playable = false;
            try {
                playable = card.canUse(AbstractDungeon.player, null);
            } catch (Throwable ignored) {
            }
            out.add(CardView.builder(new CardRef(instance, card.cardID))
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
                    .build());
        }
    }

    private static String scene() {
        if (!AbstractDungeon.isPlayerInDungeon() || AbstractDungeon.player == null) {
            return "";
        }
        AbstractRoom room = safeCurrRoom();
        if (room != null
                && room.phase == AbstractRoom.RoomPhase.COMBAT
                && !room.isBattleOver) {
            return "combat";
        }
        if (AbstractDungeon.screen != null && "MAP".equals(AbstractDungeon.screen.name())) {
            return "map";
        }
        // Event room with live event (e.g. Neow) before map/combat.
        if (room != null && room.event != null
                && room.phase != AbstractRoom.RoomPhase.COMBAT) {
            return "event";
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
        return "event";
    }

    private static String identity(AbstractCard card) {
        return card.cardID + "@" + Integer.toHexString(System.identityHashCode(card));
    }
}
