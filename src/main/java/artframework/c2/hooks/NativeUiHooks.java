package artframework.c2.hooks;

import artframework.c2.EventOptionRef;
import artframework.component.MapNodeRef;
import artframework.c2.NativeTemplateRuntime;
import artframework.component.NativeTemplateIds;
import artframework.c2.SelectCardRef;
import artframework.c2.SelectKind;
import artframework.core.SignalGroups;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalNames;
import artframework.core.SignalPaths;
import artframework.core.UiSignal;
import artframework.context.NativeInputComponent;
import artframework.context.IntentNames;
import artframework.context.SurfaceIds;
import artframework.sts1.input.NativeInputRecords;

/**
 * Pure entry points for {@code @SpirePatch} classes. Returns bus delivery results; host patches use
 * {@link HostPatchResults} for SpireReturn / continue decisions (milestone 21.5).
 */
public final class NativeUiHooks {

    private NativeUiHooks() {}

    public static SignalDispatchResult onMapNodeClick(int row, int col, String roomType) {
        NativeInputRecords.input(IntentNames.CLICK_MAP_NODE, SurfaceIds.MAP);
        if (!NativeTemplateRuntime.isMapBound()) {
            NativeInputRecords.intercept(SurfaceIds.MAP, false, false, "map_unbound");
            return SignalDispatchResult.continueEmpty("map unbound");
        }
        MapNodeRef node = new MapNodeRef(row, col, roomType);
        return record(SurfaceIds.MAP, emit(NativeTemplateIds.MAP, SignalNames.NODE_CLICKED, node));
    }

    public static SignalDispatchResult onEventOption(int index, String label) {
        NativeInputRecords.input(IntentNames.CHOOSE_EVENT_OPTION, SurfaceIds.EVENT);
        if (!NativeTemplateRuntime.isEventBound()) {
            NativeInputRecords.intercept(SurfaceIds.EVENT, false, false, "event_unbound");
            return SignalDispatchResult.continueEmpty("event unbound");
        }
        EventOptionRef option = new EventOptionRef(index, label);
        return record(SurfaceIds.EVENT, emit(NativeTemplateIds.EVENT, SignalNames.OPTION_CHOSEN, option));
    }

    public static SignalDispatchResult onSelectCard(SelectKind kind, String cardId, int index) {
        String surfaceId = selectSurface(kind);
        NativeInputRecords.input(IntentNames.SELECT_CARD, surfaceId);
        if (kind == SelectKind.GRID && !NativeTemplateRuntime.isSelectGridBound()) {
            NativeInputRecords.intercept(surfaceId, false, false, "select_grid_unbound");
            return SignalDispatchResult.continueEmpty("select grid unbound");
        }
        if (kind == SelectKind.HAND && !NativeTemplateRuntime.isSelectHandBound()) {
            NativeInputRecords.intercept(surfaceId, false, false, "select_hand_unbound");
            return SignalDispatchResult.continueEmpty("select hand unbound");
        }
        if (kind == SelectKind.GRID) {
            return record(
                    surfaceId,
                    emit(
                    NativeTemplateIds.SELECT_GRID,
                    SignalNames.CARD_SELECTED,
                    new SelectCardRef(cardId, index)));
        }
        if (kind == SelectKind.HAND) {
            return record(
                    surfaceId,
                    emit(
                    NativeTemplateIds.SELECT_HAND,
                    SignalNames.CARD_SELECTED,
                    new SelectCardRef(cardId, index)));
        }
        NativeInputRecords.intercept(surfaceId, false, false, "unknown_select_kind");
        return SignalDispatchResult.continueEmpty("unknown select kind");
    }

    public static SignalDispatchResult onSelectConfirm(SelectKind kind) {
        String surfaceId = selectSurface(kind);
        NativeInputRecords.input(IntentNames.CONFIRM_SELECT, surfaceId);
        if (kind == SelectKind.GRID && !NativeTemplateRuntime.isSelectGridBound()) {
            NativeInputRecords.intercept(surfaceId, false, false, "select_grid_unbound");
            return SignalDispatchResult.continueEmpty("select grid unbound");
        }
        if (kind == SelectKind.HAND && !NativeTemplateRuntime.isSelectHandBound()) {
            NativeInputRecords.intercept(surfaceId, false, false, "select_hand_unbound");
            return SignalDispatchResult.continueEmpty("select hand unbound");
        }
        if (kind == SelectKind.GRID) {
            return record(surfaceId, emit(NativeTemplateIds.SELECT_GRID, SignalNames.CONFIRMED, kind));
        }
        if (kind == SelectKind.HAND) {
            return record(surfaceId, emit(NativeTemplateIds.SELECT_HAND, SignalNames.CONFIRMED, kind));
        }
        NativeInputRecords.intercept(surfaceId, false, false, "unknown_select_kind");
        return SignalDispatchResult.continueEmpty("unknown select kind");
    }

    public static SignalDispatchResult onEndTurnPress() {
        NativeInputRecords.input(IntentNames.PRESS_END_TURN, SurfaceIds.END_TURN);
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            NativeInputRecords.intercept(SurfaceIds.END_TURN, false, false, "endturn_unbound");
            return SignalDispatchResult.continueEmpty("endturn unbound");
        }
        if (!NativeTemplateRuntime.endTurn().isButtonEnabled()) {
            NativeInputRecords.intercept(SurfaceIds.END_TURN, false, true, "endturn_disabled");
            return SignalDispatchResult.rejected("endturn disabled");
        }
        return record(SurfaceIds.END_TURN, emit(NativeTemplateIds.END_TURN, SignalNames.PRESSED, null));
    }

    /** Whether end-turn UI should be allowed to enable (presentation hint). */
    public static boolean isEndTurnEnabledHint() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return true;
        }
        return NativeTemplateRuntime.endTurn().isButtonEnabled();
    }

    private static SignalDispatchResult emit(String componentId, String signal, Object payload) {
        return SignalGroups.nativeGroup()
                .emit(new UiSignal(SignalPaths.component(componentId, signal), componentId, payload));
    }

    private static SignalDispatchResult record(String surfaceId, SignalDispatchResult result) {
        boolean suppress = result != null && result.isRejected();
        NativeInputRecords.intercept(surfaceId, suppress, suppress,
                suppress ? "rejected" : "emitted");
        return result;
    }

    private static String selectSurface(SelectKind kind) {
        return kind == SelectKind.HAND ? SurfaceIds.SELECT_HAND : SurfaceIds.SELECT_GRID;
    }

}
