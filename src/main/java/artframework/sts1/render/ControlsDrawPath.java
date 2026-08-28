package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.assets.ResourceIds;
import artframework.context.ControlView;
import artframework.context.ControlsView;
import artframework.sts1.FullPresentMode;
import artframework.context.SurfaceIds;
import artframework.component.Rect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combat controls full-present draw description (16.6). Host paints end-turn chrome from
 * ControlsView; native end-turn may be suppressed when ART_DELEGATED and FULL_READY, with
 * incomplete button art exposed as a pixel-supply gap.
 */
public final class ControlsDrawPath {

    public static Rect endTurnBounds(float screenWidth, float screenHeight) {
        float w = screenWidth * 0.2f;
        float h = screenHeight * 0.14f;
        return new Rect(screenWidth * 0.85f - w / 2f, screenHeight * 0.12f - h / 2f, w, h);
    }

    /** Backend-projected hitbox when available; constant layout only as a fallback. */
    public static Rect endTurnProjectedBounds(float screenWidth, float screenHeight) {
        ControlsView cv = ArtFramework.projection().controls();
        if (cv.hasEndTurnBounds()) {
            return new Rect(cv.endTurnX, cv.endTurnY, cv.endTurnW, cv.endTurnH);
        }
        return endTurnBounds(screenWidth, screenHeight);
    }

    /** Backend-projected localized label; constant text only as a fallback. */
    public static String endTurnLabel() {
        ControlsView cv = ArtFramework.projection().controls();
        return cv.endTurnLabel != null && !cv.endTurnLabel.isEmpty()
                ? cv.endTurnLabel
                : "End Turn";
    }

    public static final class DrawItem {
        public final String id;
        public final String text;
        public final boolean visible;
        public final boolean enabled;
        public final String iconSource;
        public final boolean iconFound;
        public final String resourceId;
        public final String hoverResourceId;
        public final Rect bounds;

        public DrawItem(
                String id,
                String text,
                boolean visible,
                boolean enabled,
                String iconSource,
                boolean iconFound) {
            this(id, text, visible, enabled, iconSource, iconFound, "", "", null);
        }

        public DrawItem(
                String id,
                String text,
                boolean visible,
                boolean enabled,
                String iconSource,
                boolean iconFound,
                String resourceId,
                String hoverResourceId,
                Rect bounds) {
            this.id = id;
            this.text = text;
            this.visible = visible;
            this.enabled = enabled;
            this.iconSource = iconSource;
            this.iconFound = iconFound;
            this.resourceId = resourceId != null ? resourceId : "";
            this.hoverResourceId = hoverResourceId != null ? hoverResourceId : "";
            this.bounds = bounds;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("text", text);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("iconSource", iconSource);
            m.put("iconFound", Boolean.valueOf(iconFound));
            m.put("resourceId", resourceId);
            m.put("hoverResourceId", hoverResourceId);
            if (bounds != null) {
                m.put("x", Float.valueOf(bounds.x));
                m.put("y", Float.valueOf(bounds.y));
                m.put("width", Float.valueOf(bounds.width));
                m.put("height", Float.valueOf(bounds.height));
            }
            return m;
        }
    }

    private ControlsDrawPath() {}

    public static boolean shouldSuppressNativeEndTurn() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_CONTROLS);
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

    /** Items actually materialized/drawn by the controls C2 surface (energy is separate). */
    public static int materializedDrawCount() {
        int count = 0;
        for (DrawItem item : buildFromProjection()) {
            if (item.visible && isEndTurnItem(item.id)) {
                count++;
            }
        }
        return count;
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
        String iconKey = cv.endTurnEnabled
                ? ResourceIds.UI_BUTTON_END_TURN
                : ResourceIds.UI_BUTTON_END_TURN_DISABLED;
        String hoverKey = ResourceIds.UI_BUTTON_END_TURN_HOVER;
        AssetResolveResult icon = ArtFramework.assets().resolve(iconKey);
        return new DrawItem(
                ControlsView.END_TURN_ID,
                endTurnLabel(),
                cv.endTurnVisible,
                cv.endTurnEnabled,
                icon.found ? icon.source : "",
                icon.found,
                iconKey,
                hoverKey,
                endTurnProjectedBounds(screenWidth(), screenHeight()));
    }

    private static DrawItem fromControl(ControlView c) {
        String iconKey =
                c.iconResourceId != null && !c.iconResourceId.isEmpty()
                        ? c.iconResourceId
                        : (ControlsView.END_TURN_ID.equals(c.id)
                                ? (c.enabled ? ResourceIds.UI_BUTTON_END_TURN
                                        : ResourceIds.UI_BUTTON_END_TURN_DISABLED)
                                : ResourceIds.UI_BUTTON_DEFAULT);
        if (ControlsView.END_TURN_ID.equals(c.id)) {
            return itemFromEndTurn(ArtFramework.projection().controls());
        }
        AssetResolveResult icon = ArtFramework.assets().resolve(iconKey);
        return new DrawItem(
                c.id,
                c.text,
                c.visible,
                c.enabled,
                icon.found ? icon.source : "",
                icon.found,
                iconKey,
                "",
                null);
    }

    static boolean isEndTurnItem(String id) {
        return ControlsView.END_TURN_ID.equals(id) || "end_turn".equals(id);
    }

    private static float screenWidth() {
        return com.megacrit.cardcrawl.core.Settings.WIDTH > 0f
                ? com.megacrit.cardcrawl.core.Settings.WIDTH : 1920f;
    }

    private static float screenHeight() {
        return com.megacrit.cardcrawl.core.Settings.HEIGHT > 0f
                ? com.megacrit.cardcrawl.core.Settings.HEIGHT : 1080f;
    }
}
