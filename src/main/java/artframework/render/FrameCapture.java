package artframework.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

/**
 * Captures the current default framebuffer into a texture for post-process sampling.
 * Prefer {@link #captureScreen()} (glCopyTexImage2D). Safe no-op without GL.
 */
public final class FrameCapture {

    private Texture texture;
    private FrameBuffer scratch; // optional downscale path
    private int width;
    private int height;
    private boolean lastOk;
    private String lastError = "";
    private int captureScale = 1; // 1 = full res; 2 = half

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

    public MapProbe probe() {
        return new MapProbe(this);
    }

    public void dispose() {
        disposeTextureOnly();
        if (scratch != null) {
            try {
                scratch.dispose();
            } catch (Throwable ignored) {
            }
            scratch = null;
        }
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
    }

    /** Probe-friendly snapshot without exposing Texture. */
    public static final class MapProbe {
        public final boolean hasTexture;
        public final boolean lastOk;
        public final int width;
        public final int height;
        public final int scale;
        public final String lastError;

        MapProbe(FrameCapture c) {
            this.hasTexture = c.hasTexture();
            this.lastOk = c.lastOk();
            this.width = c.width();
            this.height = c.height();
            this.scale = c.captureScale();
            this.lastError = c.lastError();
        }
    }
}
