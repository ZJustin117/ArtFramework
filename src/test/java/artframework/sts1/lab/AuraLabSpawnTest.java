package artframework.sts1.lab;

import artframework.sts1.render.AuraClaimPolicy;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AuraLabSpawnTest {

    @After
    public void tearDown() {
        AuraLabSpawn.resetForTests();
    }

    @Test
    public void classNameForMapsAliasesToClaimConstants() {
        assertEquals(
                AuraClaimPolicy.STANCE_AURA_EFFECT, AuraLabSpawn.classNameFor("stance"));
        assertEquals(AuraClaimPolicy.STANCE_AURA_EFFECT, AuraLabSpawn.classNameFor("aura"));
        assertEquals(AuraClaimPolicy.WRATH_PARTICLE_EFFECT, AuraLabSpawn.classNameFor("wrath"));
        assertEquals(
                AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT, AuraLabSpawn.classNameFor("divinity"));
    }

    @Test
    public void classNameForIsCaseInsensitiveAndTrims() {
        assertEquals(
                AuraClaimPolicy.STANCE_AURA_EFFECT, AuraLabSpawn.classNameFor("  StAnCe "));
        assertEquals(AuraClaimPolicy.WRATH_PARTICLE_EFFECT, AuraLabSpawn.classNameFor("WRATH"));
    }

    @Test
    public void classNameForRejectsUnknownAndNull() {
        assertNull(AuraLabSpawn.classNameFor(null));
        assertNull(AuraLabSpawn.classNameFor(""));
        assertNull(AuraLabSpawn.classNameFor("   "));
        assertNull(AuraLabSpawn.classNameFor("bogus"));
        assertNull(AuraLabSpawn.classNameFor("StanceAura"));
    }

    @Test
    public void spawnRejectsNullKind() {
        RecordingQueue queue = new RecordingQueue();
        AuraLabSpawn.setQueueForTests(queue);
        assertEquals(0, AuraLabSpawn.spawn(null, 3));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnRejectsUnknownKind() {
        RecordingQueue queue = new RecordingQueue();
        AuraLabSpawn.setQueueForTests(queue);
        assertEquals(0, AuraLabSpawn.spawn("bogus", 3));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnRejectsNonPositiveCount() {
        RecordingQueue queue = new RecordingQueue();
        AuraLabSpawn.setQueueForTests(queue);
        assertEquals(0, AuraLabSpawn.spawn("stance", 0));
        assertEquals(0, AuraLabSpawn.spawn("stance", -5));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnNeverThrowsWithoutLiveGameContext() {
        AuraLabSpawn.resetForTests();
        // Headless there is no libGDX asset context, so construction fails open to 0 rather than
        // throwing. The contract under test is "returns a non-negative count and never throws".
        int queued = AuraLabSpawn.spawn("wrath", 3);
        org.junit.Assert.assertTrue("expected fail-open count >= 0 but was " + queued, queued >= 0);
        assertEquals(0, AuraLabSpawn.spawn("divinity", 2));
    }

    @Test
    public void spawnDoesNotPropagateThrowingQueue() {
        AuraLabSpawn.setQueueForTests(new ThrowingQueue());
        try {
            int queued = AuraLabSpawn.spawn("stance", 4);
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
        AuraLabSpawn.setQueueForTests(new DecliningQueue());
        AuraLabSpawn.setFactoryForTests(new StubFactory());
        assertEquals(0, AuraLabSpawn.spawn("wrath", 5));
    }

    @Test
    public void spawnHappyPathQueuesExactlyClampedCount() {
        // The factory seam supplies a stub effect so the GL-backed constructors are bypassed; a
        // RecordingQueue whose add returns true then exercises the clamp/count arithmetic.
        RecordingQueue queue = new RecordingQueue();
        AuraLabSpawn.setQueueForTests(queue);
        AuraLabSpawn.setFactoryForTests(new StubFactory());

        assertEquals(5, AuraLabSpawn.spawn("wrath", 5));
        assertEquals(5, queue.added.size());

        assertEquals(AuraLabSpawn.MAX_COUNT, AuraLabSpawn.spawn("divinity", 99));
        assertEquals(5 + AuraLabSpawn.MAX_COUNT, queue.added.size());

        assertEquals(2, AuraLabSpawn.spawn("stance", 2));
        assertEquals(5 + AuraLabSpawn.MAX_COUNT + 2, queue.added.size());
    }

    @Test
    public void clearReturnsQueueRemovalCount() {
        AuraLabSpawn.setQueueForTests(new FixedCountQueue(7));
        assertEquals(7, AuraLabSpawn.clear());
    }

    @Test
    public void clearReturnsZeroWhenQueueThrows() {
        AuraLabSpawn.setQueueForTests(new ThrowingQueue());
        assertEquals(0, AuraLabSpawn.clear());
    }

    /** Records adds; removal count is derived from the recorded added count. */
    private static final class RecordingQueue implements AuraLabSpawn.Queue {
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
    private static final class DecliningQueue implements AuraLabSpawn.Queue {
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
    private static final class StubFactory implements AuraLabSpawn.EffectFactory {
        @Override
        public AbstractGameEffect create(String fqn) {
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

    private static final class FixedCountQueue implements AuraLabSpawn.Queue {
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

    private static final class ThrowingQueue implements AuraLabSpawn.Queue {
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
