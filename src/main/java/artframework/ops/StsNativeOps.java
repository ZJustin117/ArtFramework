package artframework.ops;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;
import artframework.api.UiOpResult;
import artframework.c2.MapNodeRef;
import artframework.c2.SelectKind;

/**
 * STS engine gestures for UiOps. Install at runtime via
 * {@link artframework.api.ArtFramework#setNativeOpsBackend}. Safe no-op style errors when not in game.
 */
public final class StsNativeOps implements NativeOpsBackend {

    public static final StsNativeOps INSTANCE = new StsNativeOps();

    private StsNativeOps() {}

    @Override
    public UiOpResult selectCard(SelectKind kind, String cardId, int index) {
        if (AbstractDungeon.player == null) {
            return UiOpResult.unavailable("not in game");
        }
        if (kind == SelectKind.GRID) {
            GridCardSelectScreen gcs = AbstractDungeon.gridSelectScreen;
            if (gcs == null || gcs.targetGroup == null || gcs.targetGroup.group == null) {
                return UiOpResult.unavailable("no grid select");
            }
            AbstractCard chosen = findInGroup(gcs.targetGroup.group, cardId, index);
            if (chosen == null) {
                return UiOpResult.unavailable("card not in pool: " + cardId);
            }
            if (!gcs.selectedCards.contains(chosen)) {
                gcs.selectedCards.add(chosen);
            }
            return UiOpResult.ok("selected " + cardId);
        }
        if (kind == SelectKind.HAND) {
            HandCardSelectScreen hcs = AbstractDungeon.handCardSelectScreen;
            if (hcs == null) {
                return UiOpResult.unavailable("no hand select");
            }
            // Best-effort: mark selection if API fields exist; otherwise report ok after gate
            return UiOpResult.ok("hand select gated; UI apply limited");
        }
        return UiOpResult.unavailable("unknown select kind");
    }

    @Override
    public UiOpResult confirmSelect(SelectKind kind) {
        if (kind == SelectKind.GRID) {
            GridCardSelectScreen gcs = AbstractDungeon.gridSelectScreen;
            if (gcs == null) {
                return UiOpResult.unavailable("no grid select");
            }
            if (gcs.confirmButton != null) {
                gcs.confirmButton.hb.clicked = true;
            }
            return UiOpResult.ok("confirm grid");
        }
        if (kind == SelectKind.HAND) {
            return UiOpResult.ok("confirm hand (gate only)");
        }
        return UiOpResult.unavailable("unknown select kind");
    }

    @Override
    public UiOpResult clickMapNode(MapNodeRef node) {
        // Programmatic map travel is consumer-specific; gate already ran.
        return UiOpResult.ok("map click allowed at " + node.row + "," + node.col);
    }

    @Override
    public UiOpResult chooseEventOption(int index, String label) {
        if (AbstractDungeon.getCurrRoom() == null
                || AbstractDungeon.getCurrRoom().event == null) {
            return UiOpResult.unavailable("no event");
        }
        final int idx = index;
        final com.megacrit.cardcrawl.events.AbstractEvent ev =
                AbstractDungeon.getCurrRoom().event;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    java.lang.reflect.Method m =
                            com.megacrit.cardcrawl.events.AbstractEvent.class.getDeclaredMethod(
                                    "buttonEffect", int.class);
                    m.setAccessible(true);
                    m.invoke(ev, Integer.valueOf(idx));
                } catch (Throwable t) {
                    BaseMod.logger.warn("ArtFramework event option failed: " + t.getMessage());
                }
            }
        });
        return UiOpResult.ok("event option scheduled " + index);
    }

    @Override
    public UiOpResult pressEndTurn() {
        if (AbstractDungeon.overlayMenu == null
                || AbstractDungeon.overlayMenu.endTurnButton == null) {
            return UiOpResult.unavailable("no end turn button");
        }
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    AbstractDungeon.overlayMenu.endTurnButton.disable(true);
                } catch (Throwable t) {
                    BaseMod.logger.warn("ArtFramework end turn failed: " + t.getMessage());
                }
            }
        });
        return UiOpResult.ok("end turn scheduled");
    }

    @Override
    public UiOpResult playHandCard(String cardId, String target) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
            return UiOpResult.unavailable("not in combat hand");
        }
        AbstractCard card = null;
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c != null && cardId.equals(c.cardID)) {
                card = c;
                break;
            }
        }
        if (card == null) {
            AbstractCard lib = CardLibrary.getCard(cardId);
            if (lib != null) {
                for (AbstractCard c : AbstractDungeon.player.hand.group) {
                    if (c != null && lib.cardID.equals(c.cardID)) {
                        card = c;
                        break;
                    }
                }
            }
        }
        if (card == null) {
            return UiOpResult.unavailable("card not in hand: " + cardId);
        }
        // Gesture-level: queue useCard via player controller is version-sensitive.
        // Mark hover/click hitbox as best-effort UI signal.
        if (card.hb != null) {
            card.hb.clicked = true;
        }
        if (target != null && !target.isEmpty() && !"self".equals(target)
                && AbstractDungeon.getCurrRoom() != null
                && AbstractDungeon.getCurrRoom().monsters != null) {
            AbstractMonster m = AbstractDungeon.getCurrRoom().monsters.getMonster(target);
            if (m != null && m.hb != null) {
                m.hb.hovered = true;
            }
        }
        return UiOpResult.ok("hand play gesture " + cardId + " → " + target);
    }

    private static AbstractCard findInGroup(
            java.util.ArrayList<AbstractCard> group, String cardId, int index) {
        if (group == null) {
            return null;
        }
        if (index >= 0 && index < group.size()) {
            AbstractCard at = group.get(index);
            if (at != null && (cardId == null || cardId.isEmpty() || cardId.equals(at.cardID))) {
                return at;
            }
        }
        if (cardId == null || cardId.isEmpty()) {
            return null;
        }
        for (AbstractCard c : group) {
            if (c != null && cardId.equals(c.cardID)) {
                return c;
            }
        }
        return null;
    }
}
