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
import artframework.component.MapNodeRef;

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
            if (IntentNames.CHOOSE_EVENT_OPTION.equals(intent.name)) {
                return chooseEventOption(intent.args);
            }
            if (IntentNames.SELECT_CARD.equals(intent.name)) {
                return selectCard(intent.args);
            }
            if (IntentNames.CONFIRM_SELECT.equals(intent.name)) {
                return confirmSelect(intent.args);
            }
            if (IntentNames.PRESS_PROCEED.equals(intent.name)) {
                return pressProceed();
            }
            if (IntentNames.PRESS_CANCEL.equals(intent.name)) {
                return pressCancel();
            }
            if (IntentNames.CLAIM_REWARD.equals(intent.name)) {
                return claimReward(intent.args);
            }
            if (IntentNames.SKIP_REWARD.equals(intent.name)) {
                return skipReward();
            }
            if (IntentNames.CHOOSE_REST_OPTION.equals(intent.name)) {
                return chooseRestOption(intent.args);
            }
            if (IntentNames.OPEN_CHEST.equals(intent.name)) {
                return openChest();
            }
            if (IntentNames.BUY_SHOP_ENTRY.equals(intent.name)) {
                return buyShopEntry(intent.args);
            }
            if (IntentNames.PURGE_CARD.equals(intent.name)) {
                return purgeCard();
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

    private IntentResult pressProceed() {
        try {
            if (AbstractDungeon.overlayMenu != null
                    && AbstractDungeon.overlayMenu.proceedButton != null) {
                final com.megacrit.cardcrawl.ui.buttons.ProceedButton btn =
                        AbstractDungeon.overlayMenu.proceedButton;
                Runnable run =
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    btn.show();
                                } catch (Throwable ignored) {
                                }
                            }
                        };
                if (Gdx.app != null) {
                    Gdx.app.postRunnable(run);
                } else {
                    run.run();
                }
                return IntentResult.accepted(IntentNames.PRESS_PROCEED);
            }
        } catch (Throwable t) {
            return IntentResult.rejected("proceed: " + t.getMessage());
        }
        return IntentResult.accepted(IntentNames.PRESS_PROCEED);
    }

    private IntentResult pressCancel() {
        try {
            if (AbstractDungeon.overlayMenu != null
                    && AbstractDungeon.overlayMenu.cancelButton != null) {
                final Object btn = AbstractDungeon.overlayMenu.cancelButton;
                Runnable run =
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    java.lang.reflect.Method hide =
                                            btn.getClass().getMethod("hide");
                                    hide.invoke(btn);
                                } catch (Throwable t) {
                                    try {
                                        java.lang.reflect.Field hb =
                                                btn.getClass().getField("hb");
                                        Object hit = hb.get(btn);
                                        if (hit != null) {
                                            java.lang.reflect.Field clicked =
                                                    hit.getClass().getField("clicked");
                                            clicked.setBoolean(hit, true);
                                        }
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                        };
                post(run);
                return IntentResult.accepted(IntentNames.PRESS_CANCEL);
            }
        } catch (Throwable t) {
            return IntentResult.rejected("cancel: " + t.getMessage());
        }
        return IntentResult.rejected("no cancel button");
    }

    private IntentResult claimReward(Object[] args) {
        int index = 0;
        if (args != null && args.length > 0 && args[0] instanceof Number) {
            index = ((Number) args[0]).intValue();
        } else if (args != null && args.length > 0 && args[0] != null) {
            try {
                index = Integer.parseInt(String.valueOf(args[0]));
            } catch (NumberFormatException e) {
                return IntentResult.rejected("reward index required");
            }
        }
        try {
            if (AbstractDungeon.combatRewardScreen == null
                    || AbstractDungeon.combatRewardScreen.rewards == null) {
                return IntentResult.rejected("no combat reward screen");
            }
            java.util.ArrayList<?> rewards = AbstractDungeon.combatRewardScreen.rewards;
            if (index < 0 || index >= rewards.size()) {
                return IntentResult.rejected("reward index out of range: " + index);
            }
            final Object item = rewards.get(index);
            if (item == null) {
                return IntentResult.rejected("reward item null");
            }
            post(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                java.lang.reflect.Method claim =
                                        item.getClass().getMethod("claimReward");
                                claim.invoke(item);
                            } catch (Throwable t) {
                                try {
                                    java.lang.reflect.Field hb = item.getClass().getField("hb");
                                    Object hit = hb.get(item);
                                    if (hit != null) {
                                        java.lang.reflect.Field clicked =
                                                hit.getClass().getField("clicked");
                                        clicked.setBoolean(hit, true);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    });
            return IntentResult.queued(IntentNames.CLAIM_REWARD + " " + index);
        } catch (Throwable t) {
            return IntentResult.rejected("claim reward: " + msg(t));
        }
    }

    private IntentResult skipReward() {
        try {
            if (AbstractDungeon.overlayMenu != null
                    && AbstractDungeon.overlayMenu.proceedButton != null) {
                return pressProceed();
            }
            if (AbstractDungeon.combatRewardScreen == null) {
                return IntentResult.rejected("no combat reward screen");
            }
            post(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                AbstractDungeon.closeCurrentScreen();
                            } catch (Throwable ignored) {
                            }
                        }
                    });
            return IntentResult.queued(IntentNames.SKIP_REWARD);
        } catch (Throwable t) {
            return IntentResult.rejected("skip reward: " + msg(t));
        }
    }

    private IntentResult chooseRestOption(Object[] args) {
        String optionId = argString(args, 0);
        if (optionId == null || optionId.isEmpty()) {
            return IntentResult.rejected("rest option id required");
        }
        try {
            com.megacrit.cardcrawl.rooms.AbstractRoom room = AbstractDungeon.getCurrRoom();
            if (room == null || !(room instanceof com.megacrit.cardcrawl.rooms.RestRoom)) {
                return IntentResult.rejected("not in rest room");
            }
            com.megacrit.cardcrawl.rooms.RestRoom rest =
                    (com.megacrit.cardcrawl.rooms.RestRoom) room;
            Object campfire = rest.campfireUI;
            if (campfire == null) {
                return IntentResult.rejected("no campfire UI");
            }
            java.util.ArrayList<?> buttons = null;
            try {
                java.lang.reflect.Field f = findField(campfire.getClass(), "buttons");
                if (f == null) {
                    f = findField(campfire.getClass(), "options");
                }
                if (f != null) {
                    f.setAccessible(true);
                    Object raw = f.get(campfire);
                    if (raw instanceof java.util.ArrayList) {
                        buttons = (java.util.ArrayList<?>) raw;
                    }
                }
            } catch (Throwable ignored) {
            }
            if (buttons == null || buttons.isEmpty()) {
                return IntentResult.rejected("no campfire options");
            }
            final Object match = matchCampfireOption(buttons, optionId);
            if (match == null) {
                return IntentResult.rejected("rest option not found: " + optionId);
            }
            post(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                java.lang.reflect.Method useOption =
                                        match.getClass().getMethod("useOption");
                                useOption.invoke(match);
                            } catch (Throwable t) {
                                try {
                                    java.lang.reflect.Field hb = match.getClass().getField("hb");
                                    Object hit = hb.get(match);
                                    if (hit != null) {
                                        java.lang.reflect.Field clicked =
                                                hit.getClass().getField("clicked");
                                        clicked.setBoolean(hit, true);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    });
            return IntentResult.queued(IntentNames.CHOOSE_REST_OPTION + " " + optionId);
        } catch (Throwable t) {
            return IntentResult.rejected("rest option: " + msg(t));
        }
    }

    private static Object matchCampfireOption(java.util.ArrayList<?> buttons, String optionId) {
        String needle = optionId.toLowerCase();
        for (Object b : buttons) {
            if (b == null) {
                continue;
            }
            String simple = b.getClass().getSimpleName().toLowerCase();
            if (simple.contains(needle)
                    || (needle.equals("rest") && simple.contains("restoption"))
                    || (needle.equals("smith") && simple.contains("smith"))
                    || (needle.equals("recall") && simple.contains("recall"))
                    || (needle.equals("toke") && simple.contains("toke"))
                    || (needle.equals("dig") && simple.contains("dig"))
                    || (needle.equals("lift") && simple.contains("lift"))) {
                return b;
            }
        }
        try {
            int idx = Integer.parseInt(optionId);
            if (idx >= 0 && idx < buttons.size()) {
                return buttons.get(idx);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private IntentResult openChest() {
        try {
            com.megacrit.cardcrawl.rooms.AbstractRoom room = AbstractDungeon.getCurrRoom();
            if (room == null || !(room instanceof com.megacrit.cardcrawl.rooms.TreasureRoom)) {
                return IntentResult.rejected("not in treasure room");
            }
            final com.megacrit.cardcrawl.rooms.TreasureRoom treasure =
                    (com.megacrit.cardcrawl.rooms.TreasureRoom) room;
            post(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (treasure.chest != null) {
                                    treasure.chest.open(false);
                                }
                            } catch (Throwable t) {
                                try {
                                    if (treasure.chest != null) {
                                        java.lang.reflect.Method open =
                                                treasure.chest.getClass().getMethod("open", boolean.class);
                                        open.invoke(treasure.chest, Boolean.FALSE);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    });
            return IntentResult.queued(IntentNames.OPEN_CHEST);
        } catch (Throwable t) {
            return IntentResult.rejected("open chest: " + msg(t));
        }
    }

    private IntentResult buyShopEntry(Object[] args) {
        int index = 0;
        if (args != null && args.length > 0 && args[0] instanceof Number) {
            index = ((Number) args[0]).intValue();
        } else if (args != null && args.length > 0 && args[0] != null) {
            try {
                index = Integer.parseInt(String.valueOf(args[0]));
            } catch (NumberFormatException e) {
                return IntentResult.rejected("shop entry index required");
            }
        }
        try {
            com.megacrit.cardcrawl.rooms.AbstractRoom room = AbstractDungeon.getCurrRoom();
            if (room == null || !(room instanceof com.megacrit.cardcrawl.rooms.ShopRoom)) {
                return IntentResult.rejected("not in shop room");
            }
            Object shopScreen = null;
            try {
                shopScreen = AbstractDungeon.shopScreen;
            } catch (Throwable ignored) {
            }
            if (shopScreen == null) {
                return IntentResult.rejected("no shop screen");
            }
            final Object screen = shopScreen;
            final int idx = index;
            post(
                    new Runnable() {
                        @Override
                        public void run() {
                            tryClickShopEntry(screen, idx);
                        }
                    });
            return IntentResult.queued(IntentNames.BUY_SHOP_ENTRY + " " + index);
        } catch (Throwable t) {
            return IntentResult.rejected("buy shop: " + msg(t));
        }
    }

    private static void tryClickShopEntry(Object screen, int index) {
        String[] listFields = new String[] {"coloredCards", "colorlessCards", "relics", "potions"};
        int remaining = index;
        for (int f = 0; f < listFields.length; f++) {
            try {
                java.lang.reflect.Field field = findField(screen.getClass(), listFields[f]);
                if (field == null) {
                    continue;
                }
                field.setAccessible(true);
                Object raw = field.get(screen);
                if (!(raw instanceof java.util.ArrayList)) {
                    continue;
                }
                java.util.ArrayList<?> list = (java.util.ArrayList<?>) raw;
                if (remaining < list.size()) {
                    Object entry = list.get(remaining);
                    if (entry == null) {
                        return;
                    }
                    if (entry instanceof AbstractCard) {
                        AbstractCard card = (AbstractCard) entry;
                        if (card.hb != null) {
                            card.hb.clicked = true;
                        }
                        return;
                    }
                    tryInvoke(entry, "purchaseRelic");
                    tryInvoke(entry, "purchasePotion");
                    tryInvoke(entry, "purchaseCard");
                    tryClickHb(entry);
                    return;
                }
                remaining -= list.size();
            } catch (Throwable ignored) {
            }
        }
    }

    private static java.lang.reflect.Field findField(Class<?> clz, String name) {
        Class<?> c = clz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void tryInvoke(Object target, String method) {
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(method);
            m.invoke(target);
        } catch (Throwable ignored) {
        }
    }

    private static void tryClickHb(Object target) {
        try {
            java.lang.reflect.Field hb = findField(target.getClass(), "hb");
            if (hb == null) {
                return;
            }
            hb.setAccessible(true);
            Object hit = hb.get(target);
            if (hit != null) {
                java.lang.reflect.Field clicked = hit.getClass().getField("clicked");
                clicked.setBoolean(hit, true);
            }
        } catch (Throwable ignored) {
        }
    }

    private IntentResult purgeCard() {
        try {
            com.megacrit.cardcrawl.rooms.AbstractRoom room = AbstractDungeon.getCurrRoom();
            if (room == null || !(room instanceof com.megacrit.cardcrawl.rooms.ShopRoom)) {
                return IntentResult.rejected("not in shop room");
            }
            Object shopScreen = null;
            try {
                shopScreen = AbstractDungeon.shopScreen;
            } catch (Throwable ignored) {
            }
            if (shopScreen == null) {
                return IntentResult.rejected("no shop screen");
            }
            final Object screen = shopScreen;
            post(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                java.lang.reflect.Field purge =
                                        screen.getClass().getField("purgeAvailable");
                                if (!purge.getBoolean(screen)) {
                                    return;
                                }
                            } catch (Throwable ignored) {
                            }
                            try {
                                java.lang.reflect.Method m =
                                        screen.getClass().getMethod("purgeCard");
                                m.invoke(screen);
                            } catch (Throwable t) {
                                try {
                                    java.lang.reflect.Field hb =
                                            screen.getClass().getField("purgeHb");
                                    Object hit = hb.get(screen);
                                    if (hit != null) {
                                        java.lang.reflect.Field clicked =
                                                hit.getClass().getField("clicked");
                                        clicked.setBoolean(hit, true);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    });
            return IntentResult.queued(IntentNames.PURGE_CARD);
        } catch (Throwable t) {
            return IntentResult.rejected("purge: " + msg(t));
        }
    }

    private static void post(Runnable run) {
        if (Gdx.app != null) {
            Gdx.app.postRunnable(run);
        } else {
            run.run();
        }
    }

    private static String msg(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
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
        return Sts1MapIntentBridge.click(node);
    }

    private IntentResult chooseEventOption(Object[] args) {
        int index = 0;
        if (args != null && args.length > 0 && args[0] instanceof Number) {
            index = ((Number) args[0]).intValue();
        } else if (args != null && args.length > 0 && args[0] != null) {
            try {
                index = Integer.parseInt(String.valueOf(args[0]));
            } catch (NumberFormatException e) {
                return IntentResult.rejected("event option index required");
            }
        }
        String label = args != null && args.length > 1 && args[1] != null ? String.valueOf(args[1]) : "";
        artframework.api.UiOpResult op =
                artframework.ops.StsNativeOps.INSTANCE.chooseEventOption(index, label);
        if (op != null && op.isOk()) {
            return IntentResult.queued(IntentNames.CHOOSE_EVENT_OPTION + " " + index);
        }
        return IntentResult.rejected(op != null ? op.message : "event option failed");
    }

    private IntentResult selectCard(Object[] args) {
        artframework.c2.SelectKind kind = artframework.c2.SelectKind.GRID;
        String cardId = "";
        int index = 0;
        if (args != null && args.length > 0) {
            if (args[0] instanceof artframework.c2.SelectKind) {
                kind = (artframework.c2.SelectKind) args[0];
                cardId = args.length > 1 && args[1] != null ? String.valueOf(args[1]) : "";
                if (args.length > 2 && args[2] instanceof Number) {
                    index = ((Number) args[2]).intValue();
                }
            } else if (args[0] instanceof CardRef) {
                CardRef ref = (CardRef) args[0];
                cardId = ref.cardId;
                kind = resolveSelectKind();
            } else {
                cardId = String.valueOf(args[0]);
                if (args.length > 1 && args[1] instanceof Number) {
                    index = ((Number) args[1]).intValue();
                } else if (args.length > 1 && args[1] != null) {
                    String k = String.valueOf(args[1]).toUpperCase();
                    if ("HAND".equals(k)) {
                        kind = artframework.c2.SelectKind.HAND;
                    }
                }
                if (args.length > 2 && args[2] instanceof Number) {
                    index = ((Number) args[2]).intValue();
                }
            }
        }
        if (cardId == null || cardId.isEmpty()) {
            return IntentResult.rejected("select card id required");
        }
        artframework.api.UiOpResult op =
                artframework.ops.StsNativeOps.INSTANCE.selectCard(kind, cardId, index);
        if (op != null && op.isOk()) {
            return IntentResult.queued(IntentNames.SELECT_CARD + " " + cardId);
        }
        return IntentResult.rejected(op != null ? op.message : "select card failed");
    }

    private IntentResult confirmSelect(Object[] args) {
        artframework.c2.SelectKind kind = resolveSelectKind();
        if (args != null && args.length > 0) {
            if (args[0] instanceof artframework.c2.SelectKind) {
                kind = (artframework.c2.SelectKind) args[0];
            } else if (args[0] != null) {
                String k = String.valueOf(args[0]).toUpperCase();
                if ("HAND".equals(k)) {
                    kind = artframework.c2.SelectKind.HAND;
                } else if ("GRID".equals(k)) {
                    kind = artframework.c2.SelectKind.GRID;
                }
            }
        }
        artframework.api.UiOpResult op = artframework.ops.StsNativeOps.INSTANCE.confirmSelect(kind);
        if (op != null && op.isOk()) {
            return IntentResult.queued(IntentNames.CONFIRM_SELECT + " " + kind.name());
        }
        return IntentResult.rejected(op != null ? op.message : "confirm select failed");
    }

    private static artframework.c2.SelectKind resolveSelectKind() {
        try {
            if (AbstractDungeon.handCardSelectScreen != null) {
                return artframework.c2.SelectKind.HAND;
            }
        } catch (Throwable ignored) {
        }
        return artframework.c2.SelectKind.GRID;
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
