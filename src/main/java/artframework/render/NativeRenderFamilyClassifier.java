package artframework.render;

/**
 * Host-neutral classifier for the native render family of an STS1 transient effect.
 *
 * <p>This is a deliberately <strong>controlled subset</strong> of the authoritative NRCC family
 * rules in {@code tools/nrcc/families.py}: only the {@code vfx.*} branch reachable through the
 * {@code AbstractGameEffect} container observation path is mirrored here. {@code families.py}
 * remains the authority for the full 25-family table (including method-regex rules such as
 * {@code overlay-targeting} / {@code room-backgrounds} that are not reachable from an effect
 * instance), and any future divergence must be resolved against that file.</p>
 *
 * <p>Rule order is significant and matches {@code _RULES} first-match-wins declaration order:
 * the five specific vfx subpackages are tested before the {@code vfx.} root package. Matching is
 * exact, case-sensitive FQN prefix matching on the native class name, so it is deterministic,
 * stateless and safe for concurrent read-only use.</p>
 *
 * <p>Unmatched input is <strong>fail-open</strong>: {@link #classify(String)} returns
 * {@link #FALLBACK_FAMILY} ({@code vfx-misc-root}) rather than throwing. That preserves the
 * adapter's pre-classifier behavior (every {@code effect:} owner was projected as
 * {@code vfx-misc-root}), matches the bridge's fail-open error semantics, and keeps the
 * transient-effect projection drain from aborting a whole pending batch on an unexpected class.
 * Callers that need the authority's "fail loudly" behavior can use {@link #classifyOrNull(String)}
 * and treat {@code null} as unclassified.</p>
 */
public final class NativeRenderFamilyClassifier {

    /** Prefix shared by every STS1 transient effect package. */
    public static final String VFX_PACKAGE_PREFIX = "com.megacrit.cardcrawl.vfx.";

    /** Effect-container fallback family, matching the authoritative {@code vfx.} root rule. */
    public static final String FALLBACK_FAMILY = "vfx-misc-root";

    // First-match-wins. Kept in the same order as tools/nrcc/families.py _RULES so the
    // specific subpackage rules shadow the vfx root rule. Do not reorder.
    private static final String[][] RULES = {
            {VFX_PACKAGE_PREFIX + "combat.", "vfx-combat"},
            {VFX_PACKAGE_PREFIX + "scene.", "vfx-scene-world"},
            {VFX_PACKAGE_PREFIX + "campfire.", "vfx-campfire-rest"},
            {VFX_PACKAGE_PREFIX + "cardManip.", "vfx-card-manipulation"},
            {VFX_PACKAGE_PREFIX + "stance.", "vfx-stance-aura"},
            {VFX_PACKAGE_PREFIX, FALLBACK_FAMILY},
    };

    private NativeRenderFamilyClassifier() {}

    /**
     * Returns the native render family for one effect native class name, or
     * {@link #FALLBACK_FAMILY} when no vfx rule matches (fail-open).
     */
    public static String classify(String nativeClassName) {
        String family = classifyOrNull(nativeClassName);
        return family != null ? family : FALLBACK_FAMILY;
    }

    /**
     * Strict variant: returns the matching family id, or {@code null} when the class name is null,
     * blank, or matches no rule in this controlled subset.
     */
    public static String classifyOrNull(String nativeClassName) {
        if (nativeClassName == null || nativeClassName.trim().isEmpty()) return null;
        for (String[] rule : RULES) {
            if (nativeClassName.startsWith(rule[0])) return rule[1];
        }
        return null;
    }
}
