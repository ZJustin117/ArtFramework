package artframework.sts1.render;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure-logic coverage for the family-neutral per-instance transient-effect claim plumbing (current
 * members are the {@code vfx-stance-aura} FQNs): the default-off gate, the exact-FQN claim
 * predicate, and the inert injected draw seam. No GL or device classes are touched.
 */
public class VfxDelegationSeamTest {

    @Before
    public void setUp() {
        VfxDelegationGate.resetForTests();
        VfxArtRenderer.resetForTests();
        VfxArtRenderer.resetDrawCountForTests();
    }

    @After
    public void tearDown() {
        VfxDelegationGate.resetForTests();
        VfxArtRenderer.resetForTests();
        VfxArtRenderer.resetDrawCountForTests();
    }

    @Test
    public void delegationGateDefaultsOffAndToggles() {
        assertFalse(VfxDelegationGate.isActive());
        VfxDelegationGate.setActive(true);
        assertTrue(VfxDelegationGate.isActive());
        VfxDelegationGate.resetForTests();
        assertFalse(VfxDelegationGate.isActive());
    }

    @Test
    public void claimPolicySupportsTheSupportedFqns() {
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.CALM_PARTICLE_EFFECT));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SCENE_LIGHT_FLARE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FLASH_ATK_IMG));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SCENE_LIGHT_FLARE_M));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SCENE_LIGHT_FLARE_L));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FIRE_BURST));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.RED_FIRE_BURST));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SMOKE_BLUR));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.CEILING_DUST));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.NEMESIS_FIRE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SHIELD_PARTICLE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.DEBUFF_PARTICLE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.GHOSTLY_WEAK_FIRE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.GENERIC_SMOKE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.EXHAUST_BLUR));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.ICE_SHATTER));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.WEB_PARTICLE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.ENTANGLE_EFFECT));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.BLOCK_IMPACT_LINE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.EXHAUST_PILE_PARTICLE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.UNKNOWN_PARTICLE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FLAME_PARTICLE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.DAMAGE_IMPACT_BLUR));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.DAMAGE_IMPACT_LINE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.DARK_ORB_PASSIVE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.WARNING_SIGN));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.STUN_STAR));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FALLING_DUST));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.LIGHTNING_EFFECT));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FLAME_BALL));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SHINE_LINES));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.TORCH_PARTICLE_M));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.TORCH_PARTICLE_S));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SCENE_DUST));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.GLOWY_FIRE_EYES));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FLYING_SPIKE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.CONE_EFFECT));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FALLING_ICE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.DAMAGE_HEART));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SPOOKY_CHEST));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.SPOOKIER_CHEST));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.DEATH_SCREEN_FLOATY));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.WRATH_STANCE_CHANGE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.WATER_SPLASH));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.BUFF_PARTICLE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.BOTTOM_FOG));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.GIANT_FIRE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.TORCH_HEAD_FIRE));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.CARD_TRAIL));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FLYING_ORB));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.FLICK_COIN));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.HEAL_PANEL));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.PING_HP));
        assertTrue(VfxClaimPolicy.supports(VfxClaimPolicy.REWARD_GLOW));
    }

    @Test
    public void claimPolicyRejectsNullBlankAndOtherVfxClasses() {
        assertFalse(VfxClaimPolicy.supports(null));
        assertFalse(VfxClaimPolicy.supports(""));
        assertFalse(VfxClaimPolicy.supports("   "));
        assertFalse(VfxClaimPolicy.supports(
                "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect2"));
        assertFalse(VfxClaimPolicy.supports(
                "com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle2"));
        assertFalse(VfxClaimPolicy.supports(
                "com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle$Sub"));
        assertFalse(VfxClaimPolicy.supports(
                "com.megacrit.cardcrawl.vfx.combat.StrikeEffect"));
        assertFalse(VfxClaimPolicy.supports("artframework.sts1.render.VfxClaimPolicy"));
        assertFalse("a subclass-qualified name is not the exact FQN",
                VfxClaimPolicy.supports(VfxClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
    }

    @Test
    public void supportedClassesListsTheSupportedFqns() {
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.CALM_PARTICLE_EFFECT));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.SCENE_LIGHT_FLARE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FLASH_ATK_IMG));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.SCENE_LIGHT_FLARE_M));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.SCENE_LIGHT_FLARE_L));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FIRE_BURST));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.RED_FIRE_BURST));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.SMOKE_BLUR));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.CEILING_DUST));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.NEMESIS_FIRE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.SHIELD_PARTICLE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.DEBUFF_PARTICLE));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.GHOSTLY_WEAK_FIRE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.GENERIC_SMOKE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.EXHAUST_BLUR));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.ICE_SHATTER));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.WEB_PARTICLE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.ENTANGLE_EFFECT));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.BLOCK_IMPACT_LINE));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.EXHAUST_PILE_PARTICLE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.UNKNOWN_PARTICLE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FLAME_PARTICLE));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.DAMAGE_IMPACT_BLUR));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.DAMAGE_IMPACT_LINE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.DARK_ORB_PASSIVE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.WARNING_SIGN));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.STUN_STAR));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FALLING_DUST));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.LIGHTNING_EFFECT));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FLAME_BALL));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.SHINE_LINES));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.TORCH_PARTICLE_M));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.TORCH_PARTICLE_S));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.SCENE_DUST));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.GLOWY_FIRE_EYES));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FLYING_SPIKE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.CONE_EFFECT));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FALLING_ICE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.DAMAGE_HEART));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.SPOOKY_CHEST));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.SPOOKIER_CHEST));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.DEATH_SCREEN_FLOATY));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.WRATH_STANCE_CHANGE));
        assertTrue(VfxClaimPolicy.supportedClasses()
                .contains(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.WATER_SPLASH));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.BUFF_PARTICLE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.BOTTOM_FOG));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.GIANT_FIRE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.TORCH_HEAD_FIRE));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.CARD_TRAIL));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FLYING_ORB));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.FLICK_COIN));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.HEAL_PANEL));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.PING_HP));
        assertTrue(VfxClaimPolicy.supportedClasses().contains(VfxClaimPolicy.REWARD_GLOW));
        assertEquals("the newest B05 FQN (RewardGlow) is appended last",
                VfxClaimPolicy.REWARD_GLOW,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 1));
        assertEquals("the B04 FQN (PingHp) is appended just before it",
                VfxClaimPolicy.PING_HP,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 2));
        assertEquals("the B03 FQN (HealPanel) is appended just before it",
                VfxClaimPolicy.HEAL_PANEL,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 3));
        assertEquals("the B02 FQN (FlickCoin) is appended just before it",
                VfxClaimPolicy.FLICK_COIN,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 4));
        assertEquals("the B01 FQN (FlyingOrb) is appended just before it",
                VfxClaimPolicy.FLYING_ORB,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 5));
        assertEquals("the F30 FQN (CardTrail) is appended just before it",
                VfxClaimPolicy.CARD_TRAIL,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 6));
        assertEquals("the F29 FQN (TorchHeadFire) is appended just before it",
                VfxClaimPolicy.TORCH_HEAD_FIRE,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 7));
        assertEquals("the F28 FQNs are appended just before it, BOTTOM_FOG then GIANT_FIRE",
                java.util.Arrays.asList(VfxClaimPolicy.BOTTOM_FOG, VfxClaimPolicy.GIANT_FIRE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 9,
                        VfxClaimPolicy.supportedClasses().size() - 7));
        assertEquals("the F27 FQNs are appended before them",
                java.util.Arrays.asList(VfxClaimPolicy.WATER_SPLASH, VfxClaimPolicy.BUFF_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 11,
                        VfxClaimPolicy.supportedClasses().size() - 9));
        assertEquals("the F25 FQN is appended before them",
                VfxClaimPolicy.STANCE_CHANGE_ABSORPTION,
                VfxClaimPolicy.supportedClasses().get(VfxClaimPolicy.supportedClasses().size() - 12));
        assertEquals("the F24 FQN is appended before it, in order",
                VfxClaimPolicy.WRATH_STANCE_CHANGE,
                VfxClaimPolicy.supportedClasses().get(VfxClaimPolicy.supportedClasses().size() - 13));
        assertEquals("the three F23 FQNs are appended before it, in order",
                java.util.Arrays.asList(VfxClaimPolicy.SPOOKIER_CHEST,
                        VfxClaimPolicy.CAMPFIRE_SLEEP_COVER, VfxClaimPolicy.DEATH_SCREEN_FLOATY),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 16,
                        VfxClaimPolicy.supportedClasses().size() - 13));
        assertEquals("the F22 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.SPOOKY_CHEST,
                        VfxClaimPolicy.IRONCLAD_VICTORY_FLAME),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 18,
                        VfxClaimPolicy.supportedClasses().size() - 16));
        assertEquals("the F21 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FALLING_ICE,
                        VfxClaimPolicy.DAMAGE_HEART),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 20,
                        VfxClaimPolicy.supportedClasses().size() - 18));
        assertEquals("the F20 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FLYING_SPIKE,
                        VfxClaimPolicy.CONE_EFFECT),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 22,
                        VfxClaimPolicy.supportedClasses().size() - 20));
        assertEquals("the F19 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE,
                        VfxClaimPolicy.GLOWY_FIRE_EYES),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 24,
                        VfxClaimPolicy.supportedClasses().size() - 22));
        assertEquals("the F18 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.TORCH_PARTICLE_M,
                        VfxClaimPolicy.TORCH_PARTICLE_S, VfxClaimPolicy.SCENE_DUST),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 27,
                        VfxClaimPolicy.supportedClasses().size() - 24));
        assertEquals("the F17 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.LIGHTNING_EFFECT, VfxClaimPolicy.FLAME_BALL,
                        VfxClaimPolicy.SHINE_LINES),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 30,
                        VfxClaimPolicy.supportedClasses().size() - 27));
        assertEquals("the F16 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.WARNING_SIGN, VfxClaimPolicy.STUN_STAR,
                        VfxClaimPolicy.FALLING_DUST),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 33,
                        VfxClaimPolicy.supportedClasses().size() - 30));
        assertEquals("the five F15 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FLAME_PARTICLE,
                        VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE, VfxClaimPolicy.DAMAGE_IMPACT_BLUR,
                        VfxClaimPolicy.DAMAGE_IMPACT_LINE, VfxClaimPolicy.DARK_ORB_PASSIVE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 38,
                        VfxClaimPolicy.supportedClasses().size() - 33));
        assertEquals("the four F14 FQNs are appended before the five newest, in order",
                java.util.Arrays.asList(VfxClaimPolicy.ENTANGLE_EFFECT,
                        VfxClaimPolicy.BLOCK_IMPACT_LINE, VfxClaimPolicy.EXHAUST_PILE_PARTICLE,
                        VfxClaimPolicy.UNKNOWN_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 42,
                        VfxClaimPolicy.supportedClasses().size() - 38));
        assertEquals("the two F13 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.ICE_SHATTER, VfxClaimPolicy.WEB_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 44,
                        VfxClaimPolicy.supportedClasses().size() - 42));
        assertEquals("the six F12 FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL,
                        VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxClaimPolicy.GENERIC_SMOKE,
                        VfxClaimPolicy.EXHAUST_BLUR, VfxClaimPolicy.ICE_SHATTER,
                        VfxClaimPolicy.WEB_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 48,
                        VfxClaimPolicy.supportedClasses().size() - 42));
        assertEquals("the thirteen F10/F11 FQNs stay in append order",
                java.util.Arrays.asList(VfxClaimPolicy.FIRE_BURST, VfxClaimPolicy.RED_FIRE_BURST,
                        VfxClaimPolicy.SMOKE_BLUR, VfxClaimPolicy.CEILING_DUST,
                        VfxClaimPolicy.NEMESIS_FIRE, VfxClaimPolicy.SHIELD_PARTICLE,
                        VfxClaimPolicy.DEBUFF_PARTICLE, VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL,
                        VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxClaimPolicy.GENERIC_SMOKE,
                        VfxClaimPolicy.EXHAUST_BLUR, VfxClaimPolicy.ICE_SHATTER,
                        VfxClaimPolicy.WEB_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 55,
                        VfxClaimPolicy.supportedClasses().size() - 42));
        assertEquals("the F28 giant fire FQN is appended just before the F29 torch head fire",
                VfxClaimPolicy.GIANT_FIRE,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 8));
    }

    @Test
    public void rendererDefaultIsInertNotReady() {
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(VfxArtRenderer.render(null, null));
    }

    @Test
    public void injectedAdapterIsUsedAndResetRestoresInertDefault() {
        VfxArtRenderer.setForTests(new VfxArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) { return true; }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return true; }
            @Override public boolean canDraw(Object effect) { return true; }
        });

        assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(VfxArtRenderer.render(null, null));

        VfxArtRenderer.resetForTests();
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(VfxArtRenderer.render(null, null));
    }

    @Test
    public void throwingAdapterFailsOpenWithoutPropagating() {
        VfxArtRenderer.setForTests(new VfxArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) {
                throw new IllegalStateException("ready boom");
            }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
                throw new IllegalStateException("draw boom");
            }
            @Override public boolean canDraw(Object effect) {
                throw new IllegalStateException("canDraw boom");
            }
        });

        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(VfxArtRenderer.render(null, null));
        assertFalse("a throwing canDraw delegator fails closed",
                VfxArtRenderer.canDraw(new Object()));
    }

    @Test
    public void setForTestsNullRestoresInertDefault() {
        VfxArtRenderer.setForTests(new VfxArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) { return true; }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return true; }
            @Override public boolean canDraw(Object effect) { return true; }
        });

        VfxArtRenderer.setForTests(null);

        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(VfxArtRenderer.render(null, null));
    }

    @Test
    public void probeSliceHasExactlyGateReadyDrawsKeys() {
        Map<String, Object> probe = VfxArtRenderer.probeSlice();

        assertEquals(3, probe.size());
        assertTrue(probe.containsKey("gate"));
        assertTrue(probe.containsKey("ready"));
        assertTrue(probe.containsKey("draws"));
    }

    @Test
    public void probeSliceDefaultsAreInert() {
        Map<String, Object> probe = VfxArtRenderer.probeSlice();

        assertEquals(Boolean.FALSE, probe.get("gate"));
        assertEquals(Integer.valueOf(0), probe.get("ready"));
        assertEquals(Integer.valueOf(0), probe.get("draws"));
    }

    @Test
    public void probeSliceReportsReadyCountForInstalledAdapter() {
        VfxArtRenderer.install(new VfxArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) {
                return VfxClaimPolicy.supports(nativeClassName);
            }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return true; }
            @Override public boolean canDraw(Object effect) { return true; }
        });

        Map<String, Object> probe = VfxArtRenderer.probeSlice();

        assertEquals(Integer.valueOf(VfxClaimPolicy.supportedClasses().size()), probe.get("ready"));
        VfxDelegationGate.setActive(true);
        assertEquals(Boolean.TRUE, VfxArtRenderer.probeSlice().get("gate"));
    }

    @Test
    public void probeSliceReportsGateAndDrawCount() {
        VfxDelegationGate.setActive(true);
        VfxArtRenderer.recordDraw();
        VfxArtRenderer.recordDraw();

        Map<String, Object> probe = VfxArtRenderer.probeSlice();

        assertEquals(Boolean.TRUE, probe.get("gate"));
        assertEquals(Integer.valueOf(2), probe.get("draws"));
    }

    @Test
    public void probeSliceFailsClosedWhenAdapterThrows() {
        VfxArtRenderer.setForTests(new VfxArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) {
                throw new IllegalStateException("ready boom");
            }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return false; }
            @Override public boolean canDraw(Object effect) { return false; }
        });
        VfxDelegationGate.setActive(true);

        Map<String, Object> probe = VfxArtRenderer.probeSlice();

        assertEquals(Boolean.TRUE, probe.get("gate"));
        assertEquals(Integer.valueOf(0), probe.get("ready"));
        assertEquals(Integer.valueOf(0), probe.get("draws"));
    }
}
