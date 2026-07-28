package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.context.MapNodeView;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

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
        public final String symbol;
        public final String roomKind;
        public final String resourceId;
        public final String artSource;
        public final boolean artFound;

        public DrawItem(
                int row,
                int col,
                float screenX,
                float screenY,
                boolean taken,
                boolean highlighted,
                String symbol,
                String roomKind,
                String resourceId,
                String artSource,
                boolean artFound) {
            this.row = row;
            this.col = col;
            this.screenX = screenX;
            this.screenY = screenY;
            this.taken = taken;
            this.highlighted = highlighted;
            this.symbol = symbol;
            this.roomKind = roomKind;
            this.resourceId = resourceId;
            this.artSource = artSource;
            this.artFound = artFound;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("row", Integer.valueOf(row));
            m.put("col", Integer.valueOf(col));
            m.put("screenX", Float.valueOf(screenX));
            m.put("screenY", Float.valueOf(screenY));
            m.put("taken", Boolean.valueOf(taken));
            m.put("highlighted", Boolean.valueOf(highlighted));
            m.put("symbol", symbol);
            m.put("roomKind", roomKind);
            m.put("resourceId", resourceId);
            m.put("artSource", artSource);
            m.put("artFound", Boolean.valueOf(artFound));
            return m;
        }
    }

    private MapDrawPath() {}

    public static MapPanZoom panZoom() {
        return PAN;
    }

    public static boolean shouldSuppressNativeMap() {
        return FullPresentMode.maySuppressNative(SurfaceIds.MAP)
                && ArtFramework.component(SurfaceIds.MAP) != null
                && ArtFramework.component(SurfaceIds.MAP).isMounted()
                && "map".equals(ArtFramework.projection().scene())
                && !Sts1RenderPipeline.isOverlayObserve()
                && !artframework.sts1.PresentSafety.isPanic();
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        MapView mv = ArtFramework.projection().map();
        for (MapNodeView n : mv.nodes) {
            float sx = PAN.toScreenX(n.x);
            float sy = PAN.toScreenY(n.y);
            AssetResolveResult art =
                    n.resourceId != null && !n.resourceId.isEmpty()
                            ? ArtFramework.assets().resolve(n.resourceId)
                            : AssetResolveResult.missing("", "no resource");
            out.add(
                    new DrawItem(
                            n.row,
                            n.col,
                            sx,
                            sy,
                            n.taken,
                            n.highlighted,
                            n.symbol,
                            n.roomKind,
                            n.resourceId,
                            art.found || art.fallback ? art.source : "",
                            art.found));
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
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        int limit = Math.min(items.size(), 32);
        for (int i = 0; i < limit; i++) {
            DrawItem d = items.get(i);
            list.add(d.toMap());
            if (!d.artFound) {
                missing++;
            }
        }
        m.put("items", list);
        m.put("missingArt", Integer.valueOf(missing));
        m.put("truncated", Boolean.valueOf(items.size() > limit));
        return m;
    }

    public static void resetForTests() {
        PAN.reset();
    }
}
