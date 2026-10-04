package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.RestView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rest / campfire full-present draw description (25.5). */
public final class RestDrawPath {

    /** Native option icon source size (px) in {@code AbstractCampfireOption.render}. */
    static final float NATIVE_ICON = 256f;
    /**
     * Native at-rest scale factor: {@code AbstractCampfireOption.NORM_SCALE = 0.9f * Settings.scale}
     * (verified bytecode static init). Hover uses {@code HOVER_SCALE = Settings.scale} and is not
     * replicated. So the at-rest icon is {@code NATIVE_ICON * 0.9f * scale = 230.4f * scale}.
     */
    static final float NORM_SCALE_FACTOR = 0.9f;

    public static final class DrawItem {
        public final String id;
        public final String label;
        public final boolean visible;
        public final boolean enabled;
        public final String role;
        public final String resourceId;
        /** Top-left of the drawn rect (renderer / C2 convention). */
        public final float x;
        public final float y;
        public final float w;
        public final float h;
        /** Native {@code AbstractCampfireOption.hb} center for this button index. */
        public final float centerX;
        public final float centerY;
        /** 0-based native campfire button index (even -> left column, odd -> right column). */
        public final int buttonIndex;

        public DrawItem(String id, String label, boolean visible, boolean enabled) {
            this(id, label, visible, enabled, roleForOption(id), resourceForOption(id, label, enabled), 0);
        }

        /**
         * NRO-04 D07: a campfire option is drawn as the native {@code AbstractCampfireOption}
         * icon (256*0.9*scale at rest) centered on the native {@code CampfireUI} button grid.
         * {@code buttonIndex} is the raw option position; even indices are the left column and
         * odd the right column. All {@code Settings} reads fail open to a 1920x1080 / scale=1
         * layout.
         */
        public DrawItem(String id, String label, boolean visible, boolean enabled,
                String role, String resourceId, int buttonIndex) {
            this.id = id != null ? id : "";
            this.label = label != null ? label : "";
            this.visible = visible;
            this.enabled = enabled;
            this.role = role != null ? role : "";
            this.resourceId = resourceId != null ? resourceId : "";
            this.buttonIndex = buttonIndex;
            float[] s = settings();
            float width = s[0];
            float height = s[1];
            float scale = s[2];
            float xScale = s[3];
            float cx = buttonCenterX(buttonIndex, width, xScale);
            float cy = buttonCenterY(buttonIndex, height, scale);
            float size = NATIVE_ICON * NORM_SCALE_FACTOR * scale;
            this.w = size;
            this.h = size;
            this.x = cx - size / 2f;
            this.y = cy - size / 2f;
            this.centerX = cx;
            this.centerY = cy;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("label", label);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("role", role);
            m.put("resourceId", resourceId);
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("w", Float.valueOf(w));
            m.put("h", Float.valueOf(h));
            m.put("centerX", Float.valueOf(centerX));
            m.put("centerY", Float.valueOf(centerY));
            return m;
        }
    }

    private RestDrawPath() {}

    /**
     * Native {@code CampfireUI} grid: {@code BUTTON_START_X = WIDTH*0.416f} and
     * {@code BUTTON_SPACING_X = 300f*xScale}; index {@code i} is left column when even, right
     * column when odd. Verified against {@code CampfireUI.renderCampfireButtons} bytecode.
     */
    static float buttonCenterX(int buttonIndex, float width, float xScale) {
        float startX = width * 0.416f;
        return (buttonIndex % 2 == 0) ? startX : startX + 300f * xScale;
    }

    /**
     * Native {@code CampfireUI} grid: {@code BUTTON_START_Y = HEIGHT/2f + 180f*scale},
     * {@code BUTTON_SPACING_Y = -200f*scale}, {@code BUTTON_EXTRA_SPACING_Y = -70f*scale}; the
     * extra spacing is applied on every row after the first ({@code i/2 >= 1}). Verified against
     * {@code CampfireUI.renderCampfireButtons} bytecode.
     */
    static float buttonCenterY(int buttonIndex, float height, float scale) {
        float startY = height / 2f + 180f * scale;
        int row = buttonIndex / 2;
        if (row == 0) {
            return startY;
        }
        return startY - 200f * scale * row - 70f * scale;
    }

    /** Live {@code Settings} values, fail-open to {@code {1920, 1080, 1, 1}}. */
    private static float[] settings() {
        try {
            float width = com.megacrit.cardcrawl.core.Settings.WIDTH;
            float height = com.megacrit.cardcrawl.core.Settings.HEIGHT;
            float scale = com.megacrit.cardcrawl.core.Settings.scale;
            float xScale = com.megacrit.cardcrawl.core.Settings.xScale;
            if (width > 0f && height > 0f && scale > 0f && xScale > 0f) {
                return new float[] {width, height, scale, xScale};
            }
        } catch (Throwable ignored) {
        }
        return new float[] {1920f, 1080f, 1f, 1f};
    }

    public static boolean shouldSuppressNativeRest() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REST);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        int index = 0;
        for (RestView.RestOptionView o : ArtFramework.projection().rest().options) {
            out.add(new DrawItem(o.id, o.label, o.visible, o.enabled,
                    roleForOption(o.id), resourceForOption(o.id, o.label, o.enabled), index++));
        }
        return out;
    }

    /**
     * Pure projection of the current RestView into paintable rows: a title row plus one row per
     * visible option. Empty while the view is unavailable so the renderer never invents pixels.
     */
    public static List<RoomChromeLine> chromeLines() {
        List<RoomChromeLine> out = new ArrayList<RoomChromeLine>();
        RestView rest = ArtFramework.projection().rest();
        if (!rest.available) {
            return out;
        }
        out.add(line("title", "Campfire", true, "rest-title",
                ResourceIds.UI_CAMPFIRE_PANEL, 0));
        for (DrawItem item : buildFromProjection()) {
            if (!item.visible) continue;
            out.add(new RoomChromeLine("option:" + item.id, item.label, item.enabled,
                    item.visible, item.role, item.resourceId, item.x, item.y, item.w, item.h));
        }
        return out;
    }

    public static int materializedDrawCount() {
        int count = 0;
        for (RoomChromeLine line : chromeLines()) {
            if (line.visible) count++;
        }
        return count;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("available", Boolean.valueOf(ArtFramework.projection().rest().available));
        m.put("suppressNativeRest", Boolean.valueOf(shouldSuppressNativeRest()));
        m.put("presentLevel", FullPresentMode.restLevel().name());
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.REST);
        m.put("capability", cap.state.name());
        m.put("capabilityReason", cap.reason);
        m.put("drawCount", Integer.valueOf(materializedDrawCount()));
        // Slice C phase 2: real ART-painted chrome rows (additive probe field).
        m.put("chromeLineCount", Integer.valueOf(materializedDrawCount()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        // NRO-04 D07: native campfire option-button geometry. Each option is a
        // 256*0.9*scale (NORM_SCALE) icon centered on the native CampfireUI button grid (even
        // index -> left column, odd -> right). Exposed separately so the device probe can assert
        // native button geometry without changing the public items[] semantics.
        m.put("buttonCount", Integer.valueOf(items.size()));
        List<Map<String, Object>> buttons = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            Map<String, Object> b = new LinkedHashMap<String, Object>();
            b.put("id", d.id);
            b.put("resourceId", d.resourceId);
            b.put("x", Float.valueOf(d.x));
            b.put("y", Float.valueOf(d.y));
            b.put("w", Float.valueOf(d.w));
            b.put("h", Float.valueOf(d.h));
            b.put("centerX", Float.valueOf(d.centerX));
            b.put("centerY", Float.valueOf(d.centerY));
            b.put("enabled", Boolean.valueOf(d.enabled));
            buttons.add(b);
        }
        m.put("buttons", buttons);
        return m;
    }

    public static String resourceForOption(String id, String label, boolean enabled) {
        if (!enabled) return ResourceIds.UI_CAMPFIRE_DISABLED_OPTION;
        String key = (id != null && !id.isEmpty()) ? id : label;
        if (matches(key, "rest") || matches(key, "sleep")) return ResourceIds.UI_CAMPFIRE_REST_OPTION;
        if (matches(key, "smith")) return ResourceIds.UI_CAMPFIRE_SMITH_OPTION;
        if (matches(key, "dig")) return ResourceIds.UI_CAMPFIRE_DIG_OPTION;
        if (matches(key, "recall")) return ResourceIds.UI_CAMPFIRE_RECALL_OPTION;
        if (matches(key, "toke") || matches(key, "purge")) return ResourceIds.UI_CAMPFIRE_TOKE_OPTION;
        return ResourceIds.UI_CAMPFIRE_OTHER_OPTION;
    }

    private static String roleForOption(String id) {
        if (matches(id, "dig")) return "rest-dig-option";
        if (matches(id, "recall")) return "rest-recall-option";
        if (matches(id, "smith")) return "rest-smith-option";
        if (matches(id, "toke") || matches(id, "purge")) return "rest-toke-option";
        if (matches(id, "rest") || matches(id, "sleep")) return "rest-option";
        return "rest-other-option";
    }

    private static boolean matches(String value, String needle) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(needle);
    }

    private static RoomChromeLine line(String id, String text, boolean enabled, String role,
            String resourceId, int row) {
        float x = defaultX();
        float y = defaultY(row);
        float w = "title".equals(id) ? 420f : 360f;
        float h = 40f;
        return new RoomChromeLine(id, text, enabled, true, role, resourceId,
                x - w / 2f, y - h / 2f, w, h);
    }

    private static float defaultX() {
        try { return com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f; }
        catch (Throwable t) { return 960f; }
    }

    private static float defaultY(int row) {
        try { return com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.66f - row * 40f; }
        catch (Throwable t) { return 712.8f - row * 40f; }
    }
}
