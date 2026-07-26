package spireui.render;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Region blur using screen capture + blur shader. Falls back to soft tint without capture.
 */
public final class BlurEffect implements Effect {

    public static final String ID = "blur";
    public static final String SHADER_ID = "blur";

    private final ShaderRuntime shaderRuntime;

    public BlurEffect(ShaderRuntime shaderRuntime) {
        this.shaderRuntime = shaderRuntime;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String shaderId() {
        return SHADER_ID;
    }

    @Override
    public boolean requiresCapture() {
        return true;
    }

    @Override
    public void validate(EffectBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("binding required");
        }
        float radius = binding.paramFloat("radius", 2f);
        if (radius < 0f || radius > 16f) {
            throw new IllegalArgumentException("radius must be 0..16");
        }
        float alpha = binding.paramFloat("alpha", 1f);
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("alpha must be 0..1");
        }
    }

    @Override
    public void draw(RenderTarget target, EffectBinding binding, RenderContext ctx) {
        if (binding == null || !binding.isEnabled() || ctx == null) {
            return;
        }
        float radius = binding.paramFloat("radius", 2f);
        float alpha = binding.paramFloat("alpha", 1f);
        float screenW = binding.paramFloat("screenW", target.width() > 0 ? target.x() + target.width() : 1920f);
        float screenH = binding.paramFloat("screenH", target.height() > 0 ? target.y() + target.height() : 1080f);
        // Prefer full-frame size if target is full screen-ish
        if (target.kind == RenderTargetKind.FULL_FRAME) {
            screenW = target.width();
            screenH = target.height();
        }

        if (ctx.hasCapture()) {
            FrameCapture cap = ctx.capture;
            ShaderProgram program =
                    shaderRuntime != null ? shaderRuntime.get(SHADER_ID) : null;
            EffectDraw.drawCaptureRegion(
                    ctx.spriteBatch,
                    target,
                    cap.texture(),
                    cap.width(),
                    cap.height(),
                    screenW > 0 ? screenW : cap.width(),
                    screenH > 0 ? screenH : cap.height(),
                    program,
                    radius,
                    0f,
                    ctx.timeSeconds,
                    alpha);
            return;
        }
        // Fallback: milky overlay
        EffectDraw.fill(ctx.spriteBatch, target, 0.7f, 0.75f, 0.85f, 0.25f * alpha);
    }
}
