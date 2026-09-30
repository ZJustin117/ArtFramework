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

        // A near-miss stays not-ready.
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.FIRE_BURST + "$Sub"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.SMOKE_BLUR + "2"));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.NEMESIS_FIRE + "$Sub"));
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
        assertEquals("the five newest FQNs are appended last, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FIRE_BURST, VfxClaimPolicy.RED_FIRE_BURST,
                        VfxClaimPolicy.SMOKE_BLUR, VfxClaimPolicy.CEILING_DUST,
                        VfxClaimPolicy.NEMESIS_FIRE),
                supported.subList(supported.size() - 5, supported.size()));
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
