package artframework.context;

import artframework.api.ArtFramework;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * R1: PresentProjection hard-sync behaviour across scene epochs.
 */
public class PresentProjectionTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void epochChangeClearsVisualItemsButKeepsSurfaces() {
        PresentSurfaces.get(SurfaceIds.MAP).mount();
        PresentationRegistry.context("c2-surfaces").create(
                new PresentationKey("sts1.visual.test", "item"), "item", "visual", "c2");

        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        Collections.<CardView>emptyList(),
                        ControlsView.empty(),
                        MapView.empty(),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());

        assertEquals(2, PresentationRegistry.context("c2-surfaces").entities().size());

        backend.publish(
                ContextFrame.of(
                        2L,
                        2L,
                        "combat",
                        Collections.<CardView>emptyList(),
                        ControlsView.empty(),
                        MapView.empty(),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());

        assertEquals(1, PresentationRegistry.context("c2-surfaces").entities().size());
        EntityId remaining = PresentationRegistry.context("c2-surfaces").entities().get(0);
        assertNotNull(
                "surface mount/lifecycle entity must survive the epoch change",
                PresentationRegistry.context("c2-surfaces")
                        .world()
                        .get(remaining, SurfaceIdentityComponent.class));
    }
}
