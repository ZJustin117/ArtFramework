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

    /**
     * Pure projection of the current TreasureView into paintable rows: a title row plus the live
     * chest state (closed / opened with relic label when projected). Empty while the view is
     * unavailable so the renderer never invents pixels.
     */
    public static java.util.List<RoomChromeLine> chromeLines() {
        java.util.List<RoomChromeLine> out = new java.util.ArrayList<RoomChromeLine>();
        TreasureView tv = ArtFramework.projection().treasure();
        if (!tv.available) {
            return out;
        }
        out.add(new RoomChromeLine("title", "Treasure", true));
        if (tv.chestOpen) {
            out.add(new RoomChromeLine(
                    "relic",
                    tv.relicLabel.isEmpty() ? "Chest opened" : tv.relicLabel,
                    true));
        } else {
            out.add(new RoomChromeLine("chest", "Chest closed", tv.canOpen));
        }
        return out;
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
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.TREASURE);
        m.put("capability", cap.state.name());
        m.put("capabilityReason", cap.reason);
        // Slice C phase 2: real ART-painted chrome rows (additive probe field).
        m.put("chromeLineCount", Integer.valueOf(chromeLines().size()));
        return m;
    }
}
