package artframework.sts1.render;

/**
 * Pure draw geometry for one claimed per-instance transient effect (family-neutral seam; current
 * members are the {@code vfx-stance-aura} FQNs plus the {@code vfx-scene-world}
 * {@code LightFlareSEffect}/{@code LightFlareMEffect}/{@code LightFlareLEffect}/
 * {@code CeilingDustCloudEffect}, the {@code vfx-misc-root}
 * {@code FireBurstParticleEffect}/{@code NemesisFireParticle}, and the {@code vfx-combat}
 * {@code FlashAtkImgEffect}/{@code RedFireBurstParticleEffect}/{@code SmokeBlurEffect}; the four
 * newest members are the {@code vfx-scene-world} {@code TorchParticleXLEffect} and the
 * {@code vfx-misc-root} {@code GhostlyWeakFireEffect}/{@code GenericSmokeEffect}/
 * {@code ExhaustBlurEffect}),
 * mirroring the native render formula exactly.
 *
 * <p>This class is host-neutral data: it performs no GL work, holds no host handles, and applies no
 * color/blend/UV state. The per-kind blend policy is pure and lives in {@link #additiveBlend}: most
 * claimable effects draw additively — the host draw calls {@code setColor(color)} and
 * {@code setBlendFunction(770, 1)} around {@code SpriteBatch.draw(...)} and restores
 * {@code setBlendFunction(770, 771)} afterwards — but the ambient kinds never call
 * {@code setBlendFunction} at all, so they must draw under the ambient blend and restore only the
 * previous color ({@link #additiveBlend} returns {@code false} for them). The ambient kinds are
 * {@code FlashAtkImgEffect} (the first), plus the {@code SmokeBlurEffect},
 * {@code CeilingDustCloudEffect}, {@code NemesisFireParticle}, {@code DebuffParticleEffect},
 * {@code GenericSmokeEffect}, and {@code ExhaustBlurEffect}
 * members. The native
 * {@code LightFlareSEffect} orders blend-before-color, but only the restored end state is shared
 * with the aura classes. The host draw owns that color/blend/UV (and the region's UV rect); this
 * mapping only resolves the positional/scale/rotation arguments the batch receives, with the native
 * center origin {@code (packedWidth / 2, packedHeight / 2)} and the identity width/height
 * {@code (packedWidth, packedHeight)}.
 *
 * <p>The per-kind formulas mirrored here are:
 *
 * <pre>
 *   StanceAuraEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DivinityStanceChangeParticle.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   LightFlareSEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   LightFlareMEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   LightFlareLEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   TorchParticleLEffect.render (note: has vY, but render ignores it; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   FlashAtkImgEffect.render (note: no setBlendFunction; ambient blend):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   FireBurstParticleEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   RedFireBurstParticleEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   SmokeBlurEffect.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   CeilingDustCloudEffect.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   NemesisFireParticle.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   WrathParticleEffect.render:
 *     sb.draw(img, x, y + vY, pw/2f, ph/2f, pw, ph,
 *             scale*0.8f, (0.1f + ((dur_div2*2f - duration)*2f*scale)) * Settings.scale, rotation)
 *   DivinityParticleEffect.render:
 *     sb.draw(img, x, y + vY, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   CalmParticleEffect.render:
 *     sb.draw(ImageMaster.FROST_ACTIVATE_VFX_1, x, y, 32f, 32f, 25f, 128f,
 *             scale, scale + (dur_div2*0.4f - duration) * Settings.scale, rotation,
 *             0, 0, 64, 64, false, false)
 *   ShieldParticleEffect.render (note: additive blend; rotation hardcoded to 0f):
 *     sb.draw(ImageMaster.INTENT_DEFEND, x - 32f, y - 32f, 32f, 32f, 64f, 64f,
 *             scale, scale, 0f, 0, 0, 64, 64, false, false)
 *   DebuffParticleEffect.render (note: no setBlendFunction; ambient blend; uses the rotation field):
 *     sb.draw(img, x - 16f, y - 16f, 16f, 16f, 32f, 32f,
 *             scale, scale, rotation, 0, 0, 32, 32, false, false)
 *   TorchParticleXLEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   GhostlyWeakFireEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   GenericSmokeEffect.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   ExhaustBlurEffect.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 * </pre>
 *
 * where {@code pw}/{@code ph} are the region's {@code packedWidth}/{@code packedHeight}. The three
 * bare-{@code Texture} kinds — Calm, Shield, and Debuff — draw a fixed source rect rather than a
 * packed region, so their native origin/size/source rect are host-neutral constants and the packed
 * region size is ignored; Shield hardcodes rotation {@code 0f}, Debuff consumes its
 * {@code rotation} field, and Calm keeps its {@code scaleY} formula. {@code DivinityStanceChangeParticle}, the
 * cross-family {@code LightFlareSEffect}/{@code LightFlareMEffect}/{@code LightFlareLEffect}/
 * {@code TorchParticleLEffect}, the {@code vfx-misc-root} {@code FireBurstParticleEffect}/
 * {@code NemesisFireParticle}, and the {@code vfx-combat} {@code FlashAtkImgEffect}/
 * {@code RedFireBurstParticleEffect}/{@code SmokeBlurEffect}, plus the {@code vfx-scene-world}
 * {@code CeilingDustCloudEffect}, share the {@code StanceAuraEffect} geometry (x/y passthrough, no
 * consumed {@code vY}) — the three later scene-world members, the two fire bursts, the smoke blur,
 * the ceiling dust, the nemesis fire, and the four newest members ({@code TorchParticleXLEffect},
 * {@code GhostlyWeakFireEffect}, {@code GenericSmokeEffect}, {@code ExhaustBlurEffect}) are
 * geometry-identical to the additive center-packed branch
 * and the ones that own a {@code vY} field ignore it in {@code render} — so they all map to the
 * same {@link Kind#STANCE_AURA} formula branch. Only blend distinguishes them:
 * {@code FlashAtkImgEffect}/{@code SmokeBlurEffect}/{@code CeilingDustCloudEffect}/
 * {@code NemesisFireParticle}/{@code DebuffParticleEffect}/{@code GenericSmokeEffect}/
 * {@code ExhaustBlurEffect} never switch blend function, so
 * {@link #additiveBlend} reports
 * {@code false} for them, while the two fire bursts, {@code TorchParticleXLEffect},
 * {@code GhostlyWeakFireEffect}, and {@code ShieldParticleEffect} are additive
 * like the rest. {@code ShieldParticleEffect}/{@code DebuffParticleEffect} are the first two
 * members beyond {@code CalmParticleEffect} to draw a bare {@code Texture}, so they join the
 * fixed-source-rect shape via their own geometry branches rather than the packed-region branches.
 */
public final class VfxDrawGeometry {

    /** The claimable draw formulas (the {@code vfx-stance-aura} FQNs plus the cross-family members). */
    public enum Kind {
        STANCE_AURA,
        WRATH_PARTICLE,
        DIVINITY_PARTICLE,
        CALM_PARTICLE,
        DIVINITY_STANCE_CHANGE,
        LIGHT_FLARE,
        FLASH_ATK_IMG,
        LIGHT_FLARE_M,
        LIGHT_FLARE_L,
        TORCH_PARTICLE_L,
        FIRE_BURST,
        RED_FIRE_BURST,
        SMOKE_BLUR,
        CEILING_DUST,
        NEMESIS_FIRE,
        SHIELD_PARTICLE,
        DEBUFF_PARTICLE,
        TORCH_PARTICLE_XL,
        GHOSTLY_WEAK_FIRE,
        GENERIC_SMOKE,
        EXHAUST_BLUR
    }

    // Native CalmParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the FROST_ACTIVATE_VFX_1 texture.
    /** Native Calm draw origin x ({@code 32f}). */
    public static final float CALM_ORIGIN_X = 32f;
    /** Native Calm draw origin y ({@code 32f}). */
    public static final float CALM_ORIGIN_Y = 32f;
    /** Native Calm draw width ({@code 25f}). */
    public static final float CALM_WIDTH = 25f;
    /** Native Calm draw height ({@code 128f}). */
    public static final float CALM_HEIGHT = 128f;
    /** Native Calm draw source rect x ({@code 0}). */
    public static final int CALM_SRC_X = 0;
    /** Native Calm draw source rect y ({@code 0}). */
    public static final int CALM_SRC_Y = 0;
    /** Native Calm draw source rect width ({@code 64}). */
    public static final int CALM_SRC_W = 64;
    /** Native Calm draw source rect height ({@code 64}). */
    public static final int CALM_SRC_H = 64;

    // Native ShieldParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the ImageMaster.INTENT_DEFEND texture. The rotation is hardcoded to 0.
    /** Native Shield draw origin x ({@code 32f}). */
    public static final float SHIELD_ORIGIN_X = 32f;
    /** Native Shield draw origin y ({@code 32f}). */
    public static final float SHIELD_ORIGIN_Y = 32f;
    /** Native Shield draw width ({@code 64f}). */
    public static final float SHIELD_WIDTH = 64f;
    /** Native Shield draw height ({@code 64f}). */
    public static final float SHIELD_HEIGHT = 64f;
    /** Native Shield draw source rect x ({@code 0}). */
    public static final int SHIELD_SRC_X = 0;
    /** Native Shield draw source rect y ({@code 0}). */
    public static final int SHIELD_SRC_Y = 0;
    /** Native Shield draw source rect width ({@code 64}). */
    public static final int SHIELD_SRC_W = 64;
    /** Native Shield draw source rect height ({@code 64}). */
    public static final int SHIELD_SRC_H = 64;

    // Native DebuffParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of its own instance img Texture. The rotation comes from the field.
    /** Native Debuff draw origin x ({@code 16f}). */
    public static final float DEBUFF_ORIGIN_X = 16f;
    /** Native Debuff draw origin y ({@code 16f}). */
    public static final float DEBUFF_ORIGIN_Y = 16f;
    /** Native Debuff draw width ({@code 32f}). */
    public static final float DEBUFF_WIDTH = 32f;
    /** Native Debuff draw height ({@code 32f}). */
    public static final float DEBUFF_HEIGHT = 32f;
    /** Native Debuff draw source rect x ({@code 0}). */
    public static final int DEBUFF_SRC_X = 0;
    /** Native Debuff draw source rect y ({@code 0}). */
    public static final int DEBUFF_SRC_Y = 0;
    /** Native Debuff draw source rect width ({@code 32}). */
    public static final int DEBUFF_SRC_W = 32;
    /** Native Debuff draw source rect height ({@code 32}). */
    public static final int DEBUFF_SRC_H = 32;

    /** Resolved draw arguments; all finite, origin is the native center origin. */
    public static final class Params {
        public final float x;
        public final float y;
        public final float originX;
        public final float originY;
        public final float width;
        public final float height;
        public final float scaleX;
        public final float scaleY;
        public final float rotation;

        public Params(float x, float y, float originX, float originY, float width, float height,
                float scaleX, float scaleY, float rotation) {
            this.x = x;
            this.y = y;
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.rotation = rotation;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Params)) return false;
            Params p = (Params) other;
            return Float.compare(x, p.x) == 0
                    && Float.compare(y, p.y) == 0
                    && Float.compare(originX, p.originX) == 0
                    && Float.compare(originY, p.originY) == 0
                    && Float.compare(width, p.width) == 0
                    && Float.compare(height, p.height) == 0
                    && Float.compare(scaleX, p.scaleX) == 0
                    && Float.compare(scaleY, p.scaleY) == 0
                    && Float.compare(rotation, p.rotation) == 0;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            result = 31 * result + Float.floatToIntBits(originX);
            result = 31 * result + Float.floatToIntBits(originY);
            result = 31 * result + Float.floatToIntBits(width);
            result = 31 * result + Float.floatToIntBits(height);
            result = 31 * result + Float.floatToIntBits(scaleX);
            result = 31 * result + Float.floatToIntBits(scaleY);
            result = 31 * result + Float.floatToIntBits(rotation);
            return result;
        }

        @Override
        public String toString() {
            return "Params{x=" + x + ", y=" + y + ", originX=" + originX + ", originY=" + originY
                    + ", width=" + width + ", height=" + height + ", scaleX=" + scaleX
                    + ", scaleY=" + scaleY + ", rotation=" + rotation + '}';
        }
    }

    private VfxDrawGeometry() {}

    /**
     * FQN -&gt; {@link Kind}, or {@code null} when the class is not a claimable member of the
     * seam (fail-open). Matches only the exact FQNs owned by {@link VfxClaimPolicy}; null, blank,
     * near-misses ({@code ...StanceAuraEffect2}), and nested ({@code ...StanceAuraEffect$Sub}) fail
     * open.
     */
    public static Kind kindFor(String nativeClassName) {
        if (nativeClassName == null) return null;
        String value = nativeClassName.trim();
        if (value.isEmpty()) return null;
        if (VfxClaimPolicy.STANCE_AURA_EFFECT.equals(value)) return Kind.STANCE_AURA;
        if (VfxClaimPolicy.WRATH_PARTICLE_EFFECT.equals(value)) return Kind.WRATH_PARTICLE;
        if (VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT.equals(value)) return Kind.DIVINITY_PARTICLE;
        if (VfxClaimPolicy.CALM_PARTICLE_EFFECT.equals(value)) return Kind.CALM_PARTICLE;
        if (VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE.equals(value)) {
            return Kind.DIVINITY_STANCE_CHANGE;
        }
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE.equals(value)) return Kind.LIGHT_FLARE;
        if (VfxClaimPolicy.FLASH_ATK_IMG.equals(value)) return Kind.FLASH_ATK_IMG;
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE_M.equals(value)) return Kind.LIGHT_FLARE_M;
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE_L.equals(value)) return Kind.LIGHT_FLARE_L;
        if (VfxClaimPolicy.SCENE_TORCH_PARTICLE_L.equals(value)) return Kind.TORCH_PARTICLE_L;
        if (VfxClaimPolicy.FIRE_BURST.equals(value)) return Kind.FIRE_BURST;
        if (VfxClaimPolicy.RED_FIRE_BURST.equals(value)) return Kind.RED_FIRE_BURST;
        if (VfxClaimPolicy.SMOKE_BLUR.equals(value)) return Kind.SMOKE_BLUR;
        if (VfxClaimPolicy.CEILING_DUST.equals(value)) return Kind.CEILING_DUST;
        if (VfxClaimPolicy.NEMESIS_FIRE.equals(value)) return Kind.NEMESIS_FIRE;
        if (VfxClaimPolicy.SHIELD_PARTICLE.equals(value)) return Kind.SHIELD_PARTICLE;
        if (VfxClaimPolicy.DEBUFF_PARTICLE.equals(value)) return Kind.DEBUFF_PARTICLE;
        if (VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL.equals(value)) return Kind.TORCH_PARTICLE_XL;
        if (VfxClaimPolicy.GHOSTLY_WEAK_FIRE.equals(value)) return Kind.GHOSTLY_WEAK_FIRE;
        if (VfxClaimPolicy.GENERIC_SMOKE.equals(value)) return Kind.GENERIC_SMOKE;
        if (VfxClaimPolicy.EXHAUST_BLUR.equals(value)) return Kind.EXHAUST_BLUR;
        return null;
    }

    /**
     * Pure per-kind blend policy: {@code true} when the native render installs the additive blend
     * {@code (SRC_ALPHA, ONE)} around its draw and restores {@code (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)},
     * {@code false} when it never touches the blend function and draws under whatever ambient blend
     * is active.
     *
     * <p>Most kinds are additive; the ambient kinds ({@link Kind#FLASH_ATK_IMG},
     * {@link Kind#SMOKE_BLUR}, {@link Kind#CEILING_DUST}, {@link Kind#NEMESIS_FIRE},
     * {@link Kind#DEBUFF_PARTICLE}, {@link Kind#GENERIC_SMOKE}, {@link Kind#EXHAUST_BLUR}) never call
     * {@code setBlendFunction} at all, so the host draw must not install or restore a blend function
     * for them. {@link Kind#FLASH_ATK_IMG} was the first such kind; the smoke blur, ceiling dust, and
     * nemesis fire are the first ambient members beyond it, {@link Kind#DEBUFF_PARTICLE} is the
     * first ambient bare-{@code Texture} member, and {@link Kind#GENERIC_SMOKE}/{@link
     * Kind#EXHAUST_BLUR} are the newest ambient packed-region members. Every other kind — including
     * the two fire
     * bursts ({@link Kind#FIRE_BURST}, {@link Kind#RED_FIRE_BURST}), the additive bare-texture
     * {@link Kind#SHIELD_PARTICLE}, and the additive {@link Kind#TORCH_PARTICLE_XL}/{@link
     * Kind#GHOSTLY_WEAK_FIRE} — is additive.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean additiveBlend(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind != Kind.FLASH_ATK_IMG
                && kind != Kind.SMOKE_BLUR
                && kind != Kind.CEILING_DUST
                && kind != Kind.NEMESIS_FIRE
                && kind != Kind.DEBUFF_PARTICLE
                && kind != Kind.GENERIC_SMOKE
                && kind != Kind.EXHAUST_BLUR;
    }

    /**
     * Pure geometry for one claim. The caller supplies the effect field floats and the packed
     * region size; the per-kind color/blend state is applied by the host draw (see
     * {@link #additiveBlend}: additive kinds install/restore {@code 770/1}-&rarr;{@code 770/771},
     * while the ambient kinds ({@code FLASH_ATK_IMG}, {@code SMOKE_BLUR}, {@code CEILING_DUST},
     * {@code NEMESIS_FIRE}, {@code DEBUFF_PARTICLE}, {@code GENERIC_SMOKE}, {@code EXHAUST_BLUR})
     * leave the ambient blend untouched and restore
     * only color).
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static Params params(Kind kind, float x, float y, float vY, float scale, float rotation,
            float durDiv2, float duration, float settingsScale,
            float packedWidth, float packedHeight) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        float originX = packedWidth / 2f;
        float originY = packedHeight / 2f;
        switch (kind) {
            case STANCE_AURA:
            case DIVINITY_STANCE_CHANGE:
            case LIGHT_FLARE:
            case FLASH_ATK_IMG:
            case LIGHT_FLARE_M:
            case LIGHT_FLARE_L:
            case TORCH_PARTICLE_L:
            case FIRE_BURST:
            case RED_FIRE_BURST:
            case SMOKE_BLUR:
            case CEILING_DUST:
            case NEMESIS_FIRE:
            case TORCH_PARTICLE_XL:
            case GHOSTLY_WEAK_FIRE:
            case GENERIC_SMOKE:
            case EXHAUST_BLUR:
                // DivinityStanceChangeParticle, the cross-family LightFlareSEffect/MEffect/LEffect,
                // TorchParticleLEffect, the vfx-combat FlashAtkImgEffect, the two fire bursts, the
                // smoke blur, the ceiling dust, the nemesis fire, TorchParticleXLEffect,
                // GhostlyWeakFireEffect, GenericSmokeEffect, and ExhaustBlurEffect mirror
                // StanceAuraEffect exactly: x/y passthrough (no vY is consumed; every member that
                // owns a vY field — TorchParticleLEffect and all nine newer members — uses it only
                // in update()), center origin, packed size, uniform scale. Flash, the smoke blur,
                // the ceiling dust, the nemesis fire, the generic smoke, and the exhaust blur differ
                // only in blend (ambient, via additiveBlend == false).
                return new Params(x, y, originX, originY, packedWidth, packedHeight,
                        scale, scale, rotation);
            case DIVINITY_PARTICLE:
                return new Params(x, y + vY, originX, originY, packedWidth, packedHeight,
                        scale, scale, rotation);
            case WRATH_PARTICLE: {
                float scaleX = scale * 0.8f;
                float scaleY = (0.1f + ((durDiv2 * 2f - duration) * 2f * scale)) * settingsScale;
                return new Params(x, y + vY, originX, originY, packedWidth, packedHeight,
                        scaleX, scaleY, rotation);
            }
            case CALM_PARTICLE: {
                // Native Calm ignores the (absent) region: fixed origin/size and scaleY formula;
                // packedWidth/packedHeight and vY are unused.
                float scaleY = scale + (durDiv2 * 0.4f - duration) * settingsScale;
                return new Params(x, y, CALM_ORIGIN_X, CALM_ORIGIN_Y, CALM_WIDTH, CALM_HEIGHT,
                        scale, scaleY, rotation);
            }
            case SHIELD_PARTICLE:
                // Native ShieldParticleEffect ignores the (absent) region: fixed origin/size and a
                // hardcoded zero rotation; packedWidth/packedHeight, vY, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x - SHIELD_ORIGIN_X, y - SHIELD_ORIGIN_Y,
                        SHIELD_ORIGIN_X, SHIELD_ORIGIN_Y, SHIELD_WIDTH, SHIELD_HEIGHT,
                        scale, scale, 0f);
            case DEBUFF_PARTICLE:
                // Native DebuffParticleEffect ignores the (absent) region: fixed origin/size and the
                // field rotation; packedWidth/packedHeight, vY, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x - DEBUFF_ORIGIN_X, y - DEBUFF_ORIGIN_Y,
                        DEBUFF_ORIGIN_X, DEBUFF_ORIGIN_Y, DEBUFF_WIDTH, DEBUFF_HEIGHT,
                        scale, scale, rotation);
            default:
                throw new IllegalArgumentException("unhandled kind: " + kind);
        }
    }
}
