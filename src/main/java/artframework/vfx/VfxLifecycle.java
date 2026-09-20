package artframework.vfx;

import artframework.ecs.PresentationWorld;

/** Component-replacement API for host epoch changes and explicit graph cleanup requests. */
public final class VfxLifecycle {
    private VfxLifecycle() {}

    public static boolean invalidateEpoch(PresentationWorld world, VfxInstance instance, long currentEpoch) {
        if (world == null || instance == null || currentEpoch < 0L) {
            throw new IllegalArgumentException("world, instance, and non-negative epoch required");
        }
        if (!world.contains(instance.rootEntity)) return false;
        VfxSceneRuntimeComponent state = world.get(instance.rootEntity, VfxSceneRuntimeComponent.class);
        if (state == null) return false;
        world.put(instance.rootEntity, VfxSceneRuntimeComponent.class, state.withCurrentEpoch(currentEpoch));
        return true;
    }

    public static boolean requestCleanup(PresentationWorld world, VfxInstance instance) {
        if (world == null || instance == null) throw new IllegalArgumentException("world and instance required");
        if (!world.contains(instance.rootEntity)) return false;
        VfxSceneRuntimeComponent state = world.get(instance.rootEntity, VfxSceneRuntimeComponent.class);
        if (state == null) return false;
        world.put(instance.rootEntity, VfxSceneRuntimeComponent.class, state.requestingCleanup());
        return true;
    }
}
