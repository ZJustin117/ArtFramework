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
    public final boolean reachable;
    public final boolean pinned;
    /**
     * Native node availability: the backend sets this when {@code MapRoomNode.color} is the native
     * {@code AVAILABLE_COLOR} (r/g/b 0.09/0.13/0.17) — i.e. the node is reachable/available and the
     * node texture must be tinted dark rather than white. Fail-open false when unresolvable.
     */
    public final boolean available;
    /**
     * Native current-node ring gate: {@code AbstractDungeon.firstRoomChosen} is true AND this node
     * is {@code AbstractDungeon.getCurrMapNode()} — the exact predicate native {@code MapRoomNode.render}
     * uses to draw the {@code MAP_CIRCLE_5} ring ({@code taken || (firstRoomChosen && current)}; the
     * {@code taken} branch is handled separately by the draw path).
     */
    public final boolean current;
    public final float width;
    public final float height;
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
        this(row, col, x, y, taken, highlighted, true, false,
                highlighted ? 80f : 64f, highlighted ? 80f : 64f,
                symbol, roomKind, resourceId);
    }

    public MapNodeView(
            int row,
            int col,
            float x,
            float y,
            boolean taken,
            boolean highlighted,
            boolean reachable,
            boolean pinned,
            float width,
            float height,
            String symbol,
            String roomKind,
            String resourceId) {
        this(row, col, x, y, taken, highlighted, reachable, pinned,
                false, false, width, height, symbol, roomKind, resourceId);
    }

    /**
     * Full constructor (D03 color fidelity): adds the native {@code available} tint gate and the
     * current-node ring gate. Existing callers keep the shorter overloads and fail open to
     * {@code available=false, current=false}.
     */
    public MapNodeView(
            int row,
            int col,
            float x,
            float y,
            boolean taken,
            boolean highlighted,
            boolean reachable,
            boolean pinned,
            boolean available,
            boolean current,
            float width,
            float height,
            String symbol,
            String roomKind,
            String resourceId) {
        this.row = row;
        this.col = col;
        this.x = x;
        this.y = y;
        this.taken = taken;
        this.highlighted = highlighted;
        this.reachable = reachable;
        this.pinned = pinned;
        this.available = available;
        this.current = current;
        this.width = width > 0f ? width : 64f;
        this.height = height > 0f ? height : 64f;
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
        m.put("reachable", Boolean.valueOf(reachable));
        m.put("pinned", Boolean.valueOf(pinned));
        m.put("available", Boolean.valueOf(available));
        m.put("current", Boolean.valueOf(current));
        m.put("width", Float.valueOf(width));
        m.put("height", Float.valueOf(height));
        m.put("symbol", symbol);
        m.put("roomKind", roomKind);
        m.put("resourceId", resourceId);
        return m;
    }
}
