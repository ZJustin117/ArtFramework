package artframework.sts1.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/** Pure JUnit coverage for the host-neutral {@link VfxDrawGeometry} mapping. */
public class VfxDrawGeometryTest {

    private static final float EPS = 1e-6f;

    @Test
    public void kindForMapsTheExactFqns() {
        assertSame(VfxDrawGeometry.Kind.STANCE_AURA,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertSame(VfxDrawGeometry.Kind.WRATH_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertSame(VfxDrawGeometry.Kind.DIVINITY_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT));
        assertSame(VfxDrawGeometry.Kind.CALM_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.CALM_PARTICLE_EFFECT));
        assertSame(VfxDrawGeometry.Kind.DIVINITY_STANCE_CHANGE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE));
        assertSame(VfxDrawGeometry.Kind.LIGHT_FLARE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SCENE_LIGHT_FLARE));
        assertSame(VfxDrawGeometry.Kind.FLASH_ATK_IMG,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FLASH_ATK_IMG));
        // the exact literal FQNs, not just the policy constants
        assertSame(VfxDrawGeometry.Kind.STANCE_AURA,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect"));
        assertSame(VfxDrawGeometry.Kind.WRATH_PARTICLE,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.DIVINITY_PARTICLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.CALM_PARTICLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.DIVINITY_STANCE_CHANGE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle"));
        assertSame(VfxDrawGeometry.Kind.LIGHT_FLARE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect"));
        assertSame(VfxDrawGeometry.Kind.FLASH_ATK_IMG,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect"));
    }

    @Test
    public void kindForFailsOpenForNullBlankAndNearMisses() {
        assertNull(VfxDrawGeometry.kindFor(null));
        assertNull(VfxDrawGeometry.kindFor(""));
        assertNull(VfxDrawGeometry.kindFor("   "));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect$Sub"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect$1"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffectFoo"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect$Sub"));
        assertNull(VfxDrawGeometry.kindFor("DivinityStanceChangeParticle")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle$Sub"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect$Sub"));
        assertNull(VfxDrawGeometry.kindFor("LightFlareSEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareEffect")); // near-miss (no S)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlashAtkEffect")); // near-miss (no Img)
        assertNull(VfxDrawGeometry.kindFor("FlashAtkImgEffect")); // simple name only
    }

    @Test
    public void stanceAuraUsesCenterOriginPackedSizeAndPassthroughXY() {
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
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

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.DIVINITY_PARTICLE,
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

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WRATH_PARTICLE,
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

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WRATH_PARTICLE,
                0f, 0f, 0f, scale, 0f, durDiv2, duration, settingsScale, 10f, 10f);

        assertEquals(0.1f * settingsScale, p.scaleY, EPS);
        assertEquals(scale * 0.8f, p.scaleX, EPS);
    }

    @Test
    public void calmParticleUsesFixedNativeOriginSizeAndScaleYFormula() {
        // Native: draw(tex, x, y, 32f, 32f, 25f, 128f, scale,
        //            scale + (dur_div2*0.4f - duration) * Settings.scale, rotation, 0,0,64,64,...)
        float x = 11.5f;
        float y = -7.25f;
        float scale = 0.75f;
        float rotation = 30f;
        float durDiv2 = 0.8f;
        float duration = 1.1f;
        float settingsScale = 2f;

        float expectedScaleY = scale + (durDiv2 * 0.4f - duration) * settingsScale;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                x, y, 999f /* vY ignored */, scale, rotation, durDiv2, duration, settingsScale,
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(32f, p.originX, EPS);
        assertEquals(32f, p.originY, EPS);
        assertEquals(25f, p.width, EPS);
        assertEquals(128f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(expectedScaleY, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);
        assertEquals(0, VfxDrawGeometry.CALM_SRC_X);
        assertEquals(0, VfxDrawGeometry.CALM_SRC_Y);
        assertEquals(64, VfxDrawGeometry.CALM_SRC_W);
        assertEquals(64, VfxDrawGeometry.CALM_SRC_H);
        assertEquals(32f, VfxDrawGeometry.CALM_ORIGIN_X, EPS);
        assertEquals(32f, VfxDrawGeometry.CALM_ORIGIN_Y, EPS);
        assertEquals(25f, VfxDrawGeometry.CALM_WIDTH, EPS);
        assertEquals(128f, VfxDrawGeometry.CALM_HEIGHT, EPS);
    }

    @Test
    public void calmParticleIgnoresPackedSizeAndVYAndCollapsesScaleYWhenTermsCancel() {
        // dur_div2*0.4f == duration -> scaleY == scale.
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                1f, 2f, 12345f, 0.5f, 9f,
                1f, 0.4f, 3f,
                -100f, 777f);

        assertEquals(1f, p.x, EPS);
        assertEquals(2f, p.y, EPS); // vY not added
        assertEquals(25f, p.width, EPS);
        assertEquals(128f, p.height, EPS);
        assertEquals(0.5f, p.scaleX, EPS);
        assertEquals(0.5f, p.scaleY, EPS);

        // A different packed size produces byte-identical geometry for Calm.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                1f, 2f, 0f, 0.5f, 9f, 1f, 0.4f, 3f, 0f, 0f);
        assertEquals(p, q);
    }

    @Test
    public void divinityStanceChangeMatchesStanceAuraGeometryExactly() {
        // Native DivinityStanceChangeParticle draws the same formula as StanceAuraEffect: x/y
        // passthrough (the class has no vY field), center origin, packed size, uniform scale.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.DIVINITY_STANCE_CHANGE,
                x, y, 999f /* no vY field; ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                pw, ph);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        // Byte-identical to the STANCE_AURA result for the same inputs.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph);
        assertEquals(p, q);
    }

    @Test
    public void lightFlareMatchesStanceAuraGeometryExactly() {
        // The cross-family vfx-scene-world member draws the same formula as StanceAuraEffect: x/y
        // passthrough (the class has no vY field), center origin, packed size, uniform scale.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.LIGHT_FLARE,
                x, y, 999f /* no vY field; ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                pw, ph);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        // Byte-identical to the STANCE_AURA result for the same inputs.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph);
        assertEquals(p, q);
    }

    @Test
    public void nullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.params(null, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 1f, 1f);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void flashAtkImgMatchesStanceAuraGeometryExactly() {
        // The vfx-combat member draws the same formula as StanceAuraEffect: x/y passthrough (the
        // class has no vY field), center origin, packed size, uniform scale.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLASH_ATK_IMG,
                x, y, 999f /* no vY field; ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                pw, ph);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        // Byte-identical to the STANCE_AURA result for the same inputs.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph);
        assertEquals(p, q);
    }

    @Test
    public void additiveBlendIsTrueForEveryKindExceptFlashAtkImg() {
        // FlashAtkImgEffect never calls setBlendFunction natively, so its host draw must not
        // install additive blend.
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLASH_ATK_IMG));

        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.FLASH_ATK_IMG) {
                continue;
            }
            assertTrue("expected additive blend for " + kind,
                    VfxDrawGeometry.additiveBlend(kind));
        }
    }

    @Test
    public void additiveBlendNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.additiveBlend(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void negativeAndFractionalPackedSizesMapArithmeticallyWithoutThrowing() {
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
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
