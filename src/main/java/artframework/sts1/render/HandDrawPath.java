package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.context.CardEntity;
import artframework.context.CardZone;
import artframework.sts1.assets.Sts1HostAssets;
import artframework.component.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure hand full-present draw path description. It retains logical resource ids for the STS1 host
 * renderer while keeping texture materialization outside the pure projection path.
 */
public final class HandDrawPath {

    public static final float CARD_WIDTH = 250f;
    public static final float CARD_HEIGHT = 350f;

    public static final class DrawItem {
        public final String instanceId;
        public final String cardId;
        public final float x;
        public final float y;
        public final float rotation;
        public final float scale;
        public final boolean visible;
        public final String artSource;
        public final String frameSource;
        public final boolean artFound;
        public final String artResourceId;
        public final String frameResourceId;

        public DrawItem(
                String instanceId,
                String cardId,
                float x,
                float y,
                float rotation,
                float scale,
                boolean visible,
                String artSource,
                String frameSource,
                boolean artFound,
                String artResourceId,
                String frameResourceId) {
            this.instanceId = instanceId;
            this.cardId = cardId;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.scale = scale;
            this.visible = visible;
            this.artSource = artSource;
            this.frameSource = frameSource;
            this.artFound = artFound;
            this.artResourceId = artResourceId;
            this.frameResourceId = frameResourceId;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("instanceId", instanceId);
            m.put("cardId", cardId);
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("rotation", Float.valueOf(rotation));
            m.put("scale", Float.valueOf(scale));
            m.put("visible", Boolean.valueOf(visible));
            m.put("artSource", artSource);
            m.put("frameSource", frameSource);
            m.put("artFound", Boolean.valueOf(artFound));
            m.put("artResourceId", artResourceId);
            m.put("frameResourceId", frameResourceId);
            Rect bounds = bounds();
            Map<String, Object> b = new LinkedHashMap<String, Object>();
            b.put("x", Float.valueOf(bounds.x));
            b.put("y", Float.valueOf(bounds.y));
            b.put("w", Float.valueOf(bounds.width));
            b.put("h", Float.valueOf(bounds.height));
            m.put("bounds", b);
            return m;
        }

        public Rect bounds() {
            float s = scale > 0f ? scale : 1f;
            float w = CARD_WIDTH * s;
            float h = CARD_HEIGHT * s;
            return new Rect(x - w / 2f, y - h / 2f, w, h);
        }
    }

    private HandDrawPath() {}

    private static final Map<String, CachedItem> CACHE = new HashMap<String, CachedItem>();

    /** Build draw list from current projection HAND zone + HostAssets resolve. */
    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        Set<String> current = new HashSet<String>();
        for (CardEntity e : ArtFramework.projection().listZone(CardZone.HAND)) {
            if (e == null) continue;
            current.add(e.instanceId);
            CachedItem cached = CACHE.get(e.instanceId);
            long key = key(e);
            if (cached == null || cached.key != key) {
                cached = new CachedItem(key, fromEntity(e));
                CACHE.put(e.instanceId, cached);
            }
            out.add(cached.item);
        }
        for (String id : new ArrayList<String>(CACHE.keySet())) {
            if (!current.contains(id)) {
                CACHE.remove(id);
            }
        }
        return out;
    }

    private static long key(CardEntity e) {
        long h = 17L;
        h = 31L * h + stringHash(e.cardId);
        h = 31L * h + stringHash(e.artResourceId);
        h = 31L * h + stringHash(e.frameResourceId);
        h = 31L * h + (e.pose != null ? e.pose.hashCode() : 0);
        return h;
    }

    private static int stringHash(String value) {
        return value != null ? value.hashCode() : 0;
    }

    private static final class CachedItem {
        final long key;
        final DrawItem item;

        CachedItem(long key, DrawItem item) {
            this.key = key;
            this.item = item;
        }
    }

    public static void resetForTests() {
        CACHE.clear();
    }

    public static DrawItem fromEntity(CardEntity e) {
        if (e == null) {
            return new DrawItem("", "", 0, 0, 0, 1, false, "", "", false, "", "");
        }
        float x = e.pose != null ? e.pose.x : 0f;
        float y = e.pose != null ? e.pose.y : 0f;
        float rot = e.pose != null ? e.pose.rotation : 0f;
        float scale = e.pose != null ? e.pose.scale : 1f;
        boolean vis = e.pose == null || e.pose.visible;
        AssetResolveResult art =
                e.artResourceId != null && !e.artResourceId.isEmpty()
                        ? ArtFramework.assets().resolve(e.artResourceId)
                        : Sts1HostAssets.resolveCardArt(e.cardId);
        AssetResolveResult frame =
                e.frameResourceId != null && !e.frameResourceId.isEmpty()
                        ? ArtFramework.assets().resolve(e.frameResourceId)
                        : AssetResolveResult.missing("", "no frame");
        return new DrawItem(
                e.instanceId,
                e.cardId != null ? e.cardId : "",
                x,
                y,
                rot,
                scale,
                vis,
                art.found || art.fallback ? art.source : "",
                frame.found ? frame.source : "",
                art.found,
                e.artResourceId != null ? e.artResourceId : "",
                e.frameResourceId != null ? e.frameResourceId : "");
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        int missingArt = 0;
        for (DrawItem d : items) {
            list.add(d.toMap());
            if (!d.artFound) {
                missingArt++;
            }
        }
        m.put("items", list);
        m.put("missingArt", Integer.valueOf(missingArt));
        artframework.core.PresentResolved pr =
                artframework.core.PresentResolve.forSurface(
                        artframework.context.SurfaceIds.COMBAT_HAND);
        m.put("chrome", pr.chrome.probeSummary());
        m.put("presentProfile", pr.profileId);
        m.put("projectPresent", artframework.core.ProjectPresent.id());
        m.put("fromProject", Boolean.valueOf(pr.fromProject));
        m.put("packId", pr.packId);
        return m;
    }

    /**
     * Compare projection hand poses to host samples (tolerance in present-space units).
     */
    public static GeometryCompare.Report compareToHost(List<GeometryCompare.Sample> host, float tolerance) {
        return GeometryCompare.compare(ArtFramework.projection().listZone(CardZone.HAND), host, tolerance);
    }
}
