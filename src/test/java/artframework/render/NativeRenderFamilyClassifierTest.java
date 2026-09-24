package artframework.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NativeRenderFamilyClassifierTest {

    @Test
    public void mapsRepresentativeEffectClassesToTheirVfxFamily() {
        assertEquals("vfx-combat", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.combat.StrikeEffect"));
        assertEquals("vfx-scene-world", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.scene.DeathScreenFadeEffect"));
        assertEquals("vfx-campfire-rest", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.campfire.CampfireSleepEffect"));
        assertEquals("vfx-card-manipulation", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect"));
        assertEquals("vfx-stance-aura", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect"));
    }

    @Test
    public void rootAndUnknownEffectClassesFallBackToMiscRoot() {
        // The AbstractGameEffect base class itself lives directly in the vfx root package.
        assertEquals("vfx-misc-root", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.AbstractGameEffect"));
        assertEquals("vfx-misc-root", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.SpeechBubble"));
        assertEquals("vfx-misc-root", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.deprecated.DeprecatedEffect"));
    }

    @Test
    public void unknownNonVfxClassFallsBackButStrictVariantReturnsNull() {
        assertEquals("vfx-misc-root", NativeRenderFamilyClassifier.classify(
                "com.example.UnknownEffect"));
        assertNull(NativeRenderFamilyClassifier.classifyOrNull("com.example.UnknownEffect"));
    }

    @Test
    public void blankAndNullInputsUseDocumentedFallback() {
        assertEquals("vfx-misc-root", NativeRenderFamilyClassifier.classify(null));
        assertEquals("vfx-misc-root", NativeRenderFamilyClassifier.classify(""));
        assertEquals("vfx-misc-root", NativeRenderFamilyClassifier.classify("   "));
        assertNull(NativeRenderFamilyClassifier.classifyOrNull(null));
        assertNull(NativeRenderFamilyClassifier.classifyOrNull(""));
        assertNull(NativeRenderFamilyClassifier.classifyOrNull("   "));
    }

    @Test
    public void matchingIsCaseSensitiveAndDeterministic() {
        // FQN prefix matching is case-sensitive; a near-miss package must not match.
        assertEquals("vfx-misc-root", NativeRenderFamilyClassifier.classify(
                "com.megacrit.cardcrawl.vfx.Combat.StrikeEffect"));
        for (int i = 0; i < 5; i++) {
            assertEquals("vfx-combat", NativeRenderFamilyClassifier.classify(
                    "com.megacrit.cardcrawl.vfx.combat.StrikeEffect"));
        }
    }
}
