package artframework.vfx;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

/** Pure world-level emitter stop/restart control; no GL, no host state, no retained authority. */
public final class VfxEmitterControl {
    private VfxEmitterControl() {}

    /** Stops one emitter entity: no new particles spawn, in-flight particles keep ageing out. */
    public static boolean stop(PresentationWorld world, EntityId emitter) {
        if (world == null || emitter == null) throw new IllegalArgumentException("world and emitter required");
        if (!world.contains(emitter)) return false;
        VfxEmitterStateComponent state = world.get(emitter, VfxEmitterStateComponent.class);
        if (state == null) return false;
        world.put(emitter, VfxEmitterStateComponent.class, state.requestStop());
        return true;
    }

    /** Restarts one emitter entity from its unchanged seed, re-arming deterministic one-shot spawn. */
    public static boolean restart(PresentationWorld world, EntityId emitter) {
        if (world == null || emitter == null) throw new IllegalArgumentException("world and emitter required");
        if (!world.contains(emitter)) return false;
        VfxEmitterStateComponent state = world.get(emitter, VfxEmitterStateComponent.class);
        if (state == null) return false;
        world.put(emitter, VfxEmitterStateComponent.class, state.restart());
        return true;
    }
}
