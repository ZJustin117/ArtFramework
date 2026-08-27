package artframework.sts1.render;

/** Immutable metadata linking a native presentation entity to its latest invocation. */
public final class NativeInvocationComponent {
    public final long invocationId;
    public final long frameId;
    public final String nativeClass;
    public final String nativeMethod;
    public final String surfaceFamily;
    public final String sourceIdentity;

    public NativeInvocationComponent(NativeRenderInvocation invocation) {
        if (invocation == null) throw new IllegalArgumentException("invocation required");
        invocationId = invocation.invocationId;
        frameId = invocation.frameId;
        nativeClass = invocation.nativeClass;
        nativeMethod = invocation.nativeMethod;
        surfaceFamily = invocation.surfaceFamily;
        sourceIdentity = invocation.sourceIdentity;
    }
}
