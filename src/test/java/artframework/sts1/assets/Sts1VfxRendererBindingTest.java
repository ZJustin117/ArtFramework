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
        assertEquals("the four newest FQNs are appended last, in order",
                java.util.Arrays.asList(VfxClaimPolicy.ENTANGLE_EFFECT,
                        VfxClaimPolicy.BLOCK_IMPACT_LINE, VfxClaimPolicy.EXHAUST_PILE_PARTICLE,
                        VfxClaimPolicy.UNKNOWN_PARTICLE),
                supported.subList(supported.size() - 4, supported.size()));
        assertEquals("the two next-newest FQNs are appended before them, in order",
                java.util.Arrays.asList(VfxClaimPolicy.ICE_SHATTER, VfxClaimPolicy.WEB_PARTICLE),
                supported.subList(supported.size() - 6, supported.size() - 4));
        assertEquals("the thirteen newest FQNs are appended last, in order",
                java.util.Arrays.asList(VfxClaimPolicy.FIRE_BURST, VfxClaimPolicy.RED_FIRE_BURST,
                        VfxClaimPolicy.SMOKE_BLUR, VfxClaimPolicy.CEILING_DUST,
                        VfxClaimPolicy.NEMESIS_FIRE, VfxClaimPolicy.SHIELD_PARTICLE,
                        VfxClaimPolicy.DEBUFF_PARTICLE, VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL,
                        VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxClaimPolicy.GENERIC_SMOKE,
                        VfxClaimPolicy.EXHAUST_BLUR, VfxClaimPolicy.ICE_SHATTER,
                        VfxClaimPolicy.WEB_PARTICLE),
                supported.subList(supported.size() - 17, supported.size() - 4));
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
