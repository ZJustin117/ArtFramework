package artframework.sts1.input;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import artframework.context.CardRef;
import artframework.context.IntentNames;
import artframework.context.IntentResult;
import artframework.context.UiIntent;
import artframework.c2.MapNodeRef;

/**
 * Live STS1 intent execution for full-present surfaces (16.5b). Best-effort host gestures;
 * authority outcome is always the next Backend snapshot — never paint success here.
 */
public final class Sts1IntentExecutor implements IntentExecutor {

    public static final Sts1IntentExecutor INSTANCE = new Sts1IntentExecutor();

    private Sts1IntentExecutor() {}

    @Override
    public IntentResult execute(UiIntent intent) {
        if (intent == null) {
            return IntentResult.rejected("intent required");
        }
        try {
            if (IntentNames.BEGIN_DRAG.equals(intent.name)) {
                return beginDrag(argString(intent.args, 0));
            }
            if (IntentNames.MOVE_DRAG.equals(intent.name)) {
                return moveDrag(
                        argString(intent.args, 0), argFloat(intent.args, 1), argFloat(intent.args, 2));
            }
            if (IntentNames.DROP_CARD.equals(intent.name)) {
                return dropCard(argString(intent.args, 0), argString(intent.args, 1));
            }
            if (IntentNames.CANCEL_DRAG.equals(intent.name)) {
                return cancelDrag(argString(intent.args, 0));
            }
            if (IntentNames.PLAY_CARD.equals(intent.name)) {
                return playCard(intent.args);
            }
            if (IntentNames.PRESS_END_TURN.equals(intent.name) || IntentNames.PRESS.equals(intent.name)) {
                return pressEndTurn();
            }
            if (IntentNames.CLICK_MAP_NODE.equals(intent.name)) {
                return clickMapNode(intent.args);
            }
            return IntentResult.rejected("sts1 executor unknown intent: " + intent.name);
        } catch (Throwable t) {
            String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            try {
                BaseMod.logger.warn("ArtFramework Sts1IntentExecutor: " + msg);
            } catch (Throwable ignored) {
            }
            return IntentResult.rejected("executor error: " + msg);
        }
    }

    private IntentResult beginDrag(String instanceId) {
        AbstractCard card = findByInstance(instanceId);
        if (card == null) {
            return IntentResult.rejected("card not in hand: " + instanceId);
        }
        if (AbstractDungeon.player == null) {
            return IntentResult.rejected("no player");
        }
        AbstractDungeon.player.hoveredCard = card;
        AbstractDungeon.player.isDraggingCard = true;
        if (card.hb != null) {
            card.hb.hovered = true;
            card.hb.clicked = true;
        }
        return IntentResult.accepted(IntentNames.BEGIN_DRAG);
    }

    private IntentResult moveDrag(String instanceId, float x, float y) {
        AbstractCard card = findByInstance(instanceId);
        if (card == null) {
            return IntentResult.rejected("card not in hand: " + instanceId);
        }
        card.current_x = x;
        card.current_y = y;
        card.target_x = x;
        card.target_y = y;
        return IntentResult.accepted(IntentNames.MOVE_DRAG);
    }

    private IntentResult dropCard(String instanceId, String targetId) {
        AbstractCard card = findByInstance(instanceId);
        if (card == null) {
            return IntentResult.rejected("card not in hand: " + instanceId);
        }
        AbstractMonster monster = resolveMonster(targetId);
        queueUseCard(card, monster);
        clearDrag();
        return IntentResult.queued(IntentNames.DROP_CARD);
    }

    private IntentResult cancelDrag(String instanceId) {
        clearDrag();
        return IntentResult.accepted(IntentNames.CANCEL_DRAG);
    }

    private IntentResult playCard(Object[] args) {
        CardRef ref = null;
        String target = "";
        if (args != null && args.length > 0) {
            if (args[0] instanceof CardRef) {
                ref = (CardRef) args[0];
            } else {
                String id = String.valueOf(args[0]);
                AbstractCard c = findByInstance(id);
                if (c == null) {
                    c = findByCardId(id);
                }
                if (c != null) {
                    ref = new CardRef(
                            c.uuid != null ? c.uuid.toString() : id,
                            c.cardID);
                } else {
                    ref = new CardRef(id, id);
                }
            }
            if (args.length > 1 && args[1] != null) {
                target = String.valueOf(args[1]);
            }
        }
        if (ref == null) {
            return IntentResult.rejected("CardRef required");
        }
        AbstractCard card = findByInstance(ref.instanceId);
        if (card == null) {
            card = findByCardId(ref.cardId);
        }
        if (card == null) {
            return IntentResult.rejected("card not in hand: " + ref.instanceId);
        }
        AbstractMonster monster = resolveMonster(target);
        queueUseCard(card, monster);
        clearDrag();
        return IntentResult.queued(IntentNames.PLAY_CARD);
    }

    private IntentResult pressEndTurn() {
        if (AbstractDungeon.overlayMenu == null
                || AbstractDungeon.overlayMenu.endTurnButton == null) {
            return IntentResult.rejected("no end turn button");
        }
        final com.megacrit.cardcrawl.ui.buttons.EndTurnButton btn =
                AbstractDungeon.overlayMenu.endTurnButton;
        Runnable run =
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Match StsNativeOps / lab: disable(true) ends the player turn.
                            btn.disable(true);
                        } catch (Throwable t) {
                            try {
                                BaseMod.logger.warn("ArtFramework end turn: " + t.getMessage());
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                };
        if (Gdx.app != null) {
            Gdx.app.postRunnable(run);
        } else {
            run.run();
        }
        return IntentResult.queued(IntentNames.PRESS_END_TURN);
    }

    private IntentResult clickMapNode(Object[] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof MapNodeRef)) {
            return IntentResult.rejected("MapNodeRef required");
        }
        MapNodeRef node = (MapNodeRef) args[0];
        // Travel authority stays engine/consumer; acknowledge for full-present chain.
        return IntentResult.accepted(
                IntentNames.CLICK_MAP_NODE + " " + node.row + "," + node.col);
    }

    private static void queueUseCard(final AbstractCard card, final AbstractMonster monster) {
        Runnable run =
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (AbstractDungeon.player == null || card == null) {
                                return;
                            }
                            AbstractDungeon.player.useCard(card, monster, card.energyOnUse);
                        } catch (Throwable t) {
                            try {
                                // Fallback: hitbox click gesture if useCard signature differs.
                                if (card.hb != null) {
                                    card.hb.clicked = true;
                                }
                                AbstractDungeon.player.hoveredCard = card;
                                BaseMod.logger.warn(
                                        "ArtFramework useCard fallback: " + t.getMessage());
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                };
        if (Gdx.app != null) {
            Gdx.app.postRunnable(run);
        } else {
            run.run();
        }
    }

    private static void clearDrag() {
        if (AbstractDungeon.player == null) {
            return;
        }
        AbstractDungeon.player.isDraggingCard = false;
        AbstractDungeon.player.hoveredCard = null;
    }

    private static AbstractCard findByInstance(String instanceId) {
        if (instanceId == null
                || instanceId.isEmpty()
                || AbstractDungeon.player == null
                || AbstractDungeon.player.hand == null
                || AbstractDungeon.player.hand.group == null) {
            return null;
        }
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c == null) {
                continue;
            }
            String id =
                    c.uuid != null
                            ? c.uuid.toString()
                            : c.cardID + "@" + Integer.toHexString(System.identityHashCode(c));
            if (instanceId.equals(id)) {
                return c;
            }
        }
        return null;
    }

    private static AbstractCard findByCardId(String cardId) {
        if (cardId == null
                || cardId.isEmpty()
                || AbstractDungeon.player == null
                || AbstractDungeon.player.hand == null) {
            return null;
        }
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c != null && cardId.equals(c.cardID)) {
                return c;
            }
        }
        return null;
    }

    private static AbstractMonster resolveMonster(String targetId) {
        if (targetId == null
                || targetId.isEmpty()
                || "self".equals(targetId)
                || AbstractDungeon.getCurrRoom() == null
                || AbstractDungeon.getCurrRoom().monsters == null) {
            return null;
        }
        try {
            AbstractMonster m = AbstractDungeon.getCurrRoom().monsters.getMonster(targetId);
            if (m != null) {
                return m;
            }
        } catch (Throwable ignored) {
        }
        try {
            java.util.ArrayList<AbstractMonster> list =
                    AbstractDungeon.getCurrRoom().monsters.monsters;
            if (list != null && !list.isEmpty()) {
                // index or first alive
                try {
                    int idx = Integer.parseInt(targetId);
                    if (idx >= 0 && idx < list.size()) {
                        return list.get(idx);
                    }
                } catch (NumberFormatException ignored) {
                }
                for (AbstractMonster m : list) {
                    if (m != null && !m.isDeadOrEscaped()) {
                        return m;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String argString(Object[] args, int i) {
        if (args == null || i >= args.length || args[i] == null) {
            return "";
        }
        if (args[i] instanceof CardRef) {
            return ((CardRef) args[i]).instanceId;
        }
        return String.valueOf(args[i]);
    }

    private static float argFloat(Object[] args, int i) {
        if (args == null || i >= args.length || args[i] == null) {
            return 0f;
        }
        if (args[i] instanceof Number) {
            return ((Number) args[i]).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(args[i]));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
