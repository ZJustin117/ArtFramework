package artframework.render;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Soft glow / edge emphasis. Uses compiled {@code glow} ShaderProgram when available;
 * otherwise expanded gold tint (works without GL compile).
 */
public final class GlowEffect implements Effect {

    public static final String ID = "glow";
    public static final String SHADER_ID = "glow";

    private final ShaderRuntime shaderRuntime;

    public GlowEffect() {
        this(null);
    }

    public GlowEffect(ShaderRuntime shaderRuntime) {
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
        return false;
    }

    @Override
    public void validate(EffectBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("binding required");
        }
        float intensity = binding.paramFloat("intensity", 0.5f);
        if (intensity < 0f || intensity > 2f) {
            throw new IllegalArgumentException("intensity must be 0..2");
        }
    }

    @Override
    public void draw(RenderTarget target, EffectBinding binding, RenderContext ctx) {
        if (binding == null || !binding.isEnabled() || ctx == null) {
            return;
        }
        float intensity = binding.paramFloat("intensity", 0.5f);
        float pulse = 0.85f + 0.15f * (float) Math.sin(ctx.timeSeconds * 3f);
        float alpha = Math.min(0.55f, 0.2f + intensity * 0.25f) * pulse;
        float expand = 4f + intensity * 8f;

        ShaderProgram program = null;
        if (shaderRuntime != null) {
            program = shaderRuntime.get(SHADER_ID);
        }
        if (program != null && program.isCompiled()) {
            EffectDraw.fillWithShader(
                    ctx.spriteBatch,
                    expandTarget(target, expand),
                    program,
                    intensity * pulse,
                    1f,
                    0.9f,
                    0.45f,
                    alpha);
            return;
        }
        EffectDraw.fillExpanded(ctx.spriteBatch, target, expand, 1f, 0.85f, 0.35f, alpha * 0.45f);
        EffectDraw.fill(ctx.spriteBatch, target, 1f, 0.9f, 0.45f, alpha * 0.25f);
    }

    private static RenderTarget expandTarget(RenderTarget target, float expand) {
        RenderTarget expanded = new RenderTarget(target.id + ":glow", target.kind);
        expanded.setBounds(
                target.x() - expand,
                target.y() - expand,
                target.width() + expand * 2f,
                target.height() + expand * 2f);
        return expanded;
    }
}
