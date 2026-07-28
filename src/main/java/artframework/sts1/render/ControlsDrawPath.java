package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.assets.ResourceIds;
import artframework.context.ControlView;
import artframework.context.ControlsView;
import artframework.sts1.FullPresentMode;
import artframework.context.SurfaceIds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combat controls full-present draw description (16.6). Host paints end-turn chrome from
 * ControlsView; native end-turn may be suppressed when FULL + mounted.
 */
public final class ControlsDrawPath {

    public static final class DrawItem {
        public final String id;
        public final String text;
        public final boolean visible;
        public final boolean enabled;
        public final String iconSource;
        public final boolean iconFound;

        public DrawItem(
                String id,
                String text,
                boolean visible,
                boolean enabled,
                String iconSource,
                boolean iconFound) {
            this.id = id;
            this.text = text;
            this.visible = visible;
            this.enabled = enabled;
            this.iconSource = iconSource;
            this.iconFound = iconFound;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("text", text);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("iconSource", iconSource);
            m.put("iconFound", Boolean.valueOf(iconFound));
            return m;
        }
    }

    private ControlsDrawPath() {}

    public static boolean shouldSuppressNativeEndTurn() {
        return FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_CONTROLS)
                && ArtFramework.component(SurfaceIds.COMBAT_CONTROLS) != null
                && ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).isMounted()
                && "combat".equals(ArtFramework.projection().scene())
                && !Sts1RenderPipeline.isOverlayObserve();
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        ControlsView cv = ArtFramework.projection().controls();
        if (cv.controls.isEmpty() && (cv.endTurnVisible || cv.endTurnEnabled)) {
            out.add(itemFromEndTurn(cv));
            return out;
        }
        for (ControlView c : cv.controls) {
            out.add(fromControl(c));
        }
        if (out.isEmpty()) {
            out.add(itemFromEndTurn(cv));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("endTurnEnabled", Boolean.valueOf(ArtFramework.projection().controls().endTurnEnabled));
        m.put("endTurnVisible", Boolean.valueOf(ArtFramework.projection().controls().endTurnVisible));
        m.put("energy", Integer.valueOf(ArtFramework.projection().controls().energy));
        m.put("suppressNativeEndTurn", Boolean.valueOf(shouldSuppressNativeEndTurn()));
        m.put("presentLevel", FullPresentMode.combatControlsLevel().name());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }

    private static DrawItem itemFromEndTurn(ControlsView cv) {
        String iconKey = ResourceIds.UI_PREFIX + "endturn";
        AssetResolveResult icon = ArtFramework.assets().resolve(iconKey);
        return new DrawItem(
                ControlsView.END_TURN_ID,
                "End Turn",
                cv.endTurnVisible,
                cv.endTurnEnabled,
                icon.found ? icon.source : "",
                icon.found);
    }

    private static DrawItem fromControl(ControlView c) {
        String iconKey =
                c.iconResourceId != null && !c.iconResourceId.isEmpty()
                        ? c.iconResourceId
                        : (ControlsView.END_TURN_ID.equals(c.id)
                                ? ResourceIds.UI_PREFIX + "endturn"
                                : ResourceIds.UI_BUTTON_DEFAULT);
        AssetResolveResult icon = ArtFramework.assets().resolve(iconKey);
        return new DrawItem(
                c.id, c.text, c.visible, c.enabled, icon.found ? icon.source : "", icon.found);
    }
}
