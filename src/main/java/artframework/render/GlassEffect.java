package artframework.render;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Liquid / frosted glass: capture + glass shader (blur + UV wobble + tint).
 * Falls back to translucent cool tint without capture/shader.
 */
public final class GlassEffect implements Effect {

    public static final String ID = "glass";
    public static final String SHADER_ID = "glass";

    private final ShaderRuntime shaderRuntime;

    public GlassEffect(ShaderRuntime shaderRuntime) {
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
        float radius = binding.paramFloat("radius", 2.5f);
        if (radius < 0f || radius > 16f) {
            throw new IllegalArgumentException("radius must be 0..16");
        }
        float tint = binding.paramFloat("tint", 0.45f);
        if (tint < 0f || tint > 1f) {
            throw new IllegalArgumentException("tint must be 0..1");
        }
        float alpha = binding.paramFloat("alpha", 0.92f);
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("alpha must be 0..1");
        }
    }

    @Override
    public void draw(RenderTarget target, EffectBinding binding, RenderContext ctx) {
        if (binding == null || !binding.isEnabled() || ctx == null) {
            return;
        }
        float radius = binding.paramFloat("radius", 2.5f);
        float tint = binding.paramFloat("tint", 0.45f);
        float alpha = binding.paramFloat("alpha", 0.92f);
        float screenW = target.width();
        float screenH = target.height();
        if (target.kind != RenderTargetKind.FULL_FRAME) {
            // Widget-local: screen size should be passed by host via params when available
            screenW = binding.paramFloat("screenW", 1920f);
            screenH = binding.paramFloat("screenH", 1080f);
        }

        if (ctx.hasCapture()) {
            FrameCapture cap = ctx.capture;
            ShaderProgram program =
                    shaderRuntime != null ? shaderRuntime.get(SHADER_ID) : null;
            EffectDraw.drawCaptureRegion(
                    ctx.spriteBatch,
                    target,
                    cap.textureForEffects(),
                    cap.width(),
                    cap.height(),
                    screenW > 0 ? screenW : cap.width(),
                    screenH > 0 ? screenH : cap.height(),
                    program,
                    radius,
                    tint,
                    ctx.timeSeconds,
                    alpha);
            // subtle glass plate on top
            EffectDraw.fill(ctx.spriteBatch, target, 0.8f, 0.88f, 1f, 0.08f * alpha);
            return;
        }
        EffectDraw.fill(ctx.spriteBatch, target, 0.65f, 0.78f, 0.95f, 0.35f * alpha);
        EffectDraw.fillExpanded(ctx.spriteBatch, target, 2f, 1f, 1f, 1f, 0.08f * alpha);
    }
}
