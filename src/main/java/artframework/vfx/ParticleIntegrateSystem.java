package artframework.vfx;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.List;

/** Stateless semi-implicit Euler integration for age, gravity, motion, and rotation. */
public final class ParticleIntegrateSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(VfxEmitterComponent.class, VfxParticleBufferComponent.class)) {
            VfxEmitterComponent emitter = world.get(entity, VfxEmitterComponent.class);
            List<VfxParticle> result = new ArrayList<VfxParticle>();
            for (VfxParticle particle : world.get(entity, VfxParticleBufferComponent.class).particles) {
                VfxVec2 velocity = new VfxVec2(particle.velocity.x + emitter.gravity.x * tick.deltaSeconds,
                        particle.velocity.y + emitter.gravity.y * tick.deltaSeconds);
                VfxVec2 position = new VfxVec2(particle.position.x + velocity.x * tick.deltaSeconds,
                        particle.position.y + velocity.y * tick.deltaSeconds);
                result.add(new VfxParticle(particle.spawnIndex, particle.age + tick.deltaSeconds,
                        particle.lifetime, position, velocity,
                        particle.rotationDegrees + particle.angularVelocity * tick.deltaSeconds,
                        particle.angularVelocity, particle.baseScale, particle.scale,
                        particle.alpha, particle.color));
            }
            world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(result));
        }
    }
}
