package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.OrbStanceView;
import artframework.sts1.backend.Sts1OrbStanceProjection;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Data-driven ART stance renderer (S2b-2a).
 *
 * <p>Readiness is derived from the observe-first {@link Sts1OrbStanceProjection}: an ART stance is
 * drawable only when the current projection carries a {@code kind == "stance"}, {@code visible}
 * entry with {@code hasImage == true} whose id matches the requested owner. {@link #render} then
 * reproduces the native {@code AbstractStance.render} pixel formula (additive blend, 256,256 origin,
 * unflipped {@code -angle} rotation) through the ART asset path. Any failure fails open: the method
 * returns {@code false} and the native stance render continues.
 */
public final class StanceArtRenderer {

    /**
     * Host-free draw parameters for one stance sprite: everything {@link SpriteBatch#draw} needs
     * except the resolved texture. Pure so the mapping can be compared field for field without GL.
     */
    static final class DrawParams {
        final float x, y, originX, originY, width, height, scaleX, scaleY, rotationDegrees;
        final int sourceX, sourceY, sourceWidth, sourceHeight;
        final float r, g, b, a;

        DrawParams(float x, float y, float originX, float originY, float width, float height,
                float scaleX, float scaleY, float rotationDegrees,
                int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                float r, float g, float b, float a) {
            this.x = x; this.y = y;
            this.originX = originX; this.originY = originY;
            this.width = width; this.height = height;
            this.scaleX = scaleX; this.scaleY = scaleY;
            this.rotationDegrees = rotationDegrees;
            this.sourceX = sourceX; this.sourceY = sourceY;
            this.sourceWidth = sourceWidth; this.sourceHeight = sourceHeight;
            this.r = r; this.g = g; this.b = b; this.a = a;
        }
    }

    private StanceArtRenderer() {}

    /**
     * True when the current projection holds a drawable stance entry for {@code ownerId}:
     * {@code kind == "stance"}, {@code visible}, {@code hasImage}, and an id that matches either
     * {@code ownerId} verbatim or {@code "stance:" + id}.
     */
    public static boolean isReady(String ownerId) {
        return find(ownerId) != null;
    }

    private static boolean isDrawable(OrbStanceView.Entry entry) {
        return entry != null && "stance".equals(entry.kind) && entry.visible && entry.hasImage;
    }

    /** First drawable stance entry matching {@code ownerId}, or {@code null}; never throws. */
    static OrbStanceView.Entry find(String ownerId) {
        if (ownerId == null) return null;
        try {
            for (OrbStanceView.Entry entry : Sts1OrbStanceProjection.current().entries) {
                if (!isDrawable(entry)) continue;
                if (matches(entry.id, ownerId)) return entry;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Accepts both {@code id == ownerId} and {@code ("stance:" + id) == ownerId}. */
    static boolean matches(String id, String ownerId) {
        if (id == null || ownerId == null) return false;
        if (id.equals(ownerId)) return true;
        return ("stance:" + id).equals(ownerId);
    }

    /**
     * Pure entry-to-geometry mapping. Mirrors native {@code AbstractStance.render}: draw the entry
     * size at its centre with origin {@code (256,256)}, uniform {@code scale}, rotation
     * {@code -angle}, and the full texture as source region.
     */
    static DrawParams params(OrbStanceView.Entry entry, int textureWidth, int textureHeight) {
        if (entry == null) throw new IllegalArgumentException("entry required");
        return new DrawParams(
                entry.centerX, entry.centerY, entry.originX, entry.originY,
                entry.width, entry.height, entry.scale, entry.scale, -entry.angle,
                0, 0, textureWidth, textureHeight,
                entry.colorR, entry.colorG, entry.colorB, entry.colorA);
    }

    /**
     * Draws the ART stance for {@code ownerId} if a drawable entry exists and its resource resolves
     * to a texture. Additive blend is installed for the sprite and restored to standard alpha blend
     * afterwards. Returns {@code false} without side effects (fail open) when the owner is not
     * drawable, the resource is missing, or anything throws.
     */
    public static boolean render(SpriteBatch sb, String ownerId) {
        if (sb == null) return false;
        Color previousColor = null;
        boolean restore = false;
        try {
            OrbStanceView.Entry entry = find(ownerId);
            if (entry == null) return false;
            Texture texture = artframework.sts1.assets.Sts1AssetMaterializer.resolveTexture(
                    ArtFramework.assets().resolve(entry.resourceId));
            if (texture == null) return false;
            DrawParams p = params(entry, texture.getWidth(), texture.getHeight());
            previousColor = new Color(sb.getColor());
            restore = true;
            sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            sb.setColor(p.r, p.g, p.b, p.a);
            sb.draw(texture, p.x, p.y, p.originX, p.originY, p.width, p.height,
                    p.scaleX, p.scaleY, p.rotationDegrees,
                    p.sourceX, p.sourceY, p.sourceWidth, p.sourceHeight, false, false);
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (restore) {
                try {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    sb.setColor(previousColor);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** Test-only no-op hook; readiness is derived from the projection. */
    public static void resetForTests() {
    }
}
