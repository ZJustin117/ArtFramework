package artframework.render;

/**
 * Host-owned GL / capture execution. Pure bookkeeping stays on {@link RenderHost}.
 */
public interface HostRenderBackend {

    /**
     * Optional screen capture into the provided {@link FrameCapture}.
     *
     * @return true if capture succeeded or was unnecessary
     */
    boolean captureScreen(FrameCapture capture, int width, int height);

    /**
     * Draw one effect binding for a target. Implementations may use spriteBatch (host-typed).
     */
    void drawEffect(
            Effect effect,
            RenderTarget target,
            EffectBinding binding,
            RenderContext context);

    /** Capability: frame buffer capture available. */
    boolean supportsCapture();

    /** Capability: shader programs can be compiled/used. */
    boolean supportsShaders();
}
