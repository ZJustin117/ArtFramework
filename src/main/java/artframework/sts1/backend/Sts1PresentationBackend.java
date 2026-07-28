package artframework.sts1.backend;

import artframework.assets.ResourceIds;
import artframework.context.BackendMode;
import artframework.context.CardPose;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.IntentResult;
import artframework.context.MapNodeView;
import artframework.context.MapView;
import artframework.context.PresentationBackend;
import artframework.context.UiIntent;
import artframework.context.ViewportView;
import artframework.sts1.FullPresentMode;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * STS1 read-only authority adapter. Observation-only until the matching full-present surface owns
 * native input and has a tested intent executor ({@link FullPresentMode#mayOwnInput}).
 */
public final class Sts1PresentationBackend implements PresentationBackend {

    public static final Sts1PresentationBackend INSTANCE = new Sts1PresentationBackend();

    private long frameId;
    private String scene = "";
    private long sceneEpoch;

    private Sts1PresentationBackend() {}

    @Override
    public String id() {
        return "sts1";
    }

    @Override
    public BackendMode mode() {
        return BackendMode.READ_ONLY;
    }

    @Override
    public ContextFrame snapshot() {
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
                return combatFrame();
            }
            if ("map".equals(scene)) {
                return mapFrame();
            }
            return ContextFrame.unavailable(frameId);
        } catch (Throwable t) {
            frameId++;
            return ContextFrame.unavailable(frameId);
        }
    }

    @Override
    public IntentResult submitIntent(UiIntent intent) {
        if (intent == null) {
            return IntentResult.rejected("intent required");
        }
        if (!FullPresentMode.mayOwnInput(intent.surfaceId)) {
            return IntentResult.rejected("sts1 full-present input is not enabled for " + intent.surfaceId);
        }
        return IntentResult.rejected("sts1 intent executor not implemented: " + intent.name);
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
                frameId, sceneEpoch, "combat", cards, controls, MapView.empty(), true, viewport);
    }

    private ContextFrame mapFrame() {
        List<MapNodeView> nodes = new ArrayList<MapNodeView>();
        if (AbstractDungeon.map != null) {
            for (List<MapRoomNode> row : AbstractDungeon.map) {
                if (row == null) {
                    continue;
                }
                for (MapRoomNode node : row) {
                    if (node == null) {
                        continue;
                    }
                    String kind = roomKind(node);
                    nodes.add(
                            new MapNodeView(
                                    node.y,
                                    node.x,
                                    node.offsetX,
                                    node.offsetY,
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
                frameId, sceneEpoch, "map", null, ControlsView.empty(), map, true, viewport);
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
