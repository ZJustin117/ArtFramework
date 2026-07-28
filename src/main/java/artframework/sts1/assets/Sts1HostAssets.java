package artframework.sts1.assets;

import artframework.assets.AssetResolveResult;
import artframework.assets.HostAssets;
import artframework.assets.HostAssetsHolder;
import artframework.assets.ResourceIds;

/**
 * STS1 HostAssets bootstrap: install real vanilla catalog paths (no GL). Texture/audio handle
 * materialization stays host-side for later render slices.
 */
public final class Sts1HostAssets {

    private static boolean installed;

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
