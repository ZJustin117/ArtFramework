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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SurfaceIntentExecutionSystemTest {
    @After public void tearDown() { SignalGroups.resetForTests(); }

    @Test public void consumesOneShotRequestAndRecordsResult() {
        PresentationWorld world = new PresentationWorld("surface-intent-test");
        EntityId entity = world.createEntity();
        world.put(entity, SurfaceIntentExecutionComponent.class,
                new SurfaceIntentExecutionComponent("play_card", "sts1.surface", "card-1"));
        SignalGroups.nativeGroup().connect("ui/sts1.surface/action/play_card", new SignalListener() {
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
}
