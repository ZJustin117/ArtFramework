package artframework.core;

import java.util.regex.Pattern;

/** Isolated routing boundary for operation signals. */
public final class SignalGroup {
    private final String id;
    private final SignalBus bus = new SignalBus();

    SignalGroup(String id) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("group id required");
        this.id = id;
    }

    public String id() { return id; }
    public SignalSubscription connect(String name, SignalListener listener) { return bus.connect(name, listener); }
    public SignalSubscription connect(Pattern pattern, SignalListener listener) { return bus.connect(pattern, listener); }

    public SignalDispatchResult emit(UiSignal signal) {
        if (signal == null) throw new IllegalArgumentException("signal required");
        if (!id.equals(signal.group)) {
            throw new IllegalArgumentException("signal belongs to group " + signal.group + ", not " + id);
        }
        return bus.emit(signal);
    }

    public void clear() { bus.clear(); }
}
