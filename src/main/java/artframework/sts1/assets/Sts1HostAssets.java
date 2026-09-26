package artframework.sts1.assets;

import artframework.assets.AssetResolveResult;
import artframework.assets.HostAssets;
import artframework.assets.HostAssetsHolder;
import artframework.assets.ResourceIds;
import artframework.sts1.render.AuraArtRenderer;
import artframework.sts1.render.Sts1AuraArtRenderer;

/**
 * STS1 HostAssets bootstrap: install real vanilla catalog paths (no GL). Texture/audio handle
 * materialization stays host-side for later render slices.
 */
public final class Sts1HostAssets {

    private static boolean installed;
    private static boolean auraRendererInstalled;

    private Sts1HostAssets() {}

    /** Idempotent: replace vanilla catalog with STS1 paths. */
    public static void install() {
        HostAssets assets = HostAssetsHolder.get();
        assets.registerVanillaCatalog(Sts1VanillaCatalog.catalog());
        installed = true;
    }

    public static boolean isInstalled() {
        return installed;
    }

    public static void resetForTests() {
        installed = false;
    }

    /**
     * Idempotent production binding for the real ART aura renderer: installs
     * {@link Sts1AuraArtRenderer} behind the default-off F1 claim seam
     * ({@link AuraArtRenderer}). After this call, {@link AuraArtRenderer#isReady} reports ready for
     * the three exact {@code vfx-stance-aura} FQNs (and only those); the {@code AuraDelegationGate}
     * still controls whether the renderer is consulted, so native remains authoritative while the
     * gate is off. Holds no host/GL state, so it needs no host-recreation hook.
     */
    public static void installAuraRenderer() {
        if (auraRendererInstalled) return;
        AuraArtRenderer.install(new Sts1AuraArtRenderer());
        auraRendererInstalled = true;
    }

    public static boolean isAuraRendererInstalled() {
        return auraRendererInstalled;
    }

    /** Test isolation: drop the binding and restore the inert default renderer. */
    public static void resetAuraRendererForTests() {
        auraRendererInstalled = false;
        AuraArtRenderer.uninstall();
    }

    /**
     * Resolve via current HostAssets; ensure STS1 catalog is present for known keys when tests
     * call without full mod init.
     */
    public static AssetResolveResult resolve(String resourceId) {
        if (!installed) {
            install();
        }
        return HostAssetsHolder.get().resolve(resourceId);
    }

    /** Convenience: card art key for a cardId always has a logical source after install. */
    public static AssetResolveResult resolveCardArt(String cardId) {
        String key = ResourceIds.cardArt(cardId);
        AssetResolveResult r = resolve(key);
        if (r.found) {
            return r;
        }
        // Dynamic card art: synthesize sts1 source so packs can still override the same key.
        return AssetResolveResult.hit(key, HostAssets.VANILLA_PACK_ID, Sts1VanillaCatalog.sourceFor(key));
    }
}
