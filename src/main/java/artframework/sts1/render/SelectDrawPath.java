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
        public final String cardId;
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
            this(instanceId, cardId, slot, selected, visible, true, x, y,
                    confirm, confirm ? ResourceIds.UI_SELECT_CONFIRM : ResourceIds.UI_SELECT_CARD,
                    confirm ? "" : ResourceIds.UI_SELECT_CARD_FRAME, 250f, 350f);
        }

        public DrawItem(String instanceId, String cardId, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h) {
            this.instanceId = instanceId != null ? instanceId : "";
            this.cardId = cardId != null ? cardId : "";
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
            out.add(
                    new DrawItem(
                            "confirm",
                            "Confirm",
                            -1,
                            false,
                            sv.confirmVisible,
                            sv.confirmEnabled,
                            defaultConfirmX(),
                            defaultConfirmY(),
                            true,
                            sv.confirmEnabled ? ResourceIds.UI_SELECT_CONFIRM
                                    : ResourceIds.UI_SELECT_CONFIRM_DISABLED,
                            "", 360f, 48f));
        }
        return out;
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

    private static float defaultConfirmX() {
        try {
            return com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
        } catch (Throwable t) {
            return 960f;
        }
    }

    private static float defaultConfirmY() {
        try {
            return com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.18f;
        } catch (Throwable t) {
            return 200f;
        }
    }
}
