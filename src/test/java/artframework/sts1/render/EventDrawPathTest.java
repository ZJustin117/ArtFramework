package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SelectView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
    }

    private void publishEventFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        EventView event =
                EventView.of(
                        "Neow",
                        Arrays.asList(
                                EventOptionView.of(0, "Talk", true),
                                EventOptionView.of(1, "Leave", true)));
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "event",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        event,
                        SelectView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void buildFromProjectionListsOptions() {
        publishEventFrame();
        assertEquals(2, EventDrawPath.buildFromProjection().size());
        Map<String, Object> probe = EventDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("count"));
        assertEquals("Neow", probe.get("title"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeEvent"));
    }

    @Test
    public void fullMountedDrawsWithoutSuppressingNative() {
        publishEventFrame();
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.EVENT).action("mount_event");
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.EVENT));
        // NRO-03: native GenericEventDialog.render stays the visual authority.
        assertFalse(EventDrawPath.shouldSuppressNativeEvent());
        assertEquals(Boolean.FALSE, EventDrawPath.probeSlice().get("suppressNativeEvent"));
    }

    @Test
    public void offDoesNotSuppress() {
        publishEventFrame();
        ArtFramework.component(SurfaceIds.EVENT).action("mount_event");
        assertFalse(EventDrawPath.shouldSuppressNativeEvent());
    }
}
