package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ControlsView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Proceed / cancel combat chrome (25.1). */
public final class ProceedDrawPath {

    public static final class DrawItem {
        public final String id;
        public final String text;
        public final boolean visible;
        public final boolean enabled;

        public DrawItem(String id, String text, boolean visible, boolean enabled) {
            this.id = id;
            this.text = text;
            this.visible = visible;
            this.enabled = enabled;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("text", text);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            return m;
        }
    }

    private ProceedDrawPath() {}

    public static boolean shouldSuppressNativeProceed() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_PROCEED);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        ControlsView cv = ArtFramework.projection().controls();
        if (cv.proceedVisible || cv.proceedEnabled) {
            out.add(
                    new DrawItem(
                            ControlsView.PROCEED_ID, "Proceed", cv.proceedVisible, cv.proceedEnabled));
        }
        if (cv.cancelVisible || cv.cancelEnabled) {
            out.add(
                    new DrawItem(
                            ControlsView.CANCEL_ID, "Cancel", cv.cancelVisible, cv.cancelEnabled));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        ControlsView cv = ArtFramework.projection().controls();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("proceedEnabled", Boolean.valueOf(cv.proceedEnabled));
        m.put("proceedVisible", Boolean.valueOf(cv.proceedVisible));
        m.put("cancelEnabled", Boolean.valueOf(cv.cancelEnabled));
        m.put("cancelVisible", Boolean.valueOf(cv.cancelVisible));
        m.put("suppressNativeProceed", Boolean.valueOf(shouldSuppressNativeProceed()));
        m.put("presentLevel", FullPresentMode.proceedLevel().name());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }
}
