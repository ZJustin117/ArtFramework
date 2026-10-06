package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable map surface snapshot for one present frame. */
public final class MapView {

    /**
     * Localized map-legend text (NRO-04 D03 follow-up). Native {@code Legend} reads
     * {@code CardCrawlGame.languagePack.getUIString("Legend").TEXT}: labels are
     * {@code TEXT[0/3/6/9/12/15]} (event/merchant/treasure/rest/enemy/elite) and the panel title is
     * {@code TEXT[18]}. The backend fills this from the live language pack; a missing/empty value
     * tells the pure draw path to fall back to its English defaults. The draw path never imports STS
     * classes, so this small value carries the localized text across the projection.
     */
    public static final class LegendLabels {
        public final String title;
        public final List<String> labels;

        public LegendLabels(String title, List<String> labels) {
            this.title = title != null ? title : "";
            if (labels == null || labels.isEmpty()) {
                this.labels = Collections.emptyList();
            } else {
                this.labels = Collections.unmodifiableList(new ArrayList<String>(labels));
            }
        }

        public static LegendLabels empty() {
            return new LegendLabels("", null);
        }

        public boolean isEmpty() {
            return title.isEmpty() && labels.isEmpty();
        }

        public int size() {
            return labels.size();
        }

        /** Localized label at {@code index}, or {@code ""} when absent (index-guarded). */
        public String label(int index) {
            return index >= 0 && index < labels.size() ? labels.get(index) : "";
        }
    }

    /**
     * Native map parchment background geometry for one present frame (D03 map background
     * follow-up). Native {@code DungeonMap.renderNormalMap} draws four textures ({@code mapTop/}
     * {@code mapMid}/{@code mapBot}/{@code mapBlend}) at live scroll/offset positions with the
     * {@code baseMapColor} fade alpha; ART omitted this layer, so the node tint read light-on-dark.
     * This value carries the LIVE native numbers across the projection without importing any STS
     * type. The backend fills it fail-open ({@code null} when a native field is unavailable), and
     * the pure draw path ({@code MapDrawPath.backgroundItems()}) derives the native draw rects from
     * these inputs.
     */
    public static final class MapBackground {
        /** Native {@code DungeonMap.mapMidDist} (live, or computed from {@code Settings.MAP_DST_Y}). */
        public final float mapMidDist;
        /** Native {@code DungeonMap.mapOffsetY} = {@code mapMidDist - 120*scale}. */
        public final float mapOffsetY;
        /** Native {@code DungeonMapScreen.offsetY} (live scroll). */
        public final float offsetY;
        /** Native {@code Settings.scale}. */
        public final float scale;
        /** Native {@code Settings.WIDTH} (background draw width). */
        public final int width;
        /** Native {@code Settings.HEIGHT} (viewport height, informational). */
        public final int height;
        /** Native {@code DungeonMap.H} = {@code 1020*scale}. */
        public final float h;
        /** Native {@code DungeonMap.BLEND_H} = {@code 512*scale}. */
        public final float blendH;
        /** Native {@code baseMapColor.a} fade alpha (0..1). */
        public final float alpha;
        /**
         * True when {@code AbstractDungeon.id.equals("TheEnding")}: native {@code DungeonMap} takes
         * the {@code renderFinalActMap} branch, which draws ONLY {@code top} then {@code bot} (no
         * {@code mid}) and {@code renderMapBlender} is a no-op for the final act (its
         * {@code "TheEnding"} guard skips the two {@code blend} strips). Fail-open false.
         */
        public final boolean finalAct;

        public MapBackground(
                float mapMidDist,
                float mapOffsetY,
                float offsetY,
                float scale,
                int width,
                int height,
                float h,
                float blendH,
                float alpha) {
            this(mapMidDist, mapOffsetY, offsetY, scale, width, height, h, blendH, alpha, false);
        }

        public MapBackground(
                float mapMidDist,
                float mapOffsetY,
                float offsetY,
                float scale,
                int width,
                int height,
                float h,
                float blendH,
                float alpha,
                boolean finalAct) {
            this.mapMidDist = mapMidDist;
            this.mapOffsetY = mapOffsetY;
            this.offsetY = offsetY;
            this.scale = scale;
            this.width = width;
            this.height = height;
            this.h = h;
            this.blendH = blendH;
            this.alpha = alpha;
            this.finalAct = finalAct;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("mapMidDist", Float.valueOf(mapMidDist));
            m.put("mapOffsetY", Float.valueOf(mapOffsetY));
            m.put("offsetY", Float.valueOf(offsetY));
            m.put("scale", Float.valueOf(scale));
            m.put("width", Integer.valueOf(width));
            m.put("height", Integer.valueOf(height));
            m.put("h", Float.valueOf(h));
            m.put("blendH", Float.valueOf(blendH));
            m.put("alpha", Float.valueOf(alpha));
            m.put("finalAct", Boolean.valueOf(finalAct));
            return m;
        }
    }

    public final List<MapNodeView> nodes;
    public final int viewportWidth;
    public final int viewportHeight;
    /** Localized legend text, or {@link LegendLabels#empty()} when unavailable (fail-open). */
    public final LegendLabels legend;
    /** Native map background geometry, or {@code null} when unavailable (fail-open). */
    public final MapBackground background;

    public MapView(List<MapNodeView> nodes, int viewportWidth, int viewportHeight) {
        this(nodes, viewportWidth, viewportHeight, null);
    }

    public MapView(
            List<MapNodeView> nodes, int viewportWidth, int viewportHeight, LegendLabels legend) {
        this(nodes, viewportWidth, viewportHeight, legend, null);
    }

    public MapView(
            List<MapNodeView> nodes,
            int viewportWidth,
            int viewportHeight,
            LegendLabels legend,
            MapBackground background) {
        if (nodes == null || nodes.isEmpty()) {
            this.nodes = Collections.emptyList();
        } else {
            this.nodes = Collections.unmodifiableList(new ArrayList<MapNodeView>(nodes));
        }
        this.viewportWidth = Math.max(0, viewportWidth);
        this.viewportHeight = Math.max(0, viewportHeight);
        this.legend = legend != null ? legend : LegendLabels.empty();
        this.background = background;
    }

    public static MapView empty() {
        return new MapView(null, 0, 0);
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public int nodeCount() {
        return nodes.size();
    }

    public MapNodeView find(int row, int col) {
        for (MapNodeView n : nodes) {
            if (n.row == row && n.col == col) {
                return n;
            }
        }
        return null;
    }

    /** Probe / legacy map shape. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("viewportWidth", Integer.valueOf(viewportWidth));
        m.put("viewportHeight", Integer.valueOf(viewportHeight));
        m.put("nodeCount", Integer.valueOf(nodes.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (MapNodeView n : nodes) {
            list.add(n.toMap());
        }
        m.put("nodes", list);
        return m;
    }
}
