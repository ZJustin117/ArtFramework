package artframework.c2;

import artframework.assets.ResourceIds;
import artframework.component.Rect;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dependency-neutral observe-first anchor for a host entity instance.
 *
 * <p>The anchor describes where native creature pixels are expected to be; it is not, by itself,
 * an ART pixel ownership claim.  {@link #claimed} is true only when a per-instance skeleton claim
 * has a live ART replacement ready for the exact native skeleton slot.</p>
 */
public final class EntityAnchorView {
    public final String entityId;
    public final EntityKind kind;
    public final String name;
    public final float x;
    public final float y;
    public final Rect bounds;
    public final boolean visible;
    public final boolean claimed;
    public final String assetId;

    public EntityAnchorView(String entityId, EntityKind kind, String name, float x, float y,
            Rect bounds, boolean visible, boolean claimed, String assetId) {
        if (entityId == null || entityId.trim().isEmpty()) {
            throw new IllegalArgumentException("entityId required");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind required");
        }
        this.entityId = entityId;
        this.kind = kind;
        this.name = name != null ? name : "";
        this.x = x;
        this.y = y;
        this.bounds = bounds != null ? bounds : Rect.ZERO;
        this.visible = visible;
        this.claimed = claimed;
        this.assetId = validAssetId(assetId) ? assetId : ResourceIds.CHAR_UNKNOWN;
    }

    public EntityAnchorView withClaimed(boolean value) {
        return new EntityAnchorView(entityId, kind, name, x, y, bounds, visible, value, assetId);
    }

    /** C2 chrome projection: never uses native creature art as ART-owned pixels. */
    public EntitySnapshot toSnapshot() {
        Map<String, Object> extras = new LinkedHashMap<String, Object>();
        extras.put("anchorEntityId", entityId);
        extras.put("anchorKind", kind.name());
        extras.put("anchorName", name);
        extras.put("anchorX", Float.valueOf(x));
        extras.put("anchorY", Float.valueOf(y));
        extras.put("anchorBoundsX", Float.valueOf(bounds.x));
        extras.put("anchorBoundsY", Float.valueOf(bounds.y));
        extras.put("anchorBoundsW", Float.valueOf(bounds.width));
        extras.put("anchorBoundsH", Float.valueOf(bounds.height));
        extras.put("anchorVisible", Boolean.valueOf(visible));
        extras.put("anchorClaimed", Boolean.valueOf(claimed));
        extras.put("anchorAssetId", assetId);
        extras.put("nativePixelsAuthoritative", Boolean.valueOf(!claimed));
        return new EntitySnapshot(x, y, 1f, visible,
                "", "", name, 0, 0, 0, "", extras);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("entityId", entityId);
        m.put("kind", kind.name());
        m.put("name", name);
        m.put("x", Float.valueOf(x));
        m.put("y", Float.valueOf(y));
        Map<String, Object> b = new LinkedHashMap<String, Object>();
        b.put("x", Float.valueOf(bounds.x));
        b.put("y", Float.valueOf(bounds.y));
        b.put("w", Float.valueOf(bounds.width));
        b.put("h", Float.valueOf(bounds.height));
        m.put("bounds", b);
        m.put("visible", Boolean.valueOf(visible));
        m.put("claimed", Boolean.valueOf(claimed));
        m.put("assetId", assetId);
        return m;
    }

    private static boolean validAssetId(String assetId) {
        return assetId != null && !assetId.isEmpty() && ResourceIds.isValid(assetId);
    }
}
