package artframework.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Named composite templates for {@link UiTypes#REF} expand. Pure; no GL.
 */
public final class ComponentRegistry {

    private static final ComponentRegistry GLOBAL = new ComponentRegistry();

    private final Map<String, UiNode> templates = new LinkedHashMap<String, UiNode>();

    public ComponentRegistry() {}

    public static ComponentRegistry global() {
        return GLOBAL;
    }

    public void register(String name, UiNode template) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("template name required");
        }
        if (template == null) {
            throw new IllegalArgumentException("template required");
        }
        if (UiTypes.REF.equals(template.type) || UiTypes.SLOT.equals(template.type)) {
            throw new IllegalArgumentException("template root cannot be ref/slot");
        }
        templates.put(name, template);
    }

    public void unregister(String name) {
        if (name != null) {
            templates.remove(name);
        }
    }

    public UiNode get(String name) {
        if (name == null) {
            return null;
        }
        return templates.get(name);
    }

    public boolean contains(String name) {
        return name != null && templates.containsKey(name);
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(templates.keySet());
    }

    public void clear() {
        templates.clear();
    }

    /** Test helper for the process-global registry. */
    public static void resetGlobalForTests() {
        GLOBAL.clear();
    }
}
