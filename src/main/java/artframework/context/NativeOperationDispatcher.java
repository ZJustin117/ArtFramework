package artframework.context;

import artframework.core.SignalDispatchResult;
import artframework.core.SignalGroups;
import artframework.core.UiSignal;

/** Dispatches external operations into the default native signal group. */
public final class NativeOperationDispatcher {
    private NativeOperationDispatcher() {}

    public static SignalDispatchResult dispatch(UiIntent intent) {
        if (intent == null) throw new IllegalArgumentException("intent required");
        return SignalGroups.nativeGroup().emit(new UiSignal(
                ContextSignals.action(intent.surfaceId, intent.name), intent.surfaceId, intent));
    }
}
