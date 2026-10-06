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
     * Current-node / taken-node ring geometry (native {@code MAP_CIRCLE_5} draw): the 192px ring is
     * drawn centered on the node, so relative to the 128px native node texture the projected ring
     * box is {@code nodeBox * RING_SIZE / NODE_TEXTURE_SIZE}. The native
     * {@code (nodeScale*0.95+0.2)*Settings.scale} factor is NOT applied because the projection does
     * not expose the live per-node scale (documented gap).
     */
    public static final float RING_SIZE = 192f;
    public static final float NODE_TEXTURE_SIZE = 128f;

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
            // Native MapRoomNode.render color resolution (D03):
            //   outline: highlighted ? (0.9,0.9,0.9,1) : OUTLINE_COLOR (8c8c80ff)
            //   node:    taken ? AVAILABLE_COLOR : this.color
            // `this.color` is AVAILABLE_COLOR for available/reachable nodes and NOT_TAKEN_COLOR
            // otherwise; the backend exposes that as `available` (node.color == AVAILABLE_COLOR).
            float[] node = nodeColor(taken, available);
            this.nodeR = node[0];
            this.nodeG = node[1];
            this.nodeB = node[2];
            this.nodeA = node[3];
            float[] outline = outlineColor(highlighted);
            this.outlineR = outline[0];
            this.outlineG = outline[1];
            this.outlineB = outline[2];
            this.outlineA = outline[3];
            this.currentNode = current;
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

        public LegendDrawItem(String id, int index, String label, String resourceId, String role,
                float z, Rect bounds) {
            this.id = id != null ? id : "";
            this.index = index;
            this.label = label != null ? label : "";
            this.resourceId = resourceId != null ? resourceId : "";
            this.role = role != null ? role : "";
            this.z = z;
            this.bounds = bounds != null ? bounds : Rect.ZERO;
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
                    outline.found, highlight.found, n.available, n.current, bounds));
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
     * NRO-04 D03 map legend items: native {@code Legend.render} panel + title and the 6
     * {@code LegendItem.render} room-type icon rows, at REST (non-hovered, desktop). Returns an
     * empty list when {@code Settings} is unavailable so the renderer never invents pixels.
     *
     * <p>Verified native geometry: panel {@code sb.draw(MAP_LEGEND, X-256f, Y-400f, 256f, 400f,
     * 512f, 800f, Settings.scale, Settings.yScale, 0f, ...)} with {@code X = 1670f*Settings.xScale}
     * and {@code Y = 600f*Settings.yScale}; icon {@code sb.draw(img, ICON_X-64f,
     * Y - SPACE_Y*index + OFFSET_Y - 64f, 64f, 64f, 128f, 128f, Settings.scale/1.65f,
     * Settings.scale/1.65f, ...)} with {@code ICON_X = 1575f*Settings.xScale},
     * {@code SPACE_Y = 58f*Settings.yScale} and {@code OFFSET_Y = 100f*Settings.yScale} (desktop).
     *
     * <p><b>Documented gaps.</b> Hover icon scale ({@code /1.2}) and its tip, the controller
     * reticle, and the legend alpha fade-in ({@code Legend.c.a} lerp) are NOT replicated; this path
     * draws the rest state at full alpha.
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
        for (int i = 0; i < LEGEND_ROOM_IDS.length && i < LEGEND_ROOM_LABELS.length; i++) {
            String room = LEGEND_ROOM_IDS[i];
            float icon = 128f * (scale / 1.65f);
            Rect bounds = new Rect(LEGEND_ITEM_ICON_X * xScale - 64f,
                    y - (LEGEND_ITEM_SPACE_Y * yScale) * i + (LEGEND_ITEM_OFFSET_Y * yScale) - 64f,
                    icon, icon);
            out.add(new LegendDrawItem("legend:" + room, i, legendLabel(i),
                    legendIconResource(room), "map-legend", 0.15f, bounds));
        }
        return out;
    }

    /**
     * Ordered pure submission plan for the map surface (NRO-04 D03 defect fix): exactly what
     * {@code Sts1SurfaceRenderer.renderMap} draws, in native paint order (legend first, then the
     * node band on top: node icon=1, outline=2, overlay=3). Each entry is a
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

        public Submission(String resourceId, Rect bounds, String label) {
            this(resourceId, bounds, label, "plain", 1f, 1f, 1f, 1f);
        }

        public Submission(String resourceId, Rect bounds, String label, String role,
                float r, float g, float b, float a) {
            this.resourceId = resourceId != null ? resourceId : "";
            this.bounds = bounds;
            this.label = label != null ? label : "";
            this.role = role != null ? role : "plain";
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }

    /**
     * Builds the ordered map submission plan. Legend panel/title/icon entries are emitted FIRST,
     * then the node entries (node icon, then outline when {@code reachable || highlighted}, then
     * overlay when {@code pinned || highlighted}) — matching native {@code DungeonMap.render}, which
     * draws {@code Legend.render} before {@code DungeonMapScreen.render} draws the nodes, so nodes
     * paint OVER the legend. Node/legend internal order is preserved. Entries with neither a resource
     * id nor a label are omitted so the renderer never submits a no-op; the legend title is
     * label-only (no native texture).
     */
    public static List<Submission> mapSubmissionPlan() {
        List<Submission> out = new ArrayList<Submission>();
        for (LegendDrawItem legend : legendItems()) {
            if (legend.bounds == null || legend.bounds.width <= 0f || legend.bounds.height <= 0f) {
                continue;
            }
            addSubmission(out, legend.resourceId, legend.bounds, legend.label);
        }
        for (DrawItem item : buildFromProjection()) {
            if (item.bounds == null || item.bounds.width <= 0f || item.bounds.height <= 0f) {
                continue;
            }
            addSubmission(out, item.resourceId, item.bounds, "", "node",
                    item.nodeR, item.nodeG, item.nodeB, item.nodeA);
            if (item.reachable || item.highlighted) {
                addSubmission(out, item.outlineResourceId, item.bounds, "", "outline",
                        item.outlineR, item.outlineG, item.outlineB, item.outlineA);
            }
            if (item.pinned || item.highlighted) {
                addSubmission(out, item.highlightResourceId, item.bounds, "", "overlay",
                        1f, 1f, 1f, 1f);
            }
            if (item.taken || item.currentNode) {
                // Native MapRoomNode.render (D03): MAP_CIRCLE_5 ring at AVAILABLE_COLOR when
                // `taken || (firstRoomChosen && curr)`, centered on the node, size
                // 192/128 × the projected node box (the 192 vs native 128 node texture ratio).
                // Documented gap: the live native (nodeScale*0.95+0.2)*Settings.scale factor is not
                // applied (the projection exposes no per-node scale).
                float ringW = item.bounds.width * (RING_SIZE / NODE_TEXTURE_SIZE);
                float ringH = item.bounds.height * (RING_SIZE / NODE_TEXTURE_SIZE);
                Rect ring = new Rect(item.screenX - ringW * 0.5f, item.screenY - ringH * 0.5f,
                        ringW, ringH);
                addSubmission(out, ResourceIds.UI_MAP_CIRCLE_5, ring, "", "ring",
                        AVAILABLE_COLOR[0], AVAILABLE_COLOR[1], AVAILABLE_COLOR[2], AVAILABLE_COLOR[3]);
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
        boolean hasResource = resourceId != null && !resourceId.isEmpty();
        boolean hasLabel = label != null && !label.isEmpty();
        if (!hasResource && !hasLabel) {
            return;
        }
        out.add(new Submission(resourceId, bounds, label, role, r, g, b, a));
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
        // NRO-04 D03 defect fix: the number of pixels renderMap will actually SUBMIT (node icon +
        // conditional outline/overlay, plus legend panel/title/icons). Distinct from `count`, which
        // is the projected node count. This is the probe-visible submission evidence.
        m.put("submitCount", Integer.valueOf(mapSubmissionPlan().size()));
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

    public static void resetForTests() {
        PAN.reset();
        lastScene = null;
        lastSceneEpoch = -1L;
        lastSceneKnown = false;
    }
}
