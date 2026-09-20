package artframework.vfx;

import artframework.ecs.EntityId;

/** Stable root identity used to request epoch invalidation or cleanup. */
public final class VfxInstance {
    public final EntityId rootEntity;
    public final long epoch;

    VfxInstance(EntityId rootEntity, long epoch) {
        this.rootEntity = rootEntity;
        this.epoch = epoch;
    }
}
