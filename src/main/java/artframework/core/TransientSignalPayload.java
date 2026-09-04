package artframework.core;

import artframework.component.ImmutableUiValue;
import artframework.context.ContextFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable input boundary for transient runtime commands.
 *
 * <p>Runtime payloads use the established {@link ImmutableUiValue} scalar/map/list schema and
 * additionally normalize arrays recursively to immutable lists. No mutable caller or host object
 * is retained by a command.</p>
 */
public final class TransientSignalPayload {
    private final Object value;

    private TransientSignalPayload(Object value) {
        this.value = value;
    }

    public static TransientSignalPayload of(Object value) {
        return new TransientSignalPayload(copy(value,
                Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>())));
    }

    /** Returns the recursively immutable value suitable for typed {@link UiSignal} delivery. */
    public Object value() {
        return value;
    }

    private static Object copy(Object candidate, Set<Object> activeContainers) {
        if (candidate == null || candidate instanceof String || candidate instanceof Boolean
                || candidate instanceof Byte || candidate instanceof Short
                || candidate instanceof Integer || candidate instanceof Long
                || candidate instanceof Float || candidate instanceof Double) {
            return candidate;
        }
        // Authority frames are an established immutable framework snapshot type, not host state.
        if (candidate instanceof ContextFrame) return candidate;
        if (candidate instanceof Map) return copyMap((Map<?, ?>) candidate, activeContainers);
        if (candidate instanceof List) return copyList((List<?>) candidate, activeContainers);
        if (candidate.getClass().isArray()) return copyArray(candidate, activeContainers);
        // Keep the persistent declaration schema authoritative for unsupported values/messages.
        return ImmutableUiValue.copy(candidate);
    }

    private static Map<String, Object> copyMap(Map<?, ?> source, Set<Object> activeContainers) {
        enter(source, activeContainers);
        try {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("UI value map keys must be strings");
                }
                result.put((String) entry.getKey(), copy(entry.getValue(), activeContainers));
            }
            return Collections.unmodifiableMap(result);
        } finally {
            activeContainers.remove(source);
        }
    }

    private static List<Object> copyList(List<?> source, Set<Object> activeContainers) {
        enter(source, activeContainers);
        try {
            List<Object> result = new ArrayList<Object>(source.size());
            for (Object entry : source) result.add(copy(entry, activeContainers));
            return Collections.unmodifiableList(result);
        } finally {
            activeContainers.remove(source);
        }
    }

    private static List<Object> copyArray(Object source, Set<Object> activeContainers) {
        enter(source, activeContainers);
        try {
            int length = java.lang.reflect.Array.getLength(source);
            List<Object> result = new ArrayList<Object>(length);
            for (int i = 0; i < length; i++) {
                result.add(copy(java.lang.reflect.Array.get(source, i), activeContainers));
            }
            return Collections.unmodifiableList(result);
        } finally {
            activeContainers.remove(source);
        }
    }

    private static void enter(Object value, Set<Object> activeContainers) {
        if (!activeContainers.add(value)) throw new IllegalArgumentException("cyclic UI value");
    }
}
