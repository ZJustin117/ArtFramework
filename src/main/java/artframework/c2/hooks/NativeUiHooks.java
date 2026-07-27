package artframework.c2.hooks;

import artframework.c2.EventOptionRef;
import artframework.c2.GateResult;
import artframework.c2.MapNodeInterceptor;
import artframework.c2.MapNodeRef;
import artframework.c2.NativeTemplateRuntime;
import artframework.c2.NativeComponents;
import artframework.c2.NativeTemplateIds;
import artframework.c2.SelectCardRef;
import artframework.c2.SelectKind;

/**
 * Pure entry points for {@code @SpirePatch} classes. No protocol. Inactive template → ALLOW
 * (vanilla STS continues) so consumers must bind + intercept to gate.
 */
public final class NativeUiHooks {

    private NativeUiHooks() {}

    public static MapNodeInterceptor.Result onMapNodeClick(int row, int col, String roomType) {
        if (!NativeTemplateRuntime.isMapBound()) {
            return MapNodeInterceptor.Result.ALLOW;
        }
        MapNodeRef node = new MapNodeRef(row, col, roomType);
        MapNodeInterceptor.Result result = NativeTemplateRuntime.map().dispatchNodeClick(node);
        if (result == MapNodeInterceptor.Result.ALLOW) {
            NativeComponents.emit(NativeTemplateIds.MAP, artframework.core.SignalNames.NODE_CLICKED, node);
        }
        return result;
    }

    public static GateResult onEventOption(int index, String label) {
        if (!NativeTemplateRuntime.isEventBound()) {
            return GateResult.ALLOW;
        }
        EventOptionRef option = new EventOptionRef(index, label);
        GateResult result = NativeTemplateRuntime.event().dispatchOption(option);
        if (result == GateResult.ALLOW) {
            NativeComponents.emit(
                    NativeTemplateIds.EVENT,
                    artframework.core.SignalNames.OPTION_CHOSEN,
                    Integer.valueOf(index),
                    label != null ? label : "");
        }
        return result;
    }

    public static GateResult onSelectCard(SelectKind kind, String cardId, int index) {
        if (kind == SelectKind.GRID && !NativeTemplateRuntime.isSelectGridBound()) {
            return GateResult.ALLOW;
        }
        if (kind == SelectKind.HAND && !NativeTemplateRuntime.isSelectHandBound()) {
            return GateResult.ALLOW;
        }
        if (kind == SelectKind.GRID) {
            SelectCardRef card = new SelectCardRef(cardId, index);
            GateResult result = NativeTemplateRuntime.selectGrid().dispatchCard(card);
            if (result == GateResult.ALLOW) {
                NativeComponents.emit(
                        NativeTemplateIds.SELECT_GRID,
                        artframework.core.SignalNames.CARD_SELECTED,
                        card);
            }
            return result;
        }
        if (kind == SelectKind.HAND) {
            SelectCardRef card = new SelectCardRef(cardId, index);
            GateResult result = NativeTemplateRuntime.selectHand().dispatchCard(card);
            if (result == GateResult.ALLOW) {
                NativeComponents.emit(
                        NativeTemplateIds.SELECT_HAND,
                        artframework.core.SignalNames.CARD_SELECTED,
                        card);
            }
            return result;
        }
        return GateResult.ALLOW;
    }

    public static GateResult onSelectConfirm(SelectKind kind) {
        if (kind == SelectKind.GRID && !NativeTemplateRuntime.isSelectGridBound()) {
            return GateResult.ALLOW;
        }
        if (kind == SelectKind.HAND && !NativeTemplateRuntime.isSelectHandBound()) {
            return GateResult.ALLOW;
        }
        if (kind == SelectKind.GRID) {
            GateResult result = NativeTemplateRuntime.selectGrid().dispatchConfirm();
            if (result == GateResult.ALLOW) {
                NativeComponents.emit(
                        NativeTemplateIds.SELECT_GRID,
                        artframework.core.SignalNames.CONFIRMED,
                        kind);
            }
            return result;
        }
        if (kind == SelectKind.HAND) {
            GateResult result = NativeTemplateRuntime.selectHand().dispatchConfirm();
            if (result == GateResult.ALLOW) {
                NativeComponents.emit(
                        NativeTemplateIds.SELECT_HAND,
                        artframework.core.SignalNames.CONFIRMED,
                        kind);
            }
            return result;
        }
        return GateResult.ALLOW;
    }

    public static GateResult onEndTurnPress() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return GateResult.ALLOW;
        }
        GateResult result = NativeTemplateRuntime.endTurn().dispatchPress();
        if (result == GateResult.ALLOW) {
            NativeComponents.emit(NativeTemplateIds.END_TURN, artframework.core.SignalNames.PRESSED);
        }
        return result;
    }

    /** Whether end-turn UI should be allowed to enable (presentation hint). */
    public static boolean isEndTurnEnabledHint() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return true;
        }
        return NativeTemplateRuntime.endTurn().isButtonEnabled();
    }
}
