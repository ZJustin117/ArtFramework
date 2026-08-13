package artframework.ecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EcsPipelineTest {
    public static final class Count {
        final int value;
        Count(int value) { this.value = value; }
    }

    @Test public void pipelineRunsStatelessSystemsInDeclaredOrder() {
        PresentationWorld world = new PresentationWorld("test");
        EntityId entity = world.createEntity();
        world.put(entity, Count.class, new Count(1));
        final List<String> calls = new ArrayList<String>();

        EcsSystem increment = new EcsSystem() {
            @Override public void run(PresentationWorld current, EcsTick tick) {
                calls.add("increment:" + tick.sequence);
                for (EntityId id : current.query(Count.class)) {
                    current.put(id, Count.class, new Count(current.get(id, Count.class).value + 1));
                }
            }
        };
        EcsSystem observe = new EcsSystem() {
            @Override public void run(PresentationWorld current, EcsTick tick) {
                calls.add("observe:" + current.get(entity, Count.class).value);
            }
        };

        EcsPipeline.run(world, new EcsTick(0.25f, 7L), Arrays.asList(increment, observe));

        assertEquals(Arrays.asList("increment:7", "observe:2"), calls);
        assertEquals(2, world.get(entity, Count.class).value);
    }

    @Test public void entityIdentityContainsOnlyItsAllocatedValue() {
        PresentationWorld first = new PresentationWorld("first");
        PresentationWorld second = new PresentationWorld("second");
        EntityId one = first.createEntity();
        EntityId two = second.createEntity();

        assertTrue(one.value() > 0L);
        assertTrue(two.value() > one.value());
        assertEquals(Long.toString(one.value()), one.toString());
    }

    @Test public void systemsMayBeReusedWithoutRetainedExecutionState() {
        PresentationWorld world = new PresentationWorld("test");
        EntityId entity = world.createEntity();
        world.put(entity, Count.class, new Count(3));
        final EcsSystem system = new EcsSystem() {
            @Override public void run(PresentationWorld current, EcsTick tick) {
                for (EntityId id : current.query(Count.class)) {
                    current.put(id, Count.class, new Count(current.get(id, Count.class).value + 1));
                }
            }
        };

        EcsPipeline.run(world, new EcsTick(0f, 1L), Arrays.asList(system));
        EcsPipeline.run(world, new EcsTick(0f, 2L), Arrays.asList(system));

        assertEquals(5, world.get(entity, Count.class).value);
    }
}
