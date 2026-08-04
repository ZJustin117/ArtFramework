package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime context for a {@link UiAction} invocation.
 */
public final class UiActionContext {

    public final UiTree tree;
    /** Node that declared the connection (may be null for programmatic run). */
    public final UiInstance owner;
    public final UiSignal signal;
    public final Map<String, Object> args;

    public UiActionContext(
            UiTree tree, UiInstance owner, UiSignal signal, Map<String, Object> args) {
        this.tree = tree;
        this.owner = owner;
        this.signal = signal;
        if (args == null || args.isEmpty()) {
            this.args = Collections.emptyMap();
        } else {
            this.args = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(args));
        }
    }

    public String argString(String key, String def) {
        Object v = args.get(key);
        if (v == null) {
            return def;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? def : s;
    }

    public float argFloat(String key, float def) {
        Object v = args.get(key);
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        if (v instanceof String) {
            try {
                return Float.parseFloat(((String) v).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public int argInt(String key, int def) {
        Object v = args.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt(((String) v).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public boolean argBool(String key, boolean def) {
        Object v = args.get(key);
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        if (v instanceof String) {
            return "true".equalsIgnoreCase(((String) v).trim())
                    || "1".equals(((String) v).trim());
        }
        return def;
    }

    /** Resolve payload element by {@code from_payload} index in args, or null. */
    public Object payloadAt(int index) {
        if (signal == null || signal.payload == null) {
            return null;
        }
        if (signal.payload instanceof Object[]) {
            Object[] arr = (Object[]) signal.payload;
            if (index >= 0 && index < arr.length) {
                return arr[index];
            }
            return null;
        }
        if (index == 0) {
            return signal.payload;
        }
        return null;
    }

    public Object resolveValue() {
        if (args.containsKey("from_payload")) {
            int idx = argInt("from_payload", 0);
            return payloadAt(idx);
        }
        if (args.containsKey("value")) {
            return args.get("value");
        }
        return null;
    }
}
