package artframework.context;

import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NativeIntentLifecycleSystemTest {
    private final EcsSystem system = new NativeIntentLifecycleSystem();

    @Test public void eventsAdvanceLifecycleAndAreConsumed() {
        PresentationWorld world = new PresentationWorld("intent-test");
        EntityId entity = world.createEntity();

        advance(world, entity, "play_card", NativeIntentLifecycleComponent.State.REQUESTED, "");
        assertState(world, entity, NativeIntentLifecycleComponent.State.REQUESTED);
        advance(world, entity, "play_card", NativeIntentLifecycleComponent.State.SENT, "");
        assertState(world, entity, NativeIntentLifecycleComponent.State.SENT);
        advance(world, entity, "play_card", NativeIntentLifecycleComponent.State.EXECUTED, "accepted");
        assertState(world, entity, NativeIntentLifecycleComponent.State.EXECUTED);

        world.put(entity, NativeIntentObservationComponent.class,
                NativeIntentObservationComponent.available(7L, 3L, "combat"));
        run(world, 4L);

        assertState(world, entity, NativeIntentLifecycleComponent.State.CONFIRMED);
        assertNull(world.get(entity, NativeIntentLifecycleEventComponent.class));
        assertEquals(7L, world.get(entity, NativeIntentObservationComponent.class).frameId);
    }

    @Test public void unavailableAuthorityFrameFailsExecutedIntent() {
        PresentationWorld world = new PresentationWorld("intent-test");
        EntityId entity = world.createEntity();
        advance(world, entity, "play_card", NativeIntentLifecycleComponent.State.EXECUTED, "accepted");

        world.put(entity, NativeIntentObservationComponent.class,
                NativeIntentObservationComponent.unavailable(8L, "authority frame unavailable"));
        run(world, 2L);

        assertState(world, entity, NativeIntentLifecycleComponent.State.FAILED);
        assertEquals("authority frame unavailable",
                world.get(entity, NativeIntentLifecycleComponent.class).message);
    }

    @Test public void oldObservationCannotConfirmANewIntent() {
        PresentationWorld world = new PresentationWorld("intent-test");
        EntityId entity = world.createEntity();
        world.put(entity, NativeIntentObservationComponent.class,
                NativeIntentObservationComponent.available(5L, 2L, "combat"));

        advance(world, entity, "begin_drag", NativeIntentLifecycleComponent.State.REQUESTED, "");
        advance(world, entity, "begin_drag", NativeIntentLifecycleComponent.State.EXECUTED, "accepted");

        assertState(world, entity, NativeIntentLifecycleComponent.State.EXECUTED);
        assertNull(world.get(entity, NativeIntentObservationComponent.class));
    }

    private void advance(PresentationWorld world, EntityId entity, String name,
            NativeIntentLifecycleComponent.State state, String message) {
        world.put(entity, NativeIntentLifecycleEventComponent.class,
                new NativeIntentLifecycleEventComponent(name, state, message));
        run(world, state.ordinal());
    }

    private void run(PresentationWorld world, long sequence) {
        EcsPipeline.run(world, new EcsTick(0f, sequence), Collections.singletonList(system));
    }

    private static void assertState(PresentationWorld world, EntityId entity,
            NativeIntentLifecycleComponent.State expected) {
        assertEquals(expected, world.get(entity, NativeIntentLifecycleComponent.class).state);
    }
}
