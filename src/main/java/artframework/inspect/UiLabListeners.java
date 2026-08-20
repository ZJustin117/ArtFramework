package artframework.inspect;

import artframework.api.UiOpResult;
import artframework.core.SignalHandler;
import artframework.core.UiComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.ecs.EntityId;
import artframework.api.ArtFramework;
import artframework.core.SignalSubscription;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Process-local lab signal listeners for console {@code art ui listen}. Logs via registered sink.
 */
public final class UiLabListeners {

    public static final String SIGNAL_PREFIX = "ART_UI_SIGNAL ";

    public interface LogSink {
        void log(String line);
    }

    private static final Map<String, LabEntry> ENTRIES = new LinkedHashMap<String, LabEntry>();
    private static final CopyOnWriteArrayList<LogSink> SINKS = new CopyOnWriteArrayList<LogSink>();
    private static LogSink defaultSink =
            new LogSink() {
                @Override
                public void log(String line) {
                    System.out.println(line);
                }
            };

    private UiLabListeners() {}

    public static void setDefaultSink(LogSink sink) {
        defaultSink = sink != null ? sink : defaultSink;
    }

    public static void addSink(LogSink sink) {
        if (sink != null && !SINKS.contains(sink)) {
            SINKS.add(sink);
        }
    }

    public static void removeSink(LogSink sink) {
        SINKS.remove(sink);
    }

    public static synchronized UiOpResult listen(String target, String signal) {
        if (target == null || target.isEmpty()) {
            return UiOpResult.unavailable("target required");
        }
        if (signal == null || signal.isEmpty()) {
            return UiOpResult.unavailable("signal required");
        }
        // SyntheticRuntime can be used directly; initialize the facade-owned retirement hook
        // before attaching a lab entry to a presentation entity.
        ArtFramework.isRegistered(target);
        String key = key(target, signal);
        String label = target + " " + signal;
        if (ENTRIES.containsKey(key)) {
            return UiOpResult.ok("already listening " + label);
        }
        final String t = target;
        final String s = signal;
        SignalHandler handler =
                new SignalHandler() {
                    @Override
                    public void handle(Object... args) {
                        StringBuilder sb = new StringBuilder(SIGNAL_PREFIX);
                        sb.append(t).append(' ').append(s);
                        if (args != null) {
                            for (int i = 0; i < args.length; i++) {
                                sb.append(' ').append(args[i]);
                            }
                        }
                        publish(sb.toString());
                    }
                };
        try {
            AttachResult attached = attach(target, signal, handler);
            if (!attached.ok) {
                return UiOpResult.unavailable(attached.message);
            }
            ENTRIES.put(key, new LabEntry(target, signal, handler, attached.detach,
                    attached.subscription, attached.synthetic));
            return UiOpResult.ok("listening " + label);
        } catch (IllegalArgumentException e) {
            return UiOpResult.unavailable(e.getMessage() != null ? e.getMessage() : "listen failed");
        } catch (RuntimeException e) {
            return UiOpResult.unavailable(
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    public static synchronized UiOpResult unlisten(String target, String signal) {
        if (target == null || signal == null) {
            return UiOpResult.unavailable("target and signal required");
        }
        String key = key(target, signal);
        String label = target + " " + signal;
        LabEntry entry = ENTRIES.remove(key);
        if (entry == null) {
            return UiOpResult.notBound("not listening: " + label);
        }
        try {
            entry.detach.run();
        } catch (RuntimeException ignored) {
        }
        return UiOpResult.ok("stopped " + label);
    }

    public static synchronized List<String> listKeys() {
        List<String> out = new ArrayList<String>();
        for (LabEntry e : ENTRIES.values()) {
            out.add(e.target + " " + e.signal);
        }
        return out;
    }

    /** Invalidate lab entries owned by a retired synthetic window. */
    public static synchronized void onTreeClosed(String windowId) {
        if (windowId == null || windowId.isEmpty()) return;
        String prefix = windowId + "/";
        List<LabEntry> retired = new ArrayList<LabEntry>();
        for (Map.Entry<String, LabEntry> entry : new ArrayList<Map.Entry<String, LabEntry>>(ENTRIES.entrySet())) {
            LabEntry lab = entry.getValue();
            if (lab.synthetic && (windowId.equals(lab.target) || lab.target.startsWith(prefix))) {
                ENTRIES.remove(entry.getKey());
                retired.add(lab);
            }
        }
        for (LabEntry lab : retired) {
            try {
                lab.detach.run();
            } catch (RuntimeException ignored) {
            } finally {
                if (lab.subscription != null) lab.subscription.disconnect();
            }
        }
    }

    public static synchronized void resetForTests() {
        List<LabEntry> copy = new ArrayList<LabEntry>(ENTRIES.values());
        ENTRIES.clear();
        for (LabEntry e : copy) {
            try {
                e.detach.run();
            } catch (RuntimeException ignored) {
            }
        }
        SINKS.clear();
    }

    private static void publish(String line) {
        if (SINKS.isEmpty()) {
            defaultSink.log(line);
            return;
        }
        for (LogSink sink : SINKS) {
            sink.log(line);
        }
    }

    private static String key(String target, String signal) {
        return target + "\0" + signal;
    }

    private static AttachResult attach(String target, String signal, SignalHandler handler) {
        int slash = target.indexOf('/');
        if (slash > 0 && slash < target.length() - 1) {
            String windowId = target.substring(0, slash);
            String controlId = target.substring(slash + 1);
            PresentationContext context = PresentationRuntime.context(windowId);
            if (context == null) {
                return AttachResult.fail("window not open: " + windowId);
            }
            EntityId entity = PresentationRuntime.find(context, controlId);
            if (entity == null) {
                return AttachResult.fail("control not found: " + controlId);
            }
            final SignalSubscription subscription = PresentationRuntime.connect(context, entity, signal, handler);
            return AttachResult.ok(
                    new Runnable() {
                        @Override
                        public void run() {
                            subscription.disconnect();
                        }
                    }, subscription, true);
        }
        PresentationContext context = PresentationRuntime.context(target);
        if (context != null) {
            EntityId root = PresentationRuntime.root(context);
            final SignalSubscription subscription = PresentationRuntime.connect(context, root, signal, handler);
            return AttachResult.ok(
                    new Runnable() {
                        @Override
                        public void run() {
                            subscription.disconnect();
                        }
                    }, subscription, true);
        }
        UiComponent comp = ArtFramework.component(target);
        if (comp == null) {
            return AttachResult.fail("unknown target: " + target);
        }
        final String id = target;
        final String sig = signal;
        final SignalHandler h = handler;
        final SignalSubscription subscription = comp.connect(signal, handler);
        return AttachResult.ok(
                    new Runnable() {
                    @Override
                    public void run() {
                        UiComponent c = ArtFramework.component(id);
                        if (c != null) {
                            c.disconnect(sig, h);
                        }
                    }
                    }, subscription, false);
    }

    private static final class LabEntry {
        final String target;
        final String signal;
        final SignalHandler handler;
        final Runnable detach;
        final SignalSubscription subscription;
        final boolean synthetic;

        LabEntry(String target, String signal, SignalHandler handler, Runnable detach,
                SignalSubscription subscription, boolean synthetic) {
            this.target = target;
            this.signal = signal;
            this.handler = handler;
            this.detach = detach;
            this.subscription = subscription;
            this.synthetic = synthetic;
        }
    }

    private static final class AttachResult {
        final boolean ok;
        final String message;
        final Runnable detach;
        final SignalSubscription subscription;
        final boolean synthetic;

        private AttachResult(boolean ok, String message, Runnable detach,
                SignalSubscription subscription, boolean synthetic) {
            this.ok = ok;
            this.message = message;
            this.detach = detach;
            this.subscription = subscription;
            this.synthetic = synthetic;
        }

        static AttachResult ok(Runnable detach, SignalSubscription subscription, boolean synthetic) {
            return new AttachResult(true, "", detach, subscription, synthetic);
        }

        static AttachResult fail(String message) {
            return new AttachResult(false, message, null, null, false);
        }
    }
}
