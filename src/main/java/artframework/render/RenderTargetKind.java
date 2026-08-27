package artframework.render;

/**
 * Where a {@link RenderTarget} is attached. FULL_FRAME reserved (default unavailable).
 */
public enum RenderTargetKind {
    SYNTHETIC_WINDOW,
    SYNTHETIC_WIDGET,
    ENTITY_SLOT,
    /** C2 full-present surface region, drawn below the native replacement chrome. */
    C2_SURFACE,
    /** Kept for API compatibility; not used by production render paths after Slice E1. */
    OVERLAY,
    /** Reserved for post-process; not drawn in v1 shell. */
    FULL_FRAME
}
