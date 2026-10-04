package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.SurfaceIds;
import artframework.context.TopPanelView;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Top panel HUD draw description (25.9).
 *
 * <p>Supplies ART pixels for a full-present {@code TopPanel.render} surface. The native top panel
 * is suppressed wholesale when FULL is active, so every HUD item that should remain visible must be
 * re-supplied here. As of NRO-04 D02 that includes the top-right settings gear
 * ({@code TopPanel.renderSettingsIcon}) in addition to the left-side bar/HP/gold/floor/ascension/
 * status items; the deck and map buttons remain unsupplied (later slices).
 *
 * <p><b>Native settings-icon geometry (verified bytecode).</b> {@code renderSettingsIcon} draws
 * {@code ImageMaster.SETTINGS_ICON} ({@code images/ui/topPanel/settings.png}, 64x64) via the
 * 16-arg texture overload: origin {@code (32,32)}, size {@code 64x64}, uniform scale
 * {@code Settings.scale}, rotation {@code settingsAngle}, source rect {@code (0,0,64,64)}. The
 * position is {@code x = SETTINGS_X - 32f + 32f*Settings.scale},
 * {@code y = ICON_Y - 32f + 32f*Settings.scale} with {@code ICON_W = 64f*Settings.scale},
 * {@code TOP_RIGHT_PAD_X = 10f*Settings.scale}, {@code SETTINGS_X = Settings.WIDTH - (ICON_W +
 * TOP_RIGHT_PAD_X)} and {@code ICON_Y = Settings.HEIGHT - ICON_W}. At rest
 * {@code settingsAngle == 0}, so this reproduces the native draw using the existing 4-arg
 * {@code (x, y, w, h)} path with {@code w = h = 64f*Settings.scale} and
 * {@code yFromTop = 32f - 32f*Settings.scale}.
 *
 * <p><b>Documented gap.</b> The native gear SPINS when hovered or while the settings screen is
 * open ({@code updateSettingsButtonLogic} accumulates {@code settingsAngle}). This path renders the
 * gear at its rotation-0 rest state and does NOT replicate the hover spin; D1 evidence is an
 * at-rest A/B.
 */
public final class TopPanelDrawPath {

    public static final class DrawItem {
        public final String id;
        public final String text;
        public final String resourceId;
        public final boolean visible;
        private final float x;
        private final float yFromTop;
        private final float w;
        private final float h;

        public DrawItem(
                String id,
                String text,
                String resourceId,
                boolean visible,
                float x,
                float yFromTop,
                float w,
                float h) {
            this.id = id;
            this.text = text;
            this.resourceId = resourceId != null ? resourceId : "";
            this.visible = visible;
            this.x = x;
            this.yFromTop = yFromTop;
            this.w = w;
            this.h = h;
        }

        public Rect bounds(float screenW, float screenH) {
            return new Rect(x, screenH - yFromTop - h, w, h);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("text", text);
            m.put("resourceId", resourceId);
            m.put("visible", Boolean.valueOf(visible));
            Rect r = bounds(1920f, 1080f);
            m.put("x", Float.valueOf(r.x));
            m.put("y", Float.valueOf(r.y));
            m.put("w", Float.valueOf(r.width));
            m.put("h", Float.valueOf(r.height));
            return m;
        }
    }

    private TopPanelDrawPath() {}

    public static boolean shouldSuppressNativeTopPanel() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.TOP_PANEL);
    }

    /** ART must yield the pixels to native TopPanel.render when that invocation continued. */
    public static boolean shouldDrawArtOverlay() {
        return Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.TOP_PANEL)
                && !NativeRenderBridge.nativeContinuationForSurface(SurfaceIds.TOP_PANEL);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        TopPanelView tv = ArtFramework.projection().topPanel();
        if (tv.available) {
            out.add(new DrawItem("top_panel.bar", "", ResourceIds.UI_TOP_PANEL_BAR,
                    true, 0f, 0f, 1920f, 64f));
            out.add(new DrawItem("top_panel.hp", "HP " + tv.hp + "/" + tv.maxHp,
                    ResourceIds.UI_TOP_PANEL_HP, true, 46f, 12f, 150f, 34f));
            out.add(new DrawItem("top_panel.gold", String.valueOf(tv.gold),
                    ResourceIds.UI_TOP_PANEL_GOLD, true, 216f, 12f, 102f, 34f));
            out.add(new DrawItem("top_panel.floor", "Floor " + tv.floor,
                    ResourceIds.UI_TOP_PANEL_FLOOR, true, 338f, 12f, 122f, 34f));
            out.add(new DrawItem("top_panel.ascension", "A" + tv.ascension,
                    ResourceIds.UI_TOP_PANEL_ASCENSION, true, 480f, 12f, 70f, 34f));
            out.add(new DrawItem("top_panel.status", statusLabel(tv),
                    ResourceIds.UI_TOP_PANEL_STATUS, true, 570f, 12f, 180f, 34f));
            DrawItem settings = settingsItem();
            if (settings != null) {
                out.add(settings);
            }
        }
        return out;
    }

    /**
     * Native settings-gear item (NRO-04 D02), or {@code null} when {@code Settings} is unavailable.
     * Geometry mirrors {@code TopPanel.renderSettingsIcon} at rest ({@code settingsAngle == 0});
     * the native hover spin is intentionally NOT replicated (documented gap).
     */
    private static DrawItem settingsItem() {
        float scale;
        float width;
        try {
            scale = com.megacrit.cardcrawl.core.Settings.scale;
            width = com.megacrit.cardcrawl.core.Settings.WIDTH;
        } catch (Throwable t) {
            return null;
        }
        float iconW = 64f * scale;
        float topRightPadX = 10f * scale;
        float settingsX = width - (iconW + topRightPadX);
        float x = settingsX - 32f + 32f * scale;
        float yFromTop = 32f - 32f * scale;
        return new DrawItem("top_panel.settings", "", ResourceIds.UI_TOP_PANEL_SETTINGS,
                true, x, yFromTop, iconW, iconW);
    }

    public static int materializedDrawCount() {
        int n = 0;
        for (DrawItem item : buildFromProjection()) {
            if (item.visible) n++;
        }
        return n;
    }

    public static Map<String, Object> probeSlice() {
        TopPanelView tv = ArtFramework.projection().topPanel();
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("hp", Integer.valueOf(tv.hp));
        m.put("maxHp", Integer.valueOf(tv.maxHp));
        m.put("gold", Integer.valueOf(tv.gold));
        m.put("floor", Integer.valueOf(tv.floor));
        m.put("ascension", Integer.valueOf(tv.ascension));
        m.put("characterName", tv.characterName);
        m.put("statusText", tv.statusText);
        m.put("available", Boolean.valueOf(tv.available));
        m.put("suppressNativeTopPanel", Boolean.valueOf(shouldSuppressNativeTopPanel()));
        m.put("nativeContinuation", Boolean.valueOf(
                NativeRenderBridge.nativeContinuationForSurface(SurfaceIds.TOP_PANEL)));
        m.put("artOverlay", Boolean.valueOf(shouldDrawArtOverlay()));
        m.put("presentLevel", FullPresentMode.topPanelLevel().name());
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.TOP_PANEL);
        m.put("capability", cap.state.name());
        m.put("capabilityReason", cap.reason);
        m.put("drawCount", Integer.valueOf(items.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }

    static String label(TopPanelView tv) {
        return tv.characterName
                + "  HP "
                + tv.hp
                + "/"
                + tv.maxHp
                + "  Gold "
                + tv.gold
                + "  Floor "
                + tv.floor
                + "  A"
                + tv.ascension;
    }

    static String statusLabel(TopPanelView tv) {
        if (tv.statusText != null && !tv.statusText.isEmpty()) {
            return tv.statusText;
        }
        return tv.characterName != null ? tv.characterName : "";
    }
}
