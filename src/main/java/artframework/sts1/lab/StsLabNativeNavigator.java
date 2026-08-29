package artframework.sts1.lab;

import artframework.context.IntentResult;
import artframework.core.SignalGroups;
import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.SignalSubscription;
import artframework.core.UiSignal;

/** STS-backed lab navigation executor. Outcomes still come from later backend projections. */
public final class StsLabNativeNavigator {

    private static SignalSubscription subscription;

    private StsLabNativeNavigator() {}

    public static void install() {
        if (subscription != null && subscription.isConnected()) {
            return;
        }
        subscription =
                SignalGroups.nativeGroup()
                        .connect(
                                LabNavigationSignals.REQUEST,
                                new SignalListener() {
                                    @Override
                                    public SignalDecision onSignal(UiSignal signal) {
                                        if (!(signal.payload instanceof LabNavigationIntent)) {
                                            return SignalDecision.stopRejected("lab intent payload required");
                                        }
                                        return execute((LabNavigationIntent) signal.payload);
                                    }
                                });
    }

    public static void resetForTests() {
        if (subscription != null) {
            subscription.disconnect();
        }
        subscription = null;
    }

    private static SignalDecision execute(LabNavigationIntent intent) {
        if (LabIntentNames.ENTER_EVENT_ROOM.equals(intent.name)) {
            return enterEventRoom();
        }
        if (LabIntentNames.ENTER_ROOM.equals(intent.name)) {
            return enterRoom(intent.argString(0));
        }
        if (LabIntentNames.ENTER_SELECT.equals(intent.name)) {
            return enterSelect(intent.argString(0));
        }
        return SignalDecision.stopRejected("unknown lab intent: " + intent.name);
    }

    private static SignalDecision enterEventRoom() {
        try {
            if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.player == null) {
                return SignalDecision.stopRejected("run not ready");
            }
            com.megacrit.cardcrawl.rooms.AbstractRoom room = currentRoom();
            if (room != null
                    && room.phase == com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT
                    && !room.isBattleOver) {
                return SignalDecision.stopRejected("cannot enter event during live combat");
            }
            if (room != null
                    && room.event != null
                    && room.phase != com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT) {
                return SignalDecision.stopHandled("already in event room");
            }
            openMapIfNeeded();
            IntentResult result =
                    artframework.sts1.input.Sts1MapIntentBridge.clickFirstPresentable("event");
            if (result == null || result.status == IntentResult.Status.REJECTED) {
                return SignalDecision.stopRejected(result != null ? result.message : "event map click failed");
            }
            return SignalDecision.stopHandled(result.message);
        } catch (Throwable t) {
            return SignalDecision.stopRejected(
                    t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
    }

    private static SignalDecision enterRoom(String roomKind) {
        String normalized = roomKind != null ? roomKind.trim().toLowerCase() : "";
        if (!"rest".equals(normalized)
                && !"shop".equals(normalized)
                && !"treasure".equals(normalized)) {
            return SignalDecision.stopRejected("unsupported room: " + roomKind);
        }
        try {
            if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.player == null) {
                return SignalDecision.stopRejected("run not ready");
            }
            com.megacrit.cardcrawl.rooms.AbstractRoom room = currentRoom();
            if (room != null
                    && room.getClass().getSimpleName().toLowerCase().contains(normalized)) {
                return SignalDecision.stopHandled("already in " + normalized + " room");
            }
            if (room != null
                    && room.phase == com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT
                    && !room.isBattleOver) {
                return SignalDecision.stopRejected("cannot enter room during live combat");
            }
            com.megacrit.cardcrawl.rooms.AbstractRoom next;
            if ("rest".equals(normalized)) {
                next = new com.megacrit.cardcrawl.rooms.RestRoom();
            } else if ("shop".equals(normalized)) {
                next = new com.megacrit.cardcrawl.rooms.ShopRoom();
            } else {
                next = new com.megacrit.cardcrawl.rooms.TreasureRoom();
            }
            com.megacrit.cardcrawl.map.MapRoomNode node =
                    com.megacrit.cardcrawl.dungeons.AbstractDungeon.currMapNode;
            if (node == null) {
                return SignalDecision.stopRejected("current map node unavailable");
            }
            node.room = next;
            com.megacrit.cardcrawl.dungeons.AbstractDungeon.screen =
                    com.megacrit.cardcrawl.dungeons.AbstractDungeon.CurrentScreen.NONE;
            com.megacrit.cardcrawl.dungeons.AbstractDungeon.previousScreen =
                    com.megacrit.cardcrawl.dungeons.AbstractDungeon.CurrentScreen.NONE;
            com.megacrit.cardcrawl.dungeons.AbstractDungeon.nextRoom = null;
            com.megacrit.cardcrawl.dungeons.AbstractDungeon.isScreenUp = false;
            next.onPlayerEntry();
            return SignalDecision.stopHandled(normalized + " room entered");
        } catch (Throwable t) {
            return SignalDecision.stopRejected(
                    t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
    }

    private static SignalDecision enterSelect(String selectKind) {
        String normalized = selectKind != null ? selectKind.trim().toLowerCase() : "";
        if (!"grid".equals(normalized) && !"hand".equals(normalized)) {
            return SignalDecision.stopRejected("unsupported select: " + selectKind);
        }
        try {
            if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.player == null) {
                return SignalDecision.stopRejected("run not ready");
            }
            final com.megacrit.cardcrawl.characters.AbstractPlayer player =
                    com.megacrit.cardcrawl.dungeons.AbstractDungeon.player;
            if ("grid".equals(normalized)) {
                if (player.masterDeck == null || player.masterDeck.group.isEmpty()) {
                    return SignalDecision.stopRejected("master deck is empty");
                }
                final com.megacrit.cardcrawl.cards.CardGroup group =
                        new com.megacrit.cardcrawl.cards.CardGroup(
                                player.masterDeck,
                                com.megacrit.cardcrawl.cards.CardGroup.CardGroupType.UNSPECIFIED);
                post(new Runnable() {
                    @Override
                    public void run() {
                        com.megacrit.cardcrawl.screens.select.GridCardSelectScreen screen =
                                com.megacrit.cardcrawl.dungeons.AbstractDungeon.gridSelectScreen;
                        if (screen == null) {
                            return;
                        }
                        screen.open(group, 1, false, "ART lab grid select");
                    }
                });
                return SignalDecision.stopHandled("grid select scheduled cards=" + group.group.size());
            }
            if (player.hand == null || player.hand.group.isEmpty()) {
                return SignalDecision.stopRejected("hand is empty");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    com.megacrit.cardcrawl.screens.select.HandCardSelectScreen screen =
                            com.megacrit.cardcrawl.dungeons.AbstractDungeon.handCardSelectScreen;
                    if (screen == null) {
                        return;
                    }
                    screen.open("ART lab hand select", 1, false, false, false);
                }
            });
            return SignalDecision.stopHandled("hand select scheduled cards=" + player.hand.group.size());
        } catch (Throwable t) {
            return SignalDecision.stopRejected(
                    t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
    }

    private static void post(Runnable runnable) {
        try {
            if (com.badlogic.gdx.Gdx.app != null) {
                com.badlogic.gdx.Gdx.app.postRunnable(runnable);
                return;
            }
        } catch (Throwable ignored) {
        }
        runnable.run();
    }

    private static com.megacrit.cardcrawl.rooms.AbstractRoom currentRoom() {
        try {
            if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.currMapNode == null) {
                return null;
            }
            return com.megacrit.cardcrawl.dungeons.AbstractDungeon.currMapNode.getRoom();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void openMapIfNeeded() {
        try {
            if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.screen != null
                    && "MAP".equals(com.megacrit.cardcrawl.dungeons.AbstractDungeon.screen.name())) {
                return;
            }
            if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.dungeonMapScreen != null) {
                com.megacrit.cardcrawl.dungeons.AbstractDungeon.dungeonMapScreen.open(false);
            }
        } catch (Throwable ignored) {
        }
    }
}
