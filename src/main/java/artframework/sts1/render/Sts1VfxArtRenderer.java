package artframework.sts1.render;

import artframework.assets.AtlasRegion;
import artframework.sts1.assets.Sts1GdxAtlasRegions;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.lang.reflect.Field;

/**
 * STS1 host-side {@link VfxArtRenderer.Adapter} for the family-neutral per-instance transient-effect
 * claim seam (current members are the {@code vfx-stance-aura} FQNs plus the {@code vfx-scene-world}
 * {@code LightFlareSEffect}/{@code LightFlareMEffect}/{@code LightFlareLEffect}/
 * {@code TorchParticleLEffect}/{@code TorchParticleXLEffect}/{@code CeilingDustCloudEffect}, the
 * {@code vfx-misc-root}
 * {@code FireBurstParticleEffect}/{@code NemesisFireParticle}/{@code GhostlyWeakFireEffect}/
 * {@code GenericSmokeEffect}/{@code ExhaustBlurEffect}, and the {@code vfx-combat}
 * {@code FlashAtkImgEffect}/{@code RedFireBurstParticleEffect}/{@code SmokeBlurEffect}, plus the four
 * bare-{@code Texture} members {@code ShieldParticleEffect}, {@code DebuffParticleEffect},
 * {@code IceShatterEffect}, and {@code WebParticleEffect}).
 *
 * <p>F2b1 shipped the two host-free halves of the real renderer: the readiness predicate
 * ({@link #isReady}, backed by the exact-FQN {@link VfxDrawGeometry#kindFor}) and the reflective
 * field reader ({@link #readFields}) that snapshots the native effect's own draw inputs. F2b2
 * completes the renderer: {@link #render} converts the effect's own live {@code img} region to the
 * host-neutral {@link AtlasRegion} (via {@link Sts1GdxAtlasRegions#fromGdx}), resolves the native
 * draw arguments through {@link VfxDrawGeometry#params}, and replays the native
 * {@code SpriteBatch.draw} with color save-restore and a per-kind blend policy
 * ({@link VfxDrawGeometry#additiveBlend}): additive for most kinds, but ambient — no
 * {@code setBlendFunction} call at all — for the ambient kinds ({@code FlashAtkImgEffect},
 * {@code SmokeBlurEffect}, {@code CeilingDustCloudEffect}, {@code NemesisFireParticle},
 * {@code GenericSmokeEffect}, {@code ExhaustBlurEffect}).
 * {@code CalmParticleEffect} has no
 * {@code img} and draws the bare {@link ImageMaster#FROST_ACTIVATE_VFX_1} {@link Texture}, so its
 * own branch resolves that texture and uses the raw texture + source-rect draw overload.
 * {@code ShieldParticleEffect} and {@code DebuffParticleEffect} are the second and third
 * bare-{@code Texture} kinds: they share that generalized fixed-source-rect path, with Shield
 * resolving the static {@link ImageMaster#INTENT_DEFEND} {@code Texture} additively (rotation
 * hardcoded {@code 0}) and Debuff resolving its own instance {@code Texture} {@code img} under the
 * ambient blend (consuming its {@code rotation} field). A false
 * result still means "no pixels produced" so the caller fails open to the native draw.
 *
 * <p>Reflection is confined to reading the native effect's own fields (no patch, no host mutation),
 * mirrors the existing soft reflective conventions in {@code artframework.sts1}, and always fails
 * open: missing, unreadable, or wrongly typed fields yield {@code null} (or {@code false}) rather
 * than an exception, so the caller continues with the native draw.
 */
public final class Sts1VfxArtRenderer implements VfxArtRenderer.Adapter {

    /** Sentinel distinguishing "field absent/unreadable" from a legitimately null field value. */
    private static final Object MISSING = new Object();

    /**
     * Immutable snapshot of the fields one claim draw needs, or {@code null} when any required field
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

    /** True only when the exact native class is one of the claimable seam FQNs. */
    @Override
    public boolean isReady(String nativeClassName) {
        try {
            return VfxDrawGeometry.kindFor(nativeClassName) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Immutable snapshot of the fields a bare-{@code Texture} draw needs ({@code CalmParticleEffect}
     * / {@code ShieldParticleEffect} / {@code DebuffParticleEffect} / {@code IceShatterEffect} /
     * {@code WebParticleEffect}). {@code rotation} is required for the kinds whose formula consumes
     * it (Calm, Debuff, IceShatter) and optional (defaulting to {@code 0}) for Shield and Web, which
     * hardcode {@code 0f}; {@code img} is a {@link Texture} for Debuff and IceShatter and
     * {@code null} otherwise (Web resolves the static {@link ImageMaster#WEB_VFX}).
     */
    static final class TextureFields {
        final float x;
        final float y;
        final float scale;
        final float rotation;
        final float durDiv2;
        final float duration;
        final Color color;
        final Texture img;

        TextureFields(float x, float y, float scale, float rotation, float durDiv2, float duration,
                Color color, Texture img) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotation = rotation;
            this.durDiv2 = durDiv2;
            this.duration = duration;
            this.color = color;
            this.img = img;
        }
    }

    /**
     * Snapshots the native effect's own draw fields, walking {@code getClass()} up through the
     * superclass chain (guarded by {@code getDeclaredField}+{@code setAccessible(true)}) and stopping
     * at {@link Object}.
     *
     * <p>Required: {@code x}, {@code y}, {@code scale}, {@code rotation}, {@code color}
     * ({@link Color}), and {@code img} ({@link TextureAtlas.AtlasRegion}); {@code scale}/{@code
     * rotation} are read as any {@link Number} (primitive {@code float} boxes). {@code dur_div2} and
     * {@code duration} are always optional and default to {@code 0}. {@code vY} is required only for
     * the kinds whose {@link VfxDrawGeometry#params} formula consumes it ({@code WRATH_PARTICLE},
     * {@code DIVINITY_PARTICLE}); for the kinds that ignore it ({@code STANCE_AURA},
     * {@code DIVINITY_STANCE_CHANGE}, {@code LIGHT_FLARE}, {@code LIGHT_FLARE_M},
     * {@code LIGHT_FLARE_L}, {@code TORCH_PARTICLE_L}, {@code FLASH_ATK_IMG}, and the nine newest
     * members {@code FIRE_BURST}, {@code RED_FIRE_BURST}, {@code SMOKE_BLUR}, {@code CEILING_DUST},
     * {@code NEMESIS_FIRE}, {@code TORCH_PARTICLE_XL}, {@code GHOSTLY_WEAK_FIRE},
     * {@code GENERIC_SMOKE}, {@code EXHAUST_BLUR}) it is optional and
     * defaults to {@code 0}, which is required because {@code DivinityStanceChangeParticle},
     * {@code LightFlareSEffect}, {@code FlashAtkImgEffect}, and {@code LightFlareMEffect}/
     * {@code LightFlareLEffect} have no {@code vY} field — and {@code TorchParticleLEffect} and all
     * nine newer members have one
     * but never read it in {@code render} (it is update-only). Returns {@code null}
     * when the effect is null or any required field is absent, unreadable, or of the wrong type;
     * never throws.
     */
    static Fields readFields(VfxDrawGeometry.Kind kind, Object effect) {
        if (effect == null) return null;
        // vY is only meaningful for the formulas that add it to y; requiring it elsewhere would
        // wrongly reject DivinityStanceChangeParticle/LightFlareSEffect and LightFlareMEffect/
        // LightFlareLEffect (no vY field) and TorchParticleLEffect plus the nine newest members
        // (their vY is update-only), and omitting it where it is consumed would silently draw at an
        // un-shifted y instead of failing open to the native draw.
        boolean requireVY = kind == VfxDrawGeometry.Kind.WRATH_PARTICLE
                || kind == VfxDrawGeometry.Kind.DIVINITY_PARTICLE;
        try {
            Float x = readFloat(effect, "x");
            Float y = readFloat(effect, "y");
            Float scale = readFloat(effect, "scale");
            Float rotation = readFloat(effect, "rotation");
            if (x == null || y == null || scale == null || rotation == null) {
                return null;
            }
            Float vY = readFloat(effect, "vY");
            if (requireVY && vY == null) {
                return null;
            }
            Object color = readRaw(effect, "color");
            Object img = readRaw(effect, "img");
            if (!(color instanceof Color)) return null;
            if (!(img instanceof TextureAtlas.AtlasRegion)) return null;
            return new Fields(x, y, vY != null ? vY : 0f, scale, rotation,
                    optionalFloat(effect, "dur_div2"), optionalFloat(effect, "duration"),
                    (Color) color, (TextureAtlas.AtlasRegion) img);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Snapshots the fields a bare-{@code Texture} draw needs. Required: {@code x}, {@code y},
     * {@code scale}, {@code color} ({@link Color}); {@code rotation} is additionally required for
     * {@code CALM_PARTICLE}/{@code DEBUFF_PARTICLE}/{@code ICE_SHATTER} (whose formula consumes it)
     * and optional (defaulting to {@code 0}) for {@code SHIELD_PARTICLE}/{@code WEB_PARTICLE}
     * (hardcoded rotation); {@code dur_div2}/{@code duration} are optional and default to {@code 0}.
     * For {@code DEBUFF_PARTICLE} and {@code ICE_SHATTER} the instance {@code img} must be a
     * {@link Texture} (WEB resolves the static {@link ImageMaster#WEB_VFX} instead); a
     * missing/mistyped {@code img} fails the snapshot. Returns {@code null} when the effect is null
     * or any required field is absent, unreadable, or of the wrong type; never throws.
     */
    static TextureFields readTextureFields(VfxDrawGeometry.Kind kind, Object effect) {
        if (effect == null) return null;
        boolean requireRotation = kind == VfxDrawGeometry.Kind.CALM_PARTICLE
                || kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                || kind == VfxDrawGeometry.Kind.ICE_SHATTER;
        try {
            Float x = readFloat(effect, "x");
            Float y = readFloat(effect, "y");
            Float scale = readFloat(effect, "scale");
            if (x == null || y == null || scale == null) {
                return null;
            }
            Float rotation = readFloat(effect, "rotation");
            if (requireRotation && rotation == null) {
                return null;
            }
            Object color = readRaw(effect, "color");
            if (!(color instanceof Color)) return null;
            Texture img = null;
            if (kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                    || kind == VfxDrawGeometry.Kind.ICE_SHATTER) {
                Object raw = readRaw(effect, "img");
                if (!(raw instanceof Texture)) return null;
                img = (Texture) raw;
            }
            return new TextureFields(x, y, scale, rotation != null ? rotation : 0f,
                    optionalFloat(effect, "dur_div2"), optionalFloat(effect, "duration"),
                    (Color) color, img);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Draws the real ART sprite for a claimed transient effect, reproducing the native draw
     * including its per-kind blend behavior ({@link VfxDrawGeometry#additiveBlend}: additive for
     * most kinds, ambient — no {@code setBlendFunction} call — for the ambient kinds
     * {@code FlashAtkImgEffect}, {@code SmokeBlurEffect}, {@code CeilingDustCloudEffect}, and
     * {@code NemesisFireParticle}).
     *
     * <p>For the img-based kinds: the region comes from the effect's own live
     * {@code img} through {@link Sts1GdxAtlasRegions#fromGdx}, geometry from
     * {@link VfxDrawGeometry#params}, color from the effect's own {@link Color} (or white). The
     * per-kind blend policy is {@link VfxDrawGeometry#additiveBlend}: additive kinds install
     * {@code (SRC_ALPHA, ONE)} and restore {@code (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)};
     * the ambient kinds ({@code FlashAtkImgEffect}, {@code SmokeBlurEffect},
     * {@code CeilingDustCloudEffect}, {@code NemesisFireParticle}) never call
     * {@code setBlendFunction} natively, so their path draws
     * under the ambient blend and restores only the previous color. Every branch restores the
     * previous color. That draw uses the {@code SpriteBatch#draw(TextureRegion, ...)} overload with
     * the same arguments the native call passes, so libGDX resolves the region's UV rect (including
     * any atlas {@code rotate} baking) exactly as the native effect does.
     *
     * <p>The bare-{@code Texture} kinds (Calm, Shield, Debuff, IceShatter, Web) have no packed
     * region: they draw a fixed source rect, so this branch resolves the native {@link Texture} the
     * render reads — {@link ImageMaster#FROST_ACTIVATE_VFX_1} for Calm,
     * {@link ImageMaster#INTENT_DEFEND} for Shield, {@link ImageMaster#WEB_VFX} for Web, and the
     * effect's own instance {@code img} for Debuff and IceShatter — and replays the native raw
     * texture + source-rect overload.
     *
     * <p>Never throws. Returns {@code true} only after a real draw; any null input, unmapped class,
     * unreadable field, missing/invalid region or texture, or host failure returns {@code false}
     * without side effects so the caller fails open to the native draw.
     */
    @Override
    public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
        if (sb == null || effect == null) return false;
        try {
            VfxDrawGeometry.Kind kind =
                    VfxDrawGeometry.kindFor(effect.getClass().getName());
            if (kind == null) return false;
            if (kind == VfxDrawGeometry.Kind.CALM_PARTICLE
                    || kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE
                    || kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                    || kind == VfxDrawGeometry.Kind.ICE_SHATTER
                    || kind == VfxDrawGeometry.Kind.WEB_PARTICLE) {
                return renderTexture(sb, kind, effect);
            }
            Fields f = readFields(kind, effect);
            if (f == null) return false;
            TextureAtlas.AtlasRegion gdx = f.img;
            if (gdx == null || gdx.getTexture() == null) return false;
            AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
            if (neutral == null || !neutral.valid()) return false;
            float settingsScale = Settings.scale;
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, f.x, f.y, f.vY, f.scale, f.rotation, f.durDiv2, f.duration,
                    settingsScale, gdx.getRegionWidth(), gdx.getRegionHeight());
            // Per-kind blend policy: most kinds install additive blend and restore it, but the
            // ambient kinds (FlashAtkImgEffect, SmokeBlurEffect, CeilingDustCloudEffect,
            // NemesisFireParticle) never call setBlendFunction natively, so they must draw under
            // the ambient blend and restore only color.
            boolean additive = VfxDrawGeometry.additiveBlend(kind);
            Color previous = new Color(sb.getColor());
            boolean blendChanged = false;
            try {
                sb.setColor(f.color != null ? f.color : Color.WHITE);
                if (additive) {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    blendChanged = true;
                }
                sb.draw(gdx, p.x, p.y, p.originX, p.originY, p.width, p.height,
                        p.scaleX, p.scaleY, p.rotation);
                return true;
            } finally {
                try {
                    if (blendChanged) {
                        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    }
                    sb.setColor(previous);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Resolves the {@link Color} the bare-{@code Texture} branch passes to
     * {@code SpriteBatch.setColor}. Null collapses to white (the same default the img-based branch
     * uses); for a kind that {@link VfxDrawGeometry#whiteAlphaOnly} reports true for, the RGB
     * channels are forced to {@code 1f} and only the effect color's alpha is kept, mirroring the
     * native {@code new Color(1f, 1f, 1f, color.a)}; every other kind passes the color through
     * unchanged. Never mutates the effect's color and never throws (the caller already knows the
     * color is a non-null {@link Color}, but nulls still fail safe to white).
     */
    static Color resolveColor(VfxDrawGeometry.Kind kind, Color effectColor) {
        if (effectColor == null) {
            return Color.WHITE;
        }
        if (VfxDrawGeometry.whiteAlphaOnly(kind)) {
            return new Color(1f, 1f, 1f, effectColor.a);
        }
        return effectColor;
    }

    /**
     * Bare-{@code Texture} branch: resolves the native texture the kind's {@code render} reads
     * ({@link ImageMaster#FROST_ACTIVATE_VFX_1} for Calm, {@link ImageMaster#INTENT_DEFEND} for
     * Shield, {@link ImageMaster#WEB_VFX} for Web, the effect's own {@code img} for Debuff and
     * IceShatter), replays the raw texture + source-rect draw with the {@link VfxDrawGeometry#params}
     * geometry and the per-kind src rect, honors {@link VfxDrawGeometry#additiveBlend}, applies the
     * {@link VfxDrawGeometry#whiteAlphaOnly} color rule (Web forces the set color's RGB to white),
     * and restores blend/color. Fails open ({@code false}, no side effects) on any missing input.
     */
    private boolean renderTexture(SpriteBatch sb, VfxDrawGeometry.Kind kind,
            AbstractGameEffect effect) {
        TextureFields f = readTextureFields(kind, effect);
        if (f == null) return false;
        Texture texture;
        if (kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE) {
            texture = ImageMaster.INTENT_DEFEND;
        } else if (kind == VfxDrawGeometry.Kind.WEB_PARTICLE) {
            texture = ImageMaster.WEB_VFX;
        } else if (kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                || kind == VfxDrawGeometry.Kind.ICE_SHATTER) {
            texture = f.img;
        } else {
            texture = ImageMaster.FROST_ACTIVATE_VFX_1;
        }
        if (texture == null) return false;
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                kind, f.x, f.y, 0f, f.scale, f.rotation,
                f.durDiv2, f.duration, Settings.scale, 0f, 0f);
        int srcX;
        int srcY;
        int srcW;
        int srcH;
        if (kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE) {
            srcX = VfxDrawGeometry.SHIELD_SRC_X;
            srcY = VfxDrawGeometry.SHIELD_SRC_Y;
            srcW = VfxDrawGeometry.SHIELD_SRC_W;
            srcH = VfxDrawGeometry.SHIELD_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE) {
            srcX = VfxDrawGeometry.DEBUFF_SRC_X;
            srcY = VfxDrawGeometry.DEBUFF_SRC_Y;
            srcW = VfxDrawGeometry.DEBUFF_SRC_W;
            srcH = VfxDrawGeometry.DEBUFF_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.ICE_SHATTER) {
            srcX = VfxDrawGeometry.ICE_SHATTER_SRC_X;
            srcY = VfxDrawGeometry.ICE_SHATTER_SRC_Y;
            srcW = VfxDrawGeometry.ICE_SHATTER_SRC_W;
            srcH = VfxDrawGeometry.ICE_SHATTER_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.WEB_PARTICLE) {
            srcX = VfxDrawGeometry.WEB_SRC_X;
            srcY = VfxDrawGeometry.WEB_SRC_Y;
            srcW = VfxDrawGeometry.WEB_SRC_W;
            srcH = VfxDrawGeometry.WEB_SRC_H;
        } else {
            srcX = VfxDrawGeometry.CALM_SRC_X;
            srcY = VfxDrawGeometry.CALM_SRC_Y;
            srcW = VfxDrawGeometry.CALM_SRC_W;
            srcH = VfxDrawGeometry.CALM_SRC_H;
        }
        // Per-kind blend policy: additive kinds install and restore blend, the ambient kinds
        // (DebuffParticleEffect is the only ambient bare-Texture kind) never call setBlendFunction
        // natively and restore only color.
        boolean additive = VfxDrawGeometry.additiveBlend(kind);
        Color previous = new Color(sb.getColor());
        boolean blendChanged = false;
        try {
            sb.setColor(resolveColor(kind, f.color));
            if (additive) {
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                blendChanged = true;
            }
            sb.draw(texture, p.x, p.y, p.originX, p.originY, p.width, p.height,
                    p.scaleX, p.scaleY, p.rotation, srcX, srcY, srcW, srcH, false, false);
            return true;
        } finally {
            try {
                if (blendChanged) {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }
                sb.setColor(previous);
            } catch (Throwable ignored) {
            }
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
