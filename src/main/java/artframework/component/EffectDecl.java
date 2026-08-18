package artframework.component;

import java.util.Map;

/**
 * Declarative visual attach (Attach primitive). Layout ignores this.
 */
public final class EffectDecl {

    public final String id;
    public final Map<String, Object> params;

    public EffectDecl(String id, Map<String, Object> params) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("effect id required");
        }
        this.id = id;
        this.params = ImmutableUiValue.copyMap(params);
    }
}
