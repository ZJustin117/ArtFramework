package artframework.sts1.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure per-instance claim predicate for the default-off, family-neutral transient-effect claim seam.
 *
 * <p>This is a data predicate, not a per-subclass {@code @SpirePatch}: NRO-04 forbids per-subclass
 * hooks, so the claimable classes are recognized by FQN at the shared effect-container seam. The
 * seam is generic over per-instance transient-effect families, so the current members are
 * cross-family: the five {@code vfx-stance-aura} FQNs plus the {@code vfx-scene-world}
 * {@code LightFlareSEffect}, {@code LightFlareMEffect}, {@code LightFlareLEffect}, and
 * {@code TorchParticleLEffect}, the {@code vfx-combat} {@code FlashAtkImgEffect},
 * {@code RedFireBurstParticleEffect}, and {@code SmokeBlurEffect}, the {@code vfx-misc-root}
 * {@code FireBurstParticleEffect} and {@code NemesisFireParticle}, and the {@code vfx-scene-world}
 * {@code CeilingDustCloudEffect}. The aura
 * members are the four {@code AtlasRegion}-drawn classes
 * ({@code StanceAuraEffect}, {@code WrathParticleEffect}, {@code DivinityParticleEffect},
 * {@code DivinityStanceChangeParticle}) plus {@code CalmParticleEffect} (which draws a bare
 * {@code Texture}); {@code LightFlareSEffect} is the first non-aura member and draws an
 * {@code AtlasRegion} with the same additive geometry as {@code StanceAuraEffect}; the three
 * later {@code vfx-scene-world} members ({@code LightFlareMEffect}, {@code LightFlareLEffect},
 * {@code TorchParticleLEffect}) are geometry-identical to that additive center-packed branch;
 * {@code TorchParticleLEffect} has a {@code vY} field but its native {@code render} ignores it
 * (it is used only by {@code update()}), so it stays on the optional-{@code vY} reader path;
 * {@code FlashAtkImgEffect} is the first {@code vfx-combat} member and reuses the same geometry but
 * never switches blend function (ambient blend, see
 * {@link VfxDrawGeometry#additiveBlend}); the two fire-burst members
 * ({@code FireBurstParticleEffect} in {@code vfx-misc-root} and {@code RedFireBurstParticleEffect}
 * in {@code vfx-combat}) are additive and reuse that same additive center-packed geometry; the
 * three ambient members ({@code SmokeBlurEffect}, {@code CeilingDustCloudEffect}, and
 * {@code NemesisFireParticle}) also reuse the geometry but never call
 * {@code setBlendFunction} (ambient blend, like {@code FlashAtkImgEffect}). All five new members
 * have a {@code vY} field that their native {@code render} ignores (used only by
 * {@code update()}), so they all stay on the optional-{@code vY} reader path. The two newest
 * members introduce the third draw shape, a bare {@code Texture} + fixed source rect (the first
 * being {@code CalmParticleEffect}, which became kind-driven): {@code ShieldParticleEffect}
 * paints the static {@code ImageMaster.INTENT_DEFEND} {@code Texture} additively with a
 * hardcoded zero rotation, and {@code DebuffParticleEffect} paints its own instance
 * {@code Texture} {@code img} under the ambient blend and consumes its {@code rotation} field.
 * The four newest members also reuse the additive center-packed geometry with a single
 * {@code sb.draw(img, x, y, img.packedWidth/2f, img.packedHeight/2f, img.packedWidth,
 * img.packedHeight, scale, scale, rotation)}: the {@code vfx-scene-world}
 * {@code TorchParticleXLEffect} and the {@code vfx-misc-root} {@code GhostlyWeakFireEffect} are
 * additive ({@code setBlendFunction(770, 1)} before and {@code (770, 771)} after), while the
 * {@code vfx-misc-root} {@code GenericSmokeEffect} and {@code ExhaustBlurEffect} never call
 * {@code setBlendFunction} (ambient blend). All four own a {@code vY} used only by
 * {@code update()} (never in {@code render}), so they stay on the optional-{@code vY} reader
 * path.
 * The additive
 * members are the only ones whose host draw installs additive blend; every member may be claimed
 * per instance.
 */
public final class VfxClaimPolicy {

    public static final String STANCE_AURA_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect";
    public static final String WRATH_PARTICLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect";
    public static final String DIVINITY_PARTICLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffect";
    public static final String CALM_PARTICLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect";
    public static final String DIVINITY_STANCE_CHANGE_PARTICLE =
            "com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle";
    public static final String SCENE_LIGHT_FLARE =
            "com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect";
    public static final String SCENE_LIGHT_FLARE_M =
            "com.megacrit.cardcrawl.vfx.scene.LightFlareMEffect";
    public static final String SCENE_LIGHT_FLARE_L =
            "com.megacrit.cardcrawl.vfx.scene.LightFlareLEffect";
    public static final String SCENE_TORCH_PARTICLE_L =
            "com.megacrit.cardcrawl.vfx.scene.TorchParticleLEffect";
    public static final String FLASH_ATK_IMG =
            "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect";
    public static final String FIRE_BURST =
            "com.megacrit.cardcrawl.vfx.FireBurstParticleEffect";
    public static final String RED_FIRE_BURST =
            "com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect";
    public static final String SMOKE_BLUR =
            "com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect";
    public static final String CEILING_DUST =
            "com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect";
    public static final String NEMESIS_FIRE =
            "com.megacrit.cardcrawl.vfx.NemesisFireParticle";
    public static final String SHIELD_PARTICLE =
            "com.megacrit.cardcrawl.vfx.ShieldParticleEffect";
    public static final String DEBUFF_PARTICLE =
            "com.megacrit.cardcrawl.vfx.DebuffParticleEffect";
    public static final String SCENE_TORCH_PARTICLE_XL =
            "com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect";
    public static final String GHOSTLY_WEAK_FIRE =
            "com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect";
    public static final String GENERIC_SMOKE =
            "com.megacrit.cardcrawl.vfx.GenericSmokeEffect";
    public static final String EXHAUST_BLUR =
            "com.megacrit.cardcrawl.vfx.ExhaustBlurEffect";

    private static final List<String> SUPPORTED_CLASSES = Collections.unmodifiableList(
            Arrays.asList(STANCE_AURA_EFFECT, WRATH_PARTICLE_EFFECT, DIVINITY_PARTICLE_EFFECT,
                    CALM_PARTICLE_EFFECT, DIVINITY_STANCE_CHANGE_PARTICLE, SCENE_LIGHT_FLARE,
                    FLASH_ATK_IMG, SCENE_LIGHT_FLARE_M, SCENE_LIGHT_FLARE_L,
                    SCENE_TORCH_PARTICLE_L, FIRE_BURST, RED_FIRE_BURST, SMOKE_BLUR,
                    CEILING_DUST, NEMESIS_FIRE, SHIELD_PARTICLE, DEBUFF_PARTICLE,
                    SCENE_TORCH_PARTICLE_XL, GHOSTLY_WEAK_FIRE, GENERIC_SMOKE, EXHAUST_BLUR));

    private VfxClaimPolicy() {}

    /** True only for the exact claimable FQNs; null, blank, and every other class fail open. */
    public static boolean supports(String nativeClassName) {
        if (nativeClassName == null) return false;
        String value = nativeClassName.trim();
        if (value.isEmpty()) return false;
        return STANCE_AURA_EFFECT.equals(value)
                || WRATH_PARTICLE_EFFECT.equals(value)
                || DIVINITY_PARTICLE_EFFECT.equals(value)
                || CALM_PARTICLE_EFFECT.equals(value)
                || DIVINITY_STANCE_CHANGE_PARTICLE.equals(value)
                || SCENE_LIGHT_FLARE.equals(value)
                || FLASH_ATK_IMG.equals(value)
                || SCENE_LIGHT_FLARE_M.equals(value)
                || SCENE_LIGHT_FLARE_L.equals(value)
                || SCENE_TORCH_PARTICLE_L.equals(value)
                || FIRE_BURST.equals(value)
                || RED_FIRE_BURST.equals(value)
                || SMOKE_BLUR.equals(value)
                || CEILING_DUST.equals(value)
                || NEMESIS_FIRE.equals(value)
                || SHIELD_PARTICLE.equals(value)
                || DEBUFF_PARTICLE.equals(value)
                || SCENE_TORCH_PARTICLE_XL.equals(value)
                || GHOSTLY_WEAK_FIRE.equals(value)
                || GENERIC_SMOKE.equals(value)
                || EXHAUST_BLUR.equals(value);
    }

    /**
     * The exact claimable FQNs, in their listed/append order (mirrored by {@link #supports});
     * the three newest scene-world members (m/l flare, l torch) are appended last rather than
     * following strict source-declaration order, and the five newest members (the two fire bursts,
     * the smoke blur, the ceiling dust, and the nemesis fire) are appended after those. The two
     * newest members ({@code ShieldParticleEffect}, {@code DebuffParticleEffect}) — the bare
     * {@code Texture} + fixed source-rect shape — are appended after the nemesis fire. The four
     * newest members (the {@code vfx-scene-world} {@code TorchParticleXLEffect}, the
     * {@code vfx-misc-root} {@code GhostlyWeakFireEffect}, {@code GenericSmokeEffect}, and
     * {@code ExhaustBlurEffect}) — all {@code AtlasRegion}-drawn with the same additive
     * center-packed geometry as {@code StanceAuraEffect} — are appended after the debuff particle in
     * that order; {@code TorchParticleXLEffect} and {@code GhostlyWeakFireEffect} are additive, while
     * {@code GenericSmokeEffect} and {@code ExhaustBlurEffect} are ambient (no
     * {@code setBlendFunction}), and all four own a {@code vY} that their native {@code render}
     * ignores (used only by {@code update()}).
     */
    public static List<String> supportedClasses() {
        return SUPPORTED_CLASSES;
    }
}
