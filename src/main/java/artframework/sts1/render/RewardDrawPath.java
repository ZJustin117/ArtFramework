package artframework.sts1.render;

import artframework.api.ArtFramework;
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

    public static final class DrawItem {
        public final int index;
        public final String kind;
        public final String label;
        public final boolean visible;
        public final boolean enabled;
        public final float x;
        public final float y;
        public final float w;
        public final float h;

        public DrawItem(
                int index,
                String kind,
                String label,
                boolean visible,
                boolean enabled,
                float x,
                float y,
                float w,
                float h) {
            this.index = index;
            this.kind = kind != null ? kind : "";
            this.label = label != null ? label : "";
            this.visible = visible;
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("index", Integer.valueOf(index));
            m.put("kind", kind);
            m.put("label", label);
            m.put("visible", Boolean.valueOf(visible));
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("w", Float.valueOf(w));
            m.put("h", Float.valueOf(h));
            return m;
        }
    }

    private RewardDrawPath() {}

    public static boolean shouldSuppressNativeReward() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REWARD_COMBAT)
                || Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REWARD_CARD)
                || Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.REWARD_BOSS_RELIC);
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
                            item.index, item.kind, item.label, item.visible, item.enabled, x, y, w, h));
            i++;
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        RewardView rv = ArtFramework.projection().reward();
        m.put("count", Integer.valueOf(items.size()));
        m.put("kind", rv.kind);
        m.put("title", rv.title);
        m.put("available", Boolean.valueOf(rv.available));
        m.put("suppressNativeReward", Boolean.valueOf(shouldSuppressNativeReward()));
        m.put("presentLevel", FullPresentMode.rewardLevel().name());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
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
}
