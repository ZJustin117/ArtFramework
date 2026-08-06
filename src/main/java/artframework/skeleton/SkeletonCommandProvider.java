package artframework.skeleton;

/**
 * Optional command surface for providers that expose animation control.
 */
public interface SkeletonCommandProvider extends SkeletonProvider {

    boolean hasAnimation(SkeletonHandle handle, String animationId);

    void setAnimation(SkeletonHandle handle, int trackId, String animationId, boolean loop);

    void addAnimation(SkeletonHandle handle, int trackId, String animationId, boolean loop, float delaySeconds);

    String currentAnimation(SkeletonHandle handle, int trackId);

    void setMix(SkeletonHandle handle, String from, String to, float seconds);

    void setTimeScale(SkeletonHandle handle, int trackId, float scale);

    void setTrackTime(SkeletonHandle handle, int trackId, float seconds);

    float animationEnd(SkeletonHandle handle, int trackId);

    void update(SkeletonHandle handle, float deltaSeconds);

    void apply(SkeletonHandle handle);

    BoneTransform boneTransform(SkeletonHandle handle, String boneName);

    /** Host render hook. The batch is intentionally opaque to keep core Spine-free. */
    default void render(SkeletonHandle handle, Object batch) {
        // Providers without a renderer remain data-only.
    }
}
