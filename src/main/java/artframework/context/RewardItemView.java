package artframework.context;

import java.util.LinkedHashMap;
import java.util.Map;

/** One reward row (gold, relic, card, potion, …). */
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
