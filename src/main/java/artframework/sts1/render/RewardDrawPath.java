package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.RewardItemView;
import artframework.context.RewardView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reward full-present draw description (25.3–25.4 / 25.8). */
public final class RewardDrawPath {

    /** Sheet item kind (the whole-screen panel drawn behind the rows). */
    public static final String SHEET_KIND = "reward.sheet";
    /** Rows sit at this C2/z band so they paint OVER the sheet. */
    public static final float ROW_Z = 1f;
    /** NRO-04 D04 sheet z: below the rows so it is drawn first (behind). */
    public static final float SHEET_Z = 0.5f;
    /** C2 item id/role for the sheet (the rows keep {@code reward:<index>} / {@code reward-item}). */
    public static final String SHEET_ITEM_ID = "reward.sheet";
    public static final String SHEET_ROLE = "reward-panel";

    public static final class DrawItem {
        public final int index;
        public final String kind;
        public final String label;
        public final String resourceId;
        public final boolean visible;
        public final boolean enabled;
        public final float x;
        public final float y;
        public final float w;
        public final float h;
        /**
         * Draw band for stable ordering: lower z is drawn first (behind). Rows use {@link #ROW_Z};
         * the D04 sheet uses {@link #SHEET_Z} so it always sorts before the rows.
         */
        public final float z;

        public DrawItem(
                int index,
                String kind,
                String label,
                String resourceId,
                boolean visible,
                boolean enabled,
                float x,
                float y,
                float w,
                float h) {
            this(index, kind, label, resourceId, visible, enabled, x, y, w, h, ROW_Z);
        }

        public DrawItem(
                int index,
                String kind,
                String label,
                String resourceId,
                boolean visible,
                boolean enabled,
                float x,
                float y,
                float w,
                float h,
                float z) {
            this.index = index;
            this.kind = kind != null ? kind : "";
            this.label = label != null ? label : "";
            this.resourceId = resourceId != null ? resourceId : "";
            this.visible = visible;
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.z = z;
        }

        /** Bottom-left bounds, matching how the renderer interprets {@code x}/{@code y} as center. */
        public artframework.component.Rect bounds() {
            return new artframework.component.Rect(x - w / 2f, y - h / 2f, w, h);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("index", Integer.valueOf(index));
            m.put("kind", kind);
            m.put("label", label);
            m.put("resourceId", resourceId);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("w", Float.valueOf(w));
            m.put("h", Float.valueOf(h));
            m.put("z", Float.valueOf(z));
            return m;
        }
    }

    private RewardDrawPath() {}

    public static boolean shouldSuppressNativeReward() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REWARD_COMBAT)
                || Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REWARD_CARD)
                || Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REWARD_BOSS_RELIC);
    }

    /**
     * NRO-04 D04 reward screen SHEET (panel) item, or {@code null} when {@code Settings} is
     * unavailable/not initialized (all reads fail-open, mirroring {@code MapDrawPath.legendItems}).
     *
     * <p><b>Verified native geometry</b> ({@code CombatRewardScreen.renderItemReward} bytecode):
     * {@code sb.draw(ImageMaster.REWARD_SCREEN_SHEET, Settings.WIDTH/2f - 306f,
     * Settings.HEIGHT/2f - 46f*Settings.scale - 358f, 306f, 358f, 612f, 716f, Settings.xScale,
     * Settings.scale, 0f, 0, 0, 612, 716, false, false)}. The bottom-left corner is therefore
     * {@code (WIDTH/2 - 306, HEIGHT/2 - 46*scale - 358)} and the size is
     * {@code (612*xScale, 716*scale)}. {@code DrawItem.x/y} are interpreted as the rectangle
     * CENTER by the renderers/row path, so the center is
     * {@code (WIDTH/2 - 306 + 306*xScale, HEIGHT/2 - 46*scale - 358 + 358*scale)}; at
     * {@code scale = xScale = 1, WIDTH = 1920, HEIGHT = 1080} that is center {@code (960, 494)},
     * bottom-left {@code (654, 136)}, size {@code 612x716}.
     */
    public static DrawItem sheetItem() {
        float[] s = sheetSettings();
        if (s == null) {
            return null;
        }
        float width = s[0];
        float height = s[1];
        float scale = s[2];
        float xScale = s[3];
        boolean available = ArtFramework.projection().reward().available;
        if (!available) {
            return null;
        }
        float x = width * 0.5f - 306f + 306f * xScale;
        float y = height * 0.5f - 46f * scale - 358f + 358f * scale;
        return new DrawItem(
                -1, SHEET_KIND, "", ResourceIds.UI_REWARD_SHEET,
                true, true, x, y, 612f * xScale, 716f * scale, SHEET_Z);
    }

    /**
     * Live {@code Settings} values {@code {WIDTH, HEIGHT, scale, xScale}} needed by the sheet
     * formula, or {@code null} when the host {@code Settings} type is unavailable or not yet
     * initialized (any value {@code <= 0}). Mirrors {@code MapDrawPath.legendSettings} fail-open.
     */
    private static float[] sheetSettings() {
        try {
            float width = com.megacrit.cardcrawl.core.Settings.WIDTH;
            float height = com.megacrit.cardcrawl.core.Settings.HEIGHT;
            float scale = com.megacrit.cardcrawl.core.Settings.scale;
            float xScale = com.megacrit.cardcrawl.core.Settings.xScale;
            if (width > 0f && height > 0f && scale > 0f && xScale > 0f) {
                return new float[] {width, height, scale, xScale};
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Ordered draw list for the reward surface: the D04 sheet/panel FIRST (behind), then the
     * projected rows. The sheet carries {@link #SHEET_Z} ({@code 0.5f}) and the rows
     * {@link #ROW_Z} ({@code 1f}); a stable ascending-z sort makes the "sheet sorts before rows"
     * contract literal. When {@code Settings} is unavailable the list is exactly the rows.
     */
    public static List<DrawItem> drawOrder() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        DrawItem sheet = sheetItem();
        if (sheet != null) {
            out.add(sheet);
        }
        out.addAll(buildFromProjection());
        java.util.Collections.sort(out, new java.util.Comparator<DrawItem>() {
            @Override
            public int compare(DrawItem a, DrawItem b) {
                return Float.compare(a.z, b.z);
            }
        });
        return out;
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        RewardView rv = ArtFramework.projection().reward();
        int i = 0;
        for (RewardItemView item : rv.items) {
            float y = item.y != 0f || item.h != 0f ? item.y : defaultY(i);
            float x = item.x != 0f || item.w != 0f ? item.x : defaultX();
            float w = item.w > 0f ? item.w : 360f;
            float h = item.h > 0f ? item.h : 48f;
            out.add(
                    new DrawItem(
                            item.index, item.kind, item.label,
                            resourceFor(item.kind, item.resourceId, item.enabled),
                            item.visible, item.enabled, x, y, w, h));
            i++;
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        RewardView rv = ArtFramework.projection().reward();
        m.put("count", Integer.valueOf(visibleCount(items)));
        m.put("kind", rv.kind);
        m.put("title", rv.title);
        m.put("available", Boolean.valueOf(rv.available));
        m.put("suppressNativeReward", Boolean.valueOf(shouldSuppressNativeReward()));
        m.put("presentLevel", FullPresentMode.rewardLevel().name());
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.REWARD_COMBAT);
        m.put("capability", cap.state.name());
        m.put("capabilityReason", cap.reason);
        m.put("drawCount", Integer.valueOf(visibleCount(items)));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        // NRO-04 D04: the whole-screen sheet/panel is supplied as its own sub-map (kept out of
        // items[] so the public row list semantics are unchanged). x/y use the SAME CENTER
        // convention as items[]; the renderer's x - w/2, y - h/2 yields the native bottom-left.
        // `submitCount` is the number of pixels renderReward actually submits (sheet + rows),
        // mirroring MapDrawPath's projected `count` vs submitted `submitCount` distinction.
        DrawItem sheet = sheetItem();
        Map<String, Object> sheetMap = new LinkedHashMap<String, Object>();
        if (sheet != null) {
            sheetMap.put("resourceId", sheet.resourceId);
            sheetMap.put("x", Float.valueOf(sheet.x));
            sheetMap.put("y", Float.valueOf(sheet.y));
            sheetMap.put("w", Float.valueOf(sheet.w));
            sheetMap.put("h", Float.valueOf(sheet.h));
        }
        m.put("sheet", sheetMap.isEmpty() ? null : sheetMap);
        m.put("sheetCount", Integer.valueOf(sheet != null ? 1 : 0));
        m.put("submitCount", Integer.valueOf(drawSubmissionCount()));
        return m;
    }

    private static float defaultX() {
        try {
            return com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
        } catch (Throwable t) {
            return 960f;
        }
    }

    private static float defaultY(int index) {
        float base;
        try {
            base = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.55f;
        } catch (Throwable t) {
            base = 600f;
        }
        return base - index * 56f;
    }

    private static int visibleCount(List<DrawItem> items) {
        int count = 0;
        for (DrawItem item : items) {
            if (item.visible) count++;
        }
        return count;
    }

    public static int materializedDrawCount() {
        return visibleCount(buildFromProjection());
    }

    /** 1 when the D04 sheet is supplied (Settings initialized + reward available), else 0. */
    public static int sheetCount() {
        return sheetItem() != null ? 1 : 0;
    }

    /**
     * Number of pixels {@code renderReward} actually submits: the D04 sheet (when supplied) plus
     * every visible row. Kept distinct from {@link #materializedDrawCount()} (rows only) so the
     * existing row-count evidence contract is unchanged.
     */
    public static int drawSubmissionCount() {
        return sheetCount() + visibleCount(buildFromProjection());
    }

    private static String fallbackResource(String kind, boolean enabled) {
        if (!enabled) return ResourceIds.UI_REWARD_DISABLED;
        if ("gold".equals(kind)) return ResourceIds.UI_REWARD_GOLD;
        if ("card".equals(kind)) return ResourceIds.UI_REWARD_CARD;
        if ("boss_relic".equals(kind)) return ResourceIds.UI_REWARD_BOSS_RELIC;
        if ("relic".equals(kind)) return ResourceIds.UI_REWARD_RELIC;
        return ResourceIds.UI_REWARD_ITEM_PANEL;
    }

    private static boolean catalogKnows(String resourceId) {
        return resourceId != null && !resourceId.isEmpty()
                && artframework.sts1.assets.Sts1VanillaCatalog.isKnown(resourceId);
    }

    public static String resourceFor(String kind, String resourceId, boolean enabled) {
        if (enabled && catalogKnows(resourceId)) return resourceId;
        return fallbackResource(kind, enabled);
    }

}
