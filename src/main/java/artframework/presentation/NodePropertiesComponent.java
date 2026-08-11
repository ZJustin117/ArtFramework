package artframework.presentation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable runtime property overlay initialized from a declaration or native projection. */
public final class NodePropertiesComponent {
    private final Map<String, Object> values;

    public NodePropertiesComponent(Map<String, Object> initial) {
        values = new LinkedHashMap<String, Object>();
        if (initial != null) values.putAll(initial);
    }

    public Object get(String name) { return values.get(name); }

    public void set(String name, Object value) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("property required");
        if (value == null) values.remove(name); else values.put(name, value);
    }

    public Map<String, Object> view() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
    }
}
