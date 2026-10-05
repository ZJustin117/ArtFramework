package artframework.vfx;

import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Restricted, seed-deterministic, bounded turbulence approximation (F06). This is a CPU
 * approximation of STS2 turbulence and is NOT runtime-equivalent. All checks are pure/no-GL.
 */
public class VfxTurbulenceTest {
    private static final float EPS = 1e-4f;

    @Test
    public void defaultStrengthIsZeroAndZeroIsByteIdenticalToGravityOnlyEuler() {
        ParticleEmitterDefinition definition = baseDefinition(3L);
        assertEquals(0f, VfxEmitterComponent.from(definition).turbulenceStrength, 0f);
        assertEquals(0f, VfxEmitterComponent.from(definition, 0f).turbulenceStrength, 0f);

        float dt = 0.125f, gx = -2f, gy = -3f;
        float vx = 1.5f, vy = 2.25f, px = 0.5f, py = -0.75f;
        float age = 0.5f;

        VfxParticle particle = new VfxParticle(7, age, 10f, new VfxVec2(px, py),
                new VfxVec2(vx, vy), 0f, 0f, 1f, 1f, 1f, new VfxColor(1f, 1f, 1f, 1f));
        VfxParticle integrated = integrateOne(definition, 0f, particle, dt);

        float expectedVx = vx + gx * dt;
        float expectedVy = vy + gy * dt;
        float expectedPx = px + expectedVx * dt;
        float expectedPy = py + expectedVy * dt;
        // Exact (0 delta) equality: the zero-strength path is the untouched gravity-only Euler step.
        assertEquals(expectedVx, integrated.velocity.x, 0f);
        assertEquals(expectedVy, integrated.velocity.y, 0f);
        assertEquals(expectedPx, integrated.position.x, 0f);
        assertEquals(expectedPy, integrated.position.y, 0f);
    }

    @Test
    public void positiveStrengthIsSeedDeterministic() {
        PresentationWorld first = turbulentWorld(12345L, 50f, 8, 0.05f, 40);
        PresentationWorld second = turbulentWorld(12345L, 50f, 8, 0.05f, 40);
        List<VfxParticle> a = particles(first);
        List<VfxParticle> b = particles(second);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).position.x, b.get(i).position.x, 0f);
            assertEquals(a.get(i).position.y, b.get(i).position.y, 0f);
            assertEquals(a.get(i).velocity.x, b.get(i).velocity.x, 0f);
            assertEquals(a.get(i).velocity.y, b.get(i).velocity.y, 0f);
        }
        // A positive strength actually perturbs the trajectory away from the gravity-only baseline.
        PresentationWorld none = turbulentWorld(12345L, 0f, 8, 0.05f, 40);
        assertTrue(anyPositionDiffers(particles(none), a));
    }

    @Test
    public void differentSeedProducesDifferentButFiniteTrajectory() {
        PresentationWorld a = turbulentWorld(1L, 50f, 8, 0.05f, 40);
        PresentationWorld b = turbulentWorld(2L, 50f, 8, 0.05f, 40);
        List<VfxParticle> pa = particles(a);
        List<VfxParticle> pb = particles(b);
        assertTrue(anyPositionDiffers(pa, pb));
        for (VfxParticle p : pa) assertFinite(p);
        for (VfxParticle p : pb) assertFinite(p);
    }

    @Test
    public void largeDtStepsStayFiniteAndWithinBounds() {
        PresentationWorld world = turbulentWorld(777L, VfxEmitterComponent.MAX_TURBULENCE_STRENGTH,
                4, 5f, 200);
        for (VfxParticle p : particles(world)) {
            assertFinite(p);
            assertTrue(Math.abs(p.velocity.x) <= ParticleIntegrateSystem.MAX_PARTICLE_SPEED + EPS);
            assertTrue(Math.abs(p.velocity.y) <= ParticleIntegrateSystem.MAX_PARTICLE_SPEED + EPS);
            assertTrue(Math.abs(p.position.x) <= ParticleIntegrateSystem.MAX_PARTICLE_POSITION + EPS);
            assertTrue(Math.abs(p.position.y) <= ParticleIntegrateSystem.MAX_PARTICLE_POSITION + EPS);
        }
    }

    @Test
    public void accelMagnitudeIsBoundedByStrength() {
        ParticleEmitterDefinition definition = baseDefinition(42L);
        float strength = 75f;
        VfxEmitterComponent emitter = VfxEmitterComponent.from(definition, strength);
        for (int index = 0; index < 32; index++) {
            for (float age = 0f; age < 5f; age += 0.05f) {
                VfxParticle particle = new VfxParticle(index, age, 10f, new VfxVec2(0f, 0f),
                        new VfxVec2(0f, 0f), 0f, 0f, 1f, 1f, 1f, new VfxColor(1f, 1f, 1f, 1f));
                VfxVec2 accel = ParticleIntegrateSystem.turbulenceAccel(emitter, particle);
                assertTrue(Float.isFinite(accel.x));
                assertTrue(Float.isFinite(accel.y));
                float magnitude = (float) Math.hypot(accel.x, accel.y);
                assertTrue("magnitude " + magnitude + " must be <= strength " + strength,
                        magnitude <= strength + EPS);
                assertTrue(magnitude <= VfxEmitterComponent.MAX_TURBULENCE_STRENGTH + EPS);
            }
        }
    }

    @Test
    public void nonFiniteInputStrengthIsClampedToFiniteRange() {
        ParticleEmitterDefinition definition = baseDefinition(5L);
        assertTrue(Float.isFinite(VfxEmitterComponent.from(definition, Float.NaN).turbulenceStrength));
        assertEquals(0f, VfxEmitterComponent.from(definition, Float.NaN).turbulenceStrength, 0f);
        assertEquals(VfxEmitterComponent.MAX_TURBULENCE_STRENGTH,
                VfxEmitterComponent.from(definition, Float.POSITIVE_INFINITY).turbulenceStrength, 0f);
        assertEquals(0f, VfxEmitterComponent.from(definition, -5f).turbulenceStrength, 0f);
    }

    private static VfxParticle integrateOne(ParticleEmitterDefinition definition, float strength,
            VfxParticle particle, float dt) {
        PresentationWorld world = new PresentationWorld("one");
        EntityId entity = world.createEntity();
        world.put(entity, VfxEmitterComponent.class, VfxEmitterComponent.from(definition, strength));
        List<VfxParticle> particles = new ArrayList<VfxParticle>();
        particles.add(particle);
        world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(particles));
        new ParticleIntegrateSystem().run(world, new EcsTick(dt, 0L));
        return particles(world).get(0);
    }

    private static PresentationWorld turbulentWorld(long seed, float strength, int count, float dt,
            int steps) {
        ParticleEmitterDefinition definition = baseDefinition(seed);
        PresentationWorld world = new PresentationWorld("turbulence");
        EntityId entity = world.createEntity();
        world.put(entity, VfxEmitterComponent.class, VfxEmitterComponent.from(definition, strength));
        List<VfxParticle> particles = new ArrayList<VfxParticle>();
        for (int index = 0; index < count; index++) {
            particles.add(new VfxParticle(index, 0f, 1000f, new VfxVec2(0f, 0f),
                    new VfxVec2(1f, 0f), 0f, 0f, 1f, 1f, 1f, new VfxColor(1f, 1f, 1f, 1f)));
        }
        world.put(entity, VfxParticleBufferComponent.class, new VfxParticleBufferComponent(particles));
        ParticleIntegrateSystem integrate = new ParticleIntegrateSystem();
        for (int step = 0; step < steps; step++) integrate.run(world, new EcsTick(dt, step));
        return world;
    }

    private static ParticleEmitterDefinition baseDefinition(long seed) {
        return new ParticleEmitterDefinition(1, 1000f, 0f, new VfxVec2(1f, 0f), 0f,
                new VfxRange(1f, 1f), new VfxVec2(-2f, -3f), new VfxRange(1f, 1f),
                null, null, null, new VfxRange(0f, 0f), null, null, seed);
    }

    private static List<VfxParticle> particles(PresentationWorld world) {
        EntityId entity = world.query(VfxParticleBufferComponent.class).get(0);
        return world.get(entity, VfxParticleBufferComponent.class).particles;
    }

    private static boolean anyPositionDiffers(List<VfxParticle> a, List<VfxParticle> b) {
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).position.x != b.get(i).position.x
                    || a.get(i).position.y != b.get(i).position.y) return true;
        }
        return false;
    }

    private static void assertFinite(VfxParticle p) {
        assertTrue(Float.isFinite(p.position.x));
        assertTrue(Float.isFinite(p.position.y));
        assertTrue(Float.isFinite(p.velocity.x));
        assertTrue(Float.isFinite(p.velocity.y));
    }
}
