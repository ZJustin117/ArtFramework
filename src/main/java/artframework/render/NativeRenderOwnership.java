package artframework.render;

/** Host-neutral ownership disposition recorded on a native render input. */
public enum NativeRenderOwnership {
    /** Observation only; this value never grants ART pixel ownership. */
    OBSERVED,
    /** Native pixels remain host-owned; ART may record an overlay input. */
    NATIVE_WITH_ART_OVERLAY,
    /** The only disposition that declares ART pixel ownership for this input. */
    DELEGATED_TO_ART
}
