package spireui.c2.hooks;

import spireui.c2.EventOptionRef;
import spireui.c2.GateResult;
import spireui.c2.MapNodeInterceptor;
import spireui.c2.MapNodeRef;
import spireui.c2.NativeTemplateRuntime;
import spireui.c2.SelectCardRef;
import spireui.c2.SelectKind;

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
        return NativeTemplateRuntime.map().dispatchNodeClick(new MapNodeRef(row, col, roomType));
    }

    public static GateResult onEventOption(int index, String label) {
        if (!NativeTemplateRuntime.isEventBound()) {
            return GateResult.ALLOW;
        }
        return NativeTemplateRuntime.event().dispatchOption(new EventOptionRef(index, label));
    }

    public static GateResult onSelectCard(SelectKind kind, String cardId, int index) {
        if (kind == SelectKind.GRID && !NativeTemplateRuntime.isSelectGridBound()) {
            return GateResult.ALLOW;
        }
        if (kind == SelectKind.HAND && !NativeTemplateRuntime.isSelectHandBound()) {
            return GateResult.ALLOW;
        }
        if (kind == SelectKind.GRID) {
            return NativeTemplateRuntime.selectGrid()
                    .dispatchCard(new SelectCardRef(cardId, index));
        }
        if (kind == SelectKind.HAND) {
            return NativeTemplateRuntime.selectHand()
                    .dispatchCard(new SelectCardRef(cardId, index));
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
            return NativeTemplateRuntime.selectGrid().dispatchConfirm();
        }
        if (kind == SelectKind.HAND) {
            return NativeTemplateRuntime.selectHand().dispatchConfirm();
        }
        return GateResult.ALLOW;
    }

    public static GateResult onEndTurnPress() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return GateResult.ALLOW;
        }
        return NativeTemplateRuntime.endTurn().dispatchPress();
    }

    /** Whether end-turn UI should be allowed to enable (presentation hint). */
    public static boolean isEndTurnEnabledHint() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return true;
        }
        return NativeTemplateRuntime.endTurn().isButtonEnabled();
    }
}
