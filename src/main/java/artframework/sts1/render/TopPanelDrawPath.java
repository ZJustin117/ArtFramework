package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.context.TopPanelView;
import artframework.sts1.FullPresentMode;

import java.util.LinkedHashMap;
import java.util.Map;

/** Top panel HUD draw description (25.9). */
public final class TopPanelDrawPath {

    private TopPanelDrawPath() {}

    public static boolean shouldSuppressNativeTopPanel() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.TOP_PANEL);
    }

    public static Map<String, Object> probeSlice() {
        TopPanelView tv = ArtFramework.projection().topPanel();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
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
        return m;
    }
}
