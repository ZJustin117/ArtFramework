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

    @Test public void entitiesHaveScopedStableIdsAndDeterministicOrder() {
        PresentationWorld world = new PresentationWorld("window-1");
        EntityId first = world.createEntity();
        EntityId second = world.createEntity();

        assertEquals("window-1", first.scope());
        assertEquals(1L, first.value());
        assertEquals(2L, second.value());
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
}
