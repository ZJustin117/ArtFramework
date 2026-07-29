package artframework.c2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed co-op chrome snapshot for {@link EntityPresent#sync}. Opaque Objects still accepted;
 * use {@link #from(Object)} to parse maps or snapshots.
 */
public final class EntitySnapshot {

    public final float x;
    public final float y;
    public final float scale;
    public final boolean visible;
    public final String artResourceId;
    public final String iconResourceId;
    public final String label;
    public final int hp;
    public final int maxHp;
    public final int block;
    public final String intentIconResourceId;
    public final Map<String, Object> extras;

    public EntitySnapshot(
            float x,
            float y,
            float scale,
            boolean visible,
            String artResourceId,
            String iconResourceId,
            String label,
            int hp,
            int maxHp,
            int block,
            String intentIconResourceId,
            Map<String, Object> extras) {
        this.x = x;
        this.y = y;
        this.scale = scale <= 0f ? 1f : scale;
        this.visible = visible;
        this.artResourceId = artResourceId != null ? artResourceId : "";
        this.iconResourceId = iconResourceId != null ? iconResourceId : "";
        this.label = label != null ? label : "";
        this.hp = hp;
        this.maxHp = maxHp;
        this.block = block;
        this.intentIconResourceId = intentIconResourceId != null ? intentIconResourceId : "";
        if (extras == null || extras.isEmpty()) {
            this.extras = Collections.emptyMap();
        } else {
            this.extras = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(extras));
        }
    }

    public static EntitySnapshot empty() {
        return new EntitySnapshot(0f, 0f, 1f, true, "", "", "", 0, 0, 0, "", null);
    }

    public static EntitySnapshot ofArt(String artResourceId) {
        return new EntitySnapshot(0f, 0f, 1f, true, artResourceId, "", "", 0, 0, 0, "", null);
    }

    public static EntitySnapshot ofIcon(String iconResourceId) {
        return new EntitySnapshot(0f, 0f, 1f, true, "", iconResourceId, "", 0, 0, 0, "", null);
    }

    public static EntitySnapshot playerChrome(String label, int hp, int maxHp, int block) {
        return new EntitySnapshot(0f, 0f, 1f, true, "", "", label, hp, maxHp, block, "", null);
    }

    public static EntitySnapshot monsterChrome(
            String label, int hp, int maxHp, int block, String intentIconResourceId) {
        return new EntitySnapshot(
                0f, 0f, 1f, true, "", "", label, hp, maxHp, block, intentIconResourceId, null);
    }

    /** Parse {@link EntitySnapshot}, Map keys, or return empty for unknown. */
    @SuppressWarnings("unchecked")
    public static EntitySnapshot from(Object raw) {
        if (raw == null) {
            return empty();
        }
        if (raw instanceof EntitySnapshot) {
            return (EntitySnapshot) raw;
        }
        if (raw instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) raw;
            return new EntitySnapshot(
                    floatVal(m.get("x"), 0f),
                    floatVal(m.get("y"), 0f),
                    floatVal(m.get("scale"), 1f),
                    boolVal(m.get("visible"), true),
                    str(m.get("artResourceId")),
                    str(m.get("iconResourceId")),
                    str(m.get("label")),
                    intVal(m.get("hp"), 0),
                    intVal(m.get("maxHp"), 0),
                    intVal(m.get("block"), 0),
                    str(m.get("intentIconResourceId")),
                    m);
        }
        return empty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("x", Float.valueOf(x));
        m.put("y", Float.valueOf(y));
        m.put("scale", Float.valueOf(scale));
        m.put("visible", Boolean.valueOf(visible));
        m.put("artResourceId", artResourceId);
        m.put("iconResourceId", iconResourceId);
        m.put("label", label);
        m.put("hp", Integer.valueOf(hp));
        m.put("maxHp", Integer.valueOf(maxHp));
        m.put("block", Integer.valueOf(block));
        m.put("intentIconResourceId", intentIconResourceId);
        return m;
    }

    private static int intVal(Object o, int def) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o != null) {
            try {
                return Integer.parseInt(String.valueOf(o));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static float floatVal(Object o, float def) {
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        if (o != null) {
            try {
                return Float.parseFloat(String.valueOf(o));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static boolean boolVal(Object o, boolean def) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        if (o != null) {
            return Boolean.parseBoolean(String.valueOf(o));
        }
        return def;
    }

    private static String str(Object o) {
        return o != null ? String.valueOf(o) : "";
    }
}
