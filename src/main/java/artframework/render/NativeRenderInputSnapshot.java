package artframework.render;

import artframework.component.Rect;

/** Immutable, host-neutral copy of one native render input. */
public final class NativeRenderInputSnapshot {
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

    NativeRenderInputSnapshot(NativeRenderInputComponent input) {
        this.nativeRenderFamily = input.nativeRenderFamily();
        this.ownerId = input.ownerId();
        this.phase = input.phase();
        this.z = input.z();
        this.stableKey = input.stableKey();
        Rect sourceBounds = input.bounds();
        this.bounds = new Rect(sourceBounds.x, sourceBounds.y, sourceBounds.width, sourceBounds.height);
        this.visible = input.visible();
        this.ownership = input.ownership();
        this.sceneId = input.sceneId();
        this.frameId = input.frameId();
        this.payload = input.payload();
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

    /** Optional payload copied from the source input; immutable and host-neutral. */
    public RenderPixelPayload payload() { return payload; }

    RenderOrder order() {
        return new RenderOrder(phase, z, stableKey);
    }
}
