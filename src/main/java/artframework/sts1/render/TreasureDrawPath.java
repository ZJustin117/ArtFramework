package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.context.TreasureView;
import artframework.sts1.FullPresentMode;

import java.util.LinkedHashMap;
import java.util.Map;

/** Treasure / chest full-present draw description (25.6). */
public final class TreasureDrawPath {

    private TreasureDrawPath() {}

    public static boolean shouldSuppressNativeTreasure() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.TREASURE);
    }

    public static Map<String, Object> probeSlice() {
        TreasureView tv = ArtFramework.projection().treasure();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("chestOpen", Boolean.valueOf(tv.chestOpen));
        m.put("canOpen", Boolean.valueOf(tv.canOpen));
        m.put("relicLabel", tv.relicLabel);
        m.put("relicResourceId", tv.relicResourceId);
        m.put("available", Boolean.valueOf(tv.available));
        m.put("suppressNativeTreasure", Boolean.valueOf(shouldSuppressNativeTreasure()));
        m.put("presentLevel", FullPresentMode.treasureLevel().name());
        return m;
    }
}
