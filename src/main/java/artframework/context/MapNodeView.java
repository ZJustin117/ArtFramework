package artframework.context;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable map node presentation state from a Primary Backend frame. */
public final class MapNodeView {

    public final int row;
    public final int col;
    public final float x;
    public final float y;
    public final boolean taken;
    public final boolean highlighted;
    public final String symbol;
    public final String roomKind;
    public final String resourceId;

    public MapNodeView(
            int row,
            int col,
            float x,
            float y,
            boolean taken,
            boolean highlighted,
            String symbol,
            String roomKind,
            String resourceId) {
        this.row = row;
        this.col = col;
        this.x = x;
        this.y = y;
        this.taken = taken;
        this.highlighted = highlighted;
        this.symbol = symbol != null ? symbol : "";
        this.roomKind = roomKind != null ? roomKind : "";
        this.resourceId = resourceId != null ? resourceId : "";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("row", Integer.valueOf(row));
        m.put("col", Integer.valueOf(col));
        m.put("x", Float.valueOf(x));
        m.put("y", Float.valueOf(y));
        m.put("taken", Boolean.valueOf(taken));
        m.put("highlighted", Boolean.valueOf(highlighted));
        m.put("symbol", symbol);
        m.put("roomKind", roomKind);
        m.put("resourceId", resourceId);
        return m;
    }
}
