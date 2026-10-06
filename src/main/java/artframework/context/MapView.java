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

    public final List<MapNodeView> nodes;
    public final int viewportWidth;
    public final int viewportHeight;
    /** Localized legend text, or {@link LegendLabels#empty()} when unavailable (fail-open). */
    public final LegendLabels legend;

    public MapView(List<MapNodeView> nodes, int viewportWidth, int viewportHeight) {
        this(nodes, viewportWidth, viewportHeight, null);
    }

    public MapView(
            List<MapNodeView> nodes, int viewportWidth, int viewportHeight, LegendLabels legend) {
        if (nodes == null || nodes.isEmpty()) {
            this.nodes = Collections.emptyList();
        } else {
            this.nodes = Collections.unmodifiableList(new ArrayList<MapNodeView>(nodes));
        }
        this.viewportWidth = Math.max(0, viewportWidth);
        this.viewportHeight = Math.max(0, viewportHeight);
        this.legend = legend != null ? legend : LegendLabels.empty();
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
