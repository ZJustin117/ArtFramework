package artframework.vfx;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Restricted sub-emitter arming for the single recognized trigger
 * {@link VfxNodeDefinition#TRIGGER_ON_PARENT_COMPLETE}.
 *
 * <p>A child emitter carrying a {@link VfxSubEmitterComponent} marker is created DORMANT
 * ({@code VfxEmitterStateComponent.stopped == true}) so {@link ParticleSpawnSystem} never spawns it.
 * This pure system arms it exactly once, on the first tick where its parent emitter has finished
 * emitting ({@code emissionComplete || stopped}) AND its parent particle buffer has drained
 * (no in-flight particles left). Arming is the deterministic restart of the one-shot emitter
 * ({@code spawnedCount = 0, emissionComplete = false, stopped = false}) followed by removal of the
 * marker, which makes the operation idempotent and guarantees AT MOST ONE arming.</p>
 *
 * <p>There is deliberately no GL, no host state, and no retained authority here.</p>
 */
public final class VfxSubEmitterSystem implements EcsSystem {
    /**
     * Fixed bound on how many trigger children under one parent may ever be armed. Trigger children
     * beyond this bound stay permanently dormant; the bound is never exceeded and never throws.
     */
    public static final int MAX_SUB_EMITTERS_PER_PARENT = 4;

    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        List<EntityId> armed = new ArrayList<EntityId>();
        for (EntityId child : world.query(VfxSubEmitterComponent.class)) {
            VfxSubEmitterComponent marker = world.get(child, VfxSubEmitterComponent.class);
            EntityId parent = marker.parentEntity;
            if (parent == null || !world.contains(parent)) continue;
            VfxEmitterStateComponent parentState = world.get(parent, VfxEmitterStateComponent.class);
            if (parentState == null) continue;
            boolean doneEmitting = parentState.emissionComplete || parentState.stopped;
            if (!doneEmitting) continue;
            VfxParticleBufferComponent parentParticles = world.get(parent, VfxParticleBufferComponent.class);
            if (parentParticles == null || !parentParticles.particles.isEmpty()) continue;
            armed.add(child);
        }
        // Arm after the scan so the buffer/marker reads above are a stable snapshot of this tick.
        for (EntityId child : armed) {
            VfxEmitterStateComponent state = world.get(child, VfxEmitterStateComponent.class);
            if (state == null) {
                world.remove(child, VfxSubEmitterComponent.class);
                continue;
            }
            // Deterministic arm: clear the initial dormant flag so ParticleSpawnSystem emits next.
            world.put(child, VfxEmitterStateComponent.class, state.restart());
            // The marker is the once-only latch: its removal makes re-arming impossible.
            world.remove(child, VfxSubEmitterComponent.class);
        }
    }

    /**
     * Dormant trigger-child marker. Present only on trigger children that are within the depth-1 and
     * child-cap bounds; ineligible trigger children are left permanently dormant with no marker.
     */
    public static final class VfxSubEmitterComponent {
        public final EntityId parentEntity;

        public VfxSubEmitterComponent(EntityId parentEntity) {
            if (parentEntity == null) throw new IllegalArgumentException("parent entity required");
            this.parentEntity = parentEntity;
        }
    }
}
