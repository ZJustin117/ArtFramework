package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
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
        out.add(line("title", "Treasure", true, "treasure-title",
                ResourceIds.UI_TREASURE_PANEL, 0));
        if (tv.chestOpen) {
            out.add(line("chest", "Chest open", false, "treasure-chest",
                    ResourceIds.UI_TREASURE_CHEST_OPEN, 1));
            out.add(line(
                    "relic",
                    tv.relicLabel.isEmpty() ? "Chest opened" : tv.relicLabel,
                    true,
                    "treasure-relic",
                    resourceForRelic(tv.relicResourceId),
                    2));
        } else {
            out.add(line("chest", "Chest closed", tv.canOpen, "treasure-chest",
                    tv.canOpen ? ResourceIds.UI_TREASURE_CHEST_CLOSED
                            : ResourceIds.UI_EVENT_BUTTON_DISABLED,
                    1));
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
        m.put("drawCount", Integer.valueOf(chromeLines().size()));
        return m;
    }

    private static RoomChromeLine line(String id, String text, boolean enabled, String role,
            String resourceId, int row) {
        float x = defaultX();
        float y = defaultY(row);
        float w = "title".equals(id) ? 420f : 360f;
        float h = 40f;
        return new RoomChromeLine(id, text, enabled, true, role, resourceId,
                x - w / 2f, y - h / 2f, w, h);
    }

    public static String resourceForRelic(String resourceId) {
        if (resourceId != null && !resourceId.isEmpty()
                && artframework.sts1.assets.Sts1VanillaCatalog.isKnown(resourceId)) {
            return resourceId;
        }
        return ResourceIds.UI_TREASURE_RELIC;
    }

    private static float defaultX() {
        try { return com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f; }
        catch (Throwable t) { return 960f; }
    }

    private static float defaultY(int row) {
        try { return com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.66f - row * 40f; }
        catch (Throwable t) { return 712.8f - row * 40f; }
    }
}
