package artframework.presentation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable effect state belonging to one visual entity. */
public final class EffectAttachment {
    public final String effectId;
    public final String layer;
    private final Map<String, Object> params;
    private final boolean enabled;

    public EffectAttachment(String effectId, String layer, Map<String, Object> params) {
        this(effectId, layer, params, params == null || !Boolean.FALSE.equals(params.get("enabled")));
    }

    private EffectAttachment(String effectId, String layer, Map<String, Object> params, boolean enabled) {
        if (effectId == null || effectId.isEmpty()) throw new IllegalArgumentException("effectId required");
        this.effectId = effectId;
        this.layer = layer == null || layer.isEmpty() ? "ambient" : layer;
        Map<String, Object> copy = new LinkedHashMap<String, Object>();
        if (params != null) copy.putAll(params);
        copy.put("enabled", Boolean.valueOf(enabled));
        this.params = Collections.unmodifiableMap(copy);
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }
    public EffectAttachment withEnabled(boolean value) {
        return new EffectAttachment(effectId, layer, params, value);
    }
    public float floatParam(String name, float fallback) {
        Object value = params.get(name);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }
    public EffectAttachment withParam(String name, Object value) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>(params);
        if (value == null) copy.remove(name); else copy.put(name, value);
        return new EffectAttachment(effectId, layer, copy, enabled);
    }
    public Map<String, Object> params() { return params; }
}
