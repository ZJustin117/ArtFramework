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
        // The two newest members, via constants and literal FQNs.
        assertSame(VfxDrawGeometry.Kind.FLYING_SPIKE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FLYING_SPIKE));
        assertSame(VfxDrawGeometry.Kind.FLYING_SPIKE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect"));
        assertSame(VfxDrawGeometry.Kind.CONE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.CONE_EFFECT));
        assertSame(VfxDrawGeometry.Kind.CONE,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.ConeEffect"));
        assertSame(VfxDrawGeometry.Kind.FALLING_ICE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FALLING_ICE));
        assertSame(VfxDrawGeometry.Kind.FALLING_ICE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.FallingIceEffect"));
        assertSame(VfxDrawGeometry.Kind.DAMAGE_HEART,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.DAMAGE_HEART));
        assertSame(VfxDrawGeometry.Kind.DAMAGE_HEART,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.DamageHeartEffect"));
        // The two newest mirror members, via constants and literal FQNs.
        assertSame(VfxDrawGeometry.Kind.SPOOKY_CHEST,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SPOOKY_CHEST));
        assertSame(VfxDrawGeometry.Kind.SPOOKY_CHEST,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect"));
        assertSame(VfxDrawGeometry.Kind.IRONCLAD_VICTORY_FLAME,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME));
        assertSame(VfxDrawGeometry.Kind.IRONCLAD_VICTORY_FLAME,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.IroncladVictoryFlameEffect"));
        // The three newest F23 members, via constants and literal FQNs.
        assertSame(VfxDrawGeometry.Kind.SPOOKIER_CHEST,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.SPOOKIER_CHEST));
        assertSame(VfxDrawGeometry.Kind.SPOOKIER_CHEST,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect"));
        assertSame(VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER));
        assertSame(VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect"));
        assertSame(VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.DEATH_SCREEN_FLOATY));
        assertSame(VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect"));
        // The newest F24 member, via constant and literal FQN.
        assertSame(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.WRATH_STANCE_CHANGE));
        assertSame(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle"));
        // The newest F25 member, via constant and literal FQN.
        assertSame(VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION));
        assertSame(VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.stance.StanceChangeAbsorptionParticle"));
        // The two newest F27 members, via constants and literal FQNs.
        assertSame(VfxDrawGeometry.Kind.WATER_SPLASH,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.WATER_SPLASH));
        assertSame(VfxDrawGeometry.Kind.WATER_SPLASH,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.WaterSplashParticleEffect"));
        assertSame(VfxDrawGeometry.Kind.BUFF_PARTICLE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.BUFF_PARTICLE));
        assertSame(VfxDrawGeometry.Kind.BUFF_PARTICLE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.BuffParticleEffect"));
        // The two newest F28 members, via constants and literal FQNs.
        assertSame(VfxDrawGeometry.Kind.BOTTOM_FOG,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.BOTTOM_FOG));
        assertSame(VfxDrawGeometry.Kind.BOTTOM_FOG,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.scene.BottomFogEffect"));
        assertSame(VfxDrawGeometry.Kind.GIANT_FIRE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.GIANT_FIRE));
        assertSame(VfxDrawGeometry.Kind.GIANT_FIRE,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.GiantFireEffect"));
        // The newest F29 member, via constant and literal FQN.
        assertSame(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.TORCH_HEAD_FIRE));
        assertSame(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.TorchHeadFireEffect"));
        // The newest F30 member, via constant and literal FQN.
        assertSame(VfxDrawGeometry.Kind.CARD_TRAIL,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.CARD_TRAIL));
        assertSame(VfxDrawGeometry.Kind.CARD_TRAIL,
                VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.CardTrailEffect"));
        // The newest (NRO-04 B01) member, via constant and literal FQN.
        assertSame(VfxDrawGeometry.Kind.FLYING_ORB,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FLYING_ORB));
        assertSame(VfxDrawGeometry.Kind.FLYING_ORB,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.FlyingOrbEffect"));
        // The newest (NRO-04 B02) member, via constant and literal FQN.
        assertSame(VfxDrawGeometry.Kind.FLICK_COIN,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.FLICK_COIN));
        assertSame(VfxDrawGeometry.Kind.FLICK_COIN,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.FlickCoinEffect"));
        // The newest (NRO-04 B03) member, via constant and literal FQN.
        assertSame(VfxDrawGeometry.Kind.HEAL_PANEL,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.HEAL_PANEL));
        assertSame(VfxDrawGeometry.Kind.HEAL_PANEL,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.HealPanelEffect"));
    }

    @Test
    public void torchHeadFireKindForFailsOpenForNearMisses() {
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.TorchHeadFireEffect2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.TorchHeadFireEffect$Sub"));
        assertNull(VfxDrawGeometry.kindFor("TorchHeadFireEffect"));
        // `TorchParticle*Effect` names must not be matched by the TorchHeadFire FQN.
        assertNull(VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.TorchHeadFire"));
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
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("FlyingSpikeEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FlyingSpikeEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlyingSpike")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ConeEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.ConeEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("ConeEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.ConeEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.Cone")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FallingIceEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FallingIceEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("FallingIceEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.FallingIceEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FallingIce")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DamageHeartEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DamageHeartEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("DamageHeartEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.DamageHeartEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DamageHeart")); // near-miss (no Effect)
        // The two newest mirror members: exact FQN only, near-misses fail open.
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("SpookyChestEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.SpookyChestEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.SpookyChest")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.IroncladVictoryFlameEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.IroncladVictoryFlameEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("IroncladVictoryFlameEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.IroncladVictoryFlameEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.IroncladVictoryFlame")); // near-miss (no Effect)
        // The three newest F23 members: exact FQN only, near-misses fail open.
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("SpookierChestEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.SpookierChestEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.SpookierChest")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("CampfireSleepScreenCoverEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.CampfireSleepScreenCoverEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCover")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("DeathScreenFloatyEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.DeathScreenFloatyEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.DeathScreenFloaty")); // near-miss (no Effect)
        // The newest F24 member: exact FQN only, near-misses fail open.
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("WrathStanceChangeParticle")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.WrathStanceChangeParticle")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.WrathStanceChange")); // near-miss (no Particle)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.WrathStanceAuraParticle")); // near-miss
        // The newest F25 member: exact FQN only, near-misses fail open.
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.StanceChangeAbsorptionParticle2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.StanceChangeAbsorptionParticle$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("StanceChangeAbsorptionParticle")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.StanceChangeAbsorptionParticle")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.StanceChangeAbsorption")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.stance.AbsorptionParticle")); // near-miss

        // The two newest F27 kinds: near-miss / nested / simple-name / wrong-package fail open.
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.WaterSplashParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.WaterSplashParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("WaterSplashParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.WaterSplashParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.WaterSplashEffect")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.BuffParticleEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.BuffParticleEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("BuffParticleEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.BuffParticleEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.BuffEffect")); // near-miss

        // The two newest F28 kinds: near-miss / nested / simple-name / wrong-package fail open.
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.BottomFogEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.BottomFogEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("BottomFogEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.BottomFogEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.BottomFog")); // near-miss (no Effect)
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.GiantFireEffect2")); // near-miss
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.GiantFireEffect$Sub")); // nested
        assertNull(VfxDrawGeometry.kindFor("GiantFireEffect")); // simple name only
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.scene.GiantFireEffect")); // wrong package
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.GiantFire")); // near-miss (no Effect)
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
                pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                1f, 2f, 3f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, vY, scale, rotation, durDiv2, duration, settingsScale, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                0f, 0f, 0f, scale, 0f, durDiv2, duration, settingsScale, 10f, 10f, 0f, 0f, 0f, 0f, 0f, 1f);

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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f, 1f);

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
                -100f, 777f, 0f, 0f, 0f, 0f, 0f, 1f);

        assertEquals(1f, p.x, EPS);
        assertEquals(2f, p.y, EPS); // vY not added
        assertEquals(25f, p.width, EPS);
        assertEquals(128f, p.height, EPS);
        assertEquals(0.5f, p.scaleX, EPS);
        assertEquals(0.5f, p.scaleY, EPS);

        // A different packed size produces byte-identical geometry for Calm.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                1f, 2f, 0f, 0.5f, 9f, 1f, 0.4f, 3f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
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
                pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals(p, q);
    }

    @Test
    public void nullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.params(null, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.LIGHT_FLARE_M,
                VfxDrawGeometry.Kind.LIGHT_FLARE_L,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_L }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* ignored */, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.Kind.RED_FIRE_BURST,
                VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.Kind.CEILING_DUST,
                VfxDrawGeometry.Kind.NEMESIS_FIRE }) {
            // vY=999f is ignored by every one of these formulas (vY is update-only).
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.TORCH_PARTICLE_XL,
                VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE,
                VfxDrawGeometry.Kind.GENERIC_SMOKE,
                VfxDrawGeometry.Kind.EXHAUST_BLUR }) {
            // vY=999f is ignored by every one of these formulas (vY is update-only).
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                    kind, x, y, -12345f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, 0f, scale, -123f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, 0f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, -12345f, scale, -123f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                7f, 5f, 2f, 48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, 999f, scale, 45f, 7f, 5f, 2f, 48f, 96f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.BLOCK_IMPACT_LINE,
                VfxDrawGeometry.Kind.EXHAUST_PILE }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* ignored */, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                VfxDrawGeometry.Kind.STANCE_AURA, x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.FLAME_PARTICLE,
                VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* ignored */, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

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
                48f /* packedWidth ignored */, 96f /* packedHeight ignored */, 0f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                1234f /* no vX field; ignored */, 17f, 19f /* no region origin; ignored */, 0f, 0f, 1f);

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
                x, y, 0f, -123f, -45f, 0f, 0f, settingsScale, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
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
                vX, 0f, 0f, 0f, 0f, 1f);

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
                1234f /* vX ignored */, regionOffsetX, regionOffsetY, 0f, 0f, 1f);

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
                0f, -ph / 2f /* originY offset that collapses packedHeight/2f to 0f */, 1f);

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
                1234f, 0f, 0f, 0f, -ph / 2f, 1f);
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
                1234f /* vX ignored */, 0f, 0f, 0f, 0f, 1f);

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
                x, y, 999f, scale, rotation, 7f, 5f, settingsScale, pw, ph, 1234f, 0f, 0f, 0f, 0f, 1f);
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
                1234f /* vX ignored */, 0f, 0f, 0f, 0f, 1f);
        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f, 1f);

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
                x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);

        VfxDrawGeometry.Params fl = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLAME_PARTICLE,
                x, y, 0f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
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
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f, 1f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.TORCH_PARTICLE_M,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_S }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* vY ignored (update-only) */, scale, rotation, 7f, 5f, 2f,
                    pw, ph, 1234f /* vX ignored */, 0f, 0f, 0f, 0f, 1f);
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
                0f, 0f, 1f);

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
                1234f /* vX ignored */, 6f, 10f, 0f, 0f, 1f);

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
    public void flyingSpikeReusesTheStanceAuraCenterPackedGeometryAndStaysAdditive() {
        // Native FlyingSpikeEffect: setBlendFunction(770,1); setColor(color);
        //   sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation);
        //   setBlendFunction(770,771). Its vX/vY are update-only (never read in render), so its
        // geometry is byte-identical to STANCE_AURA and it is additive.
        float pw = 52f;
        float ph = 36f;
        float x = 7.5f;
        float y = -4.25f;
        float scale = 0.85f;
        float rotation = 26f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f, 1f);
        VfxDrawGeometry.Params spike = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLYING_SPIKE,
                x, y, 999f /* vY ignored (update-only) */, scale, rotation, 7f, 5f, 2f,
                pw, ph, 1234f /* vX ignored (update-only) */, 0f, 0f, 0f, 0f, 1f);

        assertEquals("FLYING_SPIKE matches the STANCE_AURA center-packed geometry", aura, spike);
        assertTrue("FLYING_SPIKE installs additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLYING_SPIKE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FLYING_SPIKE));
    }

    @Test
    public void coneUsesTheZeroOriginXAndTheUniformOnePointOneScaleAmbiently() {
        // Native ConeEffect: setColor(color); sb.draw(img, x, y, 0f, ph/2f, pw, ph,
        //   scale*1.1f, scale*1.1f, rotation) — NO setBlendFunction. The origin X is 0f (NOT the
        // shared packedWidth/2f center), origin Y stays packedHeight/2f, and the uniform scale is the
        // effect's own scale times the hardcoded 1.1f.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.CONE,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, pw, ph,
                1234f /* vX ignored */, 6f /* regionOffsetX ignored */, 10f /* regionOffsetY ignored */,
                0f, 0f, 1f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals("the Cone origin X is 0f, NOT packedWidth/2f", 0f, p.originX, EPS);
        assertEquals("the Cone origin Y is packedHeight/2f", ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals("the Cone uniform scale is scale * 1.1f", scale * 1.1f, p.scaleX, EPS);
        assertEquals("the Cone uniform scale is scale * 1.1f", scale * 1.1f, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        assertEquals(1.1f, VfxDrawGeometry.CONE_SCALE_MULTIPLIER, EPS);
        assertFalse("CONE never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.CONE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.CONE));
    }

    @Test
    public void newestTwoKindsBlendTruthTableIsAdditiveAmbient() {
        // FLYING_SPIKE is additive (setBlendFunction around its draw); CONE never calls
        // setBlendFunction (ambient). Neither rewrites the set color.
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLYING_SPIKE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.CONE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FLYING_SPIKE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.CONE));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.FLYING_SPIKE));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.CONE));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.FLYING_SPIKE));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.CONE));
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
                1234f /* vX ignored */, regionOffsetX, regionOffsetY, 0f, 0f, 1f);

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
                0f, 0f, 1f);
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

        // The two newest members: FlyingSpikeEffect is additive while ConeEffect never calls
        // setBlendFunction (ambient).
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLYING_SPIKE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.CONE));

        // The two newest guard kinds: FallingIceEffect is additive, DamageHeartEffect never calls
        // setBlendFunction (ambient).
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FALLING_ICE));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DAMAGE_HEART));

        // The two newest mirror members are both ambient center-packed (no setBlendFunction).
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.SPOOKY_CHEST));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.IRONCLAD_VICTORY_FLAME));

        // The three newest F23 members are all ambient center-packed (no setBlendFunction).
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.SPOOKIER_CHEST));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER));
        assertFalse(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY));

        // The newest F25 member is additive (it installs/restores the additive blend natively).
        assertTrue("STANCE_CHANGE_ABSORPTION installs the additive blend",
                VfxDrawGeometry.additiveBlend(
                        VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertTrue(VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));

        // The newest F27 members: WaterSplashParticleEffect never calls setBlendFunction (ambient),
        // while BuffParticleEffect installs/restores the additive blend.
        assertFalse("WATER_SPLASH never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertTrue("BUFF_PARTICLE installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.BUFF_PARTICLE));

        // The newest F28 members: BottomFogEffect never calls setBlendFunction (ambient), while
        // GiantFireEffect installs/restores the additive blend.
        assertFalse("BOTTOM_FOG never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertTrue("GIANT_FIRE installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.GIANT_FIRE));

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
                    || kind == VfxDrawGeometry.Kind.SCENE_DUST
                    || kind == VfxDrawGeometry.Kind.CONE
                    || kind == VfxDrawGeometry.Kind.DAMAGE_HEART
                    || kind == VfxDrawGeometry.Kind.SPOOKY_CHEST
                    || kind == VfxDrawGeometry.Kind.IRONCLAD_VICTORY_FLAME
                    || kind == VfxDrawGeometry.Kind.SPOOKIER_CHEST
                    || kind == VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER
                    || kind == VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY
                    || kind == VfxDrawGeometry.Kind.WATER_SPLASH
                    || kind == VfxDrawGeometry.Kind.BOTTOM_FOG) {
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
                VfxDrawGeometry.Kind.SCENE_DUST,
                VfxDrawGeometry.Kind.FLYING_SPIKE,
                VfxDrawGeometry.Kind.FALLING_ICE,
                VfxDrawGeometry.Kind.DAMAGE_HEART,
                VfxDrawGeometry.Kind.CONE }) {
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
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FLYING_SPIKE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.CONE));

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
                -16.5f, 7.25f, 0f, 0f, 0f, 0f, 0f, 1f);

        assertEquals(-16.5f / 2f, p.originX, EPS);
        assertEquals(7.25f / 2f, p.originY, EPS);
        assertEquals(-16.5f, p.width, EPS);
        assertEquals(7.25f, p.height, EPS);
        assertEquals(-0.5f, p.scaleX, EPS);
        assertEquals(-0.5f, p.scaleY, EPS);
    }

    @Test
    public void fallingIceUsesTheFixed96RectAndTheFieldRotationAdditively() {
        // Native FallingIceEffect: if (waitTimer < 0f) { setBlendFunction(770,1); setColor(color);
        //   sb.draw(img, x, y, 48f, 48f, 96f, 96f, scale, scale, rotation, 0, 0, 96, 96,
        //           false, false); setBlendFunction(770,771); }.
        // The fixed rect is (origin 48, size 96, src 0,0,96,96) with x/y passed through unchanged
        // (the native draw passes x, y directly — there is NO position offset), the rotation comes
        // from the field, and the packed region/vY/settings are all unused.
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FALLING_ICE,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, 64f, 48f,
                1234f /* vX ignored */, 6f /* regionOffsetX ignored */, 10f /* regionOffsetY ignored */,
                0f, 0f, 1f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(48f, p.originX, EPS);
        assertEquals(48f, p.originY, EPS);
        assertEquals(96f, p.width, EPS);
        assertEquals(96f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("FALLING_ICE consumes the rotation field", rotation, p.rotation, EPS);

        // The host-neutral constants match the native hardcoded rect.
        assertEquals(48f, VfxDrawGeometry.FALLING_ICE_ORIGIN, EPS);
        assertEquals(96f, VfxDrawGeometry.FALLING_ICE_SIZE, EPS);
        assertEquals(0, VfxDrawGeometry.FALLING_ICE_SRC_X);
        assertEquals(0, VfxDrawGeometry.FALLING_ICE_SRC_Y);
        assertEquals(96, VfxDrawGeometry.FALLING_ICE_SRC_W);
        assertEquals(96, VfxDrawGeometry.FALLING_ICE_SRC_H);

        // A different vY/packed size produces byte-identical geometry.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FALLING_ICE,
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals(p, q);

        assertTrue("FALLING_ICE installs additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FALLING_ICE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FALLING_ICE));
    }

    @Test
    public void damageHeartMatchesTheStanceAuraCenterPackedGeometryAmbiently() {
        // Native DamageHeartEffect: if (delayTimer < 0f) { setColor(color);
        //   sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation); } — NO
        // setBlendFunction, so it is ambient and geometry-identical to STANCE_AURA.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f, 1f);
        VfxDrawGeometry.Params heart = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.DAMAGE_HEART,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f,
                pw, ph, 1234f /* vX ignored */, 0f, 0f, 0f, 0f, 1f);

        assertEquals("DAMAGE_HEART matches the STANCE_AURA center-packed geometry", aura, heart);
        assertFalse("DAMAGE_HEART never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.DAMAGE_HEART));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.DAMAGE_HEART));
    }

    @Test
    public void spookyChestAndIroncladVictoryFlameReuseTheStanceAuraCenterPackedGeometryAmbiently() {
        // Native SpookyChestEffect / IroncladVictoryFlameEffect: setColor(color);
        //   if (flipX != img.isFlipX()) img.flip(true, false);   (SpookyChest also flips Y)
        //   sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation);   -- NO
        //   setBlendFunction, so both are ambient center-packed and geometry-identical to
        // StanceAuraEffect (the per-instance mirror is resolved by the renderer as a UV swap).
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f, 1f);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.SPOOKY_CHEST,
                VfxDrawGeometry.Kind.IRONCLAD_VICTORY_FLAME,
                VfxDrawGeometry.Kind.SPOOKIER_CHEST,
                VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER,
                VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY }) {
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f,
                    pw, ph, 1234f /* vX ignored */, 0f, 0f, 0f, 0f, 1f);
            assertEquals("geometry must equal the STANCE_AURA center-packed geometry for " + kind,
                    aura, p);
            assertFalse("no setBlendFunction (ambient) for " + kind,
                    VfxDrawGeometry.additiveBlend(kind));
            assertFalse(VfxDrawGeometry.whiteAlphaOnly(kind));
        }
    }

    @Test
    public void instanceMirrorPredicatesAreTrueOnlyForTheThreeMirrorKinds() {
        // Native FlameParticleEffect, SpookyChestEffect, and IroncladVictoryFlameEffect mirror the
        // drawn sprite horizontally when their own flipX is set; only SpookyChestEffect also mirrors
        // vertically (FlameParticle/IroncladVictoryFlame declare no flipY field).
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.FLAME_PARTICLE));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.SPOOKY_CHEST));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(
                VfxDrawGeometry.Kind.IRONCLAD_VICTORY_FLAME));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.SPOOKIER_CHEST));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(
                VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER));
        // The newest F28 members: BottomFog carries flipX+flipY, GiantFire carries flipX only.
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.FLAME_PARTICLE));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.SPOOKY_CHEST));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(
                VfxDrawGeometry.Kind.IRONCLAD_VICTORY_FLAME));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.SPOOKIER_CHEST));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorY(
                VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertFalse("GIANT_FIRE has no flipY field, so it is not a vertical mirror kind",
                VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertFalse("DEATH_SCREEN_FLOATY has no flip flags, so it is not a mirror kind",
                VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY));
        assertFalse("DEATH_SCREEN_FLOATY has no flip flags, so it is not a mirror kind",
                VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY));

        // A sample of pre-existing kinds (including the shape-C flip kinds, which are DISTINCT).
        VfxDrawGeometry.Kind[] none = {
                VfxDrawGeometry.Kind.STANCE_AURA,
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                VfxDrawGeometry.Kind.WEB_PARTICLE,
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE,
                VfxDrawGeometry.Kind.SCENE_DUST,
                VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE,
                VfxDrawGeometry.Kind.GLOWY_FIRE_EYES,
                VfxDrawGeometry.Kind.FALLING_ICE,
                VfxDrawGeometry.Kind.DAMAGE_HEART,
                VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY };
        for (VfxDrawGeometry.Kind kind : none) {
            assertFalse("no img-path X mirror for " + kind,
                    VfxDrawGeometry.usesInstanceMirrorX(kind));
            assertFalse("no img-path Y mirror for " + kind,
                    VfxDrawGeometry.usesInstanceMirrorY(kind));
        }

        // Independent exhaustive check: enumerate the expected true kinds as fixed data and compare
        // against the actual true set over every enum constant (rather than restating the production
        // per-kind disjunction inline).
        java.util.EnumSet<VfxDrawGeometry.Kind> expectedMirrorX =
                java.util.EnumSet.of(VfxDrawGeometry.Kind.FLAME_PARTICLE,
                        VfxDrawGeometry.Kind.SPOOKY_CHEST,
                        VfxDrawGeometry.Kind.IRONCLAD_VICTORY_FLAME,
                        VfxDrawGeometry.Kind.SPOOKIER_CHEST,
                        VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER,
                        VfxDrawGeometry.Kind.BOTTOM_FOG,
                        VfxDrawGeometry.Kind.GIANT_FIRE);
        java.util.EnumSet<VfxDrawGeometry.Kind> expectedMirrorY =
                java.util.EnumSet.of(VfxDrawGeometry.Kind.SPOOKY_CHEST,
                        VfxDrawGeometry.Kind.SPOOKIER_CHEST,
                        VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER,
                        VfxDrawGeometry.Kind.BOTTOM_FOG);
        java.util.EnumSet<VfxDrawGeometry.Kind> actualMirrorX =
                java.util.EnumSet.noneOf(VfxDrawGeometry.Kind.class);
        java.util.EnumSet<VfxDrawGeometry.Kind> actualMirrorY =
                java.util.EnumSet.noneOf(VfxDrawGeometry.Kind.class);
        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (VfxDrawGeometry.usesInstanceMirrorX(kind)) {
                actualMirrorX.add(kind);
            }
            if (VfxDrawGeometry.usesInstanceMirrorY(kind)) {
                actualMirrorY.add(kind);
            }
        }
        assertEquals("the X mirror kind set is exactly the expected seven",
                expectedMirrorX, actualMirrorX);
        assertEquals("the Y mirror kind set is exactly the expected four",
                expectedMirrorY, actualMirrorY);
    }

    @Test
    public void instanceMirrorPredicatesNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.usesInstanceMirrorX(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            VfxDrawGeometry.usesInstanceMirrorY(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void nativeSkipsDrawByGuardIsTrueOnlyForTheGuardKinds() {
        // Independent truth table over a fixed sample of representative kinds (not a restatement of
        // the implementation disjunction): only the natively wait-guarded kinds are true.
        assertTrue(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.FALLING_ICE));
        assertTrue(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.DAMAGE_HEART));
        assertTrue(VfxDrawGeometry.nativeSkipsDrawByGuard(
                VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.STANCE_AURA,
                VfxDrawGeometry.Kind.FLASH_ATK_IMG,
                VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.Kind.SHIELD_PARTICLE,
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE,
                VfxDrawGeometry.Kind.ICE_SHATTER,
                VfxDrawGeometry.Kind.WEB_PARTICLE,
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE,
                VfxDrawGeometry.Kind.WARNING_SIGN,
                VfxDrawGeometry.Kind.STUN_STAR,
                VfxDrawGeometry.Kind.SHINE_LINES,
                VfxDrawGeometry.Kind.SCENE_DUST,
                VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE,
                VfxDrawGeometry.Kind.GLOWY_FIRE_EYES,
                VfxDrawGeometry.Kind.FLYING_SPIKE,
                VfxDrawGeometry.Kind.CONE }) {
            assertFalse("only the wait-guarded kinds are true: " + kind,
                    VfxDrawGeometry.nativeSkipsDrawByGuard(kind));
        }
    }

    @Test
    public void nativeSkipsDrawByGuardNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.nativeSkipsDrawByGuard(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void guardFieldNameIsTheHostNeutralNameOnlyForGuardKinds() {
        assertEquals("waitTimer",
                VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.FALLING_ICE));
        assertEquals("delayTimer",
                VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.DAMAGE_HEART));
        assertEquals("delayTimer",
                VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
        assertEquals("isDone",
                VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.FLYING_ORB));
        assertEquals("waitTimer", VfxDrawGeometry.FALLING_ICE_GUARD_FIELD);
        assertEquals("delayTimer", VfxDrawGeometry.DAMAGE_HEART_GUARD_FIELD);
        assertEquals("delayTimer", VfxDrawGeometry.WRATH_STANCE_CHANGE_GUARD_FIELD);
        assertEquals("isDone", VfxDrawGeometry.FLYING_ORB_GUARD_FIELD);

        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.FALLING_ICE) {
                assertEquals("waitTimer", VfxDrawGeometry.guardFieldName(kind));
                continue;
            }
            if (kind == VfxDrawGeometry.Kind.DAMAGE_HEART) {
                assertEquals("delayTimer", VfxDrawGeometry.guardFieldName(kind));
                continue;
            }
            if (kind == VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE) {
                assertEquals("delayTimer", VfxDrawGeometry.guardFieldName(kind));
                continue;
            }
            if (kind == VfxDrawGeometry.Kind.FLYING_ORB) {
                assertEquals("isDone", VfxDrawGeometry.guardFieldName(kind));
                continue;
            }
            assertNull("no guard field name for " + kind, VfxDrawGeometry.guardFieldName(kind));
            assertFalse("no guard for " + kind, VfxDrawGeometry.nativeSkipsDrawByGuard(kind));
        }
    }

    @Test
    public void guardBlocksEncodesThePerKindThresholdDifference() {
        // WRATH blocks ONLY when the field is strictly positive (native: if (delayTimer > 0f) return),
        // so 0f does NOT block; FALLING_ICE/DAMAGE_HEART block at >= 0f (native: if (field < 0f)).
        assertFalse("Wrath draws at delayTimer == 0f",
                VfxDrawGeometry.guardBlocks(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, 0f));
        assertFalse("Wrath draws at a negative delayTimer",
                VfxDrawGeometry.guardBlocks(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, -0.5f));
        assertTrue("Wrath blocks at delayTimer > 0f",
                VfxDrawGeometry.guardBlocks(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, 0.01f));
        assertTrue(VfxDrawGeometry.guardBlocks(
                VfxDrawGeometry.Kind.FALLING_ICE, 0f));
        assertTrue(VfxDrawGeometry.guardBlocks(
                VfxDrawGeometry.Kind.DAMAGE_HEART, 0f));
        assertFalse(VfxDrawGeometry.guardBlocks(
                VfxDrawGeometry.Kind.FALLING_ICE, -0.01f));
        assertTrue(VfxDrawGeometry.guardBlocks(
                VfxDrawGeometry.Kind.FALLING_ICE, 0.01f));

        // NaN parity with the native comparisons:
        //  - native FALLING_ICE/DAMAGE_HEART draw iff `field < 0f`, and `NaN < 0f` is false, so NaN
        //    BLOCKS (ART must not draw where native draws nothing);
        //  - native WRATH returns iff `delayTimer > 0f`, and `NaN > 0f` is false, so NaN does NOT
        //    block (native draws).
        assertTrue("NaN blocks FALLING_ICE (native NaN < 0f is false)",
                VfxDrawGeometry.guardBlocks(VfxDrawGeometry.Kind.FALLING_ICE, Float.NaN));
        assertTrue("NaN blocks DAMAGE_HEART (native NaN < 0f is false)",
                VfxDrawGeometry.guardBlocks(VfxDrawGeometry.Kind.DAMAGE_HEART, Float.NaN));
        assertFalse("NaN does not block WRATH (native NaN > 0f is false)",
                VfxDrawGeometry.guardBlocks(
                        VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, Float.NaN));
        assertTrue("+Inf blocks FALLING_ICE", VfxDrawGeometry.guardBlocks(
                VfxDrawGeometry.Kind.FALLING_ICE, Float.POSITIVE_INFINITY));
        assertFalse("-Inf does not block FALLING_ICE", VfxDrawGeometry.guardBlocks(
                VfxDrawGeometry.Kind.FALLING_ICE, Float.NEGATIVE_INFINITY));
        assertTrue("+Inf blocks WRATH", VfxDrawGeometry.guardBlocks(
                VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, Float.POSITIVE_INFINITY));
        assertFalse("-Inf does not block WRATH", VfxDrawGeometry.guardBlocks(
                VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, Float.NEGATIVE_INFINITY));

        // Every non-guard kind is never blocked regardless of the value.
        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (VfxDrawGeometry.nativeSkipsDrawByGuard(kind)) continue;
            assertFalse("no value blocks a non-guard kind: " + kind,
                    VfxDrawGeometry.guardBlocks(kind, 0f));
            assertFalse("no value blocks a non-guard kind: " + kind,
                    VfxDrawGeometry.guardBlocks(kind, 123.4f));
            assertFalse("no value blocks a non-guard kind: " + kind,
                    VfxDrawGeometry.guardBlocks(kind, -123.4f));
        }
    }

    @Test
    public void randomRangesIsOrderedForWrathAndEmptyForOtherKinds() {
        java.util.List<float[]> wrath =
                VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE);
        assertEquals("Wrath consumes exactly two RNG draws in order", 2, wrath.size());
        assertEquals(2.9f, wrath.get(0)[0], EPS);
        assertEquals(3.1f, wrath.get(0)[1], EPS);
        assertEquals(0.95f, wrath.get(1)[0], EPS);
        assertEquals(1.05f, wrath.get(1)[1], EPS);

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.STANCE_AURA,
                VfxDrawGeometry.Kind.CALM_PARTICLE,
                VfxDrawGeometry.Kind.FLASH_ATK_IMG,
                VfxDrawGeometry.Kind.ICE_SHATTER,
                VfxDrawGeometry.Kind.FALLING_ICE,
                VfxDrawGeometry.Kind.DAMAGE_HEART,
                VfxDrawGeometry.Kind.SPOOKY_CHEST }) {
            assertTrue("no RNG is consumed for " + kind,
                    VfxDrawGeometry.randomRanges(kind).isEmpty());
        }
    }

    @Test
    public void playerHitboxRelativeXIsTrueOnlyForWrath() {
        assertTrue(VfxDrawGeometry.playerHitboxRelativeX(
                VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE) continue;
            assertFalse("only Wrath is player-hitbox-relative: " + kind,
                    VfxDrawGeometry.playerHitboxRelativeX(kind));
        }
    }

    @Test
    public void wrathStanceChangeNewPredicatesThrowOnNullKind() {
        try {
            VfxDrawGeometry.guardBlocks(null, 0f);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            VfxDrawGeometry.randomRanges(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            VfxDrawGeometry.playerHitboxRelativeX(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void wrathStanceChangeUsesStanceAuraGeometryAndIsAdditive() {
        // The native geometry is exactly StanceAuraEffect center-packed; the RNG scale factors and
        // player-relative x are applied by the renderer, so params here is the shared center-packed
        // shape (scale, scale).
        float pw = 64f;
        float ph = 48f;
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE,
                3.5f, -2.5f, 999f /* vY ignored */, 0.7f, 33f,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals(3.5f, p.x, EPS);
        assertEquals(-2.5f, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(0.7f, p.scaleX, EPS);
        assertEquals(0.7f, p.scaleY, EPS);
        assertEquals(33f, p.rotation, EPS);

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                3.5f, -2.5f, 0f, 0.7f, 33f, 0f, 0f, 1f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals("Wrath geometry equals the StanceAura shape", aura, p);

        assertTrue("Wrath installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawWithoutImage(
                VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
    }

    @Test
    public void guardFieldNameNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.guardFieldName(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void stanceChangeAbsorptionUsesTheFixedShapeCRectAndRotationOffset() {
        // Native StanceChangeAbsorptionParticle:
        //   sb.draw(ImageMaster.WOBBLY_ORB_VFX, x - 16f, y - 16f, 16f, 16f, 32f, 32f, <scales>,
        //           rotation - 200f, 0, 0, 32, 32, false, false) TWICE.
        // params returns the shared fixed rect with the BASE (scale, scale); the per-pass RNG scales
        // are applied by the renderer (drawPassRandomRanges).
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.7f;
        float rotation = 210f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION,
                x, y, 999f /* vY ignored */, scale, rotation, 7f /* durDiv2 ignored */,
                5f /* duration ignored */, 2f /* settingsScale ignored */,
                64f /* packedWidth ignored */, 48f /* packedHeight ignored */,
                1234f /* vX ignored */, 6f /* regionOffsetX ignored */, 10f /* regionOffsetY ignored */,
                0f, 0f, 1f);

        assertEquals(x - 16f, p.x, EPS);
        assertEquals(y - 16f, p.y, EPS);
        assertEquals(16f, p.originX, EPS);
        assertEquals(16f, p.originY, EPS);
        assertEquals(32f, p.width, EPS);
        assertEquals(32f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("the rotation is offset by -200f", rotation - 200f, p.rotation, EPS);

        // A different packed size / vY / region offsets produce byte-identical geometry.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION,
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals(p, q);

        // Host-neutral constants match the native hardcoded rect.
        assertEquals(16f, VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_OFFSET, EPS);
        assertEquals(16f, VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_ORIGIN, EPS);
        assertEquals(32f, VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SIZE, EPS);
        assertEquals(-200f, VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_ROTATION_OFFSET, EPS);
        assertEquals(0, VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SRC_X);
        assertEquals(0, VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SRC_Y);
        assertEquals(32, VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SRC_W);
        assertEquals(32, VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SRC_H);

        assertTrue("STANCE_CHANGE_ABSORPTION installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorX(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxDrawGeometry.playerHitboxRelativeX(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawWithoutImage(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        // It is a MULTI-DRAW kind, so the single-draw randomRanges list stays empty.
        assertTrue(VfxDrawGeometry.randomRanges(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION).isEmpty());
    }

    @Test
    public void drawPassRandomRangesIsTheOrderedTwoPassSequenceForAbsorption() {
        java.util.List<java.util.List<float[]>> passes = VfxDrawGeometry.drawPassRandomRanges(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION);

        assertEquals("Absorption draws in exactly two passes", 2, passes.size());
        // Pass 0: scaleX then scaleY, both (0.5f, 2.0f).
        assertEquals(2, passes.get(0).size());
        assertEquals(0.5f, passes.get(0).get(0)[0], EPS);
        assertEquals(2.0f, passes.get(0).get(0)[1], EPS);
        assertEquals(0.5f, passes.get(0).get(1)[0], EPS);
        assertEquals(2.0f, passes.get(0).get(1)[1], EPS);
        // Pass 1: scaleX then scaleY, both (0.6f, 2.5f).
        assertEquals(2, passes.get(1).size());
        assertEquals(0.6f, passes.get(1).get(0)[0], EPS);
        assertEquals(2.5f, passes.get(1).get(0)[1], EPS);
        assertEquals(0.6f, passes.get(1).get(1)[0], EPS);
        assertEquals(2.5f, passes.get(1).get(1)[1], EPS);

        // Every other kind has an EMPTY outer list (WRATH keeps its single-draw randomRanges).
        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION) continue;
            assertTrue("no multi-pass RNG for " + kind,
                    VfxDrawGeometry.drawPassRandomRanges(kind).isEmpty());
        }
        assertTrue("Wrath has no multi-pass ranges (single-draw randomRanges)",
                VfxDrawGeometry.drawPassRandomRanges(
                        VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE).isEmpty());
    }

    @Test
    public void drawPassRandomRangesNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.drawPassRandomRanges(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void waterSplashUsesTheCenterPackedGeometryWithTheAnisotropicScaleY() {
        // Native WaterSplashParticleEffect:
        //   setColor(color); sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale * 0.54f,
        //           rotation);  // NO setBlendFunction
        // The draw position/origin/size are the shared center-packed ones, scaleX is scale, and the
        // NEW pure rule is the anisotropic scaleY = scale * 0.54f (the renderer passes the
        // scaleYMultiplier tail scalar 0.54f for WATER_SPLASH).
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.7f;
        float rotation = 51f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WATER_SPLASH,
                x, y, 999f /* vY ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                pw, ph, 1234f /* vX ignored */, 6f /* regionOffsetX ignored */,
                10f /* regionOffsetY ignored */, 0f, 0f,
                VfxDrawGeometry.WATER_SPLASH_SCALE_Y_MULTIPLIER);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals("scaleX is the uniform scale", scale, p.scaleX, EPS);
        assertEquals("scaleY is scale * 0.54f", scale * 0.54f, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        assertEquals(0.54f, VfxDrawGeometry.WATER_SPLASH_SCALE_Y_MULTIPLIER, EPS);
        assertFalse("WATER_SPLASH never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertNull(VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertFalse(VfxDrawGeometry.playerHitboxRelativeX(VfxDrawGeometry.Kind.WATER_SPLASH));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawWithoutImage(
                VfxDrawGeometry.Kind.WATER_SPLASH));
        assertTrue(VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.WATER_SPLASH).isEmpty());
        assertTrue(VfxDrawGeometry.drawPassRandomRanges(
                VfxDrawGeometry.Kind.WATER_SPLASH).isEmpty());
    }

    @Test
    public void buffParticleUsesHalfPackedPositionAndRegionOffsetOriginAdditively() {
        // Native BuffParticleEffect:
        //   setBlendFunction(770, 1); setColor(color);
        //   sb.draw(img, x - pw/2f, y - ph/2f, img.offsetX, img.offsetY, pw, ph, scale, scale,
        //           rotation);
        //   setBlendFunction(770, 771);
        // The NEW pure rule: the draw POSITION is offset by half the packed footprint and the ORIGIN
        // is the region's own (offsetX, offsetY) rather than packed/2.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.7f;
        float rotation = 51f;
        float regionOffsetX = 6f;
        float regionOffsetY = 10f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.BUFF_PARTICLE,
                x, y, 999f /* vY ignored (update-only) */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                pw, ph, 1234f /* vX ignored */, regionOffsetX, regionOffsetY,
                0f, 0f, 0.54f /* ignored: BUFF_PARTICLE keeps scale, scale */);

        assertEquals("the position is offset by half the packed width", x - pw / 2f, p.x, EPS);
        assertEquals("the position is offset by half the packed height", y - ph / 2f, p.y, EPS);
        assertEquals("the origin X is the region's own offsetX", regionOffsetX, p.originX, EPS);
        assertEquals("the origin Y is the region's own offsetY", regionOffsetY, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals("BUFF_PARTICLE is uniform-scaled (ignores the scaleYMultiplier)",
                scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        assertTrue("BUFF_PARTICLE installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.BUFF_PARTICLE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.BUFF_PARTICLE));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.BUFF_PARTICLE));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.BUFF_PARTICLE));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.BUFF_PARTICLE));
        assertNull(VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.BUFF_PARTICLE));
        assertFalse(VfxDrawGeometry.playerHitboxRelativeX(VfxDrawGeometry.Kind.BUFF_PARTICLE));
        assertTrue(VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.BUFF_PARTICLE).isEmpty());
        assertTrue(VfxDrawGeometry.drawPassRandomRanges(
                VfxDrawGeometry.Kind.BUFF_PARTICLE).isEmpty());
    }

    @Test
    public void preexistingCenterPackedKindIsUnchangedByTheScaleYMultiplierTailParam() {
        // REGRESSION: the new trailing scaleYMultiplier parameter must default to 1f for every
        // pre-existing kind (the renderer passes 1f for all kinds except WATER_SPLASH), so a
        // previously center-packed kind's geometry is byte-identical to before the parameter existed:
        // scaleX == scaleY == scale.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals("scaleX is scale", scale, p.scaleX, EPS);
        assertEquals("scaleY stays scale with the 1f default", scale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        // The same call also matches every other pre-existing center-packed kind at 1f.
        VfxDrawGeometry.Params wrath = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLYING_SPIKE,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f, 1f);
        assertEquals("a reused center-packed kind is unchanged at the 1f default", p, wrath);
    }

    @Test
    public void bottomFogMatchesTheStanceAuraCenterPackedGeometryAmbiently() {
        // Native BottomFogEffect: setColor(color); [in-place flipX/flipY mirror];
        //   sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation);  -- NO
        // setBlendFunction, so it is ambient center-packed and geometry-identical to STANCE_AURA
        // (the per-instance mirror is resolved by the renderer as a UV swap). NO new formula.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, pw, ph, 1234f, 0f, 0f, 0f, 0f, 1f);
        VfxDrawGeometry.Params fog = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.BOTTOM_FOG,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f,
                pw, ph, 1234f /* vX ignored */, 0f, 0f, 0f, 0f, 1f);

        assertEquals("BOTTOM_FOG matches the STANCE_AURA center-packed geometry", aura, fog);
        assertFalse("BOTTOM_FOG never calls setBlendFunction (ambient)",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertNull(VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.BOTTOM_FOG));
        assertEquals(1f, VfxDrawGeometry.uniformScaleMultiplier(
                VfxDrawGeometry.Kind.BOTTOM_FOG, 1.333f), EPS);
        assertTrue(VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.BOTTOM_FOG).isEmpty());
        assertTrue(VfxDrawGeometry.drawPassRandomRanges(VfxDrawGeometry.Kind.BOTTOM_FOG).isEmpty());
    }

    @Test
    public void giantFireUsesTheUniformSettingsScaleOnBothAxesAdditively() {
        // Native GiantFireEffect: setColor(color); setBlendFunction(770, 1); [flipX mirror];
        //   sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale * Settings.scale, scale * Settings.scale,
        //           rotation); setBlendFunction(770, 771).
        // ADDITIVE center-packed with a per-instance horizontal flipX mirror (no flipY) and a NEW
        // pure uniform-scale rule: BOTH axes are scale * settingsScale. A non-1.0 settingsScale is
        // used to guard the rule.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.7f;
        float rotation = 51f;
        float settingsScale = 1.333f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.GIANT_FIRE,
                x, y, 999f /* vY ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, settingsScale,
                pw, ph, 1234f /* vX ignored */, 6f /* regionOffsetX ignored */,
                10f /* regionOffsetY ignored */, 0f, 0f, 1f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(pw / 2f, p.originX, EPS);
        assertEquals(ph / 2f, p.originY, EPS);
        assertEquals(pw, p.width, EPS);
        assertEquals(ph, p.height, EPS);
        assertEquals("scaleX is scale * settingsScale", scale * settingsScale, p.scaleX, EPS);
        assertEquals("scaleY is scale * settingsScale too", scale * settingsScale, p.scaleY, EPS);
        assertEquals(rotation, p.rotation, EPS);

        assertTrue("GIANT_FIRE installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertTrue(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertFalse("GIANT_FIRE has no flipY field, so it is not a vertical mirror kind",
                VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertNull(VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertFalse(VfxDrawGeometry.playerHitboxRelativeX(VfxDrawGeometry.Kind.GIANT_FIRE));
        assertTrue(VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.GIANT_FIRE).isEmpty());
        assertTrue(VfxDrawGeometry.drawPassRandomRanges(VfxDrawGeometry.Kind.GIANT_FIRE).isEmpty());
    }

    @Test
    public void uniformScaleMultiplierIsTheSettingsScaleOnlyForGiantFire() {
        assertEquals("GIANT_FIRE uses the supplied settings scale",
                1.333f, VfxDrawGeometry.uniformScaleMultiplier(
                        VfxDrawGeometry.Kind.GIANT_FIRE, 1.333f), EPS);
        assertEquals("GIANT_FIRE uses the supplied settings scale",
                0.5f, VfxDrawGeometry.uniformScaleMultiplier(
                        VfxDrawGeometry.Kind.GIANT_FIRE, 0.5f), EPS);

        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.GIANT_FIRE) continue;
            assertEquals("the uniform multiplier is 1f for " + kind,
                    1f, VfxDrawGeometry.uniformScaleMultiplier(kind, 1.333f), EPS);
            assertEquals("the uniform multiplier is 1f for " + kind,
                    1f, VfxDrawGeometry.uniformScaleMultiplier(kind, 0.5f), EPS);
        }
    }

    @Test
    public void uniformScaleMultiplierNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.uniformScaleMultiplier(null, 1f);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void preexistingCenterPackedAndWaterSplashAreUnchangedByTheUniformMultiplier() {
        // REGRESSION: uniformScaleMultiplier returns 1f for every kind except GIANT_FIRE, so a
        // pre-existing center-packed kind (and WATER_SPLASH's anisotropic 0.54f scaleY tail) is
        // byte-identical to before the uniform rule existed.
        float pw = 64f;
        float ph = 48f;
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;
        float settingsScale = 1.333f;

        VfxDrawGeometry.Params aura = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.STANCE_AURA,
                x, y, 999f, scale, rotation, 7f, 5f, settingsScale, pw, ph, 0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals("scaleX stays scale for a pre-existing center-packed kind",
                scale, aura.scaleX, EPS);
        assertEquals("scaleY stays scale for a pre-existing center-packed kind",
                scale, aura.scaleY, EPS);
        assertEquals(1f, VfxDrawGeometry.uniformScaleMultiplier(
                VfxDrawGeometry.Kind.STANCE_AURA, settingsScale), EPS);

        VfxDrawGeometry.Params splash = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.WATER_SPLASH,
                x, y, 999f, scale, rotation, 7f, 5f, settingsScale, pw, ph, 0f, 0f, 0f, 0f, 0f,
                VfxDrawGeometry.WATER_SPLASH_SCALE_Y_MULTIPLIER);
        assertEquals("WATER_SPLASH scaleX is still scale", scale, splash.scaleX, EPS);
        assertEquals("WATER_SPLASH scaleY is still scale * 0.54f",
                scale * VfxDrawGeometry.WATER_SPLASH_SCALE_Y_MULTIPLIER, splash.scaleY, EPS);
    }

    @Test
    public void torchHeadFireReusesTheGlowyFireEyesRectWithTheAsymmetricXScale() {
        // Native TorchHeadFireEffect: setBlendFunction(770,1); setColor(color);
        //   sb.draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f, scale * 1.2f, scale, 0f,
        //           0, 0, 128, 128, flippedX, false); setBlendFunction(770,771).
        // Shape-C fixed rect reusing the GlowyFireEyesEffect constants exactly, ADDITIVE, with a
        // hardcoded zero rotation; the ONE new pure rule is the asymmetric X scale.
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.TORCH_HEAD_FIRE,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, 64f, 48f,
                1234f /* vX ignored */, 6f /* regionOffsetX ignored */, 10f /* regionOffsetY ignored */,
                0f, 0f, 1f);

        assertEquals(x - 64f, p.x, EPS);
        assertEquals(y - 64f, p.y, EPS);
        assertEquals(64f, p.originX, EPS);
        assertEquals(64f, p.originY, EPS);
        assertEquals(128f, p.width, EPS);
        assertEquals(128f, p.height, EPS);
        assertEquals("scaleX is scale * 1.2f", scale * 1.2f, p.scaleX, EPS);
        assertEquals("scaleY stays scale (asymmetric)", scale, p.scaleY, EPS);
        assertEquals("TorchHeadFire hardcodes rotation 0f", 0f, p.rotation, EPS);

        // The rect constants are literally shared with GLOWY_FIRE_EYES.
        assertEquals(64f, VfxDrawGeometry.GLOWY_FIRE_EYES_OFFSET, EPS);
        assertEquals(64f, VfxDrawGeometry.GLOWY_FIRE_EYES_ORIGIN, EPS);
        assertEquals(128f, VfxDrawGeometry.GLOWY_FIRE_EYES_SIZE, EPS);
        assertEquals(128, VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_W);
        assertEquals(128, VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_H);
        assertEquals(1.2f, VfxDrawGeometry.TORCH_HEAD_FIRE_SCALE_X_MULTIPLIER, EPS);

        assertTrue("TORCH_HEAD_FIRE installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertTrue("the flippedX field drives the horizontal flip",
                VfxDrawGeometry.usesTexturedFlipX(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertNull(VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertFalse(VfxDrawGeometry.playerHitboxRelativeX(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));
        assertTrue(VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE).isEmpty());
        assertTrue(VfxDrawGeometry.drawPassRandomRanges(
                VfxDrawGeometry.Kind.TORCH_HEAD_FIRE).isEmpty());

        // A different vY/packed size still produces the same asymmetric rect.
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.TORCH_HEAD_FIRE,
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals(p, q);

        // The X scale is exactly 1.2x the GlowyFireEyes uniform scale, and Y matches it.
        VfxDrawGeometry.Params gfe = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.GLOWY_FIRE_EYES,
                x, y, 999f, scale, rotation, 7f, 5f, 2f, 64f, 48f, 1234f, 0f, 0f, 0f, 0f, 1f);
        assertEquals(gfe.scaleX * 1.2f, p.scaleX, EPS);
        assertEquals(gfe.scaleY, p.scaleY, EPS);
    }

    @Test
    public void usesTexturedFlipXIsTrueOnlyForTheFlippedXFieldKinds() {
        assertTrue(VfxDrawGeometry.usesTexturedFlipX(VfxDrawGeometry.Kind.GLOWY_FIRE_EYES));
        assertTrue(VfxDrawGeometry.usesTexturedFlipX(VfxDrawGeometry.Kind.TORCH_HEAD_FIRE));

        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES
                    || kind == VfxDrawGeometry.Kind.TORCH_HEAD_FIRE) {
                continue;
            }
            assertFalse("no flippedX field for " + kind,
                    VfxDrawGeometry.usesTexturedFlipX(kind));
        }
    }

    @Test
    public void usesTexturedFlipXNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.usesTexturedFlipX(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void cardTrailUsesAFixedOriginAndSizeIndependentOfThePackedRegion() {
        // Native CardTrailEffect: setBlendFunction(770,1); setColor(color);
        //   sb.draw(img, x, y, 6f, 6f, 12f, 12f, scale, scale, 0f);
        //   setBlendFunction(770,771).
        // The img (AtlasRegion) path with ONE new pure rule: a fixed ORIGIN (6f, 6f) and fixed SIZE
        // (12f, 12f) INDEPENDENT of the region's packed size; rotation hardcoded 0f; ADDITIVE.
        float x = 12.5f;
        float y = 33.25f;
        float scale = 0.6f;
        float rotation = 45f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.CARD_TRAIL,
                x, y, 999f /* vY ignored */, scale, rotation, 7f, 5f, 2f, 1f, 1f,
                1234f /* vX ignored */, 6f /* regionOffsetX ignored */, 10f /* regionOffsetY ignored */,
                0f, 0f, 1f);

        assertEquals(x, p.x, EPS);
        assertEquals(y, p.y, EPS);
        assertEquals(6f, p.originX, EPS);
        assertEquals(6f, p.originY, EPS);
        assertEquals(12f, p.width, EPS);
        assertEquals(12f, p.height, EPS);
        assertEquals(scale, p.scaleX, EPS);
        assertEquals(scale, p.scaleY, EPS);
        assertEquals("CardTrail hardcodes rotation 0f", 0f, p.rotation, EPS);
        assertEquals(6f, VfxDrawGeometry.CARD_TRAIL_ORIGIN, EPS);
        assertEquals(12f, VfxDrawGeometry.CARD_TRAIL_SIZE, EPS);

        // The fixed rect is INDEPENDENT of the region's packed size: a large packed region must not
        // change the origin/size (unlike the center-packed kinds).
        VfxDrawGeometry.Params q = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.CARD_TRAIL,
                x, y, -12345f, scale, rotation, 0f, 0f, 1f, 512f, 384f,
                0f, 0f, 0f, 0f, 0f, 1f);
        assertEquals(p, q);

        assertTrue("CARD_TRAIL installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertFalse(VfxDrawGeometry.usesTexturedFlipX(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertNull(VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertFalse(VfxDrawGeometry.playerHitboxRelativeX(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertEquals(1f, VfxDrawGeometry.uniformScaleMultiplier(
                VfxDrawGeometry.Kind.CARD_TRAIL, 2f), EPS);
        assertTrue(VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.CARD_TRAIL).isEmpty());
        assertTrue(VfxDrawGeometry.drawPassRandomRanges(
                VfxDrawGeometry.Kind.CARD_TRAIL).isEmpty());
    }

    @Test
    public void cardTrailKindForFailsOpenForNearMisses() {
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.CardTrailEffect2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.CardTrailEffect$Sub"));
        assertNull(VfxDrawGeometry.kindFor("CardTrailEffect"));
        assertNull(VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.CardTrail"));
    }

    // --- NRO-04 B01 FlyingOrbEffect: the first VARIABLE-LENGTH MULTI-DRAW kind ---

    @Test
    public void flyingOrbKindForFailsOpenForNearMisses() {
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlyingOrbEffect2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlyingOrbEffect$Sub"));
        assertNull(VfxDrawGeometry.kindFor("FlyingOrbEffect"));
        assertNull(VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.FlyingOrbEffect"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlyingOrb"));
    }

    @Test
    public void variableLengthMultiDrawIsTrueOnlyForFlyingOrb() {
        assertTrue("FlyingOrbEffect is the first variable-length multi-draw kind",
                VfxDrawGeometry.variableLengthMultiDraw(VfxDrawGeometry.Kind.FLYING_ORB));

        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.FLYING_ORB) continue;
            assertFalse("no variable-length draw for " + kind,
                    VfxDrawGeometry.variableLengthMultiDraw(kind));
        }
        // The fixed-length multi-draw F25 kind is NOT variable-length (compile-time pass count).
        assertFalse(VfxDrawGeometry.variableLengthMultiDraw(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
    }

    @Test
    public void variableLengthMultiDrawNullKindThrowsIllegalArgument() {
        try {
            VfxDrawGeometry.variableLengthMultiDraw(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void flyingOrbDecayConstantsMatchTheNativeBytecode() {
        // Native bytecode (see the class Javadoc): `float s = Settings.scale * 1.5f;` then, after each
        // DRAWN point, `s *= 0.975f;`.
        assertEquals(1.5f, VfxDrawGeometry.FLYING_ORB_START_SCALE_MULTIPLIER, EPS);
        assertEquals(1.5f, VfxDrawGeometry.flyingOrbStartScaleMultiplier(), EPS);
        assertEquals(0.975f, VfxDrawGeometry.FLYING_ORB_SCALE_DECAY_PER_DRAW, EPS);
        assertEquals(0.975f, VfxDrawGeometry.flyingOrbScaleDecayPerDraw(), EPS);
    }

    @Test
    public void flyingOrbGuardPredicatesAreBooleanOnlyForFlyingOrb() {
        assertTrue(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.FLYING_ORB));
        assertTrue(VfxDrawGeometry.guardIsBoolean(VfxDrawGeometry.Kind.FLYING_ORB));
        assertEquals("isDone", VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.FLYING_ORB));

        // The boolean threshold blocks iff the flag is true.
        assertTrue("isDone == true blocks",
                VfxDrawGeometry.guardBlocksBoolean(VfxDrawGeometry.Kind.FLYING_ORB, true));
        assertFalse("isDone == false draws",
                VfxDrawGeometry.guardBlocksBoolean(VfxDrawGeometry.Kind.FLYING_ORB, false));

        // Every other kind is neither boolean-guarded nor blocked by the boolean predicate — including
        // the float-guarded kinds, whose guard is the float guardBlocks threshold.
        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.FLYING_ORB) continue;
            assertFalse("no boolean guard for " + kind, VfxDrawGeometry.guardIsBoolean(kind));
            assertFalse("no boolean block for " + kind,
                    VfxDrawGeometry.guardBlocksBoolean(kind, true));
            assertFalse("no boolean block for " + kind,
                    VfxDrawGeometry.guardBlocksBoolean(kind, false));
        }

        assertTrue("FLYING_ORB installs the additive blend natively",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLYING_ORB));
    }

    @Test
    public void flyingOrbGuardPredicatesThrowOnNullKind() {
        try {
            VfxDrawGeometry.guardIsBoolean(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            VfxDrawGeometry.guardBlocksBoolean(null, true);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // --- NRO-04 B02 FlickCoinEffect: single-draw img path, cX/cY/yOffset position, anisotropic ---

    @Test
    public void flickCoinKindForFailsOpenForNearMisses() {
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlickCoinEffect2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.FlickCoinEffect$Sub"));
        assertNull(VfxDrawGeometry.kindFor("FlickCoinEffect"));
        assertNull(VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.FlickCoinEffect"));
        assertNull(VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.combat.FlickCoin"));
    }

    @Test
    public void flickCoinUsesIntegerHalfPositionFloatHalfOriginAndAnisotropicScale() {
        // Native FlickCoinEffect (verified bytecode): ADDITIVE; cX/cY/yOffset position fields (NO
        // x/y); the POSITION half uses INTEGER division of the packed size and the ORIGIN half uses
        // FLOAT division (differing by 0.5 for an odd region); the scale is ANISOTROPIC
        // (scaleX = scale*0.7f, scaleY = scale*0.4f) with the effect's own rotation.
        int pw = 81;
        int ph = 81;
        float cX = 123.5f;
        float cY = 456.75f;
        float yOffset = -7.25f;
        float scale = 1.3f;
        float rotation = 41f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLICK_COIN,
                cX, cY, yOffset /* consumed as the yOffset */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, 2f /* settingsScale ignored */,
                pw, ph, 1234f /* vX ignored */, 6f /* regionOffsetX ignored */,
                10f /* regionOffsetY ignored */, 0f, 0f, 1f);

        // POSITION: integer half (81/2 == 40), NOT float half (40.5).
        assertEquals("position uses the integer half of the packed size",
                cX - 40f, p.x, EPS);
        assertEquals("position adds yOffset to the integer half",
                cY - 40f + yOffset, p.y, EPS);
        // ORIGIN: float half (40.5).
        assertEquals("origin uses the float half of the packed width", 40.5f, p.originX, EPS);
        assertEquals("origin uses the float half of the packed height", 40.5f, p.originY, EPS);
        assertEquals((float) pw, p.width, EPS);
        assertEquals((float) ph, p.height, EPS);
        assertEquals("scaleX is scale * 0.7f",
                scale * VfxDrawGeometry.FLICK_COIN_SCALE_X, p.scaleX, EPS);
        assertEquals("scaleY is scale * 0.4f",
                scale * VfxDrawGeometry.FLICK_COIN_SCALE_Y, p.scaleY, EPS);
        assertEquals("FlickCoin consumes the rotation field", rotation, p.rotation, EPS);

        assertEquals(0.7f, VfxDrawGeometry.FLICK_COIN_SCALE_X, EPS);
        assertEquals(0.4f, VfxDrawGeometry.FLICK_COIN_SCALE_Y, EPS);
        assertTrue(VfxDrawGeometry.flickCoinUsesAnisotropicScale(
                VfxDrawGeometry.Kind.FLICK_COIN));
        assertEquals(0.7f, VfxDrawGeometry.flickCoinScaleXMultiplier(), EPS);
        assertEquals(0.4f, VfxDrawGeometry.flickCoinScaleYMultiplier(), EPS);

        assertTrue("FLICK_COIN installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.FLICK_COIN));
        // It joins NO guard / flip / mirror / RNG capability.
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.guardIsBoolean(VfxDrawGeometry.Kind.FLICK_COIN));
        assertNull(VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawWithoutImage(
                VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.usesTexturedFlipX(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.playerHitboxRelativeX(VfxDrawGeometry.Kind.FLICK_COIN));
        assertFalse(VfxDrawGeometry.variableLengthMultiDraw(VfxDrawGeometry.Kind.FLICK_COIN));
        assertTrue(VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.FLICK_COIN).isEmpty());
        assertTrue(VfxDrawGeometry.drawPassRandomRanges(
                VfxDrawGeometry.Kind.FLICK_COIN).isEmpty());
    }

    @Test
    public void flickCoinAnisotropicScaleIsTrueOnlyForFlickCoinAndNullThrows() {
        assertTrue(VfxDrawGeometry.flickCoinUsesAnisotropicScale(
                VfxDrawGeometry.Kind.FLICK_COIN));
        for (VfxDrawGeometry.Kind kind : VfxDrawGeometry.Kind.values()) {
            if (kind == VfxDrawGeometry.Kind.FLICK_COIN) continue;
            assertFalse("only FlickCoin uses the anisotropic anisotropic-scale predicate: " + kind,
                    VfxDrawGeometry.flickCoinUsesAnisotropicScale(kind));
        }
        try {
            VfxDrawGeometry.flickCoinUsesAnisotropicScale(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // --- NRO-04 B03 HealPanelEffect: bare static Texture + fixed 64x64 rect, panel-space position ---

    @Test
    public void healPanelKindForFailsOpenForNearMisses() {
        assertSame(VfxDrawGeometry.Kind.HEAL_PANEL,
                VfxDrawGeometry.kindFor(VfxClaimPolicy.HEAL_PANEL));
        assertSame(VfxDrawGeometry.Kind.HEAL_PANEL,
                VfxDrawGeometry.kindFor(
                        "com.megacrit.cardcrawl.vfx.combat.HealPanelEffect"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.HealPanelEffect2"));
        assertNull(VfxDrawGeometry.kindFor(
                "com.megacrit.cardcrawl.vfx.combat.HealPanelEffect$Sub"));
        assertNull(VfxDrawGeometry.kindFor("HealPanelEffect"));
        assertNull(VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.HealPanelEffect"));
        assertNull(VfxDrawGeometry.kindFor("com.megacrit.cardcrawl.vfx.combat.HealPanel"));
    }

    @Test
    public void healPanelUsesTheFixedRectAndPanelSpacePosition() {
        // Native HealPanelEffect (verified bytecode): ADDITIVE; the STATIC Texture img (no instance
        // img field); a fixed shape-C rect (src 0,0,64,64, origin 32,32, size 64,64); a PANEL-SPACE
        // position x = x - 32f + 32f*Settings.scale and y = Settings.HEIGHT - 32f*Settings.scale - 32f;
        // uniform scale; the field rotation.
        float x = 100f;
        float scale = 0.8f;
        float rotation = 37f;
        float settingsScale = 1.5f;
        float settingsHeight = 1080f;

        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.HEAL_PANEL,
                x, 222f /* y ignored (panel-space) */, 999f /* vY ignored */, scale, rotation,
                7f /* durDiv2 ignored */, 5f /* duration ignored */, settingsScale,
                10f /* packedWidth ignored */, 20f /* packedHeight ignored */,
                1234f /* vX ignored */, 6f /* regionOffsetX ignored */, 7f /* regionOffsetY ignored */,
                0f, 0f, 1f, settingsHeight);

        assertEquals("x uses the Settings.scale term",
                x - VfxDrawGeometry.HEAL_PANEL_X_OFFSET
                        + VfxDrawGeometry.HEAL_PANEL_X_OFFSET * settingsScale,
                p.x, EPS);
        assertEquals("y is anchored to Settings.HEIGHT with the Settings.scale term and fixed offset",
                settingsHeight - VfxDrawGeometry.HEAL_PANEL_Y_OFFSET * settingsScale
                        - VfxDrawGeometry.HEAL_PANEL_Y_OFFSET,
                p.y, EPS);
        assertEquals("origin is the fixed 32", 32f, p.originX, EPS);
        assertEquals("origin is the fixed 32", 32f, p.originY, EPS);
        assertEquals("size is the fixed 64", 64f, p.width, EPS);
        assertEquals("size is the fixed 64", 64f, p.height, EPS);
        assertEquals("uniform scale on X", scale, p.scaleX, EPS);
        assertEquals("uniform scale on Y", scale, p.scaleY, EPS);
        assertEquals("HealPanel consumes the rotation field", rotation, p.rotation, EPS);

        // The named constants mirror the native bytecode.
        assertEquals(0, VfxDrawGeometry.HEAL_PANEL_SRC_X);
        assertEquals(0, VfxDrawGeometry.HEAL_PANEL_SRC_Y);
        assertEquals(64, VfxDrawGeometry.HEAL_PANEL_SRC_W);
        assertEquals(64, VfxDrawGeometry.HEAL_PANEL_SRC_H);
        assertEquals(32f, VfxDrawGeometry.HEAL_PANEL_ORIGIN, EPS);
        assertEquals(64f, VfxDrawGeometry.HEAL_PANEL_SIZE, EPS);
        assertEquals(32f, VfxDrawGeometry.HEAL_PANEL_X_OFFSET, EPS);
        assertEquals(32f, VfxDrawGeometry.HEAL_PANEL_Y_OFFSET, EPS);

        assertTrue("HEAL_PANEL installs the additive blend",
                VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse("HEAL_PANEL uses its own color, not the white-alpha rule",
                VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.HEAL_PANEL));
        // It joins NO guard / flip / mirror / RNG / variable-length capability.
        assertFalse(VfxDrawGeometry.nativeSkipsDrawByGuard(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.guardIsBoolean(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertNull(VfxDrawGeometry.guardFieldName(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.nativeSkipsDrawWithoutImage(
                VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.usesInstanceFlipX(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.usesInstanceFlipY(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.usesTexturedFlipX(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorX(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.usesInstanceMirrorY(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.playerHitboxRelativeX(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.variableLengthMultiDraw(VfxDrawGeometry.Kind.HEAL_PANEL));
        assertFalse(VfxDrawGeometry.flickCoinUsesAnisotropicScale(
                VfxDrawGeometry.Kind.HEAL_PANEL));
        assertTrue(VfxDrawGeometry.randomRanges(VfxDrawGeometry.Kind.HEAL_PANEL).isEmpty());
        assertTrue(VfxDrawGeometry.drawPassRandomRanges(
                VfxDrawGeometry.Kind.HEAL_PANEL).isEmpty());
    }
}
