package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.context.TopPanelView;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Top panel HUD draw description (25.9). */
public final class TopPanelDrawPath {

    public static final class DrawItem {
        public final String id;
        public final String text;
        public final boolean visible;

        public DrawItem(String id, String text, boolean visible) {
            this.id = id;
            this.text = text;
            this.visible = visible;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("text", text);
            m.put("visible", Boolean.valueOf(visible));
            return m;
        }
    }

    private TopPanelDrawPath() {}

    public static boolean shouldSuppressNativeTopPanel() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.TOP_PANEL);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        TopPanelView tv = ArtFramework.projection().topPanel();
        if (tv.available) {
            out.add(new DrawItem("top_panel_hud", label(tv), true));
        }
        return out;
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
        m.put("available", Boolean.valueOf(tv.available));
        m.put("suppressNativeTopPanel", Boolean.valueOf(shouldSuppressNativeTopPanel()));
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
}
