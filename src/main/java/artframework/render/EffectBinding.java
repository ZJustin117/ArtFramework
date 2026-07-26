package artframework.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An effect instance bound to a {@link RenderTarget}.
 */
public final class EffectBinding {

    public final String effectId;
    public final Map<String, Object> params;
    private boolean enabled = true;

    public EffectBinding(String effectId, Map<String, Object> params) {
        if (effectId == null || effectId.isEmpty()) {
            throw new IllegalArgumentException("effectId required");
        }
        this.effectId = effectId;
        if (params == null || params.isEmpty()) {
            this.params = Collections.emptyMap();
        } else {
            this.params = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(params));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float paramFloat(String key, float defaultValue) {
        Object v = params.get(key);
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        return defaultValue;
    }
}
