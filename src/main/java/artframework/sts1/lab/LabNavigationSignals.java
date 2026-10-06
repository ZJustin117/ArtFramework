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

    private static String lastError = "";
    private static int errorCount;

    private LabNavigationSignals() {}

    /**
     * Records a lab-navigation failure so it stays observable even when it happens on the app
     * thread inside a posted runnable. Surfaced by {@code art lab status} / {@code art lab dump}
     * through {@link LabRecipeRunner#statusMap()}.
     */
    public static synchronized void recordError(String context, Throwable error) {
        if (error == null) {
            return;
        }
        errorCount++;
        String prefix = context != null && !context.isEmpty() ? context + ": " : "";
        String message = error.getMessage();
        lastError =
                prefix
                        + error.getClass().getSimpleName()
                        + (message != null && !message.isEmpty() ? ": " + message : "");
    }

    /** Most recent lab-navigation failure, or the empty string. */
    public static synchronized String lastError() {
        return lastError;
    }

    /** Number of lab-navigation failures recorded since the last reset. */
    public static synchronized int errorCount() {
        return errorCount;
    }

    public static synchronized void resetErrorsForTests() {
        lastError = "";
        errorCount = 0;
    }

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
