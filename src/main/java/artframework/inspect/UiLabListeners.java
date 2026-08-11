package artframework.inspect;

import artframework.api.UiOpResult;
import artframework.core.SignalHandler;
import artframework.core.UiComponent;
import artframework.presentation.Node;
import artframework.presentation.NodeTree;
import artframework.presentation.NodeTrees;
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

    public static UiOpResult listen(String target, String signal) {
        if (target == null || target.isEmpty()) {
            return UiOpResult.unavailable("target required");
        }
        if (signal == null || signal.isEmpty()) {
            return UiOpResult.unavailable("signal required");
        }
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
            ENTRIES.put(key, new LabEntry(target, signal, handler, attached.detach));
            return UiOpResult.ok("listening " + label);
        } catch (IllegalArgumentException e) {
            return UiOpResult.unavailable(e.getMessage() != null ? e.getMessage() : "listen failed");
        } catch (RuntimeException e) {
            return UiOpResult.unavailable(
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    public static UiOpResult unlisten(String target, String signal) {
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

    public static List<String> listKeys() {
        List<String> out = new ArrayList<String>();
        for (LabEntry e : ENTRIES.values()) {
            out.add(e.target + " " + e.signal);
        }
        return out;
    }

    public static void resetForTests() {
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
            NodeTree tree = NodeTrees.get(windowId);
            if (tree == null) {
                return AttachResult.fail("window not open: " + windowId);
            }
            if (tree.get(controlId) == null) {
                return AttachResult.fail("control not found: " + controlId);
            }
            final SignalSubscription subscription = tree.connect(controlId, signal, handler);
            final String w = windowId;
            final String c = controlId;
            final String sig = signal;
            final SignalHandler h = handler;
            return AttachResult.ok(
                    new Runnable() {
                        @Override
                        public void run() {
                            subscription.disconnect();
                        }
                    });
        }
        NodeTree tree = NodeTrees.get(target);
        if (tree != null) {
            String rootId =
                    tree.root() != null && !tree.root().name().isEmpty()
                            ? tree.root().name()
                            : target;
            final SignalSubscription subscription = tree.connect(rootId, signal, handler);
            final String w = target;
            final String c = rootId;
            final String sig = signal;
            final SignalHandler h = handler;
            return AttachResult.ok(
                    new Runnable() {
                        @Override
                        public void run() {
                            subscription.disconnect();
                        }
                    });
        }
        UiComponent comp = ArtFramework.component(target);
        if (comp == null) {
            return AttachResult.fail("unknown target: " + target);
        }
        comp.connect(signal, handler);
        final String id = target;
        final String sig = signal;
        final SignalHandler h = handler;
        return AttachResult.ok(
                new Runnable() {
                    @Override
                    public void run() {
                        UiComponent c = ArtFramework.component(id);
                        if (c != null) {
                            c.disconnect(sig, h);
                        }
                    }
                });
    }

    private static final class LabEntry {
        final String target;
        final String signal;
        final SignalHandler handler;
        final Runnable detach;

        LabEntry(String target, String signal, SignalHandler handler, Runnable detach) {
            this.target = target;
            this.signal = signal;
            this.handler = handler;
            this.detach = detach;
        }
    }

    private static final class AttachResult {
        final boolean ok;
        final String message;
        final Runnable detach;

        private AttachResult(boolean ok, String message, Runnable detach) {
            this.ok = ok;
            this.message = message;
            this.detach = detach;
        }

        static AttachResult ok(Runnable detach) {
            return new AttachResult(true, "", detach);
        }

        static AttachResult fail(String message) {
            return new AttachResult(false, message, null);
        }
    }
}
