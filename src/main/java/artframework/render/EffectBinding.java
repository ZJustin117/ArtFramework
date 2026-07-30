package artframework.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An effect instance bound to a {@link RenderTarget}.
 */
public final class EffectBinding {

    public final String effectId;
    private final Map<String, Object> params;
    private boolean enabled = true;

    public EffectBinding(String effectId, Map<String, Object> params) {
        if (effectId == null || effectId.isEmpty()) {
            throw new IllegalArgumentException("effectId required");
        }
        this.effectId = effectId;
        this.params = new LinkedHashMap<String, Object>();
        if (params != null) {
            this.params.putAll(params);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> paramsView() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(params));
    }

    public float paramFloat(String key, float defaultValue) {
        Object v = params.get(key);
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        return defaultValue;
    }

    public void setParam(String key, Object value) {
        if (key == null || key.isEmpty()) {
            return;
        }
        if (value == null) {
            params.remove(key);
        } else {
            params.put(key, value);
        }
    }

    public void setParamFloat(String key, float value) {
        setParam(key, Float.valueOf(value));
    }
}
