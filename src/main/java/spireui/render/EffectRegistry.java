package spireui.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Named {@link Effect} factories. Pure; no GL.
 */
public final class EffectRegistry {

    private final Map<String, Effect> effects = new LinkedHashMap<String, Effect>();

    public void register(Effect effect) {
        if (effect == null || effect.id() == null || effect.id().isEmpty()) {
            throw new IllegalArgumentException("effect required");
        }
        effects.put(effect.id(), effect);
    }

    public void unregister(String id) {
        if (id != null) {
            effects.remove(id);
        }
    }

    public Effect get(String id) {
        return id == null ? null : effects.get(id);
    }

    public boolean contains(String id) {
        return id != null && effects.containsKey(id);
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(effects.keySet());
    }

    public void clear() {
        effects.clear();
    }
}
