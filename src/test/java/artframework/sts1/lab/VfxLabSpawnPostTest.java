package artframework.sts1.lab;

import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Focused tests for the console-thread-vs-{@code AbstractDungeon.update()} race hardening: the live
 * {@link VfxLabSpawn} sink must POST the structural append onto the game/render thread (via the
 * injectable {@link VfxLabSpawn.PostRunner} seam) rather than mutating the live list inline, and
 * must fail open to the guarded direct append when no runner/app is available. The actual native
 * append is exercised through the {@link VfxLabSpawn.LiveAppender} seam so the
 * {@code AbstractDungeon} static initializer (which needs a live game context) is never triggered.
 */
public class VfxLabSpawnPostTest {

    @After
    public void tearDown() {
        VfxLabSpawn.resetForTests();
    }

    @Test
    public void addPostsTheAppendAndDoesNotApplyItInline() {
        RecordingAppender live = new RecordingAppender();
        VfxLabSpawn.setAppenderForTests(live);
        CapturingPostRunner runner = new CapturingPostRunner();
        VfxLabSpawn.setPostRunnerForTests(runner);

        AbstractGameEffect effect = new StubEffect();
        VfxLabSpawn.Queue queue = VfxLabSpawn.defaultQueueForTests();

        assertTrue("a scheduled append reports true", queue.add(effect));
        assertEquals("the append must be posted, not performed inline", 1, runner.posted.size());
        assertEquals("the live append must not run on the console thread", 0, live.appended.size());

        // Draining the posted Runnable is what applies the append on the game thread.
        runner.posted.get(0).run();
        assertEquals(1, live.appended.size());
        assertSame(effect, live.appended.get(0));
    }

    @Test
    public void addFallsBackToDirectAppendWhenNoRunnerAndNoApp() {
        RecordingAppender live = new RecordingAppender();
        VfxLabSpawn.setAppenderForTests(live);
        // No PostRunner is injected and Gdx.app is null headlessly: the guarded direct append runs.
        AbstractGameEffect effect = new StubEffect();

        assertTrue(VfxLabSpawn.defaultQueueForTests().add(effect));
        assertEquals("fallback appends directly on this thread", 1, live.appended.size());
        assertSame(effect, live.appended.get(0));
    }

    @Test
    public void addFallsBackToDirectAppendWhenTheRunnerThrows() {
        RecordingAppender live = new RecordingAppender();
        VfxLabSpawn.setAppenderForTests(live);
        VfxLabSpawn.setPostRunnerForTests(new VfxLabSpawn.PostRunner() {
            @Override
            public void post(Runnable runnable) {
                throw new IllegalStateException("no app thread");
            }
        });
        AbstractGameEffect effect = new StubEffect();

        assertTrue("a throwing post must fail open to the direct append",
                VfxLabSpawn.defaultQueueForTests().add(effect));
        assertEquals(1, live.appended.size());
        assertSame(effect, live.appended.get(0));
    }

    @Test
    public void addReturnsFalseWhenNoRunnerAndAppendDeclines() {
        // No runner injected and the guarded append declines (off-game): add reports false, no throw.
        VfxLabSpawn.setAppenderForTests(new DecliningAppender());
        assertFalse(VfxLabSpawn.defaultQueueForTests().add(new StubEffect()));
    }

    @Test
    public void addWithRunnerReportsScheduledEvenWhenAppendDeclines() {
        // No live context at all: the runner owns the guard, so the add is still "scheduled" (true);
        // draining the posted work against a declining appender must stay fail-open.
        VfxLabSpawn.setAppenderForTests(new DecliningAppender());
        CapturingPostRunner runner = new CapturingPostRunner();
        VfxLabSpawn.setPostRunnerForTests(runner);

        assertTrue(VfxLabSpawn.defaultQueueForTests().add(new StubEffect()));
        assertEquals(1, runner.posted.size());
        try {
            runner.posted.get(0).run();
        } catch (Throwable thrown) {
            fail("draining against a declining appender must not throw: " + thrown);
        }
    }

    @Test
    public void addRejectsNullEffectWithoutPosting() {
        CapturingPostRunner runner = new CapturingPostRunner();
        VfxLabSpawn.setPostRunnerForTests(runner);
        assertFalse(VfxLabSpawn.defaultQueueForTests().add(null));
        assertEquals(0, runner.posted.size());
    }

    @Test
    public void retireMatchingOnlyMarksDoneWithoutStructuralMutation() {
        AbstractGameEffect aura = new StubEffect();
        AbstractGameEffect other = new StubEffect();
        List<AbstractGameEffect> live = new ArrayList<AbstractGameEffect>();
        live.add(aura);
        live.add(other);

        int retired = invokeRetireIn(live, effect -> effect == aura);

        assertEquals(1, retired);
        assertTrue("the matched effect is flagged done", aura.isDone);
        assertFalse("the unmatched effect is untouched", other.isDone);
        assertEquals("retirement must not structurally mutate the live list", 2, live.size());
        assertSame("the matched effect is still present in the list", aura, live.get(0));
    }

    /** Drives the real private {@code retireIn} (the live retirement path) without native statics. */
    private static int invokeRetireIn(
            List<AbstractGameEffect> list,
            java.util.function.Predicate<AbstractGameEffect> match) {
        try {
            java.lang.reflect.Method method = VfxLabSpawn.class
                    .getDeclaredMethod("retireIn", List.class, java.util.function.Predicate.class);
            method.setAccessible(true);
            return (Integer) method.invoke(null, list, match);
        } catch (Exception failure) {
            throw new AssertionError("could not invoke retireIn", failure);
        }
    }

    /** Captures posted Runnables so a test can assert scheduling and drain them manually. */
    private static final class CapturingPostRunner implements VfxLabSpawn.PostRunner {
        final List<Runnable> posted = new ArrayList<Runnable>();

        @Override
        public void post(Runnable runnable) {
            assertNotNull(runnable);
            posted.add(runnable);
        }
    }

    /** Records appended effects without touching a native list. */
    private static final class RecordingAppender implements VfxLabSpawn.LiveAppender {
        final List<AbstractGameEffect> appended = new ArrayList<AbstractGameEffect>();

        @Override
        public boolean append(AbstractGameEffect effect) {
            appended.add(effect);
            return true;
        }
    }

    /** Mirrors the live append when no game context exists. */
    private static final class DecliningAppender implements VfxLabSpawn.LiveAppender {
        @Override
        public boolean append(AbstractGameEffect effect) {
            return false;
        }
    }

    /** Minimal concrete effect; never rendered or updated by these tests. */
    private static final class StubEffect extends AbstractGameEffect {
        @Override
        public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {}

        @Override
        public void dispose() {}
    }
}
