package artframework.sts1.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure per-instance claim predicate for the {@code vfx-stance-aura} family.
 *
 * <p>This is a data predicate, not a per-subclass {@code @SpirePatch}: NRO-04 forbids per-subclass
 * hooks, so the claimable classes are recognized by FQN at the shared effect-container seam. The
 * three classes draw a private {@code AtlasRegion} with additive blend and are the only members
 * of the family ART may claim per instance.
 */
public final class AuraClaimPolicy {

    public static final String STANCE_AURA_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect";
    public static final String WRATH_PARTICLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect";
    public static final String DIVINITY_PARTICLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffect";

    private static final List<String> SUPPORTED_CLASSES = Collections.unmodifiableList(
            Arrays.asList(STANCE_AURA_EFFECT, WRATH_PARTICLE_EFFECT, DIVINITY_PARTICLE_EFFECT));

    private AuraClaimPolicy() {}

    /** True only for the exact three claimable FQNs; null, blank, and every other class fail open. */
    public static boolean supports(String nativeClassName) {
        if (nativeClassName == null) return false;
        String value = nativeClassName.trim();
        if (value.isEmpty()) return false;
        return STANCE_AURA_EFFECT.equals(value)
                || WRATH_PARTICLE_EFFECT.equals(value)
                || DIVINITY_PARTICLE_EFFECT.equals(value);
    }

    /** The exact claimable FQNs, in declaration order. */
    public static List<String> supportedClasses() {
        return SUPPORTED_CLASSES;
    }
}
