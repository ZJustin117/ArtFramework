package artframework.vfx;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless semi-implicit Euler integration for age, gravity, motion, and rotation.
 *
 * <p>An optional restricted turbulence term is added only when the emitter's
 * {@link VfxEmitterComponent#turbulenceStrength} is positive. It is a deliberately bounded,
 * seed-deterministic CPU <em>approximation</em> of STS2 turbulence and is <strong>not</strong>
 * runtime-equivalent. With the default strength of 0 the integration is byte-identical to the
 * gravity-only path.</p>
 */
public final class ParticleIntegrateSystem implements EcsSystem {
    /** Finite per-axis velocity bound applied only while the turbulence term is active. */
    public static final float MAX_PARTICLE_SPEED = 100000f;
    /** Finite per-axis position bound applied only while the turbulence term is active. */
    public static final float MAX_PARTICLE_POSITION = 100000f;

    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float MIN_FREQUENCY = 0.5f, MAX_FREQUENCY = 3f;
    private static final float MIN_SWIRL_FREQUENCY = 1f, MAX_SWIRL_FREQUENCY = 4f;

    @Override
    public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId entity : world.query(VfxEmitterComponent.class, VfxParticleBufferComponent.class)) {
            VfxEmitterComponent emitter = world.get(entity, VfxEmitterComponent.class);
            boolean turbulent = emitter.turbulenceStrength > 0f;
            List<VfxParticle> result = new ArrayList<VfxParticle>();
            for (VfxParticle particle : world.get(entity, VfxParticleBufferComponent.class).particles) {
                float velocityX = particle.velocity.x + emitter.gravity.x * tick.deltaSeconds;
                float velocityY = particle.velocity.y + emitter.gravity.y * tick.deltaSeconds;
                if (turbulent) {
                    VfxVec2 accel = turbulenceAccel(emitter, particle);
                    velocityX = clamp(velocityX + accel.x * tick.deltaSeconds,
                            -MAX_PARTICLE_SPEED, MAX_PARTICLE_SPEED);
                    velocityY = clamp(velocityY + accel.y * tick.deltaSeconds,
                            -MAX_PARTICLE_SPEED, MAX_PARTICLE_SPEED);
                }
                VfxVec2 velocity = new VfxVec2(velocityX, velocityY);
                float positionX = particle.position.x + velocity.x * tick.deltaSeconds;
                float positionY = particle.position.y + velocity.y * tick.deltaSeconds;
                if (turbulent) {
                    positionX = clamp(positionX, -MAX_PARTICLE_POSITION, MAX_PARTICLE_POSITION);
                    positionY = clamp(positionY, -MAX_PARTICLE_POSITION, MAX_PARTICLE_POSITION);
                }
                VfxVec2 position = new VfxVec2(positionX, positionY);
                result.add(new VfxParticle(particle.spawnIndex, particle.age + tick.deltaSeconds,
                        particle.lifetime, position, velocity,
                        particle.rotationDegrees + particle.angularVelocity * tick.deltaSeconds,
                        particle.angularVelocity, particle.baseScale, particle.scale,
                        particle.alpha, particle.color));
            }
            world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(result));
        }
    }

    /**
     * Bounded, seed-deterministic turbulence acceleration for one particle.
     *
     * <p>Model: a rotating sinusoidal (curl-like) field.
     * {@code theta = age * angleFrequency + anglePhase},
     * {@code amplitude = strength * 0.5 * (1 + sin(age * swirlFrequency + swirlPhase))},
     * {@code accel = (amplitude * cos(theta), amplitude * sin(theta))}.
     * Every phase/frequency is derived from {@code emitter.randomSeed} and {@code particle.spawnIndex}
     * via the same splitmix-style hash used by {@link ParticleSpawnSystem}, and the field advances with
     * {@code particle.age}, so identical seeds + fixed steps produce identical trajectories. The
     * magnitude is bounded by {@link VfxEmitterComponent#MAX_TURBULENCE_STRENGTH} (amplitude in
     * {@code [0, strength]}).</p>
     */
    static VfxVec2 turbulenceAccel(VfxEmitterComponent emitter, VfxParticle particle) {
        float strength = emitter.turbulenceStrength;
        int index = particle.spawnIndex;
        long seed = emitter.randomSeed;
        float anglePhase = unit(seed, index, 10) * TWO_PI;
        float angleFrequency = MIN_FREQUENCY + unit(seed, index, 11) * (MAX_FREQUENCY - MIN_FREQUENCY);
        float swirlPhase = unit(seed, index, 12) * TWO_PI;
        float swirlFrequency = MIN_SWIRL_FREQUENCY
                + unit(seed, index, 13) * (MAX_SWIRL_FREQUENCY - MIN_SWIRL_FREQUENCY);
        float theta = particle.age * angleFrequency + anglePhase;
        float amplitude = strength * 0.5f * (1f + (float) Math.sin(particle.age * swirlFrequency + swirlPhase));
        return new VfxVec2(amplitude * (float) Math.cos(theta), amplitude * (float) Math.sin(theta));
    }

    private static float unit(long seed, int index, int channel) {
        long value = seed + 0x9E3779B97F4A7C15L * (index + 1L) + 0x632BE59BD9B4E019L * (channel + 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (float) ((value >>> 40) & 0xFFFFFFL) / 16777216f;
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) return 0f;
        return Math.max(min, Math.min(max, value));
    }
}
