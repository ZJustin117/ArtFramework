package artframework.context;

import artframework.core.SignalGroups;
import artframework.core.SignalDecision;
import artframework.core.SignalListener;
import artframework.core.UiSignal;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurfaceIntentExecutionSystemTest {
    @After public void tearDown() { SignalGroups.resetForTests(); }

    @Test public void consumesOneShotRequestAndRecordsResult() {
        PresentationWorld world = new PresentationWorld("surface-intent-test");
        EntityId entity = world.createEntity();
        world.put(entity, SurfaceIntentExecutionComponent.class,
                new SurfaceIntentExecutionComponent("play_card", "sts1.surface", "card-1"));
        SignalGroups.nativeGroup().connect(ContextSignals.action("sts1.surface", "play_card"), new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return SignalDecision.continueSignal();
            }
        });

        EcsPipeline.run(world, new EcsTick(0f, 1L),
                java.util.Collections.<artframework.ecs.EcsSystem>singletonList(
                        new SurfaceIntentExecutionSystem()));

        assertFalse(world.has(entity, SurfaceIntentExecutionComponent.class));
        assertEquals(IntentResult.Status.ACCEPTED,
                world.get(entity, SurfaceResultComponent.class).status);
        world.close();
    }

    @Test public void recordsAcceptedQueuedRejectedAndBlockedOutcomes() {
        assertOutcome(SignalDecision.continueSignal(), IntentResult.Status.ACCEPTED, "");
        assertOutcome(SignalDecision.stopHandled("queued:host"), IntentResult.Status.QUEUED,
                "queued:host");
        assertOutcome(SignalDecision.stopRejected("rejected"), IntentResult.Status.REJECTED,
                "rejected");
        assertOutcome(SignalDecision.stopRejected("blocked:policy"), IntentResult.Status.REJECTED,
                "blocked:policy");
    }

    @Test
    public void intentArgsAreRecursivelyCopied() {
        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put("value", "before");
        List<Object> list = new ArrayList<Object>();
        list.add(nested);
        SurfaceIntentExecutionComponent request =
                new SurfaceIntentExecutionComponent("intent", "surface", list);

        nested.put("value", "after");
        list.add("mutated");

        Map<?, ?> stored = (Map<?, ?>) ((List<?>) request.args.get(0)).get(0);
        assertEquals("before", stored.get("value"));
    }

    @Test
    public void intentArgsEncodeReferencesAtAnyDepth() {
        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put("card", new CardRef("instance", "Strike"));
        List<Object> args = new ArrayList<Object>();
        args.add(nested);
        args.add(new artframework.component.MapNodeRef(1, 2, "rest"));
        SurfaceIntentExecutionComponent request =
                new SurfaceIntentExecutionComponent("intent", "surface", args);

        Map<?, ?> stored = (Map<?, ?>) ((List<?>) request.args.get(0)).get(0);
        assertEquals("CardRef", ((Map<?, ?>) stored.get("card")).get("__art_type"));
        assertEquals("MapNodeRef", ((Map<?, ?>) ((List<?>) request.args.get(0)).get(1))
                .get("__art_type"));
        PresentationWorld world = new PresentationWorld("surface-intent-nested-refs");
        EntityId entity = world.createEntity();
        world.put(entity, SurfaceIntentExecutionComponent.class, request);
        SignalGroups.nativeGroup().connect(ContextSignals.action("surface", "intent"),
                new SignalListener() {
                    @Override public SignalDecision onSignal(UiSignal signal) {
                        UiIntent intent = (UiIntent) signal.payload;
                        List<?> dispatched = (List<?>) intent.args[0];
                        assertEquals(new CardRef("instance", "Strike"),
                                ((Map<?, ?>) dispatched.get(0)).get("card"));
                        assertEquals(new artframework.component.MapNodeRef(1, 2, "rest"),
                                dispatched.get(1));
                        return SignalDecision.continueSignal();
                    }
                });
        new SurfaceIntentExecutionSystem().run(world, new EcsTick(0f, 1L));
        world.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void intentArgsRejectHostValues() {
        new SurfaceIntentExecutionComponent("intent", "surface", new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void intentArgsReserveWireTypeKey() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("__art_type", "CardRef");
        payload.put("instanceId", "application-data");
        new SurfaceIntentExecutionComponent("intent", "surface", payload);
    }

    @Test
    public void dispatchFailureStillConsumesOneShotRequest() {
        PresentationWorld world = new PresentationWorld("surface-intent-failure");
        EntityId entity = world.createEntity();
        world.put(entity, SurfaceIntentExecutionComponent.class,
                new SurfaceIntentExecutionComponent("play_card", "sts1.surface", "card-1"));
        SignalGroups.nativeGroup().connect(ContextSignals.action("sts1.surface", "play_card"),
                new SignalListener() {
                    @Override public SignalDecision onSignal(UiSignal signal) {
                        throw new IllegalStateException("boom");
                    }
                });
        try {
            new SurfaceIntentExecutionSystem().run(world, new EcsTick(0f, 1L));
        } catch (IllegalStateException expected) {
        }
        assertFalse(world.has(entity, SurfaceIntentExecutionComponent.class));
        world.close();
    }

    private void assertOutcome(final SignalDecision decision, IntentResult.Status status,
            String message) {
        SignalGroups.resetForTests();
        PresentationWorld world = new PresentationWorld("surface-intent-outcome");
        EntityId entity = world.createEntity();
        world.put(entity, SurfaceIntentExecutionComponent.class,
                new SurfaceIntentExecutionComponent("play_card", "sts1.surface", "card-1"));
        SignalGroups.nativeGroup().connect(ContextSignals.action("sts1.surface", "play_card"), new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) { return decision; }
        });

        new SurfaceIntentExecutionSystem().run(world, new EcsTick(0f, 1L));

        SurfaceResultComponent result = world.get(entity, SurfaceResultComponent.class);
        assertEquals(status, result.status);
        assertEquals(message, result.message);
        assertFalse(world.has(entity, SurfaceIntentExecutionComponent.class));
        world.close();
    }
}
