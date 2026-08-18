package artframework.c2;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.sts1.FullPresentMode;
import artframework.context.SurfaceIds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure co-op chrome draw description from {@link EntityPresent} slots (milestone 24).
 * Combat hand FULL owns in-combat card pixels — CARD slots are overlay-only then.
 */
public final class EntityDrawPath {

    public static final class DrawItem {
        public final String slotId;
        public final String kind;
        public final String refId;
        public final float x;
        public final float y;
        public final float w;
        public final float h;
        public final float scale;
        public final boolean visible;
        public final boolean overlayOnly;
        public final String artSource;
        public final boolean artFound;
        public final String iconSource;
        public final boolean iconFound;
        public final String label;
        public final int hp;
        public final int maxHp;
        public final int block;

        public DrawItem(
                String slotId,
                String kind,
                String refId,
                float x,
                float y,
                float w,
                float h,
                float scale,
                boolean visible,
                boolean overlayOnly,
                String artSource,
                boolean artFound,
                String iconSource,
                boolean iconFound,
                String label,
                int hp,
                int maxHp,
                int block) {
            this.slotId = slotId;
            this.kind = kind;
            this.refId = refId;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.scale = scale;
            this.visible = visible;
            this.overlayOnly = overlayOnly;
            this.artSource = artSource;
            this.artFound = artFound;
            this.iconSource = iconSource;
            this.iconFound = iconFound;
            this.label = label;
            this.hp = hp;
            this.maxHp = maxHp;
            this.block = block;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("slotId", slotId);
            m.put("kind", kind);
            m.put("refId", refId);
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("w", Float.valueOf(w));
            m.put("h", Float.valueOf(h));
            m.put("scale", Float.valueOf(scale));
            m.put("visible", Boolean.valueOf(visible));
            m.put("overlayOnly", Boolean.valueOf(overlayOnly));
            m.put("artSource", artSource);
            m.put("artFound", Boolean.valueOf(artFound));
            m.put("iconSource", iconSource);
            m.put("iconFound", Boolean.valueOf(iconFound));
            m.put("label", label);
            m.put("hp", Integer.valueOf(hp));
            m.put("maxHp", Integer.valueOf(maxHp));
            m.put("block", Integer.valueOf(block));
            return m;
        }
    }

    private EntityDrawPath() {}

    /** Combat hand FULL + mounted ⇒ EntityPresent CARD is overlay-only (24.4). */
    public static boolean cardOverlayOnly() {
        if (!FullPresentMode.combatHandLevel().allowsFullPresent()) {
            return false;
        }
        artframework.core.UiComponent hand = ArtFramework.component(SurfaceIds.COMBAT_HAND);
        return hand != null && hand.isMounted();
    }

    public static List<DrawItem> buildFromPresent() {
        return buildFromSlots(EntityPresentViews.list());
    }

    public static List<DrawItem> buildFromPresent(EntityPresent present) {
        if (present == null) {
            return new ArrayList<DrawItem>();
        }
        List<EntitySlot> slots = new ArrayList<EntitySlot>();
        for (String id : present.listSlotIds()) {
            EntitySlot slot = present.get(id);
            if (slot != null) slots.add(slot);
        }
        return buildFromSlots(slots);
    }

    private static List<DrawItem> buildFromSlots(List<EntitySlot> slots) {
        List<DrawItem> out = new ArrayList<DrawItem>();
        boolean cardOverlay = cardOverlayOnly();
        for (EntitySlot slot : slots) {
            if (!slot.isLaidOut()) continue;
            EntitySnapshot snap = slot.snapshot() != null ? slot.snapshot() : EntitySnapshot.empty();
            float scale = slot.scale() > 0f ? slot.scale() : (snap.scale > 0f ? snap.scale : 1f);
            float[] size = defaultSize(slot.kind, scale);
            float x = slot.x() != 0f || slot.y() != 0f ? slot.x() : snap.x;
            float y = slot.x() != 0f || slot.y() != 0f ? slot.y() : snap.y;
            String artKey = snap.artResourceId;
            String iconKey = snap.iconResourceId;
            AssetResolveResult art = resolve(artKey);
            AssetResolveResult icon = resolve(iconKey);
            boolean overlay = slot.kind == EntityKind.CARD && cardOverlay;
            out.add(
                    new DrawItem(
                            slot.slotId,
                            slot.kind.name(),
                            slot.refId,
                            x,
                            y,
                            size[0],
                            size[1],
                            scale,
                            snap.visible,
                            overlay,
                            art != null && art.found ? art.source : artKey,
                            art != null && art.found,
                            icon != null && icon.found ? icon.source : iconKey,
                            icon != null && icon.found,
                            snap.label,
                            snap.hp,
                            snap.maxHp,
                            snap.block));
        }
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromPresent();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(items.size()));
        m.put("cardOverlayOnly", Boolean.valueOf(cardOverlayOnly()));
        m.put("handPresentLevel", FullPresentMode.combatHandLevel().name());
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }

    /** Default draw size by kind (width, height) at scale 1. */
    public static float[] defaultSize(EntityKind kind, float scale) {
        float s = scale <= 0f ? 1f : scale;
        if (kind == EntityKind.CARD) {
            return new float[] {250f * s, 350f * s};
        }
        if (kind == EntityKind.RELIC) {
            return new float[] {64f * s, 64f * s};
        }
        if (kind == EntityKind.PLAYER || kind == EntityKind.MONSTER) {
            return new float[] {120f * s, 160f * s};
        }
        return new float[] {64f * s, 64f * s};
    }

    private static AssetResolveResult resolve(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        try {
            return ArtFramework.assets().resolve(key);
        } catch (Throwable t) {
            return null;
        }
    }
}
