package artframework.context;

/** Data-only native input gate decision. */
public final class NativeInterceptComponent {
    public final boolean processed;
    public final boolean suppressNative;
    public final String reason;

    public NativeInterceptComponent(boolean processed, boolean suppressNative, String reason) {
        this.processed = processed;
        this.suppressNative = suppressNative;
        this.reason = reason != null ? reason : "";
    }
}
