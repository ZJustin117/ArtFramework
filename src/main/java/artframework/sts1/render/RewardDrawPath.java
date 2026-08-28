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
