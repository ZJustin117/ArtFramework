package artframework.sts1.render;

import artframework.context.TargetingSessionComponent;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TargetingDrawPathTest {

    @Test
    public void inactiveSessionStillProducesGeometry() {
        TargetingDrawPath.DrawItem item = TargetingDrawPath.build(
                new TargetingSessionComponent(false, "c1", "m1", TargetingSessionComponent.Phase.ARMED),
                100f, 100f, 200f, 200f);
        assertFalse(item.active);
    }

    @Test
    public void computesQuadraticControlPointForValidTarget() {
        TargetingSessionComponent session = new TargetingSessionComponent(
                true, "c1", "m1", TargetingSessionComponent.Phase.VALID);
        TargetingDrawPath.DrawItem item = TargetingDrawPath.build(session, 0f, 0f, 100f, 0f);
        assertEquals(0f, item.startX, 0.0001f);
        assertEquals(0f, item.startY, 0.0001f);
        assertEquals(100f, item.endX, 0.0001f);
        assertEquals(0f, item.endY, 0.0001f);
        assertEquals(50f, item.controlX, 0.0001f);
        // Perpendicular bend should push control point up or down.
        assertTrue(Math.abs(item.controlY) > 1f);
    }

    @Test
    public void fallbackWhenSourceAndTargetCoincide() {
        TargetingSessionComponent session = new TargetingSessionComponent(
                true, "c1", "m1", TargetingSessionComponent.Phase.ARMED);
        TargetingDrawPath.DrawItem item = TargetingDrawPath.build(session, 10f, 10f, 10f, 10f);
        assertEquals(10f, item.controlX, 0.0001f);
        assertEquals(10f, item.controlY, 0.0001f);
    }
}
