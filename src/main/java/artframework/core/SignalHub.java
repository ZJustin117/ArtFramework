package artframework.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-tree signal connect / emit / disconnect (pure; no GL).
 */
public final class SignalHub {

    private final List<Registration> registrations = new ArrayList<Registration>();

    public void connect(String instanceId, String signal, SignalHandler handler) {
        if (instanceId == null || signal == null || handler == null) {
            throw new IllegalArgumentException("instanceId, signal, handler required");
        }
        registrations.add(new Registration(instanceId, signal, handler,
                SignalBuses.get().connect(name(instanceId, signal), new SignalListener() {
                    @Override public SignalDecision onSignal(UiSignal event) {
                        Object[] payload = event.payload instanceof Object[]
                                ? (Object[]) event.payload : new Object[] {event.payload};
                        handler.handle(payload);
                        return SignalDecision.continueSignal();
                    }
                })));
    }

    public void disconnect(String instanceId, String signal, SignalHandler handler) {
        if (instanceId == null || signal == null || handler == null) {
            return;
        }
        for (Registration registration : new ArrayList<Registration>(registrations)) {
            if (registration.instanceId.equals(instanceId) && registration.signal.equals(signal)
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
        for (Registration registration : registrations) registration.subscription.disconnect();
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
            if (registration.instanceId.equals(instanceId) && registration.signal.equals(signal)
                    && registration.subscription.isConnected()) total++;
        }
        return total;
    }

    public static String name(String instanceId, String signal) {
        return "ui/" + instanceId + "/" + signal;
    }

    private static final class Registration {
        final String instanceId;
        final String signal;
        final SignalHandler handler;
        final SignalSubscription subscription;
        Registration(String instanceId, String signal, SignalHandler handler, SignalSubscription subscription) {
            this.instanceId = instanceId; this.signal = signal; this.handler = handler; this.subscription = subscription;
        }
    }
}
