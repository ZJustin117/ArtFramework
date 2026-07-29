package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.MonsterIntentView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Monster intent observe/draw path (25.10). */
public final class IntentDrawPath {

    public static final class DrawItem {
        public final String monsterId;
        public final String monsterName;
        public final String intentType;
        public final String iconResourceId;
        public final int multiAmount;
        public final float x;
        public final float y;

        public DrawItem(
                String monsterId,
                String monsterName,
                String intentType,
                String iconResourceId,
                int multiAmount,
                float x,
                float y) {
            this.monsterId = monsterId != null ? monsterId : "";
            this.monsterName = monsterName != null ? monsterName : "";
            this.intentType = intentType != null ? intentType : "";
            this.iconResourceId = iconResourceId != null ? iconResourceId : "";
            this.multiAmount = multiAmount;
            this.x = x;
            this.y = y;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("monsterId", monsterId);
            m.put("monsterName", monsterName);
            m.put("intentType", intentType);
            m.put("iconResourceId", iconResourceId);
            m.put("multiAmount", Integer.valueOf(multiAmount));
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            return m;
        }
    }

    private IntentDrawPath() {}

    public static boolean shouldSuppressNativeIntents() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_INTENTS);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        for (MonsterIntentView.IntentEntry e : ArtFramework.projection().intents().entries) {
            out.add(
                    new DrawItem(
                            e.monsterId,
                            e.monsterName,
                            e.intentType,
                            e.iconResourceId,
                            e.multiAmount,
                            e.x,
                            e.y));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("available", Boolean.valueOf(ArtFramework.projection().intents().available));
        m.put("suppressNativeIntents", Boolean.valueOf(shouldSuppressNativeIntents()));
        m.put("presentLevel", FullPresentMode.intentsLevel().name());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }
}
