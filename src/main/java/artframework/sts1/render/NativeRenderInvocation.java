package artframework.sts1.render;

import artframework.component.Rect;

/** Immutable observation of one STS1 native display invocation. */
public final class NativeRenderInvocation {
    public final long invocationId;
    public final long frameId;
    public final String scene;
    public final String ownerId;
    public final String nativeClass;
    public final String nativeMethod;
    public final String surfaceFamily;
    public final String sourceIdentity;
    public final Rect boundsHint;

    public NativeRenderInvocation(long invocationId, long frameId, String scene, String ownerId,
            String nativeClass, String nativeMethod, String surfaceFamily,
            String sourceIdentity, Rect boundsHint) {
        this.invocationId = invocationId;
        this.frameId = frameId;
        this.scene = value(scene);
        this.ownerId = value(ownerId);
        this.nativeClass = value(nativeClass);
        this.nativeMethod = value(nativeMethod);
        this.surfaceFamily = value(surfaceFamily);
        this.sourceIdentity = value(sourceIdentity);
        this.boundsHint = boundsHint != null ? boundsHint : Rect.ZERO;
    }

    private static String value(String value) {
        return value != null ? value : "";
    }
}
