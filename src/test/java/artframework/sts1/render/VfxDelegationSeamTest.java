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
        assertEquals("the four newest FQNs are appended last, in order",
                java.util.Arrays.asList(VfxClaimPolicy.ENTANGLE_EFFECT,
                        VfxClaimPolicy.BLOCK_IMPACT_LINE, VfxClaimPolicy.EXHAUST_PILE_PARTICLE,
                        VfxClaimPolicy.UNKNOWN_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 4,
                        VfxClaimPolicy.supportedClasses().size()));
        assertEquals("the two newest FQNs are appended last, in order",
                java.util.Arrays.asList(VfxClaimPolicy.ICE_SHATTER, VfxClaimPolicy.WEB_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 6,
                        VfxClaimPolicy.supportedClasses().size() - 4));
        assertEquals("the six newest FQNs are appended last, in order",
                java.util.Arrays.asList(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL,
                        VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxClaimPolicy.GENERIC_SMOKE,
                        VfxClaimPolicy.EXHAUST_BLUR, VfxClaimPolicy.ICE_SHATTER,
                        VfxClaimPolicy.WEB_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 10,
                        VfxClaimPolicy.supportedClasses().size() - 4));
        assertEquals("the thirteen newest FQNs stay in append order",
                java.util.Arrays.asList(VfxClaimPolicy.FIRE_BURST, VfxClaimPolicy.RED_FIRE_BURST,
                        VfxClaimPolicy.SMOKE_BLUR, VfxClaimPolicy.CEILING_DUST,
                        VfxClaimPolicy.NEMESIS_FIRE, VfxClaimPolicy.SHIELD_PARTICLE,
                        VfxClaimPolicy.DEBUFF_PARTICLE, VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL,
                        VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxClaimPolicy.GENERIC_SMOKE,
                        VfxClaimPolicy.EXHAUST_BLUR, VfxClaimPolicy.ICE_SHATTER,
                        VfxClaimPolicy.WEB_PARTICLE),
                VfxClaimPolicy.supportedClasses().subList(
                        VfxClaimPolicy.supportedClasses().size() - 17,
                        VfxClaimPolicy.supportedClasses().size() - 4));
        assertEquals("the last FQN is the unknown particle",
                VfxClaimPolicy.UNKNOWN_PARTICLE,
                VfxClaimPolicy.supportedClasses()
                        .get(VfxClaimPolicy.supportedClasses().size() - 1));
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
        });

        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(VfxArtRenderer.render(null, null));
    }

    @Test
    public void setForTestsNullRestoresInertDefault() {
        VfxArtRenderer.setForTests(new VfxArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) { return true; }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return true; }
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
        });
        VfxDelegationGate.setActive(true);

        Map<String, Object> probe = VfxArtRenderer.probeSlice();

        assertEquals(Boolean.TRUE, probe.get("gate"));
        assertEquals(Integer.valueOf(0), probe.get("ready"));
        assertEquals(Integer.valueOf(0), probe.get("draws"));
    }
}
