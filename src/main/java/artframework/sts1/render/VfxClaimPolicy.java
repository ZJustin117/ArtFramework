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
 * {@code LightFlareSEffect} and the {@code vfx-combat} {@code FlashAtkImgEffect}. The aura members
 * are the four {@code AtlasRegion}-drawn classes
 * ({@code StanceAuraEffect}, {@code WrathParticleEffect}, {@code DivinityParticleEffect},
 * {@code DivinityStanceChangeParticle}) plus {@code CalmParticleEffect} (which draws a bare
 * {@code Texture}); {@code LightFlareSEffect} is the first non-aura member and draws an
 * {@code AtlasRegion} with the same additive geometry as {@code StanceAuraEffect};
 * {@code FlashAtkImgEffect} is the first {@code vfx-combat} member and reuses the same geometry but
 * never switches blend function (ambient blend, see
 * {@link VfxDrawGeometry#additiveBlend}). The additive members are the only ones whose host draw
 * installs additive blend; every member may be claimed per instance.
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
    public static final String FLASH_ATK_IMG =
            "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect";

    private static final List<String> SUPPORTED_CLASSES = Collections.unmodifiableList(
            Arrays.asList(STANCE_AURA_EFFECT, WRATH_PARTICLE_EFFECT, DIVINITY_PARTICLE_EFFECT,
                    CALM_PARTICLE_EFFECT, DIVINITY_STANCE_CHANGE_PARTICLE, SCENE_LIGHT_FLARE,
                    FLASH_ATK_IMG));

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
                || FLASH_ATK_IMG.equals(value);
    }

    /** The exact claimable FQNs, in declaration order. */
    public static List<String> supportedClasses() {
        return SUPPORTED_CLASSES;
    }
}
