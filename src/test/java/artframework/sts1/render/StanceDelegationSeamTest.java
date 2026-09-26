package artframework.sts1.render;

import artframework.context.SurfaceIds;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StanceDelegationSeamTest {

    @After
    public void tearDown() {
        StanceDelegationGate.resetForTests();
        StanceArtRenderer.resetForTests();
    }

    @Test
    public void surfaceIdsCanonicalizeStance() {
        assertEquals(SurfaceIds.STANCE, SurfaceIds.canonicalize("sts1.stance"));
    }

    @Test
    public void delegationGateDefaultsOffAndToggles() {
        assertFalse(StanceDelegationGate.isActive());
        StanceDelegationGate.setActive(true);
        assertTrue(StanceDelegationGate.isActive());
        StanceDelegationGate.resetForTests();
        assertFalse(StanceDelegationGate.isActive());
    }

    @Test
    public void artRendererDefaultsNotReadyAndToggles() {
        assertFalse(StanceArtRenderer.isReady());
        StanceArtRenderer.setReadyForTests(true);
        assertTrue(StanceArtRenderer.isReady());
        StanceArtRenderer.resetForTests();
        assertFalse(StanceArtRenderer.isReady());
    }
}
