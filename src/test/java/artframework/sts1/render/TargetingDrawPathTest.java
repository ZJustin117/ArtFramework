package artframework.sts1.render;

import artframework.context.TargetingSessionComponent;
import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.MonsterIntentView;
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

    @Test
    public void buildsDynamicGeometryFromCardAndMonsterProjection() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.ofFull(
                7L, 2L, "combat",
                java.util.Arrays.asList(CardView.builder(new CardRef("card-1", "Strike_R"))
                        .zone(CardZone.HAND).pose(artframework.context.CardPose.at(12f, 24f)).build()),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null,
                null, null, null, null, null, null,
                MonsterIntentView.of(java.util.Arrays.asList(
                        new MonsterIntentView.IntentEntry("monster-1", "Jaw Worm", "attack", "", 0,
                                112f, 224f))),
                artframework.context.ViewportView.unavailable()));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.projection().setTargetingSession(new TargetingSessionComponent(
                true, "card-1", "monster-1", TargetingSessionComponent.Phase.VALID));

        List<TargetingDrawPath.DrawItem> items = TargetingDrawPath.buildFromProjection();

        assertEquals(1, items.size());
        TargetingDrawPath.DrawItem item = items.get(0);
        assertTrue(item.active);
        assertEquals(12f, item.startX, 0.0001f);
        assertEquals(24f, item.startY, 0.0001f);
        assertEquals(112f, item.endX, 0.0001f);
        assertEquals(224f, item.endY, 0.0001f);
        assertEquals(TargetingSessionComponent.Phase.VALID, item.phase);
    }

    @Test
    public void armedInvalidTargetUsesFallbackGeometryAndDoesNotInventTarget() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(8L, 3L, "combat",
                java.util.Arrays.asList(CardView.builder(new CardRef("card-1", "Strike_R"))
                        .pose(artframework.context.CardPose.at(12f, 24f)).build()),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.projection().setTargetingSession(new TargetingSessionComponent(
                true, "card-1", "missing-monster", TargetingSessionComponent.Phase.ARMED));

        List<TargetingDrawPath.DrawItem> items = TargetingDrawPath.buildFromProjection();

        assertEquals(1, items.size());
        assertEquals(0f, items.get(0).endX, 0.0001f);
        assertEquals(0f, items.get(0).endY, 0.0001f);
        assertEquals(TargetingSessionComponent.Phase.ARMED, items.get(0).phase);
    }
}
