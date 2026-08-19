package artframework.sts1.lab;

import artframework.api.UiOpResult;
import artframework.core.SignalGroups;
import artframework.core.SignalDecision;
import artframework.core.SignalDispatchResult;
import artframework.core.UiSignal;

/** Dispatches lab navigation intents without letting lab commands mutate presentation state. */
public final class LabNavigationSignals {

    public static final String REQUEST = "lab/navigation/request";
    public static final String SOURCE = "art.lab";

    private LabNavigationSignals() {}

    public static UiOpResult dispatch(LabNavigationIntent intent) {
        if (intent == null || intent.name.isEmpty()) {
            return UiOpResult.unavailable("lab intent required");
        }
        SignalDispatchResult result =
                SignalGroups.nativeGroup().dispatch(new UiSignal(REQUEST, SOURCE, intent));
        if (result == null || result.terminal == SignalDecision.Kind.CONTINUE) {
            return UiOpResult.unavailable("no lab navigator installed: " + intent.name);
        }
        if (result.terminal == SignalDecision.Kind.STOP_REJECTED) {
            return UiOpResult.unavailable(result.message);
        }
        return UiOpResult.ok(result.message != null && !result.message.isEmpty() ? result.message : intent.name);
    }
}
