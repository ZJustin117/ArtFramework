package artframework.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.IdentityHashMap;

/** Validated, recursively immutable value tree for persistent UI declaration data. */
public final class ImmutableUiValue {

    private ImmutableUiValue() {}

    public static Object copy(Object value) {
        return copy(value, java.util.Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
    }

    private static Object copy(Object value, Set<Object> activeContainers) {
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof Float || value instanceof Double) {
            return value;
        }
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            if (!activeContainers.add(source)) {
                throw new IllegalArgumentException("cyclic UI value");
            }
            Map<String, Object> copy = new LinkedHashMap<String, Object>();
            try {
                for (Map.Entry<?, ?> entry : source.entrySet()) {
                    if (!(entry.getKey() instanceof String)) {
                        throw new IllegalArgumentException("UI value map keys must be strings");
                    }
                    copy.put((String) entry.getKey(), copy(entry.getValue(), activeContainers));
                }
            } finally {
                activeContainers.remove(source);
            }
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List) {
            List<?> source = (List<?>) value;
            if (!activeContainers.add(source)) {
                throw new IllegalArgumentException("cyclic UI value");
            }
            List<Object> copy = new ArrayList<Object>(source.size());
            try {
                for (Object item : source) copy.add(copy(item, activeContainers));
            } finally {
                activeContainers.remove(source);
            }
            return Collections.unmodifiableList(copy);
        }
        throw new IllegalArgumentException("unsupported mutable UI value: " + value.getClass().getName());
    }

    public static Map<String, Object> copyMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) return Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> copy = (Map<String, Object>) copy((Object) values);
        return copy;
    }
}
