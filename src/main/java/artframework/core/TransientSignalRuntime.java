package artframework.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Owner for synchronous framework transient command subscriptions. */
public final class TransientSignalRuntime implements AutoCloseable {
    private final SignalGroup group;
    private final boolean currentGroupRequired;
    private final List<SignalSubscription> owned = new ArrayList<SignalSubscription>();
    private final List<Consumer> consumers = new ArrayList<Consumer>();
    /** Per-thread mode so a raw nested dispatch remains raw inside a typed listener. */
    private final ThreadLocal<Boolean> typedDispatch = new ThreadLocal<Boolean>();
    private boolean closed;
    private boolean disposeGroupOnClose;
    private int inFlight;
    private volatile Runnable dispatchAdmissionHook;

    public TransientSignalRuntime() {
        this(SignalGroups.transientRuntimeRawGroup(), true);
    }

    TransientSignalRuntime(SignalGroup group) {
        this(group, true);
    }

    public TransientSignalRuntime(SignalGroup group, boolean currentGroupRequired) {
        if (group == null) throw new IllegalArgumentException("group required");
        if (currentGroupRequired && !SignalGroups.TRANSIENT_RUNTIME.equals(group.id())) {
            throw new IllegalArgumentException("transient runtime group required");
        }
        this.group = group;
        this.currentGroupRequired = currentGroupRequired;
        this.disposeGroupOnClose = !currentGroupRequired
                && SignalGroups.retainIsolatedTransientRuntimeGroup(group);
    }

    public SignalSubscription connect(String path, SignalListener listener) {
        synchronized (this) {
            requireOpenAndCurrent();
            SignalSubscription subscription = group.connectForRuntime(path, typedListener(listener));
            owned.add(subscription);
            return subscription;
        }
    }

    public SignalSubscription connect(Pattern path, SignalListener listener) {
        synchronized (this) {
            requireOpenAndCurrent();
            SignalSubscription subscription = group.connectForRuntime(path, typedListener(listener));
            owned.add(subscription);
            return subscription;
        }
    }

    /** Registers a committing schedule consumer after ordinary interceptors. */
    public SignalSubscription connectConsumer(String path, SignalListener listener) {
        synchronized (this) {
            requireOpenAndCurrent();
            if (path == null || path.isEmpty() || listener == null) {
                throw new IllegalArgumentException("name and listener required");
            }
            final Consumer consumer = new Consumer(path, typedListener(listener));
            consumers.add(consumer);
            SignalSubscription subscription = consumer.subscription;
            owned.add(subscription);
            return subscription;
        }
    }

    /** Dispatches a validated immutable runtime command synchronously. */
    public SignalDispatchResult dispatch(String path, String source, Object payload) {
        return dispatchTyped(path, source, TransientSignalPayload.of(payload));
    }

    private SignalDispatchResult dispatchTyped(
            String path, String source, TransientSignalPayload payload) {
        if (payload == null) throw new IllegalArgumentException("payload required");
        SignalGroup dispatchGroup;
        SignalGroup.CapturedDispatch captured;
        List<Consumer> consumerSnapshot;
        synchronized (this) {
            requireOpenAndCurrent();
            dispatchGroup = group;
            inFlight++;
            consumerSnapshot = connectedConsumers();
            captured = dispatchGroup.capture(new UiSignal(
                    dispatchGroup.id(), null, path, source, payload.value(), null));
        }
        Boolean previousMode = typedDispatch.get();
        typedDispatch.set(Boolean.TRUE);
        try {
            runAdmissionHook();
            // Reset and close after admission retain the listener set captured above.
            SignalDispatchResult intercepted = dispatchGroup.dispatch(captured);
            return dispatchConsumers(path, intercepted, consumerSnapshot);
        } finally {
            restoreDispatchMode(previousMode);
            finishDispatch();
        }
    }

    /** Dispatches an existing raw signal without changing UiSignal compatibility semantics. */
    public SignalDispatchResult dispatch(UiSignal signal) {
        SignalGroup dispatchGroup;
        SignalGroup.CapturedDispatch captured;
        List<Consumer> consumerSnapshot;
        synchronized (this) {
            requireOpenAndCurrent();
            dispatchGroup = group;
            inFlight++;
            consumerSnapshot = connectedConsumers();
            captured = dispatchGroup.capture(signal);
        }
        Boolean previousMode = typedDispatch.get();
        typedDispatch.set(Boolean.FALSE);
        try {
            runAdmissionHook();
            SignalDispatchResult intercepted = dispatchGroup.dispatch(captured);
            return dispatchConsumers(signal.name, intercepted, consumerSnapshot);
        } finally {
            restoreDispatchMode(previousMode);
            finishDispatch();
        }
    }

    public synchronized boolean isClosed() { return closed; }

    /** Package-private test seam for the framework-owned fixture. */
    SignalGroup groupForTests() { return group; }

    /** Test-only hook after runtime admission captures its listener set and before execution. */
    public void setDispatchAdmissionHookForTests(Runnable hook) { dispatchAdmissionHook = hook; }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        disconnectWhenIdle();
    }

    private void requireOpenAndCurrent() {
        if (closed) throw new IllegalStateException("transient signal runtime is closed");
        if ((currentGroupRequired && !SignalGroups.isCurrentTransientRuntimeGroup(group))
                || (disposeGroupOnClose && !SignalGroups.isRegistered(group))) {
            throw new IllegalStateException("transient signal runtime was retired by reset");
        }
    }

    private SignalListener typedListener(final SignalListener listener) {
        if (listener == null) throw new IllegalArgumentException("name and listener required");
        return new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                SignalDecision decision = listener.onSignal(signal);
                if (!Boolean.TRUE.equals(typedDispatch.get()) || decision == null
                        || decision.kind != SignalDecision.Kind.REPLACE
                        || decision.signal == null) return decision;
                TransientSignalPayload replacement = TransientSignalPayload.of(
                        decision.signal.payload);
                return SignalDecision.replace(new UiSignal(decision.signal.group, decision.signal.id,
                        decision.signal.name, decision.signal.source, replacement.value(),
                        decision.signal.metadata));
            }
        };
    }

    private synchronized void finishDispatch() {
        inFlight--;
        disconnectWhenIdle();
    }

    private void disconnectWhenIdle() {
        if (!closed || inFlight != 0) return;
        for (SignalSubscription subscription : owned) subscription.disconnect();
        owned.clear();
        consumers.clear();
        if (disposeGroupOnClose) {
            disposeGroupOnClose = false;
            SignalGroups.disposeIsolatedTransientRuntimeGroup(group);
        }
    }

    private void runAdmissionHook() {
        Runnable hook = dispatchAdmissionHook;
        if (hook != null) hook.run();
    }

    private void restoreDispatchMode(Boolean previousMode) {
        if (previousMode == null) typedDispatch.remove();
        else typedDispatch.set(previousMode);
    }

    private List<Consumer> connectedConsumers() {
        List<Consumer> snapshot = new ArrayList<Consumer>();
        for (Consumer consumer : consumers) {
            if (consumer.isConnected()) snapshot.add(consumer);
        }
        return snapshot;
    }

    private SignalDispatchResult dispatchConsumers(String path, SignalDispatchResult result,
            List<Consumer> snapshot) {
        if (result == null || result.isStopped()) return result;
        UiSignal current = result.signal;
        for (Consumer consumer : snapshot) {
            if (!consumer.path.equals(path)) continue;
            SignalDecision decision = consumer.listener.onSignal(current);
            if (decision == null || decision.kind == SignalDecision.Kind.CONTINUE) continue;
            if (decision.kind == SignalDecision.Kind.REPLACE) {
                if (decision.signal == null || !current.group.equals(decision.signal.group)
                        || !current.id.equals(decision.signal.id)
                        || !current.name.equals(decision.signal.name)) {
                    return new SignalDispatchResult(current, SignalDecision.Kind.STOP_REJECTED,
                            "replacement must preserve signal group, id and name");
                }
                current = decision.signal;
                continue;
            }
            return new SignalDispatchResult(current, decision.kind, decision.message);
        }
        return new SignalDispatchResult(current, SignalDecision.Kind.CONTINUE, "");
    }

    private static final class Consumer {
        final String path;
        final SignalListener listener;
        volatile boolean connected = true;
        final SignalSubscription subscription;
        Consumer(final String path, SignalListener listener) {
            this.path = path;
            this.listener = listener;
            this.subscription = new SignalSubscription() {
                @Override public void disconnect() { connected = false; }
                @Override public boolean isConnected() { return connected; }
            };
        }
        boolean isConnected() { return connected; }
    }
}
