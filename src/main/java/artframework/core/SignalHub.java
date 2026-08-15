package artframework.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Per-tree signal connect / emit / disconnect (pure; no GL).
 * Exact instance/signal pairs and raw bus name / regex share {@link SignalBus}.
 */
public final class SignalHub {

    private final String windowId;
    private final List<Registration> registrations = new ArrayList<Registration>();

    /** Component-level hub using {@code ui/<component>/<signal>} names. */
    public SignalHub() {
        this.windowId = null;
    }

    /** C1 window hub using {@code ui/<window>/<node-path>/<signal>} names. */
    public SignalHub(String windowId) {
        if (windowId == null || windowId.trim().isEmpty()) {
            throw new IllegalArgumentException("windowId required");
        }
        this.windowId = windowId;
    }

    public SignalSubscription connect(String nodePath, String signal, SignalHandler handler) {
        if (nodePath == null || signal == null || handler == null) {
            throw new IllegalArgumentException("nodePath, signal, handler required");
        }
        SignalSubscription busSubscription = SignalGroups.nativeGroup()
                .connect(routeName(nodePath, signal), wrapHandler(handler));
        Registration registration = new Registration(nodePath, signal, handler, busSubscription);
        registrations.add(registration);
        return new ManagedSubscription(registration);
    }

    /**
     * Subscribe to a full bus signal name (e.g. {@code ui/ok/pressed}), same as
     * {@link SignalBus#connect(String, SignalListener)}.
     */
    public SignalSubscription connectBus(String busName, SignalListener listener) {
        if (busName == null || busName.isEmpty() || listener == null) {
            throw new IllegalArgumentException("busName and listener required");
        }
        SignalSubscription busSubscription = SignalGroups.nativeGroup().connect(busName, listener);
        Registration registration = new Registration("", busName, null, busSubscription);
        registrations.add(registration);
        return new ManagedSubscription(registration);
    }

    /**
     * Regex subscribe on full bus names — same semantics as
     * {@link SignalBus#connect(Pattern, SignalListener)}.
     */
    public SignalSubscription connectBus(Pattern pattern, SignalListener listener) {
        if (pattern == null || listener == null) {
            throw new IllegalArgumentException("pattern and listener required");
        }
        SignalSubscription busSubscription = SignalGroups.nativeGroup().connect(pattern, listener);
        Registration registration = new Registration("", pattern.pattern(), null, busSubscription);
        registrations.add(registration);
        return new ManagedSubscription(registration);
    }

    public void disconnect(String nodePath, String signal, SignalHandler handler) {
        if (nodePath == null || signal == null || handler == null) {
            return;
        }
        for (Registration registration : new ArrayList<Registration>(registrations)) {
            if (registration.instanceId.equals(nodePath)
                    && registration.signal.equals(signal)
                    && registration.handler == handler) {
                registration.subscription.disconnect();
                registrations.remove(registration);
            }
        }
    }

    public void emit(String nodePath, String signal, Object... args) {
        dispatch(nodePath, signal, args);
    }

    public SignalDispatchResult dispatch(String nodePath, String signal, Object... args) {
        if (nodePath == null || signal == null) {
            throw new IllegalArgumentException("nodePath and signal required");
        }
        Object[] payload = args != null ? args : new Object[0];
        return SignalGroups.nativeGroup().emit(
                new UiSignal(routeName(nodePath, signal), nodePath, payload));
    }

    public void clear() {
        for (Registration registration : registrations) {
            registration.subscription.disconnect();
        }
        registrations.clear();
    }

    /** Remove all handlers for one instance id. */
    public void clearInstance(String instanceId) {
        if (instanceId == null) {
            return;
        }
        for (Registration registration : new ArrayList<Registration>(registrations)) {
            if (registration.instanceId.equals(instanceId)) {
                registration.subscription.disconnect();
                registrations.remove(registration);
            }
        }
    }

    public int handlerCount(String instanceId, String signal) {
        int total = 0;
        for (Registration registration : registrations) {
            if (registration.instanceId.equals(instanceId)
                    && registration.signal.equals(signal)
                    && registration.subscription.isConnected()) {
                total++;
            }
        }
        return total;
    }

    /** Canonical external/C2 component signal name. */
    public static String name(String componentId, String signal) {
        return SignalPaths.component(componentId, signal);
    }

    /** Canonical C1 node signal name. */
    public static String name(String windowId, String nodePath, String signal) {
        return SignalPaths.node(windowId, nodePath, signal);
    }

    private String routeName(String source, String signal) {
        return windowId != null ? name(windowId, source, signal) : name(source, signal);
    }

    private static SignalListener wrapHandler(final SignalHandler handler) {
        return new SignalListener() {
            @Override
            public SignalDecision onSignal(UiSignal event) {
                Object[] payload =
                        event.payload instanceof Object[]
                                ? (Object[]) event.payload
                                : new Object[] {event.payload};
                handler.handle(payload);
                return SignalDecision.continueSignal();
            }
        };
    }

    private final class ManagedSubscription implements SignalSubscription {
        private final Registration registration;

        ManagedSubscription(Registration registration) {
            this.registration = registration;
        }

        @Override public void disconnect() {
            registration.subscription.disconnect();
            registrations.remove(registration);
        }

        @Override public boolean isConnected() {
            return registration.subscription.isConnected();
        }
    }

    private static final class Registration {
        final String instanceId;
        final String signal;
        final SignalHandler handler;
        final SignalSubscription subscription;

        Registration(
                String instanceId,
                String signal,
                SignalHandler handler,
                SignalSubscription subscription) {
            this.instanceId = instanceId;
            this.signal = signal;
            this.handler = handler;
            this.subscription = subscription;
        }
    }
}
