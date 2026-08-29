package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.RoomShellView;
import artframework.sts1.PresentSafety;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.backend.Sts1RoomShellProjection;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1RoomShellDrawPathTest {
    @After
    public void tearDown() {
        Sts1RoomShellProjection.clear();
        PresentSafety.resetForTests();
        ArtFramework.resetForTests();
    }

    @Test
    public void knownAndFallbackResourcesAreStable() {
        RoomShellView event = new RoomShellView("event", "event-1", "Event", "COMPLETE",
                true, new Rect(10f, 20f, 300f, 40f), ResourceIds.UI_ROOM_SHELL_EVENT, true);
        assertEquals(ResourceIds.UI_ROOM_SHELL_EVENT, Sts1RoomShellDrawPath.resourceFor(event));

        RoomShellView unknown = new RoomShellView("future", "future-1", "Future", "",
                true, Rect.ZERO, "ui.room_shell.future", true);
        assertEquals(ResourceIds.UI_ROOM_SHELL_UNKNOWN, Sts1RoomShellDrawPath.resourceFor(unknown));
        assertTrue(Sts1VanillaCatalog.isKnown(ResourceIds.UI_ROOM_SHELL_UNKNOWN));
    }

    @Test
    public void probeCarriesGeometryStateAndNativeContinuationContract() {
        RoomShellView view = new RoomShellView("neow", "neow-1", "Neow", "COMBAT",
                true, new Rect(11f, 22f, 333f, 44f), ResourceIds.UI_ROOM_SHELL_NEOW, true);
        Sts1RoomShellProjection.publish(view);

        Map<String, Object> probe = Sts1RoomShellDrawPath.probeSlice();
        assertEquals("neow", probe.get("kind"));
        assertEquals("neow-1", probe.get("id"));
        assertEquals("COMBAT", probe.get("phase"));
        assertEquals(Float.valueOf(11f), probe.get("x"));
        assertEquals(Float.valueOf(333f), probe.get("w"));
        assertTrue((Boolean) probe.get("available"));
        assertTrue((Boolean) probe.get("visible"));
        assertTrue((Boolean) probe.get("nativeContinuation"));
        assertFalse((Boolean) probe.get("nativePixelsSuppressed"));
        assertEquals("overlay-only", probe.get("surface"));
    }

    @Test
    public void unavailableProjectionIsEmptyAndRecoveryClearsIt() {
        Sts1RoomShellProjection.publish(new RoomShellView("generic", "r", "Room", "",
                true, new Rect(1f, 2f, 3f, 4f), ResourceIds.UI_ROOM_SHELL_GENERIC, true));
        PresentSafety.panic("room-shell-test");
        assertFalse(Sts1RoomShellProjection.current().available);
        PresentSafety.clearPanic();
        Sts1RoomShellProjection.publish(new RoomShellView("generic", "r2", "Room", "",
                true, Rect.ZERO, ResourceIds.UI_ROOM_SHELL_GENERIC, false));
        PresentSafety.onHostRecreated();
        assertFalse(Sts1RoomShellProjection.current().available);
    }
}
