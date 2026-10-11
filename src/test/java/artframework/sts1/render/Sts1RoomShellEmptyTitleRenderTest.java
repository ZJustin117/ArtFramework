package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.RoomShellView;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsTick;
import artframework.sts1.PresentSafety;
import artframework.sts1.backend.Sts1RoomShellProjection;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * U03: an empty room-shell title must produce NO title draw anywhere downstream.
 *
 * <p>Native rooms declare no room-shell title (see {@code Sts1RoomShellTitleTest} for the verified
 * decompiled evidence), so the backend now yields {@code title == ""}. This test pins the renderer
 * consequence: the metadata frame is empty, no room-shell label metadata is published, and the
 * {@code frameHasTitle} probe stays {@code false} — the stray non-native "Generic" label can never
 * reappear on the world/select screens.</p>
 */
public class Sts1RoomShellEmptyTitleRenderTest {

    @After
    public void tearDown() {
        artframework.core.PackSystems.resetForTests();
        RoomShellRenderProjectionSystem.resetForTests();
        Sts1RoomShellProjection.clear();
        PresentSafety.resetForTests();
        ArtFramework.resetForTests();
    }

    @Test
    public void emptyTitleViewDrawsNoTitleThroughTheProjectionPipeline() {
        RoomShellView emptyTitle = new RoomShellView("generic", "r", "", "COMBAT", true,
                new Rect(1f, 2f, 3f, 4f), ResourceIds.UI_ROOM_SHELL_GENERIC, true);

        // Pure mapping: no title means an empty metadata frame (the draw path skips its label).
        assertTrue("an empty-title view must yield an empty metadata frame",
                RoomShellRenderFrame.of(emptyTitle, false).isEmpty());

        // Downstream of the projection pipeline the label must stay absent as well.
        Sts1RoomShellProjection.publish(emptyTitle);
        new RoomShellRenderProjectionSystem().run(ArtEcs.world(), new EcsTick(0f, 1L));
        new artframework.render.ArtRenderFrameAggregationSystem()
                .run(ArtEcs.world(), new EcsTick(0f, 2L));

        assertNull("no label metadata may be published for an empty title",
                Sts1RoomShellDrawPath.publishedMetadata());
        Map<String, Object> probe = Sts1RoomShellDrawPath.probeSlice();
        assertEquals(Boolean.FALSE, probe.get("frameHasTitle"));
    }
}
