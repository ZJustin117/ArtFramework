package artframework.core;

import java.util.List;
import java.util.regex.Pattern;

/** Isolated routing boundary for operation signals. */
public final class SignalGroup {
    private final String id;
    private final boolean runtimeAuthorityOnly;
    private final SignalBus bus = new SignalBus();

    SignalGroup(String id) {
        this(id, false);
    }

    SignalGroup(String id, boolean runtimeAuthorityOnly) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("group id required");
        this.id = id;
        this.runtimeAuthorityOnly = runtimeAuthorityOnly;
    }

    public String id() { return id; }
    public SignalSubscription connect(String name, SignalListener listener) {
        if (runtimeAuthorityOnly) throw new IllegalStateException("transient runtime group is owned by TransientSignalRuntime");
        return bus.connect(name, listener);
    }
    public SignalSubscription connect(Pattern pattern, SignalListener listener) {
        if (runtimeAuthorityOnly) throw new IllegalStateException("transient runtime group is owned by TransientSignalRuntime");
        return bus.connect(pattern, listener);
    }

    SignalSubscription connectForRuntime(String name, SignalListener listener) { return bus.connect(name, listener); }
    SignalSubscription connectForRuntime(Pattern pattern, SignalListener listener) { return bus.connect(pattern, listener); }

    public SignalDispatchResult emit(UiSignal signal) {
        return dispatch(signal);
    }

    public SignalDispatchResult dispatch(UiSignal signal) {
        if (signal == null) throw new IllegalArgumentException("signal required");
        if (!id.equals(signal.group)) {
            throw new IllegalArgumentException("signal belongs to group " + signal.group + ", not " + id);
        }
        return bus.emit(signal);
    }

    CapturedDispatch capture(UiSignal signal) {
        requireGroup(signal);
        return new CapturedDispatch(signal, bus.capture(signal));
    }

    SignalDispatchResult dispatch(CapturedDispatch captured) {
        return bus.emitCaptured(captured.signal, captured.entries);
    }

    public void clear() { bus.clear(); }

    private void requireGroup(UiSignal signal) {
        if (signal == null) throw new IllegalArgumentException("signal required");
        if (!id.equals(signal.group)) {
            throw new IllegalArgumentException("signal belongs to group " + signal.group + ", not " + id);
        }
    }

    static final class CapturedDispatch {
        final UiSignal signal;
        final List<SignalBus.Entry> entries;
        CapturedDispatch(UiSignal signal, List<SignalBus.Entry> entries) {
            this.signal = signal;
            this.entries = entries;
        }
    }
}
