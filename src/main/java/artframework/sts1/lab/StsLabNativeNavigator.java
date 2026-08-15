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
