package artframework.skeleton;

/** Stable external identity for an ART-owned skeleton presentation entity. */
public final class SkeletonIdentityComponent {
    public final String entityKey;

    public SkeletonIdentityComponent(String entityKey) {
        if (entityKey == null || entityKey.trim().isEmpty()) {
            throw new IllegalArgumentException("entityKey required");
        }
        this.entityKey = entityKey;
    }
}
