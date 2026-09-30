package artframework.sts1.render;

/**
 * Pure draw geometry for one claimed per-instance transient effect (family-neutral seam; current
 * members are the {@code vfx-stance-aura} FQNs plus the {@code vfx-scene-world}
 * {@code LightFlareSEffect}/{@code LightFlareMEffect}/{@code LightFlareLEffect}/
 * {@code CeilingDustCloudEffect}, the {@code vfx-misc-root}
 * {@code FireBurstParticleEffect}/{@code NemesisFireParticle}, and the {@code vfx-combat}
 * {@code FlashAtkImgEffect}/{@code RedFireBurstParticleEffect}/{@code SmokeBlurEffect}; the four
 * later members are the {@code vfx-scene-world} {@code TorchParticleXLEffect} and the
 * {@code vfx-misc-root} {@code GhostlyWeakFireEffect}/{@code GenericSmokeEffect}/
 * {@code ExhaustBlurEffect}, and the two newest are the {@code vfx-combat} bare-{@code Texture}
 * {@code IceShatterEffect}/{@code WebParticleEffect}; the four newest members are the
 * {@code vfx-combat} {@code EntangleEffect} (byte-identical to {@code WebParticleEffect}) and
 * {@code BlockImpactLineEffect}/{@code UnknownParticleEffect} plus the {@code vfx-misc-root}
 * {@code ExhaustPileParticle}),
 * mirroring the native render formula exactly; the five newest members are the {@code vfx-combat}
 * {@code FlameParticleEffect}/{@code LightningOrbActivateEffect}/{@code DamageImpactBlurEffect}/
 * {@code DamageImpactLineEffect}/{@code DarkOrbPassiveEffect}, and the three newest are the
 * {@code vfx-misc-root} {@code WarningSignEffect}, the {@code vfx-combat} {@code StunStarEffect}, and
 * the {@code vfx-misc-root} {@code FallingDustEffect} (in that order).
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
 * {@code GenericSmokeEffect}, {@code ExhaustBlurEffect}, {@code BlockImpactLineEffect},
 * {@code ExhaustPileParticle}, {@code UnknownParticleEffect}, {@code DamageImpactBlurEffect},
 * {@code DamageImpactLineEffect}, {@code StunStarEffect}, and {@code FallingDustEffect}
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
 *   IceShatterEffect.render (note: additive blend; uses the rotation field; vY is update-only):
 *     sb.draw(img, x, y, 32f, 32f, 64f, 64f, scale, scale, rotation,
 *             0, 0, 64, 64, false, false)
 *   WebParticleEffect.render (note: additive blend; rotation hardcoded to 0f):
 *     sb.draw(ImageMaster.WEB_VFX, x, y, 32f, 32f, 64f, 64f, scale, scale, 0f,
 *             0, 0, 64, 64, false, false)
 *   EntangleEffect.render (note: additive blend; rotation hardcoded to 0f; byte-identical to Web;
 *                          no rotation field, no img field):
 *     sb.draw(ImageMaster.WEB_VFX, x, y, 32f, 32f, 64f, 64f, scale, scale, 0f,
 *             0, 0, 64, 64, false, false)
 *   BlockImpactLineEffect.render (note: no setBlendFunction; ambient blend):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   ExhaustPileParticle.render (note: no setBlendFunction; ambient blend; img is private static
 *                               and declared on the class):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   UnknownParticleEffect.render (note: no setBlendFunction; ambient blend; uses the rotation
 *                                 field and its own instance Texture img):
 *     sb.draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f, scale, scale, rotation,
 *             0, 0, 128, 128, false, false)
 *   FlameParticleEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   LightningOrbActivateEffect.render (note: additive blend):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DamageImpactBlurEffect.render (note: no setBlendFunction; ambient blend; no isDone guard):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DamageImpactLineEffect.render (note: no setBlendFunction; ambient blend; if (!isDone) guard):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DarkOrbPassiveEffect.render (note: additive blend; uses the rotation field and its own instance
 *                                 Texture img; src 0,0,74,74 is the full region):
 *     sb.draw(img, x - 37f, y - 37f, 37f, 37f, 74f, 74f, scale, scale, rotation,
 *             0, 0, 74, 74, false, false)
 *   WarningSignEffect.render (note: additive blend; rotation hardcoded to 0f; the uniform scale is
 *                              the hardcoded Settings.scale * 2f, NOT an effect scale field, which
 *                              the class does not have):
 *     sb.draw(ImageMaster.WARNING_ICON_VFX, x - 32f, y - 32f, 32f, 32f, 64f, 64f,
 *             Settings.scale*2f, Settings.scale*2f, 0f, 0, 0, 64, 64, false, false)
 *   StunStarEffect.render (note: no setBlendFunction; ambient blend; the draw POSITION is offset by
 *                          the effect's own vX/vY, both scaled by Settings.scale):
 *     sb.draw(img, x - vX*30f*Settings.scale, y - vY*5f*Settings.scale,
 *             pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   FallingDustEffect.render (note: no setBlendFunction; ambient blend; the ORIGIN is the region's
 *                             own offsetX/offsetY, NOT packed/2):
 *     sb.draw(img, x, y, img.offsetX, img.offsetY, pw, ph, scale, scale, rotation)
 * </pre>
 *
 * where {@code pw}/{@code ph} are the region's {@code packedWidth}/{@code packedHeight}. The
 * bare-{@code Texture} kinds — Calm, Shield, Debuff, IceShatter, Web, Entangle, Unknown, WarningSign,
 * and
 * DarkOrb — draw a
 * fixed source rect
 * rather than a packed region, so their native origin/size/source rect are host-neutral constants
 * and the packed region size is ignored; Shield, Web, Entangle, and WarningSign hardcode rotation
 * {@code 0f},
 * Debuff, IceShatter, Unknown, and DarkOrb consume their {@code rotation} field, and Calm keeps its
 * {@code scaleY} formula. WarningSign is the only kind whose uniform scale is a hardcoded
 * {@code settingsScale * 2f} rather than the effect's own {@code scale} field (it has none). Web and
 * Entangle
 * are the only kinds whose native {@code render} rewrites the set color, forcing RGB to white
 * and taking alpha from the effect's color (see {@link #whiteAlphaOnly}). {@code DivinityStanceChangeParticle}, the
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
 * {@code ExhaustBlurEffect}/{@code DamageImpactBlurEffect}/{@code DamageImpactLineEffect} never
 * switch blend function, so
 * {@link #additiveBlend} reports
 * {@code false} for them, while the two fire bursts, {@code TorchParticleXLEffect},
 * {@code GhostlyWeakFireEffect}, {@code FlameParticleEffect}, {@code LightningOrbActivateEffect},
 * and {@code ShieldParticleEffect} are additive
 * like the rest. {@code ShieldParticleEffect}/{@code DebuffParticleEffect} are the first two
 * members beyond {@code CalmParticleEffect} to draw a bare {@code Texture}, so they join the
 * fixed-source-rect shape via their own geometry branches rather than the packed-region branches;
 * the two newest members {@code IceShatterEffect}/{@code WebParticleEffect} join that same
 * bare-{@code Texture} shape, with {@code IceShatterEffect} consuming its {@code rotation} field
 * ({@link Kind#ICE_SHATTER}) and {@code WebParticleEffect} hardcoding rotation {@code 0f} and
 * forcing its set color to {@code (1, 1, 1, color.a)} ({@link Kind#WEB_PARTICLE}).
 * The four newest members reuse the three existing shapes with a single new fixed rect:
 * {@code EntangleEffect} ({@link Kind#ENTANGLE}) is byte-identical to {@code WebParticleEffect} —
 * same static {@code ImageMaster.WEB_VFX} texture, offset 0, origin {@code (32, 32)}, size
 * {@code (64, 64)}, src {@code (0, 0, 64, 64)}, hardcoded rotation {@code 0f}, additive blend, and
 * the white-alpha set-color rule (so {@link #whiteAlphaOnly} is true for it too) — {@code
 * BlockImpactLineEffect} ({@link Kind#BLOCK_IMPACT_LINE}) and {@code ExhaustPileParticle}
 * ({@link Kind#EXHAUST_PILE}) reuse the ambient center-packed {@link Kind#STANCE_AURA} geometry
 * ({@code ExhaustPileParticle.img} is a {@code private static} {@code AtlasRegion} declared on the
 * class), and {@code UnknownParticleEffect} ({@link Kind#UNKNOWN_PARTICLE}) is a NEW ambient
 * fixed-rect formula — offset {@code (-64, -64)}, origin {@code (64, 64)}, size {@code (128, 128)},
 * src {@code (0, 0, 128, 128)}, consuming its {@code rotation} field — over its own instance
 * {@code Texture img}. It is the first kind whose fixed rect is not {@code (64, 64)} or
 * {@code (32, 32)}, and it draws under the ambient blend (no {@code setBlendFunction}).
 * The five newest members are all {@code vfx-combat} and again reuse the two existing shapes:
 * {@code FlameParticleEffect} ({@link Kind#FLAME_PARTICLE}) and
 * {@code LightningOrbActivateEffect} ({@link Kind#LIGHTNING_ORB_ACTIVATE}) reuse the additive
 * center-packed geometry, {@code DamageImpactBlurEffect} ({@link Kind#DAMAGE_IMPACT_BLUR}) and
 * {@code DamageImpactLineEffect} ({@link Kind#DAMAGE_IMPACT_LINE}) reuse that same geometry under
 * the ambient blend (neither calls {@code setBlendFunction}; only {@code DamageImpactLineEffect}
 * guards its draw with {@code if (!isDone)}), and {@code DarkOrbPassiveEffect}
 * ({@link Kind#DARK_ORB_PASSIVE}) is a NEW
 * additive shape-C fixed-rect formula — offset {@code (-37, -37)}, origin {@code (37, 37)}, size
 * {@code (74, 74)}, src {@code (0, 0, 74, 74)}, the full 74&times;74 region, consuming its
 * {@code rotation} field — over its own instance {@code Texture img}.
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
        EXHAUST_BLUR,
        ICE_SHATTER,
        WEB_PARTICLE,
        ENTANGLE,
        BLOCK_IMPACT_LINE,
        EXHAUST_PILE,
        UNKNOWN_PARTICLE,
        FLAME_PARTICLE,
        LIGHTNING_ORB_ACTIVATE,
        DAMAGE_IMPACT_BLUR,
        DAMAGE_IMPACT_LINE,
        DARK_ORB_PASSIVE,
        WARNING_SIGN,
        STUN_STAR,
        FALLING_DUST
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

    // Native IceShatterEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of its own instance img Texture. The rotation comes from the field.
    /** Native IceShatter draw origin x ({@code 32f}). */
    public static final float ICE_SHATTER_ORIGIN_X = 32f;
    /** Native IceShatter draw origin y ({@code 32f}). */
    public static final float ICE_SHATTER_ORIGIN_Y = 32f;
    /** Native IceShatter draw width ({@code 64f}). */
    public static final float ICE_SHATTER_WIDTH = 64f;
    /** Native IceShatter draw height ({@code 64f}). */
    public static final float ICE_SHATTER_HEIGHT = 64f;
    /** Native IceShatter draw source rect x ({@code 0}). */
    public static final int ICE_SHATTER_SRC_X = 0;
    /** Native IceShatter draw source rect y ({@code 0}). */
    public static final int ICE_SHATTER_SRC_Y = 0;
    /** Native IceShatter draw source rect width ({@code 64}). */
    public static final int ICE_SHATTER_SRC_W = 64;
    /** Native IceShatter draw source rect height ({@code 64}). */
    public static final int ICE_SHATTER_SRC_H = 64;

    // Native WebParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the static ImageMaster.WEB_VFX Texture. The rotation is hardcoded to 0.
    /** Native Web draw origin x ({@code 32f}). */
    public static final float WEB_ORIGIN_X = 32f;
    /** Native Web draw origin y ({@code 32f}). */
    public static final float WEB_ORIGIN_Y = 32f;
    /** Native Web draw width ({@code 64f}). */
    public static final float WEB_WIDTH = 64f;
    /** Native Web draw height ({@code 64f}). */
    public static final float WEB_HEIGHT = 64f;
    /** Native Web draw source rect x ({@code 0}). */
    public static final int WEB_SRC_X = 0;
    /** Native Web draw source rect y ({@code 0}). */
    public static final int WEB_SRC_Y = 0;
    /** Native Web draw source rect width ({@code 64}). */
    public static final int WEB_SRC_W = 64;
    /** Native Web draw source rect height ({@code 64}). */
    public static final int WEB_SRC_H = 64;

    // Native UnknownParticleEffect draw constants (see the class Javadoc): fixed offset/origin/size
    // and the fixed source rect of its own instance img Texture. The rotation comes from the field.
    /** Native Unknown draw offset/origin ({@code 64f}). */
    public static final float UNKNOWN_OFFSET = 64f;
    /** Native Unknown draw origin ({@code 64f}). */
    public static final float UNKNOWN_ORIGIN = 64f;
    /** Native Unknown draw width/height ({@code 128f}). */
    public static final float UNKNOWN_SIZE = 128f;
    /** Native Unknown draw source rect x ({@code 0}). */
    public static final int UNKNOWN_SRC_X = 0;
    /** Native Unknown draw source rect y ({@code 0}). */
    public static final int UNKNOWN_SRC_Y = 0;
    /** Native Unknown draw source rect width ({@code 128}). */
    public static final int UNKNOWN_SRC_W = 128;
    /** Native Unknown draw source rect height ({@code 128}). */
    public static final int UNKNOWN_SRC_H = 128;

    // Native DarkOrbPassiveEffect draw constants (see the class Javadoc): fixed
    // offset/origin/size and the fixed source rect (0, 0, 74, 74 — the full 74x74 region) over its
    // own instance img Texture. The rotation comes from the field.
    /** Native DarkOrb draw offset/origin ({@code 37f}). */
    public static final float DARK_ORB_OFFSET = 37f;
    /** Native DarkOrb draw origin ({@code 37f}). */
    public static final float DARK_ORB_ORIGIN = 37f;
    /** Native DarkOrb draw width/height ({@code 74f}). */
    public static final float DARK_ORB_SIZE = 74f;
    /** Native DarkOrb draw source rect x ({@code 0}). */
    public static final int DARK_ORB_SRC_X = 0;
    /** Native DarkOrb draw source rect y ({@code 0}). */
    public static final int DARK_ORB_SRC_Y = 0;
    /** Native DarkOrb draw source rect width ({@code 74}, the full region). */
    public static final int DARK_ORB_SRC_W = 74;
    /** Native DarkOrb draw source rect height ({@code 74}, the full region). */
    public static final int DARK_ORB_SRC_H = 74;

    // Native WarningSignEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the static ImageMaster.WARNING_ICON_VFX Texture. The rotation is
    // hardcoded to 0 and the uniform scale is the hardcoded Settings.scale * 2f (the effect has no
    // scale field).
    /** Native WarningSign draw origin x ({@code 32f}). */
    public static final float WARNING_ORIGIN_X = 32f;
    /** Native WarningSign draw origin y ({@code 32f}). */
    public static final float WARNING_ORIGIN_Y = 32f;
    /** Native WarningSign draw width ({@code 64f}). */
    public static final float WARNING_WIDTH = 64f;
    /** Native WarningSign draw height ({@code 64f}). */
    public static final float WARNING_HEIGHT = 64f;
    /** Native WarningSign uniform scale multiplier ({@code 2f}, applied to {@code Settings.scale}). */
    public static final float WARNING_SCALE_FACTOR = 2f;
    /** Native WarningSign draw source rect x ({@code 0}). */
    public static final int WARNING_SRC_X = 0;
    /** Native WarningSign draw source rect y ({@code 0}). */
    public static final int WARNING_SRC_Y = 0;
    /** Native WarningSign draw source rect width ({@code 64}). */
    public static final int WARNING_SRC_W = 64;
    /** Native WarningSign draw source rect height ({@code 64}). */
    public static final int WARNING_SRC_H = 64;

    // Native StunStarEffect position-offset multipliers (see the class Javadoc): the draw position
    // is x - vX * 30f * Settings.scale, y - vY * 5f * Settings.scale.
    /** StunStar vX position-offset multiplier ({@code 30f}). */
    public static final float STUN_STAR_VX_FACTOR = 30f;
    /** StunStar vY position-offset multiplier ({@code 5f}). */
    public static final float STUN_STAR_VY_FACTOR = 5f;

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
        if (VfxClaimPolicy.ICE_SHATTER.equals(value)) return Kind.ICE_SHATTER;
        if (VfxClaimPolicy.WEB_PARTICLE.equals(value)) return Kind.WEB_PARTICLE;
        if (VfxClaimPolicy.ENTANGLE_EFFECT.equals(value)) return Kind.ENTANGLE;
        if (VfxClaimPolicy.BLOCK_IMPACT_LINE.equals(value)) return Kind.BLOCK_IMPACT_LINE;
        if (VfxClaimPolicy.EXHAUST_PILE_PARTICLE.equals(value)) return Kind.EXHAUST_PILE;
        if (VfxClaimPolicy.UNKNOWN_PARTICLE.equals(value)) return Kind.UNKNOWN_PARTICLE;
        if (VfxClaimPolicy.FLAME_PARTICLE.equals(value)) return Kind.FLAME_PARTICLE;
        if (VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE.equals(value)) {
            return Kind.LIGHTNING_ORB_ACTIVATE;
        }
        if (VfxClaimPolicy.DAMAGE_IMPACT_BLUR.equals(value)) return Kind.DAMAGE_IMPACT_BLUR;
        if (VfxClaimPolicy.DAMAGE_IMPACT_LINE.equals(value)) return Kind.DAMAGE_IMPACT_LINE;
        if (VfxClaimPolicy.DARK_ORB_PASSIVE.equals(value)) return Kind.DARK_ORB_PASSIVE;
        if (VfxClaimPolicy.WARNING_SIGN.equals(value)) return Kind.WARNING_SIGN;
        if (VfxClaimPolicy.STUN_STAR.equals(value)) return Kind.STUN_STAR;
        if (VfxClaimPolicy.FALLING_DUST.equals(value)) return Kind.FALLING_DUST;
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
     * {@link Kind#DEBUFF_PARTICLE}, {@link Kind#GENERIC_SMOKE}, {@link Kind#EXHAUST_BLUR},
     * {@link Kind#BLOCK_IMPACT_LINE}, {@link Kind#EXHAUST_PILE}, {@link Kind#UNKNOWN_PARTICLE},
     * {@link Kind#DAMAGE_IMPACT_BLUR}, {@link Kind#DAMAGE_IMPACT_LINE}, {@link Kind#STUN_STAR},
     * {@link Kind#FALLING_DUST})
     * never call
     * {@code setBlendFunction} at all, so the host draw must not install or restore a blend function
     * for them. {@link Kind#FLASH_ATK_IMG} was the first such kind; the smoke blur, ceiling dust, and
     * nemesis fire are the first ambient members beyond it, {@link Kind#DEBUFF_PARTICLE} is the
     * first ambient bare-{@code Texture} member, {@link Kind#GENERIC_SMOKE}/{@link
     * Kind#EXHAUST_BLUR} are earlier ambient packed-region members, the earlier ambient
     * members {@link Kind#BLOCK_IMPACT_LINE}/{@link Kind#EXHAUST_PILE} (ambient center-packed)
     * plus {@link Kind#UNKNOWN_PARTICLE} (the first ambient bare-{@code Texture} member of the new
     * 128-rect) and {@link Kind#DAMAGE_IMPACT_BLUR}/{@link Kind#DAMAGE_IMPACT_LINE} (ambient
     * center-packed) are not the newest any more; the two newest ambient members are
     * {@link Kind#STUN_STAR}/{@link Kind#FALLING_DUST} (ambient center-packed, adding only a position
     * offset and a region-offset origin respectively). Every other kind — including
     * the two fire
     * bursts ({@link Kind#FIRE_BURST}, {@link Kind#RED_FIRE_BURST}), the additive bare-texture
     * {@link Kind#SHIELD_PARTICLE}, the additive {@link Kind#TORCH_PARTICLE_XL}/{@link
     * Kind#GHOSTLY_WEAK_FIRE}, the two additive bare-texture members
     * {@link Kind#ICE_SHATTER}/{@link Kind#WEB_PARTICLE}, {@link Kind#ENTANGLE}, the additive
     * center-packed members {@link Kind#FLAME_PARTICLE}/{@link Kind#LIGHTNING_ORB_ACTIVATE},
     * the additive bare-texture members {@link Kind#DARK_ORB_PASSIVE} and the newest additive
     * member {@link Kind#WARNING_SIGN} — is additive.
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
                && kind != Kind.EXHAUST_BLUR
                && kind != Kind.BLOCK_IMPACT_LINE
                && kind != Kind.EXHAUST_PILE
                && kind != Kind.UNKNOWN_PARTICLE
                && kind != Kind.DAMAGE_IMPACT_BLUR
                && kind != Kind.DAMAGE_IMPACT_LINE
                && kind != Kind.STUN_STAR
                && kind != Kind.FALLING_DUST;
    }

    /**
     * Pure per-kind predicate: {@code true} only for the kinds whose native {@code render} can
     * legitimately produce no pixels at all because it guards the draw on a present image. Today
     * that is exactly {@link Kind#FLASH_ATK_IMG}, whose native {@code FlashAtkImgEffect.render}
     * wraps its {@code sb.draw} in {@code if (img != null)} — so an instance with a null
     * {@code img} draws nothing natively, and an ART fail-open on that instance loses no pixels.
     * Every other claimable kind draws unconditionally (or draws a fixed static texture), so it is
     * {@code false}. Callers use this to classify a declined claim as a benign no-pixel decline
     * rather than a {@code dispositionMismatch}.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean nativeSkipsDrawWithoutImage(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.FLASH_ATK_IMG;
    }

    /**
     * Pure per-kind color rule for the bare-{@code Texture} shape: {@code true} only for
     * {@link Kind#WEB_PARTICLE} and {@link Kind#ENTANGLE}, whose native {@code render} does not pass
     * the effect's own {@code color} to {@code setColor} but instead builds
     * {@code new Color(1f, 1f, 1f, color.a)} — i.e. it forces the RGB channels to white and takes
     * only the alpha from the effect's color ({@code EntangleEffect} is byte-identical to
     * {@code WebParticleEffect}). Every other kind (including the other bare-{@code Texture} members
     * {@link Kind#CALM_PARTICLE}, {@link Kind#SHIELD_PARTICLE}, {@link Kind#DEBUFF_PARTICLE},
     * {@link Kind#ICE_SHATTER}, {@link Kind#UNKNOWN_PARTICLE}, {@link Kind#WARNING_SIGN}, and
     * {@link Kind#DARK_ORB_PASSIVE})
     * sets the effect's {@code color}
     * unchanged, so the host draw must not rewrite its RGB.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean whiteAlphaOnly(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.WEB_PARTICLE || kind == Kind.ENTANGLE;
    }

    /**
     * Pure geometry for one claim. The caller supplies the effect field floats and the packed
     * region size; the per-kind color/blend state is applied by the host draw (see
     * {@link #additiveBlend}: additive kinds install/restore {@code 770/1}-&rarr;{@code 770/771},
     * while the ambient kinds ({@code FLASH_ATK_IMG}, {@code SMOKE_BLUR}, {@code CEILING_DUST},
     * {@code NEMESIS_FIRE}, {@code DEBUFF_PARTICLE}, {@code GENERIC_SMOKE}, {@code EXHAUST_BLUR},
     * {@code BLOCK_IMPACT_LINE}, {@code EXHAUST_PILE}, {@code UNKNOWN_PARTICLE},
     * {@code DAMAGE_IMPACT_BLUR}, {@code DAMAGE_IMPACT_LINE}, {@code STUN_STAR},
     * {@code FALLING_DUST})
     * leave the ambient blend untouched and restore
     * only color; see {@link #whiteAlphaOnly} for the two kinds ({@code WEB_PARTICLE} and
     * {@code ENTANGLE}) that also rewrite their set color's
     * RGB to white).
     *
     * <p>The three trailing scalars were added for the two newest kinds and default to {@code 0} for
     * every other caller: {@code vX} is the effect's own horizontal velocity used only by
     * {@code STUN_STAR} (whose draw position is shifted by it), while {@code regionOffsetX}/
     * {@code regionOffsetY} are the region's own trim offsets used as the draw origin only by
     * {@code FALLING_DUST} (whose origin is NOT {@code packedWidth/2}, {@code packedHeight/2}).
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static Params params(Kind kind, float x, float y, float vY, float scale, float rotation,
            float durDiv2, float duration, float settingsScale,
            float packedWidth, float packedHeight,
            float vX, float regionOffsetX, float regionOffsetY) {
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
            case BLOCK_IMPACT_LINE:
            case EXHAUST_PILE:
            case FLAME_PARTICLE:
            case LIGHTNING_ORB_ACTIVATE:
            case DAMAGE_IMPACT_BLUR:
            case DAMAGE_IMPACT_LINE:
                // DivinityStanceChangeParticle, the cross-family LightFlareSEffect/MEffect/LEffect,
                // TorchParticleLEffect, the vfx-combat FlashAtkImgEffect, the two fire bursts, the
                // smoke blur, the ceiling dust, the nemesis fire, TorchParticleXLEffect,
                // GhostlyWeakFireEffect, GenericSmokeEffect, and ExhaustBlurEffect mirror
                // StanceAuraEffect exactly: x/y passthrough (no vY is consumed; every member that
                // owns a vY field — TorchParticleLEffect and all nine newer members — uses it only
                // in update()), center origin, packed size, uniform scale. Flash, the smoke blur,
                // the ceiling dust, the nemesis fire, the generic smoke, and the exhaust blur differ
                // only in blend (ambient, via additiveBlend == false). The five newest members
                // mirror the same geometry: FlameParticleEffect and LightningOrbActivateEffect are
                // additive while DamageImpactBlurEffect and DamageImpactLineEffect never call
                // setBlendFunction (ambient, again only additiveBlend differs).
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
            case ICE_SHATTER:
                // Native IceShatterEffect ignores the (absent) region: fixed origin/size and the
                // field rotation; packedWidth/packedHeight, vY, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x, y, ICE_SHATTER_ORIGIN_X, ICE_SHATTER_ORIGIN_Y,
                        ICE_SHATTER_WIDTH, ICE_SHATTER_HEIGHT, scale, scale, rotation);
            case WEB_PARTICLE:
                // Native WebParticleEffect ignores the (absent) region: fixed origin/size and a
                // hardcoded zero rotation; packedWidth/packedHeight, vY, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x, y, WEB_ORIGIN_X, WEB_ORIGIN_Y,
                        WEB_WIDTH, WEB_HEIGHT, scale, scale, 0f);
            case ENTANGLE:
                // Native EntangleEffect is byte-identical to WebParticleEffect: the static
                // ImageMaster.WEB_VFX Texture, fixed origin/size, and a hardcoded zero rotation
                // (EntangleEffect has no rotation field); packedWidth/packedHeight, vY, dur_div2,
                // duration, and Settings.scale are unused. Reuses the WEB constants so the two kinds
                // share one static-texture/white-alpha configuration.
                return new Params(x, y, WEB_ORIGIN_X, WEB_ORIGIN_Y,
                        WEB_WIDTH, WEB_HEIGHT, scale, scale, 0f);
            case UNKNOWN_PARTICLE:
                // Native UnknownParticleEffect ignores the (absent) region: a new fixed
                // offset/origin/size rect and the field rotation; packedWidth/packedHeight, vY,
                // dur_div2, duration, and Settings.scale are unused.
                return new Params(x - UNKNOWN_OFFSET, y - UNKNOWN_OFFSET,
                        UNKNOWN_ORIGIN, UNKNOWN_ORIGIN, UNKNOWN_SIZE, UNKNOWN_SIZE,
                        scale, scale, rotation);
            case DARK_ORB_PASSIVE:
                // Native DarkOrbPassiveEffect ignores the (absent) region: a new fixed
                // offset/origin/size rect and the field rotation; packedWidth/packedHeight, vY,
                // dur_div2, duration, and Settings.scale are unused. Its src rect is (0, 0, 74, 74),
                // the full 74x74 region the effect passes natively.
                return new Params(x - DARK_ORB_OFFSET, y - DARK_ORB_OFFSET,
                        DARK_ORB_ORIGIN, DARK_ORB_ORIGIN, DARK_ORB_SIZE, DARK_ORB_SIZE,
                        scale, scale, rotation);
            case WARNING_SIGN:
                // Native WarningSignEffect ignores the (absent) region and has no scale/rotation
                // field either: a fixed 64x64 rect and a hardcoded additive uniform scale of
                // Settings.scale * 2f (with a hardcoded zero rotation). packedWidth/packedHeight,
                // vY, vX, the region offsets, dur_div2, duration, and the effect scale/rotation
                // inputs are all unused.
                return new Params(x - WARNING_ORIGIN_X, y - WARNING_ORIGIN_Y,
                        WARNING_ORIGIN_X, WARNING_ORIGIN_Y, WARNING_WIDTH, WARNING_HEIGHT,
                        settingsScale * WARNING_SCALE_FACTOR, settingsScale * WARNING_SCALE_FACTOR,
                        0f);
            case STUN_STAR:
                // Native StunStarEffect reuses the ambient center-packed geometry but shifts the
                // draw POSITION by -(vX * 30f * Settings.scale), -(vY * 5f * Settings.scale); origin
                // packed/2, size packed, uniform scale, and the field rotation are as usual.
                return new Params(
                        x - vX * STUN_STAR_VX_FACTOR * settingsScale,
                        y - vY * STUN_STAR_VY_FACTOR * settingsScale,
                        originX, originY, packedWidth, packedHeight, scale, scale, rotation);
            case FALLING_DUST:
                // Native FallingDustEffect reuses the ambient center-packed geometry except that its
                // ORIGIN is the region's own offsetX/offsetY (NOT packedWidth/2, packedHeight/2);
                // size packed, uniform scale, and the field rotation are as usual.
                return new Params(x, y, regionOffsetX, regionOffsetY, packedWidth, packedHeight,
                        scale, scale, rotation);
            default:
                throw new IllegalArgumentException("unhandled kind: " + kind);
        }
    }
}
