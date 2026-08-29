package artframework.sts1.backend;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.context.RelicPotionBlightView;
import artframework.assets.ResourceIds;
import artframework.sts1.PresentSafety;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1RelicPotionBlightProjectionTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RelicPotionBlightProjection.resetForTests();
        PresentSafety.clearPanic();
    }

    @Test
    public void resourceIdsCoverRelicPotionBlightFamilies() {
        assertEquals("relic.burning_blood", ResourceIds.relic("burning_blood"));
        assertEquals("potion.fire_potion", ResourceIds.potion("fire_potion"));
        assertEquals("blight.grotesque_statue", ResourceIds.blight("grotesque_statue"));
    }

    @Test
    public void projectionIsImmutableAndDrawPathKeepsNativeContinuation() {
        RelicPotionBlightView view = new RelicPotionBlightView(
                Arrays.asList(new RelicPotionBlightView.Entry(
                        "relic:burning_blood", "relic", "Burning Blood",
                        ResourceIds.relic("burning_blood"), 1, true,
                        new Rect(10f, 20f, 32f, 32f), true)), true);
        Sts1RelicPotionBlightProjection.publish(view);
        assertEquals(1, Sts1RelicPotionBlightProjection.current().entryCount());
        assertTrue(Sts1RelicPotionBlightProjection.current().available);
        assertTrue((Boolean) artframework.sts1.render.Sts1RelicPotionBlightDrawPath.probeSlice()
                .get("nativeContinuation"));
    }

    @Test
    public void publishSoftFallsBackWhenReflectionPanicsOrPlayerMissing() throws Exception {
        Sts1RelicPotionBlightProjection.publish(new RelicPotionBlightView(
                Arrays.asList(new RelicPotionBlightView.Entry("stale", "relic", "Stale", "relic.x",
                        1, true, new Rect(1f, 2f, 3f, 4f), true)), true));
        PresentSafety.onHostRecreated();
        assertFalse("host recovery clears stale observation", Sts1RelicPotionBlightProjection.current().available);
        invokePublish();
        assertEquals(0, Sts1RelicPotionBlightProjection.current().entryCount());
        PresentSafety.panic("relic-projection");
        invokePublish();
        assertFalse(Sts1RelicPotionBlightProjection.current().available);
    }

    private static void invokePublish() throws Exception {
        Method m = Sts1PresentationBackend.class.getDeclaredMethod("publishRelicPotionBlightProjection");
        m.setAccessible(true);
        m.invoke(Sts1PresentationBackend.INSTANCE);
    }
}
