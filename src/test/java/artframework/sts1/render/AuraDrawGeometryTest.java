package artframework.sts1.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

/** Pure JUnit coverage for the host-neutral {@link AuraDrawGeometry} mapping. */
public class AuraDrawGeometryTest {

    private static final float EPS = 1e-6f;

    @Test
    public void kindForMapsTheThreeExactFqns() {
        assertSame(AuraDrawGeometry.Kind.STANCE_AURA,
                AuraDrawGeometry.kindFor(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertSame(AuraDrawGeometry.Kind.WRATH_PARTICLE,
                AuraDrawGeometry.kindFor(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertSame(AuraDrawGeometry.Kind.DIVINITY_PARTICLE,
                AuraDrawGeometry.kindFor(AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT));
        // the exact literal FQNs, not just the policy constants
        assertSame(AuraDrawGeometry.Kind.STANCE_AURA,
                AuraDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect"));
        assertSame(AuraDrawGeometry.Kind.WRATH_PARTICLE,
                AuraDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect"));
        assertSame(AuraDrawGeometry.Kind.DIVINITY_PARTICLE,
                AuraDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffect"));
    }

    @Test
    public void kindForFailsOpenForNullBlankAndNearMisses() {
        assertNull(AuraDrawGeometry.kindFor(null));
        assertNull(AuraDrawGeometry.kindFor(""));
        assertNull(AuraDrawGeometry.kindFor("   "));
        assertNull(AuraDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect"));
        assertNull(AuraDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect2"));
        assertNull(AuraDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect$Sub"));
        assertNull(AuraDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect$1"));
        assertNull(AuraDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffectFoo"));
    }

    @Test
    public void stanceAuraUsesCenterOriginPackedSizeAndPassthroughXY() {
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        AuraDrawGeometry.Params p = AuraDrawGeometry.params(
                AuraDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f /* vY ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                pw, ph);

        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(rotation, p.rotation, EPS);
    }

    @Test
    public void divinityParticleAddsVYAndUsesUniformScale() {
        float pw = 40f;
        float ph = 24f;
        float x = 5f;
        float y = 10f;
        float vY = -3.5f;
        float scale = 1.25f;
        float rotation = 90f;

        AuraDrawGeometry.Params p = AuraDrawGeometry.params(
                AuraDrawGeometry.Kind.DIVINITY_PARTICLE,
                x, y, vY, scale, rotation,
                1f, 2f, 3f, pw, ph);

        assertEquals(x, p.x, EPS);
        assertEquals(y + vY, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);
    }

    @Test
    public void wrathParticleAddsVYAndAppliesTheExactScalarFormula() {
        // Concrete numeric case.
        float pw = 32f;
        float ph = 16f;
        float x = 7f;
        float y = 20f;
        float vY = 1.5f;
        float scale = 0.5f;
        float rotation = 12f;
        float durDiv2 = 0.75f;
        float duration = 1.4f;
        float settingsScale = 2f;

        float expectedScaleY = (0.1f + ((durDiv2 * 2f - duration) * 2f * scale)) * settingsScale;

        AuraDrawGeometry.Params p = AuraDrawGeometry.params(
                AuraDrawGeometry.Kind.WRATH_PARTICLE,
                x, y, vY, scale, rotation, durDiv2, duration, settingsScale, pw, ph);

        assertEquals(x, p.x, EPS);
        assertEquals(y + vY, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale * 0.8f, p.scaleX, EPS);
        assertEquals(expectedScaleY, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);
    }

    @Test
    public void wrathParticleScaleYCollapsesToBaseWhenDurDiv2TimesTwoEqualsDuration() {
        float scale = 0.5f;
        float durDiv2 = 1.1f;
        float duration = 2.2f; // durDiv2 * 2f == duration
        float settingsScale = 1.5f;

        AuraDrawGeometry.Params p = AuraDrawGeometry.params(
                AuraDrawGeometry.Kind.WRATH_PARTICLE,
                0f, 0f, 0f, scale, 0f, durDiv2, duration, settingsScale, 10f, 10f);

        assertEquals(0.1f * settingsScale, p.scaleY, EPS);
        assertEquals(scale * 0.8f, p.scaleX, EPS);
    }

    @Test
    public void nullKindThrowsIllegalArgument() {
        try {
            AuraDrawGeometry.params(null, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 1f, 1f);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void negativeAndFractionalPackedSizesMapArithmeticallyWithoutThrowing() {
        AuraDrawGeometry.Params p = AuraDrawGeometry.params(
                AuraDrawGeometry.Kind.STANCE_AURA,
                -1.5f, -2.25f, 0f, -0.5f, -30f, 0f, 0f, 1f,
                -16.5f, 7.25f);

        assertEquals(-16.5f / 2f, p.originX, EPS);
        assertEquals(7.25f / 2f, p.originY, EPS);
        assertEquals(-16.5f, p.width, EPS);
        assertEquals(7.25f, p.height, EPS);
        assertEquals(-0.5f, p.scaleX, EPS);
        assertEquals(-0.5f, p.scaleY, EPS);
    }
}
