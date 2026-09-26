package artframework.sts1.assets;

import artframework.sts1.render.AuraArtRenderer;
import artframework.sts1.render.AuraClaimPolicy;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * F2c production-binding coverage: the same idempotent entry point the mod bootstrap calls
 * ({@link Sts1HostAssets#installAuraRenderer()}) must install the real {@code Sts1AuraArtRenderer}
 * so the default-off aura claim seam reports ready for the three supported FQNs, and must be
 * reversible back to the inert default. No GL/game classes are needed, so this runs headless.
 */
public class Sts1AuraRendererBindingTest {

    @After
    public void tearDown() {
        Sts1HostAssets.resetAuraRendererForTests();
    }

    @Test
    public void installAuraRendererBindsTheRealRendererForSupportedClassesOnly() {
        // Inert default before the production install path runs.
        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));

        Sts1HostAssets.installAuraRenderer();
        assertTrue(Sts1HostAssets.isAuraRendererInstalled());

        assertTrue(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(AuraArtRenderer.isReady(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(AuraArtRenderer.isReady(AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT));

        // A different vfx class (and a near-miss) stays not-ready.
        assertFalse(AuraArtRenderer.isReady("com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect"));
        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
    }

    @Test
    public void installAuraRendererIsIdempotent() {
        Sts1HostAssets.installAuraRenderer();
        Sts1HostAssets.installAuraRenderer();
        assertTrue(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
    }

    @Test
    public void uninstallRestoresTheInertDefault() {
        Sts1HostAssets.installAuraRenderer();
        assertTrue(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));

        Sts1HostAssets.resetAuraRendererForTests();
        assertFalse(Sts1HostAssets.isAuraRendererInstalled());
        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(AuraArtRenderer.render(null, null));
    }
}
