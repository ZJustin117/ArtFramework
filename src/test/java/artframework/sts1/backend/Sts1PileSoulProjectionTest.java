package artframework.sts1.backend;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.PileSoulView;
import artframework.sts1.PresentSafety;
import artframework.sts1.render.Sts1PileSoulDrawPath;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1PileSoulProjectionTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1PileSoulProjection.resetForTests();
        PresentSafety.clearPanic();
    }

    @Test
    public void resourceIdsCoverPileSoulOverlayFamilies() {
        assertEquals("ui.pile.draw", ResourceIds.UI_PILE_DRAW);
        assertEquals("ui.pile.discard", ResourceIds.UI_PILE_DISCARD);
        assertEquals("ui.pile.exhaust", ResourceIds.UI_PILE_EXHAUST);
        assertEquals("ui.pile.deck", ResourceIds.UI_PILE_DECK);
        assertEquals("ui.card_group.unknown", ResourceIds.UI_CARD_GROUP_UNKNOWN);
        assertEquals("ui.soul.unknown", ResourceIds.UI_SOUL_UNKNOWN);
    }

    @Test
    public void projectionIsImmutableAndKeepsNativeCardPixels() {
        ArtFramework.assets().loadMinimalVanillaCatalog();
        List<PileSoulView.Entry> entries = new ArrayList<PileSoulView.Entry>();
        entries.add(new PileSoulView.Entry("pile:draw", "pile", "draw", 7,
                new Rect(10f, 20f, 30f, 40f), true, ResourceIds.UI_PILE_DRAW, "Draw"));
        PileSoulView view = new PileSoulView(entries, true);
        entries.clear();

        Sts1PileSoulProjection.publish(view);

        assertEquals(1, Sts1PileSoulProjection.current().entryCount());
        Map<String, Object> probe = Sts1PileSoulDrawPath.probeSlice();
        assertTrue((Boolean) probe.get("nativeContinuation"));
        assertFalse((Boolean) probe.get("nativePixelsSuppressed"));
        assertTrue((Boolean) probe.get("cardPixelsNative"));
        assertEquals(Integer.valueOf(1), probe.get("entryCount"));
    }

    @Test
    public void fallbackAvoidsReportingCompleteCardArtAsOverlayOwned() {
        Sts1PileSoulProjection.publish(new PileSoulView(Arrays.asList(
                new PileSoulView.Entry("group:card", "card_group", "other", 3,
                        new Rect(1f, 2f, 3f, 4f), true,
                        ResourceIds.cardArt("Strike_R"), "Cards"),
                new PileSoulView.Entry("soul:0", "soul", "soul", 1,
                        new Rect(5f, 6f, 7f, 8f), true,
                        "missing.soul.texture", "Soul")), true));

        List<Sts1PileSoulDrawPath.DrawItem> items = Sts1PileSoulDrawPath.buildFromProjection();
        assertEquals(ResourceIds.UI_CARD_GROUP_UNKNOWN, items.get(0).resourceId);
        assertEquals(ResourceIds.UI_SOUL_UNKNOWN, items.get(1).resourceId);
    }

    @Test
    public void geometryAndCountRemainProjectedChromeDataOnly() {
        Sts1PileSoulProjection.publish(new PileSoulView(Arrays.asList(
                new PileSoulView.Entry("pile:discard", "pile", "discard", 12,
                        new Rect(100f, 200f, 64f, 96f), true,
                        ResourceIds.UI_PILE_DISCARD, "Discard")), true));

        Sts1PileSoulDrawPath.DrawItem item = Sts1PileSoulDrawPath.buildFromProjection().get(0);
        assertEquals("discard", item.zone);
        assertEquals(12, item.count);
        assertEquals(100f, item.bounds.x, 0.001f);
        assertEquals(96f, item.bounds.height, 0.001f);
    }

    @Test
    public void nativeContinuationObservationDoesNotBecomeArtDrawItems() throws Exception {
        Sts1PileSoulProjection.publish(new PileSoulView(Arrays.asList(
                new PileSoulView.Entry("pile:draw", "pile", "draw", 7,
                        new Rect(10f, 20f, 30f, 40f), true,
                        ResourceIds.UI_PILE_DRAW, "Draw"),
                new PileSoulView.Entry("soul:0", "soul", "soul", 1,
                        new Rect(50f, 60f, 30f, 40f), true,
                        ResourceIds.UI_SOUL_UNKNOWN, "Soul")), true));

        assertFalse(nativeContinuationOverlayEnabled());
        assertEquals(2, Sts1PileSoulProjection.current().entryCount());
        assertEquals(Integer.valueOf(2), Sts1PileSoulDrawPath.probeSlice().get("entryCount"));
        assertTrue((Boolean) Sts1PileSoulDrawPath.probeSlice().get("nativeContinuation"));
    }

    @Test
    public void publishSoftFallsBackWhenReflectionPanicsOrPlayerMissing() throws Exception {
        Sts1PileSoulProjection.publish(new PileSoulView(Arrays.asList(
                new PileSoulView.Entry("stale", "pile", "draw", 1,
                        new Rect(1f, 2f, 3f, 4f), true,
                        ResourceIds.UI_PILE_DRAW, "Stale")), true));
        PresentSafety.onHostRecreated();
        assertFalse("host recovery clears stale observation", Sts1PileSoulProjection.current().available);

        invokePublish();
        assertEquals(0, Sts1PileSoulProjection.current().entryCount());

        PresentSafety.panic("pile-soul-projection");
        invokePublish();
        assertFalse(Sts1PileSoulProjection.current().available);
    }

    private static void invokePublish() throws Exception {
        Method m = Sts1PresentationBackend.class.getDeclaredMethod("publishPileSoulProjection");
        m.setAccessible(true);
        m.invoke(Sts1PresentationBackend.INSTANCE);
    }

    private static boolean nativeContinuationOverlayEnabled() throws Exception {
        Method m = artframework.sts1.render.Sts1SurfaceRenderer.class
                .getDeclaredMethod("shouldDrawNativeContinuationOverlay");
        m.setAccessible(true);
        return ((Boolean) m.invoke(null)).booleanValue();
    }
}
