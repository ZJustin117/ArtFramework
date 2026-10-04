package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.PileSoulView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.backend.Sts1PileSoulProjection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combat draw-pile panel present (NRO-04 D01). The model reproduces the two native
 * {@code DrawPilePanel} layers with native textures and native geometry (bytecode-verified from
 * {@code $ART_STS_JAR}, {@code com.megacrit.cardcrawl.ui.panels.DrawPilePanel}); the observed pile
 * projection supplies only the card count, never the geometry.
 */
public final class PileDrawDrawPath {

    /**
     * Native {@code DrawPilePanel} constants and geometry (bytecode-verified from
     * {@code $ART_STS_JAR}, {@code com.megacrit.cardcrawl.ui.panels.DrawPilePanel}). Static
     * initializer, constructor, and {@code render} bodies define:
     *
     * <ul>
     *   <li>{@code RAW_W = 128} (deck button texture source width).
     *   <li>The constructor calls
     *       {@code AbstractPanel.<init>(show_x=0f, show_y=0f, hide_x=-300f*Settings.scale,
     *       hide_y=-300f*Settings.scale, null, hidden=true)} — the panel shows at the screen origin
     *       and rests hidden off-screen (confirmed at bytecode offsets 1-14 of {@code DrawPilePanel()}).
     *   <li>{@code render} draws {@code ImageMaster.DECK_BTN_BASE}
     *       ({@code images/ui/deckButton/base.png}) with source rect {@code (0, 0, 128, 128)} and
     *       width/height {@code 128f} at {@code (current_x + DECK_X, current_y + DECK_Y + bob.y/2)}
     *       with scale {@code scale}, where {@code DECK_X = 76f * Settings.scale - 64f} and
     *       {@code DECK_Y = 74f * Settings.scale - 64f}.
     *   <li>{@code COUNT_CIRCLE_W = 128f * Settings.scale}; {@code ImageMaster.DECK_COUNT_CIRCLE}
     *       ({@code images/ui/topPanel/countCircle.png}) is drawn at
     *       {@code (current_x + COUNT_OFFSET_X, current_y + COUNT_OFFSET_Y)} with
     *       {@code COUNT_OFFSET_X = 54f * Settings.scale} and
     *       {@code COUNT_OFFSET_Y = -18f * Settings.scale}.
     *   <li>The count text is {@code FontHelper.renderFontCentered(turnNumFont, Integer.toString(size),
     *       current_x + COUNT_X, current_y + COUNT_Y)} where {@code COUNT_X = 118f * Settings.scale}
     *       and {@code COUNT_Y = 48f * Settings.scale}.
     *   <li>{@code HITBOX_W = 120f * Settings.scale}.
     * </ul>
     *
     * <p>The panel is visible during combat at its show position ({@code show_x = show_y = 0}), so
     * the icon region is the 128px deck button anchored at the native {@code DECK_X/DECK_Y} offsets
     * relative to that origin.
     */
    public static final float RAW_W = 128f;
    public static final float DECK_BTN_SIZE = 128f;
    public static final float COUNT_CIRCLE_W = 128f;
    public static final float HITBOX_W = 120f;
    /** Native {@code show_x}/{@code show_y} while the combat panel is visible (bytecode-confirmed 0f). */
    public static final float PANEL_SHOW_X = 0f;
    public static final float PANEL_SHOW_Y = 0f;

    private PileDrawDrawPath() {}

    /** Native {@code DECK_X = 76f * Settings.scale - 64f}. */
    public static float deckX() {
        return 76f * scale() - 64f;
    }

    /** Native {@code DECK_Y = 74f * Settings.scale - 64f}. */
    public static float deckY() {
        return 74f * scale() - 64f;
    }

    /** Native {@code COUNT_X = 118f * Settings.scale}. */
    public static float countX() {
        return 118f * scale();
    }

    /** Native {@code COUNT_Y = 48f * Settings.scale}. */
    public static float countY() {
        return 48f * scale();
    }

    /** Native {@code COUNT_OFFSET_X = 54f * Settings.scale}. */
    public static float countOffsetX() {
        return 54f * scale();
    }

    /** Native {@code COUNT_OFFSET_Y = -18f * Settings.scale}. */
    public static float countOffsetY() {
        return -18f * scale();
    }

    private static float scale() {
        try {
            return com.megacrit.cardcrawl.core.Settings.scale;
        } catch (Throwable ignored) {
            return 1f;
        }
    }

    public static boolean shouldSuppressNativePileDraw() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_PILE_DRAW);
    }

    /** The draw-zone projection entry, or null when the backend has published no draw zone. */
    private static PileSoulView.Entry drawEntry() {
        for (PileSoulView.Entry entry : Sts1PileSoulProjection.current().entries) {
            if ("draw".equals(entry.zone)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Native draw-pile size: prefer the shared combat controls view {@code drawSize}; fall back to
     * the draw-zone projection count when the controls value is zero/absent.
     */
    public static int drawCount() {
        try {
            int drawSize = ArtFramework.projection().controls().drawSize;
            if (drawSize > 0) {
                return drawSize;
            }
            PileSoulView.Entry draw = drawEntry();
            if (draw != null && draw.count > 0) {
                return draw.count;
            }
            return drawSize >= 0 ? drawSize : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** One draw-pile panel item: label/count/resourceId/bounds. */
    public static final class DrawItem {
        public final String id;
        public final String label;
        public final int count;
        public final String resourceId;
        public final Rect bounds;
        public final boolean visible;

        public DrawItem(String id, String label, int count, String resourceId, Rect bounds,
                boolean visible) {
            this.id = id != null ? id : "";
            this.label = label != null ? label : "";
            this.count = count;
            this.resourceId = resourceId != null ? resourceId : "";
            this.bounds = bounds != null ? bounds : Rect.ZERO;
            this.visible = visible;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("label", label);
            m.put("count", Integer.valueOf(count));
            m.put("resourceId", resourceId);
            m.put("visible", Boolean.valueOf(visible));
            m.put("x", Float.valueOf(bounds.x));
            m.put("y", Float.valueOf(bounds.y));
            m.put("width", Float.valueOf(bounds.width));
            m.put("height", Float.valueOf(bounds.height));
            return m;
        }
    }

    /**
     * Builds the two native draw-pile layers: the deck-button base icon (empty label) followed by
     * the count circle carrying the numeric count. Geometry is the native {@code DrawPilePanel}
     * layout, never the observed pile projection bounds; the projection only contributes the count
     * fallback and a visibility hint.
     */
    public static List<DrawItem> buildFromProjection() {
        int count = drawCount();
        PileSoulView.Entry draw = drawEntry();
        boolean visible = draw == null || draw.visible;
        List<DrawItem> out = new ArrayList<DrawItem>();
        out.add(new DrawItem(
                "pile:draw:icon", "", count, ResourceIds.UI_PILE_DRAW, iconBounds(), visible));
        out.add(new DrawItem(
                "pile:draw:count", String.valueOf(count), count, ResourceIds.UI_PILE_COUNT_CIRCLE,
                countCircleBounds(), visible));
        return out;
    }

    /**
     * Native {@code DECK_BTN_BASE} bounds: the 128x128 deck-button texture drawn at
     * {@code (current_x + DECK_X, current_y + DECK_Y)} with scale {@code Settings.scale}. While the
     * combat panel is visible {@code current_x/y} are the native {@code show_x/show_y} ({@code 0, 0}).
     */
    public static Rect iconBounds() {
        float s = scale();
        return new Rect(PANEL_SHOW_X + deckX(), PANEL_SHOW_Y + deckY(),
                DECK_BTN_SIZE * s, DECK_BTN_SIZE * s);
    }

    /**
     * Native {@code DECK_COUNT_CIRCLE} bounds: drawn at
     * {@code (current_x + COUNT_OFFSET_X, current_y + COUNT_OFFSET_Y)} with size
     * {@code COUNT_CIRCLE_W}.
     */
    public static Rect countCircleBounds() {
        float s = scale();
        return new Rect(PANEL_SHOW_X + countOffsetX(), PANEL_SHOW_Y + countOffsetY(),
                COUNT_CIRCLE_W * s, COUNT_CIRCLE_W * s);
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("suppressNativePileDraw", Boolean.valueOf(shouldSuppressNativePileDraw()));
        m.put("presentLevel", FullPresentMode.pileDrawLevel().name());
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.COMBAT_PILE_DRAW);
        m.put("capability", cap.state.name());
        m.put("capabilityReason", cap.reason);
        m.put("drawCount", Integer.valueOf(drawCount()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", Collections.unmodifiableList(list));
        return m;
    }
}
