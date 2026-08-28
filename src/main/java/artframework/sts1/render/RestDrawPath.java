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

    public static final class DrawItem {
        public final String id;
        public final String label;
        public final boolean visible;
        public final boolean enabled;
        public final String role;
        public final String resourceId;
        public final float x;
        public final float y;
        public final float w;
        public final float h;

        public DrawItem(String id, String label, boolean visible, boolean enabled) {
            this(id, label, visible, enabled, roleForOption(id), resourceForOption(id, label, enabled), 0);
        }

        public DrawItem(String id, String label, boolean visible, boolean enabled,
                String role, String resourceId, int row) {
            this.id = id != null ? id : "";
            this.label = label != null ? label : "";
            this.visible = visible;
            this.enabled = enabled;
            this.role = role != null ? role : "";
            this.resourceId = resourceId != null ? resourceId : "";
            float cx = defaultX();
            float cy = defaultY(row);
            this.w = 360f;
            this.h = 40f;
            this.x = cx - w / 2f;
            this.y = cy - h / 2f;
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
            return m;
        }
    }

    private RestDrawPath() {}

    public static boolean shouldSuppressNativeRest() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REST);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        int row = 1;
        for (RestView.RestOptionView o : ArtFramework.projection().rest().options) {
            out.add(new DrawItem(o.id, o.label, o.visible, o.enabled,
                    roleForOption(o.id), resourceForOption(o.id, o.label, o.enabled), row++));
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
