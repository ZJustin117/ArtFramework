package artframework.render;

/**
 * Soft color overlay (no shader). Fallback visual when GLSL unavailable.
 */
public final class TintEffect implements Effect {

    public static final String ID = "tint";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String shaderId() {
        return "";
    }

    @Override
    public boolean requiresCapture() {
        return false;
    }

    @Override
    public void validate(EffectBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("binding required");
        }
        float a = binding.paramFloat("alpha", 0.35f);
        if (a < 0f || a > 1f) {
            throw new IllegalArgumentException("alpha must be 0..1");
        }
    }

    @Override
    public void draw(RenderTarget target, EffectBinding binding, RenderContext ctx) {
        if (binding == null || !binding.isEnabled() || ctx == null) {
            return;
        }
        float a = binding.paramFloat("alpha", 0.35f);
        float r = binding.paramFloat("r", 0.2f);
        float g = binding.paramFloat("g", 0.35f);
        float b = binding.paramFloat("b", 0.6f);
        EffectDraw.fill(ctx.spriteBatch, target, r, g, b, a);
    }
}
