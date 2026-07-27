package artframework.render;

/**
 * Default STS1-oriented backend: capture via {@link FrameCapture}, draw via {@link Effect#draw}.
 */
public final class DirectHostRenderBackend implements HostRenderBackend {

    public static final DirectHostRenderBackend INSTANCE = new DirectHostRenderBackend();

    private DirectHostRenderBackend() {}

    @Override
    public boolean captureScreen(FrameCapture capture, int width, int height) {
        if (capture == null) {
            return false;
        }
        try {
            capture.captureScreen(width, height);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void drawEffect(
            Effect effect, RenderTarget target, EffectBinding binding, RenderContext context) {
        if (effect != null) {
            effect.draw(target, binding, context);
        }
    }

    @Override
    public boolean supportsCapture() {
        return true;
    }

    @Override
    public boolean supportsShaders() {
        return true;
    }
}
