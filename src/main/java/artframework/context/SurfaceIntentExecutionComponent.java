package artframework.context;

import artframework.component.ImmutableUiValue;
import artframework.component.MapNodeRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One-shot data-only request consumed by {@link SurfaceIntentExecutionSystem}. */
public final class SurfaceIntentExecutionComponent {
    private static final String WIRE_TYPE = "__art_type";
    public final String name;
    public final String surfaceId;
    public final List<Object> args;

    public SurfaceIntentExecutionComponent(String name, String surfaceId, Object... args) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name required");
        this.name = name;
        this.surfaceId = surfaceId != null ? surfaceId : "";
        List<Object> copy = new ArrayList<Object>();
        if (args != null) {
            for (Object arg : args) {
                copy.add(normalize(arg));
            }
        }
        this.args = Collections.unmodifiableList(copy);
    }

    static Object normalize(Object value) {
        return ImmutableUiValue.copy(encode(value,
                Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>())));
    }

    private static Object encode(Object value, Set<Object> activeContainers) {
        if (value instanceof CardRef) {
            CardRef ref = (CardRef) value;
            Map<String, Object> encoded = new LinkedHashMap<String, Object>();
            encoded.put(WIRE_TYPE, "CardRef");
            encoded.put("instanceId", ref.instanceId);
            encoded.put("cardId", ref.cardId);
            return encoded;
        }
        if (value instanceof MapNodeRef) {
            MapNodeRef ref = (MapNodeRef) value;
            Map<String, Object> encoded = new LinkedHashMap<String, Object>();
            encoded.put(WIRE_TYPE, "MapNodeRef");
            encoded.put("row", Integer.valueOf(ref.row));
            encoded.put("col", Integer.valueOf(ref.col));
            encoded.put("roomType", ref.roomType);
            return encoded;
        }
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            if (!activeContainers.add(source)) throw new IllegalArgumentException("cyclic intent value");
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            try {
                if (source.containsKey(WIRE_TYPE)) {
                    throw new IllegalArgumentException("intent value key reserved: " + WIRE_TYPE);
                }
                for (Map.Entry<?, ?> entry : source.entrySet()) {
                    if (!(entry.getKey() instanceof String)) {
                        throw new IllegalArgumentException("intent value map keys must be strings");
                    }
                    result.put((String) entry.getKey(), encode(entry.getValue(), activeContainers));
                }
            } finally {
                activeContainers.remove(source);
            }
            return result;
        }
        if (value instanceof List) {
            List<?> source = (List<?>) value;
            if (!activeContainers.add(source)) throw new IllegalArgumentException("cyclic intent value");
            List<Object> result = new ArrayList<Object>();
            try {
                for (Object item : source) result.add(encode(item, activeContainers));
            } finally {
                activeContainers.remove(source);
            }
            return result;
        }
        return value;
    }

    static Object denormalize(Object value) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if ("CardRef".equals(map.get(WIRE_TYPE))
                    && map.get("instanceId") instanceof String
                    && map.get("cardId") instanceof String) {
                return new CardRef((String) map.get("instanceId"), (String) map.get("cardId"));
            }
            if ("MapNodeRef".equals(map.get(WIRE_TYPE))
                    && map.get("row") instanceof Number
                    && map.get("col") instanceof Number
                    && map.get("roomType") instanceof String) {
                return new MapNodeRef(((Number) map.get("row")).intValue(),
                        ((Number) map.get("col")).intValue(), (String) map.get("roomType"));
            }
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put((String) entry.getKey(), denormalize(entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<Object>();
            for (Object item : (List<?>) value) result.add(denormalize(item));
            return Collections.unmodifiableList(result);
        }
        return value;
    }
}
