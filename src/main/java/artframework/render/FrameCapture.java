package artframework.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

/**
 * Captures the current default framebuffer into a texture for post-process sampling.
 * Prefer {@link #captureScreen()} (glCopyTexImage2D). Safe no-op without GL.
 */
public final class FrameCapture {

    private Texture texture;
    private FrameBuffer blurHorizontal;
    private FrameBuffer blurVertical;
    private SpriteBatch blurBatch;
    private int width;
    private int height;
    private boolean lastOk;
    private String lastError = "";
    private int captureScale = 1; // 1 = full res; 2 = half
    private int blurPasses = 2;
    private boolean blurReady;

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean hasTexture() {
        return texture != null && lastOk;
    }

    public Texture texture() {
        return texture;
    }

    /** Texture produced by the last Gaussian chain, or the raw capture when unavailable. */
    public Texture textureForEffects() {
        return blurReady && blurVertical != null
                ? blurVertical.getColorBufferTexture() : texture;
    }

    public boolean hasBlurredTexture() {
        return blurReady && blurVertical != null;
    }

    public boolean lastOk() {
        return lastOk;
    }

    public String lastError() {
        return lastError;
    }

    /** 1 = full resolution, 2 = half (faster blur). */
    public void setCaptureScale(int scale) {
        if (scale < 1) {
            scale = 1;
        }
        if (scale > 4) {
            scale = 4;
        }
        this.captureScale = scale;
    }

    public int captureScale() {
        return captureScale;
    }

    /** Number of horizontal/vertical pass pairs; clamped to keep the host cost bounded. */
    public void setBlurPasses(int passes) {
        blurPasses = Math.max(1, Math.min(4, passes));
        blurReady = false;
    }

    public int blurPasses() {
        return blurPasses;
    }

    /**
     * Ensure backing texture matches screen size / scale.
     */
    public void ensureSize(int screenW, int screenH) {
        if (screenW <= 0 || screenH <= 0) {
            return;
        }
        int tw = Math.max(1, screenW / captureScale);
        int th = Math.max(1, screenH / captureScale);
        if (texture != null && width == tw && height == th) {
            return;
        }
        disposeTextureOnly();
        disposeBlurResources();
        width = tw;
        height = th;
        try {
            if (Gdx.gl == null) {
                lastOk = false;
                lastError = "no GL";
                return;
            }
            texture = new Texture(width, height, Pixmap.Format.RGB888);
            texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            texture.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
            lastError = "";
        } catch (Throwable t) {
            texture = null;
            lastOk = false;
            lastError = t.getMessage() != null ? t.getMessage() : "texture create failed";
        }
    }

    /**
     * Copy current screen color buffer into {@link #texture()}.
     * Call after the game has drawn and SpriteBatch is ended.
     */
    public boolean captureScreen(int screenW, int screenH) {
        lastOk = false;
        blurReady = false;
        ensureSize(screenW, screenH);
        if (texture == null) {
            return false;
        }
        try {
            if (Gdx.gl == null) {
                lastError = "no GL";
                return false;
            }
            // Copy from default framebuffer (y may be flipped depending on platform)
            texture.bind();
            Gdx.gl.glCopyTexImage2D(
                    GL20.GL_TEXTURE_2D,
                    0,
                    GL20.GL_RGB,
                    0,
                    0,
                    width,
                    height,
                    0);
            lastOk = true;
            lastError = "";
            return true;
        } catch (Throwable t) {
            lastOk = false;
            lastError = t.getMessage() != null ? t.getMessage() : "capture failed";
            return false;
        }
    }

    /**
     * Run separable horizontal/vertical Gaussian passes over the current capture.
     * This is deliberately host-side and remains a safe no-op without a GL context.
     */
    public boolean prepareBlur(ShaderProgram shader, float radius) {
        blurReady = false;
        if (!hasTexture() || shader == null || !shader.isCompiled() || Gdx.gl == null) {
            return false;
        }
        try {
            ensureBlurBuffers();
            if (blurBatch == null) {
                blurBatch = new SpriteBatch();
            }
            Matrix4 projection = new Matrix4().setToOrtho2D(0f, 0f, width, height);
            blurBatch.setProjectionMatrix(projection);
            Texture input = texture;
            for (int pass = 0; pass < blurPasses; pass++) {
                blurHorizontal.begin();
                drawBlurPass(input, shader, radius, 1f, 0f);
                blurHorizontal.end();
                input = blurHorizontal.getColorBufferTexture();

                blurVertical.begin();
                drawBlurPass(input, shader, radius, 0f, 1f);
                blurVertical.end();
                input = blurVertical.getColorBufferTexture();
            }
            blurReady = true;
            return true;
        } catch (Throwable t) {
            blurReady = false;
            lastError = t.getMessage() != null ? t.getMessage() : "blur failed";
            return false;
        }
    }

    private void drawBlurPass(Texture input, ShaderProgram shader, float radius,
            float directionX, float directionY) {
        blurBatch.begin();
        blurBatch.setShader(shader);
        if (shader.hasUniform("u_texel")) {
            shader.setUniformf("u_texel", 1f / width, 1f / height);
        }
        if (shader.hasUniform("u_radius")) {
            shader.setUniformf("u_radius", Math.max(0f, radius));
        }
        if (shader.hasUniform("u_direction")) {
            shader.setUniformf("u_direction", directionX, directionY);
        }
        blurBatch.draw(input, 0f, 0f, width, height);
        blurBatch.setShader(null);
        blurBatch.end();
    }

    private void ensureBlurBuffers() {
        if (blurHorizontal != null && blurVertical != null
                && blurHorizontal.getWidth() == width && blurHorizontal.getHeight() == height) {
            return;
        }
        disposeBlurResources();
        blurHorizontal = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        blurVertical = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
    }

    public MapProbe probe() {
        return new MapProbe(this);
    }

    public void dispose() {
        disposeTextureOnly();
        disposeBlurResources();
    }

    private void disposeTextureOnly() {
        if (texture != null) {
            try {
                texture.dispose();
            } catch (Throwable ignored) {
            }
            texture = null;
        }
        width = 0;
        height = 0;
        lastOk = false;
        blurReady = false;
    }

    private void disposeBlurResources() {
        if (blurBatch != null) {
            try { blurBatch.dispose(); } catch (Throwable ignored) { }
            blurBatch = null;
        }
        if (blurHorizontal != null) {
            try { blurHorizontal.dispose(); } catch (Throwable ignored) { }
            blurHorizontal = null;
        }
        if (blurVertical != null) {
            try { blurVertical.dispose(); } catch (Throwable ignored) { }
            blurVertical = null;
        }
        blurReady = false;
    }

    /** Probe-friendly snapshot without exposing Texture. */
    public static final class MapProbe {
        public final boolean hasTexture;
        public final boolean lastOk;
        public final int width;
        public final int height;
        public final int scale;
        public final int blurPasses;
        public final boolean hasBlurredTexture;
        public final String lastError;

        MapProbe(FrameCapture c) {
            this.hasTexture = c.hasTexture();
            this.lastOk = c.lastOk();
            this.width = c.width();
            this.height = c.height();
            this.scale = c.captureScale();
            this.blurPasses = c.blurPasses();
            this.hasBlurredTexture = c.hasBlurredTexture();
            this.lastError = c.lastError();
        }
    }
}
