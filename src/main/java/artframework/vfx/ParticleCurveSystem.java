package artframework.vfx;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.List;

/** Replaces particle render values with normalized-age curve and gradient samples. */
public final class ParticleCurveSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(VfxEmitterComponent.class, VfxParticleBufferComponent.class)) {
            VfxEmitterComponent emitter = world.get(entity, VfxEmitterComponent.class);
            List<VfxParticle> result = new ArrayList<VfxParticle>();
            for (VfxParticle particle : world.get(entity, VfxParticleBufferComponent.class).particles) {
                float normalizedAge = Math.max(0f, Math.min(1f, particle.age / particle.lifetime));
                float scale = particle.baseScale * VfxSampling.curve(emitter.scaleCurve, normalizedAge, 1f);
                float alpha = VfxSampling.curve(emitter.alphaCurve, normalizedAge, 1f);
                VfxColor color = VfxSampling.gradient(emitter.colorRamp, normalizedAge,
                        new VfxColor(1f, 1f, 1f, 1f));
                alpha *= color.a;
                result.add(new VfxParticle(particle.spawnIndex, particle.age, particle.lifetime,
                        particle.position, particle.velocity, particle.rotationDegrees,
                        particle.angularVelocity, particle.baseScale, scale, alpha,
                        new VfxColor(color.r, color.g, color.b, color.a)));
            }
            world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(result));
        }
    }
}
