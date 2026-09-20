package artframework.vfx;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.List;

/** Deterministically materializes each bounded one-shot emitter on its first tick. */
public final class ParticleSpawnSystem implements EcsSystem {
    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(VfxEmitterComponent.class,
                VfxEmitterStateComponent.class, VfxParticleBufferComponent.class)) {
            VfxEmitterStateComponent state = world.get(entity, VfxEmitterStateComponent.class);
            if (state.emissionComplete) continue;
            VfxEmitterComponent emitter = world.get(entity, VfxEmitterComponent.class);
            List<VfxParticle> particles = new ArrayList<VfxParticle>(emitter.amount);
            for (int index = 0; index < emitter.amount; index++) particles.add(spawn(emitter, index));
            world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(particles));
            world.put(entity, VfxEmitterStateComponent.class,
                    new VfxEmitterStateComponent(emitter.amount, true));
        }
    }

    private static VfxParticle spawn(VfxEmitterComponent emitter, int index) {
        float lifeRandom = random(emitter.randomSeed, index, 0);
        float lifetime = emitter.lifetime * (1f - emitter.lifetimeRandomness * lifeRandom);
        float speed = lerp(emitter.initialVelocity.min, emitter.initialVelocity.max,
                random(emitter.randomSeed, index, 1));
        float spread = (random(emitter.randomSeed, index, 2) * 2f - 1f) * emitter.spreadDegrees;
        float radians = (float) Math.toRadians(spread);
        float length = (float) Math.sqrt(emitter.direction.x * emitter.direction.x
                + emitter.direction.y * emitter.direction.y);
        float dx = length == 0f ? 1f : emitter.direction.x / length;
        float dy = length == 0f ? 0f : emitter.direction.y / length;
        VfxVec2 velocity = new VfxVec2((dx * (float) Math.cos(radians) - dy * (float) Math.sin(radians)) * speed,
                (dx * (float) Math.sin(radians) + dy * (float) Math.cos(radians)) * speed);
        float scale = lerp(emitter.scale.min, emitter.scale.max, random(emitter.randomSeed, index, 3));
        float angular = lerp(emitter.angularVelocity.min, emitter.angularVelocity.max,
                random(emitter.randomSeed, index, 4));
        return new VfxParticle(index, 0f, Math.max(0.0001f, lifetime), new VfxVec2(0f, 0f),
                velocity, 0f, angular, scale, scale, 1f, new VfxColor(1f, 1f, 1f, 1f));
    }

    private static float random(long seed, int index, int channel) {
        long value = seed + 0x9E3779B97F4A7C15L * (index + 1L) + 0x632BE59BD9B4E019L * (channel + 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (float) ((value >>> 40) & 0xFFFFFFL) / 16777216f;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
