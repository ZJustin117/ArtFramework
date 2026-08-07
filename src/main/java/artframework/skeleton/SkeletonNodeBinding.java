package artframework.skeleton;

/** Declarative link from a UI node to a presentation skeleton entity. */
public final class SkeletonNodeBinding {
    public final String entityKey;
    public final String anchorBone;
    public final float offsetX;
    public final float offsetY;
    public final float localScale;
    public final boolean nodeOwnsEntity;

    public SkeletonNodeBinding(String entityKey, String anchorBone, float offsetX,
            float offsetY, float localScale, boolean nodeOwnsEntity) {
        if (entityKey == null || entityKey.trim().isEmpty()) {
            throw new IllegalArgumentException("entityKey required");
        }
        if (!(localScale > 0f)) throw new IllegalArgumentException("localScale must be positive");
        this.entityKey = entityKey;
        this.anchorBone = anchorBone != null ? anchorBone : "";
        this.offsetX = offsetX; this.offsetY = offsetY;
        this.localScale = localScale; this.nodeOwnsEntity = nodeOwnsEntity;
    }
}
