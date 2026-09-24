package artframework.sts1.render;

import artframework.render.RenderPixelPayload;
import artframework.render.RenderPlan;
import artframework.vfx.VfxDrawList;
import artframework.vfx.VfxParticleDraw;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Narrow host boundary for immutable VFX draw data. Native STS pixels are never suppressed. */
public final class Sts1VfxOverlayRenderer {
    public interface TextureResolver { Texture resolve(String bundleRelativePath); }
    private Sts1VfxOverlayRenderer() {}

    /**
     * Host-free draw parameters for one VFX item: everything {@link SpriteBatch#draw} needs except
     * the resolved texture. Computing these is pure so the payload path can be compared field for
     * field against the legacy draw path without touching GL.
     */
    static final class DrawParams {
        final float x, y, originX, originY, width, height, scaleX, scaleY, rotation;
        final int sourceX, sourceY, sourceWidth, sourceHeight;
        final float r, g, b, a;
        final String blendMode;
        final boolean flipX, flipY;

        DrawParams(float x, float y, float originX, float originY, float width, float height,
                float scaleX, float scaleY, float rotation,
                int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                float r, float g, float b, float a, String blendMode,
                boolean flipX, boolean flipY) {
            this.x = x; this.y = y;
            this.originX = originX; this.originY = originY;
            this.width = width; this.height = height;
            this.scaleX = scaleX; this.scaleY = scaleY; this.rotation = rotation;
            this.sourceX = sourceX; this.sourceY = sourceY;
            this.sourceWidth = sourceWidth; this.sourceHeight = sourceHeight;
            this.r = r; this.g = g; this.b = b; this.a = a;
            this.blendMode = blendMode;
            this.flipX = flipX; this.flipY = flipY;
        }
    }

    /**
     * Submits one payload-bearing frame entry list (already ordered by the caller). Entries with a
     * {@code null} payload are skipped so identity/geometry-only entries stay native-drawn. The
     * {@link TextureResolver} is the only host boundary: no {@link Texture} ever enters the payload.
     */
    public static void render(SpriteBatch batch, List<RenderPlan.Entry> entries,
            TextureResolver resolver) {
        if (batch == null || entries == null || resolver == null) return;
        Color previousColor = new Color(batch.getColor());
        int previousSrc = batch.getBlendSrcFunc();
        int previousDst = batch.getBlendDstFunc();
        try {
            for (RenderPlan.Entry entry : entries) {
                if (entry == null || entry.payload == null) continue;
                try {
                    RenderPixelPayload payload = entry.payload;
                    Texture texture = resolver.resolve(payload.resourceId());
                    if (texture == null) continue;
                    DrawParams p = params(payload, texture.getWidth(), texture.getHeight());
                    int[] blend = blendFunctions(p.blendMode);
                    batch.setBlendFunction(blend[0], blend[1]);
                    batch.setColor(p.r, p.g, p.b, p.a);
                    batch.draw(texture, p.x, p.y, p.originX, p.originY, p.width, p.height,
                            p.scaleX, p.scaleY, p.rotation,
                            p.sourceX, p.sourceY, p.sourceWidth, p.sourceHeight,
                            p.flipX, p.flipY);
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) {
        } finally {
            batch.setBlendFunction(previousSrc, previousDst);
            batch.setColor(previousColor);
        }
    }

    /**
     * Legacy direct-draw entry, retained for existing callers and tests. Production VFX now renders
     * from {@link RenderPixelPayload} via {@link #render(SpriteBatch, List, TextureResolver)}.
     *
     * @deprecated use the payload entry point; the pixel result is identical.
     */
    @Deprecated
    public static void render(SpriteBatch batch, VfxDrawList list, TextureResolver resolver) {
        if (batch == null || list == null || resolver == null) return;
        Color previousColor = new Color(batch.getColor());
        int previousSrc = batch.getBlendSrcFunc();
        int previousDst = batch.getBlendDstFunc();
        try {
            for (VfxParticleDraw draw : orderedDraws(list)) {
                try {
                    Texture texture = resolver.resolve(draw.textureReference);
                    if (texture == null) continue;
                    DrawParams p = params(draw, texture.getWidth(), texture.getHeight());
                    int[] blend = blendFunctions(p.blendMode);
                    batch.setBlendFunction(blend[0], blend[1]);
                    batch.setColor(p.r, p.g, p.b, p.a);
                    batch.draw(texture, p.x, p.y, p.originX, p.originY, p.width, p.height,
                            p.scaleX, p.scaleY, p.rotation,
                            p.sourceX, p.sourceY, p.sourceWidth, p.sourceHeight,
                            p.flipX, p.flipY);
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) {
        } finally {
            batch.setBlendFunction(previousSrc, previousDst);
            batch.setColor(previousColor);
        }
    }

    /**
     * Pure payload-to-geometry mapping. {@code payload.x()/y()} is the particle centre (the legacy
     * path drew at {@code x - tw/2} with origin {@code tw/2}); the texture region is derived from the
     * flipbook fields with the same integer division as the legacy path, not from the normalized UV
     * rectangle (which only agrees when the sheet divides evenly).
     */
    static DrawParams params(RenderPixelPayload payload, int textureWidth, int textureHeight) {
        if (payload == null) throw new IllegalArgumentException("payload required");
        return core(payload.x(), payload.y(), payload.scaleX(), payload.scaleY(),
                payload.rotationDegrees(), payload.r(), payload.g(), payload.b(), payload.a(),
                payload.blendMode(), payload.flipX(), payload.flipY(),
                payload.flipbookFrame(), payload.flipbookColumns(), payload.flipbookRows(),
                textureWidth, textureHeight);
    }

    /** Pure legacy draw-to-geometry mapping; must stay field-for-field identical to the payload path. */
    static DrawParams params(VfxParticleDraw draw, int textureWidth, int textureHeight) {
        if (draw == null) throw new IllegalArgumentException("draw required");
        return core(draw.x, draw.y, draw.scaleX, draw.scaleY, draw.rotationDegrees,
                draw.r, draw.g, draw.b, draw.alpha, draw.blendMode, draw.flipX, draw.flipY,
                draw.flipbookFrame, draw.flipbookColumns, draw.flipbookRows,
                textureWidth, textureHeight);
    }

    private static DrawParams core(float x, float y, float scaleX, float scaleY, float rotation,
            float r, float g, float b, float a, String blendMode, boolean flipX, boolean flipY,
            int flipbookFrame, int flipbookColumns, int flipbookRows,
            int textureWidth, int textureHeight) {
        int columns = Math.max(1, flipbookColumns);
        int rows = Math.max(1, flipbookRows);
        int tw = Math.max(1, textureWidth / columns);
        int th = Math.max(1, textureHeight / rows);
        int col = flipbookFrame % columns;
        int row = flipbookFrame / columns;
        return new DrawParams(
                x - tw * 0.5f, y - th * 0.5f, tw * 0.5f, th * 0.5f, tw, th,
                scaleX, scaleY, rotation,
                col * tw, row * th, tw, th,
                r, g, b, a, blendMode, flipX, flipY);
    }

    static List<VfxParticleDraw> orderedDraws(VfxDrawList list) {
        if (list == null || list.draws.isEmpty()) return Collections.emptyList();
        List<VfxParticleDraw> ordered = new ArrayList<VfxParticleDraw>(list.draws);
        Collections.sort(ordered, new Comparator<VfxParticleDraw>() {
            @Override public int compare(VfxParticleDraw a, VfxParticleDraw b) {
                return a.renderOrder == null || b.renderOrder == null
                        ? Integer.compare(a.particleIndex, b.particleIndex)
                        : artframework.render.RenderOrder.COMPARATOR.compare(
                                a.renderOrder, b.renderOrder);
            }
        });
        return Collections.unmodifiableList(ordered);
    }

    /** Pure blend-mode mapping to {@code {src,dst}}; unchanged from the legacy direct path. */
    static int[] blendFunctions(String blendMode) {
        if ("ADD".equals(blendMode)) {
            return new int[] { GL20.GL_SRC_ALPHA, GL20.GL_ONE };
        } else if ("PREMULT_ALPHA".equals(blendMode)) {
            return new int[] { GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA };
        } else if ("MUL".equals(blendMode)) {
            return new int[] { GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA };
        }
        return new int[] { GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA };
    }
}
