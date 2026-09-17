package artframework.sts1.render;

import artframework.ecs.ArtEcs;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsTick;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransientEffectLifecycleAdapterTest {
    private final TransientEffectLedger ledger = new TransientEffectLedger();
    private final TransientEffectRegistry registry = new TransientEffectRegistry();
    private final TransientEffectLifecycleAdapter adapter =
            new TransientEffectLifecycleAdapter(ledger, registry);
    private final TransientEffectProjectionSystem projection =
            new TransientEffectProjectionSystem(registry);

    @After
    public void tearDown() {
        registry.clear();
        artframework.presentation.PresentationRegistry.resetForTests();
    }

    private TransientEffectIdentity identity(String id) {
        return new TransientEffectIdentity(id, "native.Effect", id.hashCode(), 1L);
    }

    private void drain() {
        EcsPipeline.run(ArtEcs.world(), new EcsTick(0f, 1L),
                Collections.singletonList(projection));
    }

    private void renderEntities(int count, String prefix) {
        for (int index = 0; index < count; index++) {
            adapter.render(identity(prefix + index), index, "render");
        }
        drain();
    }

    @Test
    public void renderCreatesOneEntityAndCompletionRemovesIt() {
        TransientEffectIdentity identity = identity("a");
        adapter.render(identity, 7L, "render");
        drain();

        assertEquals(Integer.valueOf(1), Integer.valueOf(registry.activeCount()));
        assertTrue(Sts1NativePresentationAdapter.hasEntity("effect:" + identity.instanceId));

        adapter.update(identity, true);
        drain();
        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:" + identity.instanceId));
        assertEquals(Integer.valueOf(0), ledger.probeSlice().get("active"));
    }

    @Test
    public void cancelRemovesOnlyTheCancelledInstance() {
        TransientEffectIdentity first = identity("first");
        TransientEffectIdentity second = identity("second");
        adapter.render(first, 1L, "render");
        adapter.render(second, 1L, "render");
        drain();

        adapter.cancel(first);
        drain();
        assertEquals(Integer.valueOf(1), Integer.valueOf(registry.activeCount()));
        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:" + first.instanceId));
        assertTrue(Sts1NativePresentationAdapter.hasEntity("effect:" + second.instanceId));
    }

    @Test
    public void cleanupAllCountsUnfinishedInstancesAsLeakedAndRemovesEntities() {
        adapter.render(identity("leaked"), 1L, "render");
        drain();
        assertTrue(Sts1NativePresentationAdapter.hasEntity("effect:leaked"));

        adapter.cleanupAll();
        drain();

        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.leakedCount()));
        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:leaked"));
    }

    @Test
    public void cleanupAllClearsEveryEntityWhenActiveCountExceedsPendingCapacity() {
        renderEntities(TransientEffectRegistry.DEFAULT_PENDING_CAPACITY + 1, "cleanup-all-");

        adapter.cleanupAll();
        drain();

        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(
                artframework.presentation.PresentationRegistry.context("nrcc-native")
                        .entities().size()));
        assertEquals(Integer.valueOf(0),
                Integer.valueOf(registry.drainPendingProjections().size()));
    }

    @Test
    public void recoveryCleanupDoesNotCountRecoveryOwnedEffectAsLeak() {
        adapter.render(identity("recovery"), 1L, "render");
        drain();
        adapter.cleanupForRecovery();
        drain();

        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.leakedCount()));
        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:recovery"));
    }

    @Test
    public void recoveryCleanupClearsEveryEntityWhenActiveCountExceedsPendingCapacity() {
        renderEntities(TransientEffectRegistry.DEFAULT_PENDING_CAPACITY + 1, "recovery-all-");

        adapter.cleanupForRecovery();
        drain();

        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(
                artframework.presentation.PresentationRegistry.context("nrcc-native")
                        .entities().size()));
        assertEquals(Integer.valueOf(0),
                Integer.valueOf(registry.drainPendingProjections().size()));
    }

    @Test
    public void recoveryCleanupClearsTransientEntitiesWithoutRemovingNativeSurfaceEntities() {
        Sts1NativePresentationAdapter.present(new NativeRenderInvocation(1L, 1L, "combat",
                "surface:unrelated", "NativeSurface", "render", "surface", "surface",
                artframework.component.Rect.ZERO));
        renderEntities(TransientEffectRegistry.DEFAULT_PENDING_CAPACITY + 1, "recovery-mixed-");

        adapter.cleanupForRecovery();
        drain();

        assertTrue(Sts1NativePresentationAdapter.hasEntity("surface:unrelated"));
        assertEquals(Integer.valueOf(1), Integer.valueOf(
                artframework.presentation.PresentationRegistry.context("nrcc-native")
                        .entities().size()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.drainPendingProjections().size()));
        for (int index = 0; index < TransientEffectRegistry.DEFAULT_PENDING_CAPACITY + 1; index++) {
            assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:recovery-mixed-" + index));
        }
    }

    @Test
    public void terminalRenderDoesNotRecreatePresentationEntity() {
        TransientEffectIdentity identity = identity("terminal-render");
        adapter.render(identity, 1L, "render");
        drain();
        adapter.complete(identity);
        drain();

        adapter.render(identity, 2L, "late-render");
        drain();

        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.unknownLifecycleCount()));
    }

    @Test
    public void recoveryRenderDoesNotRecreatePresentationEntity() {
        TransientEffectIdentity identity = identity("recovery-render");
        adapter.render(identity, 1L, "render");
        drain();
        adapter.cleanupForRecovery();
        drain();

        adapter.render(identity, 2L, "late-render");
        drain();

        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.unknownLifecycleCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.totalCount()));
    }

    @Test
    public void disposedAndEvictedRendersDoNotRecreatePresentationEntity() {
        TransientEffectLedger boundedLedger = new TransientEffectLedger(1);
        TransientEffectLifecycleAdapter boundedAdapter =
                new TransientEffectLifecycleAdapter(boundedLedger, registry);
        TransientEffectIdentity disposed = identity("disposed-render");
        boundedAdapter.render(disposed, 1L, "render");
        boundedAdapter.cancel(disposed);
        drain();
        boundedAdapter.render(disposed, 2L, "late-render");

        TransientEffectIdentity evicted = identity("evicted-render");
        boundedAdapter.render(evicted, 3L, "render");
        boundedAdapter.cancel(evicted);
        drain();
        TransientEffectIdentity newer = identity("newer-render");
        boundedAdapter.render(newer, 4L, "render");
        boundedAdapter.cancel(newer);
        drain();
        boundedAdapter.render(evicted, 5L, "late-render");
        drain();

        assertEquals(Integer.valueOf(0), Integer.valueOf(boundedLedger.activeCount()));
        assertEquals(Integer.valueOf(3), Integer.valueOf(boundedLedger.totalCount()));
        assertEquals(Integer.valueOf(2), Integer.valueOf(boundedLedger.unknownLifecycleCount()));
    }
}
