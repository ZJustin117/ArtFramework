package artframework.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Per-tree signal connect / emit / disconnect (pure; no GL).
 * Exact instance/signal pairs and raw bus name / regex share {@link SignalBus}.
 */
public final class SignalHub {

    private final List<Registration> registrations = new ArrayList<Registration>();

    public void connect(String instanceId, String signal, SignalHandler handler) {
        if (instanceId == null || signal == null || handler == null) {
            throw new IllegalArgumentException("instanceId, signal, handler required");
        }
        registrations.add(
                new Registration(
                        instanceId,
                        signal,
                        handler,
                        SignalBuses.get()
                                .connect(
                                        name(instanceId, signal),
                                        wrapHandler(handler))));
    }

    /**
     * Subscribe to a full bus signal name (e.g. {@code ui/ok/pressed}), same as
     * {@link SignalBus#connect(String, SignalListener)}.
     */
    public SignalSubscription connectBus(String busName, SignalListener listener) {
        if (busName == null || busName.isEmpty() || listener == null) {
            throw new IllegalArgumentException("busName and listener required");
        }
        SignalSubscription sub = SignalBuses.get().connect(busName, listener);
        registrations.add(new Registration("", busName, null, sub));
        return sub;
    }

    /**
     * Regex subscribe on full bus names — same semantics as
     * {@link SignalBus#connect(Pattern, SignalListener)}.
     */
    public SignalSubscription connectBus(Pattern pattern, SignalListener listener) {
        if (pattern == null || listener == null) {
            throw new IllegalArgumentException("pattern and listener required");
        }
        SignalSubscription sub = SignalBuses.get().connect(pattern, listener);
        registrations.add(new Registration("", pattern.pattern(), null, sub));
        return sub;
    }

    public void disconnect(String instanceId, String signal, SignalHandler handler) {
        if (instanceId == null || signal == null || handler == null) {
            return;
        }
        for (Registration registration : new ArrayList<Registration>(registrations)) {
            if (registration.instanceId.equals(instanceId)
                    && registration.signal.equals(signal)
                    && registration.handler == handler) {
                registration.subscription.disconnect();
                registrations.remove(registration);
            }
        }
    }

    public void emit(String instanceId, String signal, Object... args) {
        if (instanceId == null || signal == null) {
            return;
        }
        Object[] payload = args != null ? args : new Object[0];
        SignalBuses.get().emit(new UiSignal(name(instanceId, signal), instanceId, payload));
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

    public static String name(String instanceId, String signal) {
        return "ui/" + instanceId + "/" + signal;
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
