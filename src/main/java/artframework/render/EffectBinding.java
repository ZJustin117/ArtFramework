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
        if (!this.params.containsKey("layer")) {
            this.params.put("layer", LAYER_AMBIENT);
        }
        Object en = this.params.get("enabled");
        if (en instanceof Boolean) {
            this.enabled = ((Boolean) en).booleanValue();
        } else if (en instanceof Number) {
            this.enabled = ((Number) en).floatValue() > 0.5f;
        } else if (en instanceof String) {
            this.enabled =
                    !"false".equalsIgnoreCase(((String) en).trim())
                            && !"0".equals(((String) en).trim());
        }
    }

    /** Logical layer id ({@link #LAYER_AMBIENT}, {@link #LAYER_PULSE}, …). */
    public String layer() {
        Object v = params.get("layer");
        if (v == null) {
            return LAYER_AMBIENT;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? LAYER_AMBIENT : s;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        params.put("enabled", Boolean.valueOf(enabled));
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
