package artframework.sts1.input;

import artframework.c2.MapNodeRef;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

}
