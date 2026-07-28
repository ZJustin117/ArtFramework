package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable map surface snapshot for one present frame. */
public final class MapView {

    public final List<MapNodeView> nodes;
    public final int viewportWidth;
    public final int viewportHeight;

    public MapView(List<MapNodeView> nodes, int viewportWidth, int viewportHeight) {
        if (nodes == null || nodes.isEmpty()) {
            this.nodes = Collections.emptyList();
        } else {
            this.nodes = Collections.unmodifiableList(new ArrayList<MapNodeView>(nodes));
        }
        this.viewportWidth = Math.max(0, viewportWidth);
        this.viewportHeight = Math.max(0, viewportHeight);
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
