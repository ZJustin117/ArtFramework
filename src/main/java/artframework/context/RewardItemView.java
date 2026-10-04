package artframework.context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One reward row (gold, relic, card, potion, …).
 *
 * <p>{@code x}/{@code y} are the row CENTER (native {@code RewardItem.hb} center, which native moves
 * to {@code (Settings.WIDTH/2, y)}), NOT the bottom-left. {@code RewardDrawPath} renders rows
 * centered on {@code x}/{@code y}, matching the native {@code REWARD_SCREEN_ITEM} panel that is
 * centered on {@code y}; publishing a hitbox bottom-left here shifts rows ~half a panel to the left.
 */
public final class RewardItemView {

    public final int index;
    public final String kind;
    public final String label;
    public final String resourceId;
    public final boolean visible;
    public final boolean enabled;
    public final float x;
    public final float y;
    public final float w;
    public final float h;

    public RewardItemView(
            int index,
            String kind,
            String label,
            String resourceId,
            boolean visible,
            boolean enabled,
            float x,
            float y,
            float w,
            float h) {
        this.index = index;
        this.kind = kind != null ? kind : "";
        this.label = label != null ? label : "";
        this.resourceId = resourceId != null ? resourceId : "";
        this.visible = visible;
        this.enabled = enabled;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static RewardItemView of(int index, String kind, String label) {
        return new RewardItemView(index, kind, label, "", true, true, 0f, 0f, 0f, 0f);
    }

    public static RewardItemView of(int index, String kind, String label,
            float x, float y, float w, float h) {
        return new RewardItemView(index, kind, label, "", true, true, x, y, w, h);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("index", Integer.valueOf(index));
        m.put("kind", kind);
        m.put("label", label);
        m.put("resourceId", resourceId);
        m.put("visible", Boolean.valueOf(visible));
        m.put("enabled", Boolean.valueOf(enabled));
        m.put("x", Float.valueOf(x));
        m.put("y", Float.valueOf(y));
        m.put("w", Float.valueOf(w));
        m.put("h", Float.valueOf(h));
        return m;
    }
}
