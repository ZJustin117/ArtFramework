package artframework.sts1.render;

import artframework.vfx.VfxDrawList;
import artframework.vfx.VfxParticleDraw;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Narrow host boundary for immutable VFX draw data. Native STS pixels are never suppressed. */
public final class Sts1VfxOverlayRenderer {
    public interface TextureResolver { Texture resolve(String bundleRelativePath); }
    private Sts1VfxOverlayRenderer() {}

    public static void render(SpriteBatch batch, VfxDrawList list, TextureResolver resolver) {
        if (batch == null || list == null || resolver == null) return;
        Color previousColor = new Color(batch.getColor());
        int previousSrc = batch.getBlendSrcFunc();
        int previousDst = batch.getBlendDstFunc();
        try {
            for (VfxParticleDraw draw : list.draws) {
                try {
                    Texture texture = resolver.resolve(draw.textureReference);
                    if (texture == null) continue;
                    int tw = Math.max(1, texture.getWidth() / Math.max(1, draw.flipbookColumns));
                    int th = Math.max(1, texture.getHeight() / Math.max(1, draw.flipbookRows));
                    int col = draw.flipbookFrame % Math.max(1, draw.flipbookColumns);
                    int row = draw.flipbookFrame / Math.max(1, draw.flipbookColumns);
                    applyBlend(batch, draw.blendMode);
                    batch.setColor(draw.r, draw.g, draw.b, draw.alpha);
                    batch.draw(texture, draw.x - tw * 0.5f, draw.y - th * 0.5f,
                            tw * 0.5f, th * 0.5f, tw, th, draw.scaleX, draw.scaleY,
                            draw.rotationDegrees, col * tw, row * th, tw, th,
                            draw.flipX, draw.flipY);
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) {
        } finally {
            batch.setBlendFunction(previousSrc, previousDst);
            batch.setColor(previousColor);
        }
    }

    private static void applyBlend(SpriteBatch batch, String blendMode) {
        if ("ADD".equals(blendMode)) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        } else if ("PREMULT_ALPHA".equals(blendMode)) {
            batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        } else if ("MUL".equals(blendMode)) {
            batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }
}
