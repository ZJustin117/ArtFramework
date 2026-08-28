package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Event full-present draw description (22.3). Host paints option chrome from EventView; native
 * event UI may be suppressed when ART_DELEGATED and FULL_READY, with base dialog pixels tracked
 * as an exposed supply gap.
 */
public final class EventDrawPath {

    public static final class DrawItem {
        public final int index;
        public final String label;
        public final boolean visible;
        public final boolean enabled;
        public final float x;
        public final float y;
        public final float w;
        public final float h;

        public DrawItem(
                int index,
                String label,
                boolean visible,
                boolean enabled,
                float x,
                float y,
                float w,
                float h) {
            this.index = index;
            this.label = label != null ? label : "";
            this.visible = visible;
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("index", Integer.valueOf(index));
            m.put("label", label);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("w", Float.valueOf(w));
            m.put("h", Float.valueOf(h));
            return m;
        }
    }

    private EventDrawPath() {}

    public static boolean shouldSuppressNativeEvent() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.EVENT);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        EventView ev = ArtFramework.projection().event();
        int i = 0;
        for (EventOptionView o : ev.options) {
            float y = o.y != 0f || o.h != 0f ? o.y : defaultOptionY(i, ev.optionCount());
            float x = o.x != 0f || o.w != 0f ? o.x : defaultOptionX();
            float w = o.w > 0f ? o.w : 420f;
            float h = o.h > 0f ? o.h : 40f;
            out.add(new DrawItem(o.index, o.label, o.visible, o.enabled, x, y, w, h));
            i++;
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("title", ArtFramework.projection().event().title);
        m.put("available", Boolean.valueOf(ArtFramework.projection().event().available));
        m.put("suppressNativeEvent", Boolean.valueOf(shouldSuppressNativeEvent()));
        m.put("presentLevel", FullPresentMode.eventLevel().name());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }

    private static float defaultOptionX() {
        try {
            return com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
        } catch (Throwable t) {
            return 960f;
        }
    }

    private static float defaultOptionY(int index, int total) {
        float base;
        try {
            base = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.35f;
        } catch (Throwable t) {
            base = 400f;
        }
        return base - index * 48f;
    }
}
