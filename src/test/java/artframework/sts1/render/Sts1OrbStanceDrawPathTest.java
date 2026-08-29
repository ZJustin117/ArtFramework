package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.OrbStanceView;
import artframework.sts1.assets.Sts1HostAssets;
import artframework.sts1.backend.Sts1OrbStanceProjection;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1OrbStanceDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1OrbStanceProjection.resetForTests();
    }

    @Test
    public void buildsGeometryStateAndResolvesKnownResourcesWithFallbacks() {
        Sts1HostAssets.install();
        Sts1OrbStanceProjection.publish(new OrbStanceView(
                java.util.Arrays.asList(
                        new OrbStanceView.Entry("orb:Frost", "orb", "Frost", 0, 5, 8, true,
                                ResourceIds.orb("Frost"), new Rect(11f, 22f, 33f, 44f), true),
                        new OrbStanceView.Entry("orb:missing", "orb", "Missing", 1, 0, 0, false,
                                ResourceIds.orb("missing"), new Rect(1f, 2f, 3f, 4f), true),
                        new OrbStanceView.Entry("stance:Missing", "stance", "Missing", 2, 0, 0, true,
                                ResourceIds.stance("Missing"), new Rect(5f, 6f, 7f, 8f), true)), true));

        List<Sts1OrbStanceDrawPath.DrawItem> items = Sts1OrbStanceDrawPath.buildFromProjection();

        assertEquals(3, items.size());
        assertEquals(11f, items.get(0).bounds.x, 0.01f);
        assertEquals(44f, items.get(0).bounds.height, 0.01f);
        assertEquals(5, items.get(0).passive);
        assertEquals(8, items.get(0).evoke);
        assertTrue(items.get(0).active);
        assertTrue(items.get(0).resourceFound);
        assertTrue("unknown orb uses known fallback resource", items.get(1).resourceFound);
        assertEquals(ResourceIds.ORB_UNKNOWN, items.get(1).resourceId);
        assertFalse(items.get(1).active);
        assertTrue("unknown stance uses known fallback resource", items.get(2).resourceFound);
        assertEquals(ResourceIds.STANCE_UNKNOWN, items.get(2).resourceId);
    }

    @Test
    public void probeEvidenceDeclaresOverlayOnlyNativeContinuation() {
        Sts1HostAssets.install();
        Sts1OrbStanceProjection.publish(new OrbStanceView(
                java.util.Arrays.asList(
                        new OrbStanceView.Entry("orb:Frost", "orb", "Frost", 0, 5, 8, true,
                                ResourceIds.orb("Frost"), new Rect(11f, 22f, 33f, 44f), true)), true));

        Map<String, Object> probe = Sts1OrbStanceDrawPath.probeSlice();

        assertEquals(Integer.valueOf(1), probe.get("entryCount"));
        assertTrue((Boolean) probe.get("nativeContinuation"));
        assertFalse((Boolean) probe.get("nativePixelsSuppressed"));
        assertEquals("overlay-only", probe.get("surface"));
    }
}
