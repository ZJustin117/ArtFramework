package spireui.render;

/**
 * Where a {@link RenderTarget} is attached. FULL_FRAME reserved (default unavailable).
 */
public enum RenderTargetKind {
    SYNTHETIC_WINDOW,
    SYNTHETIC_WIDGET,
    ENTITY_SLOT,
    OVERLAY,
    /** Reserved for post-process; not drawn in v1 shell. */
    FULL_FRAME
}
