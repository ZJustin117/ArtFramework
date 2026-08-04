package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.CardPose;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.sts1.assets.Sts1HostAssets;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HandDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1HostAssets.resetForTests();
        Sts1RenderPipeline.resetForTests();
    }

    private void pushHand() {
        Sts1HostAssets.install();
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        Arrays.asList(
                                CardView.builder(new CardRef("a", "Strike_R"))
                                        .zone(CardZone.HAND)
                                        .slot(0)
                                        .pose(new CardPose(100f, 200f, 5f, 0.9f, 0f, true))
                                        .art(ResourceIds.cardArt("Strike_R"))
                                        .frame(ResourceIds.CARD_FRAME_RED)
                                        .build(),
                                CardView.builder(new CardRef("b", "Defend_R"))
                                        .zone(CardZone.HAND)
                                        .slot(1)
                                        .pose(CardPose.at(150f, 200f))
                                        .art(ResourceIds.cardArt("Defend_R"))
                                        .build()),
                        ControlsView.empty(),
                        MapView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void buildOrdersHandWithResolvedArt() {
        pushHand();
        List<HandDrawPath.DrawItem> items = HandDrawPath.buildFromProjection();
        assertEquals(2, items.size());
        assertEquals("a", items.get(0).instanceId);
        assertEquals(100f, items.get(0).x, 0.01f);
        assertEquals(200f, items.get(0).y, 0.01f);
        assertTrue(items.get(0).artFound);
        assertTrue(items.get(0).artSource.contains("Strike_R") || items.get(0).artSource.startsWith("sts1:"));
        assertTrue(items.get(0).frameSource.length() > 0);
        assertEquals(ResourceIds.cardArt("Strike_R"), items.get(0).artResourceId);
        assertEquals(ResourceIds.CARD_FRAME_RED, items.get(0).frameResourceId);
    }

    @Test
    public void unchangedProjectionReusesDrawItem() {
        pushHand();
        HandDrawPath.resetForTests();
        HandDrawPath.DrawItem first = HandDrawPath.buildFromProjection().get(0);
        HandDrawPath.DrawItem second = HandDrawPath.buildFromProjection().get(0);
        assertSame(first, second);
    }

    @Test
    public void drawItemBoundsFollowCardPoseAndScale() {
        pushHand();
        HandDrawPath.DrawItem first = HandDrawPath.buildFromProjection().get(0);
        artframework.component.Rect bounds = first.bounds();
        assertEquals(-12.5f, bounds.x, 0.01f);
        assertEquals(42.5f, bounds.y, 0.01f);
        assertEquals(225f, bounds.width, 0.01f);
        assertEquals(315f, bounds.height, 0.01f);
    }

    @Test
    public void geometryCompareMatchesSelf() {
        pushHand();
        List<GeometryCompare.Sample> samples =
                Arrays.asList(
                        GeometryCompare.fromPose("a", ArtFramework.projection().get("a").pose),
                        GeometryCompare.fromPose("b", ArtFramework.projection().get("b").pose));
        GeometryCompare.Report r = HandDrawPath.compareToHost(samples, 0.01f);
        assertTrue(r.ok);
        assertEquals(2, r.compared);
        assertEquals(0, r.diffs.size());
    }

    @Test
    public void geometryCompareDetectsDrift() {
        pushHand();
        List<GeometryCompare.Sample> samples =
                Collections.singletonList(new GeometryCompare.Sample("a", 100f, 250f, 5f, 0.9f));
        GeometryCompare.Report r = HandDrawPath.compareToHost(samples, 0.5f);
        assertFalse(r.ok);
        assertTrue(r.diffs.size() >= 1);
        assertEquals(1, r.missingInHost);
    }

    @Test
    public void probeSliceCounts() {
        pushHand();
        Map<String, Object> m = HandDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), m.get("count"));
        assertEquals(Integer.valueOf(0), m.get("missingArt"));
    }
}
