package artframework.core;

/** Stable routes for framework-owned transient runtime commands. */
public final class TransientSignalPaths {
    public static final String SURFACE_LIFECYCLE = "transient/surface/lifecycle";
    public static final String SURFACE_INTENT = "transient/surface/intent";
    public static final String NATIVE_INTENT_LIFECYCLE = "transient/native/intent_lifecycle";
    public static final String AUTHORITY_FRAME = "transient/authority/frame";
    public static final String AUTHORITY_BUSINESS_CONFIRMATION =
            "transient/authority/business_confirmation";

    private TransientSignalPaths() {}
}
