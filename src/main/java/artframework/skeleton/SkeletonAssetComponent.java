package artframework.skeleton;

/** Immutable asset identity for a presentation skeleton. */
public final class SkeletonAssetComponent {
    public final String providerId;
    public final String atlasResource;
    public final String skeletonResource;
    public final String skin;
    public final float scale;

    public SkeletonAssetComponent(String providerId, String atlasResource,
            String skeletonResource, String skin, float scale) {
        this.providerId = providerId != null ? providerId : "";
        this.atlasResource = atlasResource != null ? atlasResource : "";
        this.skeletonResource = skeletonResource != null ? skeletonResource : "";
        this.skin = skin != null ? skin : "";
        if (!(scale > 0f)) throw new IllegalArgumentException("scale must be positive");
        this.scale = scale;
    }
}
