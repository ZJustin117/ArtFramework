package artframework.context;

import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.core.UiComponent;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Milestone 19.5: event/select full-present surfaces (mount, probe, intent actions). */
public class EventSelectPresentTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        CombatInputRouter.resetForTests();
        FullPresentMode.resetForTests();
    }

    @Test
    public void eventSurfaceMountProbeAndChooseOption() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        UiComponent event = ArtFramework.component(SurfaceIds.EVENT);
        assertNotNull(event);
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        assertEquals(UiOpResult.Status.OK, event.action("mount_event").status);
        assertTrue(event.isMounted());
        Map<String, Object> slice = event.probeSlice();
        assertEquals(SurfaceIds.EVENT, slice.get("id"));
        assertEquals("FULL", slice.get("presentLevel"));
        assertTrue(Boolean.TRUE.equals(slice.get("mounted")));
        UiOpResult choose = event.action("choose_option", Integer.valueOf(1), "Leave");
        assertEquals(UiOpResult.Status.OK, choose.status);
        assertEquals(1, backend.signalLog().size());
        assertTrue(backend.signalLog().get(0).payload instanceof UiIntent);
        assertEquals(
                IntentNames.CHOOSE_EVENT_OPTION, ((UiIntent) backend.signalLog().get(0).payload).name);
    }

    @Test
    public void selectGridAndHandSurfaces() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        FullPresentMode.setSelectLevel(PresentLevel.OBSERVE);
        UiComponent grid = ArtFramework.component(SurfaceIds.SELECT_GRID);
        UiComponent hand = ArtFramework.component(SurfaceIds.SELECT_HAND);
        assertNotNull(grid);
        assertNotNull(hand);
        assertEquals(UiOpResult.Status.OK, grid.action("mount_select").status);
        assertEquals(UiOpResult.Status.OK, hand.action("mount_select").status);
        assertEquals("GRID", grid.probeSlice().get("selectKind"));
        assertEquals("HAND", hand.probeSlice().get("selectKind"));
        assertEquals("OBSERVE", grid.probeSlice().get("presentLevel"));
        assertEquals(UiOpResult.Status.OK, grid.action("select_card", "Strike_R", Integer.valueOf(0)).status);
        assertEquals(UiOpResult.Status.OK, grid.action("confirm").status);
        assertTrue(backend.signalLog().size() >= 2);
    }
}
