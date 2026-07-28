package artframework.sts1.backend;

import artframework.assets.ResourceIds;
import artframework.context.BackendMode;
import artframework.context.CardPose;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.IntentResult;
import artframework.context.PresentationBackend;
import artframework.context.UiIntent;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * STS1 read-only authority adapter. It is deliberately observation-only until the matching
 * full-present surface owns native input and has a tested intent executor.
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
                sceneEpoch++;
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
        return IntentResult.rejected("sts1 full-present input is not enabled");
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

        Map<String, Object> controls = new LinkedHashMap<String, Object>();
        controls.put("sceneEpoch", Long.valueOf(sceneEpoch));
        controls.put("energy", Integer.valueOf(player.energy != null ? player.energy.energy : 0));
        controls.put("handSize", Integer.valueOf(player.hand != null ? player.hand.size() : 0));
        controls.put("drawSize", Integer.valueOf(player.drawPile != null ? player.drawPile.size() : 0));
        controls.put("discardSize", Integer.valueOf(player.discardPile != null ? player.discardPile.size() : 0));
        controls.put("exhaustSize", Integer.valueOf(player.exhaustPile != null ? player.exhaustPile.size() : 0));
        boolean endTurnEnabled = AbstractDungeon.overlayMenu != null
                && AbstractDungeon.overlayMenu.endTurnButton != null
                && AbstractDungeon.overlayMenu.endTurnButton.enabled;
        controls.put("endTurnEnabled", Boolean.valueOf(endTurnEnabled));
        controls.put("viewportWidth", Integer.valueOf(com.megacrit.cardcrawl.core.Settings.WIDTH));
        controls.put("viewportHeight", Integer.valueOf(com.megacrit.cardcrawl.core.Settings.HEIGHT));
        return new ContextFrame(
                frameId,
                sceneEpoch,
                "combat",
                cards,
                controls,
                null,
                true,
                new artframework.context.ViewportView(
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT,
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT));
    }

    private ContextFrame mapFrame() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("sceneEpoch", Long.valueOf(sceneEpoch));
        map.put("viewportWidth", Integer.valueOf(com.megacrit.cardcrawl.core.Settings.WIDTH));
        map.put("viewportHeight", Integer.valueOf(com.megacrit.cardcrawl.core.Settings.HEIGHT));
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        if (AbstractDungeon.map != null) {
            for (List<MapRoomNode> row : AbstractDungeon.map) {
                if (row == null) {
                    continue;
                }
                for (MapRoomNode node : row) {
                    if (node == null) {
                        continue;
                    }
                    Map<String, Object> view = new LinkedHashMap<String, Object>();
                    view.put("row", Integer.valueOf(node.y));
                    view.put("col", Integer.valueOf(node.x));
                    view.put("x", Float.valueOf(node.offsetX));
                    view.put("y", Float.valueOf(node.offsetY));
                    view.put("taken", Boolean.valueOf(node.taken));
                    view.put("highlighted", Boolean.valueOf(node.highlighted));
                    view.put("symbol", node.getRoomSymbol(Boolean.FALSE));
                    view.put("resourceId", ResourceIds.mapNode(roomKind(node)));
                    nodes.add(view);
                }
            }
        }
        map.put("nodes", nodes);
        return new ContextFrame(
                frameId,
                sceneEpoch,
                "map",
                null,
                null,
                map,
                true,
                new artframework.context.ViewportView(
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT,
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT));
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
