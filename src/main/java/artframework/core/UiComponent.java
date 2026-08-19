package artframework.core;

import artframework.api.UiOpResult;

import java.util.Map;

/**
 * Unified component surface for C1 synthetic roots and C2 native hosts.
 */
public interface UiComponent {

    String id();

    ComponentKind kind();

    boolean isMounted();

    void mount();

    void unmount();

    SignalSubscription connect(String signal, SignalHandler handler);

    default SignalSubscription connectListener(String signal, SignalListener listener) {
        throw new UnsupportedOperationException("decision-aware listeners are not supported");
    }

    void disconnect(String signal, SignalHandler handler);

    default void disconnectListener(String signal, SignalListener listener) {
        throw new UnsupportedOperationException("decision-aware listeners are not supported");
    }

    SignalDispatchResult emit(String signal, Object... args);

    /** Canonical result-bearing signal operation. */
    default SignalDispatchResult dispatch(String signal, Object... args) {
        return emit(signal, args);
    }

    UiOpResult action(String name, Object... args);

    Map<String, Object> probeSlice();
}
