package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.CardView;
import artframework.context.SelectView;
import artframework.context.SurfaceIds;
import artframework.assets.ResourceIds;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grid/hand select full-present draw description (22.3). Host paints pool chrome from SelectView.
 */
public final class SelectDrawPath {

    public static final class DrawItem {
        public final String instanceId;
        /** Raw card id (e.g. {@code Strike_R}); identity/resource selection only. */
        public final String cardId;
        /**
         * Localized card name (the projection's {@link CardView#title}) used for the drawn text,
         * resolved to the raw {@link #cardId} when the title is null/empty. Never used for
         * identity or resource selection.
         */
        public final String label;
        public final int slot;
        public final boolean selected;
        public final boolean visible;
        public final float x;
        public final float y;
        public final boolean confirm;
        public final boolean enabled;
        public final float w;
        public final float h;
        public final String resourceId;
        public final String frameResourceId;

        public DrawItem(
                String instanceId,
                String cardId,
                int slot,
                boolean selected,
                boolean visible,
                float x,
                float y,
                boolean confirm) {
            this(instanceId, cardId, cardId, slot, selected, visible, true, x, y,
                    confirm, confirm ? ResourceIds.UI_SELECT_CONFIRM : ResourceIds.UI_SELECT_CARD,
                    confirm ? "" : ResourceIds.UI_SELECT_CARD_FRAME, 250f, 350f);
        }

        public DrawItem(String instanceId, String cardId, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h) {
            this(instanceId, cardId, cardId, slot, selected, visible, enabled, x, y, confirm,
                    resourceId, frameResourceId, w, h);
        }

        public DrawItem(String instanceId, String cardId, String label, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h) {
            this.instanceId = instanceId != null ? instanceId : "";
            this.cardId = cardId != null ? cardId : "";
            this.label = label != null && !label.isEmpty() ? label : this.cardId;
            this.slot = slot;
            this.selected = selected;
            this.visible = visible;
            this.x = x;
            this.y = y;
            this.confirm = confirm;
            this.enabled = enabled;
            this.w = w;
            this.h = h;
            this.resourceId = resourceId != null ? resourceId : "";
            this.frameResourceId = frameResourceId != null ? frameResourceId : "";
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("instanceId", instanceId);
            m.put("cardId", cardId);
            m.put("label", label);
            m.put("slot", Integer.valueOf(slot));
            m.put("selected", Boolean.valueOf(selected));
            m.put("visible", Boolean.valueOf(visible));
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("confirm", Boolean.valueOf(confirm));
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("w", Float.valueOf(w));
            m.put("h", Float.valueOf(h));
            m.put("resourceId", resourceId);
            m.put("frameResourceId", frameResourceId);
            return m;
        }
    }

    private SelectDrawPath() {}

    public static boolean shouldSuppressNativeSelect() {
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        return plan.shouldSuppressNative(SurfaceIds.SELECT_GRID)
                || plan.shouldSuppressNative(SurfaceIds.SELECT_HAND);
    }

    public static boolean shouldSuppressNativeGrid() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.SELECT_GRID);
    }

    public static boolean shouldSuppressNativeHandSelect() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.SELECT_HAND);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        SelectView sv = ArtFramework.projection().select();
        int i = 0;
        for (CardView c : sv.pool) {
            boolean selected = c.selected || sv.isSelected(c.ref.instanceId);
            float x = c.pose != null ? c.pose.x : defaultCardX(i, sv.poolCount());
            float y = c.pose != null ? c.pose.y : defaultCardY();
            out.add(
                    new DrawItem(
                            c.ref.instanceId,
                            c.ref.cardId,
                            c.title,
                            c.slotIndex,
                            selected,
                            c.pose == null || c.pose.visible,
                            c.playable,
                            x,
                            y,
                            false,
                            cardResource(selected, c.playable),
                            knownOrFallback(c.frameResourceId, ResourceIds.UI_SELECT_CARD_FRAME),
                            250f,
                            350f));
            i++;
        }
        if (sv.confirmVisible || sv.confirmEnabled) {
            out.add(confirmItem());
        }
        return out;
    }

    /**
     * The native select-screen confirm button item, or {@code null} when the confirm control is
     * neither visible nor enabled in the current projection.
     *
     * <p><b>Verified native geometry</b> ({@code CardSelectConfirmButton.renderButton} bytecode):
     * {@code TAKE_Y = 475f * Settings.scale} and the enabled/disabled draw is
     * {@code sb.draw(texture, Settings.WIDTH/2f - 256f, TAKE_Y - 128f, 256f, 128f, 512f, 256f,
     * Settings.scale, Settings.scale, 0f, 0, 0, 512, 256, false, false)}. With libGDX origin
     * scaling both axes scale around the texture centre, so the rendered button CENTRE is
     * {@code (WIDTH/2f, 475f*scale)} and its SIZE is {@code (512f*scale, 256f*scale)};
     * {@code DrawItem.x/y} use the SAME CENTER convention as the card items, so the native
     * top-left {@code (WIDTH/2 - 256*scale, 475*scale - 128*scale)} falls out of the renderer's
     * {@code x - w/2, y - h/2}. At {@code scale=1, WIDTH=1920} that is centre {@code (960, 475)},
     * size {@code 512x256}. All {@code Settings} reads fail-open (defaults {@code WIDTH=1920},
     * {@code scale=1}) so an uninitialized host still yields native-at-unit-scale geometry.
     */
    public static DrawItem confirmItem() {
        SelectView sv = ArtFramework.projection().select();
        if (!sv.confirmVisible && !sv.confirmEnabled) {
            return null;
        }
        float[] cs = confirmSettings();
        float width = cs[0];
        float scale = cs[1];
        return new DrawItem(
                "confirm",
                "Confirm",
                -1,
                false,
                sv.confirmVisible,
                sv.confirmEnabled,
                width * 0.5f,
                CONFIRM_TAKE_Y * scale,
                true,
                sv.confirmEnabled ? ResourceIds.UI_SELECT_CONFIRM
                        : ResourceIds.UI_SELECT_CONFIRM_DISABLED,
                "",
                CONFIRM_TEXTURE_W * scale,
                CONFIRM_TEXTURE_H * scale);
    }

    /** Native {@code TAKE_Y} base (unscaled): {@code 475f * Settings.scale}. */
    private static final float CONFIRM_TAKE_Y = 475f;
    /** Native confirm texture draw width in pixels (unscaled): {@code 512f}. */
    private static final float CONFIRM_TEXTURE_W = 512f;
    /** Native confirm texture draw height in pixels (unscaled): {@code 256f}. */
    private static final float CONFIRM_TEXTURE_H = 256f;

    /**
     * Live {@code Settings} values {@code {WIDTH, scale}} used by the native confirm geometry,
     * fail-open to {@code {1920, 1}} when the host type is unavailable or not yet initialized
     * (any value {@code <= 0}). Mirrors {@code RewardDrawPath.sheetSettings} fail-open, but defaults
     * rather than declining so the confirm button is never a zero-size degenerate draw.
     */
    private static float[] confirmSettings() {
        float width = 1920f;
        float scale = 1f;
        try {
            float w = com.megacrit.cardcrawl.core.Settings.WIDTH;
            float s = com.megacrit.cardcrawl.core.Settings.scale;
            if (w > 0f) {
                width = w;
            }
            if (s > 0f) {
                scale = s;
            }
        } catch (Throwable ignored) {
        }
        return new float[] {width, scale};
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        SelectView sv = ArtFramework.projection().select();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(visibleCount(items)));
        m.put("kind", sv.kind);
        m.put("available", Boolean.valueOf(sv.available));
        m.put("poolCount", Integer.valueOf(sv.poolCount()));
        m.put("selectedCount", Integer.valueOf(sv.selectedInstanceIds.size()));
        m.put("confirmEnabled", Boolean.valueOf(sv.confirmEnabled));
        m.put("confirmVisible", Boolean.valueOf(sv.confirmVisible));
        m.put("suppressNativeSelect", Boolean.valueOf(shouldSuppressNativeSelect()));
        m.put("presentLevel", FullPresentMode.selectLevel().name());
        // NRO-04 D09: expose the native confirm-button geometry as its own sub-map (kept out of
        // items[] so the public item-list semantics are unchanged). x/y use the SAME CENTER
        // convention as items[]; the renderer's x - w/2, y - h/2 yields the native top-left.
        DrawItem confirm = confirmItem();
        if (confirm != null) {
            Map<String, Object> c = new LinkedHashMap<String, Object>();
            c.put("resourceId", confirm.resourceId);
            c.put("x", Float.valueOf(confirm.x));
            c.put("y", Float.valueOf(confirm.y));
            c.put("w", Float.valueOf(confirm.w));
            c.put("h", Float.valueOf(confirm.h));
            c.put("enabled", Boolean.valueOf(confirm.enabled));
            c.put("visible", Boolean.valueOf(confirm.visible));
            m.put("confirm", c);
        } else {
            m.put("confirm", null);
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }

    public static int materializedDrawCount() { return visibleCount(buildFromProjection()); }

    private static int visibleCount(List<DrawItem> items) {
        int count = 0;
        for (DrawItem item : items) if (item.visible) count++;
        return count;
    }

    private static String knownOrFallback(String resourceId, String fallback) {
        return resourceId != null && !resourceId.isEmpty()
                && artframework.sts1.assets.Sts1VanillaCatalog.isKnown(resourceId)
                ? resourceId : fallback;
    }

    private static String cardResource(boolean selected, boolean enabled) {
        if (!enabled) return ResourceIds.UI_SELECT_CARD_DISABLED;
        return selected ? ResourceIds.UI_SELECT_CARD_SELECTED : ResourceIds.UI_SELECT_CARD;
    }

    private static float defaultCardX(int index, int total) {
        float mid;
        try {
            mid = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
        } catch (Throwable t) {
            mid = 960f;
        }
        float spacing = 140f;
        float start = mid - (Math.max(total, 1) - 1) * spacing * 0.5f;
        return start + index * spacing;
    }

    private static float defaultCardY() {
        try {
            return com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.45f;
        } catch (Throwable t) {
            return 540f;
        }
    }
}
