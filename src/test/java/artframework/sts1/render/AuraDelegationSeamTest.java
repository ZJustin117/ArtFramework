package artframework.sts1.render;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure-logic coverage for the F1 {@code vfx-stance-aura} per-instance claim plumbing: the
 * default-off gate, the exact-FQN claim predicate, and the inert injected draw seam. No GL or
 * device classes are touched.
 */
public class AuraDelegationSeamTest {

    @After
    public void tearDown() {
        AuraDelegationGate.resetForTests();
        AuraArtRenderer.resetForTests();
    }

    @Test
    public void delegationGateDefaultsOffAndToggles() {
        assertFalse(AuraDelegationGate.isActive());
        AuraDelegationGate.setActive(true);
        assertTrue(AuraDelegationGate.isActive());
        AuraDelegationGate.resetForTests();
        assertFalse(AuraDelegationGate.isActive());
    }

    @Test
    public void claimPolicySupportsExactlyTheThreeAuraFqns() {
        assertTrue(AuraClaimPolicy.supports(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(AuraClaimPolicy.supports(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(AuraClaimPolicy.supports(AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT));
    }

    @Test
    public void claimPolicyRejectsNullBlankAndOtherVfxClasses() {
        assertFalse(AuraClaimPolicy.supports(null));
        assertFalse(AuraClaimPolicy.supports(""));
        assertFalse(AuraClaimPolicy.supports("   "));
        assertFalse(AuraClaimPolicy.supports(
                "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect"));
        assertFalse(AuraClaimPolicy.supports(
                "com.megacrit.cardcrawl.vfx.combat.StrikeEffect"));
        assertFalse(AuraClaimPolicy.supports("artframework.sts1.render.AuraClaimPolicy"));
        assertFalse("a subclass-qualified name is not the exact FQN",
                AuraClaimPolicy.supports(AuraClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
    }

    @Test
    public void supportedClassesListsTheThreeFqns() {
        assertTrue(AuraClaimPolicy.supportedClasses().contains(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(AuraClaimPolicy.supportedClasses().contains(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(AuraClaimPolicy.supportedClasses()
                .contains(AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT));
    }

    @Test
    public void rendererDefaultIsInertNotReady() {
        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(AuraArtRenderer.render(null, null));
    }

    @Test
    public void injectedAdapterIsUsedAndResetRestoresInertDefault() {
        AuraArtRenderer.setForTests(new AuraArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) { return true; }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return true; }
        });

        assertTrue(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(AuraArtRenderer.render(null, null));

        AuraArtRenderer.resetForTests();
        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(AuraArtRenderer.render(null, null));
    }

    @Test
    public void throwingAdapterFailsOpenWithoutPropagating() {
        AuraArtRenderer.setForTests(new AuraArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) {
                throw new IllegalStateException("ready boom");
            }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) {
                throw new IllegalStateException("draw boom");
            }
        });

        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(AuraArtRenderer.render(null, null));
    }

    @Test
    public void setForTestsNullRestoresInertDefault() {
        AuraArtRenderer.setForTests(new AuraArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) { return true; }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return true; }
        });

        AuraArtRenderer.setForTests(null);

        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(AuraArtRenderer.render(null, null));
    }
}
