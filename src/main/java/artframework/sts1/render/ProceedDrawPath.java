package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
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
        public final String resourceId;
        public final boolean visible;
        public final boolean enabled;
        private final int row;

        public DrawItem(String id, String text, String resourceId, boolean visible, boolean enabled, int row) {
            this.id = id;
            this.text = text;
            this.resourceId = resourceId != null ? resourceId : "";
            this.visible = visible;
            this.enabled = enabled;
            this.row = row;
        }

        public Rect bounds(float screenW, float screenH) {
            float w = 360f;
            float h = 56f;
            float x = screenW * 0.5f - w * 0.5f;
            float y = screenH * 0.2f - row * 64f - h * 0.5f;
            return new Rect(x, y, w, h);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("text", text);
            m.put("resourceId", resourceId);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            Rect r = bounds(1920f, 1080f);
            m.put("x", Float.valueOf(r.x));
            m.put("y", Float.valueOf(r.y));
            m.put("w", Float.valueOf(r.width));
            m.put("h", Float.valueOf(r.height));
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
        int row = 0;
        if (cv.proceedVisible || cv.proceedEnabled) {
            out.add(
                    new DrawItem(
                            ControlsView.PROCEED_ID,
                            "Proceed",
                            cv.proceedEnabled
                                    ? ResourceIds.UI_BUTTON_PROCEED_ENABLED
                                    : ResourceIds.UI_BUTTON_PROCEED_DISABLED,
                            cv.proceedVisible,
                            cv.proceedEnabled,
                            row++));
        }
        if (cv.cancelVisible || cv.cancelEnabled) {
            out.add(
                    new DrawItem(
                            ControlsView.CANCEL_ID,
                            "Cancel",
                            cv.cancelEnabled
                                    ? ResourceIds.UI_BUTTON_CANCEL_ENABLED
                                    : ResourceIds.UI_BUTTON_CANCEL_DISABLED,
                            cv.cancelVisible,
                            cv.cancelEnabled,
                            row));
        }
        return out;
    }

    public static int materializedDrawCount() {
        int n = 0;
        for (DrawItem item : buildFromProjection()) {
            if (item.visible) n++;
        }
        return n;
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
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.COMBAT_PROCEED);
        m.put("capability", cap.state.name());
        m.put("capabilityReason", cap.reason);
        m.put("drawCount", Integer.valueOf(items.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }
}
