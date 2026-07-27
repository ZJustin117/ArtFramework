package artframework.render;

/**
 * Test / headless backend: no capture, no draw.
 */
public final class NoOpHostRenderBackend implements HostRenderBackend {

    public static final NoOpHostRenderBackend INSTANCE = new NoOpHostRenderBackend();

    private NoOpHostRenderBackend() {}

    @Override
    public boolean captureScreen(FrameCapture capture, int width, int height) {
        return false;
    }

    @Override
    public void drawEffect(
            Effect effect, RenderTarget target, EffectBinding binding, RenderContext context) {
        // no-op
    }

    @Override
    public boolean supportsCapture() {
        return false;
    }

    @Override
    public boolean supportsShaders() {
        return false;
    }
}
