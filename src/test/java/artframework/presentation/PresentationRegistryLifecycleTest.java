package artframework.presentation;

import artframework.api.ArtFramework;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TE-26 / TE-27: registry reset clears ownership without replacing context instances, and a context
 * may mutate only the entities it keyed. Both boundaries were examined by the R25 closure review.
 */
public class PresentationRegistryLifecycleTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    /**
     * TE-26: reset must clear ownership but keep the same instance, because production holders pin
     * their context in a {@code static final} field. Replacing it would split writers from readers.
     */
    @Test
    public void resetClearsOwnershipWithoutReplacingOrUnregisteringScopes() {
        PresentationContext before = PresentationRegistry.context("te26-scope");
        EntityId entity = before.create(
                new PresentationKey("te26", "a"), "a", "control", "test");
        assertTrue(PresentationRegistry.scopes().contains("te26-scope"));

        PresentationRegistry.resetForTests();

        PresentationContext after = PresentationRegistry.context("te26-scope");
        assertSame("reset must not replace a registered context instance", before, after);
        assertTrue("reset must clear scope ownership", after.entities().isEmpty());
        assertFalse("reset must destroy the scope's entities",
                ArtEcs.world().contains(entity));
    }

    /** TE-26: only close(String) retires a scope, and it must fully unregister it. */
    @Test
    public void closeUnregistersTheScopeThatResetRetains() {
        PresentationRegistry.context("te26-close");
        assertTrue(PresentationRegistry.scopes().contains("te26-close"));

        PresentationRegistry.close("te26-close");

        assertNull("close must unregister the scope",
                PresentationRegistry.existingContext("te26-close"));
        assertFalse(PresentationRegistry.scopes().contains("te26-close"));
    }

    /** TE-27: an entity this scope never keyed must be rejected, not dereferenced. */
    @Test
    public void destroyRejectsAnUnkeyedSharedWorldEntity() {
        PresentationContext context = PresentationRegistry.context("te27-unkeyed");
        EntityId unkeyed = ArtEcs.world().createEntity();

        assertFalse("an unkeyed entity is not owned by this scope",
                context.destroy(unkeyed));
        assertTrue("rejecting must not destroy the foreign entity",
                ArtEcs.world().contains(unkeyed));
    }

    /** TE-27: one scope must not be able to destroy another scope's keyed entity. */
    @Test
    public void destroyRejectsAnotherScopesEntity() {
        PresentationContext owner = PresentationRegistry.context("te27-owner");
        PresentationContext other = PresentationRegistry.context("te27-other");
        PresentationKey key = new PresentationKey("c2", "owned");
        EntityId owned = owner.create(key, "owned", "control", "c2");

        assertFalse("a foreign scope must not destroy another scope's entity",
                other.destroy(owned));
        assertTrue(ArtEcs.world().contains(owned));
        assertEquals("the owning scope must still resolve its key", owned, owner.entity(key));

        assertTrue("the owning scope may still destroy it", owner.destroy(owned));
        assertFalse(ArtEcs.world().contains(owned));
    }

    @Test
    public void registryWorldCannotBeClosedThroughTheSharedWorldView() {
        try {
            PresentationRegistry.world().close();
            fail("registry-owned world must not be directly closable");
        } catch (IllegalStateException expected) {
            assertTrue(PresentationRegistry.world().isOpen());
        }

        PresentationContext context = PresentationRegistry.context("registry-close");
        EntityId entity = context.create(new PresentationKey("registry", "entity"),
                "entity", "control", "test");
        assertTrue(PresentationRegistry.world().contains(entity));
    }

    @Test
    public void entityLookupDoesNotRepairOwnershipAsAReadSideEffect() {
        PresentationContext context = PresentationRegistry.context("read-purity");
        PresentationKey key = new PresentationKey("read", "entity");
        EntityId entity = context.create(key, "entity", "control", "test");

        assertTrue(PresentationRegistry.world().destroyEntity(entity));
        assertNull(context.entity(key));

        try {
            context.create(key, "replacement", "control", "test");
            fail("lookup must not mutate ownership or permit duplicate recreation");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("duplicate presentation key"));
        }
    }
}
