package artframework.sts1.render;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure-logic coverage for the F1 {@code vfx-stance-aura} per-instance claim plumbing: the
 * default-off gate, the exact-FQN claim predicate, and the inert injected draw seam. No GL or
 * device classes are touched.
 */
public class AuraDelegationSeamTest {

    @Before
    public void setUp() {
        AuraDelegationGate.resetForTests();
        AuraArtRenderer.resetForTests();
        AuraArtRenderer.resetDrawCountForTests();
    }

    @After
    public void tearDown() {
        AuraDelegationGate.resetForTests();
        AuraArtRenderer.resetForTests();
        AuraArtRenderer.resetDrawCountForTests();
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
    public void claimPolicySupportsTheSupportedAuraFqns() {
        assertTrue(AuraClaimPolicy.supports(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(AuraClaimPolicy.supports(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(AuraClaimPolicy.supports(AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT));
        assertTrue(AuraClaimPolicy.supports(AuraClaimPolicy.CALM_PARTICLE_EFFECT));
    }

    @Test
    public void claimPolicyRejectsNullBlankAndOtherVfxClasses() {
        assertFalse(AuraClaimPolicy.supports(null));
        assertFalse(AuraClaimPolicy.supports(""));
        assertFalse(AuraClaimPolicy.supports("   "));
        assertFalse(AuraClaimPolicy.supports(
                "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect2"));
        assertFalse(AuraClaimPolicy.supports(
                "com.megacrit.cardcrawl.vfx.combat.StrikeEffect"));
        assertFalse(AuraClaimPolicy.supports("artframework.sts1.render.AuraClaimPolicy"));
        assertFalse("a subclass-qualified name is not the exact FQN",
                AuraClaimPolicy.supports(AuraClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
    }

    @Test
    public void supportedClassesListsTheSupportedFqns() {
        assertTrue(AuraClaimPolicy.supportedClasses().contains(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(AuraClaimPolicy.supportedClasses().contains(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(AuraClaimPolicy.supportedClasses()
                .contains(AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT));
        assertTrue(AuraClaimPolicy.supportedClasses()
                .contains(AuraClaimPolicy.CALM_PARTICLE_EFFECT));
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

    @Test
    public void probeSliceHasExactlyGateReadyDrawsKeys() {
        Map<String, Object> probe = AuraArtRenderer.probeSlice();

        assertEquals(3, probe.size());
        assertTrue(probe.containsKey("gate"));
        assertTrue(probe.containsKey("ready"));
        assertTrue(probe.containsKey("draws"));
    }

    @Test
    public void probeSliceDefaultsAreInert() {
        Map<String, Object> probe = AuraArtRenderer.probeSlice();

        assertEquals(Boolean.FALSE, probe.get("gate"));
        assertEquals(Integer.valueOf(0), probe.get("ready"));
        assertEquals(Integer.valueOf(0), probe.get("draws"));
    }

    @Test
    public void probeSliceReportsReadyCountForInstalledAdapter() {
        AuraArtRenderer.install(new AuraArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) {
                return AuraClaimPolicy.supports(nativeClassName);
            }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return true; }
        });

        Map<String, Object> probe = AuraArtRenderer.probeSlice();

        assertEquals(Integer.valueOf(AuraClaimPolicy.supportedClasses().size()), probe.get("ready"));
        AuraDelegationGate.setActive(true);
        assertEquals(Boolean.TRUE, AuraArtRenderer.probeSlice().get("gate"));
    }

    @Test
    public void probeSliceReportsGateAndDrawCount() {
        AuraDelegationGate.setActive(true);
        AuraArtRenderer.recordDraw();
        AuraArtRenderer.recordDraw();

        Map<String, Object> probe = AuraArtRenderer.probeSlice();

        assertEquals(Boolean.TRUE, probe.get("gate"));
        assertEquals(Integer.valueOf(2), probe.get("draws"));
    }

    @Test
    public void probeSliceFailsClosedWhenAdapterThrows() {
        AuraArtRenderer.setForTests(new AuraArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) {
                throw new IllegalStateException("ready boom");
            }
            @Override public boolean render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb,
                    com.megacrit.cardcrawl.vfx.AbstractGameEffect effect) { return false; }
        });
        AuraDelegationGate.setActive(true);

        Map<String, Object> probe = AuraArtRenderer.probeSlice();

        assertEquals(Boolean.TRUE, probe.get("gate"));
        assertEquals(Integer.valueOf(0), probe.get("ready"));
        assertEquals(Integer.valueOf(0), probe.get("draws"));
    }
}
