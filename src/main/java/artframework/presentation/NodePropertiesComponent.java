package artframework.presentation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable runtime property overlay initialized from a declaration or native projection. */
public final class NodePropertiesComponent {
    private final Map<String, Object> values;

    public NodePropertiesComponent(Map<String, Object> initial) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>();
        if (initial != null) copy.putAll(initial);
        values = Collections.unmodifiableMap(copy);
    }

    public Object get(String name) { return values.get(name); }

    public NodePropertiesComponent with(String name, Object value) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("property required");
        Map<String, Object> copy = new LinkedHashMap<String, Object>(values);
        if (value == null) copy.remove(name); else copy.put(name, value);
        return new NodePropertiesComponent(copy);
    }

    public Map<String, Object> view() {
        return values;
    }
}
