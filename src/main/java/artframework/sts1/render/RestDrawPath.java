package artframework.sts1.render;

import artframework.api.ArtFramework;
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

        public DrawItem(String id, String label, boolean visible, boolean enabled) {
            this.id = id != null ? id : "";
            this.label = label != null ? label : "";
            this.visible = visible;
            this.enabled = enabled;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("label", label);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            return m;
        }
    }

    private RestDrawPath() {}

    public static boolean shouldSuppressNativeRest() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REST);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        for (RestView.RestOptionView o : ArtFramework.projection().rest().options) {
            out.add(new DrawItem(o.id, o.label, o.visible, o.enabled));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("available", Boolean.valueOf(ArtFramework.projection().rest().available));
        m.put("suppressNativeRest", Boolean.valueOf(shouldSuppressNativeRest()));
        m.put("presentLevel", FullPresentMode.restLevel().name());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }
}
