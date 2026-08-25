package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
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

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
    }

    private void publishSelectFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        SelectView select =
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .selected(true)
                                        .build()),
                        Collections.singletonList("g1"),
                        true,
                        true);
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "select",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        EventView.empty(),
                        select,
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void buildIncludesPoolAndConfirm() {
        publishSelectFrame();
        assertTrue(SelectDrawPath.buildFromProjection().size() >= 2);
        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(SelectView.KIND_GRID, probe.get("kind"));
        assertEquals(Integer.valueOf(1), probe.get("poolCount"));
        assertEquals(Boolean.TRUE, probe.get("confirmEnabled"));
    }

    @Test
    public void fullMountedGridDrawsWithoutSuppressingNative() {
        publishSelectFrame();
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SELECT_GRID).action("mount_select");
        assertTrue(Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.SELECT_GRID));
        // NRO-03: native grid/hand select renderers stay the visual authority.
        assertFalse(SelectDrawPath.shouldSuppressNativeGrid());
        assertFalse(SelectDrawPath.shouldSuppressNativeSelect());
    }
}
