package artframework.assets;

/** Outcome of HostAssets.resolve. */
public final class AssetResolveResult {

    public final String resourceId;
    public final boolean found;
    public final boolean fallback;
    public final String packId;
    public final String source;
    public final String message;

    private AssetResolveResult(
            String resourceId,
            boolean found,
            boolean fallback,
            String packId,
            String source,
            String message) {
        this.resourceId = resourceId != null ? resourceId : "";
        this.found = found;
        this.fallback = fallback;
        this.packId = packId != null ? packId : "";
        this.source = source != null ? source : "";
        this.message = message != null ? message : "";
    }

    public static AssetResolveResult hit(String resourceId, String packId, String source) {
        return new AssetResolveResult(resourceId, true, false, packId, source, "");
    }

    public static AssetResolveResult fallback(String resourceId, String message) {
        return new AssetResolveResult(resourceId, false, true, "", "", message);
    }

    public static AssetResolveResult missing(String resourceId, String message) {
        return new AssetResolveResult(resourceId, false, false, "", "", message);
    }
}
