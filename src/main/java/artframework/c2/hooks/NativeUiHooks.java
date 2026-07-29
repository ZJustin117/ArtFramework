package artframework.c2.hooks;

import artframework.c2.EventOptionRef;
import artframework.c2.MapNodeRef;
import artframework.c2.NativeTemplateRuntime;
import artframework.c2.NativeTemplateIds;
import artframework.c2.SelectCardRef;
import artframework.c2.SelectKind;
import artframework.core.SignalBuses;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalNames;
import artframework.core.UiSignal;

/**
 * Pure entry points for {@code @SpirePatch} classes. Returns bus delivery results; host patches use
 * {@link HostPatchResults} for SpireReturn / continue decisions (milestone 21.5).
 */
public final class NativeUiHooks {

    private NativeUiHooks() {}

    public static SignalDispatchResult onMapNodeClick(int row, int col, String roomType) {
        if (!NativeTemplateRuntime.isMapBound()) {
            return SignalDispatchResult.continueEmpty("map unbound");
        }
        MapNodeRef node = new MapNodeRef(row, col, roomType);
        return emit(NativeTemplateIds.MAP, SignalNames.NODE_CLICKED, node);
    }

    public static SignalDispatchResult onEventOption(int index, String label) {
        if (!NativeTemplateRuntime.isEventBound()) {
            return SignalDispatchResult.continueEmpty("event unbound");
        }
        EventOptionRef option = new EventOptionRef(index, label);
        return emit(NativeTemplateIds.EVENT, SignalNames.OPTION_CHOSEN, option);
    }

    public static SignalDispatchResult onSelectCard(SelectKind kind, String cardId, int index) {
        if (kind == SelectKind.GRID && !NativeTemplateRuntime.isSelectGridBound()) {
            return SignalDispatchResult.continueEmpty("select grid unbound");
        }
        if (kind == SelectKind.HAND && !NativeTemplateRuntime.isSelectHandBound()) {
            return SignalDispatchResult.continueEmpty("select hand unbound");
        }
        if (kind == SelectKind.GRID) {
            return emit(
                    NativeTemplateIds.SELECT_GRID,
                    SignalNames.CARD_SELECTED,
                    new SelectCardRef(cardId, index));
        }
        if (kind == SelectKind.HAND) {
            return emit(
                    NativeTemplateIds.SELECT_HAND,
                    SignalNames.CARD_SELECTED,
                    new SelectCardRef(cardId, index));
        }
        return SignalDispatchResult.continueEmpty("unknown select kind");
    }

    public static SignalDispatchResult onSelectConfirm(SelectKind kind) {
        if (kind == SelectKind.GRID && !NativeTemplateRuntime.isSelectGridBound()) {
            return SignalDispatchResult.continueEmpty("select grid unbound");
        }
        if (kind == SelectKind.HAND && !NativeTemplateRuntime.isSelectHandBound()) {
            return SignalDispatchResult.continueEmpty("select hand unbound");
        }
        if (kind == SelectKind.GRID) {
            return emit(NativeTemplateIds.SELECT_GRID, SignalNames.CONFIRMED, kind);
        }
        if (kind == SelectKind.HAND) {
            return emit(NativeTemplateIds.SELECT_HAND, SignalNames.CONFIRMED, kind);
        }
        return SignalDispatchResult.continueEmpty("unknown select kind");
    }

    public static SignalDispatchResult onEndTurnPress() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return SignalDispatchResult.continueEmpty("endturn unbound");
        }
        if (!NativeTemplateRuntime.endTurn().isButtonEnabled()) {
            return SignalDispatchResult.rejected("endturn disabled");
        }
        return emit(NativeTemplateIds.END_TURN, SignalNames.PRESSED, null);
    }

    /** Whether end-turn UI should be allowed to enable (presentation hint). */
    public static boolean isEndTurnEnabledHint() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return true;
        }
        return NativeTemplateRuntime.endTurn().isButtonEnabled();
    }

    private static SignalDispatchResult emit(String componentId, String signal, Object payload) {
        return SignalBuses.get()
                .emit(new UiSignal(signalName(componentId, signal), componentId, payload));
    }

    private static String signalName(String componentId, String signal) {
        return "ui/" + componentId + "/" + signal;
    }
}
