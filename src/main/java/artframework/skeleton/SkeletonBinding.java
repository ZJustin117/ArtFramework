package artframework.skeleton;

/**
 * Null-safe facade around a loaded skeleton handle and command-capable provider.
 */
public final class SkeletonBinding {

    private final SkeletonCommandProvider provider;
    private final SkeletonHandle handle;

    public SkeletonBinding(SkeletonCommandProvider provider, SkeletonHandle handle) {
        this.provider = provider;
        this.handle = handle;
    }

    public boolean isValid() {
        return provider != null && handle != null && handle.isAlive();
    }

    public boolean hasAnimation(String animationId) {
        return isValid() && provider.hasAnimation(handle, animationId);
    }

    public void setAnimation(String animationId, boolean loop) {
        if (isValid()) {
            provider.setAnimation(handle, 0, animationId, loop);
        }
    }

    public void addAnimation(String animationId, boolean loop, float delaySeconds) {
        if (isValid()) {
            provider.addAnimation(handle, 0, animationId, loop, delaySeconds);
        }
    }

    public String currentAnimation() {
        return isValid() ? provider.currentAnimation(handle, 0) : null;
    }

    public void setMix(String from, String to, float seconds) {
        if (isValid()) {
            provider.setMix(handle, from, to, seconds);
        }
    }

    public BoneTransform boneTransform(String boneName) {
        return isValid() ? provider.boneTransform(handle, boneName) : null;
    }
}
