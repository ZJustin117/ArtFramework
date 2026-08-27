package artframework.context;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable event dialog option for one present frame. */
public final class EventOptionView {

    public final int index;
    public final String label;
    public final boolean enabled;
    public final boolean visible;
    public final float x;
    public final float y;
    public final float w;
    public final float h;

    public EventOptionView(
            int index,
            String label,
            boolean enabled,
            boolean visible,
            float x,
            float y,
            float w,
            float h) {
        this.index = index;
        this.label = label != null ? label : "";
        this.enabled = enabled;
        this.visible = visible;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static EventOptionView of(int index, String label, boolean enabled) {
        return new EventOptionView(index, label, enabled, true, 0f, 0f, 0f, 0f);
    }

    public static EventOptionView of(int index, String label, boolean enabled,
            float x, float y, float w, float h) {
        return new EventOptionView(index, label, enabled, true, x, y, w, h);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("index", Integer.valueOf(index));
        m.put("label", label);
        m.put("enabled", Boolean.valueOf(enabled));
        m.put("visible", Boolean.valueOf(visible));
        m.put("x", Float.valueOf(x));
        m.put("y", Float.valueOf(y));
        m.put("w", Float.valueOf(w));
        m.put("h", Float.valueOf(h));
        return m;
    }
}
