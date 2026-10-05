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

    /** NRO-04 D08: C2 id/role for the actual {@code AbstractChest} sprite (distinct from the rows). */
    public static final String CHEST_ITEM_ID = "treasure.chest.sprite";
    public static final String CHEST_ROLE = "treasure-chest-sprite";
    /** NRO-04 D08 chest sprite z: below the row z ({@code syncRoomChromeItems} uses 1f) so it paints first (behind). */
    public static final float CHEST_Z = 0.5f;

    /**
     * NRO-04 D08: the actual {@code AbstractChest} sprite. Native {@code AbstractChest.render} draws
     * a 512x512 texture with {@code CHEST_LOC_X = Settings.WIDTH/2f + 348f*scale},
     * {@code CHEST_LOC_Y = AbstractDungeon.floorY + 192f*scale}, and libgdx places the sprite CENTER
     * at that point (origin 256/256, scaleX=scaleY=scale) with size {@code 512*scale x 512*scale}.
     * {@code x}/{@code y} here are the native CENTER; {@link #bounds()} returns the bottom-left the
     * renderer draws at, exactly matching native.
     */
    public static final class ChestItem {
        public final String resourceId;
        public final float x;
        public final float y;
        public final float w;
        public final float h;

        public ChestItem(String resourceId, float x, float y, float w, float h) {
            this.resourceId = resourceId != null ? resourceId : "";
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        /** Native bottom-left bounds {@code (x - w/2, y - h/2, w, h)} for the center convention. */
        public artframework.component.Rect bounds() {
            return new artframework.component.Rect(x - w / 2f, y - h / 2f, w, h);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("resourceId", resourceId);
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("w", Float.valueOf(w));
            m.put("h", Float.valueOf(h));
            return m;
        }
    }

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

    /**
     * NRO-04 D08: the actual {@code AbstractChest} sprite item, or {@code null} when {@code Settings}
     * is unavailable/not initialized or the treasure view is unavailable (all reads fail-open,
     * mirroring {@code ShopDrawPath.rugItem}/{@code RewardDrawPath.sheetItem}).
     *
     * <p><b>Verified native geometry</b> ({@code AbstractChest.render} bytecode): center
     * {@code (Settings.WIDTH/2f + 348f*scale, AbstractDungeon.floorY + 192f*scale)}, size
     * {@code 512*scale x 512*scale}, at-rest rotation 0. When the projected {@code chestResourceId}
     * is empty the item falls back to a medium CLOSED chest ({@code mediumChest.png}) so it never
     * invents a wrong kind. At {@code scale = 1, WIDTH = 1920, floorY = y} the center is
     * {@code (1308, y + 192)} and the size is {@code 512x512}.
     */
    public static ChestItem chestItem() {
        float[] s = chestSettings();
        if (s == null) {
            return null;
        }
        TreasureView tv = ArtFramework.projection().treasure();
        if (!tv.available) {
            return null;
        }
        return chestItemAt(tv.chestResourceId, s[0], s[1], s[2]);
    }

    /**
     * Pure chest-sprite geometry from explicit hosts values ({@code scale}, {@code width},
     * {@code floorY}). Kept separate so the geometry/selection are testable in pure JUnit, where
     * the live {@code Settings}/{@code AbstractDungeon} globals are not initialized. Returns
     * {@code null} for a non-positive {@code scale}/{@code width}; falls back to a medium CLOSED
     * chest when {@code resourceId} is empty.
     */
    static ChestItem chestItemAt(String resourceId, float scale, float width, float floorY) {
        if (scale <= 0f || width <= 0f) {
            return null;
        }
        String rid = resourceId != null && !resourceId.isEmpty()
                ? resourceId
                : ResourceIds.chestSprite("medium", false);
        float centerX = width * 0.5f + 348f * scale;
        float centerY = floorY + 192f * scale;
        float size = 512f * scale;
        return new ChestItem(rid, centerX, centerY, size, size);
    }

    /**
     * Live {@code Settings} + {@code AbstractDungeon.floorY} values {@code {scale, WIDTH, floorY}}
     * needed by the chest formula, or {@code null} when {@code Settings} is unavailable/not yet
     * initialized ({@code WIDTH}/{@code scale} {@code <= 0}). All reads fail-open; the
     * {@code Settings} guard runs BEFORE the {@code AbstractDungeon.floorY} read so an uninitialized
     * host (whose {@code AbstractDungeon} class initializer can fail) is never touched.
     */
    private static float[] chestSettings() {
        try {
            float width = com.megacrit.cardcrawl.core.Settings.WIDTH;
            float scale = com.megacrit.cardcrawl.core.Settings.scale;
            if (width <= 0f || scale <= 0f) {
                return null;
            }
            float floorY = com.megacrit.cardcrawl.dungeons.AbstractDungeon.floorY;
            return new float[] {scale, width, floorY};
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 1 when the chest sprite is supplied (Settings initialized + treasure available), else 0. */
    public static int chestCount() {
        return chestItem() != null ? 1 : 0;
    }

    /** Number of pixels {@code renderTreasure} submits: the chest sprite (when supplied) + rows. */
    public static int submitCount() {
        return chestCount() + chromeLines().size();
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
        // NRO-04 D08: the actual AbstractChest sprite is supplied as its own sub-map (kept out of
        // the row list so the public chrome-row semantics are unchanged). x/y use the CENTER
        // convention (native CHEST_LOC_*), matching RewardDrawPath's sheet sub-map.
        ChestItem chest = chestItem();
        m.put("chest", chest != null ? chest.toMap() : null);
        m.put("chestCount", Integer.valueOf(chest != null ? 1 : 0));
        m.put("submitCount", Integer.valueOf(submitCount()));
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
