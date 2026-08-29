package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.assets.ResourceIds;
import artframework.context.MapNodeView;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.component.Rect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Map full-present draw path (16.7): project MapView nodes through pan/zoom + HostAssets keys.
 */
public final class MapDrawPath {

    private static final MapPanZoom PAN = new MapPanZoom();

    public static final class DrawItem {
        public final int row;
        public final int col;
        public final float screenX;
        public final float screenY;
        public final boolean taken;
        public final boolean highlighted;
        public final boolean reachable;
        public final boolean pinned;
        public final String symbol;
        public final String roomKind;
        public final String resourceId;
        public final String artSource;
        public final boolean artFound;
        public final String outlineResourceId;
        public final String highlightResourceId;
        public final String outlineSource;
        public final String highlightSource;
        public final boolean outlineFound;
        public final boolean highlightFound;
        public final Rect bounds;

        public DrawItem(
                int row,
                int col,
                float screenX,
                float screenY,
                boolean taken,
                boolean highlighted,
                boolean reachable,
                boolean pinned,
                String symbol,
                String roomKind,
                String resourceId,
                String artSource,
                boolean artFound,
                String outlineResourceId,
                String highlightResourceId,
                String outlineSource,
                String highlightSource,
                boolean outlineFound,
                boolean highlightFound,
                Rect bounds) {
            this.row = row;
            this.col = col;
            this.screenX = screenX;
            this.screenY = screenY;
            this.taken = taken;
            this.highlighted = highlighted;
            this.reachable = reachable;
            this.pinned = pinned;
            this.symbol = symbol;
            this.roomKind = roomKind;
            this.resourceId = resourceId;
            this.artSource = artSource;
            this.artFound = artFound;
            this.outlineResourceId = outlineResourceId;
            this.highlightResourceId = highlightResourceId;
            this.outlineSource = outlineSource;
            this.highlightSource = highlightSource;
            this.outlineFound = outlineFound;
            this.highlightFound = highlightFound;
            this.bounds = bounds;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("row", Integer.valueOf(row));
            m.put("col", Integer.valueOf(col));
            m.put("screenX", Float.valueOf(screenX));
            m.put("screenY", Float.valueOf(screenY));
            m.put("taken", Boolean.valueOf(taken));
            m.put("highlighted", Boolean.valueOf(highlighted));
            m.put("reachable", Boolean.valueOf(reachable));
            m.put("pinned", Boolean.valueOf(pinned));
            m.put("symbol", symbol);
            m.put("roomKind", roomKind);
            m.put("resourceId", resourceId);
            m.put("artSource", artSource);
            m.put("artFound", Boolean.valueOf(artFound));
            m.put("outlineResourceId", outlineResourceId);
            m.put("highlightResourceId", highlightResourceId);
            m.put("outlineSource", outlineSource);
            m.put("highlightSource", highlightSource);
            m.put("outlineFound", Boolean.valueOf(outlineFound));
            m.put("highlightFound", Boolean.valueOf(highlightFound));
            Map<String, Object> geometry = new LinkedHashMap<String, Object>();
            geometry.put("x", Float.valueOf(bounds.x));
            geometry.put("y", Float.valueOf(bounds.y));
            geometry.put("width", Float.valueOf(bounds.width));
            geometry.put("height", Float.valueOf(bounds.height));
            m.put("bounds", geometry);
            return m;
        }
    }

    private static String lastScene;
    private static long lastSceneEpoch = -1L;
    private static boolean lastSceneKnown;

    private MapDrawPath() {}

    public static MapPanZoom panZoom() {
        return PAN;
    }

    private static void maybeResetPanZoom() {
        String scene = ArtFramework.projection().scene();
        long epoch = ArtFramework.projection().sceneEpoch();
        if (!"map".equals(scene)) {
            PAN.reset();
        } else if (lastSceneKnown
                && (!"map".equals(lastScene) || epoch != lastSceneEpoch)) {
            PAN.reset();
        }
        lastScene = scene;
        lastSceneEpoch = epoch;
        lastSceneKnown = true;
    }

    public static boolean shouldSuppressNativeMap() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.MAP);
    }

    public static List<DrawItem> buildFromProjection() {
        maybeResetPanZoom();
        List<DrawItem> out = new ArrayList<DrawItem>();
        MapView mv = ArtFramework.projection().map();
        for (MapNodeView n : mv.nodes) {
            float sx = PAN.toScreenX(n.x);
            float sy = PAN.toScreenY(n.y);
            AssetResolveResult art =
                    n.resourceId != null && !n.resourceId.isEmpty()
                            ? ArtFramework.assets().resolve(n.resourceId)
                            : AssetResolveResult.missing("", "no resource");
            String kind = n.roomKind.isEmpty() ? "unknown" : n.roomKind;
            String outlineId = ResourceIds.mapOutline(kind);
            AssetResolveResult outline = ArtFramework.assets().resolve(outlineId);
            String highlightId = n.pinned ? ResourceIds.UI_MAP_PIN
                    : (n.highlighted ? ResourceIds.UI_MAP_HIGHLIGHT : "");
            AssetResolveResult highlight = highlightId.isEmpty()
                    ? AssetResolveResult.missing("", "not active")
                    : ArtFramework.assets().resolve(highlightId);
            Rect bounds = new Rect(sx - n.width * PAN.zoom() / 2f,
                    sy - n.height * PAN.zoom() / 2f,
                    n.width * PAN.zoom(), n.height * PAN.zoom());
            out.add(new DrawItem(n.row, n.col, sx, sy, n.taken, n.highlighted, n.reachable,
                    n.pinned, n.symbol, n.roomKind, n.resourceId,
                    art.found || art.fallback ? art.source : "", art.found, outlineId, highlightId,
                    outline.found || outline.fallback ? outline.source : "",
                    highlight.found || highlight.fallback ? highlight.source : "",
                    outline.found, highlight.found, bounds));
        }
        return out;
    }

    public static DrawItem hitTest(float screenX, float screenY, float radius) {
        float r = radius > 0f ? radius : 40f;
        float r2 = r * r;
        DrawItem best = null;
        float bestD = Float.MAX_VALUE;
        for (DrawItem d : buildFromProjection()) {
            float dx = d.screenX - screenX;
            float dy = d.screenY - screenY;
            float dist = dx * dx + dy * dy;
            if (dist <= r2 && dist < bestD) {
                bestD = dist;
                best = d;
            }
        }
        return best;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("suppressNativeMap", Boolean.valueOf(shouldSuppressNativeMap()));
        m.put("presentLevel", FullPresentMode.mapLevel().name());
        m.put("panZoom", PAN.toMap());
        m.put("scene", ArtFramework.projection().scene());
        int missing = 0;
        int overlays = 0;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        int limit = Math.min(items.size(), 32);
        for (int i = 0; i < limit; i++) {
            DrawItem d = items.get(i);
            list.add(d.toMap());
            if (!d.artFound) {
                missing++;
            }
            if (d.reachable || d.highlighted) overlays++;
            if (d.pinned || d.highlighted) overlays++;
        }
        m.put("items", list);
        m.put("missingArt", Integer.valueOf(missing));
        m.put("overlayCount", Integer.valueOf(overlays));
        m.put("truncated", Boolean.valueOf(items.size() > limit));
        return m;
    }

    public static void resetForTests() {
        PAN.reset();
        lastScene = null;
        lastSceneEpoch = -1L;
        lastSceneKnown = false;
    }
}
