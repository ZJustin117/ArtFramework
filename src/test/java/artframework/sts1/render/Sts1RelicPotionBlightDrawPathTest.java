package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.RelicPotionBlightView;
import artframework.sts1.backend.Sts1RelicPotionBlightProjection;
import artframework.sts1.assets.Sts1HostAssets;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1RelicPotionBlightDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RelicPotionBlightProjection.resetForTests();
    }

    @Test
    public void buildsGeometryAndResolvesKnownResourcesWithFallbacks() {
        Sts1HostAssets.install();
        Sts1RelicPotionBlightProjection.publish(new RelicPotionBlightView(
                java.util.Arrays.asList(
                        new RelicPotionBlightView.Entry("relic:known", "relic", "Known",
                                ResourceIds.relic("burning_blood"), 1, true,
                                new Rect(11f, 22f, 33f, 44f), true),
                        new RelicPotionBlightView.Entry("potion:unknown", "potion", "Unknown",
                                ResourceIds.potion("missing"), 2, false,
                                new Rect(1f, 2f, 3f, 4f), true)), true));

        List<Sts1RelicPotionBlightDrawPath.DrawItem> items =
                Sts1RelicPotionBlightDrawPath.buildFromProjection();

        assertEquals(2, items.size());
        assertEquals(11f, items.get(0).bounds.x, 0.01f);
        assertEquals(44f, items.get(0).bounds.height, 0.01f);
        assertTrue(items.get(0).resourceFound);
        assertTrue("unknown family entry uses a known fallback resource", items.get(1).resourceFound);
        assertEquals(ResourceIds.POTION_UNKNOWN, items.get(1).resourceId);
        assertTrue((Boolean) Sts1RelicPotionBlightDrawPath.probeSlice().get("nativeContinuation"));
    }

    @Test
    public void nativeContinuationObservationDoesNotEnableArtOverlayPixels() {
        assertFalse(Sts1SurfaceRenderer.shouldDrawNativeContinuationOverlay());
    }
}
