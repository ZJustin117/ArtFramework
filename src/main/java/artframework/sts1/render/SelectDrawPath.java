package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.CardView;
import artframework.context.SelectView;
import artframework.context.SurfaceIds;
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

        public DrawItem(
                String instanceId,
                String cardId,
                int slot,
                boolean selected,
                boolean visible,
                float x,
                float y,
                boolean confirm) {
            this.instanceId = instanceId != null ? instanceId : "";
            this.cardId = cardId != null ? cardId : "";
            this.slot = slot;
            this.selected = selected;
            this.visible = visible;
            this.x = x;
            this.y = y;
            this.confirm = confirm;
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
                            x,
                            y,
                            false));
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
                            defaultConfirmX(),
                            defaultConfirmY(),
                            true));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        SelectView sv = ArtFramework.projection().select();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
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
