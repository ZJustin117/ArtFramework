package artframework.render;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Diagonal light-wave band. Shader path for the band; white border is drawn separately
 * (typically after scene2d so it is not covered by panel chrome).
 */
public final class LightwaveEffect implements Effect {

    public static final String ID = "lightwave";
    public static final String SHADER_ID = "lightwave";

    private final ShaderRuntime shaderRuntime;

    public LightwaveEffect() {
        this(null);
    }

    public LightwaveEffect(ShaderRuntime shaderRuntime) {
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
        float intensity = binding.paramFloat("intensity", 0.55f);
        if (intensity < 0f || intensity > 2f) {
            throw new IllegalArgumentException("intensity must be 0..2");
        }
        float width = binding.paramFloat("width", 0.18f);
        if (width <= 0f || width > 1f) {
            throw new IllegalArgumentException("width must be 0..1 exclusive of 0");
        }
    }

    public static boolean shouldDrawBorder(EffectBinding binding) {
        if (binding == null || !binding.isEnabled()) {
            return false;
        }
        float border = binding.paramFloat("border", 1f);
        return border > 0f;
    }

    /**
     * Map logical intensity 0..1 (slider) / up to 2 (pulse) to a strong on-screen band.
     */
    public static float visualIntensity(float logical) {
        if (logical <= 0f) {
            return 0f;
        }
        // Floor so mid slider is already visible; max is hot.
        float v = 0.35f + logical * 0.9f;
        if (v > 2.2f) {
            v = 2.2f;
        }
        return v;
    }

    @Override
    public void draw(RenderTarget target, EffectBinding binding, RenderContext ctx) {
        drawBand(target, binding, ctx);
        // Border is drawn in {@link #drawBorderOnly} after scene2d (see StageHost).
    }

    public void drawBand(RenderTarget target, EffectBinding binding, RenderContext ctx) {
        if (binding == null || !binding.isEnabled() || ctx == null || target == null) {
            return;
        }
        float logical = binding.paramFloat("intensity", 0.55f);
        float intensity = visualIntensity(logical);
        float angleDeg = binding.paramFloat("angle", 35f);
        float width = binding.paramFloat("width", 0.22f);
        float speed = binding.paramFloat("speed", 0.35f);
        // freeze>0: hold phase (one-shot flash without speeding the scroll)
        float freeze = binding.paramFloat("freeze", 0f);
        // Defaults align LightwaveTheme Lightwave/band + glow cool tint
        float r = binding.paramFloat("r", 0.55f);
        float g = binding.paramFloat("g", 0.88f);
        float b = binding.paramFloat("b", 1f);
        float time = ctx.timeSeconds;
        float phase;
        if (freeze > 0.5f) {
            phase = binding.paramFloat("phase", 0.5f);
        } else {
            phase = (time * speed) - (float) Math.floor(time * speed);
        }

        ShaderProgram program = null;
        if (shaderRuntime != null) {
            program = shaderRuntime.get(SHADER_ID);
        }
        // Batch alpha: allow full bright at high intensity
        float batchA = Math.min(1f, 0.45f + intensity * 0.4f);
        if (!LightwaveDiagnostics.forceFallback() && program != null && program.isCompiled()) {
            EffectDraw.fillWithLightwave(
                    ctx.spriteBatch,
                    target,
                    program,
                    intensity,
                    angleDeg,
                    width,
                    phase,
                    r,
                    g,
                    b,
                    batchA);
        } else {
            drawFallbackStrips(target, ctx, phase, angleDeg, width, intensity, r, g, b);
        }
    }

    /** Bounds stroke after scene2d; colors from binding (defaults match LightwaveTheme border). */
    public void drawBorderOnly(RenderTarget target, EffectBinding binding, RenderContext ctx) {
        if (!shouldDrawBorder(binding) || target == null || ctx == null) {
            return;
        }
        float bw = binding.paramFloat("borderWidth", 3f);
        if (bw < 2f) {
            bw = 3f;
        }
        float br = binding.paramFloat("borderR", 1f);
        float bg = binding.paramFloat("borderG", 1f);
        float bb = binding.paramFloat("borderB", 1f);
        float ba = binding.paramFloat("borderA", 0.95f);
        drawBorder(ctx, target, bw, br, bg, bb, ba);
    }

    private static void drawFallbackStrips(
            RenderTarget target,
            RenderContext ctx,
            float phase,
            float angleDeg,
            float width,
            float intensity,
            float r,
            float g,
            float b) {
        float tw = target.width();
        float th = target.height();
        if (tw <= 0f || th <= 0f) {
            return;
        }
        float band = Math.max(8f, Math.min(tw, th) * width);
        float travel = tw + th + band * 2f;
        float pos = phase * travel - band;
        double rad = Math.toRadians(angleDeg);
        float dx = (float) Math.cos(rad);
        float dy = (float) Math.sin(rad);
        int strips = 7;
        for (int i = 0; i < strips; i++) {
            float t = (i / (float) (strips - 1)) - 0.5f;
            float along = pos + t * band * 0.5f;
            float cx = target.x() + tw * 0.5f + dx * (along - travel * 0.5f);
            float cy = target.y() + th * 0.5f + dy * (along - travel * 0.5f);
            float alpha = (0.2f + intensity * 0.35f) * (1f - Math.abs(t) * 1.1f);
            if (alpha <= 0f) {
                continue;
            }
            if (alpha > 0.95f) {
                alpha = 0.95f;
            }
            RenderTarget strip = new RenderTarget(target.id + ":lw" + i, target.kind);
            float sw = band * (0.4f + intensity * 0.25f);
            float sh = Math.max(th, tw) * 1.2f;
            strip.setBounds(cx - sw * 0.5f, cy - sh * 0.5f, sw, sh);
            EffectDraw.fill(ctx.spriteBatch, strip, r, g, b, alpha);
        }
    }

    static void drawBorder(
            RenderContext ctx, RenderTarget target, float bw, float r, float g, float b, float a) {
        float x = target.x();
        float y = target.y();
        float w = target.width();
        float h = target.height();
        if (w <= 0f || h <= 0f) {
            return;
        }
        RenderTarget top = new RenderTarget(target.id + ":bt", target.kind);
        top.setBounds(x, y + h - bw, w, bw);
        EffectDraw.fill(ctx.spriteBatch, top, r, g, b, a);
        RenderTarget bottom = new RenderTarget(target.id + ":bb", target.kind);
        bottom.setBounds(x, y, w, bw);
        EffectDraw.fill(ctx.spriteBatch, bottom, r, g, b, a);
        RenderTarget left = new RenderTarget(target.id + ":bl", target.kind);
        left.setBounds(x, y, bw, h);
        EffectDraw.fill(ctx.spriteBatch, left, r, g, b, a);
        RenderTarget right = new RenderTarget(target.id + ":br", target.kind);
        right.setBounds(x + w - bw, y, bw, h);
        EffectDraw.fill(ctx.spriteBatch, right, r, g, b, a);
    }
}
