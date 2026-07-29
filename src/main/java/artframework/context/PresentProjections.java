package artframework.context;

import artframework.core.SignalBuses;
import artframework.core.SignalDecision;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalListener;
import artframework.core.SignalSubscription;
import artframework.core.UiSignal;

/** Global presentation projection, maintained by an ordinary frame-signal listener. */
public final class PresentProjections {
    private static final PresentProjection PROJECTION = new PresentProjection();
    private static SignalSubscription subscription;
    private static FrameDiff last = FrameDiff.skipped("no frame signal received");

    private PresentProjections() {}

    public static PresentProjection get() {
        ensureListener();
        return PROJECTION;
    }

    public static FrameDiff publish(ContextFrame frame) {
        if (frame == null) return FrameDiff.skipped("frame required");
        ensureListener();
        last = FrameDiff.skipped("frame listener did not run");
        SignalDispatchResult result = SignalBuses.get().emit(
                new UiSignal(ContextSignals.FRAME_UPDATED, "context", frame));
        return result.isRejected() ? FrameDiff.skipped(result.message) : last;
    }

    public static void resetForTests() {
        PROJECTION.reset();
        subscription = null;
        ensureListener();
    }

    private static void ensureListener() {
        if (subscription != null && subscription.isConnected()) return;
        subscription = SignalBuses.get().connect(ContextSignals.FRAME_UPDATED, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                if (!(signal.payload instanceof ContextFrame)) {
                    return SignalDecision.stopRejected("context frame payload required");
                }
                last = PROJECTION.applyFrame((ContextFrame) signal.payload);
                return SignalDecision.continueSignal();
            }
        });
    }
}
