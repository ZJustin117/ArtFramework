package artframework.presentation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable effect state belonging to one visual entity. */
public final class EffectAttachment {
    public final String effectId;
    public final String layer;
    private final Map<String, Object> params;
    private boolean enabled;

    public EffectAttachment(String effectId, String layer, Map<String, Object> params) {
        if (effectId == null || effectId.isEmpty()) throw new IllegalArgumentException("effectId required");
        this.effectId = effectId;
        this.layer = layer == null || layer.isEmpty() ? "ambient" : layer;
        this.params = new LinkedHashMap<String, Object>();
        if (params != null) this.params.putAll(params);
        this.enabled = !Boolean.FALSE.equals(this.params.get("enabled"));
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; params.put("enabled", Boolean.valueOf(value)); }
    public float floatParam(String name, float fallback) {
        Object value = params.get(name);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }
    public void setParam(String name, Object value) { if (value == null) params.remove(name); else params.put(name, value); }
    public Map<String, Object> params() { return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(params)); }
}
