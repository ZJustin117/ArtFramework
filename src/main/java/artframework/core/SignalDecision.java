package artframework.core;

/** Ordered broadcast decision. */
public final class SignalDecision {
    public enum Kind { CONTINUE, REPLACE, STOP_HANDLED, STOP_REJECTED }

    public final Kind kind;
    public final UiSignal signal;
    public final String message;

    private SignalDecision(Kind kind, UiSignal signal, String message) {
        this.kind = kind;
        this.signal = signal;
        this.message = message != null ? message : "";
    }

    public static SignalDecision continueSignal() { return new SignalDecision(Kind.CONTINUE, null, ""); }
    public static SignalDecision replace(UiSignal signal) { return new SignalDecision(Kind.REPLACE, signal, ""); }
    public static SignalDecision stopHandled(String message) { return new SignalDecision(Kind.STOP_HANDLED, null, message); }
    public static SignalDecision stopRejected(String message) { return new SignalDecision(Kind.STOP_REJECTED, null, message); }
}
