package spireui.ops;

import spireui.c2.EndTurnInterceptor;
import spireui.c2.EventOptionInterceptor;
import spireui.c2.EventOptionRef;
import spireui.c2.GateResult;
import spireui.c2.MapNodeInterceptor;
import spireui.c2.MapNodeRef;
import spireui.c2.NativeTemplateRuntime;
import spireui.c2.SelectCardInterceptor;
import spireui.c2.SelectCardRef;
import spireui.c2.SelectConfirmInterceptor;
import spireui.c2.SelectKind;

/**
 * Temporary BLOCK interceptors for lab/device verification (not multiplayer policy).
 * Console: {@code spireui gate <map|event|endturn|select> block|clear}.
 */
public final class GateLab {

    private static final MapNodeInterceptor MAP_BLOCK =
            new MapNodeInterceptor() {
                @Override
                public Result intercept(MapNodeRef node) {
                    return Result.BLOCK;
                }
            };
    private static final EventOptionInterceptor EVENT_BLOCK =
            new EventOptionInterceptor() {
                @Override
                public GateResult intercept(EventOptionRef option) {
                    return GateResult.BLOCK;
                }
            };
    private static final EndTurnInterceptor END_TURN_BLOCK =
            new EndTurnInterceptor() {
                @Override
                public GateResult intercept() {
                    return GateResult.BLOCK;
                }
            };
    private static final SelectCardInterceptor SELECT_CARD_BLOCK =
            new SelectCardInterceptor() {
                @Override
                public GateResult intercept(SelectKind kind, SelectCardRef card) {
                    return GateResult.BLOCK;
                }
            };
    private static final SelectConfirmInterceptor SELECT_CONFIRM_BLOCK =
            new SelectConfirmInterceptor() {
                @Override
                public GateResult intercept(SelectKind kind) {
                    return GateResult.BLOCK;
                }
            };

    private static boolean mapOn;
    private static boolean eventOn;
    private static boolean endTurnOn;
    private static boolean selectGridOn;
    private static boolean selectHandOn;

    private GateLab() {}

    public static String apply(String target, String action) {
        if (target == null || action == null) {
            return "Usage: spireui gate <map|event|endturn|select-grid|select-hand|select|all> <block|clear>";
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
            NativeTemplateRuntime.map().addInterceptor(MAP_BLOCK);
            mapOn = true;
        } else if (!block && mapOn) {
            NativeTemplateRuntime.map().removeInterceptor(MAP_BLOCK);
            mapOn = false;
        }
    }

    private static void setEvent(boolean block) {
        if (block && !eventOn) {
            NativeTemplateRuntime.event().addInterceptor(EVENT_BLOCK);
            eventOn = true;
        } else if (!block && eventOn) {
            NativeTemplateRuntime.event().removeInterceptor(EVENT_BLOCK);
            eventOn = false;
        }
    }

    private static void setEndTurn(boolean block) {
        if (block && !endTurnOn) {
            NativeTemplateRuntime.endTurn().addInterceptor(END_TURN_BLOCK);
            endTurnOn = true;
        } else if (!block && endTurnOn) {
            NativeTemplateRuntime.endTurn().removeInterceptor(END_TURN_BLOCK);
            endTurnOn = false;
        }
    }

    private static void setSelect(SelectKind kind, boolean block) {
        if (kind == SelectKind.GRID) {
            if (block && !selectGridOn) {
                NativeTemplateRuntime.selectGrid().addCardInterceptor(SELECT_CARD_BLOCK);
                NativeTemplateRuntime.selectGrid().addConfirmInterceptor(SELECT_CONFIRM_BLOCK);
                selectGridOn = true;
            } else if (!block && selectGridOn) {
                NativeTemplateRuntime.selectGrid().removeCardInterceptor(SELECT_CARD_BLOCK);
                NativeTemplateRuntime.selectGrid().removeConfirmInterceptor(SELECT_CONFIRM_BLOCK);
                selectGridOn = false;
            }
        } else if (kind == SelectKind.HAND) {
            if (block && !selectHandOn) {
                NativeTemplateRuntime.selectHand().addCardInterceptor(SELECT_CARD_BLOCK);
                NativeTemplateRuntime.selectHand().addConfirmInterceptor(SELECT_CONFIRM_BLOCK);
                selectHandOn = true;
            } else if (!block && selectHandOn) {
                NativeTemplateRuntime.selectHand().removeCardInterceptor(SELECT_CARD_BLOCK);
                NativeTemplateRuntime.selectHand().removeConfirmInterceptor(SELECT_CONFIRM_BLOCK);
                selectHandOn = false;
            }
        }
    }
}
