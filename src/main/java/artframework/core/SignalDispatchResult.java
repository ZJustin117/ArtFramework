package artframework.core;

/** Final result of ordered signal delivery. */
public final class SignalDispatchResult {
    public final UiSignal signal;
    public final SignalDecision.Kind terminal;
    public final String message;

    SignalDispatchResult(UiSignal signal, SignalDecision.Kind terminal, String message) {
        this.signal = signal;
        this.terminal = terminal;
        this.message = message != null ? message : "";
    }

    /** Host/patch helper: native path continues without a delivered signal. */
    public static SignalDispatchResult continueEmpty(String message) {
        return new SignalDispatchResult(null, SignalDecision.Kind.CONTINUE, message);
    }

    /** Host/patch helper: policy rejected the native path. */
    public static SignalDispatchResult rejected(String message) {
        return new SignalDispatchResult(null, SignalDecision.Kind.STOP_REJECTED, message);
    }

    public boolean isStopped() { return terminal == SignalDecision.Kind.STOP_HANDLED || terminal == SignalDecision.Kind.STOP_REJECTED; }
    public boolean isRejected() { return terminal == SignalDecision.Kind.STOP_REJECTED; }
}
