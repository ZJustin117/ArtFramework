package artframework.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An effect instance bound to a {@link RenderTarget}.
 */
public final class EffectBinding {

    /** Ambient / base band (default when layer param omitted). */
    public static final String LAYER_AMBIENT = "ambient";
    /** One-shot overlay band (e.g. lightwave pulse sweep). */
    public static final String LAYER_PULSE = "pulse";

    public final String effectId;
    private volatile Snapshot snapshot;

    public EffectBinding(String effectId, Map<String, Object> params) {
        if (effectId == null || effectId.isEmpty()) {
            throw new IllegalArgumentException("effectId required");
        }
        this.effectId = effectId;
        Map<String, Object> initial = new LinkedHashMap<String, Object>();
        if (params != null) {
            initial.putAll(params);
        }
        if (!initial.containsKey("layer")) {
            initial.put("layer", LAYER_AMBIENT);
        }
        boolean enabled = true;
        Object en = initial.get("enabled");
        if (en instanceof Boolean) {
            enabled = ((Boolean) en).booleanValue();
        } else if (en instanceof Number) {
            enabled = ((Number) en).floatValue() > 0.5f;
        } else if (en instanceof String) {
            enabled =
                    !"false".equalsIgnoreCase(((String) en).trim())
                            && !"0".equals(((String) en).trim());
        }
        this.snapshot = new Snapshot(immutableCopy(initial), enabled);
    }

    /** Logical layer id ({@link #LAYER_AMBIENT}, {@link #LAYER_PULSE}, …). */
    public String layer() {
        Map<String, Object> params = snapshot.params;
        Object v = params.get("layer");
        if (v == null) {
            return LAYER_AMBIENT;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? LAYER_AMBIENT : s;
    }

    public boolean isEnabled() {
        return snapshot.enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        Map<String, Object> next = new LinkedHashMap<String, Object>(snapshot.params);
        next.put("enabled", Boolean.valueOf(enabled));
        snapshot = new Snapshot(immutableCopy(next), enabled);
    }

    public Map<String, Object> paramsView() {
        return snapshot.params;
    }

    synchronized void replaceParams(Map<String, Object> next, boolean enabled) {
        Map<String, Object> replacement = new LinkedHashMap<String, Object>();
        if (next != null) {
            replacement.putAll(next);
        }
        if (!replacement.containsKey("layer")) {
            replacement.put("layer", LAYER_AMBIENT);
        }
        replacement.put("enabled", Boolean.valueOf(enabled));
        snapshot = new Snapshot(immutableCopy(replacement), enabled);
    }

    public float paramFloat(String key, float defaultValue) {
        Map<String, Object> params = snapshot.params;
        Object v = params.get(key);
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        return defaultValue;
    }

    public synchronized void setParam(String key, Object value) {
        if (key == null || key.isEmpty()) {
            return;
        }
        Snapshot current = snapshot;
        Map<String, Object> next = new LinkedHashMap<String, Object>(current.params);
        if (value == null) {
            next.remove(key);
        } else {
            next.put(key, value);
        }
        snapshot = new Snapshot(immutableCopy(next), current.enabled);
    }

    public void setParamFloat(String key, float value) {
        setParam(key, Float.valueOf(value));
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(source));
    }

    private static final class Snapshot {
        final Map<String, Object> params;
        final boolean enabled;

        Snapshot(Map<String, Object> params, boolean enabled) {
            this.params = params;
            this.enabled = enabled;
        }
    }
}
