package artframework.skeleton;

/** Immutable backend snapshot for one skeleton entity. */
public final class SkeletonPresentationView {
    public final String entityKey;
    public final SkeletonAssetComponent asset;
    public final SkeletonPoseComponent pose;
    public final SkeletonAnimationComponent animation;
    public final SkeletonVisualComponent visual;

    public SkeletonPresentationView(String entityKey, SkeletonAssetComponent asset,
            SkeletonPoseComponent pose, SkeletonAnimationComponent animation,
            SkeletonVisualComponent visual) {
        if (entityKey == null || entityKey.trim().isEmpty()) throw new IllegalArgumentException("entityKey required");
        if (asset == null || pose == null || animation == null || visual == null) {
            throw new IllegalArgumentException("skeleton view components required");
        }
        this.entityKey = entityKey; this.asset = asset; this.pose = pose;
        this.animation = animation; this.visual = visual;
    }
}
