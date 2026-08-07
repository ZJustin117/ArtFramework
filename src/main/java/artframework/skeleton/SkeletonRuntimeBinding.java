package artframework.skeleton;

import artframework.ecs.EntityId;

/** Host-neutral identity of a loaded provider handle. The native object remains provider-owned. */
public final class SkeletonRuntimeBinding {
    public final EntityId entityId;
    public final SkeletonHandle handle;
    public final String assetKey;

    public SkeletonRuntimeBinding(EntityId entityId, SkeletonHandle handle, String assetKey) {
        this.entityId = entityId; this.handle = handle; this.assetKey = assetKey != null ? assetKey : "";
    }
}
