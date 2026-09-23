package artframework.render;

import artframework.component.Rect;

/** Immutable, host-neutral input describing one native render family item. */
public final class NativeRenderInputComponent {
    private final String nativeRenderFamily;
    private final String ownerId;
    private final RenderPhase phase;
    private final float z;
    private final String stableKey;
    private final Rect bounds;
    private final boolean visible;
    private final NativeRenderOwnership ownership;

    public NativeRenderInputComponent(String nativeRenderFamily, String ownerId,
            RenderPhase phase, float z, String stableKey, Rect bounds, boolean visible,
            NativeRenderOwnership ownership) {
        this.nativeRenderFamily = required(nativeRenderFamily, "native render family");
        this.ownerId = required(ownerId, "owner id");
        if (phase == null) throw new IllegalArgumentException("render phase required");
        if (Float.isNaN(z) || Float.isInfinite(z)) {
            throw new IllegalArgumentException("render z must be finite");
        }
        this.phase = phase;
        this.z = z;
        this.stableKey = required(stableKey, "stable key");
        if (bounds == null) throw new IllegalArgumentException("bounds required");
        requireFinite(bounds.x, "bounds x");
        requireFinite(bounds.y, "bounds y");
        requireFinite(bounds.width, "bounds width");
        requireFinite(bounds.height, "bounds height");
        if (bounds.width < 0f || bounds.height < 0f) {
            throw new IllegalArgumentException("bounds dimensions must be non-negative");
        }
        this.bounds = new Rect(bounds.x, bounds.y, bounds.width, bounds.height);
        this.visible = visible;
        if (ownership == null) throw new IllegalArgumentException("ownership required");
        this.ownership = ownership;
    }

    public String nativeRenderFamily() { return nativeRenderFamily; }
    public String ownerId() { return ownerId; }
    public RenderPhase phase() { return phase; }
    public float z() { return z; }
    public String stableKey() { return stableKey; }
    public Rect bounds() { return new Rect(bounds.x, bounds.y, bounds.width, bounds.height); }
    public boolean visible() { return visible; }
    public NativeRenderOwnership ownership() { return ownership; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " required");
        }
        return value.trim();
    }

    private static void requireFinite(float value, String name) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
