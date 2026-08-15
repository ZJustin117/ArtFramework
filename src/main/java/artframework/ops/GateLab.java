package artframework.ops;

import artframework.c2.NativeTemplateIds;
import artframework.c2.SelectKind;
import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.SignalPaths;
import artframework.core.SignalGroups;
import artframework.core.SignalSubscription;
import artframework.core.UiSignal;

/**
 * Temporary BLOCK interceptors for lab/device verification (not multiplayer policy).
 * Console: {@code artframework gate <map|event|endturn|select> block|clear}.
 */
public final class GateLab {

    private static SignalSubscription mapSubscription;
    private static SignalSubscription eventSubscription;
    private static SignalSubscription endTurnSubscription;
    private static SignalSubscription selectGridCardSubscription;
    private static SignalSubscription selectGridConfirmSubscription;
    private static SignalSubscription selectHandCardSubscription;
    private static SignalSubscription selectHandConfirmSubscription;

    private static boolean mapOn;
    private static boolean eventOn;
    private static boolean endTurnOn;
    private static boolean selectGridOn;
    private static boolean selectHandOn;

    private GateLab() {}

    public static String apply(String target, String action) {
        if (target == null || action == null) {
            return "Usage: artframework gate <map|event|endturn|select-grid|select-hand|select|all> <block|clear>";
        }
        String t = target.toLowerCase();
        String a = action.toLowerCase();
        boolean block;
        if ("block".equals(a) || "on".equals(a)) {
            block = true;
        } else if ("clear".equals(a) || "off".equals(a)) {
            block = false;
        } else {
            return "unknown action: " + action + " (use block|clear)";
        }
        if ("all".equals(t)) {
            setMap(block);
            setEvent(block);
            setEndTurn(block);
            setSelect(SelectKind.GRID, block);
            setSelect(SelectKind.HAND, block);
            return status();
        }
        if ("map".equals(t)) {
            setMap(block);
            return status();
        }
        if ("event".equals(t)) {
            setEvent(block);
            return status();
        }
        if ("endturn".equals(t) || "end-turn".equals(t)) {
            setEndTurn(block);
            return status();
        }
        if ("select".equals(t) || "select-grid".equals(t) || "grid".equals(t)) {
            setSelect(SelectKind.GRID, block);
            if ("select".equals(t)) {
                setSelect(SelectKind.HAND, block);
            }
            return status();
        }
        if ("select-hand".equals(t) || "hand".equals(t)) {
            setSelect(SelectKind.HAND, block);
            return status();
        }
        return "unknown target: " + target;
    }

    public static String status() {
        return "gate map="
                + mapOn
                + " event="
                + eventOn
                + " endturn="
                + endTurnOn
                + " selectGrid="
                + selectGridOn
                + " selectHand="
                + selectHandOn;
    }

    public static void resetForTests() {
        setMap(false);
        setEvent(false);
        setEndTurn(false);
        setSelect(SelectKind.GRID, false);
        setSelect(SelectKind.HAND, false);
    }

    private static void setMap(boolean block) {
        if (block && !mapOn) {
            mapSubscription = block(SignalPaths.component(NativeTemplateIds.MAP, "node_clicked"));
            mapOn = true;
        } else if (!block && mapOn) {
            mapSubscription.disconnect();
            mapOn = false;
        }
    }

    private static void setEvent(boolean block) {
        if (block && !eventOn) {
            eventSubscription = block(SignalPaths.component(NativeTemplateIds.EVENT, "option_chosen"));
            eventOn = true;
        } else if (!block && eventOn) {
            eventSubscription.disconnect();
            eventOn = false;
        }
    }

    private static void setEndTurn(boolean block) {
        if (block && !endTurnOn) {
            endTurnSubscription = block(SignalPaths.component(NativeTemplateIds.END_TURN, "pressed"));
            endTurnOn = true;
        } else if (!block && endTurnOn) {
            endTurnSubscription.disconnect();
            endTurnOn = false;
        }
    }

    private static void setSelect(SelectKind kind, boolean block) {
        if (kind == SelectKind.GRID) {
            if (block && !selectGridOn) {
                selectGridCardSubscription = block(SignalPaths.component(NativeTemplateIds.SELECT_GRID, "card_selected"));
                selectGridConfirmSubscription = block(SignalPaths.component(NativeTemplateIds.SELECT_GRID, "confirmed"));
                selectGridOn = true;
            } else if (!block && selectGridOn) {
                selectGridCardSubscription.disconnect();
                selectGridConfirmSubscription.disconnect();
                selectGridOn = false;
            }
        } else if (kind == SelectKind.HAND) {
            if (block && !selectHandOn) {
                selectHandCardSubscription = block(SignalPaths.component(NativeTemplateIds.SELECT_HAND, "card_selected"));
                selectHandConfirmSubscription = block(SignalPaths.component(NativeTemplateIds.SELECT_HAND, "confirmed"));
                selectHandOn = true;
            } else if (!block && selectHandOn) {
                selectHandCardSubscription.disconnect();
                selectHandConfirmSubscription.disconnect();
                selectHandOn = false;
            }
        }
    }

    private static SignalSubscription block(String name) {
        return SignalGroups.nativeGroup().connect(name, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return SignalDecision.stopRejected("blocked by gate lab");
            }
        });
    }
}
