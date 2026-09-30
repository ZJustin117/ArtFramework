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
        // the two newest members, via constants and literal FQNs
        assertSame(VfxDrawGeometry.Kind.ICE_SHATTER,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.ICE_SHATTER));
        assertSame(VfxDrawGeometry.Kind.ICE_SHATTER,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.IceShatterEffect"));
        assertSame(VfxDrawGeometry.Kind.WEB_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.WEB_PARTICLE));
        assertSame(VfxDrawGeometry.Kind.WEB_PARTICLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.WebParticleEffect"));
        // the four newest members, via constants and literal FQNs
        assertSame(VfxDrawGeometry.Kind.ENTANGLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.ENTANGLE_EFFECT));
        assertSame(VfxDrawGeometry.Kind.ENTANGLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.EntangleEffect"));
        assertSame(VfxDrawGeometry.Kind.BLOCK_IMPACT_LINE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.BLOCK_IMPACT_LINE));
        assertSame(VfxDrawGeometry.Kind.BLOCK_IMPACT_LINE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect"));
        assertSame(VfxDrawGeometry.Kind.EXHAUST_PILE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.EXHAUST_PILE_PARTICLE));
        assertSame(VfxDrawGeometry.Kind.EXHAUST_PILE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.ExhaustPileParticle"));
        assertSame(VfxDrawGeometry.Kind.UNKNOWN_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.UNKNOWN_PARTICLE));
        assertSame(VfxDrawGeometry.Kind.UNKNOWN_PARTICLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.UnknownParticleEffect"));
        // the five newest members, via constants and literal FQNs
        assertSame(VfxDrawGeometry.Kind.FLAME_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FLAME_PARTICLE));
        assertSame(VfxDrawGeometry.Kind.FLAME_PARTICLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE));
        assertSame(VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffect"));
        assertSame(VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.DAMAGE_IMPACT_BLUR));
        assertSame(VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.DamageImpactBlurEffect"));
        assertSame(VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.DAMAGE_IMPACT_LINE));
        assertSame(VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.DamageImpactLineEffect"));
        assertSame(VfxDrawGeometry.Kind.DARK_ORB_PASSIVE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.DARK_ORB_PASSIVE));
        assertSame(VfxDrawGeometry.Kind.DARK_ORB_PASSIVE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect"));
        // the three newest members, via constants and literal FQNs
        assertSame(VfxDrawGeometry.Kind.WARNING_SIGN,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.WARNING_SIGN));
        assertSame(VfxDrawGeometry.Kind.WARNING_SIGN,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.WarningSignEffect"));
        assertSame(VfxDrawGeometry.Kind.STUN_STAR,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.STUN_STAR));
        assertSame(VfxDrawGeometry.Kind.STUN_STAR,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.StunStarEffect"));
        assertSame(VfxDrawGeometry.Kind.FALLING_DUST,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FALLING_DUST));
        assertSame(VfxDrawGeometry.Kind.LIGHTNING_EFFECT,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.LIGHTNING_EFFECT));
        assertSame(VfxDrawGeometry.Kind.FLAME_BALL,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FLAME_BALL));
        assertSame(VfxDrawGeometry.Kind.SHINE_LINES,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SHINE_LINES));
        assertSame(VfxDrawGeometry.Kind.LIGHTNING_EFFECT,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.combat.LightningEffect"));
        assertSame(VfxDrawGeometry.Kind.FLAME_BALL,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.FlameBallParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.SHINE_LINES,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.ShineLinesEffect"));
        assertSame(VfxDrawGeometry.Kind.TORCH_PARTICLE_M,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.TORCH_PARTICLE_M));
        assertSame(VfxDrawGeometry.Kind.TORCH_PARTICLE_M,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect"));
        assertSame(VfxDrawGeometry.Kind.TORCH_PARTICLE_S,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.TORCH_PARTICLE_S));
        assertSame(VfxDrawGeometry.Kind.TORCH_PARTICLE_S,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect"));
        assertSame(VfxDrawGeometry.Kind.SCENE_DUST,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SCENE_DUST));
        assertSame(VfxDrawGeometry.Kind.SCENE_DUST,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.scene.DustEffect"));
        assertSame(VfxDrawGeometry.Kind.FALLING_DUST,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.FallingDustEffect"));
        assertSame(VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE));
        assertSame(VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect"));
        assertSame(VfxDrawGeometry.Kind.GLOWY_FIRE_EYES,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.GLOWY_FIRE_EYES));
        assertSame(VfxDrawGeometry.Kind.GLOWY_FIRE_EYES,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect"));
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
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.IceShatterEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.IceShatterEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("IceShatterEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.IceShatterEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.IceShatter")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.WebParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.WebParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("WebParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.WebParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.WebParticle")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.EntangleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.EntangleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("EntangleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.EntangleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.Entangle")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("BlockImpactLineEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.BlockImpactLineEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.BlockImpactEffect")); // near-miss (no Line)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ExhaustPileParticle2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ExhaustPileParticle$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("ExhaustPileParticle")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.ExhaustPileParticle")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ExhaustPileEffect")); // near-miss (Particle)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.UnknownParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.UnknownParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("UnknownParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.UnknownParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.UnknownParticle")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("FlameParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FlameParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlameParticle")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("LightningOrbActivateEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.LightningOrbActivateEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningOrbActivate")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DamageImpactBlurEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DamageImpactBlurEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("DamageImpactBlurEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DamageImpactBlurEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DamageImpactEffect")); // near-miss (no Blur)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DamageImpactLineEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DamageImpactLineEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("DamageImpactLineEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DamageImpactLineEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DamageImpactEffect")); // near-miss (no Line)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("DarkOrbPassiveEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DarkOrbPassiveEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DarkOrbPassive")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.WarningSignEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.WarningSignEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("WarningSignEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.WarningSignEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.WarningSign")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.StunStarEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.StunStarEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("StunStarEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.StunStarEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.StunStar")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FallingDustEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FallingDustEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("FallingDustEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.FallingDustEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FallingDust")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("LightningEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.LightningEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningEffect$1")); // nested anonymous
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FlameBallParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FlameBallParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("FlameBallParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlameBallParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FlameBallParticle")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ShineLinesEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ShineLinesEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("ShineLinesEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.ShineLinesEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ShineLines")); // near-miss (no Effect)
        // The three newest scene-world members: exact FQN only, near-misses fail open.
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("TorchParticleMEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.TorchParticleMEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("TorchParticleSEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.TorchParticleSEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.DustEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.DustEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("DustEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DustEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.Dust")); // near-miss (no Effect)
        // The two newest flip members: exact FQN only, near-misses fail open.
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("LightningOrbPassiveEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.LightningOrbPassiveEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningOrbPassive")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffectPassive"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("GlowyFireEyesEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.GlowyFireEyesEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.GlowyFireEyes")); // near-miss (no Effect)
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
                pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                1f, 2f, 3f, pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                x, y, vY, scale, rotation, durDiv2, duration, settingsScale, pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                0f, 0f, 0f, scale, 0f, durDiv2, duration, settingsScale, 10f, 10f, 0f, 0f, 0f, 0f, 0f);

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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f);

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
                -100f, 777f, 0f, 0f, 0f, 0f, 0f);

        assertEquals(1f, p.x, EPS);
        assertEquals(2f, p.y, EPS); // vY not added
        assertEquals(25f, p.width, EPS);
        assertEquals(128f, p.height, EPS);
        assertEquals(0.5f, p.scaleX, EPS);
        assertEquals(0.5f, p.scaleY, EPS);

        // A different packed size produces byte-identical geometry for Calm.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                1f, 2f, 0f, 0.5f, 9f, 1f, 0.4f, 3f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
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
                pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);
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
                pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);
        assertEquals(p, q);
    }

    @Test
    public void nullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.params(null, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 0f);
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
                pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);
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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.LIGHT_FLARE_M,
                VfxDrawGeometry.Kind.LIGHT_FLARE_L,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_L }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* ignored */, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.Kind.RED_FIRE_BURST,
                VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.Kind.CEILING_DUST,
                VfxDrawGeometry.Kind.NEMESIS_FIRE }) {
            // vY=999f is ignored by every one of these formulas (vY is update-only).
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.TORCH_PARTICLE_XL,
                VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE,
                VfxDrawGeometry.Kind.GENERIC_SMOKE,
                VfxDrawGeometry.Kind.EXHAUST_BLUR }) {
            // vY=999f is ignored by every one of these formulas (vY is update-only).
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

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
                    kind, x, y, -12345f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);
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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f);

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
                x, y, 0f, scale, -123f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f);

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
                x, y, 0f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
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
    public void iceShatterUsesTheFixedNativeRectAndTheFieldRotation() {
        // Native: draw(img, x, y, 32f, 32f, 64f, 64f,
        //            scale, scale, rotation, 0, 0, 64, 64, false, false)
        float x = -7.5f;
        float y = 21.25f;
        float scale = 1.25f;
        float rotation = 137f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.ICE_SHATTER,
                x, y, 999f /* vY ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(32f, p.originX, EPS);
        assertEquals(32f, p.originY, EPS);
        assertEquals(64f, p.width, EPS);
        assertEquals(64f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("the rotation field is consumed", rotation, p.rotation, EPS);

        // A different vY and packed size produce byte-identical geometry.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.ICE_SHATTER,
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        assertEquals(p, q);

        // The host-neutral constants match the native hardcoded rect.
        assertEquals(32f, VfxDrawGeometry.ICE_SHATTER_ORIGIN_X, EPS);
        assertEquals(32f, VfxDrawGeometry.ICE_SHATTER_ORIGIN_Y, EPS);
        assertEquals(64f, VfxDrawGeometry.ICE_SHATTER_WIDTH, EPS);
        assertEquals(64f, VfxDrawGeometry.ICE_SHATTER_HEIGHT, EPS);
        assertEquals(0, VfxDrawGeometry.ICE_SHATTER_SRC_X);
        assertEquals(0, VfxDrawGeometry.ICE_SHATTER_SRC_Y);
        assertEquals(64, VfxDrawGeometry.ICE_SHATTER_SRC_W);
        assertEquals(64, VfxDrawGeometry.ICE_SHATTER_SRC_H);
    }

    @Test
    public void webParticleUsesTheFixedNativeRectAndForcesZeroRotation() {
        // Native: draw(ImageMaster.WEB_VFX, x, y, 32f, 32f, 64f, 64f,
        //            scale, scale, 0f, 0, 0, 64, 64, false, false)
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WEB_PARTICLE,
                x, y, 999f /* vY ignored */, scale, 45f /* rotation forced to 0 */,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(32f, p.originX, EPS);
        assertEquals(32f, p.originY, EPS);
        assertEquals(64f, p.width, EPS);
        assertEquals(64f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("rotation is hardcoded to 0f", 0f, p.rotation, EPS);

        // A totally different rotation input produces byte-identical geometry (rotation ignored).
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WEB_PARTICLE,
                x, y, -12345f, scale, -123f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        assertEquals(p, q);

        // The host-neutral constants match the native hardcoded rect.
        assertEquals(32f, VfxDrawGeometry.WEB_ORIGIN_X, EPS);
        assertEquals(32f, VfxDrawGeometry.WEB_ORIGIN_Y, EPS);
        assertEquals(64f, VfxDrawGeometry.WEB_WIDTH, EPS);
        assertEquals(64f, VfxDrawGeometry.WEB_HEIGHT, EPS);
        assertEquals(0, VfxDrawGeometry.WEB_SRC_X);
        assertEquals(0, VfxDrawGeometry.WEB_SRC_Y);
        assertEquals(64, VfxDrawGeometry.WEB_SRC_W);
        assertEquals(64, VfxDrawGeometry.WEB_SRC_H);
    }

    @Test
    public void entangleMatchesWebParticleGeometryAndWhiteAlphaRuleExactly() {
        // Native EntangleEffect is byte-identical to WebParticleEffect: the static
        // ImageMaster.WEB_VFX Texture, x/y passthrough, origin (32, 32), size (64, 64), src
        // (0, 0, 64, 64), and a hardcoded zero rotation (EntangleEffect has no rotation field).
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.ENTANGLE,
                x, y, 999f /* vY ignored */, scale, 45f /* rotation ignored */,
                7f, 5f, 2f, 48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(32f, p.originX, EPS);
        assertEquals(32f, p.originY, EPS);
        assertEquals(64f, p.width, EPS);
        assertEquals(64f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("rotation is hardcoded to 0f", 0f, p.rotation, EPS);

        // Byte-identical to the WEB_PARTICLE result for the same inputs.
        VfxDrawGeometry.Params web = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WEB_PARTICLE,
                x, y, 999f, scale, 45f, 7f, 5f, 2f, 48f, 96f, 0f, 0f, 0f, 0f, 0f);
        assertEquals("ENTANGLE must reuse the WEB_PARTICLE configuration", web, p);

        assertTrue("ENTANGLE is additive", VfxDrawGeometry.additiveBlend(
                VfxDrawGeometry.Kind.ENTANGLE));
        assertTrue("ENTANGLE shares the WEB white-alpha rule",
                VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.ENTANGLE));
    }

    @Test
    public void blockImpactLineAndExhaustPileMatchStanceAuraGeometryExactlyAndDrawAmbient() {
        // BlockImpactLineEffect and ExhaustPileParticle both mirror StanceAuraEffect geometry
        // exactly (x/y passthrough, center packed/2 origin, packed w/h, uniform scale, rotation) and
        // never call setBlendFunction (ambient blend).
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.BLOCK_IMPACT_LINE,
                VfxDrawGeometry.Kind.EXHAUST_PILE }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* ignored */, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

            assertEquals("x passthrough for " + kind, x, p.x, EPS);
            assertEquals("y passthrough for " + kind, y, p.y, EPS);
            assertEquals("originX for " + kind, pw / 2f, p.originX, EPS);
            assertEquals("originY for " + kind, ph / 2f, p.originY, EPS);
            assertEquals("width for " + kind, pw, p.width, EPS);
            assertEquals("height for " + kind, ph, p.height, EPS);
            assertEquals("scaleX for " + kind, scale, p.scaleX, EPS);
            assertEquals("scaleY for " + kind, scale, p.scaleY, EPS);
            assertEquals("rotation for " + kind, rotation, p.rotation, EPS);
            assertEquals("geometry must equal STANCE_AURA for " + kind, aura, p);
            assertFalse("expected ambient blend for " + kind,
                    VfxDrawGeometry.additiveBlend(kind));
        }
    }

    @Test
    public void unknownParticleUsesTheNew128RectAndTheFieldRotationAmbiently() {
        // Native: draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f,
        //            scale, scale, rotation, 0, 0, 128, 128, false, false)
        float x = -7.5f;
        float y = 21.25f;
        float scale = 1.25f;
        float rotation = 137f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE,
                x, y, 999f /* vY ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f);

        assertEquals(x - 64f, p.x, EPS);
        assertEquals(y - 64f, p.y, EPS);
        assertEquals(64f, p.originX, EPS);
        assertEquals(64f, p.originY, EPS);
        assertEquals(128f, p.width, EPS);
        assertEquals(128f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("the rotation field is consumed", rotation, p.rotation, EPS);

        // A different vY and packed size produce byte-identical geometry.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE,
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        assertEquals(p, q);

        // The host-neutral constants match the native hardcoded rect.
        assertEquals(64f, VfxDrawGeometry.UNKNOWN_OFFSET, EPS);
        assertEquals(64f, VfxDrawGeometry.UNKNOWN_ORIGIN, EPS);
        assertEquals(128f, VfxDrawGeometry.UNKNOWN_SIZE, EPS);
        assertEquals(0, VfxDrawGeometry.UNKNOWN_SRC_X);
        assertEquals(0, VfxDrawGeometry.UNKNOWN_SRC_Y);
        assertEquals(128, VfxDrawGeometry.UNKNOWN_SRC_W);
        assertEquals(128, VfxDrawGeometry.UNKNOWN_SRC_H);

        assertFalse("UNKNOWN_PARTICLE never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.UNKNOWN_PARTICLE));
    }

    @Test
    public void fiveNewestMembersReuseTheExistingShapesWithOneNewFixedRect() {
        // The five newest vfx-combat members: FlameParticleEffect and LightningOrbActivateEffect
        // reuse the additive center-packed geometry; DamageImpactBlurEffect and
        // DamageImpactLineEffect reuse it ambiently; DarkOrbPassiveEffect is a NEW additive fixed
        // rect over its own instance Texture that consumes its rotation field.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.FLAME_PARTICLE,
                VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* ignored */, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);

            assertEquals("x passthrough for " + kind, x, p.x, EPS);
            assertEquals("y passthrough for " + kind, y, p.y, EPS);
            assertEquals("originX for " + kind, pw / 2f, p.originX, EPS);
            assertEquals("originY for " + kind, ph / 2f, p.originY, EPS);
            assertEquals("width for " + kind, pw, p.width, EPS);
            assertEquals("height for " + kind, ph, p.height, EPS);
            assertEquals("scaleX for " + kind, scale, p.scaleX, EPS);
            assertEquals("scaleY for " + kind, scale, p.scaleY, EPS);
            assertEquals("rotation for " + kind, rotation, p.rotation, EPS);
            assertEquals("geometry must equal STANCE_AURA for " + kind, aura, p);
        }

        assertTrue("FLAME_PARTICLE is additive",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLAME_PARTICLE));
        assertTrue("LIGHTNING_ORB_ACTIVATE is additive",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE));
        assertFalse("DAMAGE_IMPACT_BLUR never calls setBlendFunction",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR));
        assertFalse("DAMAGE_IMPACT_LINE never calls setBlendFunction",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE));

        // DarkOrbPassiveEffect: a new additive 74x74 fixed rect using the rotation field.
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.DARK_ORB_PASSIVE,
                x, y, 999f /* vY ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f);

        assertEquals(x - 37f, p.x, EPS);
        assertEquals(y - 37f, p.y, EPS);
        assertEquals(37f, p.originX, EPS);
        assertEquals(37f, p.originY, EPS);
        assertEquals(74f, p.width, EPS);
        assertEquals(74f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("the rotation field is consumed", rotation, p.rotation, EPS);

        // A different vY and packed size produce byte-identical geometry.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.DARK_ORB_PASSIVE,
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        assertEquals(p, q);

        assertEquals(37f, VfxDrawGeometry.DARK_ORB_OFFSET, EPS);
        assertEquals(37f, VfxDrawGeometry.DARK_ORB_ORIGIN, EPS);
        assertEquals(74f, VfxDrawGeometry.DARK_ORB_SIZE, EPS);
        assertEquals(0, VfxDrawGeometry.DARK_ORB_SRC_X);
        assertEquals(0, VfxDrawGeometry.DARK_ORB_SRC_Y);
        assertEquals(74, VfxDrawGeometry.DARK_ORB_SRC_W);
        assertEquals(74, VfxDrawGeometry.DARK_ORB_SRC_H);

        assertTrue("DARK_ORB_PASSIVE is additive",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DARK_ORB_PASSIVE));
    }

    @Test
    public void warningSignUsesTheFixedStaticRectAndTheHardcodedDoubleSettingsScale() {
        // Native: setBlendFunction(770,1); sb.draw(ImageMaster.WARNING_ICON_VFX, x - 32f, y - 32f,
        //            32f, 32f, 64f, 64f, Settings.scale*2f, Settings.scale*2f, 0f, 0, 0, 64, 64,
        //            false, false). WarningSignEffect has NO scale and NO rotation field, so both
        //            effect-side inputs are ignored in favour of the hardcoded settingsScale * 2f
        //            uniform scale and the hardcoded zero rotation.
        float x = -7.5f;
        float y = 21.25f;
        float settingsScale = 1.5f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WARNING_SIGN,
                x, y, 999f /* vY ignored */, 0.25f /* no scale field; ignored */,
                137f /* no rotation field; ignored */,
                7f, 5f, settingsScale,
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */,
                1234f /* no vX field; ignored */, 17f, 19f /* no region origin; ignored */, 0f, 0f);

        assertEquals(x - 32f, p.x, EPS);
        assertEquals(y - 32f, p.y, EPS);
        assertEquals(32f, p.originX, EPS);
        assertEquals(32f, p.originY, EPS);
        assertEquals(64f, p.width, EPS);
        assertEquals(64f, p.height, EPS);
        assertEquals("the uniform scale is settingsScale * 2f",
                settingsScale * 2f, p.scaleX, EPS);
        assertEquals(settingsScale * 2f, p.scaleY, EPS);
        assertEquals("rotation is hardcoded to 0f", 0f, p.rotation, EPS);

        // Different scale/rotation/region inputs produce byte-identical geometry.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WARNING_SIGN,
                x, y, 0f, -123f, -45f, 0f, 0f, settingsScale, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        assertEquals(p, q);

        assertEquals(32f, VfxDrawGeometry.WARNING_ORIGIN_X, EPS);
        assertEquals(32f, VfxDrawGeometry.WARNING_ORIGIN_Y, EPS);
        assertEquals(64f, VfxDrawGeometry.WARNING_WIDTH, EPS);
        assertEquals(64f, VfxDrawGeometry.WARNING_HEIGHT, EPS);
        assertEquals(2f, VfxDrawGeometry.WARNING_SCALE_FACTOR, EPS);
        assertEquals(0, VfxDrawGeometry.WARNING_SRC_X);
        assertEquals(0, VfxDrawGeometry.WARNING_SRC_Y);
        assertEquals(64, VfxDrawGeometry.WARNING_SRC_W);
        assertEquals(64, VfxDrawGeometry.WARNING_SRC_H);

        assertTrue("WARNING_SIGN installs additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.WARNING_SIGN));
        assertFalse("WARNING_SIGN passes its color through unchanged",
                VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.WARNING_SIGN));
    }

    @Test
    public void stunStarShiftsThePositionByItsScaledVXVYAndStaysAmbient() {
        // Native: sb.draw(img, x - vX*30f*Settings.scale, y - vY*5f*Settings.scale,
        //            pw/2f, ph/2f, pw, ph, scale, scale, rotation) with no setBlendFunction.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float vX = 2.5f;
        float vY = -1.5f;
        float scale = 0.6f;
        float rotation = 45f;
        float settingsScale = 1.25f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STUN_STAR,
                x, y, vY, scale, rotation, 7f, 5f, settingsScale, pw, ph,
                vX, 0f, 0f, 0f, 0f);

        assertEquals(x - vX * 30f * settingsScale, p.x, EPS);
        assertEquals(y - vY * 5f * settingsScale, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        assertEquals(30f, VfxDrawGeometry.STUN_STAR_VX_FACTOR, EPS);
        assertEquals(5f, VfxDrawGeometry.STUN_STAR_VY_FACTOR, EPS);

        assertFalse("STUN_STAR never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.STUN_STAR));
    }

    @Test
    public void fallingDustUsesTheRegionOffsetsAsItsOriginAndStaysAmbient() {
        // Native: sb.draw(img, x, y, img.offsetX, img.offsetY, pw, ph, scale, scale, rotation) with
        //         no setBlendFunction — the ORIGIN is the region's own trim offset, not packed/2.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;
        float regionOffsetX = 6f;
        float regionOffsetY = 10f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FALLING_DUST,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, pw, ph,
                1234f /* vX ignored */, regionOffsetX, regionOffsetY, 0f, 0f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals("the origin is the region offsetX", regionOffsetX, p.originX, EPS);
        assertEquals("the origin is the region offsetY", regionOffsetY, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        assertFalse("FALLING_DUST never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FALLING_DUST));
    }

    @Test
    public void lightningEffectUsesOriginYZeroAndStaysAdditive() {
        // Native: setBlendFunction(770,1); sb.draw(img, x, y, pw/2f, 0f, pw, ph, scale, scale,
        //            rotation); setBlendFunction(770,771). LightningEffect has no vY; its origin Y is
        //            0f (NOT packedHeight/2f) and its offset is scale-independent.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.LIGHTNING_EFFECT,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, pw, ph,
                1234f /* vX ignored */, 0f, 0f,
                0f, -ph / 2f /* originY offset that collapses packedHeight/2f to 0f */);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals("originX stays packedWidth/2f", pw / 2f, p.originX, EPS);
        assertEquals("originY is 0f", 0f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        // The origin-Y-0 rule is scale-independent: at a non-1.0 settings scale (e.g. a 2560-wide
        // viewport) originY is still exactly 0f (its offset is -packedHeight/2f with no scale term).
        VfxDrawGeometry.Params scaled = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.LIGHTNING_EFFECT,
                x, y, 999f, scale, rotation, 7f, 5f, 1.333f, pw, ph,
                1234f, 0f, 0f, 0f, -ph / 2f);
        assertEquals("originY stays exactly 0f at a non-1.0 settings scale",
                0f, scaled.originY, EPS);
        assertEquals(pw / 2f, scaled.originX, EPS);

        assertTrue("LIGHTNING_EFFECT installs additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.LIGHTNING_EFFECT));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.LIGHTNING_EFFECT));
    }

    @Test
    public void flameBallUsesOriginYLiftedByTwentyTimesSettingsScaleAndStaysAdditive() {
        // Native: setBlendFunction(770,1); sb.draw(img, x, y, pw/2f, ph/2f + 20f * Settings.scale,
        //            pw, ph, scale, scale, rotation); setBlendFunction(770,771).
        //            FlameBallParticleEffect has a vY that render ignores (update-only), so it is not
        //            consumed here.
        float pw = 40f;
        float ph = 24f;
        float x = -7.5f;
        float y = 21.25f;
        float scale = 1.25f;
        float rotation = 90f;

        // settingsScale == 1.0: the lift is exactly 20f.
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLAME_BALL,
                x, y, 999f /* vY ignored (update-only) */, scale, rotation, 7f, 5f, 1f, pw, ph,
                1234f /* vX ignored */, 0f, 0f, 0f, 0f);

        assertEquals(x, p.x, EPS);
        assertEquals("vY is not added to y", y, p.y, EPS);
        assertEquals("originX stays packedWidth/2f", pw / 2f, p.originX, EPS);
        assertEquals("originY is packedHeight/2f + 20f * settingsScale",
                ph / 2f + 20f * 1f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        // Regression guard (the parity fix): at a NON-1.0 settings scale (a 2560-wide viewport is
        // ~1.333) the origin lift scales with it — a bare +20f would drift by 20 * (scale - 1).
        float settingsScale = 1.333f;
        VfxDrawGeometry.Params scaled = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLAME_BALL,
                x, y, 999f, scale, rotation, 7f, 5f, settingsScale, pw, ph, 1234f, 0f, 0f, 0f, 0f);
        assertEquals("originY must scale by Settings.scale",
                ph / 2f + 20f * settingsScale, scaled.originY, EPS);

        assertEquals(20f, VfxDrawGeometry.FLAME_BALL_ORIGIN_Y_OFFSET, EPS);
        assertTrue("FLAME_BALL installs additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLAME_BALL));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FLAME_BALL));
    }

    @Test
    public void shineLinesReusesTheStanceAuraCenterPackedGeometryAndStaysAmbient() {
        // Native: if (!isDone) { setColor(color); sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale,
        //            scale, rotation); } — NO setBlendFunction (ambient) and NO new rule, so its
        //            geometry is byte-identical to STANCE_AURA.
        float pw = 48f;
        float ph = 32f;
        float x = 3.5f;
        float y = -9.25f;
        float scale = 0.75f;
        float rotation = 17f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.SHINE_LINES,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, pw, ph,
                1234f /* vX ignored */, 0f, 0f, 0f, 0f);
        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f);

        assertEquals("SHINE_LINES must equal the STANCE_AURA center-packed geometry", aura, p);

        assertFalse("SHINE_LINES never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.SHINE_LINES));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.SHINE_LINES));
    }

    @Test
    public void newTailOriginOffsetsDoNotChangeAnyPreexistingCenterPackedKind() {
        // Regression: the two new tail scalars default to 0f for every pre-existing kind, so the
        // pre-existing center-packed geometry is unchanged (origin still packedWidth/2f,
        // packedHeight/2f).
        float pw = 64f;
        float ph = 48f;
        float x = 5f;
        float y = 6f;
        float scale = 0.5f;
        float rotation = 33f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);

        VfxDrawGeometry.Params fl = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLAME_PARTICLE,
                x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f);
        assertEquals(pw / 2f, fl.originX, EPS);
        assertEquals(ph / 2f, fl.originY, EPS);
    }

    @Test
    public void torchParticleMAndSReuseTheStanceAuraCenterPackedGeometryAndStayAdditive() {
        // Native (TorchParticleMEffect/TorchParticleSEffect): setBlendFunction(770,1);
        // sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation);
        // setBlendFunction(770,771). Both have a vY that render ignores (update-only), so it is not
        // consumed and their geometry is byte-identical to STANCE_AURA.
        float pw = 52f;
        float ph = 36f;
        float x = 7.5f;
        float y = -4.25f;
        float scale = 0.85f;
        float rotation = 26f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.TORCH_PARTICLE_M,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_S }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* vY ignored (update-only) */, scale, rotation, 7f, 5f, 2f,
                    pw, ph, 1234f /* vX ignored */, 0f, 0f, 0f, 0f);
            assertEquals("TORCH geometry must equal the STANCE_AURA center-packed geometry for "
                    + kind, aura, p);
            assertTrue("expected additive blend for " + kind,
                    VfxDrawGeometry.additiveBlend(kind));
            assertFalse(VfxDrawGeometry.whiteAlphaOnly(kind));
        }
    }

    @Test
    public void lightningOrbPassiveUsesItsFixedRectRotationAndBothFlipFlags() {
        // Native: setColor(color); setBlendFunction(770,1);
        //   sb.draw(img, x - 61f, y - 61f, 61f, 61f, 122f, 122f, scale, scale, rotation,
        //           0, 0, 122, 122, flipX, flipY); setBlendFunction(770,771).
        // The fixed rect is (offset/origin 61, size 122, src 0,0,122,122), the rotation comes from
        // the field, and the flip booleans are per-instance (resolved by the renderer).
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, 64f, 48f,
                1234f /* vX ignored */, 6f /* regionOffsetX ignored */, 10f /* regionOffsetY ignored */,
                0f, 0f);

        assertEquals(x - 61f, p.x, EPS);
        assertEquals(y - 61f, p.y, EPS);
        assertEquals(61f, p.originX, EPS);
        assertEquals(61f, p.originY, EPS);
        assertEquals(122f, p.width, EPS);
        assertEquals(122f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("LOP consumes the rotation field", rotation, p.rotation, EPS);

        assertEquals(61f, VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_OFFSET, EPS);
        assertEquals(61f, VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_ORIGIN, EPS);
        assertEquals(122f, VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SIZE, EPS);
        assertEquals(122, VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_W);
        assertEquals(122, VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_H);

        assertTrue("LIGHTNING_ORB_PASSIVE installs additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE));
    }

    @Test
    public void glowyFireEyesUsesItsFixedRectAndHardcodedZeroRotation() {
        // Native: setBlendFunction(770,1); setColor(color);
        //   sb.draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f, scale, scale, 0f,
        //           0, 0, 128, 128, flippedX, false); setBlendFunction(770,771).
        // The rotation is hardcoded 0f (GlowyFireEyesEffect has no rotation field) and only the
        // horizontal flip is per-instance.
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.GLOWY_FIRE_EYES,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, 64f, 48f,
                1234f /* vX ignored */, 6f, 10f, 0f, 0f);

        assertEquals(x - 64f, p.x, EPS);
        assertEquals(y - 64f, p.y, EPS);
        assertEquals(64f, p.originX, EPS);
        assertEquals(64f, p.originY, EPS);
        assertEquals(128f, p.width, EPS);
        assertEquals(128f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("GFE hardcodes rotation 0f", 0f, p.rotation, EPS);

        assertEquals(64f, VfxDrawGeometry.GLOWY_FIRE_EYES_OFFSET, EPS);
        assertEquals(64f, VfxDrawGeometry.GLOWY_FIRE_EYES_ORIGIN, EPS);
        assertEquals(128f, VfxDrawGeometry.GLOWY_FIRE_EYES_SIZE, EPS);
        assertEquals(128, VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_W);
        assertEquals(128, VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_H);

        assertTrue("GLOWY_FIRE_EYES installs additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.GLOWY_FIRE_EYES));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.GLOWY_FIRE_EYES));
    }

    @Test
    public void instanceFlipPredicatesAreTrueOnlyForTheTwoNewFlipKinds() {
        // LOP passes BOTH flipX and flipY; GFE passes only the horizontal flippedX (its vertical
        // flip is hardcoded false); every other kind hardcodes false, false.
        assertTrue(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE));
        assertTrue(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE));
        assertTrue(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.GLOWY_FIRE_EYES));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.GLOWY_FIRE_EYES));

        // A sample of pre-existing kinds (including every other bare-Texture member).
        VfxDrawGeometry.Kind[] none = {
                VfxDrawGeometry.Kind.STANCE_AURA,
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                VfxDrawGeometry.Kind.SHIELD_PARTICLE,
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE,
                VfxDrawGeometry.Kind.ICE_SHATTER,
                VfxDrawGeometry.Kind.WEB_PARTICLE,
                VfxDrawGeometry.Kind.ENTANGLE,
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE,
                VfxDrawGeometry.Kind.WARNING_SIGN,
                VfxDrawGeometry.Kind.DARK_ORB_PASSIVE,
                VfxDrawGeometry.Kind.SCENE_DUST };
        for (VfxDrawGeometry.Kind kind : none) {
            assertFalse("no per-instance X flip for " + kind,
                    VfxDrawGeometry.usesInstanceFlipX(kind));
            assertFalse("no per-instance Y flip for " + kind,
                    VfxDrawGeometry.usesInstanceFlipY(kind));
        }

        // The truth table is exhaustive over the enum.
        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            assertEquals("X flip truth table for " + kind,
                    kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE
                            || kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES,
                    VfxDrawGeometry.usesInstanceFlipX(kind));
            assertEquals("Y flip truth table for " + kind,
                    kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE,
                    VfxDrawGeometry.usesInstanceFlipY(kind));
        }
    }

    @Test
    public void instanceFlipPredicatesNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.usesInstanceFlipX(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            VfxDrawGeometry.usesInstanceFlipY(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void sceneDustReusesTheFallingDustRegionOffsetOriginAndStaysAmbient() {
        // Native DustEffect: setColor(color); sb.draw(img, x, y, img.offsetX, img.offsetY, pw, ph,
        // scale, scale, rotation) — NO setBlendFunction. It reuses the FALLING_DUST rule verbatim:
        // the ORIGIN is the region's own trim offset (not packed/2).
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;
        float regionOffsetX = 6f;
        float regionOffsetY = 10f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.SCENE_DUST,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, pw, ph,
                1234f /* vX ignored */, regionOffsetX, regionOffsetY, 0f, 0f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals("the origin is the region offsetX", regionOffsetX, p.originX, EPS);
        assertEquals("the origin is the region offsetY", regionOffsetY, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        // The shared rule: SCENE_DUST and FALLING_DUST produce identical geometry for the same input.
        VfxDrawGeometry.Params falling = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FALLING_DUST,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, regionOffsetX, regionOffsetY,
                0f, 0f);
        assertEquals("SCENE_DUST must reuse the FALLING_DUST region-offset origin", falling, p);

        assertFalse("SCENE_DUST never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.SCENE_DUST));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.SCENE_DUST));
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

        // The two newest bare-Texture members are additive too.
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.ICE_SHATTER));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.WEB_PARTICLE));

        // The four newest members: Entangle is additive while the block impact line, exhaust pile,
        // and unknown particle never call setBlendFunction natively (ambient).
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.ENTANGLE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.BLOCK_IMPACT_LINE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.EXHAUST_PILE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.UNKNOWN_PARTICLE));

        // The five newest members: the flame particle, lightning orb activate, and dark orb passive
        // are additive; the damage impact blur and line never call setBlendFunction (ambient).
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLAME_PARTICLE));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DARK_ORB_PASSIVE));

        // The three newest members: WarningSignEffect is additive, while StunStarEffect and
        // FallingDustEffect never call setBlendFunction (ambient).
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.WARNING_SIGN));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.STUN_STAR));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FALLING_DUST));

        // The three newest members: LightningEffect and FlameBallParticleEffect are additive, while
        // ShineLinesEffect never calls setBlendFunction (ambient).
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.LIGHTNING_EFFECT));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLAME_BALL));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.SHINE_LINES));

        // The three newest members: TorchParticleMEffect and TorchParticleSEffect are additive, while
        // DustEffect never calls setBlendFunction (ambient).
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.TORCH_PARTICLE_M));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.TORCH_PARTICLE_S));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.SCENE_DUST));

        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.FLASH_ATK_IMG
                    || kind == VfxDrawGeometry.Kind.SMOKE_BLUR
                    || kind == VfxDrawGeometry.Kind.CEILING_DUST
                    || kind == VfxDrawGeometry.Kind.NEMESIS_FIRE
                    || kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                    || kind == VfxDrawGeometry.Kind.GENERIC_SMOKE
                    || kind == VfxDrawGeometry.Kind.EXHAUST_BLUR
                    || kind == VfxDrawGeometry.Kind.BLOCK_IMPACT_LINE
                    || kind == VfxDrawGeometry.Kind.EXHAUST_PILE
                    || kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE
                    || kind == VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR
                    || kind == VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE
                    || kind == VfxDrawGeometry.Kind.STUN_STAR
                    || kind == VfxDrawGeometry.Kind.FALLING_DUST
                    || kind == VfxDrawGeometry.Kind.SHINE_LINES
                    || kind == VfxDrawGeometry.Kind.SCENE_DUST) {
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
    public void nativeSkipsDrawWithoutImageIsTrueOnlyForFlashAtkImg() {
        // Only FlashAtkImgEffect guards its native draw with if (img != null); every other kind
        // either draws unconditionally or draws a fixed static texture.
        assertTrue("only FLASH_ATK_IMG guards its draw on a present image",
                VfxDrawGeometry.nativeSkipsDrawWithoutImage(
                        VfxDrawGeometry.Kind.FLASH_ATK_IMG));

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.STANCE_AURA,
                VfxDrawGeometry.Kind.WRATH_PARTICLE,
                VfxDrawGeometry.Kind.DIVINITY_PARTICLE,
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                VfxDrawGeometry.Kind.DIVINITY_STANCE_CHANGE,
                VfxDrawGeometry.Kind.LIGHT_FLARE,
                VfxDrawGeometry.Kind.LIGHT_FLARE_M,
                VfxDrawGeometry.Kind.LIGHT_FLARE_L,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_L,
                VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.Kind.RED_FIRE_BURST,
                VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.Kind.CEILING_DUST,
                VfxDrawGeometry.Kind.NEMESIS_FIRE,
                VfxDrawGeometry.Kind.SHIELD_PARTICLE,
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_XL,
                VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE,
                VfxDrawGeometry.Kind.GENERIC_SMOKE,
                VfxDrawGeometry.Kind.EXHAUST_BLUR,
                VfxDrawGeometry.Kind.ICE_SHATTER,
                VfxDrawGeometry.Kind.WEB_PARTICLE,
                VfxDrawGeometry.Kind.ENTANGLE,
                VfxDrawGeometry.Kind.BLOCK_IMPACT_LINE,
                VfxDrawGeometry.Kind.EXHAUST_PILE,
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE,
                VfxDrawGeometry.Kind.FLAME_PARTICLE,
                VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE,
                VfxDrawGeometry.Kind.DARK_ORB_PASSIVE,
                VfxDrawGeometry.Kind.WARNING_SIGN,
                VfxDrawGeometry.Kind.STUN_STAR,
                VfxDrawGeometry.Kind.FALLING_DUST,
                VfxDrawGeometry.Kind.LIGHTNING_EFFECT,
                VfxDrawGeometry.Kind.FLAME_BALL,
                VfxDrawGeometry.Kind.SHINE_LINES,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_M,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_S,
                VfxDrawGeometry.Kind.SCENE_DUST }) {
            assertFalse("must not be a no-pixel-without-image kind: " + kind,
                    VfxDrawGeometry.nativeSkipsDrawWithoutImage(kind));
        }

        // The truth table is exhaustive over the enum except FLASH_ATK_IMG.
        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            assertEquals("no-pixel-without-image must be FLASH_ATK_IMG-only for " + kind,
                    kind == VfxDrawGeometry.Kind.FLASH_ATK_IMG,
                    VfxDrawGeometry.nativeSkipsDrawWithoutImage(kind));
        }
    }

    @Test
    public void nativeSkipsDrawWithoutImageNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.nativeSkipsDrawWithoutImage(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void whiteAlphaOnlyIsTrueOnlyForWebParticleAndEntangle() {
        // WebParticleEffect and (byte-identically) EntangleEffect are the only kinds whose native
        // render rewrites the set color, forcing RGB to white and taking alpha from the effect's
        // color.
        assertTrue(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.WEB_PARTICLE));
        assertTrue(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.ENTANGLE));

        // Every other kind — including the other bare-Texture members — passes the effect's own
        // color through unchanged.
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.ICE_SHATTER));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.CALM_PARTICLE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.SHIELD_PARTICLE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.DEBUFF_PARTICLE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.UNKNOWN_PARTICLE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.DARK_ORB_PASSIVE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FLAME_PARTICLE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.WARNING_SIGN));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.STUN_STAR));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FALLING_DUST));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.LIGHTNING_EFFECT));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FLAME_BALL));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.SHINE_LINES));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.TORCH_PARTICLE_M));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.TORCH_PARTICLE_S));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.SCENE_DUST));

        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.WEB_PARTICLE
                    || kind == VfxDrawGeometry.Kind.ENTANGLE) {
                continue;
            }
            assertFalse("expected the pass-through color rule for " + kind,
                    VfxDrawGeometry.whiteAlphaOnly(kind));
        }
    }

    @Test
    public void whiteAlphaOnlyNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.whiteAlphaOnly(null);
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
                -16.5f, 7.25f, 0f, 0f, 0f, 0f, 0f);

        assertEquals(-16.5f / 2f, p.originX, EPS);
        assertEquals(7.25f / 2f, p.originY, EPS);
        assertEquals(-16.5f, p.width, EPS);
        assertEquals(7.25f, p.height, EPS);
        assertEquals(-0.5f, p.scaleX, EPS);
        assertEquals(-0.5f, p.scaleY, EPS);
    }
}
