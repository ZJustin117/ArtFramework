package artframework.assets;

/** Pack-local descriptor for a ResourceId (path or logical source). */
public final class AssetRef {

    public final String source;
    public final String packId;

    public AssetRef(String source, String packId) {
        this.source = source != null ? source : "";
        this.packId = packId != null ? packId : "";
    }

    public static AssetRef of(String source) {
        return new AssetRef(source, "");
    }
}
