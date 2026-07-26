package spireui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-tree signal connect / emit / disconnect (pure; no GL).
 */
public final class SignalHub {

    private final Map<String, List<SignalHandler>> handlers =
            new LinkedHashMap<String, List<SignalHandler>>();

    public void connect(String instanceId, String signal, SignalHandler handler) {
        if (instanceId == null || signal == null || handler == null) {
            throw new IllegalArgumentException("instanceId, signal, handler required");
        }
        String k = key(instanceId, signal);
        List<SignalHandler> list = handlers.get(k);
        if (list == null) {
            list = new ArrayList<SignalHandler>();
            handlers.put(k, list);
        }
        if (!list.contains(handler)) {
            list.add(handler);
        }
    }

    public void disconnect(String instanceId, String signal, SignalHandler handler) {
        if (instanceId == null || signal == null || handler == null) {
            return;
        }
        List<SignalHandler> list = handlers.get(key(instanceId, signal));
        if (list != null) {
            list.remove(handler);
        }
    }

    public void emit(String instanceId, String signal, Object... args) {
        if (instanceId == null || signal == null) {
            return;
        }
        List<SignalHandler> list = handlers.get(key(instanceId, signal));
        if (list == null || list.isEmpty()) {
            return;
        }
        Object[] payload = args != null ? args : new Object[0];
        List<SignalHandler> copy = new ArrayList<SignalHandler>(list);
        for (SignalHandler h : copy) {
            h.handle(payload);
        }
    }

    public void clear() {
        handlers.clear();
    }

    /** Remove all handlers for one instance id. */
    public void clearInstance(String instanceId) {
        if (instanceId == null) {
            return;
        }
        String prefix = instanceId + "\0";
        List<String> remove = new ArrayList<String>();
        for (String k : handlers.keySet()) {
            if (k.startsWith(prefix)) {
                remove.add(k);
            }
        }
        for (String k : remove) {
            handlers.remove(k);
        }
    }

    public int handlerCount(String instanceId, String signal) {
        List<SignalHandler> list = handlers.get(key(instanceId, signal));
        return list == null ? 0 : list.size();
    }

    private static String key(String instanceId, String signal) {
        return instanceId + "\0" + signal;
    }
}
