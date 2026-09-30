package artframework.sts1.render;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure bounds tests for the projected-entity index. The index is an accelerator for
 * presentation-entity removal, not authoritative state, so it must stay bounded even when a
 * host path never reports effect completion.
 */
public class TransientEffectRegistryTest {
    private TransientEffectIdentity identity(String id) {
        return new TransientEffectIdentity(id, "native.Effect", id.hashCode(), 1L);
    }

    @Test
    public void entityIndexIsBoundedByEntityCapacityAndEvictsOldest() {
        int cap = 3;
        TransientEffectRegistry registry = new TransientEffectRegistry(4, cap);

        for (int index = 0; index < cap + 5; index++) {
            registry.recordProjected("effect-" + index, "entity-" + index);
        }

        assertEquals("entity index can never exceed its capacity",
                Integer.valueOf(cap), Integer.valueOf(registry.activeCount()));
        assertEquals(Integer.valueOf(cap), Integer.valueOf(registry.entityCapacity()));
        assertNull("oldest projected identity is evicted first",
                registry.entity(identity("effect-0")));
        assertEquals("entity-5", registry.entity(identity("effect-5")));
    }

    @Test
    public void evictedIndexEntryStillRequestsPresentationEntityRemoval() {
        int cap = 1;
        TransientEffectRegistry registry = new TransientEffectRegistry(4, cap);

        registry.recordProjected("evicted", "entity-evicted");
        registry.recordProjected("survivor", "entity-survivor");

        List<TransientEffectRegistry.PendingProjection> drained =
                registry.drainPendingProjections();
        assertEquals(Integer.valueOf(1), Integer.valueOf(drained.size()));
        assertEquals("evicted", drained.get(0).instanceId);
        assertTrue("eviction must still remove the orphaned presentation entity",
                drained.get(0).removal);
    }

    @Test
    public void cleanupRemovesTrackedEntityAndCapacityDoesNotAffectExplicitRemoval() {
        TransientEffectRegistry registry = new TransientEffectRegistry(4, 2);
        registry.recordProjected("one", "entity-one");
        registry.recordProjected("two", "entity-two");

        registry.cleanup(identity("one"));

        assertEquals(Integer.valueOf(1), Integer.valueOf(registry.activeCount()));
        assertNull(registry.entity(identity("one")));
        assertEquals("entity-two", registry.entity(identity("two")));
        boolean sawRemoval = false;
        for (TransientEffectRegistry.PendingProjection projection
                : registry.drainPendingProjections()) {
            if ("one".equals(projection.instanceId) && projection.removal) sawRemoval = true;
        }
        assertTrue(sawRemoval);
    }

    @Test
    public void defaultCapacitiesAreGenerousAndPositive() {
        TransientEffectRegistry registry = new TransientEffectRegistry();
        assertTrue(TransientEffectRegistry.DEFAULT_ENTITY_CAPACITY >= 1024);
        assertEquals(Integer.valueOf(TransientEffectRegistry.DEFAULT_ENTITY_CAPACITY),
                Integer.valueOf(registry.entityCapacity()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(registry.activeCount()));
    }

    @Test
    public void capEvictionRetainsExactlyTheCapacityAndDropsEvictedIdentity() {
        int cap = 2;
        TransientEffectRegistry registry = new TransientEffectRegistry(4, cap);
        for (int index = 0; index < cap + 3; index++) {
            registry.recordProjected("effect-" + index, "entity-" + index);
        }

        assertEquals("exactly the capacity is retained after overflow",
                Integer.valueOf(cap), Integer.valueOf(registry.activeCount()));
        assertNull("the evicted identity no longer resolves",
                registry.entity(identity("effect-0")));
        assertEquals("the newest identity resolves", "entity-4",
                registry.entity(identity("effect-4")));
    }
}
