package artframework.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * SpriteBatch helpers for effects. Safe when batch is null or assets missing (no-op).
 */
public final class EffectDraw {

    private EffectDraw() {}

    public static void fill(Object batchObj, RenderTarget target, float r, float g, float b, float a) {
        fillWithShader(batchObj, target, null, 0f, r, g, b, a);
    }

    public static void fillExpanded(
            Object batchObj,
            RenderTarget target,
            float expand,
            float r,
            float g,
            float b,
            float a) {
        if (target == null) {
            return;
        }
        RenderTarget expanded = new RenderTarget(target.id + ":exp", target.kind);
        expanded.setBounds(
                target.x() - expand,
                target.y() - expand,
                target.width() + expand * 2f,
                target.height() + expand * 2f);
        fill(batchObj, expanded, r, g, b, a);
    }

    /**
     * Draw white square with optional shader. Restores previous ShaderProgram always.
     */
    public static void fillWithShader(
            Object batchObj,
            RenderTarget target,
            ShaderProgram shader,
            float intensity,
            float r,
            float g,
            float b,
            float a) {
        if (target == null || batchObj == null) {
            return;
        }
        if (!(batchObj instanceof SpriteBatch)) {
            return;
        }
        if (ImageMaster.WHITE_SQUARE_IMG == null) {
            return;
        }
        if (target.width() <= 0f || target.height() <= 0f) {
            return;
        }
        SpriteBatch sb = (SpriteBatch) batchObj;
        Color prev = sb.getColor();
        ShaderProgram prevShader = sb.getShader();
        try {
            if (shader != null && shader.isCompiled()) {
                sb.setShader(shader);
                if (shader.hasUniform("u_intensity")) {
                    shader.setUniformf("u_intensity", intensity);
                }
            }
            sb.setColor(r, g, b, a);
            sb.draw(
                    ImageMaster.WHITE_SQUARE_IMG,
                    target.x(),
                    target.y(),
                    target.width(),
                    target.height());
        } catch (Throwable ignored) {
        } finally {
            try {
                sb.setShader(prevShader);
            } catch (Throwable ignored) {
            }
            sb.setColor(prev);
        }
    }

    /**
     * Sample a region of a full-screen capture texture into {@code target} bounds.
     * UV assumes capture is the full screen in the same coordinate system as the target.
     */
    public static void drawCaptureRegion(
            Object batchObj,
            RenderTarget target,
            Texture capture,
            int captureW,
            int captureH,
            float screenW,
            float screenH,
            ShaderProgram shader,
            float radius,
            float tint,
            float timeSeconds,
            float alpha) {
        if (target == null || batchObj == null || capture == null) {
            return;
        }
        if (!(batchObj instanceof SpriteBatch)) {
            return;
        }
        if (target.width() <= 0f || target.height() <= 0f) {
            return;
        }
        if (captureW <= 0 || captureH <= 0 || screenW <= 0f || screenH <= 0f) {
            return;
        }

        // Map target rect (screen space) to capture UV. OpenGL copy is bottom-left origin.
        float u = target.x() / screenW;
        float v = target.y() / screenH;
        float u2 = (target.x() + target.width()) / screenW;
        float v2 = (target.y() + target.height()) / screenH;
        u = clamp01(u);
        v = clamp01(v);
        u2 = clamp01(u2);
        v2 = clamp01(v2);

        int srcX = (int) (u * captureW);
        int srcY = (int) (v * captureH);
        int srcW = Math.max(1, (int) ((u2 - u) * captureW));
        int srcH = Math.max(1, (int) ((v2 - v) * captureH));
        if (srcX + srcW > captureW) {
            srcW = captureW - srcX;
        }
        if (srcY + srcH > captureH) {
            srcH = captureH - srcY;
        }
        if (srcW <= 0 || srcH <= 0) {
            return;
        }

        SpriteBatch sb = (SpriteBatch) batchObj;
        Color prev = sb.getColor();
        ShaderProgram prevShader = sb.getShader();
        try {
            if (shader != null && shader.isCompiled()) {
                sb.setShader(shader);
                if (shader.hasUniform("u_texel")) {
                    shader.setUniformf("u_texel", 1f / captureW, 1f / captureH);
                }
                if (shader.hasUniform("u_radius")) {
                    shader.setUniformf("u_radius", radius);
                }
                if (shader.hasUniform("u_tint")) {
                    shader.setUniformf("u_tint", tint);
                }
                if (shader.hasUniform("u_time")) {
                    shader.setUniformf("u_time", timeSeconds);
                }
                if (shader.hasUniform("u_intensity")) {
                    shader.setUniformf("u_intensity", radius);
                }
            }
            sb.setColor(1f, 1f, 1f, alpha);
            // LibGDX Texture draw srcY is top-left in texture space for region API —
            // with glCopyTexImage2D bottom-left, flip Y for sampling.
            int flippedY = captureH - srcY - srcH;
            if (flippedY < 0) {
                flippedY = 0;
            }
            sb.draw(
                    capture,
                    target.x(),
                    target.y(),
                    target.width(),
                    target.height(),
                    srcX,
                    flippedY,
                    srcW,
                    srcH,
                    false,
                    false);
        } catch (Throwable ignored) {
        } finally {
            try {
                sb.setShader(prevShader);
            } catch (Throwable ignored) {
            }
            sb.setColor(prev);
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }
}
