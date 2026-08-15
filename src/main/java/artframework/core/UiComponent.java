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

    void disconnect(String signal, SignalHandler handler);

    SignalDispatchResult emit(String signal, Object... args);

    UiOpResult action(String name, Object... args);

    Map<String, Object> probeSlice();
}
