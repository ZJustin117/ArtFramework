package artframework.sts1.lab;

import artframework.sts1.render.VfxClaimPolicy;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VfxLabSpawnTest {

    @After
    public void tearDown() {
        VfxLabSpawn.resetForTests();
    }

    @Test
    public void classNameForMapsAliasesToClaimConstants() {
        assertEquals(
                VfxClaimPolicy.STANCE_AURA_EFFECT, VfxLabSpawn.classNameFor("stance"));
        assertEquals(VfxClaimPolicy.STANCE_AURA_EFFECT, VfxLabSpawn.classNameFor("aura"));
        assertEquals(VfxClaimPolicy.WRATH_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("wrath"));
        assertEquals(
                VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("divinity"));
        assertEquals(VfxClaimPolicy.CALM_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("calm"));
        assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE,
                VfxLabSpawn.classNameFor("divinitychange"));
        assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE,
                VfxLabSpawn.classNameFor("dsc"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, VfxLabSpawn.classNameFor("flare"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, VfxLabSpawn.classNameFor("lightflare"));
        assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, VfxLabSpawn.classNameFor("flash"));
        assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, VfxLabSpawn.classNameFor("flashatk"));
    }

    @Test
    public void classNameForIsCaseInsensitiveAndTrims() {
        assertEquals(
                VfxClaimPolicy.STANCE_AURA_EFFECT, VfxLabSpawn.classNameFor("  StAnCe "));
        assertEquals(VfxClaimPolicy.WRATH_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("WRATH"));
        assertEquals(VfxClaimPolicy.CALM_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("  CaLm "));
        assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE,
                VfxLabSpawn.classNameFor("  DiViNiTyChAnGe "));
        assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE,
                VfxLabSpawn.classNameFor(" DSC "));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, VfxLabSpawn.classNameFor("  FlArE "));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, VfxLabSpawn.classNameFor("LIGHTFLARE"));
        assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, VfxLabSpawn.classNameFor("  FlAsH "));
        assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, VfxLabSpawn.classNameFor("FLASHATK"));
    }

    @Test
    public void classNameForRejectsUnknownAndNull() {
        assertNull(VfxLabSpawn.classNameFor(null));
        assertNull(VfxLabSpawn.classNameFor(""));
        assertNull(VfxLabSpawn.classNameFor("   "));
        assertNull(VfxLabSpawn.classNameFor("bogus"));
        assertNull(VfxLabSpawn.classNameFor("StanceAura"));
    }

    @Test
    public void spawnRejectsNullKind() {
        RecordingQueue queue = new RecordingQueue();
        VfxLabSpawn.setQueueForTests(queue);
        assertEquals(0, VfxLabSpawn.spawn(null, 3));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnRejectsUnknownKind() {
        RecordingQueue queue = new RecordingQueue();
        VfxLabSpawn.setQueueForTests(queue);
        assertEquals(0, VfxLabSpawn.spawn("bogus", 3));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnRejectsNonPositiveCount() {
        RecordingQueue queue = new RecordingQueue();
        VfxLabSpawn.setQueueForTests(queue);
        assertEquals(0, VfxLabSpawn.spawn("stance", 0));
        assertEquals(0, VfxLabSpawn.spawn("stance", -5));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnNeverThrowsWithoutLiveGameContext() {
        VfxLabSpawn.resetForTests();
        // Headless there is no libGDX asset context, so construction fails open to 0 rather than
        // throwing. The contract under test is "returns a non-negative count and never throws".
        int queued = VfxLabSpawn.spawn("wrath", 3);
        org.junit.Assert.assertTrue("expected fail-open count >= 0 but was " + queued, queued >= 0);
        assertEquals(0, VfxLabSpawn.spawn("divinity", 2));
    }

    @Test
    public void spawnDoesNotPropagateThrowingQueue() {
        VfxLabSpawn.setQueueForTests(new ThrowingQueue());
        try {
            int queued = VfxLabSpawn.spawn("stance", 4);
            org.junit.Assert.assertTrue("expected count >= 0 but was " + queued, queued >= 0);
            assertEquals(0, queued);
        } catch (RuntimeException unexpected) {
            org.junit.Assert.fail("spawn propagated: " + unexpected);
        }
    }

    @Test
    public void spawnDoesNotCountEffectsTheQueueDeclines() {
        // A queue whose add reports false (the live DungeonQueue no-context behavior) must not
        // over-report: queued stays 0 even though construction succeeded.
        VfxLabSpawn.setQueueForTests(new DecliningQueue());
        VfxLabSpawn.setFactoryForTests(new StubFactory());
        assertEquals(0, VfxLabSpawn.spawn("wrath", 5));
    }

    @Test
    public void spawnHappyPathQueuesExactlyClampedCount() {
        // The factory seam supplies a stub effect so the GL-backed constructors are bypassed; a
        // RecordingQueue whose add returns true then exercises the clamp/count arithmetic.
        RecordingQueue queue = new RecordingQueue();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(new StubFactory());

        assertEquals(5, VfxLabSpawn.spawn("wrath", 5));
        assertEquals(5, queue.added.size());

        assertEquals(VfxLabSpawn.MAX_COUNT, VfxLabSpawn.spawn("divinity", 99));
        assertEquals(5 + VfxLabSpawn.MAX_COUNT, queue.added.size());

        assertEquals(2, VfxLabSpawn.spawn("stance", 2));
        assertEquals(5 + VfxLabSpawn.MAX_COUNT + 2, queue.added.size());
    }

    @Test
    public void spawnHappyPathQueuesCalmThroughTheFactorySeam() {
        // Calm is the 4th claimable FQN. The factory seam captures the requested FQN, so the
        // calm -> CALM_PARTICLE_EFFECT mapping is genuinely asserted without the GL-backed
        // CalmParticleEffect constructor.
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn("calm", 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(VfxClaimPolicy.CALM_PARTICLE_EFFECT, fqn);
        }
    }

    @Test
    public void spawnHappyPathQueuesDivinityStanceChangeThroughTheFactorySeam() {
        // DivinityStanceChangeParticle is the 5th claimable FQN; the capturing factory proves the
        // divinitychange alias requests exactly that FQN without touching its GL-backed constructor.
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn("divinitychange", 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE, fqn);
        }
    }

    @Test
    public void spawnHappyPathQueuesLightFlareThroughTheFactorySeam() {
        // The first non-aura (vfx-scene-world) claimable FQN; the capturing factory proves the
        // flare/lightflare aliases request exactly that FQN without touching its GL-backed
        // constructor.
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn("flare", 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, fqn);
        }

        RecordingQueue queue2 = new RecordingQueue();
        CapturingFactory factory2 = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue2);
        VfxLabSpawn.setFactoryForTests(factory2);

        assertEquals(3, VfxLabSpawn.spawn("lightflare", 3));
        assertEquals(3, factory2.requested.size());
        for (String fqn : factory2.requested) {
            assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, fqn);
        }
    }

    @Test
    public void spawnHappyPathQueuesFlashAtkImgThroughTheFactorySeam() {
        // The first vfx-combat claimable FQN; the capturing factory proves the flash/flashatk aliases
        // request exactly that FQN without touching its GL/image-backed constructor.
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn("flash", 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, fqn);
        }

        RecordingQueue queue2 = new RecordingQueue();
        CapturingFactory factory2 = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue2);
        VfxLabSpawn.setFactoryForTests(factory2);

        assertEquals(3, VfxLabSpawn.spawn("flashatk", 3));
        assertEquals(3, factory2.requested.size());
        for (String fqn : factory2.requested) {
            assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, fqn);
        }
    }

    @Test
    public void clearReturnsQueueRemovalCount() {
        VfxLabSpawn.setQueueForTests(new FixedCountQueue(7));
        assertEquals(7, VfxLabSpawn.clear());
    }

    @Test
    public void clearReturnsZeroWhenQueueThrows() {
        VfxLabSpawn.setQueueForTests(new ThrowingQueue());
        assertEquals(0, VfxLabSpawn.clear());
    }

    /** Records adds; removal count is derived from the recorded added count. */
    private static final class RecordingQueue implements VfxLabSpawn.Queue {
        final List<AbstractGameEffect> added = new ArrayList<AbstractGameEffect>();

        @Override
        public boolean add(AbstractGameEffect effect) {
            added.add(effect);
            return true;
        }

        @Override
        public int removeMatching(Predicate<AbstractGameEffect> match) {
            return 0;
        }
    }

    /** Accepts nothing: mirrors the live DungeonQueue when no game context exists. */
    private static final class DecliningQueue implements VfxLabSpawn.Queue {
        @Override
        public boolean add(AbstractGameEffect effect) {
            return false;
        }

        @Override
        public int removeMatching(Predicate<AbstractGameEffect> match) {
            return 0;
        }
    }

    /** Supplies stub effects so construction never needs a live GL/asset context. */
    private static final class StubFactory implements VfxLabSpawn.EffectFactory {
        @Override
        public AbstractGameEffect create(String fqn) {
            return new StubEffect();
        }
    }

    /** Supplies stub effects and records each requested FQN so the mapping can be asserted. */
    private static final class CapturingFactory implements VfxLabSpawn.EffectFactory {
        final List<String> requested = new ArrayList<String>();

        @Override
        public AbstractGameEffect create(String fqn) {
            requested.add(fqn);
            return new StubEffect();
        }
    }

    /** Minimal concrete effect; the stubs never render or update it. */
    private static final class StubEffect extends AbstractGameEffect {
        @Override
        public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {}

        @Override
        public void dispose() {}
    }

    private static final class FixedCountQueue implements VfxLabSpawn.Queue {
        private final int removalCount;

        private FixedCountQueue(int removalCount) {
            this.removalCount = removalCount;
        }

        @Override
        public boolean add(AbstractGameEffect effect) {
            return true;
        }

        @Override
        public int removeMatching(Predicate<AbstractGameEffect> match) {
            return removalCount;
        }
    }

    private static final class ThrowingQueue implements VfxLabSpawn.Queue {
        @Override
        public boolean add(AbstractGameEffect effect) {
            throw new IllegalStateException("no context");
        }

        @Override
        public int removeMatching(Predicate<AbstractGameEffect> match) {
            throw new IllegalStateException("no context");
        }
    }
}
