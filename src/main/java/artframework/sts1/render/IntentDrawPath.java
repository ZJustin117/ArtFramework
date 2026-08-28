package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.MonsterIntentView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.assets.ResourceIds;
import artframework.component.Rect;

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
        public final int amount;
        public final int multiAmount;
        public final float x;
        public final float y;
        public final String text;
        public final Rect bounds;

        public DrawItem(
                String monsterId,
                String monsterName,
                String intentType,
                String iconResourceId,
                int multiAmount,
                float x,
                float y) {
            this(monsterId, monsterName, intentType, iconResourceId, multiAmount, multiAmount, x, y);
        }

        public DrawItem(String monsterId, String monsterName, String intentType,
                String iconResourceId, int amount, int multiAmount, float x, float y) {
            this.monsterId = monsterId != null ? monsterId : "";
            this.monsterName = monsterName != null ? monsterName : "";
            this.intentType = intentType != null ? intentType : "";
            this.iconResourceId = iconResourceId != null ? iconResourceId : "";
            this.amount = amount;
            this.multiAmount = multiAmount;
            this.x = x;
            this.y = y;
            this.text = String.valueOf(amount);
            this.bounds = new Rect(x - 32f, y - 32f, 64f, 64f);
        }

        public DrawItem(String monsterId, String monsterName, String intentType,
                String iconResourceId, int multiAmount, float x, float y, String text, Rect bounds) {
            this.monsterId = monsterId != null ? monsterId : "";
            this.monsterName = monsterName != null ? monsterName : "";
            this.intentType = intentType != null ? intentType : "";
            this.iconResourceId = iconResourceId != null ? iconResourceId : "";
            this.amount = multiAmount;
            this.multiAmount = multiAmount;
            this.x = x;
            this.y = y;
            this.text = text != null ? text : "";
            this.bounds = bounds;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("monsterId", monsterId);
            m.put("monsterName", monsterName);
            m.put("intentType", intentType);
            m.put("iconResourceId", iconResourceId);
            m.put("amount", Integer.valueOf(amount));
            m.put("multiAmount", Integer.valueOf(multiAmount));
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("text", text);
            if (bounds != null) {
                m.put("width", Float.valueOf(bounds.width));
                m.put("height", Float.valueOf(bounds.height));
            }
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
            String resourceId = e.iconResourceId != null && !e.iconResourceId.isEmpty()
                    ? canonicalIntentResource(e.iconResourceId) : ResourceIds.UI_COMBAT_INTENT_UNKNOWN;
            out.add(
                    new DrawItem(
                            e.monsterId,
                            e.monsterName,
                            e.intentType,
                            resourceId,
                            e.amount,
                            e.multiAmount,
                            e.x,
                            e.y));
        }
        return out;
    }

    private static String canonicalIntentResource(String resourceId) {
        if (resourceId.startsWith(ResourceIds.UI_INTENT_PREFIX + "attack.")) {
            return resourceId.replace(ResourceIds.UI_INTENT_PREFIX + "attack.",
                    ResourceIds.UI_INTENT_PREFIX + "attack/");
        }
        return resourceId;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("available", Boolean.valueOf(ArtFramework.projection().intents().available));
        m.put("suppressNativeIntents", Boolean.valueOf(shouldSuppressNativeIntents()));
        m.put("presentLevel", FullPresentMode.intentsLevel().name());
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.COMBAT_INTENTS);
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
}
