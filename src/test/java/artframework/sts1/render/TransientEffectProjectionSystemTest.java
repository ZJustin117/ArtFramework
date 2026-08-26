package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.api.PresentationSchedule;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsTick;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Transient-effect Registry→ECS projection converges into the schedule-owned system. */
public class TransientEffectProjectionSystemTest {
    private final TransientEffectRegistry registry = NativeRenderBridge.effectRegistry();
    private final TransientEffectProjectionSystem system = new TransientEffectProjectionSystem(registry);

    @After
    public void tearDown() {
        NativeRenderBridge.resetForTests();
        PresentationRegistry.resetForTests();
        ArtFramework.resetForTests();
    }

    private TransientEffectIdentity identity(String id) {
        return new TransientEffectIdentity(id, "native.Effect", id.hashCode(), 1L);
    }

    private void runPipeline() {
        EcsPipeline.run(ArtEcs.world(), new EcsTick(0f, 1L), Collections.singletonList(system));
    }

    @Test
    public void pipelineRunProjectsPendingEventsIntoNrccNativeEntities() {
        TransientEffectIdentity effect = identity("pipeline");
        registry.present(effect, 7L, "render");

        assertFalse("projection must be deferred until the system drains",
                Sts1NativePresentationAdapter.hasEntity("effect:" + effect.instanceId));

        runPipeline();

        assertTrue(Sts1NativePresentationAdapter.hasEntity("effect:" + effect.instanceId));
        assertNotNull(PresentationRegistry.context("nrcc-native").entity(
                new PresentationKey("sts1.native", "effect_" + effect.instanceId)));
        assertEquals(Sts1NativePresentationAdapter.entity("effect:" + effect.instanceId).toString(),
                registry.entity(effect));
    }

    @Test
    public void repeatedRendersUpsertTheSameEntityIdempotently() {
        TransientEffectIdentity effect = identity("upsert");
        registry.present(effect, 1L, "render");
        runPipeline();
        String entityId = registry.entity(effect);

        registry.present(effect, 2L, "render");
        runPipeline();

        assertEquals(entityId, registry.entity(effect));
        assertEquals(Integer.valueOf(1),
                Integer.valueOf(PresentationRegistry.context("nrcc-native").entities().size()));
    }

    @Test
    public void disposeRemovesTheEntityThroughTheSystemOnly() {
        TransientEffectIdentity effect = identity("dispose");
        registry.present(effect, 1L, "render");
        runPipeline();
        assertTrue(Sts1NativePresentationAdapter.hasEntity("effect:" + effect.instanceId));

        registry.cleanup(effect);
        runPipeline();

        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:" + effect.instanceId));
        assertNull(registry.entity(effect));
    }

    @Test
    public void emptyQueueDrainIsNoOp() {
        runPipeline();
        assertEquals(Integer.valueOf(0),
                Integer.valueOf(PresentationRegistry.context("nrcc-native").entities().size()));
    }

    @Test
    public void scheduleCompatEntryDrainsTheSameOwnedInstance() {
        TransientEffectIdentity viaSchedule = identity("schedule-compat");
        PresentationSchedule schedule = new PresentationSchedule();

        registry.present(viaSchedule, 3L, "render");
        schedule.executeTransientEffectProjections();

        assertTrue(Sts1NativePresentationAdapter.hasEntity("effect:" + viaSchedule.instanceId));

        registry.cleanup(viaSchedule);
        schedule.executeTransientEffectProjections();
        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:" + viaSchedule.instanceId));
    }

    @Test
    public void projectionRunsInHostPresentationPhaseBeforeRenderProjection() {
        PresentationSchedule schedule = new PresentationSchedule();
        int hostPresentation = schedule.phases()
                .indexOf(PresentationSchedule.Phase.HOST_PRESENTATION);
        int renderProjection = schedule.phases()
                .indexOf(PresentationSchedule.Phase.RENDER_PROJECTION);
        assertTrue(hostPresentation >= 0);
        assertTrue(hostPresentation < renderProjection);
    }
}
