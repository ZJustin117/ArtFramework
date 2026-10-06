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
    /**
     * Native option LABEL anchor delta BELOW the icon center. Verified in
     * {@code AbstractCampfireOption.render}:
     * {@code FontHelper.renderFontCenteredTopAligned(sb, FontHelper.topPanelInfoFont, this.label,
     * this.hb.cX, this.hb.cY - 60f*Settings.scale - 50f*Settings.scale*(this.scale/Settings.scale))}.
     * At rest {@code this.scale = NORM_SCALE = 0.9f*Settings.scale}, so the factor is 0.9 and the
     * top-aligned anchor sits {@code 60 + 50*0.9 = 105f * Settings.scale} BELOW {@code hb.cY}; the
     * label X is {@code hb.cX} (icon center, centered). Hover ({@code this.scale -> Settings.scale})
     * would move it to {@code 110f*scale}; not replicated.
     */
    static final float LABEL_OFFSET_Y = 105f;

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
        /**
         * Native option LABEL anchor: X = icon center X, Y top-aligned at
         * {@code centerY - LABEL_OFFSET_Y*Settings.scale} ({@code renderFontCenteredTopAligned}).
         */
        public final float labelAnchorX;
        public final float labelAnchorY;
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
            this.labelAnchorX = cx;
            this.labelAnchorY = cy - LABEL_OFFSET_Y * scale;
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
            m.put("labelAnchorX", Float.valueOf(labelAnchorX));
            m.put("labelAnchorY", Float.valueOf(labelAnchorY));
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

    /**
     * The resolved native option label Y offset BELOW the icon center: {@code LABEL_OFFSET_Y *
     * Settings.scale} (fail-open to {@code LABEL_OFFSET_Y} at scale=1). Kept here so the renderer
     * uses the native formula rather than a per-call magic number.
     */
    public static float labelOffsetY() {
        try {
            float scale = com.megacrit.cardcrawl.core.Settings.scale;
            if (scale > 0f) {
                return LABEL_OFFSET_Y * scale;
            }
        } catch (Throwable ignored) {
        }
        return LABEL_OFFSET_Y;
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
     * Pure projection of the current RestView into paintable rows: one row per visible option.
     * Empty while the view is unavailable so the renderer never invents pixels.
     *
     * <p>No synthetic title row: verified native {@code CampfireUI.render} draws NO campfire site
     * title (only fire, the player, bubbles, {@code bubbleMsg}, the option buttons, the scrollbar,
     * and the touch confirm button). The previous ART-only {@code "Campfire"} title was therefore
     * not native and is dropped (it also occluded the centered options).
     */
    public static List<RoomChromeLine> chromeLines() {
        List<RoomChromeLine> out = new ArrayList<RoomChromeLine>();
        RestView rest = ArtFramework.projection().rest();
        if (!rest.available) {
            return out;
        }
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
        // NRO-04 D07 follow-up: verified native CampfireUI.render draws NO campfire site title, so
        // ART emits no synthetic title chrome row. Exposed so the device probe can assert it.
        m.put("hasTitle", Boolean.FALSE);
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
            // Native option label anchor (renderFontCenteredTopAligned): X = icon centerX,
            // TOP-aligned Y = centerY - 105*scale (at rest), i.e. BELOW the icon center.
            b.put("labelAnchorX", Float.valueOf(d.labelAnchorX));
            b.put("labelAnchorY", Float.valueOf(d.labelAnchorY));
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
}
