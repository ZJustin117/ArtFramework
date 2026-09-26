package artframework.sts1.backend;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.OrbStanceView;
import artframework.sts1.PresentSafety;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1OrbStanceProjectionTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1OrbStanceProjection.resetForTests();
        PresentSafety.clearPanic();
    }

    @Test
    public void resourceIdsCoverOrbAndStanceFamilies() {
        assertEquals("orb.Frost", ResourceIds.orb("Frost"));
        assertEquals("stance.Wrath", ResourceIds.stance("Wrath"));
    }

    @Test
    public void projectionIsImmutableAndDrawPathKeepsNativeContinuation() {
        OrbStanceView view = new OrbStanceView(
                Arrays.asList(new OrbStanceView.Entry(
                        "orb:Frost", "orb", "Frost", 0, 5, 8, true,
                        ResourceIds.orb("Frost"), new Rect(10f, 20f, 32f, 32f), true)), true);
        Sts1OrbStanceProjection.publish(view);
        assertEquals(1, Sts1OrbStanceProjection.current().entryCount());
        assertTrue(Sts1OrbStanceProjection.current().available);
        assertTrue((Boolean) artframework.sts1.render.Sts1OrbStanceDrawPath.probeSlice()
                .get("nativeContinuation"));
        assertFalse((Boolean) artframework.sts1.render.Sts1OrbStanceDrawPath.probeSlice()
                .get("nativePixelsSuppressed"));
    }

    @Test
    public void legacyEntryConstructorUsesSafePresentationDefaults() {
        OrbStanceView.Entry entry = new OrbStanceView.Entry(
                "orb:Frost", "orb", "Frost", 0, 5, 8, true,
                ResourceIds.orb("Frost"), new Rect(10f, 20f, 32f, 32f), true);
        assertEquals(0f, entry.angle, 0f);
        assertEquals(1f, entry.colorR, 0f);
        assertEquals(1f, entry.colorG, 0f);
        assertEquals(1f, entry.colorB, 0f);
        assertEquals(1f, entry.colorA, 0f);
        assertEquals(0f, entry.centerX, 0f);
        assertEquals(0f, entry.centerY, 0f);
        assertEquals(0f, entry.width, 0f);
        assertEquals(0f, entry.height, 0f);
        assertEquals(1f, entry.scale, 0f);
        assertFalse(entry.hasImage);
        assertEquals(0f, entry.originX, 0f);
        assertEquals(0f, entry.originY, 0f);
        // Existing keys remain present and unchanged.
        java.util.Map<String, Object> map = entry.toMap();
        assertEquals("orb:Frost", map.get("id"));
        assertEquals(Float.valueOf(10f), map.get("x"));
    }

    @Test
    public void fullEntryConstructorExposesPresentationFieldsImmutably() {
        OrbStanceView view = new OrbStanceView(
                Arrays.asList(new OrbStanceView.Entry(
                        "stance:Wrath", "stance", "Wrath", 2, 0, 0, true,
                        ResourceIds.stance("Wrath"), new Rect(1f, 2f, 512f, 512f), true,
                        30f, 0.5f, 0.25f, 0.75f, 1f,
                        100f, 200f, 512f, 512f, 1.5f, true, 256f, 256f)), true);
        OrbStanceView.Entry entry = view.entries.get(0);
        assertEquals(30f, entry.angle, 0f);
        assertEquals(0.5f, entry.colorR, 0f);
        assertEquals(0.25f, entry.colorG, 0f);
        assertEquals(0.75f, entry.colorB, 0f);
        assertEquals(1f, entry.colorA, 0f);
        assertEquals(100f, entry.centerX, 0f);
        assertEquals(200f, entry.centerY, 0f);
        assertEquals(512f, entry.width, 0f);
        assertEquals(512f, entry.height, 0f);
        assertEquals(1.5f, entry.scale, 0f);
        assertTrue(entry.hasImage);
        assertEquals(256f, entry.originX, 0f);
        assertEquals(256f, entry.originY, 0f);

        java.util.Map<String, Object> map = entry.toMap();
        assertEquals(Float.valueOf(30f), map.get("angle"));
        assertEquals(Float.valueOf(0.5f), map.get("colorR"));
        assertEquals(Float.valueOf(0.25f), map.get("colorG"));
        assertEquals(Float.valueOf(0.75f), map.get("colorB"));
        assertEquals(Float.valueOf(1f), map.get("colorA"));
        assertEquals(Float.valueOf(100f), map.get("centerX"));
        assertEquals(Float.valueOf(200f), map.get("centerY"));
        assertEquals(Float.valueOf(512f), map.get("width"));
        assertEquals(Float.valueOf(512f), map.get("height"));
        assertEquals(Float.valueOf(1.5f), map.get("scale"));
        assertEquals(Boolean.TRUE, map.get("hasImage"));
        assertEquals(Float.valueOf(256f), map.get("originX"));
        assertEquals(Float.valueOf(256f), map.get("originY"));

        // New fields surface through the aggregate map; the published list is immutable.
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> entries =
                (java.util.List<java.util.Map<String, Object>>) view.toMap().get("entries");
        assertEquals(Float.valueOf(1.5f), entries.get(0).get("scale"));
        try {
            view.entries.add(entry);
            org.junit.Assert.fail("entries list must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected: the published entry list is immutable
        }
    }

    @Test
    public void publishSoftFallsBackWhenReflectionPanicsOrPlayerMissing() throws Exception {
        Sts1OrbStanceProjection.publish(new OrbStanceView(
                Arrays.asList(new OrbStanceView.Entry("stale", "orb", "Stale", 0, 1, 2, true,
                        "orb.x", new Rect(1f, 2f, 3f, 4f), true)), true));
        PresentSafety.onHostRecreated();
        assertFalse("host recovery clears stale observation", Sts1OrbStanceProjection.current().available);
        invokePublish();
        assertEquals(0, Sts1OrbStanceProjection.current().entryCount());
        PresentSafety.panic("orb-stance-projection");
        invokePublish();
        assertFalse(Sts1OrbStanceProjection.current().available);
    }

    private static void invokePublish() throws Exception {
        Method m = Sts1PresentationBackend.class.getDeclaredMethod("publishOrbStanceProjection");
        m.setAccessible(true);
        m.invoke(Sts1PresentationBackend.INSTANCE);
    }
}
