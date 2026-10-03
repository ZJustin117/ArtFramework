package artframework.sts1.assets;

import artframework.sts1.render.VfxArtRenderer;
import artframework.sts1.render.VfxClaimPolicy;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * F2c production-binding coverage: the same idempotent entry point the mod bootstrap calls
 * ({@link Sts1HostAssets#installVfxRenderer()}) must install the real {@code Sts1VfxArtRenderer}
 * so the default-off claim seam reports ready for the supported {@code vfx-stance-aura} FQNs,
 * and must be reversible back to the inert default. No GL/game classes are needed, so this runs
 * headless.
 */
public class Sts1VfxRendererBindingTest {

    @After
    public void tearDown() {
        Sts1HostAssets.resetVfxRendererForTests();
    }

    @Test
    public void installVfxRendererBindsTheRealRendererForSupportedClassesOnly() {
        // Inert default before the production install path runs.
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));

        Sts1HostAssets.installVfxRenderer();
        assertTrue(Sts1HostAssets.isVfxRendererInstalled());

        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.CALM_PARTICLE_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_LIGHT_FLARE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLASH_ATK_IMG));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_LIGHT_FLARE_M));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_LIGHT_FLARE_L));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FIRE_BURST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.RED_FIRE_BURST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SMOKE_BLUR));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.CEILING_DUST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.NEMESIS_FIRE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SHIELD_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DEBUFF_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.GHOSTLY_WEAK_FIRE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.GENERIC_SMOKE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.EXHAUST_BLUR));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.ICE_SHATTER));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WEB_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.ENTANGLE_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.BLOCK_IMPACT_LINE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.EXHAUST_PILE_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.UNKNOWN_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLAME_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_BLUR));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_LINE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DARK_ORB_PASSIVE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WARNING_SIGN));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STUN_STAR));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FALLING_DUST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLAME_BALL));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SHINE_LINES));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_M));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_S));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_DUST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.GLOWY_FIRE_EYES));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLYING_SPIKE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.CONE_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FALLING_ICE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_HEART));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SPOOKY_CHEST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SPOOKIER_CHEST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DEATH_SCREEN_FLOATY));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WRATH_STANCE_CHANGE));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.stance.StanceChangeAbsorptionParticle"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WATER_SPLASH));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.WaterSplashParticleEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.BUFF_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.BuffParticleEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.BOTTOM_FOG));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.BottomFogEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.GIANT_FIRE));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.GiantFireEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_HEAD_FIRE));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.TorchHeadFireEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.CARD_TRAIL));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.CardTrailEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLYING_ORB));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.FlyingOrbEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLICK_COIN));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.FlickCoinEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.HEAL_PANEL));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.HealPanelEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.PING_HP));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.PingHpEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.REWARD_GLOW));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.RewardGlowEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.MAP_CIRCLE));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.MapCircleEffect"));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SPOTLIGHT));
        assertTrue(VfxArtRenderer.isReady(
                "com.megacrit.cardcrawl.vfx.SpotlightEffect"));

        // A near-miss stays not-ready.
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FIRE_BURST + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SMOKE_BLUR + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.NEMESIS_FIRE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SHIELD_PARTICLE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SHIELD_PARTICLE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DEBUFF_PARTICLE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DEBUFF_PARTICLE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.GHOSTLY_WEAK_FIRE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.GENERIC_SMOKE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.EXHAUST_BLUR + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.ICE_SHATTER + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.ICE_SHATTER + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WEB_PARTICLE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WEB_PARTICLE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.ENTANGLE_EFFECT + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.ENTANGLE_EFFECT + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.BLOCK_IMPACT_LINE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.BLOCK_IMPACT_LINE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.EXHAUST_PILE_PARTICLE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.EXHAUST_PILE_PARTICLE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.UNKNOWN_PARTICLE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.UNKNOWN_PARTICLE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FLAME_PARTICLE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FLAME_PARTICLE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_BLUR + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_BLUR + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_LINE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_LINE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DARK_ORB_PASSIVE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DARK_ORB_PASSIVE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WARNING_SIGN + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WARNING_SIGN + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STUN_STAR + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STUN_STAR + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FALLING_DUST + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FALLING_DUST + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_EFFECT + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_EFFECT + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FLAME_BALL + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FLAME_BALL + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SHINE_LINES + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SHINE_LINES + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_M + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_M + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_S + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_S + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_DUST + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_DUST + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FLYING_SPIKE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FLYING_SPIKE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.CONE_EFFECT + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.CONE_EFFECT + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FALLING_ICE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FALLING_ICE + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_HEART + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_HEART + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SPOOKY_CHEST + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SPOOKY_CHEST + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SPOOKIER_CHEST + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SPOOKIER_CHEST + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DEATH_SCREEN_FLOATY + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DEATH_SCREEN_FLOATY + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WRATH_STANCE_CHANGE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WRATH_STANCE_CHANGE + "2"));
        assertFalse(VfxArtRenderer.isReady("WrathStanceChangeParticle"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION + "2"));
        assertFalse(VfxArtRenderer.isReady("StanceChangeAbsorptionParticle"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WATER_SPLASH + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WATER_SPLASH + "2"));
        assertFalse(VfxArtRenderer.isReady("WaterSplashParticleEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.BUFF_PARTICLE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.BUFF_PARTICLE + "2"));
        assertFalse(VfxArtRenderer.isReady("BuffParticleEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.BOTTOM_FOG + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.BOTTOM_FOG + "2"));
        assertFalse(VfxArtRenderer.isReady("BottomFogEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.GIANT_FIRE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.GIANT_FIRE + "2"));
        assertFalse(VfxArtRenderer.isReady("GiantFireEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_HEAD_FIRE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_HEAD_FIRE + "2"));
        assertFalse(VfxArtRenderer.isReady("TorchHeadFireEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.CARD_TRAIL + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.CARD_TRAIL + "2"));
        assertFalse(VfxArtRenderer.isReady("CardTrailEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FLYING_ORB + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FLYING_ORB + "2"));
        assertFalse(VfxArtRenderer.isReady("FlyingOrbEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.HEAL_PANEL + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.HEAL_PANEL + "2"));
        assertFalse(VfxArtRenderer.isReady("HealPanelEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.PING_HP + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.PING_HP + "2"));
        assertFalse(VfxArtRenderer.isReady("PingHpEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.REWARD_GLOW + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.REWARD_GLOW + "2"));
        assertFalse(VfxArtRenderer.isReady("RewardGlowEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.MAP_CIRCLE + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.MAP_CIRCLE + "2"));
        assertFalse(VfxArtRenderer.isReady("MapCircleEffect"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SPOTLIGHT + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SPOTLIGHT + "2"));
        assertFalse(VfxArtRenderer.isReady("SpotlightEffect"));
    }

    @Test
    public void installVfxRendererIsIdempotent() {
        Sts1HostAssets.installVfxRenderer();
        Sts1HostAssets.installVfxRenderer();
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
    }

    @Test
    public void newerClaimableFqnsAreAppendedLast() {
        Sts1HostAssets.installVfxRenderer();

        java.util.List<String> supported = VfxClaimPolicy.supportedClasses();
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.NEMESIS_FIRE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SHIELD_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DEBUFF_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.GHOSTLY_WEAK_FIRE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.GENERIC_SMOKE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.EXHAUST_BLUR));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.ICE_SHATTER));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WEB_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.ENTANGLE_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.BLOCK_IMPACT_LINE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.EXHAUST_PILE_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.UNKNOWN_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLAME_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_BLUR));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_LINE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DARK_ORB_PASSIVE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WARNING_SIGN));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STUN_STAR));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FALLING_DUST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLAME_BALL));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SHINE_LINES));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_M));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_S));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_DUST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.GLOWY_FIRE_EYES));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLYING_SPIKE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.CONE_EFFECT));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FALLING_ICE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DAMAGE_HEART));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SPOOKY_CHEST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SPOOKIER_CHEST));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.DEATH_SCREEN_FLOATY));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WRATH_STANCE_CHANGE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.WATER_SPLASH));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.BUFF_PARTICLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.BOTTOM_FOG));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.GIANT_FIRE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.TORCH_HEAD_FIRE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.CARD_TRAIL));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLYING_ORB));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.FLICK_COIN));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.REWARD_GLOW));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.MAP_CIRCLE));
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.SPOTLIGHT));
        assertEquals("the newest B07 FQN is appended last",
                VfxClaimPolicy.SPOTLIGHT, supported.get(supported.size() - 1));
        assertEquals("the B06 FQN (MapCircle) is appended before it",
                VfxClaimPolicy.MAP_CIRCLE, supported.get(supported.size() - 2));
        assertEquals("the B05 FQN (RewardGlow) is appended before it",
                VfxClaimPolicy.REWARD_GLOW, supported.get(supported.size() - 3));
        assertEquals("the B04 FQN (PingHp) is appended before it",
                VfxClaimPolicy.PING_HP, supported.get(supported.size() - 4));
        assertEquals("the B03 FQN (HealPanel) is appended before it",
                VfxClaimPolicy.HEAL_PANEL, supported.get(supported.size() - 5));
        assertEquals("the B02 FQN (FlickCoin) is appended before it",
                VfxClaimPolicy.FLICK_COIN, supported.get(supported.size() - 6));
        assertEquals("the B01 FQN is appended before it",
                VfxClaimPolicy.FLYING_ORB, supported.get(supported.size() - 7));
        assertEquals("the newest F30 FQN is appended before it",
                VfxClaimPolicy.CARD_TRAIL, supported.get(supported.size() - 8));
        assertEquals("the F29 TorchHeadFire FQN is appended just before it",
                VfxClaimPolicy.TORCH_HEAD_FIRE, supported.get(supported.size() - 9));
        assertEquals("the F28 GiantFire FQN is appended just before it",
                VfxClaimPolicy.GIANT_FIRE, supported.get(supported.size() - 10));
        assertEquals("the F28 BottomFog FQN is appended just before it",
                VfxClaimPolicy.BOTTOM_FOG, supported.get(supported.size() - 11));
        assertEquals("the F27 FQNs are appended before them",
                java.util.Arrays.asList(VfxClaimPolicy.WATER_SPLASH, VfxClaimPolicy.BUFF_PARTICLE),
                supported.subList(supported.size() - 13, supported.size() - 11));
        assertEquals("the F25 FQN is appended before them",
                VfxClaimPolicy.STANCE_CHANGE_ABSORPTION, supported.get(supported.size() - 14));
        assertEquals("the F24 FQN is appended before it, in order",
                VfxClaimPolicy.WRATH_STANCE_CHANGE, supported.get(supported.size() - 15));
        assertEquals("the three F23 FQNs are appended before it, in order",
                java.util.Arrays.asList(VfxClaimPolicy.SPOOKIER_CHEST,
                        VfxClaimPolicy.CAMPFIRE_SLEEP_COVER, VfxClaimPolicy.DEATH_SCREEN_FLOATY),
                supported.subList(supported.size() - 18, supported.size() - 15));
        assertEquals("the F22 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.SPOOKY_CHEST,
                        VfxClaimPolicy.IRONCLAD_VICTORY_FLAME),
                supported.subList(supported.size() - 20, supported.size() - 18));
        assertEquals("the F21 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FALLING_ICE,
                        VfxClaimPolicy.DAMAGE_HEART),
                supported.subList(supported.size() - 22, supported.size() - 20));
        assertEquals("the F20 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FLYING_SPIKE,
                        VfxClaimPolicy.CONE_EFFECT),
                supported.subList(supported.size() - 24, supported.size() - 22));
        assertEquals("the F19 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE,
                        VfxClaimPolicy.GLOWY_FIRE_EYES),
                supported.subList(supported.size() - 26, supported.size() - 24));
        assertEquals("the F18 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.TORCH_PARTICLE_M,
                        VfxClaimPolicy.TORCH_PARTICLE_S, VfxClaimPolicy.SCENE_DUST),
                supported.subList(supported.size() - 29, supported.size() - 26));
        assertEquals("the F17 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.LIGHTNING_EFFECT, VfxClaimPolicy.FLAME_BALL,
                        VfxClaimPolicy.SHINE_LINES),
                supported.subList(supported.size() - 32, supported.size() - 29));
        assertEquals("the F16 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.WARNING_SIGN, VfxClaimPolicy.STUN_STAR,
                        VfxClaimPolicy.FALLING_DUST),
                supported.subList(supported.size() - 35, supported.size() - 32));
        assertEquals("the five newest FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FLAME_PARTICLE,
                        VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE, VfxClaimPolicy.DAMAGE_IMPACT_BLUR,
                        VfxClaimPolicy.DAMAGE_IMPACT_LINE, VfxClaimPolicy.DARK_ORB_PASSIVE),
                supported.subList(supported.size() - 40, supported.size() - 35));
        assertEquals("the four newest FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.ENTANGLE_EFFECT,
                        VfxClaimPolicy.BLOCK_IMPACT_LINE, VfxClaimPolicy.EXHAUST_PILE_PARTICLE,
                        VfxClaimPolicy.UNKNOWN_PARTICLE),
                supported.subList(supported.size() - 44, supported.size() - 40));
        assertEquals("the two next-newest FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.ICE_SHATTER, VfxClaimPolicy.WEB_PARTICLE),
                supported.subList(supported.size() - 46, supported.size() - 44));
        assertEquals("the thirteen newest FQNs are appended last, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FIRE_BURST, VfxClaimPolicy.RED_FIRE_BURST,
                        VfxClaimPolicy.SMOKE_BLUR, VfxClaimPolicy.CEILING_DUST,
                        VfxClaimPolicy.NEMESIS_FIRE, VfxClaimPolicy.SHIELD_PARTICLE,
                        VfxClaimPolicy.DEBUFF_PARTICLE, VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL,
                        VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxClaimPolicy.GENERIC_SMOKE,
                        VfxClaimPolicy.EXHAUST_BLUR, VfxClaimPolicy.ICE_SHATTER,
                        VfxClaimPolicy.WEB_PARTICLE),
                supported.subList(supported.size() - 57, supported.size() - 44));
    }

    @Test
    public void uninstallRestoresTheInertDefault() {
        Sts1HostAssets.installVfxRenderer();
        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));

        Sts1HostAssets.resetVfxRendererForTests();
        assertFalse(Sts1HostAssets.isVfxRendererInstalled());
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(VfxArtRenderer.render(null, null));
    }
}
