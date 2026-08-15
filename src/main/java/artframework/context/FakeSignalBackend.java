package artframework.context;

import artframework.core.SignalGroups;
import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.SignalSubscription;
import artframework.core.UiSignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Scriptable backend listener/emitter for signal tests. */
public final class FakeSignalBackend implements SignalBackend {
    private final String id;
    private BackendMode mode = BackendMode.FULL_CONTROL;
    private ContextFrame current = ContextFrame.unavailable(0L);
    private final List<UiSignal> signalLog = new ArrayList<UiSignal>();
    private boolean accept = true;
    private String rejectMessage = "rejected by fake";
    private SignalSubscription subscription;

    public FakeSignalBackend() { this("fake"); }
    public FakeSignalBackend(String id) { this.id = id != null ? id : "fake"; }
    public String id() { return id; }
    public FakeSignalBackend setMode(BackendMode value) { mode = value != null ? value : BackendMode.FULL_CONTROL; return this; }
    public FakeSignalBackend setAccept(boolean value) { accept = value; return this; }
    public FakeSignalBackend setRejectMessage(String value) { rejectMessage = value != null ? value : "rejected by fake"; return this; }
    public FakeSignalBackend publish(ContextFrame frame) { current = require(frame); return this; }
    public ContextFrame currentFrame() { return current; }
    public List<UiSignal> signalLog() { return Collections.unmodifiableList(new ArrayList<UiSignal>(signalLog)); }
    public void clearSignalLog() { signalLog.clear(); }

    @Override public void installSignals() {
        if (subscription != null && subscription.isConnected()) return;
        subscription = SignalGroups.nativeGroup().connect(Pattern.compile("^ui/.+/.+$"), new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                if (!(signal.payload instanceof UiIntent)) return SignalDecision.continueSignal();
                signalLog.add(signal);
                if (mode == BackendMode.READ_ONLY) return SignalDecision.stopRejected("read-only backend");
                return accept ? SignalDecision.continueSignal() : SignalDecision.stopRejected(rejectMessage);
            }
        });
    }

    @Override public void uninstallSignals() {
        if (subscription != null) subscription.disconnect();
        subscription = null;
    }

    private static ContextFrame require(ContextFrame frame) {
        if (frame == null) throw new IllegalArgumentException("frame required");
        return frame;
    }
}
