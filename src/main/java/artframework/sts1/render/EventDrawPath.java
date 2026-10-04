package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.SurfaceIds;
import artframework.assets.ResourceIds;
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
        public final String id;
        public final String resourceId;
        public final String role;
        /**
         * When true the renderer must NOT submit {@link #resourceId} as a texture; the item is a
         * TEXT-only draw (label) centered on {@link #x}/{@link #y}. Used by the event TITLE, whose
         * {@code UI_EVENT_TITLE} resource is currently mis-mapped to the panel texture (see the
         * mis-map note in {@link #titleItem}).
         */
        public final boolean textOnly;

        public DrawItem(
                int index,
                String label,
                boolean visible,
                boolean enabled,
                float x,
                float y,
                float w,
                float h) {
            this("option:" + index,
                    enabled ? ResourceIds.UI_EVENT_BUTTON_ENABLED : ResourceIds.UI_EVENT_BUTTON_DISABLED,
                    "event-option", index,
                    label, visible, enabled, x, y, w, h);
        }

        public DrawItem(String id, String resourceId, String role, int index, String label,
                boolean visible, boolean enabled, float x, float y, float w, float h) {
            this(id, resourceId, role, index, label, visible, enabled, x, y, w, h, false);
        }

        public DrawItem(String id, String resourceId, String role, int index, String label,
                boolean visible, boolean enabled, float x, float y, float w, float h,
                boolean textOnly) {
            this.id = id != null ? id : "";
            this.resourceId = resourceId != null ? resourceId : ResourceIds.UI_PANEL_DEFAULT;
            this.role = role != null ? role : "event-item";
            this.index = index;
            this.label = label != null ? label : "";
            this.visible = visible;
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.textOnly = textOnly;
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
            m.put("id", id);
            m.put("resourceId", resourceId);
            m.put("role", role);
            m.put("textOnly", Boolean.valueOf(textOnly));
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
        // NRO-04 D06: native GenericEventDialog.render geometry (verified bytecode).
        float[] s = eventSettings();
        float width = s[0];
        float scale = s[2];
        float xScale = s[3];
        float eventY = s[4];
        // Native panel: sb.draw(eventBackgroundImg, WIDTH/2f - 881.5f - 12f*xScale,
        // EVENT_Y - 403f - 64f*scale, 881.5f, 403f, 1763f, 806f, xScale, scale, ...). libgdx places
        // the sprite's bottom-left at x + originX*(1-scaleX) and the drawn size is (1763*xScale,
        // 806*scale); with originX = 881.5 = 1763/2 the CENTER -- the convention DrawItem.x/y use --
        // simplifies to (WIDTH/2 - 12*xScale, EVENT_Y - 64*scale).
        out.add(new DrawItem("panel", ResourceIds.UI_EVENT_PANEL, "event-panel", -1, "",
                ev.available, true,
                width * 0.5f - 12f * xScale,
                eventY - 64f * scale,
                1763f * xScale, 806f * scale));
        // Native title: FontHelper.renderFontCentered(sb, losePowerFont, title, TITLE_X, TITLE_Y,
        // titleColor, ...) with TITLE_X = 570f*xScale and TITLE_Y = EVENT_Y + 408f*scale; drawn
        // CENTERED, so the item center is exactly (TITLE_X, TITLE_Y). The item is TEXT ONLY
        // (textOnly=true): UI_EVENT_TITLE is currently MIS-MAPPED to images/ui/event/panel.png
        // (the panel file), so submitting it as a texture would paint a bogus panel-sized
        // rectangle. The item keeps id/role/ResourceId for identity + tests; renderEvent skips the
        // texture draw for textOnly items. w/h are a sensible text box, not a native constant.
        out.add(new DrawItem("title", ResourceIds.UI_EVENT_TITLE, "event-title", -1, ev.title,
                ev.available && !ev.title.isEmpty(), true,
                570f * xScale, eventY + 408f * scale,
                800f * xScale, 64f * scale, true));
        int i = 0;
        for (EventOptionView o : ev.options) {
            float y = o.y != 0f || o.h != 0f ? o.y : defaultOptionY(i, ev.optionCount());
            float x = o.x != 0f || o.w != 0f ? o.x : defaultOptionX();
            float w = o.w > 0f ? o.w : 420f;
            float h = o.h > 0f ? o.h : 40f;
            out.add(new DrawItem("option:" + o.index,
                    o.enabled ? ResourceIds.UI_EVENT_BUTTON_ENABLED : ResourceIds.UI_EVENT_BUTTON_DISABLED,
                    "event-option", o.index, o.label, o.visible, o.enabled, x, y, w, h));
            i++;
        }
        return out;
    }

    /**
     * The event dialog item with id {@code "panel"}, or {@code null} when it is not visible.
     * Convenience for {@link #probeSlice} so the native panel geometry is probe-addressable as a
     * sub-map without changing {@code items[]} semantics.
     */
    public static DrawItem panelItem() {
        for (DrawItem item : buildFromProjection()) {
            if ("panel".equals(item.id)) return item.visible ? item : null;
        }
        return null;
    }

    /** Number of panel pixels {@code renderEvent} would submit ({@code 1} when visible). */
    public static int panelCount() {
        return panelItem() != null ? 1 : 0;
    }

    /**
     * Number of pixels {@code renderEvent} actually submits. Unlike the D04 reward sheet (kept out
     * of {@code items[]}), the event panel already lives in {@code items[]}, so this equals
     * {@link #materializedDrawCount()} (every visible item is submitted). Kept as an explicit,
     * consistently named probe field.
     */
    public static int submitCount() {
        return visibleCount(buildFromProjection());
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(visibleCount(items)));
        m.put("title", ArtFramework.projection().event().title);
        m.put("available", Boolean.valueOf(ArtFramework.projection().event().available));
        m.put("suppressNativeEvent", Boolean.valueOf(shouldSuppressNativeEvent()));
        m.put("presentLevel", FullPresentMode.eventLevel().name());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        // NRO-04 D06: the native panel geometry as its own sub-map (the panel stays in items[],
        // so public item-list semantics are unchanged). x/y are the SAME CENTER convention as
        // items[]; renderEvent's x - w/2, y - h/2 yields the native bottom-left.
        DrawItem panel = panelItem();
        Map<String, Object> panelMap = new LinkedHashMap<String, Object>();
        if (panel != null) {
            panelMap.put("resourceId", panel.resourceId);
            panelMap.put("x", Float.valueOf(panel.x));
            panelMap.put("y", Float.valueOf(panel.y));
            panelMap.put("w", Float.valueOf(panel.w));
            panelMap.put("h", Float.valueOf(panel.h));
        }
        m.put("panel", panelMap.isEmpty() ? null : panelMap);
        m.put("panelCount", Integer.valueOf(panelCount()));
        m.put("submitCount", Integer.valueOf(submitCount()));
        return m;
    }

    public static int materializedDrawCount() {
        return visibleCount(buildFromProjection());
    }

    private static int visibleCount(List<DrawItem> items) {
        int count = 0;
        for (DrawItem item : items) if (item.visible) count++;
        return count;
    }

    /**
     * Live {@code Settings} values {@code {WIDTH, HEIGHT, scale, xScale, EVENT_Y}} needed by the
     * native event formulas, each read fail-open with a safe default. {@code EVENT_Y} falls back to
     * {@code HEIGHT/2f - 128f*scale} (the {@code Settings} static-init formula) when absent/zero.
     */
    private static float[] eventSettings() {
        float width = 1920f;
        float height = 1080f;
        float scale = 1f;
        float xScale = 1f;
        float eventY = Float.NaN;
        try {
            float w = com.megacrit.cardcrawl.core.Settings.WIDTH;
            float h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
            float sc = com.megacrit.cardcrawl.core.Settings.scale;
            float xs = com.megacrit.cardcrawl.core.Settings.xScale;
            if (w > 0f) width = w;
            if (h > 0f) height = h;
            if (sc > 0f) scale = sc;
            if (xs > 0f) xScale = xs;
            eventY = com.megacrit.cardcrawl.core.Settings.EVENT_Y;
        } catch (Throwable ignored) {
            // keep the safe defaults above
        }
        if (Float.isNaN(eventY) || eventY <= 0f) {
            eventY = height * 0.5f - 128f * scale;
        }
        return new float[] {width, height, scale, xScale, eventY};
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
