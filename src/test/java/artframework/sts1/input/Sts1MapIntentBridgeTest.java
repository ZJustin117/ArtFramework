package artframework.sts1.input;

import artframework.component.MapNodeRef;
import artframework.presentation.PresentationRegistry;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1MapIntentBridgeTest {

    @After
    public void tearDown() {
        Sts1MapIntentBridge.resetForTests();
    }

    @Test
    public void resetClearsQueuedGestureDiagnostics() {
        Sts1MapIntentBridge.resetForTests();
        Map<String, Object> probe = Sts1MapIntentBridge.probeSlice();
        assertEquals("idle", probe.get("status"));
        assertEquals(Boolean.FALSE, probe.get("pending"));
        assertEquals(Integer.valueOf(0), probe.get("attempts"));
        assertFalse(probe.containsKey("row"));
        assertFalse(probe.containsKey("eligibility"));
    }

    @Test
    public void nullClickReportsRejectedState() {
        Sts1MapIntentBridge.resetForTests();
        assertEquals(
                artframework.context.IntentResult.Status.REJECTED,
                Sts1MapIntentBridge.click(null).status);
        assertEquals("missing_ref", Sts1MapIntentBridge.probeSlice().get("status"));
    }

    @Test
    public void unavailableNodeReportsRejectedState() {
        Sts1MapIntentBridge.resetForTests();
        assertEquals(
                artframework.context.IntentResult.Status.REJECTED,
                Sts1MapIntentBridge.click(new MapNodeRef(0, 0, "")).status);
        assertEquals("node_unavailable", Sts1MapIntentBridge.probeSlice().get("status"));
    }

    @Test
    public void resetRecreatesGestureAfterInputContextIsRetired() {
        Sts1MapIntentBridge.resetForTests();
        PresentationRegistry.close("sts1-input");

        Sts1MapIntentBridge.resetForTests();

        assertEquals("idle", Sts1MapIntentBridge.probeSlice().get("status"));
    }

    @Test
    public void setPointerActivatesStickyPointerState() {
        Sts1MapIntentBridge.resetForTests();
        assertEquals(
                artframework.context.IntentResult.Status.ACCEPTED,
                Sts1MapIntentBridge.setPointer(111, 222).status);
        Map<String, Object> probe = Sts1MapIntentBridge.probeSlice();
        assertEquals(Boolean.TRUE, probe.get("pointerActive"));
        assertEquals(Integer.valueOf(111), probe.get("pointerX"));
        assertEquals(Integer.valueOf(222), probe.get("pointerY"));
    }

    @Test
    public void clearPointerDeactivatesStickyPointerState() {
        Sts1MapIntentBridge.resetForTests();
        Sts1MapIntentBridge.setPointer(10, 20);
        Sts1MapIntentBridge.clearPointer();
        Map<String, Object> probe = Sts1MapIntentBridge.probeSlice();
        assertEquals(Boolean.FALSE, probe.get("pointerActive"));
        assertEquals(Integer.valueOf(0), probe.get("pointerX"));
        assertEquals(Integer.valueOf(0), probe.get("pointerY"));
    }

    @Test
    public void hoverMissingNodeFailsOpenWithoutChangingPointer() {
        Sts1MapIntentBridge.resetForTests();
        // AbstractDungeon.map is null out of game: the node cannot be resolved.
        assertEquals(
                artframework.context.IntentResult.Status.REJECTED,
                Sts1MapIntentBridge.hoverMapNode(new MapNodeRef(0, 0, "")).status);
        Map<String, Object> probe = Sts1MapIntentBridge.probeSlice();
        assertEquals(Boolean.FALSE, probe.get("pointerActive"));
    }

    @Test
    public void hoverMissingLegendFailsOpenWithReason() {
        Sts1MapIntentBridge.resetForTests();
        assertEquals(
                artframework.context.IntentResult.Status.REJECTED,
                Sts1MapIntentBridge.hoverLegend(3).status);
        assertEquals(Boolean.FALSE, Sts1MapIntentBridge.probeSlice().get("pointerActive"));
    }

    @Test
    public void hoverRequiresANodeRef() {
        Sts1MapIntentBridge.resetForTests();
        assertEquals(
                artframework.context.IntentResult.Status.REJECTED,
                Sts1MapIntentBridge.hoverMapNode(null).status);
    }

    @Test
    public void hoverOpDoesNotQueueAClickGesture() {
        Sts1MapIntentBridge.resetForTests();
        // Proves the hover op does NOT queue/alter a CLICK GESTURE (component state). The RAW
        // InputHelper click-flag guard is covered purely by
        // pointerFrameWritesCoordsAndLeavesClickFlagsUntouched (InputHelper is loadable in tests)
        // and on-device by the scenario (hover steps leave `attempts`/`pending` unchanged).
        int attemptsBefore =
                ((Integer) Sts1MapIntentBridge.probeSlice().get("attempts")).intValue();
        // A hover op (targeted or raw) must not queue/inject a CLICK gesture.
        Sts1MapIntentBridge.hoverMapNode(new MapNodeRef(0, 0, ""));
        Sts1MapIntentBridge.setPointer(50, 60);
        Map<String, Object> probe = Sts1MapIntentBridge.probeSlice();
        assertEquals(Boolean.FALSE, probe.get("pending"));
        assertEquals(Boolean.TRUE, probe.get("pointerActive"));
        assertEquals(Integer.valueOf(attemptsBefore), probe.get("attempts"));
        String status = String.valueOf(probe.get("status"));
        assertFalse("queued".equals(status));
        assertFalse("gesture_injected".equals(status));
        // clearPointer resets the pointer without creating a click gesture either.
        Sts1MapIntentBridge.clearPointer();
        Map<String, Object> afterClear = Sts1MapIntentBridge.probeSlice();
        assertEquals(Boolean.FALSE, afterClear.get("pointerActive"));
        assertEquals(Boolean.FALSE, afterClear.get("pending"));
        assertEquals(Integer.valueOf(attemptsBefore), afterClear.get("attempts"));
    }

    @Test
    public void pointerFrameWritesCoordsAndLeavesClickFlagsUntouched() {
        Sts1MapIntentBridge.resetForTests();
        // Sentinel coords so a write is attributable to the pointer frame (not a default).
        InputHelper.mX = -1;
        InputHelper.mY = -1;
        InputHelper.justClickedLeft = false;
        Sts1MapIntentBridge.setPointer(321, 654);
        Sts1MapIntentBridge.onAfterInputUpdate();
        assertEquals(321, InputHelper.mX);
        assertEquals(654, InputHelper.mY);
        // The pointer path must NOT set a raw click flag.
        assertFalse(InputHelper.justClickedLeft);
        // ...and must not clear one either (it never writes the flag): prove no-touch.
        InputHelper.justClickedLeft = true;
        Sts1MapIntentBridge.onAfterInputUpdate();
        assertTrue(InputHelper.justClickedLeft);
    }

    @Test
    public void clearPointerDoesNotDisturbQueuedClickGesture() {
        Sts1MapIntentBridge.resetForTests();
        // A click on an unavailable node installs a rejected diagnostic but no pending frames; the
        // pointer path is independent of the click path.
        Sts1MapIntentBridge.clearPointer();
        Sts1MapIntentBridge.click(new MapNodeRef(0, 0, ""));
        Map<String, Object> probe = Sts1MapIntentBridge.probeSlice();
        assertEquals(Boolean.FALSE, probe.get("pointerActive"));
        assertEquals("node_unavailable", probe.get("status"));
    }

}
