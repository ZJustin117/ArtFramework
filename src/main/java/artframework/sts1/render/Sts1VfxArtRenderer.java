package artframework.sts1.render;

import artframework.assets.AtlasRegion;
import artframework.sts1.assets.Sts1GdxAtlasRegions;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
 * {@code IceShatterEffect}, and {@code WebParticleEffect}, and the four newest members
 * {@code EntangleEffect} (reusing the Web static-texture config), {@code BlockImpactLineEffect} and
 * {@code ExhaustPileParticle} (ambient center-packed; the latter's {@code img} is a
 * {@code private static} field), {@code UnknownParticleEffect} (a new ambient fixed rect over
 * its own instance {@code Texture}), and the five newest {@code vfx-combat} members
 * {@code FlameParticleEffect}/{@code LightningOrbActivateEffect} (additive center-packed),
 * {@code DamageImpactBlurEffect}/{@code DamageImpactLineEffect} (ambient center-packed), and
 * {@code DarkOrbPassiveEffect} (a new additive fixed rect over its own instance
 * {@code Texture})), and the three newest members {@code WarningSignEffect} (a bare-{@code Texture}
 * fixed rect over the static {@code ImageMaster.WARNING_ICON_VFX} with a hardcoded
 * {@code Settings.scale * 2f} uniform scale), {@code StunStarEffect} (ambient center-packed with its
 * position shifted by its own {@code vX}/{@code vY}), and {@code FallingDustEffect} (ambient
 * center-packed with the region's own {@code offsetX}/{@code offsetY} as its origin), plus the three
 * newest members {@code LightningEffect} (additive center-packed with origin Y {@code 0f}),
 * {@code FlameBallParticleEffect} (additive center-packed with origin Y {@code packedHeight/2f +
 * 20f * Settings.scale}), and {@code ShineLinesEffect} (ambient center-packed; its native
 * {@code render} also guards
 * the draw with {@code if (!isDone)}), plus the three newest {@code vfx-scene-world} members
 * {@code TorchParticleMEffect}/{@code TorchParticleSEffect} (additive center-packed, no new rule;
 * their {@code vY} is update-only) and {@code DustEffect} (ambient center-packed, NO-ARG
 * constructor, reusing the region-offset origin of {@code FallingDustEffect}), plus the two newest
 * bare-{@code Texture} members {@code LightningOrbPassiveEffect} (additive, 122&times;122 fixed rect
 * consuming its {@code rotation} field and its own {@code flipX}/{@code flipY} booleans) and
 * {@code GlowyFireEyesEffect} (additive, 128&times;128 fixed rect with a hardcoded zero rotation and
 * its own {@code flippedX} horizontal flip) — the first kinds on the shape-C path to pass
 * per-instance flip flags; the two newest are the {@code vfx-combat} {@code FlyingSpikeEffect}
 * (additive center-packed, no new rule; its {@code vX}/{@code vY} are update-only) and the
 * {@code vfx-misc-root} {@code ConeEffect} (ambient center-packed with a NEW origin rule —
 * {@code originX = 0f} rather than {@code packedWidth/2f} — and a {@code scale * 1.1f} uniform
 * scale); and the two newest are the {@code vfx-combat} {@code FallingIceEffect} (additive
 * shape-C fixed rect, origin 48, size 96&times;96, src {@code 0,0,96,96}, x/y passthrough,
 * consuming its
 * {@code rotation} field and its own instance {@code Texture img}) and the {@code vfx-misc-root}
 * {@code DamageHeartEffect} (ambient center-packed with a public {@code AtlasRegion img}) — the
 * seam's first per-kind NATIVE DRAW GUARD kinds ({@link VfxDrawGeometry#nativeSkipsDrawByGuard}),
 * whose {@code render} declines (draws nothing) whenever the guard field is present and
 * {@code >= 0f}.
 *
 * <p>F2b1 shipped the two host-free halves of the real renderer: the readiness predicate
 * ({@link #isReady}, backed by the exact-FQN {@link VfxDrawGeometry#kindFor}) and the reflective
 * field reader ({@link #readFields}) that snapshots the native effect's own draw inputs. F2b2
 * completes the renderer: {@link #render} converts the effect's own live {@code img} region to the
 * host-neutral {@link AtlasRegion} (via {@link Sts1GdxAtlasRegions#fromGdx}), resolves the native
 * draw arguments through {@link VfxDrawGeometry#params}, and replays the native
 * {@code SpriteBatch.draw} with color save-restore and a per-kind blend policy
 * ({@link VfxDrawGeometry#additiveBlend} — the single source of truth for the ambient set):
 * additive for most kinds, but ambient — no {@code setBlendFunction} call at all — for exactly the
 * kinds whose {@code VfxDrawGeometry.additiveBlend(kind)} is {@code false}, today
 * {@code FlashAtkImgEffect}, {@code SmokeBlurEffect}, {@code CeilingDustCloudEffect},
 * {@code NemesisFireParticle}, {@code DebuffParticleEffect}, {@code GenericSmokeEffect},
 * {@code ExhaustBlurEffect}, {@code BlockImpactLineEffect}, {@code ExhaustPileParticle},
 * {@code UnknownParticleEffect}, {@code DamageImpactBlurEffect}, {@code DamageImpactLineEffect},
 * {@code StunStarEffect}, {@code FallingDustEffect}, {@code ShineLinesEffect},
 * {@code DustEffect}, {@code ConeEffect}, and {@code DamageHeartEffect}.
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
        final float vX;
        final float regionOffsetX;
        final float regionOffsetY;
        final float scale;
        final float rotation;
        final float durDiv2;
        final float duration;
        final Color color;
        final TextureAtlas.AtlasRegion img;

        Fields(float x, float y, float vY, float vX, float regionOffsetX, float regionOffsetY,
                float scale, float rotation, float durDiv2,
                float duration, Color color, TextureAtlas.AtlasRegion img) {
            this.x = x;
            this.y = y;
            this.vY = vY;
            this.vX = vX;
            this.regionOffsetX = regionOffsetX;
            this.regionOffsetY = regionOffsetY;
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
     * {@code WebParticleEffect} / {@code EntangleEffect} / {@code UnknownParticleEffect} /
     * {@code DarkOrbPassiveEffect} / {@code LightningOrbPassiveEffect} /
     * {@code GlowyFireEyesEffect} / {@code FallingIceEffect}).
     * {@code rotation} is required for the kinds whose formula consumes
     * it (Calm, Debuff, IceShatter, Unknown, DarkOrb, LightningOrbPassive, FallingIce) and optional
     * (defaulting
     * to {@code 0}) for
     * Shield, Web,
     * Entangle, and GlowyFireEyes, which hardcode {@code 0f}; {@code img} is a {@link Texture} for
     * Debuff, IceShatter, Unknown, DarkOrb, FallingIce, and the two flip kinds (LightningOrbPassive,
     * GlowyFireEyes) and {@code null} otherwise (Web/Entangle resolve the static
     * {@link ImageMaster#WEB_VFX}). {@code flipX}/{@code flipY} are the resolved per-instance flip
     * flags (both {@code false} for every kind that does not pass them natively).
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
        final boolean flipX;
        final boolean flipY;

        TextureFields(float x, float y, float scale, float rotation, float durDiv2, float duration,
                Color color, Texture img, boolean flipX, boolean flipY) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotation = rotation;
            this.durDiv2 = durDiv2;
            this.duration = duration;
            this.color = color;
            this.img = img;
            this.flipX = flipX;
            this.flipY = flipY;
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
     * {@code DIVINITY_PARTICLE}, {@code STUN_STAR}); for the kinds that ignore it
     * ({@code STANCE_AURA},
     * {@code DIVINITY_STANCE_CHANGE}, {@code LIGHT_FLARE}, {@code LIGHT_FLARE_M},
     * {@code LIGHT_FLARE_L}, {@code TORCH_PARTICLE_L}, {@code FLASH_ATK_IMG}, and the nine newest
     * members {@code FIRE_BURST}, {@code RED_FIRE_BURST}, {@code SMOKE_BLUR}, {@code CEILING_DUST},
     * {@code NEMESIS_FIRE}, {@code TORCH_PARTICLE_XL}, {@code GHOSTLY_WEAK_FIRE},
     * {@code GENERIC_SMOKE}, {@code EXHAUST_BLUR} — plus the three newest img kinds
     * {@code LIGHTNING_EFFECT}, {@code FLAME_BALL}, {@code SHINE_LINES} — plus the three newest
     * img kinds {@code TORCH_PARTICLE_M}, {@code TORCH_PARTICLE_S}, {@code SCENE_DUST}, whose
     * {@code rotation}
     * field all three consume ({@code TORCH_PARTICLE_M}/{@code TORCH_PARTICLE_S}'s {@code vY} is
     * update-only)) it is optional and
     * defaults to {@code 0}, which is required because {@code DivinityStanceChangeParticle},
     * {@code LightFlareSEffect}, {@code FlashAtkImgEffect}, and {@code LightFlareMEffect}/
     * {@code LightFlareLEffect} have no {@code vY} field — and {@code TorchParticleLEffect}, the
     * two newest {@code TorchParticleMEffect}/{@code TorchParticleSEffect}, and all
     * nine earlier img members have one
     * but never read it in {@code render} (it is update-only). {@code vX} is likewise optional
     * (defaulting to {@code 0}) for every kind: {@code StunStarEffect} consumes it in its position
     * offset, and every other kind ignores it.
     *
     * <p>The two region-offset scalars are taken from the effect's own live
     * {@link TextureAtlas.AtlasRegion} ({@code offsetX}/{@code offsetY}), not from an effect field;
     * they are the draw origin only for {@code FALLING_DUST} and the newest {@code SCENE_DUST}
     * (which reuses that same region-offset-origin rule). Because {@code AtlasRegion.flip(...)}
     * mutates {@code offsetX}/{@code offsetY} in place on a shared static region, the values are
     * normalized back to the unflipped orientation (the exact inverse of
     * {@code AtlasRegion.flip}'s offset transform) when {@code isFlipX()}/{@code isFlipY()} reports a
     * native flip, matching the flip-invariant canonical draw (F15d).
     *
     * <p>Returns {@code null}
     * when the effect is null or any required field is absent, unreadable, or of the wrong type;
     * never throws.
     */
    static Fields readFields(VfxDrawGeometry.Kind kind, Object effect) {
        if (effect == null) return null;
        // vY is only meaningful for the formulas that add it to y or use it in the position offset;
        // requiring it elsewhere would wrongly reject DivinityStanceChangeParticle/LightFlareSEffect
        // and LightFlareMEffect/LightFlareLEffect (no vY field) and TorchParticleLEffect plus the
        // nine newest members (their vY is update-only), the three F17 img kinds (LightningEffect
        // and ShineLinesEffect have no vY field; FlameBallParticleEffect's is update-only), and the
        // two newest TorchParticleMEffect/TorchParticleSEffect (their vY is update-only), and
        // omitting it where it is consumed would
        // silently draw at an un-shifted y/position instead of failing open to the native draw.
        // StunStarEffect consumes vY in its position offset (y - vY*5f*Settings.scale).
        boolean requireVY = kind == VfxDrawGeometry.Kind.WRATH_PARTICLE
                || kind == VfxDrawGeometry.Kind.DIVINITY_PARTICLE
                || kind == VfxDrawGeometry.Kind.STUN_STAR;
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
            TextureAtlas.AtlasRegion region = (TextureAtlas.AtlasRegion) img;
            return new Fields(x, y, vY != null ? vY : 0f, optionalFloat(effect, "vX"),
                    unflippedOffsetX(region), unflippedOffsetY(region),
                    scale, rotation,
                    optionalFloat(effect, "dur_div2"), optionalFloat(effect, "duration"),
                    (Color) color, region);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * The region's unflipped horizontal trim origin: {@code AtlasRegion.flip(true, *)} rewrites
     * {@code offsetX = originalWidth - offsetX - rotatedPackedWidth} in place, so this exact inverse
     * recovers the trim origin the atlas declared (a no-op for an unflipped region). Mirrors the
     * F15d flip-invariant canonical draw.
     */
    private static float unflippedOffsetX(TextureAtlas.AtlasRegion region) {
        if (region.isFlipX()) {
            return region.originalWidth - region.offsetX - region.getRotatedPackedWidth();
        }
        return region.offsetX;
    }

    /**
     * The region's unflipped vertical trim origin (see {@link #unflippedOffsetX}); the same inverse
     * of {@code AtlasRegion.flip(*, true)}'s {@code offsetY} transform.
     */
    private static float unflippedOffsetY(TextureAtlas.AtlasRegion region) {
        if (region.isFlipY()) {
            return region.originalHeight - region.offsetY - region.getRotatedPackedHeight();
        }
        return region.offsetY;
    }

    /**
     * Snapshots the fields a bare-{@code Texture} draw needs. Required: {@code x}, {@code y},
     * {@code scale}, {@code color} ({@link Color}); {@code rotation} is additionally required for
     * {@code CALM_PARTICLE}/{@code DEBUFF_PARTICLE}/{@code ICE_SHATTER}/{@code UNKNOWN_PARTICLE}/
     * {@code DARK_ORB_PASSIVE}/{@code LIGHTNING_ORB_PASSIVE}/{@code FALLING_ICE}
     * (whose formula consumes it)
     * and optional (defaulting to {@code 0}) for {@code SHIELD_PARTICLE}/{@code WEB_PARTICLE}/
     * {@code ENTANGLE}/{@code GLOWY_FIRE_EYES} (hardcoded rotation); {@code dur_div2}/{@code duration}
     * are optional and
     * default to {@code 0}.
     * For {@code DEBUFF_PARTICLE}, {@code ICE_SHATTER}, {@code UNKNOWN_PARTICLE},
     * {@code DARK_ORB_PASSIVE}, {@code LIGHTNING_ORB_PASSIVE}, {@code FALLING_ICE}, and
     * {@code GLOWY_FIRE_EYES} the instance
     * {@code img} must be a
     * {@link Texture} (CALM/SHIELD/WEB/ENTANGLE resolve a static {@code ImageMaster} texture
     * instead); a missing/mistyped {@code img} fails the snapshot. The per-instance flip flags are
     * resolved only for the kinds whose native render passes them
     * ({@link VfxDrawGeometry#usesInstanceFlipX}/{@link VfxDrawGeometry#usesInstanceFlipY});
     * a missing/unreadable flag defaults to {@code false} (flip is optional decoration, so it does
     * NOT fail the snapshot). Returns {@code null} when the
     * effect is null or any required field is absent, unreadable, or of the wrong type; never throws.
     */
    static TextureFields readTextureFields(VfxDrawGeometry.Kind kind, Object effect) {
        if (effect == null) return null;
        boolean requireRotation = kind == VfxDrawGeometry.Kind.CALM_PARTICLE
                || kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                || kind == VfxDrawGeometry.Kind.ICE_SHATTER
                || kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE
                || kind == VfxDrawGeometry.Kind.DARK_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.FALLING_ICE;
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
            if (usesInstanceTexture(kind)) {
                Object raw = readRaw(effect, "img");
                if (!(raw instanceof Texture)) return null;
                img = (Texture) raw;
            }
            // Per-instance flip flags (NRO-04 F19): resolved only for the kinds whose native render
            // passes them (LightningOrbPassiveEffect's flipX/flipY; GlowyFireEyesEffect's flippedX
            // with a hardcoded false Y). Flip is OPTIONAL decoration, so an absent/unreadable flag
            // defaults to false rather than failing the whole snapshot (the rest of the snapshot
            // still fails open on its required fields).
            boolean flipX = false;
            boolean flipY = false;
            if (VfxDrawGeometry.usesInstanceFlipX(kind)) {
                flipX = optionalBoolean(effect,
                        kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES ? "flippedX" : "flipX");
            }
            if (VfxDrawGeometry.usesInstanceFlipY(kind)) {
                flipY = optionalBoolean(effect, "flipY");
            }
            return new TextureFields(x, y, scale, rotation != null ? rotation : 0f,
                    optionalFloat(effect, "dur_div2"), optionalFloat(effect, "duration"),
                    (Color) color, img, flipX, flipY);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * True for the bare-{@code Texture} kinds that resolve their own instance {@code Texture img}
     * rather than a static {@code ImageMaster} texture ({@code DebuffParticleEffect},
     * {@code IceShatterEffect}, {@code UnknownParticleEffect}, {@code DarkOrbPassiveEffect},
     * {@code LightningOrbPassiveEffect}, {@code GlowyFireEyesEffect}, {@code FallingIceEffect}).
     * {@code CalmParticleEffect},
     * {@code ShieldParticleEffect}, {@code WebParticleEffect}, and {@code EntangleEffect} draw a
     * static {@code ImageMaster} texture instead.
     */
    private static boolean usesInstanceTexture(VfxDrawGeometry.Kind kind) {
        return kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                || kind == VfxDrawGeometry.Kind.ICE_SHATTER
                || kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE
                || kind == VfxDrawGeometry.Kind.DARK_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES
                || kind == VfxDrawGeometry.Kind.FALLING_ICE;
    }

    /**
     * Builds a canonical (pollution-immune) {@link TextureRegion} view of a live gdx
     * {@code AtlasRegion}: the packed footprint normalized to its canonical orientation
     * ({@code u}=min, {@code u2}=max, {@code v}=min, {@code v2}=max) with the same texture. Native
     * siblings draw the same shared static regions and call {@code region.flip(...)} in place, which
     * swaps {@code u}/{@code u2} (and, for {@code AtlasRegion}, {@code offsetX}); the claim
     * suppresses that native draw, so ART must emit the canonical rect rather than whatever a native
     * sibling last left behind. Returns {@code null} when the region or its texture is null. Never
     * mutates the input region.
     */
    private static TextureRegion canonicalRegion(TextureAtlas.AtlasRegion region) {
        if (region == null) return null;
        Texture texture = region.getTexture();
        if (texture == null) return null;
        float u = Math.min(region.getU(), region.getU2());
        float u2 = Math.max(region.getU(), region.getU2());
        float v = Math.min(region.getV(), region.getV2());
        float v2 = Math.max(region.getV(), region.getV2());
        return new TextureRegion(texture, u, v, u2, v2);
    }

    /**
     * The per-kind fixed horizontal offset added to the shared center-packed origin
     * ({@code packedWidth / 2f}); {@code 0f} for every current kind.
     */
    private static float originOffsetX(VfxDrawGeometry.Kind kind) {
        return 0f;
    }

    /**
     * The per-kind fixed vertical offset added to the shared center-packed origin
     * ({@code packedHeight / 2f}). {@code LIGHTNING_EFFECT} passes {@code -packedHeight / 2f} so its
     * origin Y becomes exactly {@code 0f} (a scale-independent native offset); every other kind
     * passes {@code 0f} and keeps the unchanged {@code packedHeight / 2f} origin.
     * {@code FLAME_BALL}'s settings-scaled origin lift ({@code +20f * Settings.scale}) is handled
     * inside {@link VfxDrawGeometry#params} (which already receives {@code settingsScale}), so it
     * passes {@code 0f} here.
     */
    private static float originOffsetY(VfxDrawGeometry.Kind kind, TextureAtlas.AtlasRegion gdx) {
        if (kind == VfxDrawGeometry.Kind.LIGHTNING_EFFECT) {
            return -gdx.getRegionHeight() / 2f;
        }
        return 0f;
    }

    /**
     * The seam's first per-kind NATIVE DRAW GUARD (NRO-04 F21): true unless the native render's
     * wait-phase guard blocks the draw. For a guarded kind ({@link VfxDrawGeometry#nativeSkipsDrawByGuard})
     * the guard field named by {@link VfxDrawGeometry#guardFieldName} is read reflectively; when it
     * is present and {@code >= 0f} the native render would skip its draw entirely, so ART must also
     * draw nothing (return {@code false}) to stay in pixel parity. An absent/unreadable/wrongly
     * typed guard field is treated as SATISFIED (returns {@code true}) — the native render would
     * then not be able to read a guard either. A cheap no-op for every non-guard kind. Never throws.
     *
     * <p>Package-private so the no-GL tests can exercise the absent/unreadable-guard path directly
     * (the real mapping classes always declare their guard field).
     */
    static boolean guardSatisfied(VfxDrawGeometry.Kind kind, Object effect) {
        if (kind == null || effect == null) return true;
        if (!VfxDrawGeometry.nativeSkipsDrawByGuard(kind)) return true;
        String field = VfxDrawGeometry.guardFieldName(kind);
        if (field == null) return true;
        Float value = null;
        try {
            value = readFloat(effect, field);
        } catch (Throwable ignored) {
            return true;
        }
        // The native guard is `if (<field> < 0f) { draw }`, so a present, >= 0f field blocks it.
        return value == null || value.floatValue() < 0f;
    }

    /**
     * Draws the real ART sprite for a claimed transient effect, reproducing the native draw
     * including its per-kind blend behavior ({@link VfxDrawGeometry#additiveBlend} — the single
     * source of truth for the ambient set: additive for
     * most kinds, ambient — no {@code setBlendFunction} call — for the ambient kinds
     * {@code FlashAtkImgEffect}, {@code SmokeBlurEffect}, {@code CeilingDustCloudEffect},
     * {@code NemesisFireParticle}, {@code DebuffParticleEffect}, {@code GenericSmokeEffect},
     * {@code ExhaustBlurEffect}, {@code BlockImpactLineEffect}, {@code ExhaustPileParticle},
     * {@code UnknownParticleEffect}, {@code DamageImpactBlurEffect}, {@code DamageImpactLineEffect},
     * {@code StunStarEffect}, {@code FallingDustEffect}, {@code ShineLinesEffect},
     * {@code DustEffect}, and {@code ConeEffect}).
     *
     * <p>For the img-based kinds: the region comes from the effect's own live
     * {@code img} through {@link Sts1GdxAtlasRegions#fromGdx}, geometry from
     * {@link VfxDrawGeometry#params}, color from the effect's own {@link Color} (or white). The
     * per-kind blend policy is {@link VfxDrawGeometry#additiveBlend} (the single source of truth for
     * the ambient set): additive kinds install
     * {@code (SRC_ALPHA, ONE)} and restore {@code (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)};
     * the ambient kinds ({@code FlashAtkImgEffect}, {@code SmokeBlurEffect},
     * {@code CeilingDustCloudEffect}, {@code NemesisFireParticle}, {@code DebuffParticleEffect},
     * {@code GenericSmokeEffect}, {@code ExhaustBlurEffect}, {@code BlockImpactLineEffect},
     * {@code ExhaustPileParticle}, {@code UnknownParticleEffect}, {@code DamageImpactBlurEffect},
     * {@code DamageImpactLineEffect}, {@code StunStarEffect}, {@code FallingDustEffect},
     * {@code ShineLinesEffect}, {@code DustEffect}, and {@code ConeEffect})
     * never call
     * {@code setBlendFunction} natively, so their path draws
     * under the ambient blend and restores only the previous color. Every branch restores the
     * previous color. That draw uses the {@code SpriteBatch#draw(TextureRegion, ...)} overload with
     * the same arguments the native call passes, so libGDX resolves the region's UV rect (including
     * any atlas {@code rotate} baking) exactly as the native effect does.
     *
     * <p>The bare-{@code Texture} kinds (Calm, Shield, Debuff, IceShatter, Web, Entangle, Unknown,
     * DarkOrb, LightningOrbPassive, GlowyFireEyes, FallingIce)
     * have no packed
     * region: they draw a fixed source rect, so this branch resolves the native {@link Texture} the
     * render reads — {@link ImageMaster#FROST_ACTIVATE_VFX_1} for Calm,
     * {@link ImageMaster#INTENT_DEFEND} for Shield, {@link ImageMaster#WEB_VFX} for Web and Entangle,
     * and the
     * effect's own instance {@code img} for Debuff, IceShatter, Unknown, DarkOrb, LightningOrbPassive,
     * GlowyFireEyes, and FallingIce — and replays
     * the native
     * raw
     * texture + source-rect overload, now including the effect's own per-instance flip booleans for
     * the kinds that pass them (LightningOrbPassive's {@code flipX}/{@code flipY} and
     * GlowyFireEyes's {@code flippedX} with a hardcoded {@code false} Y; every other kind keeps
     * {@code false, false}).
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
            // Native draw guard (NRO-04 F21): a guarded kind whose wait-phase field is present and
            // >= 0f draws nothing natively, so ART must draw nothing too (fail open: the patch calls
            // native render, which also draws nothing). Cheap no-op for every non-guard kind.
            if (!guardSatisfied(kind, effect)) return false;
            if (isTextureDrawKind(kind)) {
                return renderTexture(sb, kind, effect);
            }
            Fields f = readFields(kind, effect);
            if (f == null) return false;
            TextureAtlas.AtlasRegion gdx = f.img;
            if (gdx == null || gdx.getTexture() == null) return false;
            AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
            if (neutral == null || !neutral.valid()) return false;
            // The claim suppresses native rendering, so draw a CANONICAL (pollution-immune) view
            // rather than the possibly-natively-flipped shared region: a native sibling may have
            // called region.flip(...) in place on this same static AtlasRegion.
            TextureRegion canonical = canonicalRegion(gdx);
            if (canonical == null) return false;
            float settingsScale = Settings.scale;
            // getRegionWidth()/getRegionHeight() are the flip-invariant packed footprint, so they
            // equal the canonical region's width/height even when the shared region was flipped.
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, f.x, f.y, f.vY, f.scale, f.rotation, f.durDiv2, f.duration,
                    settingsScale, gdx.getRegionWidth(), gdx.getRegionHeight(),
                    f.vX, f.regionOffsetX, f.regionOffsetY,
                    originOffsetX(kind), originOffsetY(kind, gdx));
            // Per-kind blend policy: most kinds install additive blend and restore it, but the
            // ambient kinds (VfxDrawGeometry.additiveBlend(kind) == false — see that method for the
            // authoritative list, which includes the newest DustEffect)
            // never call setBlendFunction natively, so they must draw under
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
                sb.draw(canonical, p.x, p.y, p.originX, p.originY, p.width, p.height,
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
     * True only when {@link #render} could actually draw this exact instance: the class maps to a
     * kind, the native draw guard is satisfied (a guard-blocked kind cannotDraw — native draws
     * nothing in that state either), the required fields resolve, and the resolved region/texture is
     * present. Mirrors
     * {@code render}'s early-return conditions up to (but not including) the batch draw, so a
     * {@code false} here means the native effect would also produce no pixels for this instance
     * under this renderer. Never throws.
     */
    @Override
    public boolean canDraw(Object effect) {
        if (effect == null) return false;
        try {
            VfxDrawGeometry.Kind kind = VfxDrawGeometry.kindFor(effect.getClass().getName());
            if (kind == null) return false;
            // Native draw guard (NRO-04 F21): the same check render performs. A guard-blocked
            // instance cannotDraw, which lets the bridge classify the decline as a benign no-pixel
            // decline (native draws nothing in that state either).
            if (!guardSatisfied(kind, effect)) return false;
            return imagePresent(kind, effect);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * True when this exact instance's own image source is present, using the SAME field/resource
     * checks {@link #render} performs up to the batch draw: for the bare-{@code Texture} kinds the
     * snapshot must resolve and its {@link Texture} (instance {@code img} or the resolved static
     * {@code ImageMaster} texture) must be non-null; for the img kinds the snapshot's
     * {@link TextureAtlas.AtlasRegion} must be present, texturable, and convert to a valid neutral
     * region. Shared by {@link #canDraw} and {@link #declinedWithoutPixels} so the two stay
     * consistent. Never throws.
     */
    private static boolean imagePresent(VfxDrawGeometry.Kind kind, Object effect) {
        if (kind == null || effect == null) return false;
        if (isTextureDrawKind(kind)) {
            TextureFields f = readTextureFields(kind, effect);
            return f != null && resolveTexture(kind, f) != null;
        }
        Fields f = readFields(kind, effect);
        if (f == null) return false;
        TextureAtlas.AtlasRegion gdx = f.img;
        if (gdx == null || gdx.getTexture() == null) return false;
        AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
        return neutral != null && neutral.valid();
    }

    /**
     * True for the bare-{@code Texture} (fixed source-rect) kinds that {@link #render} serves through
     * {@link #renderTexture} rather than the packed-region path. The single source of truth shared by
     * {@link #render}, {@link #canDraw}, and {@link #imagePresent} so a kind cannot be routed one way
     * by one and another way by another.
     */
    private static boolean isTextureDrawKind(VfxDrawGeometry.Kind kind) {
        return kind == VfxDrawGeometry.Kind.CALM_PARTICLE
                || kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE
                || kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                || kind == VfxDrawGeometry.Kind.ICE_SHATTER
                || kind == VfxDrawGeometry.Kind.WEB_PARTICLE
                || kind == VfxDrawGeometry.Kind.ENTANGLE
                || kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE
                || kind == VfxDrawGeometry.Kind.WARNING_SIGN
                || kind == VfxDrawGeometry.Kind.DARK_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES
                || kind == VfxDrawGeometry.Kind.FALLING_ICE;
    }

    /**
     * Instance-aware benign no-pixel probe (NRO-04 F21): true only when THIS exact instance would
     * natively draw NOTHING, so an ART decline loses no pixels. That is exactly two cases: (a) a kind
     * whose native render guards its draw on a present image
     * ({@link VfxDrawGeometry#nativeSkipsDrawWithoutImage}) and whose instance has no drawable image,
     * or (b) a wait-phase-guarded kind ({@link VfxDrawGeometry#nativeSkipsDrawByGuard}) whose guard
     * field is present and {@code >= 0f} (the guard blocks the native draw). Every other instance —
     * including a guard-SATISFIED instance whose image snapshot fails — is {@code false}, so a
     * genuine renderer failure is never masked as benign. Deliberately narrower than
     * {@link #canDraw}, which also reports {@code false} for any other undrawable instance. Never
     * throws.
     */
    @Override
    public boolean declinedWithoutPixels(Object effect) {
        if (effect == null) return false;
        try {
            VfxDrawGeometry.Kind kind = VfxDrawGeometry.kindFor(effect.getClass().getName());
            if (kind == null) return false;
            if (VfxDrawGeometry.nativeSkipsDrawWithoutImage(kind)) {
                // The native render draws only with a present image, so a missing image is benign.
                return !imagePresent(kind, effect);
            }
            if (VfxDrawGeometry.nativeSkipsDrawByGuard(kind)) {
                // The native render draws only when the wait-phase guard is satisfied (< 0f).
                return !guardSatisfied(kind, effect);
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Resolves the native {@link Texture} the bare-{@code Texture} kinds draw (the static
     * {@link ImageMaster} textures for Calm/Shield/Web/Entangle, the instance {@code img} for
     * Debuff/IceShatter/Unknown/DarkOrb/FallingIce and the two flip kinds
     * LightningOrbPassive/GlowyFireEyes),
     * or {@code null} when it is absent. Shared by
     * {@link #renderTexture} and {@link #canDraw} so the two stay consistent.
     */
    private static Texture resolveTexture(VfxDrawGeometry.Kind kind, TextureFields f) {
        if (kind == VfxDrawGeometry.Kind.WARNING_SIGN) {
            // WarningSignEffect draws the static ImageMaster.WARNING_ICON_VFX Texture.
            return ImageMaster.WARNING_ICON_VFX;
        }
        if (kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE) {
            return ImageMaster.INTENT_DEFEND;
        }
        if (kind == VfxDrawGeometry.Kind.WEB_PARTICLE
                || kind == VfxDrawGeometry.Kind.ENTANGLE) {
            // WebParticleEffect and EntangleEffect share the static ImageMaster.WEB_VFX Texture.
            return ImageMaster.WEB_VFX;
        }
        if (usesInstanceTexture(kind)) {
            return f.img;
        }
        return ImageMaster.FROST_ACTIVATE_VFX_1;
    }

    /**
     * Bare-{@code Texture} branch: resolves the native texture the kind's {@code render} reads
     * ({@link ImageMaster#FROST_ACTIVATE_VFX_1} for Calm, {@link ImageMaster#INTENT_DEFEND} for
     * Shield, {@link ImageMaster#WEB_VFX} for Web and Entangle, the effect's own {@code img} for
     * Debuff, IceShatter, Unknown, DarkOrb, LightningOrbPassive, GlowyFireEyes, and FallingIce),
     * replays the raw
     * texture + source-rect draw with the
     * {@link VfxDrawGeometry#params}
     * geometry and the per-kind src rect, passes the per-instance flip booleans for the kinds whose
     * native render supplies them ({@link VfxDrawGeometry#usesInstanceFlipX}/
     * {@link VfxDrawGeometry#usesInstanceFlipY}; {@code false, false} for every other kind), honors
     * {@link VfxDrawGeometry#additiveBlend}, applies the
     * {@link VfxDrawGeometry#whiteAlphaOnly} color rule (Web and Entangle force the set color's RGB
     * to white),
     * and restores blend/color. Fails open ({@code false}, no side effects) on any missing input.
     */
    private boolean renderTexture(SpriteBatch sb, VfxDrawGeometry.Kind kind,
            AbstractGameEffect effect) {
        TextureFields f = readTextureFields(kind, effect);
        if (f == null) return false;
        Texture texture = resolveTexture(kind, f);
        if (texture == null) return false;
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                kind, f.x, f.y, 0f, f.scale, f.rotation,
                f.durDiv2, f.duration, Settings.scale, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        int srcX;
        int srcY;
        int srcW;
        int srcH;
        if (kind == VfxDrawGeometry.Kind.WARNING_SIGN) {
            // WarningSignEffect draws the static ImageMaster.WARNING_ICON_VFX Texture's full rect;
            // the geometry already carries the hardcoded Settings.scale * 2f uniform scale.
            srcX = VfxDrawGeometry.WARNING_SRC_X;
            srcY = VfxDrawGeometry.WARNING_SRC_Y;
            srcW = VfxDrawGeometry.WARNING_SRC_W;
            srcH = VfxDrawGeometry.WARNING_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE) {
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
        } else if (kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE) {
            srcX = VfxDrawGeometry.UNKNOWN_SRC_X;
            srcY = VfxDrawGeometry.UNKNOWN_SRC_Y;
            srcW = VfxDrawGeometry.UNKNOWN_SRC_W;
            srcH = VfxDrawGeometry.UNKNOWN_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.DARK_ORB_PASSIVE) {
            // DarkOrbPassiveEffect passes the full 74x74 region as its src rect.
            srcX = VfxDrawGeometry.DARK_ORB_SRC_X;
            srcY = VfxDrawGeometry.DARK_ORB_SRC_Y;
            srcW = VfxDrawGeometry.DARK_ORB_SRC_W;
            srcH = VfxDrawGeometry.DARK_ORB_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE) {
            // LightningOrbPassiveEffect passes the full 122x122 rect as its src rect.
            srcX = VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_X;
            srcY = VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_Y;
            srcW = VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_W;
            srcH = VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES) {
            // GlowyFireEyesEffect passes the full 128x128 rect as its src rect.
            srcX = VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_X;
            srcY = VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_Y;
            srcW = VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_W;
            srcH = VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.FALLING_ICE) {
            // FallingIceEffect passes the full 96x96 rect as its src rect.
            srcX = VfxDrawGeometry.FALLING_ICE_SRC_X;
            srcY = VfxDrawGeometry.FALLING_ICE_SRC_Y;
            srcW = VfxDrawGeometry.FALLING_ICE_SRC_W;
            srcH = VfxDrawGeometry.FALLING_ICE_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.WEB_PARTICLE
                || kind == VfxDrawGeometry.Kind.ENTANGLE) {
            // WebParticleEffect and EntangleEffect share the static WEB_VFX src rect.
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
        // (VfxDrawGeometry.additiveBlend(kind) == false — the bare-Texture kinds DebuffParticleEffect
        // and UnknownParticleEffect are both ambient, and WarningSignEffect is additive)
        // never call setBlendFunction natively and restore only color.
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
                    p.scaleX, p.scaleY, p.rotation, srcX, srcY, srcW, srcH,
                    f.flipX, f.flipY);
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

    /**
     * Optional boolean: absent/unreadable/wrong type collapses to {@code false}. Flip is decorative,
     * so a missing flag must not fail the snapshot open.
     */
    private static boolean optionalBoolean(Object target, String name) {
        Object raw = readRaw(target, name);
        return raw instanceof Boolean && (Boolean) raw;
    }
}
