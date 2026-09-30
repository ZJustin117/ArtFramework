package artframework.sts1.lab;

import artframework.sts1.render.VfxClaimPolicy;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect;
import com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_M, VfxLabSpawn.classNameFor("flareM"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_M,
                VfxLabSpawn.classNameFor("lightflareM"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_L, VfxLabSpawn.classNameFor("flareL"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_L,
                VfxLabSpawn.classNameFor("lightflareL"));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L, VfxLabSpawn.classNameFor("torch"));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L,
                VfxLabSpawn.classNameFor("torchparticle"));
        assertEquals(VfxClaimPolicy.FIRE_BURST, VfxLabSpawn.classNameFor("fireburst"));
        assertEquals(VfxClaimPolicy.FIRE_BURST, VfxLabSpawn.classNameFor("fire"));
        assertEquals(VfxClaimPolicy.RED_FIRE_BURST, VfxLabSpawn.classNameFor("redfireburst"));
        assertEquals(VfxClaimPolicy.RED_FIRE_BURST, VfxLabSpawn.classNameFor("redfire"));
        assertEquals(VfxClaimPolicy.SMOKE_BLUR, VfxLabSpawn.classNameFor("smokeblur"));
        assertEquals(VfxClaimPolicy.SMOKE_BLUR, VfxLabSpawn.classNameFor("smoke"));
        assertEquals(VfxClaimPolicy.CEILING_DUST, VfxLabSpawn.classNameFor("ceilingdust"));
        assertEquals(VfxClaimPolicy.CEILING_DUST, VfxLabSpawn.classNameFor("dust"));
        assertEquals(VfxClaimPolicy.NEMESIS_FIRE, VfxLabSpawn.classNameFor("nemesisfire"));
        assertEquals(VfxClaimPolicy.NEMESIS_FIRE, VfxLabSpawn.classNameFor("nemesis"));
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
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_M, VfxLabSpawn.classNameFor("  FlArEm "));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_M,
                VfxLabSpawn.classNameFor("LIGHTFLAREM"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_L, VfxLabSpawn.classNameFor("FLAREL"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_L,
                VfxLabSpawn.classNameFor("lightflarel"));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L, VfxLabSpawn.classNameFor("  ToRcH "));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L,
                VfxLabSpawn.classNameFor("TORCHPARTICLE"));
        assertEquals(VfxClaimPolicy.FIRE_BURST, VfxLabSpawn.classNameFor("  FiReBuRsT "));
        assertEquals(VfxClaimPolicy.FIRE_BURST, VfxLabSpawn.classNameFor("FIRE"));
        assertEquals(VfxClaimPolicy.RED_FIRE_BURST, VfxLabSpawn.classNameFor("  ReDfIrE "));
        assertEquals(VfxClaimPolicy.SMOKE_BLUR, VfxLabSpawn.classNameFor("SMOKEBLUR"));
        assertEquals(VfxClaimPolicy.SMOKE_BLUR, VfxLabSpawn.classNameFor("  SmOkE "));
        assertEquals(VfxClaimPolicy.CEILING_DUST, VfxLabSpawn.classNameFor("CEILINGDUST"));
        assertEquals(VfxClaimPolicy.CEILING_DUST, VfxLabSpawn.classNameFor("DUST"));
        assertEquals(VfxClaimPolicy.NEMESIS_FIRE, VfxLabSpawn.classNameFor("NEMESISFIRE"));
        assertEquals(VfxClaimPolicy.NEMESIS_FIRE, VfxLabSpawn.classNameFor("  NeMeSiS "));
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
    public void spawnHappyPathQueuesLaterSceneWorldMembersThroughTheFactorySeam() {
        // The three later vfx-scene-world claimable FQNs; the capturing factory proves each alias
        // requests exactly its FQN without touching the GL-backed constructors (the flare pair's
        // static imgs[] and TorchParticleLEffect's getImg() are null off-game).
        assertSpawnRequests("flareM", VfxClaimPolicy.SCENE_LIGHT_FLARE_M);
        assertSpawnRequests("lightflareM", VfxClaimPolicy.SCENE_LIGHT_FLARE_M);
        assertSpawnRequests("flareL", VfxClaimPolicy.SCENE_LIGHT_FLARE_L);
        assertSpawnRequests("lightflareL", VfxClaimPolicy.SCENE_LIGHT_FLARE_L);
        assertSpawnRequests("torch", VfxClaimPolicy.SCENE_TORCH_PARTICLE_L);
        assertSpawnRequests("torchparticle", VfxClaimPolicy.SCENE_TORCH_PARTICLE_L);
    }

    @Test
    public void spawnHappyPathQueuesTheNewestFiveMembersThroughTheFactorySeam() {
        // The two fire-burst and three ambient FQNs; the capturing factory proves each alias
        // requests exactly its FQN without touching the GL/image-backed constructors (the static
        // ImageMaster regions may be null off-game).
        assertSpawnRequests("fireburst", VfxClaimPolicy.FIRE_BURST);
        assertSpawnRequests("fire", VfxClaimPolicy.FIRE_BURST);
        assertSpawnRequests("redfireburst", VfxClaimPolicy.RED_FIRE_BURST);
        assertSpawnRequests("redfire", VfxClaimPolicy.RED_FIRE_BURST);
        assertSpawnRequests("smokeblur", VfxClaimPolicy.SMOKE_BLUR);
        assertSpawnRequests("smoke", VfxClaimPolicy.SMOKE_BLUR);
        assertSpawnRequests("ceilingdust", VfxClaimPolicy.CEILING_DUST);
        assertSpawnRequests("dust", VfxClaimPolicy.CEILING_DUST);
        assertSpawnRequests("nemesisfire", VfxClaimPolicy.NEMESIS_FIRE);
        assertSpawnRequests("nemesis", VfxClaimPolicy.NEMESIS_FIRE);
    }

    private static void assertSpawnRequests(String alias, String expectedFqn) {
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn(alias, 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(expectedFqn, fqn);
        }
    }

    @Test
    public void clearRetiresMatchingEffectsByFlagAndReturnsCount() {
        RetiringQueue queue = new RetiringQueue();
        AbstractGameEffect aura = claimable(StanceAuraEffect.class);
        AbstractGameEffect wrath = claimable(WrathParticleEffect.class);
        AbstractGameEffect other = new StubEffect();
        queue.add(aura);
        queue.add(wrath);
        queue.add(other);
        VfxLabSpawn.setQueueForTests(queue);

        assertEquals(2, VfxLabSpawn.clear());

        // Matching effects are retired through the game-native isDone flag, not structurally removed.
        assertTrue("matching aura should be marked done", aura.isDone);
        assertTrue("matching wrath should be marked done", wrath.isDone);
        assertFalse("non-claimable effect must not be retired", other.isDone);
        // No structural mutation: every effect is still present in the backing container.
        assertEquals(3, queue.backing.size());
    }

    @Test
    public void clearDoesNotRetireNonMatchingEffects() {
        RetiringQueue queue = new RetiringQueue();
        AbstractGameEffect other = new StubEffect();
        queue.add(other);
        VfxLabSpawn.setQueueForTests(queue);

        assertEquals(0, VfxLabSpawn.clear());
        assertFalse(other.isDone);
        assertEquals(1, queue.backing.size());
    }

    @Test
    public void clearReturnsZeroWhenQueueThrows() {
        VfxLabSpawn.setQueueForTests(new ThrowingQueue());
        assertEquals(0, VfxLabSpawn.clear());
    }

    /** Allocates a real claimable effect without running a GL-backed constructor. */
    private static AbstractGameEffect claimable(Class<? extends AbstractGameEffect> type) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            return (AbstractGameEffect) unsafe.getClass()
                    .getMethod("allocateInstance", Class.class)
                    .invoke(unsafe, type);
        } catch (Exception failure) {
            throw new AssertionError("could not allocate claimable effect " + type, failure);
        }
    }

    /**
     * Minimal live-container stub: keeps its backing list (so a test can assert no structural
     * mutation) and retires by setting the {@code isDone} flag exactly like {@code DungeonQueue}.
     */
    private static final class RetiringQueue implements VfxLabSpawn.Queue {
        final List<AbstractGameEffect> backing = new ArrayList<AbstractGameEffect>();

        @Override
        public boolean add(AbstractGameEffect effect) {
            backing.add(effect);
            return true;
        }

        @Override
        public int retireMatching(Predicate<AbstractGameEffect> match) {
            int retired = 0;
            for (AbstractGameEffect effect : backing) {
                boolean matched;
                try {
                    matched = match.test(effect);
                } catch (Throwable error) {
                    matched = false;
                }
                if (matched && effect != null) {
                    effect.isDone = true;
                    retired++;
                }
            }
            return retired;
        }
    }

    /** Records adds; retirement is a no-op. */
    private static final class RecordingQueue implements VfxLabSpawn.Queue {
        final List<AbstractGameEffect> added = new ArrayList<AbstractGameEffect>();

        @Override
        public boolean add(AbstractGameEffect effect) {
            added.add(effect);
            return true;
        }

        @Override
        public int retireMatching(Predicate<AbstractGameEffect> match) {
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
        public int retireMatching(Predicate<AbstractGameEffect> match) {
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

    /**
     * Minimal concrete effect; its own class name is not a claimable FQN, so it stands in for a
     * non-matching effect. The stubs never render or update it.
     */
    private static final class StubEffect extends AbstractGameEffect {
        @Override
        public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {}

        @Override
        public void dispose() {}
    }

    private static final class ThrowingQueue implements VfxLabSpawn.Queue {
        @Override
        public boolean add(AbstractGameEffect effect) {
            throw new IllegalStateException("no context");
        }

        @Override
        public int retireMatching(Predicate<AbstractGameEffect> match) {
            throw new IllegalStateException("no context");
        }
    }
}
