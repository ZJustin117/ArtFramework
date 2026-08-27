package artframework.context;

import artframework.api.ArtFramework;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PresentProjectionTargetingTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void storesTargetingSessionInMetadataEntity() {
        TargetingSessionComponent session = new TargetingSessionComponent(
                true, "card-1", "monster-1", TargetingSessionComponent.Phase.VALID);
        ArtFramework.projection().setTargetingSession(session);

        TargetingSessionComponent stored = ArtFramework.projection().targetingSession();
        assertTrue(stored.active);
        assertEquals("card-1", stored.cardInstanceId);
        assertEquals("monster-1", stored.targetKey);
        assertEquals(TargetingSessionComponent.Phase.VALID, stored.phase);
    }

    @Test
    public void clearDragAlsoClearsTargetingSession() {
        ArtFramework.projection().setTargetingSession(new TargetingSessionComponent(
                true, "card-1", "monster-1", TargetingSessionComponent.Phase.VALID));

        ArtFramework.projection().clearDrag();

        TargetingSessionComponent stored = ArtFramework.projection().targetingSession();
        assertFalse(stored.active);
        assertEquals("", stored.cardInstanceId);
        assertEquals("", stored.targetKey);
    }

    @Test
    public void probeSliceExposesTargetingFields() {
        ArtFramework.projection().setTargetingSession(new TargetingSessionComponent(
                true, "card-1", "monster-1", TargetingSessionComponent.Phase.ARMED));

        java.util.Map<String, Object> probe = ArtFramework.projection().probeSlice();
        assertEquals(Boolean.TRUE, probe.get("targetingActive"));
        assertEquals("card-1", probe.get("targetingCardInstanceId"));
        assertEquals("monster-1", probe.get("targetingTargetKey"));
        assertEquals("ARMED", probe.get("targetingPhase"));
    }
}
