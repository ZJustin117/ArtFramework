package artframework.ecs;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PresentationWorldTest {
    public static final class Identity {
        final String value;
        Identity(String value) { this.value = value; }
    }

    public static final class Position {
        final int x;
        Position(int x) { this.x = x; }
    }

    @Test public void entitiesHaveArtOwnedIdsAndDeterministicWorldOrder() {
        PresentationWorld world = new PresentationWorld("window-1");
        EntityId first = world.createEntity();
        EntityId second = world.createEntity();

        assertTrue(first.value() > 0L);
        assertTrue(second.value() > first.value());
        assertEquals(Arrays.asList(first, second), world.entities());
        assertEquals(first, first);
        assertFalse(first.equals(second));
    }

    @Test public void componentsCanBeAddedReplacedQueriedAndRemoved() {
        PresentationWorld world = new PresentationWorld("projection");
        EntityId entity = world.createEntity();
        world.put(entity, Identity.class, new Identity("card-1"));
        world.put(entity, Position.class, new Position(10));

        assertTrue(world.has(entity, Identity.class));
        assertEquals("card-1", world.get(entity, Identity.class).value);
        assertEquals(2, world.componentTypes(entity).size());

        world.put(entity, Position.class, new Position(20));
        assertEquals(20, world.get(entity, Position.class).x);
        assertNotNull(world.remove(entity, Identity.class));
        assertNull(world.get(entity, Identity.class));
        assertFalse(world.has(entity, Identity.class));
    }

    @Test public void destroyingEntityRemovesAllComponents() {
        PresentationWorld world = new PresentationWorld("projection");
        EntityId entity = world.createEntity();
        world.put(entity, Identity.class, new Identity("card-1"));

        assertTrue(world.destroyEntity(entity));
        assertFalse(world.contains(entity));
        assertEquals(0, world.entities().size());
    }

    @Test public void queryReturnsOnlyEntitiesWithEveryRequiredDataComponent() {
        PresentationWorld world = new PresentationWorld("projection");
        EntityId positioned = world.createEntity();
        EntityId identityOnly = world.createEntity();
        world.put(positioned, Identity.class, new Identity("card-1"));
        world.put(positioned, Position.class, new Position(5));
        world.put(identityOnly, Identity.class, new Identity("card-2"));

        assertEquals(Arrays.asList(positioned), world.query(Identity.class, Position.class));
    }

    @Test public void registeredScopesShareTheArtWorldButKeepDistinctKeys() {
        artframework.presentation.PresentationRegistry.resetForTests();
        artframework.presentation.PresentationContext first =
                artframework.presentation.PresentationRegistry.context("first");
        artframework.presentation.PresentationContext second =
                artframework.presentation.PresentationRegistry.context("second");

        assertTrue(first.world() == second.world());
        assertTrue(first.world() == artframework.presentation.PresentationRegistry.world());
        EntityId a = first.create(new artframework.presentation.PresentationKey("first", "a"),
                "a", "label", "test");
        EntityId b = second.create(new artframework.presentation.PresentationKey("second", "b"),
                "b", "label", "test");
        assertEquals(2, artframework.presentation.PresentationRegistry.world().entities().size());
        assertTrue(first.world().contains(a));
        assertTrue(second.world().contains(b));
        assertEquals(Arrays.asList(a), first.entities());
        assertEquals(Arrays.asList(b), second.entities());
    }

    @Test public void closeEndsWorldAndDisposesEntities() {
        PresentationWorld world = new PresentationWorld("tree-1");
        world.createEntity();
        world.close();

        assertFalse(world.isOpen());
        try {
            world.entities();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("closed world accepted an operation");
    }

    @Test public void closingRegisteredScopeDestroysOnlyItsOwnedEntities() {
        artframework.presentation.PresentationRegistry.resetForTests();
        artframework.presentation.PresentationContext first =
                artframework.presentation.PresentationRegistry.context("first");
        artframework.presentation.PresentationContext second =
                artframework.presentation.PresentationRegistry.context("second");
        EntityId firstEntity = first.create(new artframework.presentation.PresentationKey("first", "a"),
                "a", "label", "test");
        EntityId secondEntity = second.create(new artframework.presentation.PresentationKey("second", "b"),
                "b", "label", "test");

        artframework.presentation.PresentationRegistry.close("first");

        assertFalse(artframework.presentation.PresentationRegistry.world().contains(firstEntity));
        assertTrue(artframework.presentation.PresentationRegistry.world().contains(secondEntity));
        assertEquals(Arrays.asList(secondEntity), second.entities());
    }

    @Test public void registryResetClearsContextOwnershipWithoutReplacingContexts() {
        artframework.presentation.PresentationRegistry.resetForTests();
        artframework.presentation.PresentationContext before =
                artframework.presentation.PresentationRegistry.context("reset");
        EntityId entity = before.create(new artframework.presentation.PresentationKey("reset", "a"),
                "a", "label", "test");

        artframework.presentation.PresentationRegistry.resetForTests();
        artframework.presentation.PresentationContext after =
                artframework.presentation.PresentationRegistry.context("reset");

        assertTrue(before == after);
        assertFalse(after.world().contains(entity));
        assertTrue(after.entities().isEmpty());
    }
}
