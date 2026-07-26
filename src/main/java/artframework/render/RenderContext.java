package artframework.render;

/**
 * Per-frame draw context. {@link #capture} may be null when capture is off or failed.
 */
public final class RenderContext {

    public final Object spriteBatch;
    public final float timeSeconds;
    public final FrameCapture capture;

    public RenderContext(Object spriteBatch, float timeSeconds, FrameCapture capture) {
        this.spriteBatch = spriteBatch;
        this.timeSeconds = timeSeconds;
        this.capture = capture;
    }

    public boolean hasCapture() {
        return capture != null && capture.hasTexture();
    }
}
