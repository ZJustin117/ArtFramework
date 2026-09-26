package artframework.sts1.render;

import artframework.assets.AtlasRegion;
import artframework.sts1.assets.Sts1GdxAtlasRegions;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.lang.reflect.Field;

/**
 * STS1 host-side {@link AuraArtRenderer.Adapter} for the {@code vfx-stance-aura} family.
 *
 * <p>F2b1 shipped the two host-free halves of the real renderer: the readiness predicate
 * ({@link #isReady}, backed by the exact-FQN {@link AuraDrawGeometry#kindFor}) and the reflective
 * field reader ({@link #readFields}) that snapshots the native effect's own draw inputs. F2b2
 * completes the renderer: {@link #render} converts the effect's own live {@code img} region to the
 * host-neutral {@link AtlasRegion} (via {@link Sts1GdxAtlasRegions#fromGdx}), resolves the native
 * draw arguments through {@link AuraDrawGeometry#params}, and replays the native additive
 * {@code SpriteBatch.draw} with color/blend save-restore. A false result still means "no pixels
 * produced" so the caller fails open to the native draw.
 *
 * <p>Reflection is confined to reading the native effect's own fields (no patch, no host mutation),
 * mirrors the existing soft reflective conventions in {@code artframework.sts1}, and always fails
 * open: missing, unreadable, or wrongly typed fields yield {@code null} (or {@code false}) rather
 * than an exception, so the caller continues with the native draw.
 */
public final class Sts1AuraArtRenderer implements AuraArtRenderer.Adapter {

    /** Sentinel distinguishing "field absent/unreadable" from a legitimately null field value. */
    private static final Object MISSING = new Object();

    /**
     * Immutable snapshot of the fields one aura draw needs, or {@code null} when any required field
     * is missing.
     */
    static final class Fields {
        final float x;
        final float y;
        final float vY;
        final float scale;
        final float rotation;
        final float durDiv2;
        final float duration;
        final Color color;
        final TextureAtlas.AtlasRegion img;

        Fields(float x, float y, float vY, float scale, float rotation, float durDiv2,
                float duration, Color color, TextureAtlas.AtlasRegion img) {
            this.x = x;
            this.y = y;
            this.vY = vY;
            this.scale = scale;
            this.rotation = rotation;
            this.durDiv2 = durDiv2;
            this.duration = duration;
            this.color = color;
            this.img = img;
        }
    }

    /** True only when the exact native class is one of the three claimable aura FQNs. */
    @Override
    public boolean isReady(String nativeClassName) {
        try {
            return AuraDrawGeometry.kindFor(nativeClassName) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Snapshots the native effect's own draw fields, walking {@code getClass()} up through the
     * superclass chain (guarded by {@code getDeclaredField}+{@code setAccessible(true)}) and stopping
     * at {@link Object}.
     *
     * <p>Required: {@code x}, {@code y}, {@code vY}, {@code scale}, {@code rotation}, {@code color}
     * ({@link Color}), and {@code img} ({@link TextureAtlas.AtlasRegion}); {@code scale}/{@code
     * rotation} are read as any {@link Number} (primitive {@code float} boxes). {@code dur_div2} and
     * {@code duration} are optional and default to {@code 0}. Returns {@code null} when the effect is
     * null or any required field is absent, unreadable, or of the wrong type; never throws.
     */
    static Fields readFields(Object effect) {
        if (effect == null) return null;
        try {
            Float x = readFloat(effect, "x");
            Float y = readFloat(effect, "y");
            Float vY = readFloat(effect, "vY");
            Float scale = readFloat(effect, "scale");
            Float rotation = readFloat(effect, "rotation");
            if (x == null || y == null || vY == null || scale == null || rotation == null) {
                return null;
            }
            Object color = readRaw(effect, "color");
            Object img = readRaw(effect, "img");
            if (!(color instanceof Color)) return null;
            if (!(img instanceof TextureAtlas.AtlasRegion)) return null;
            return new Fields(x, y, vY, scale, rotation,
                    optionalFloat(effect, "dur_div2"), optionalFloat(effect, "duration"),
                    (Color) color, (TextureAtlas.AtlasRegion) img);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Draws the real ART atlas sprite for a claimed {@code vfx-stance-aura} effect, reproducing the
     * native additive draw: region from the effect's own live {@code img} through
     * {@link Sts1GdxAtlasRegions#fromGdx}, geometry from {@link AuraDrawGeometry#params}, color set
     * from the effect's own {@link Color} (or white), additive blend {@code (SRC_ALPHA, ONE)}, and
     * blend/color restored to the previous state afterwards.
     *
     * <p>The draw uses the {@code SpriteBatch#draw(TextureRegion, ...)} overload with the same
     * arguments the native call passes, so libGDX resolves the region's UV rect (including any
     * atlas {@code rotate} baking) exactly as the native effect does; the raw texture+src overload
     * would bypass that and is intentionally not used.
     *
     * <p>Never throws. Returns {@code true} only after a real draw; any null input, unmapped class,
     * unreadable field, missing/invalid region, or host failure returns {@code false} without side
     * effects so the caller fails open to the native draw.
     */
    @Override
    public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
        if (sb == null || effect == null) return false;
        try {
            AuraDrawGeometry.Kind kind =
                    AuraDrawGeometry.kindFor(effect.getClass().getName());
            if (kind == null) return false;
            Fields f = readFields(effect);
            if (f == null) return false;
            TextureAtlas.AtlasRegion gdx = f.img;
            if (gdx == null || gdx.getTexture() == null) return false;
            AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
            if (neutral == null || !neutral.valid()) return false;
            float settingsScale = Settings.scale;
            AuraDrawGeometry.Params p = AuraDrawGeometry.params(
                    kind, f.x, f.y, f.vY, f.scale, f.rotation, f.durDiv2, f.duration,
                    settingsScale, gdx.getRegionWidth(), gdx.getRegionHeight());
            Color previous = new Color(sb.getColor());
            try {
                sb.setColor(f.color != null ? f.color : Color.WHITE);
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                sb.draw(gdx, p.x, p.y, p.originX, p.originY, p.width, p.height,
                        p.scaleX, p.scaleY, p.rotation);
                return true;
            } finally {
                try {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    sb.setColor(previous);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Reads one field walking the superclass chain, or {@link #MISSING} when absent/unreadable. */
    private static Object readRaw(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                return MISSING;
            }
        }
        return MISSING;
    }

    /** Required float: any {@link Number}, or {@code null} when absent/unreadable/wrong type. */
    private static Float readFloat(Object target, String name) {
        Object raw = readRaw(target, name);
        return raw instanceof Number ? ((Number) raw).floatValue() : null;
    }

    /** Optional float: absent/unreadable/wrong type collapses to {@code 0}. */
    private static float optionalFloat(Object target, String name) {
        Object raw = readRaw(target, name);
        return raw instanceof Number ? ((Number) raw).floatValue() : 0f;
    }
}
