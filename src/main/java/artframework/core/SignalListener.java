package artframework.core;

/** Receives one signal and controls delivery to subsequent listeners. */
public interface SignalListener {
    SignalDecision onSignal(UiSignal signal);
}
