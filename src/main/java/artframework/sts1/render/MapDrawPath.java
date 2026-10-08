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

    /**
     * Native {@code MapRoomNode} tint constants (VERIFIED decompiled {@code MapRoomNode.java}
     * ~:37-40): {@code AVAILABLE_COLOR = (0.09,0.13,0.17,1)}, {@code NOT_TAKEN_COLOR =
     * (0.34,0.34,0.34,1)}, {@code OUTLINE_COLOR = Color.valueOf("8c8c80ff")} =
     * (0.549,0.549,0.502,1), highlight {@code (0.9,0.9,0.9,1)}. The pure draw path mirrors the
     * numbers rather than importing STS classes.
     */
    public static final float[] AVAILABLE_COLOR = {0.09f, 0.13f, 0.17f, 1f};
    public static final float[] NOT_TAKEN_COLOR = {0.34f, 0.34f, 0.34f, 1f};
    public static final float[] OUTLINE_COLOR = {0x8c / 255f, 0x8c / 255f, 0x80 / 255f, 1f};
    public static final float[] HIGHLIGHT_COLOR = {0.9f, 0.9f, 0.9f, 1f};

    /** Native node tint: {@code taken || available ? AVAILABLE_COLOR : NOT_TAKEN_COLOR}. */
    static float[] nodeColor(boolean taken, boolean available) {
        return taken || available ? AVAILABLE_COLOR : NOT_TAKEN_COLOR;
    }

    /** Native outline tint: {@code highlighted ? (0.9,0.9,0.9,1) : OUTLINE_COLOR}. */
    static float[] outlineColor(boolean highlighted) {
        return highlighted ? HIGHLIGHT_COLOR : OUTLINE_COLOR;
    }

    /**
     * Current-node / taken-node ring geometry (native {@code MAP_CIRCLE_5} draw). VERIFIED decompiled
     * {@code MapRoomNode.render}: {@code sb.draw(ImageMaster.MAP_CIRCLE_5, x*SPACING_X+OFFSET_X-96+
     * offsetX, y*Settings.MAP_DST_Y+OFFSET_Y+DungeonMapScreen.offsetY-96+offsetY, 96,96 origin,
     * 192,192 size, (this.scale*0.95f + 0.2f)*Settings.scale, (this.scale*0.95f + 0.2f)*Settings.scale,
     * this.angle, 0,0, 192,192, false, false)}. The ring is therefore centered on the node with final
     * size {@code 192 * (nodeScale*0.95 + 0.2) * Settings.scale} and rotated by the node's animated
     * {@code angle}. ART computes exactly this (the earlier {@code nodeBox * RING/NODE_TEXTURE} form
     * omitted the {@code nodeScale*0.95+0.2} factor and the rotation).
     */
    public static final float RING_SIZE = 192f;
    public static final float NODE_TEXTURE_SIZE = 128f;
    /** Native ring scale factor slope: {@code this.scale * 0.95f}. */
    public static final float RING_SCALE_SLOPE = 0.95f;
    /** Native ring scale factor offset: {@code + 0.2f}. */
    public static final float RING_SCALE_OFFSET = 0.2f;

    /**
     * Native {@code MAP_CIRCLE_5} ring size: {@code 192 * (nodeScale*0.95f + 0.2f) * Settings.scale},
     * then {@code * zoom} in ART present-space so the ring keeps tracking the (zoomed) node box.
     * Fail-open: a non-positive/NaN scale or zoom is treated as 1 so the ring never collapses.
     */
    public static float ringSize(float nodeScale, float scale, float zoom) {
        float ns = nodeScale > 0f && !Float.isNaN(nodeScale) && !Float.isInfinite(nodeScale)
                ? nodeScale : 1f;
        float sc = scale > 0f && !Float.isNaN(scale) && !Float.isInfinite(scale) ? scale : 1f;
        float z = zoom > 0f && !Float.isNaN(zoom) && !Float.isInfinite(zoom) ? zoom : 1f;
        return RING_SIZE * (ns * RING_SCALE_SLOPE + RING_SCALE_OFFSET) * sc * z;
    }

    public static final class DrawItem {
        public final int row;
        public final int col;
        public final float screenX;
        public final float screenY;
        public final boolean taken;
        public final boolean highlighted;
        public final boolean reachable;
        public final boolean pinned;
        public final boolean available;
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
        /**
         * Resolved native node tint (D03): {@code taken || available ? AVAILABLE_COLOR :
         * NOT_TAKEN_COLOR}, as r/g/b/a.
         */
        public final float nodeR;
        public final float nodeG;
        public final float nodeB;
        public final float nodeA;
        /**
         * Resolved native outline tint (D03): {@code highlighted ? (0.9,0.9,0.9,1) : OUTLINE_COLOR}
         * (native {@code OUTLINE_COLOR = Color.valueOf("8c8c80ff")}), as r/g/b/a.
         */
        public final float outlineR;
        public final float outlineG;
        public final float outlineB;
        public final float outlineA;
        /**
         * Native current-node flag (D03): {@code AbstractDungeon.firstRoomChosen &&
         * AbstractDungeon.getCurrMapNode() == node}. The renderer draws the {@code MAP_CIRCLE_5}
         * ring when {@code taken || currentNode} (native {@code MapRoomNode.render} predicate).
         */
        public final boolean currentNode;
        /**
         * M01: native {@code MapRoomNode.scale} (ring size factor, default 1.0 when unreadable).
         */
        public final float nodeScale;
        /** M01: native {@code MapRoomNode.angle} (ring rotation, degrees; 0 when unreadable). */
        public final float angle;
        /**
         * M02: true when the node fill rgba came from the LIVE native {@code MapRoomNode.color}
         * (rather than the constant fail-open derived from {@code taken}/{@code available}).
         */
        public final boolean nodeColorLive;

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
                boolean available,
                boolean current,
                Rect bounds) {
            this(row, col, screenX, screenY, taken, highlighted, reachable, pinned, symbol,
                    roomKind, resourceId, artSource, artFound, outlineResourceId,
                    highlightResourceId, outlineSource, highlightSource, outlineFound,
                    highlightFound, available, current, bounds, 1f, 0f);
        }

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
                boolean available,
                boolean current,
                Rect bounds,
                float nodeScale,
                float angle) {
            this(row, col, screenX, screenY, taken, highlighted, reachable, pinned, symbol,
                    roomKind, resourceId, artSource, artFound, outlineResourceId,
                    highlightResourceId, outlineSource, highlightSource, outlineFound,
                    highlightFound, available, current, bounds, nodeScale, angle,
                    available ? AVAILABLE_COLOR[0] : NOT_TAKEN_COLOR[0],
                    available ? AVAILABLE_COLOR[1] : NOT_TAKEN_COLOR[1],
                    available ? AVAILABLE_COLOR[2] : NOT_TAKEN_COLOR[2],
                    1f, false);
        }

        /**
         * M02 full constructor: adds the LIVE native {@code MapRoomNode.color} rgba (the per-node
         * fill tint, whose alpha oscillates on pickable AVAILABLE nodes) and a {@code liveColor} flag.
         * The fill tint is resolved natively: {@code taken ? AVAILABLE_COLOR : liveNodeColor}; the
         * fail-open path (flag false) derives the constant tint from {@code taken}/{@code available}.
         */
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
                boolean available,
                boolean current,
                Rect bounds,
                float nodeScale,
                float angle,
                float liveR,
                float liveG,
                float liveB,
                float liveA,
                boolean liveColor) {
            this.row = row;
            this.col = col;
            this.screenX = screenX;
            this.screenY = screenY;
            this.taken = taken;
            this.highlighted = highlighted;
            this.reachable = reachable;
            this.pinned = pinned;
            this.available = available;
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
            // Native MapRoomNode.render color resolution (D03 + M02):
            //   outline: highlighted ? (0.9,0.9,0.9,1) : OUTLINE_COLOR (8c8c80ff)
            //   node:    taken ? AVAILABLE_COLOR : this.color
            // `this.color` is LIVE: on pickable AVAILABLE nodes its ALPHA oscillates
            // (oscillateColor: 0.66..0.993) and hover/current force AVAILABLE_COLOR (alpha 1). M02
            // consumes the projected live rgba when readable; when it is NOT, the draw path fails
            // open to the constant tint derived from `taken`/`available` (AVAILABLE_COLOR when
            // available, else NOT_TAKEN_COLOR). `taken` ALWAYS wins with AVAILABLE_COLOR (native).
            this.nodeColorLive = liveColor && !taken;
            float nr;
            float ng;
            float nb;
            float na;
            if (taken) {
                nr = AVAILABLE_COLOR[0];
                ng = AVAILABLE_COLOR[1];
                nb = AVAILABLE_COLOR[2];
                na = AVAILABLE_COLOR[3];
            } else if (liveColor) {
                nr = liveR;
                ng = liveG;
                nb = liveB;
                na = liveA;
            } else {
                float[] fallback = available ? AVAILABLE_COLOR : NOT_TAKEN_COLOR;
                nr = fallback[0];
                ng = fallback[1];
                nb = fallback[2];
                na = fallback[3];
            }
            this.nodeR = nr;
            this.nodeG = ng;
            this.nodeB = nb;
            this.nodeA = na;
            float[] outline = outlineColor(highlighted);
            this.outlineR = outline[0];
            this.outlineG = outline[1];
            this.outlineB = outline[2];
            this.outlineA = outline[3];
            this.currentNode = current;
            // M01 fail-open ring geometry inputs.
            this.nodeScale = nodeScale > 0f && !Float.isNaN(nodeScale)
                    && !Float.isInfinite(nodeScale) ? nodeScale : 1f;
            this.angle = Float.isNaN(angle) || Float.isInfinite(angle) ? 0f : angle;
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
            m.put("available", Boolean.valueOf(available));
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
            Map<String, Object> nodeColor = new LinkedHashMap<String, Object>();
            nodeColor.put("r", Float.valueOf(nodeR));
            nodeColor.put("g", Float.valueOf(nodeG));
            nodeColor.put("b", Float.valueOf(nodeB));
            nodeColor.put("a", Float.valueOf(nodeA));
            m.put("color", nodeColor);
            Map<String, Object> outlineColor = new LinkedHashMap<String, Object>();
            outlineColor.put("r", Float.valueOf(outlineR));
            outlineColor.put("g", Float.valueOf(outlineG));
            outlineColor.put("b", Float.valueOf(outlineB));
            outlineColor.put("a", Float.valueOf(outlineA));
            m.put("outlineColor", outlineColor);
            m.put("currentNode", Boolean.valueOf(currentNode));
            m.put("nodeScale", Float.valueOf(nodeScale));
            m.put("angle", Float.valueOf(angle));
            // M02: true when the fill rgba came from the LIVE native node.color. The D1 scenario uses
            // this to prove the available-node alpha pulse reaches the draw (not forced to 1).
            m.put("nodeColorLive", Boolean.valueOf(nodeColorLive));
            m.put("colorHex", hex(nodeR, nodeG, nodeB, nodeA));
            m.put("outlineColorHex", hex(outlineR, outlineG, outlineB, outlineA));
            Map<String, Object> geometry = new LinkedHashMap<String, Object>();            geometry.put("x", Float.valueOf(bounds.x));
            geometry.put("y", Float.valueOf(bounds.y));
            geometry.put("width", Float.valueOf(bounds.width));
            geometry.put("height", Float.valueOf(bounds.height));
            m.put("bounds", geometry);
            return m;
        }
    }

    /**
     * One map-legend drawable (NRO-04 D03). The panel/title/icons mirror native
     * {@code Legend.render} + {@code LegendItem.render} at REST (non-hovered, desktop).
     */
    public static final class LegendDrawItem {
        public final String id;
        public final int index;
        public final String label;
        public final String resourceId;
        public final String role;
        public final float z;
        public final Rect bounds;
        /**
         * Resolved native c2 tint for the room-row ICON and LABEL (V04): native
         * {@code Legend.render} computes {@code c2 = (MapRoomNode.AVAILABLE_COLOR.r/g/b, c.a)} =
         * (0.09, 0.13, 0.17, legendAlpha), sets it once ({@code sb.setColor(c2)}) and passes it to
         * {@code LegendItem.render}. The panel and title keep the existing white/theme draws, so they
         * carry the neutral default (1,1,1,1).
         */
        public final float r;
        public final float g;
        public final float b;
        public final float a;
        /**
         * Native per-STATE icon scale (V04): a room row carries {@code legendIconScale(false,false,
         * scale) = scale/1.65f} at the desktop REST state ART draws. Panel/title (no icon) carry 0.
         */
        public final float iconScale;
        /** Native label LEFT edge x (V04): {@code TEXT_X*xScale - 50f*scale}; 0 for panel/title. */
        public final float labelX;
        /** Native label TOP y (V04): {@code rowY + 13f*yScale}; 0 for panel/title. */
        public final float labelTopY;
        /**
         * True for the 6 room rows (V04): the label is drawn LEFT-TOP aligned at
         * ({@link #labelX}, {@link #labelTopY}) — NOT centered on the icon box — matching native
         * {@code FontHelper.renderFontLeftTopAligned}.
         */
        public final boolean labelLeftAligned;

        public LegendDrawItem(String id, int index, String label, String resourceId, String role,
                float z, Rect bounds) {
            this(id, index, label, resourceId, role, z, bounds, 1f, 1f, 1f, 1f, 0f, 0f, 0f, false);
        }

        public LegendDrawItem(String id, int index, String label, String resourceId, String role,
                float z, Rect bounds, float r, float g, float b, float a, float iconScale,
                float labelX, float labelTopY, boolean labelLeftAligned) {
            this.id = id != null ? id : "";
            this.index = index;
            this.label = label != null ? label : "";
            this.resourceId = resourceId != null ? resourceId : "";
            this.role = role != null ? role : "";
            this.z = z;
            this.bounds = bounds != null ? bounds : Rect.ZERO;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.iconScale = iconScale;
            this.labelX = labelX;
            this.labelTopY = labelTopY;
            this.labelLeftAligned = labelLeftAligned;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("index", Integer.valueOf(index));
            m.put("label", label);
            m.put("resourceId", resourceId);
            m.put("x", Float.valueOf(bounds.x));
            m.put("y", Float.valueOf(bounds.y));
            m.put("w", Float.valueOf(bounds.width));
            m.put("h", Float.valueOf(bounds.height));
            Map<String, Object> color = new LinkedHashMap<String, Object>();
            color.put("r", Float.valueOf(r));
            color.put("g", Float.valueOf(g));
            color.put("b", Float.valueOf(b));
            color.put("a", Float.valueOf(a));
            m.put("color", color);
            m.put("colorHex", hex(r, g, b, a));
            m.put("iconScale", Float.valueOf(iconScale));
            m.put("labelX", Float.valueOf(labelX));
            m.put("labelTopY", Float.valueOf(labelTopY));
            m.put("labelLeftAligned", Boolean.valueOf(labelLeftAligned));
            return m;
        }
    }

    // Verified native constants (Legend / LegendItem bytecode). The trailing scale/yScale factors
    // are applied from the live Settings at build time so tests can pin the formula.
    private static final float LEGEND_X = 1670f;
    private static final float LEGEND_Y = 600f;
    private static final float LEGEND_PANEL_LW = 512f;
    private static final float LEGEND_PANEL_LH = 800f;
    private static final float LEGEND_ITEM_ICON_X = 1575f;
    private static final float LEGEND_ITEM_TEXT_X = 1670f;
    private static final float LEGEND_ITEM_SPACE_Y = 58f;   // desktop
    private static final float LEGEND_ITEM_OFFSET_Y = 100f; // desktop
    /**
     * Native {@code LegendItem.render} label offset from {@code TEXT_X} (V04):
     * {@code TEXT_X - 50f*Settings.scale} (desktop + mobile).
     */
    private static final float LEGEND_ITEM_LABEL_DX = 50f;
    /**
     * Native {@code LegendItem.render} label y offset below the item row (V04):
     * {@code rowY + 13f*Settings.yScale}.
     */
    private static final float LEGEND_ITEM_LABEL_DY = 13f;
    /**
     * Native desktop REST icon scale divisor (V04): {@code Settings.scale / 1.65f}. VERIFIED
     * decompiled {@code LegendItem.render}: {@code !mobile && !hb.hovered} branch. The other three
     * native divisors are the HOVER states ({@code desktop-hover /1.2f}, {@code mobile-hover /1f},
     * {@code mobile-rest /1.3f}), not per-row scales; ART renders the REST state.
     */
    private static final float LEGEND_ICON_REST_DIVISOR = 1.65f;
    /** Native desktop HOVER icon scale divisor (V04, documented gap: hover not yet replicated). */
    private static final float LEGEND_ICON_HOVER_DIVISOR = 1.2f;

    private static final String[] LEGEND_ROOM_IDS = {
        "event", "merchant", "treasure", "rest", "enemy", "elite"
    };
    /**
     * English fallback labels (native {@code Legend} default locale). The projection may carry the
     * live localized {@code Legend} UIStrings; when it does, those labels win (see
     * {@link #legendLabels()} / {@link #legendTitle()}).
     */
    private static final String[] LEGEND_ROOM_LABELS = {
        "Event", "Merchant", "Treasure", "Rest", "Enemy", "Elite"
    };
    private static final String LEGEND_TITLE = "Legend";

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
                    outline.found, highlight.found, n.available, n.current, bounds,
                    n.nodeScale, n.angle,
                    n.colorR, n.colorG, n.colorB, n.colorA, n.liveColor));
        }
        return out;
    }

    /**
     * Live {@code Settings} scale factors {@code {scale, xScale, yScale}}, or {@code null} when the
     * host {@code Settings} type is unavailable or not yet initialized (all reads fail-open).
     */
    private static float[] legendSettings() {
        try {
            float scale = com.megacrit.cardcrawl.core.Settings.scale;
            float xScale = com.megacrit.cardcrawl.core.Settings.xScale;
            float yScale = com.megacrit.cardcrawl.core.Settings.yScale;
            if (scale > 0f && xScale > 0f && yScale > 0f) {
                return new float[] {scale, xScale, yScale};
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String legendIconResource(String room) {
        if ("event".equals(room)) return ResourceIds.MAP_NODE_EVENT;
        if ("merchant".equals(room)) return ResourceIds.MAP_NODE_SHOP;
        if ("treasure".equals(room)) return ResourceIds.MAP_NODE_TREASURE;
        if ("rest".equals(room)) return ResourceIds.MAP_NODE_REST;
        if ("enemy".equals(room)) return ResourceIds.MAP_NODE_MONSTER;
        if ("elite".equals(room)) return ResourceIds.MAP_NODE_ELITE;
        return "";
    }

    /**
     * Projected localized legend value or {@code null} when the map projection carries none
     * (fail-open). The draw path never imports STS classes; the backend fills
     * {@link MapView#legend} from the native {@code Legend} UIStrings.
     */
    private static MapView.LegendLabels projectedLegend() {
        try {
            MapView mv = ArtFramework.projection().map();
            if (mv == null || mv.legend == null || mv.legend.isEmpty()) {
                return null;
            }
            return mv.legend;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Resolved legend text (NRO-04 D03 follow-up, all-or-nothing). Native {@code Legend} always
     * supplies a complete title + 6 labels, so ART consumes the localized set only when the
     * projection carries a non-blank title AND all 6 labels non-blank; any incomplete/blank value
     * falls the WHOLE legend back to the English defaults, so a partial/malformed localization can
     * never produce a mixed-language legend. Returns {@code null} when the projection is
     * unavailable or incomplete (callers use the English defaults).
     */
    private static ResolvedLegend resolvedLegend() {
        MapView.LegendLabels legend = projectedLegend();
        if (legend == null) {
            return null;
        }
        String title = legend.title;
        if (title == null || title.trim().isEmpty() || legend.size() < LEGEND_ROOM_LABELS.length) {
            return null;
        }
        List<String> labels = new ArrayList<String>(LEGEND_ROOM_LABELS.length);
        for (int i = 0; i < LEGEND_ROOM_LABELS.length; i++) {
            String localized = legend.label(i);
            if (localized == null || localized.trim().isEmpty()) {
                return null;
            }
            labels.add(localized);
        }
        return new ResolvedLegend(title, labels);
    }

    /** A complete resolved legend (localized set when all 7 values are present, else null). */
    private static final class ResolvedLegend {
        final String title;
        final List<String> labels;

        ResolvedLegend(String title, List<String> labels) {
            this.title = title;
            this.labels = labels;
        }
    }

    /** Resolved legend panel title: the localized value when the whole set is present, else English. */
    public static String legendTitle() {
        ResolvedLegend resolved = resolvedLegend();
        return resolved != null ? resolved.title : LEGEND_TITLE;
    }

    /**
     * Resolved room label at {@code index} (0..5, native {@code TEXT[0/3/6/9/12/15]} order). The
     * full localized set is used only when complete; otherwise the English defaults are used for all
     * indices (no mixed languages). Index-guarded and fail-open.
     */
    public static String legendLabel(int index) {
        if (index < 0 || index >= LEGEND_ROOM_LABELS.length) {
            return "";
        }
        ResolvedLegend resolved = resolvedLegend();
        return resolved != null ? resolved.labels.get(index) : LEGEND_ROOM_LABELS[index];
    }

    /** Resolved room labels in native legend order (complete localized set or English defaults). */
    public static List<String> legendLabels() {
        ResolvedLegend resolved = resolvedLegend();
        if (resolved != null) {
            return new ArrayList<String>(resolved.labels);
        }
        List<String> out = new ArrayList<String>(LEGEND_ROOM_LABELS.length);
        for (int i = 0; i < LEGEND_ROOM_LABELS.length; i++) {
            out.add(LEGEND_ROOM_LABELS[i]);
        }
        return out;
    }

    /**
     * Native {@code LegendItem.render} c2 tint (V04): the legend reuses
     * {@code MapRoomNode.AVAILABLE_COLOR} rgb with the legend panel's live alpha
     * ({@code Legend.c.a}, which fades to 1 at rest). ART draws the settled rest state, and the
     * projection carries no legend alpha, so the alpha is the documented rest value 1.0.
     */
    public static float[] legendTint() {
        return new float[] {
            AVAILABLE_COLOR[0], AVAILABLE_COLOR[1], AVAILABLE_COLOR[2], 1f
        };
    }

    /**
     * Native per-STATE icon scale divisor (V04), VERIFIED decompiled {@code LegendItem.render}:
     * <ul>
     *   <li>desktop rest: {@code Settings.scale / 1.65f}</li>
     *   <li>desktop hover: {@code Settings.scale / 1.2f}</li>
     *   <li>mobile rest: {@code Settings.scale / 1.3f}</li>
     *   <li>mobile hover: {@code Settings.scale}</li>
     * </ul>
     * ART renders the desktop rest state, so {@link #legendIconScale(float)} returns {@code
     * scale/1.65f}. The four native values are HOVER/PLATFORM states, NOT six per-row scales (the
     * earlier V04 brief's "per-row branches" read is corrected here against the bytecode).
     */
    public static float legendIconScale(float scale) {
        return scale / LEGEND_ICON_REST_DIVISOR;
    }

    /** Native desktop HOVER icon scale ({@code scale/1.2f}); icon bounds helper for future hover. */
    public static float legendIconScaleHover(float scale) {
        return scale / LEGEND_ICON_HOVER_DIVISOR;
    }

    /** Native label LEFT x (V04): {@code TEXT_X*xScale - 50f*scale}. */
    public static float legendLabelX(float xScale, float scale) {
        return LEGEND_ITEM_TEXT_X * xScale - LEGEND_ITEM_LABEL_DX * scale;
    }

    /** Native label TOP y (V04): {@code Y*yScale - SPACE_Y*yScale*index + OFFSET_Y*yScale + 13f*yScale}. */
    public static float legendLabelTopY(float yScale, int index) {
        return LEGEND_Y * yScale - (LEGEND_ITEM_SPACE_Y * yScale) * index
                + (LEGEND_ITEM_OFFSET_Y * yScale) + LEGEND_ITEM_LABEL_DY * yScale;
    }

    /**
     * NRO-04 D03 map legend items: native {@code Legend.render} panel + title and the 6
     * {@code LegendItem.render} room-type icon rows, at REST (non-hovered, desktop). Returns an
     * empty list when {@code Settings} is unavailable so the renderer never invents pixels.
     *
     * <p>Verified native geometry (V04, decompiled {@code Legend.java} / {@code LegendItem.java}):
     * panel {@code sb.draw(MAP_LEGEND, X-256f, Y-400f, 256f, 400f, 512f, 800f, Settings.scale,
     * Settings.yScale, 0f, ...)} with {@code X = 1670f*Settings.xScale} and
     * {@code Y = 600f*Settings.yScale}; icon {@code sb.draw(img, ICON_X-64f, Y - SPACE_Y*index +
     * OFFSET_Y - 64f, 64f, 64f, 128f, 128f, Settings.scale/1.65f, Settings.scale/1.65f, ...)} with
     * {@code ICON_X = 1575f*Settings.xScale}, {@code SPACE_Y = 58f*Settings.yScale} and
     * {@code OFFSET_Y = 100f*Settings.yScale} (desktop). Both the icon and the label are tinted with
     * {@code c2 = (AVAILABLE_COLOR.rgb, Legend.c.a)}; the LABEL is left-top aligned at
     * {@code TEXT_X - 50f*scale}, {@code rowY + 13f*yScale}, NOT centered on the icon box.
     *
     * <p><b>Documented gaps.</b> Hover icon scale ({@code /1.2}), hover tip, the controller reticle,
     * and the legend alpha fade-in ({@code Legend.c.a} lerp) are NOT replicated; this path draws the
     * desktop REST state at settled full alpha (=1).
     */
    public static List<LegendDrawItem> legendItems() {
        List<LegendDrawItem> out = new ArrayList<LegendDrawItem>();
        float[] s = legendSettings();
        if (s == null) {
            return out;
        }
        float scale = s[0];
        float xScale = s[1];
        float yScale = s[2];
        float x = LEGEND_X * xScale;
        float y = LEGEND_Y * yScale;
        float[] tint = legendTint();
        out.add(new LegendDrawItem("legend.panel", -1, "", ResourceIds.UI_MAP_LEGEND,
                "map-legend", 0.1f,
                new Rect(x - 256f, y - 400f,
                        LEGEND_PANEL_LW * scale, LEGEND_PANEL_LH * yScale)));
        // Native title is a centered font draw with no rectangle; this box is the projected
        // C2 text carrier centered on (X, Y + 170f*yScale) where the text X is LegendItem.TEXT_X
        // (documented approximation).
        float textX = LEGEND_ITEM_TEXT_X * xScale;
        out.add(new LegendDrawItem("legend.title", -1, legendTitle(), "",
                "map-legend", 0.2f,
                new Rect(textX - 160f * scale, y + 170f * yScale - 20f * yScale,
                        320f * scale, 40f * yScale)));
        float icon = 128f * legendIconScale(scale);
        for (int i = 0; i < LEGEND_ROOM_IDS.length && i < LEGEND_ROOM_LABELS.length; i++) {
            String room = LEGEND_ROOM_IDS[i];
            Rect bounds = new Rect(LEGEND_ITEM_ICON_X * xScale - 64f,
                    y - (LEGEND_ITEM_SPACE_Y * yScale) * i + (LEGEND_ITEM_OFFSET_Y * yScale) - 64f,
                    icon, icon);
            out.add(new LegendDrawItem("legend:" + room, i, legendLabel(i),
                    legendIconResource(room), "map-legend", 0.15f, bounds,
                    tint[0], tint[1], tint[2], tint[3], icon,
                    legendLabelX(xScale, scale), legendLabelTopY(yScale, i), true));
        }
        return out;
    }

    /**
     * One native map parchment background drawable (D03 map background follow-up). The rects mirror
     * native {@code DungeonMap.renderNormalMap} / {@code renderMapCenters} / {@code renderMapBlender}
     * non-mobile draws ({@code mapTop}, {@code mapMid}, {@code mapBot}, then {@code mapBlend}×2), all
     * tinted with the live {@code baseMapColor} alpha (white RGB).
     */
    public static final class BackgroundDrawItem {
        public final String id;
        public final String resourceId;
        public final Rect bounds;
        public final float alpha;

        public BackgroundDrawItem(String id, String resourceId, Rect bounds, float alpha) {
            this.id = id != null ? id : "";
            this.resourceId = resourceId != null ? resourceId : "";
            this.bounds = bounds != null ? bounds : Rect.ZERO;
            this.alpha = alpha;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("resourceId", resourceId);
            m.put("x", Float.valueOf(bounds.x));
            m.put("y", Float.valueOf(bounds.y));
            m.put("w", Float.valueOf(bounds.width));
            m.put("h", Float.valueOf(bounds.height));
            m.put("alpha", Float.valueOf(alpha));
            return m;
        }
    }

    /**
     * Native map parchment background draws in native ORDER (D03 map background follow-up). Native
     * {@code DungeonMap.renderNormalMap} (VERIFIED desktop-1.0.jar bytecode, non-mobile branch):
     * {@code top} (after {@code renderMapCenters} draws {@code mid}), then {@code bot}, then
     * {@code renderMapBlender} draws {@code blend}×2; all five use {@code baseMapColor} (white RGB +
     * the live fade alpha). Rects are the raw native {@code SpriteBatch.draw(tex, x, y, w, h)} values,
     * derived from the projected {@link MapView.MapBackground}:
     * <ul>
     *   <li>top:   {@code (0, h + offsetY + mapOffsetY, width, 1080*scale)}</li>
     *   <li>mid:   {@code (0, offsetY + mapOffsetY, width, 1080*scale)}</li>
     *   <li>bot:   {@code (0, -mapMidDist + offsetY + mapOffsetY + 1, width, 1080*scale)}</li>
     *   <li>blend A: {@code (0, offsetY + mapOffsetY + 800*scale, width, blendH)}</li>
     *   <li>blend B: {@code (0, offsetY + mapOffsetY - 220*scale, width, blendH)}</li>
     * </ul>
     * For the final act ({@link MapView.MapBackground#finalAct}) native takes {@code renderFinalActMap},
     * which draws ONLY {@code top} then {@code bot} — no {@code mid} and no {@code blend} strips (its
     * {@code renderMapBlender} {@code "TheEnding"} guard skips them); this method emits just those two
     * (same rects/formulas), preserving native order. Fail-open: an empty list when the projection
     * carries no background (native fields unavailable), so the map supply is unchanged and the
     * renderer never invents pixels.
     */
    public static List<BackgroundDrawItem> backgroundItems() {
        List<BackgroundDrawItem> out = new ArrayList<BackgroundDrawItem>();
        MapView.MapBackground bg = projectedBackground();
        if (bg == null) {
            return out;
        }
        float width = bg.width;
        float h1080 = 1080f * bg.scale;
        float base = bg.offsetY + bg.mapOffsetY;
        out.add(new BackgroundDrawItem("map.bg.top", ResourceIds.MAP_BG_TOP,
                new Rect(0f, bg.h + base, width, h1080), bg.alpha));
        if (bg.finalAct) {
            // Native renderFinalActMap (non-mobile): top, then bot — nothing else.
            out.add(new BackgroundDrawItem("map.bg.bot", ResourceIds.MAP_BG_BOT,
                    new Rect(0f, -bg.mapMidDist + base + 1f, width, h1080), bg.alpha));
            return out;
        }
        out.add(new BackgroundDrawItem("map.bg.mid", ResourceIds.MAP_BG_MID,
                new Rect(0f, base, width, h1080), bg.alpha));
        out.add(new BackgroundDrawItem("map.bg.bot", ResourceIds.MAP_BG_BOT,
                new Rect(0f, -bg.mapMidDist + base + 1f, width, h1080), bg.alpha));
        out.add(new BackgroundDrawItem("map.bg.blend.a", ResourceIds.MAP_BG_BLEND,
                new Rect(0f, base + 800f * bg.scale, width, bg.blendH), bg.alpha));
        out.add(new BackgroundDrawItem("map.bg.blend.b", ResourceIds.MAP_BG_BLEND,
                new Rect(0f, base - 220f * bg.scale, width, bg.blendH), bg.alpha));
        return out;
    }

    /** Projected native map background, or {@code null} when absent (fail-open). */
    private static MapView.MapBackground projectedBackground() {
        try {
            MapView mv = ArtFramework.projection().map();
            return mv != null ? mv.background : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Projected map view, or {@code null} when unavailable (fail-open). */
    private static MapView projectedMap() {
        try {
            return ArtFramework.projection().map();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Native {@code MapDot.render} texture half-size (16px texture, origin 8,8). */
    public static final float DOT_HALF = 8f;
    /** Native {@code MapDot.render} texture size ({@code images/ui/map/dot1.png} = 16px). */
    public static final float DOT_TEXTURE_SIZE = 16f;
    /** Native {@code MapDot.OFFSET_Y} = {@code 172f * Settings.scale}. */
    public static final float DOT_OFFSET_Y = 172f;

    /**
     * One native map-edge dot draw (D03 map edges). The native {@code MapDot} dot jitter is baked into
     * the stored x/y at edge-construction time, so {@link #edgeItems()} reads the PROJECTED stored
     * values rather than recomputing. The rect is the final SCREEN box; the rotation is the stored
     * dot rotation; the tint is the owning edge's color.
     */
    public static final class EdgeDrawItem {
        public final String id;
        public final String resourceId;
        public final Rect bounds;
        /** Native stored {@code MapDot.rotation} (degrees). */
        public final float rotationDegrees;
        public final float r;
        public final float g;
        public final float b;
        public final float a;

        public EdgeDrawItem(String id, String resourceId, Rect bounds, float rotationDegrees,
                float r, float g, float b, float a) {
            this.id = id != null ? id : "";
            this.resourceId = resourceId != null ? resourceId : "";
            this.bounds = bounds != null ? bounds : Rect.ZERO;
            this.rotationDegrees = rotationDegrees;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("resourceId", resourceId);
            m.put("x", Float.valueOf(bounds.x));
            m.put("y", Float.valueOf(bounds.y));
            m.put("w", Float.valueOf(bounds.width));
            m.put("h", Float.valueOf(bounds.height));
            m.put("rotation", Float.valueOf(rotationDegrees));
            Map<String, Object> color = new LinkedHashMap<String, Object>();
            color.put("r", Float.valueOf(r));
            color.put("g", Float.valueOf(g));
            color.put("b", Float.valueOf(b));
            color.put("a", Float.valueOf(a));
            m.put("color", color);
            m.put("colorHex", hex(r, g, b, a));
            return m;
        }
    }

    /**
     * Native map connection dot draws (D03 map edges) in stable order: each projected edge in
     * projection order, then each of its stored dots in native order. Native {@code MapDot.render}
     * draws {@code images/ui/map/dot1.png} (16px) at {@code dot.x - 8f, dot.y - 8f + offsetY +
     * 172f*scale}, size {@code 16f*scale}, tinted by the edge color with the stored dot rotation.
     * The final screen rect is therefore {@code x = dot.x - 8*scale}, {@code y = dot.y - 8*scale +
     * offsetY + 172*scale}, {@code size = 16*scale}, where {@code offsetY} is the LIVE
     * {@link MapView.MapBackground#offsetY} and {@code scale} the live {@code Settings.scale}.
     * Fail-open: an empty list when the projection carries no background/edges (never invents pixels).
     */
    public static List<EdgeDrawItem> edgeItems() {
        List<EdgeDrawItem> out = new ArrayList<EdgeDrawItem>();
        MapView mv = projectedMap();
        if (mv == null) {
            return out;
        }
        MapView.MapBackground bg = mv.background;
        if (bg == null) {
            return out;
        }
        float scale = bg.scale;
        if (!(scale > 0f)) {
            return out;
        }
        float size = DOT_TEXTURE_SIZE * scale;
        int edgeIndex = 0;
        for (MapView.MapEdgeView edge : mv.edges) {
            if (edge == null) {
                edgeIndex++;
                continue;
            }
            int dotIndex = 0;
            for (MapView.MapDotView dot : edge.dots) {
                if (dot == null) {
                    dotIndex++;
                    continue;
                }
                float x = dot.x - DOT_HALF * scale;
                float y = dot.y - DOT_HALF * scale + bg.offsetY + DOT_OFFSET_Y * scale;
                out.add(new EdgeDrawItem(
                        "map.edge." + edgeIndex + "." + dotIndex,
                        ResourceIds.MAP_EDGE_DOT,
                        new Rect(x, y, size, size),
                        dot.rotation,
                        edge.r, edge.g, edge.b, edge.a));
                dotIndex++;
            }
            edgeIndex++;
        }
        return out;
    }

    /**
     * Full native map paint order exactly as the renderer submits it (D03 layering fix). The opaque
     * parchment background is the BOTTOM layer — the renderer draws it in
     * {@code Sts1SurfaceRenderer.render} immediately BEFORE the C2 band, then {@code renderMap} draws
     * the legend and the node band on top. This method returns the ordered layer keys:
     * {@code bg:<id>} first, then {@code legend:<id>}, then the node band in the corrected native
     * per-node submission order: {@code outline:r:c} (when {@code reachable || highlighted}) BEFORE
     * {@code node:r:c}, then {@code overlay:r:c}, then {@code ring:r:c} — matching
     * {@link #mapSubmissionPlan()} and native {@code MapRoomNode.render}, which draws the outline
     * before the fill. Pure: derived
     * from {@link #backgroundItems()}, {@link #legendItems()} and the node projection, so a test can
     * prove the background sits strictly below the legend/nodes and the outline sits below the fill.
     */
    public static List<String> paintOrder() {
        List<String> out = new ArrayList<String>();
        for (BackgroundDrawItem bg : backgroundItems()) {
            out.add("bg:" + bg.id);
        }
        // D03 map edges: native MapEdge.render draws the connection dots at the START of each node's
        // render, so a global "all edges before all nodes" band is the correct approximation. The
        // edges paint over the background but under the legend/node band (the parenthesised native
        // per-node ordering is collapsed into this single band).
        for (EdgeDrawItem edge : edgeItems()) {
            out.add("edge:" + edge.id);
        }
        for (LegendDrawItem legend : legendItems()) {
            out.add("legend:" + legend.id);
        }
        for (DrawItem item : buildFromProjection()) {
            String key = item.row + ":" + item.col;
            // Native MapRoomNode.render draws the OUTLINE before the FILL (both at the same node
            // box), so the fill covers the outline center — mirror that order here (D03 wash fix).
            if (item.reachable || item.highlighted) {
                out.add("outline:" + key);
            }
            out.add("node:" + key);
            if (item.pinned || item.highlighted) {
                out.add("overlay:" + key);
            }
            if (item.taken || item.currentNode) {
                out.add("ring:" + key);
            }
        }
        return out;
    }

    /**
     * Ordered pure submission plan for the map surface (NRO-04 D03 defect fix): exactly what
     * {@code Sts1SurfaceRenderer.renderMap} draws, in native paint order (legend first, then the
     * node band on top: outline, then node icon, then overlay; native {@code MapRoomNode.render}
     * draws the outline before the fill). Each entry is a
     * {@code (resourceId, bounds, label)} triple.
     *
     * <p>Resource selection mirrors the projector: {@link DrawItem#resourceId} is the logical id
     * (catalog-mapped by HostAssets); {@link DrawItem#artSource} is the resolved source. The
     * plan keeps the logical id because {@code drawResolvedTexture} resolves via
     * {@code ArtFramework.assets().resolve(resourceId)} — the map node ids ARE vanilla-catalog
     * registered, so {@code resolve(resourceId)} returns found and no source substitution is
     * needed. The plan is pure (no SpriteBatch/GL) so submission order/content and fail-open
     * skipping are unit-testable.
     */
    public static final class Submission {
        public final String resourceId;
        public final Rect bounds;
        public final String label;
        /** Native paint role: {@code legend|node|outline|overlay|ring|plain}. */
        public final String role;
        public final float r;
        public final float g;
        public final float b;
        public final float a;
        /**
         * V04: true when the label is drawn LEFT-TOP aligned at ({@code labelX}, {@code labelTopY})
         * (the 6 native {@code LegendItem} rows) instead of centered in {@link #bounds}. Panel/title
         * and node submissions keep the centered draw (false).
         */
        public final boolean labelLeftAligned;
        /** V04: native label LEFT x for {@link #labelLeftAligned} submits; 0 otherwise. */
        public final float labelX;
        /** V04: native label TOP y for {@link #labelLeftAligned} submits; 0 otherwise. */
        public final float labelTopY;
        /**
         * M01: rotation in DEGREES about the box center (native {@code MapRoomNode.angle} for the
         * {@code MAP_CIRCLE_5} ring; 0 for every other role). The renderer uses the rotation-aware
         * {@code drawResolvedTexture} overload when this is non-zero.
         */
        public final float rotationDegrees;

        public Submission(String resourceId, Rect bounds, String label) {
            this(resourceId, bounds, label, "plain", 1f, 1f, 1f, 1f, false, 0f, 0f, 0f);
        }

        public Submission(String resourceId, Rect bounds, String label, String role,
                float r, float g, float b, float a) {
            this(resourceId, bounds, label, role, r, g, b, a, false, 0f, 0f, 0f);
        }

        public Submission(String resourceId, Rect bounds, String label, String role,
                float r, float g, float b, float a, boolean labelLeftAligned,
                float labelX, float labelTopY) {
            this(resourceId, bounds, label, role, r, g, b, a, labelLeftAligned, labelX, labelTopY,
                    0f);
        }

        /** M01 full constructor: adds the ring rotation (degrees about the box center). */
        public Submission(String resourceId, Rect bounds, String label, String role,
                float r, float g, float b, float a, boolean labelLeftAligned,
                float labelX, float labelTopY, float rotationDegrees) {
            this.resourceId = resourceId != null ? resourceId : "";
            this.bounds = bounds;
            this.label = label != null ? label : "";
            this.role = role != null ? role : "plain";
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.labelLeftAligned = labelLeftAligned;
            this.labelX = labelX;
            this.labelTopY = labelTopY;
            this.rotationDegrees = Float.isNaN(rotationDegrees) || Float.isInfinite(rotationDegrees)
                    ? 0f : rotationDegrees;
        }
    }

    /**
     * Builds the ordered map submission plan. Legend panel/title/icon entries are emitted FIRST,
     * then the node entries (outline when {@code reachable || highlighted} FIRST, then the node
     * icon, then overlay when {@code pinned || highlighted}) — matching native
     * {@code DungeonMap.render}, which draws {@code Legend.render} before {@code DungeonMapScreen.render}
     * draws the nodes, so nodes paint OVER the legend. Node/legend internal order is preserved.
     *
     * <p>D03 wash fix: native {@code MapRoomNode.render} draws the OUTLINE
     * ({@code this.room.getMapImgOutline()}) BEFORE the node FILL ({@code this.room.getMapImg()}) at the
     * SAME node box, so the fill covers the outline's center and only the outline edges remain visible.
     * The plan submits the outline FIRST for the same reason (D03 A/B: node read ~0.53 grey outline
     * when the fill was painted first). Entries with neither a resource id nor a label are omitted so
     * the renderer never submits a no-op; the legend title is label-only (no native texture).
     */
    public static List<Submission> mapSubmissionPlan() {
        List<Submission> out = new ArrayList<Submission>();
        for (LegendDrawItem legend : legendItems()) {
            if (legend.bounds == null || legend.bounds.width <= 0f || legend.bounds.height <= 0f) {
                continue;
            }
            // V04: the 6 room rows carry the native c2 (AVAILABLE_COLOR) tint + left-top label
            // placement; the panel/title keep the neutral white/centered draw.
            if (legend.labelLeftAligned) {
                addSubmission(out, legend.resourceId, legend.bounds, legend.label, legend.role,
                        legend.r, legend.g, legend.b, legend.a, true,
                        legend.labelX, legend.labelTopY);
            } else {
                addSubmission(out, legend.resourceId, legend.bounds, legend.label);
            }
        }
        for (DrawItem item : buildFromProjection()) {
            if (item.bounds == null || item.bounds.width <= 0f || item.bounds.height <= 0f) {
                continue;
            }
            // Native order (MapRoomNode.render ~:370 vs ~:383): outline FIRST, then node fill, both
            // at item.bounds, so the fill covers the outline center (leaving only the outline edges).
            if (item.reachable || item.highlighted) {
                addSubmission(out, item.outlineResourceId, item.bounds, "", "outline",
                        item.outlineR, item.outlineG, item.outlineB, item.outlineA);
            }
            addSubmission(out, item.resourceId, item.bounds, "", "node",
                    item.nodeR, item.nodeG, item.nodeB, item.nodeA);
            if (item.pinned || item.highlighted) {
                addSubmission(out, item.highlightResourceId, item.bounds, "", "overlay",
                        1f, 1f, 1f, 1f);
            }
            if (item.taken || item.currentNode) {
                // Native MapRoomNode.render (VERIFIED ~:387-393): MAP_CIRCLE_5 ring at
                // AVAILABLE_COLOR when `taken || (firstRoomChosen && curr)`, centered on the node,
                // final size `192 * (this.scale*0.95f + 0.2f) * Settings.scale`, rotated by
                // `this.angle`. This is computed DIRECTLY from the native formula (the projected node
                // box is a layout box, not the native 128*scale texture, so it cannot carry the
                // factor); `zoom` is applied on top so the ring keeps tracking a zoomed node. Fail-open
                // to scale 1 when Settings is unavailable.
                float scale = liveSettingsScale();
                float ringSize = ringSize(item.nodeScale, scale, PAN.zoom());
                Rect ring = new Rect(item.screenX - ringSize * 0.5f,
                        item.screenY - ringSize * 0.5f, ringSize, ringSize);
                addSubmission(out, ResourceIds.UI_MAP_CIRCLE_5, ring, "", "ring",
                        AVAILABLE_COLOR[0], AVAILABLE_COLOR[1], AVAILABLE_COLOR[2],
                        AVAILABLE_COLOR[3], false, 0f, 0f, item.angle);
            }
        }
        return out;
    }

    private static void addSubmission(
            List<Submission> out, String resourceId, Rect bounds, String label) {
        addSubmission(out, resourceId, bounds, label, "plain", 1f, 1f, 1f, 1f);
    }

    private static void addSubmission(
            List<Submission> out,
            String resourceId,
            Rect bounds,
            String label,
            String role,
            float r,
            float g,
            float b,
            float a) {
        addSubmission(out, resourceId, bounds, label, role, r, g, b, a, false, 0f, 0f);
    }

    private static void addSubmission(
            List<Submission> out,
            String resourceId,
            Rect bounds,
            String label,
            String role,
            float r,
            float g,
            float b,
            float a,
            boolean labelLeftAligned,
            float labelX,
            float labelTopY) {
        addSubmission(out, resourceId, bounds, label, role, r, g, b, a, labelLeftAligned, labelX,
                labelTopY, 0f);
    }

    private static void addSubmission(
            List<Submission> out,
            String resourceId,
            Rect bounds,
            String label,
            String role,
            float r,
            float g,
            float b,
            float a,
            boolean labelLeftAligned,
            float labelX,
            float labelTopY,
            float rotationDegrees) {
        boolean hasResource = resourceId != null && !resourceId.isEmpty();
        boolean hasLabel = label != null && !label.isEmpty();
        if (!hasResource && !hasLabel) {
            return;
        }
        out.add(new Submission(resourceId, bounds, label, role, r, g, b, a,
                labelLeftAligned, labelX, labelTopY, rotationDegrees));
    }

    /**
     * Live native {@code Settings.scale} for the ring size factor, or {@code 1f} when the host
     * {@code Settings} type is unavailable/uninitialized (fail-open; never throws).
     */
    static float liveSettingsScale() {
        try {
            float scale = com.megacrit.cardcrawl.core.Settings.scale;
            if (scale > 0f && !Float.isNaN(scale) && !Float.isInfinite(scale)) {
                return scale;
            }
        } catch (Throwable ignored) {
        }
        return 1f;
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

    /** Returns an 8-hex-digit RGBA string (device-probe friendly). */
    static String hex(float r, float g, float b, float a) {
        return String.format("%02x%02x%02x%02x", channel(r), channel(g), channel(b), channel(a));
    }

    static String hex(float[] rgba) {
        return hex(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    private static int channel(float v) {
        int i = Math.round(Math.max(0f, Math.min(1f, v)) * 255f);
        return i;
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
        // M01 gate evidence: how many projected nodes are the native current node / taken. The ring
        // gate is `taken || currentNode`; the D1 scenario asserts currentNodeCount >= 1 after a
        // `firstRoomChosen && getCurrMapNode()` state, and ringCount >= currentNodeCount.
        int currentNodeCount = 0;
        int takenCount = 0;
        for (DrawItem d : items) {
            if (d.currentNode) currentNodeCount++;
            if (d.taken) takenCount++;
        }
        m.put("currentNodeCount", Integer.valueOf(currentNodeCount));
        m.put("takenCount", Integer.valueOf(takenCount));
        // M02: the LIVE native node fill alpha pulse. Native `oscillateColor()` sets a pickable
        // AVAILABLE node's `color.a = 0.66f + (cos(oscillateTimer)+1.0f)/6.0f` (range ~0.66..0.993,
        // never 1.0). The aggregate below covers only the PULSING (non-highlighted) available nodes:
        // it EXCLUDES taken, current, AND highlighted nodes, because native `MapRoomNode.update()`
        // forces a HOVERED available node to `color = AVAILABLE_COLOR` (alpha 1) — ART's
        // `d.highlighted` ~ native hovered — so a highlighted node would legitimately carry alpha 1
        // and must not fall into the pulse range. `...Min`/`...Max` therefore bound the pulse, while
        // `currentNodeColorAlpha` (AVAILABLE alpha 1) sits OUTSIDE that set.
        List<Map<String, Object>> availableColorAlphas = new ArrayList<Map<String, Object>>();
        float alphaMin = Float.NaN;
        float alphaMax = Float.NaN;
        float currentColorAlpha = -1f;
        for (DrawItem d : items) {
            if (!d.taken && !d.currentNode && !d.highlighted && d.available && d.nodeColorLive) {
                Map<String, Object> s = new LinkedHashMap<String, Object>();
                s.put("row", Integer.valueOf(d.row));
                s.put("col", Integer.valueOf(d.col));
                s.put("available", Boolean.valueOf(d.available));
                s.put("alpha", Float.valueOf(d.nodeA));
                s.put("colorHex", hex(d.nodeR, d.nodeG, d.nodeB, d.nodeA));
                availableColorAlphas.add(s);
                if (Float.isNaN(alphaMin) || d.nodeA < alphaMin) alphaMin = d.nodeA;
                if (Float.isNaN(alphaMax) || d.nodeA > alphaMax) alphaMax = d.nodeA;
            }
            if (d.currentNode) {
                currentColorAlpha = d.nodeA;
            }
        }
        m.put("availableColorAlphas", availableColorAlphas);
        m.put("availableColorAlphaCount", Integer.valueOf(availableColorAlphas.size()));
        m.put("availableColorAlphaMin",
                Float.valueOf(Float.isNaN(alphaMin) ? -1f : alphaMin));
        m.put("availableColorAlphaMax",
                Float.valueOf(Float.isNaN(alphaMax) ? -1f : alphaMax));
        m.put("currentNodeColorAlpha", Float.valueOf(currentColorAlpha));
        // Count of projected nodes whose fill tint was resolved from the LIVE native node.color.
        int liveColorCount = 0;
        for (DrawItem d : items) {
            if (d.nodeColorLive) liveColorCount++;
        }
        m.put("liveColorCount", Integer.valueOf(liveColorCount));
        // NRO-04 D03 defect fix: the number of pixels renderMap will actually SUBMIT (node icon +
        // conditional outline/overlay, plus legend panel/title/icons). Distinct from `count`, which
        // is the projected node count. This is the probe-visible submission evidence.
        m.put("submitCount", Integer.valueOf(mapSubmissionPlan().size()));
        // M01 ring geometry: current/taken-node MAP_CIRCLE_5 submissions in the plan, with the native
        // formula size + rotation. `ringCount` proves supply; `rings`/`ring` carry the samples.
        m.put("ringCount", Integer.valueOf(ringCount()));
        List<Map<String, Object>> rings = ringSamples();
        m.put("rings", rings);
        m.put("ring", rings.isEmpty() ? null : rings.get(0));
        m.put("ringScale", Float.valueOf(liveSettingsScale()));
        // D03 map background follow-up: the native parchment background layer that ART now supplies
        // (mapTop/mapMid/mapBot + mapBlend×2). Empty when the projection carries no background.
        List<Map<String, Object>> backgroundList = new ArrayList<Map<String, Object>>();
        for (BackgroundDrawItem bg : backgroundItems()) {
            backgroundList.add(bg.toMap());
        }
        m.put("background", backgroundList);
        m.put("backgroundCount", Integer.valueOf(backgroundList.size()));
        // D03 map edges: the native map connection dots (ImageMaster.MAP_DOT_1 = dot1.png) read from
        // the stored (jittered) MapDot positions, tinted by the owning edge color, drawn BELOW the
        // nodes. `edgeCount` is the number of resolved dot draws; `edgeSample` carries the first few.
        List<EdgeDrawItem> edges = edgeItems();
        List<Map<String, Object>> edgeList = new ArrayList<Map<String, Object>>();
        int edgeLimit = Math.min(edges.size(), 16);
        for (int i = 0; i < edgeLimit; i++) {
            edgeList.add(edges.get(i).toMap());
        }
        m.put("edges", edgeList);
        m.put("edgeCount", Integer.valueOf(edges.size()));
        m.put("edgesPresent", Boolean.valueOf(!edges.isEmpty()));
        // D03 layering fix: the full paint order, background strictly below legend/nodes. Exposed so
        // the D1 scenario and unit tests can assert the layering that fixes the node wash-out.
        m.put("paintOrder", paintOrder());
        List<LegendDrawItem> legend = legendItems();
        Map<String, Object> legendMap = new LinkedHashMap<String, Object>();
        Map<String, Object> panel = null;
        List<Map<String, Object>> legendList = new ArrayList<Map<String, Object>>();
        for (LegendDrawItem li : legend) {
            if ("legend.panel".equals(li.id)) {
                panel = li.toMap();
            } else {
                legendList.add(li.toMap());
            }
        }
        legendMap.put("panel", panel);
        legendMap.put("items", legendList);
        // NRO-04 D03 follow-up: the RESOLVED legend text (localized when the projection carries the
        // native `Legend` UIStrings, else the English defaults) so the D1 scenario can assert it.
        legendMap.put("title", legendTitle());
        legendMap.put("labels", legendLabels());
        m.put("legend", legendMap);
        m.put("legendCount", Integer.valueOf(legend.size()));
        m.put("legendTitle", legendTitle());
        m.put("legendLabels", legendLabels());
        // D03: pure color-resolution samples derived from the state predicates (not a copied
        // constant). The D1 scenario asserts these against the VERIFIED native MapRoomNode values.
        Map<String, Object> samples = new LinkedHashMap<String, Object>();
        samples.put("takenColorHex", hex(nodeColor(true, false)));
        samples.put("availableColorHex", hex(nodeColor(false, true)));
        samples.put("untakenColorHex", hex(nodeColor(false, false)));
        samples.put("outlineColorHex", hex(outlineColor(false)));
        samples.put("highlightColorHex", hex(outlineColor(true)));
        m.put("colorSamples", samples);
        return m;
    }

    /** Number of {@code MAP_CIRCLE_5} ring submissions in the current plan (M01 probe evidence). */
    static int ringCount() {
        int n = 0;
        for (Submission s : mapSubmissionPlan()) {
            if (ResourceIds.UI_MAP_CIRCLE_5.equals(s.resourceId)) {
                n++;
            }
        }
        return n;
    }

    /** Ring submission probe samples (M01): rect, rotation, AVAILABLE tint, native size inputs. */
    static List<Map<String, Object>> ringSamples() {
        float scale = liveSettingsScale();
        float zoom = PAN.zoom();
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Submission s : mapSubmissionPlan()) {
            if (!ResourceIds.UI_MAP_CIRCLE_5.equals(s.resourceId)) {
                continue;
            }
            Map<String, Object> r = new LinkedHashMap<String, Object>();
            r.put("resourceId", s.resourceId);
            r.put("x", Float.valueOf(s.bounds.x));
            r.put("y", Float.valueOf(s.bounds.y));
            r.put("w", Float.valueOf(s.bounds.width));
            r.put("h", Float.valueOf(s.bounds.height));
            r.put("rotation", Float.valueOf(s.rotationDegrees));
            // Native size inputs: nodeScale/angle come from the gated node (matched by ring center),
            // settingsScale/zoom are the live draw-path inputs. `formulaSize` re-expresses the native
            // 192*(nodeScale*0.95+0.2)*scale(*zoom) so the D1 scenario can assert ring.w == formulaSize
            // (the exact-formula proof is pinned independently in JUnit from the native constants).
            float centerX = s.bounds.x + s.bounds.width * 0.5f;
            float centerY = s.bounds.y + s.bounds.height * 0.5f;
            float nodeScale = 1f;
            float angle = s.rotationDegrees;
            for (DrawItem d : buildFromProjection()) {
                if (Math.abs(d.screenX - centerX) < 0.01f && Math.abs(d.screenY - centerY) < 0.01f) {
                    nodeScale = d.nodeScale;
                    angle = d.angle;
                    break;
                }
            }
            r.put("nodeScale", Float.valueOf(nodeScale));
            r.put("angle", Float.valueOf(angle));
            r.put("settingsScale", Float.valueOf(scale));
            r.put("zoom", Float.valueOf(zoom));
            r.put("formulaSize", Float.valueOf(ringSize(nodeScale, scale, zoom)));
            Map<String, Object> color = new LinkedHashMap<String, Object>();
            color.put("r", Float.valueOf(s.r));
            color.put("g", Float.valueOf(s.g));
            color.put("b", Float.valueOf(s.b));
            color.put("a", Float.valueOf(s.a));
            r.put("color", color);
            r.put("colorHex", hex(s.r, s.g, s.b, s.a));
            out.add(r);
        }
        return out;
    }

    public static void resetForTests() {
        PAN.reset();
        lastScene = null;
        lastSceneEpoch = -1L;
        lastSceneKnown = false;
    }
}
