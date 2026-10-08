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
    /**
     * Native {@code MapRoomNode.scale} (M01 ring geometry): the live per-node animated scale.
     * Defaults to 1.0 when the backend cannot read it (fail-open source-compatible default).
     */
    public final float nodeScale;
    /**
     * Native {@code MapRoomNode.angle} (M01 ring rotation, degrees): the ring is rotated about the
     * node center by this value. Defaults to 0.0 when the backend cannot read it.
     */
    public final float angle;
    /**
     * M02: LIVE native {@code MapRoomNode.color} r/g/b/a. Native {@code MapRoomNode.render} draws the
     * node FILL with {@code taken ? AVAILABLE_COLOR : this.color}; {@code color} is a LIVE field whose
     * ALPHA oscillates on pickable AVAILABLE nodes (native {@code oscillateColor}:
     * {@code color.a = 0.66f + (cos(oscillateTimer)+1.0f)/6.0f}, range ~0.66..0.993) and is forced to
     * {@code AVAILABLE_COLOR} (alpha 1) on hover/current. {@link #liveColor} is false when the backend
     * could not read the field, in which case the draw path fails open to the constant tint derived
     * from {@code taken}/{@code available}.
     */
    public final float colorR;
    public final float colorG;
    public final float colorB;
    public final float colorA;
    /** M02: true when {@link #colorR}/{@link #colorG}/{@link #colorB}/{@link #colorA} are a live read. */
    public final boolean liveColor;

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
        this(row, col, x, y, taken, highlighted, reachable, pinned, available, current,
                width, height, symbol, roomKind, resourceId, 1f, 0f);
    }

    /**
     * M01 full constructor: adds the native per-node {@code nodeScale} (ring size factor) and
     * {@code angle} (ring rotation, degrees). Existing callers keep the shorter overload and fail
     * open to {@code nodeScale=1.0, angle=0.0} so {@link MapView#empty()} and legacy projections
     * stay valid.
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
            String resourceId,
            float nodeScale,
            float angle) {
        this(row, col, x, y, taken, highlighted, reachable, pinned, available, current,
                width, height, symbol, roomKind, resourceId, nodeScale, angle,
                0f, 0f, 0f, 1f, false);
    }

    /**
     * M02 full constructor: adds the LIVE native {@code MapRoomNode.color} rgba and a
     * {@code liveColor} flag. Existing callers keep the shorter overload and fail open to
     * {@code liveColor=false} so the draw path derives the constant tint from {@code taken}/
     * {@code available}; {@link MapView#empty()} and legacy projections stay valid.
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
            String resourceId,
            float nodeScale,
            float angle,
            float colorR,
            float colorG,
            float colorB,
            float colorA,
            boolean liveColor) {
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
        // Fail-open: a non-finite / non-positive scale must not collapse the ring.
        this.nodeScale = nodeScale > 0f && !Float.isNaN(nodeScale)
                && !Float.isInfinite(nodeScale) ? nodeScale : 1f;
        this.angle = Float.isNaN(angle) || Float.isInfinite(angle) ? 0f : angle;
        // M02: a non-finite live color channel is unreadable -> fail open (liveColor false).
        boolean colorFinite = isFinite(colorR) && isFinite(colorG) && isFinite(colorB)
                && isFinite(colorA);
        this.colorR = colorFinite ? colorR : 0f;
        this.colorG = colorFinite ? colorG : 0f;
        this.colorB = colorFinite ? colorB : 0f;
        this.colorA = colorFinite ? colorA : 1f;
        this.liveColor = liveColor && colorFinite;
    }

    private static boolean isFinite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
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
        m.put("nodeScale", Float.valueOf(nodeScale));
        m.put("angle", Float.valueOf(angle));
        // M02: the LIVE native node fill color (rgba) + whether it was a real read. The draw path uses
        // the derived constant tint when liveColor is false (fail-open).
        m.put("colorR", Float.valueOf(colorR));
        m.put("colorG", Float.valueOf(colorG));
        m.put("colorB", Float.valueOf(colorB));
        m.put("colorA", Float.valueOf(colorA));
        m.put("liveColor", Boolean.valueOf(liveColor));
        return m;
    }
}
