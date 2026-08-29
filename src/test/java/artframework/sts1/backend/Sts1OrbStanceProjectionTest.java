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
