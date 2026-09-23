package artframework.render;

/** Host-neutral ownership disposition for a native render input. */
public enum NativeRenderOwnership {
    OBSERVED,
    NATIVE_WITH_ART_OVERLAY,
    DELEGATED_TO_ART
}
