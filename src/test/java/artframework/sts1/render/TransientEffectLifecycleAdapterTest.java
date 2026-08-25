package artframework.sts1.render;

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

    @After
    public void tearDown() {
        registry.clear();
        artframework.presentation.PresentationRegistry.resetForTests();
    }

    private TransientEffectIdentity identity(String id) {
        return new TransientEffectIdentity(id, "native.Effect", id.hashCode(), 1L);
    }

    @Test
    public void renderCreatesOneEntityAndCompletionRemovesIt() {
        TransientEffectIdentity identity = identity("a");
        adapter.render(identity, 7L, "render");

        assertEquals(Integer.valueOf(1), Integer.valueOf(registry.activeCount()));
        assertTrue(Sts1NativePresentationAdapter.hasEntity("effect:" + identity.instanceId));

        adapter.update(identity, true);
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

        adapter.cancel(first);
        assertEquals(Integer.valueOf(1), Integer.valueOf(registry.activeCount()));
        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:" + first.instanceId));
        assertTrue(Sts1NativePresentationAdapter.hasEntity("effect:" + second.instanceId));
    }

    @Test
    public void cleanupAllCountsUnfinishedInstancesAsLeakedAndRemovesEntities() {
        adapter.render(identity("leaked"), 1L, "render");
        adapter.cleanupAll();

        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.leakedCount()));
        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:leaked"));
    }
}
