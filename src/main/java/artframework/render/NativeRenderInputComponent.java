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
    private final String sceneId;
    private final long frameId;
    private final RenderPixelPayload payload;

    public NativeRenderInputComponent(String nativeRenderFamily, String ownerId,
            RenderPhase phase, float z, String stableKey, Rect bounds, boolean visible,
            NativeRenderOwnership ownership) {
        this(nativeRenderFamily, ownerId, phase, z, stableKey, bounds, visible, ownership, "", -1L);
    }

    public NativeRenderInputComponent(String nativeRenderFamily, String ownerId,
            RenderPhase phase, float z, String stableKey, Rect bounds, boolean visible,
            NativeRenderOwnership ownership, String sceneId, long frameId) {
        this(nativeRenderFamily, ownerId, phase, z, stableKey, bounds, visible, ownership,
                sceneId, frameId, null);
    }

    /**
     * Full constructor. The optional {@code payload} is host-neutral pixel data only. Its
     * presence or absence never changes {@code ownership} and must never be read as delegated
     * pixel ownership.
     */
    public NativeRenderInputComponent(String nativeRenderFamily, String ownerId,
            RenderPhase phase, float z, String stableKey, Rect bounds, boolean visible,
            NativeRenderOwnership ownership, String sceneId, long frameId,
            RenderPixelPayload payload) {
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
        if (sceneId == null) throw new IllegalArgumentException("scene id required");
        if (frameId < -1L) throw new IllegalArgumentException("frame id must be -1 or non-negative");
        this.sceneId = sceneId;
        this.frameId = frameId;
        this.payload = payload;
    }

    public String nativeRenderFamily() { return nativeRenderFamily; }
    public String ownerId() { return ownerId; }
    public RenderPhase phase() { return phase; }
    public float z() { return z; }
    public String stableKey() { return stableKey; }
    public Rect bounds() { return new Rect(bounds.x, bounds.y, bounds.width, bounds.height); }
    public boolean visible() { return visible; }
    public NativeRenderOwnership ownership() { return ownership; }
    public String sceneId() { return sceneId; }
    public long frameId() { return frameId; }

    /** Optional host-neutral pixel payload; {@code null} means this input carries no pixels. */
    public RenderPixelPayload payload() { return payload; }

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
