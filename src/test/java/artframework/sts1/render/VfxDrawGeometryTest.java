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
        assertSame(VfxDrawGeometry.Kind.LIGHT_FLARE_M,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SCENE_LIGHT_FLARE_M));
        assertSame(VfxDrawGeometry.Kind.LIGHT_FLARE_L,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SCENE_LIGHT_FLARE_L));
        assertSame(VfxDrawGeometry.Kind.TORCH_PARTICLE_L,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L));
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
        assertSame(VfxDrawGeometry.Kind.LIGHT_FLARE_M,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.LightFlareMEffect"));
        assertSame(VfxDrawGeometry.Kind.LIGHT_FLARE_L,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.LightFlareLEffect"));
        assertSame(VfxDrawGeometry.Kind.TORCH_PARTICLE_L,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.TorchParticleLEffect"));
        assertSame(VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FIRE_BURST));
        assertSame(VfxDrawGeometry.Kind.RED_FIRE_BURST,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.RED_FIRE_BURST));
        assertSame(VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SMOKE_BLUR));
        assertSame(VfxDrawGeometry.Kind.CEILING_DUST,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.CEILING_DUST));
        assertSame(VfxDrawGeometry.Kind.NEMESIS_FIRE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.NEMESIS_FIRE));
        // the exact literal FQNs of the five newest members
        assertSame(VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.FireBurstParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.RED_FIRE_BURST,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect"));
        assertSame(VfxDrawGeometry.Kind.CEILING_DUST,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect"));
        assertSame(VfxDrawGeometry.Kind.NEMESIS_FIRE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.NemesisFireParticle"));
        assertSame(VfxDrawGeometry.Kind.SHIELD_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SHIELD_PARTICLE));
        assertSame(VfxDrawGeometry.Kind.SHIELD_PARTICLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.ShieldParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.DEBUFF_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.DEBUFF_PARTICLE));
        assertSame(VfxDrawGeometry.Kind.DEBUFF_PARTICLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.DebuffParticleEffect"));
        // the four newest members, via constants and literal FQNs
        assertSame(VfxDrawGeometry.Kind.TORCH_PARTICLE_XL,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL));
        assertSame(VfxDrawGeometry.Kind.TORCH_PARTICLE_XL,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect"));
        assertSame(VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.GHOSTLY_WEAK_FIRE));
        assertSame(VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect"));
        assertSame(VfxDrawGeometry.Kind.GENERIC_SMOKE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.GENERIC_SMOKE));
        assertSame(VfxDrawGeometry.Kind.GENERIC_SMOKE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.GenericSmokeEffect"));
        assertSame(VfxDrawGeometry.Kind.EXHAUST_BLUR,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.EXHAUST_BLUR));
        assertSame(VfxDrawGeometry.Kind.EXHAUST_BLUR,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.ExhaustBlurEffect"));
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
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareMEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareMEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("LightFlareMEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareLEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareLEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("LightFlareLEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleLEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleLEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleEffect")); // near-miss (no L)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FireBurstParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FireBurstParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("FireBurstParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FireBurstParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("SmokeBlurEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.CeilingDustEffect")); // near-miss (no Cloud)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.NemesisFireParticle2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.NemesisFireParticle$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.NemesisFireParticle")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ShieldParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ShieldParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("ShieldParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.ShieldParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DebuffParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DebuffParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("DebuffParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DebuffParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("TorchParticleXLEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.TorchParticleXLEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleEffect")); // near-miss (no XL)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("GhostlyWeakFireEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.GhostlyWeakFireEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GhostlyWeakFire")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GenericSmokeEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GenericSmokeEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("GenericSmokeEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.GenericSmokeEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GenericSmoke")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ExhaustBlurEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ExhaustBlurEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("ExhaustBlurEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.ExhaustBlurEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ExhaustBlur")); // near-miss (no Effect)
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
    public void laterSceneWorldMembersMatchStanceAuraGeometryExactly() {
        // LightFlareMEffect/LightFlareLEffect/TorchParticleLEffect all draw the same formula as
        // StanceAuraEffect: x/y passthrough, center origin, packed size, uniform scale. The flare
        // pair has no vY field; TorchParticleLEffect has one but render never reads it.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.LIGHT_FLARE_M,
                VfxDrawGeometry.Kind.LIGHT_FLARE_L,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_L }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* ignored */, scale, rotation, 7f, 5f, 2f, pw, ph);

            assertEquals("x passthrough for " + kind, x, p.x, EPS);
            assertEquals("y passthrough for " + kind, y, p.y, EPS);
            assertEquals("originX for " + kind, pw / 2f, p.originX, EPS);
            assertEquals("originY for " + kind, ph / 2f, p.originY, EPS);
            assertEquals("width for " + kind, pw, p.width, EPS);
            assertEquals("height for " + kind, ph, p.height, EPS);
            assertEquals("scaleX for " + kind, scale, p.scaleX, EPS);
            assertEquals("scaleY for " + kind, scale, p.scaleY, EPS);
            assertEquals("rotation for " + kind, rotation, p.rotation, EPS);
            // Byte-identical to the STANCE_AURA result for the same inputs.
            assertEquals("geometry must equal STANCE_AURA for " + kind, aura, p);
            assertTrue("expected additive blend for " + kind,
                    VfxDrawGeometry.additiveBlend(kind));
        }
    }

    @Test
    public void twoFireBurstsAndThreeAmbientMembersMatchStanceAuraGeometryExactly() {
        // The five newest members all draw the same formula as StanceAuraEffect: x/y passthrough
        // (every one of them has a vY field that render never reads), center origin, packed size,
        // uniform scale. The two fire bursts are additive; the smoke blur, ceiling dust, and
        // nemesis fire never call setBlendFunction at all.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.Kind.RED_FIRE_BURST,
                VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.Kind.CEILING_DUST,
                VfxDrawGeometry.Kind.NEMESIS_FIRE }) {
            // vY=999f is ignored by every one of these formulas (vY is update-only).
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph);

            assertEquals("x passthrough for " + kind, x, p.x, EPS);
            assertEquals("y passthrough for " + kind, y, p.y, EPS);
            assertEquals("originX for " + kind, pw / 2f, p.originX, EPS);
            assertEquals("originY for " + kind, ph / 2f, p.originY, EPS);
            assertEquals("width for " + kind, pw, p.width, EPS);
            assertEquals("height for " + kind, ph, p.height, EPS);
            assertEquals("scaleX for " + kind, scale, p.scaleX, EPS);
            assertEquals("scaleY for " + kind, scale, p.scaleY, EPS);
            assertEquals("rotation for " + kind, rotation, p.rotation, EPS);
            // Byte-identical to the STANCE_AURA result (vY=0) for the same inputs.
            assertEquals("geometry must equal STANCE_AURA for " + kind, aura, p);

            boolean additive = kind == VfxDrawGeometry.Kind.FIRE_BURST
                    || kind == VfxDrawGeometry.Kind.RED_FIRE_BURST;
            assertEquals("blend policy for " + kind, additive,
                    VfxDrawGeometry.additiveBlend(kind));
        }
    }

    @Test
    public void fourNewestMembersMatchStanceAuraGeometryExactlyWithVYIgnored() {
        // The four newest members all draw the same formula as StanceAuraEffect: x/y passthrough
        // (every one of them has a vY field that render never reads), center origin, packed size,
        // uniform scale. TorchParticleXLEffect and GhostlyWeakFireEffect are additive; the generic
        // smoke and exhaust blur never call setBlendFunction at all.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.TORCH_PARTICLE_XL,
                VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE,
                VfxDrawGeometry.Kind.GENERIC_SMOKE,
                VfxDrawGeometry.Kind.EXHAUST_BLUR }) {
            // vY=999f is ignored by every one of these formulas (vY is update-only).
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph);

            assertEquals("x passthrough for " + kind, x, p.x, EPS);
            assertEquals("y passthrough for " + kind, y, p.y, EPS);
            assertEquals("originX for " + kind, pw / 2f, p.originX, EPS);
            assertEquals("originY for " + kind, ph / 2f, p.originY, EPS);
            assertEquals("width for " + kind, pw, p.width, EPS);
            assertEquals("height for " + kind, ph, p.height, EPS);
            assertEquals("scaleX for " + kind, scale, p.scaleX, EPS);
            assertEquals("scaleY for " + kind, scale, p.scaleY, EPS);
            assertEquals("rotation for " + kind, rotation, p.rotation, EPS);
            // Byte-identical to the STANCE_AURA result (vY=0) for the same inputs.
            assertEquals("geometry must equal STANCE_AURA for " + kind, aura, p);

            // A different vY produces byte-identical geometry (vY is ignored).
            VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                    kind, x, y, -12345f, scale, rotation, 7f, 5f, 2f, pw, ph);
            assertEquals("vY must be ignored for " + kind, p, q);

            boolean additive = kind == VfxDrawGeometry.Kind.TORCH_PARTICLE_XL
                    || kind == VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE;
            assertEquals("blend policy for " + kind, additive,
                    VfxDrawGeometry.additiveBlend(kind));
        }
    }

    @Test
    public void shieldParticleUsesTheFixedNativeRectAndForcesZeroRotation() {
        // Native: draw(ImageMaster.INTENT_DEFEND, x - 32f, y - 32f, 32f, 32f, 64f, 64f,
        //            scale, scale, 0f, 0, 0, 64, 64, false, false)
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.SHIELD_PARTICLE,
                x, y, 999f /* vY ignored */, scale, 45f /* rotation forced to 0 */,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */);

        assertEquals(x - 32f, p.x, EPS);
        assertEquals(y - 32f, p.y, EPS);
        assertEquals(32f, p.originX, EPS);
        assertEquals(32f, p.originY, EPS);
        assertEquals(64f, p.width, EPS);
        assertEquals(64f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("rotation is hardcoded to 0f", 0f, p.rotation, EPS);

        // A totally different rotation input produces byte-identical geometry (rotation ignored).
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.SHIELD_PARTICLE,
                x, y, 0f, scale, -123f, 0f, 0f, 1f, 0f, 0f);
        assertEquals(p, q);

        // The host-neutral constants match the native hardcoded rect.
        assertEquals(32f, VfxDrawGeometry.SHIELD_ORIGIN_X, EPS);
        assertEquals(32f, VfxDrawGeometry.SHIELD_ORIGIN_Y, EPS);
        assertEquals(64f, VfxDrawGeometry.SHIELD_WIDTH, EPS);
        assertEquals(64f, VfxDrawGeometry.SHIELD_HEIGHT, EPS);
        assertEquals(0, VfxDrawGeometry.SHIELD_SRC_X);
        assertEquals(0, VfxDrawGeometry.SHIELD_SRC_Y);
        assertEquals(64, VfxDrawGeometry.SHIELD_SRC_W);
        assertEquals(64, VfxDrawGeometry.SHIELD_SRC_H);
    }

    @Test
    public void debuffParticleUsesTheFixedNativeRectAndTheFieldRotation() {
        // Native: draw(img, x - 16f, y - 16f, 16f, 16f, 32f, 32f,
        //            scale, scale, rotation, 0, 0, 32, 32, false, false)
        float x = -7.5f;
        float y = 21.25f;
        float scale = 1.25f;
        float rotation = 137f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE,
                x, y, 999f /* vY ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */);

        assertEquals(x - 16f, p.x, EPS);
        assertEquals(y - 16f, p.y, EPS);
        assertEquals(16f, p.originX, EPS);
        assertEquals(16f, p.originY, EPS);
        assertEquals(32f, p.width, EPS);
        assertEquals(32f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("the rotation field is consumed", rotation, p.rotation, EPS);

        // Different packed sizes produce byte-identical geometry (the region is ignored).
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE,
                x, y, 0f, scale, rotation, 0f, 0f, 1f, 0f, 0f);
        assertEquals(p, q);

        // The host-neutral constants match the native hardcoded rect.
        assertEquals(16f, VfxDrawGeometry.DEBUFF_ORIGIN_X, EPS);
        assertEquals(16f, VfxDrawGeometry.DEBUFF_ORIGIN_Y, EPS);
        assertEquals(32f, VfxDrawGeometry.DEBUFF_WIDTH, EPS);
        assertEquals(32f, VfxDrawGeometry.DEBUFF_HEIGHT, EPS);
        assertEquals(0, VfxDrawGeometry.DEBUFF_SRC_X);
        assertEquals(0, VfxDrawGeometry.DEBUFF_SRC_Y);
        assertEquals(32, VfxDrawGeometry.DEBUFF_SRC_W);
        assertEquals(32, VfxDrawGeometry.DEBUFF_SRC_H);
    }

    @Test
    public void additiveBlendIsTrueForEveryKindExceptTheAmbientOnes() {
        // FlashAtkImgEffect, SmokeBlurEffect, CeilingDustCloudEffect, NemesisFireParticle, and
        // DebuffParticleEffect never call setBlendFunction natively, so their host draw must not
        // install additive blend.
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLASH_ATK_IMG));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.SMOKE_BLUR));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.CEILING_DUST));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.NEMESIS_FIRE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DEBUFF_PARTICLE));

        // The two fire bursts are additive despite being fire-family; ShieldParticleEffect is the
        // additive bare-texture member.
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FIRE_BURST));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.RED_FIRE_BURST));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.SHIELD_PARTICLE));

        // The two newest ambient packed-region members never call setBlendFunction ...
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.GENERIC_SMOKE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.EXHAUST_BLUR));
        // ... while the two newest additive members do.
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.TORCH_PARTICLE_XL));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE));

        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.FLASH_ATK_IMG
                    || kind == VfxDrawGeometry.Kind.SMOKE_BLUR
                    || kind == VfxDrawGeometry.Kind.CEILING_DUST
                    || kind == VfxDrawGeometry.Kind.NEMESIS_FIRE
                    || kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                    || kind == VfxDrawGeometry.Kind.GENERIC_SMOKE
                    || kind == VfxDrawGeometry.Kind.EXHAUST_BLUR) {
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
