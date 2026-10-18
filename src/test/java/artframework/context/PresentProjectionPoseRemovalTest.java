package artframework.context;

import artframework.api.ArtFramework;
import artframework.presentation.BoundsComponent;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * U05b follow-up: a card may legitimately carry NO pose (the select draw path then falls back to its
 * own on-screen row). When a card goes posed -> pose-less, the projection must NOT keep the previous
 * publish's {@link BoundsComponent}, otherwise the inspect/probe surfaces keep reporting a stale rect
 * for a card that has no placement at all.
 */
public class PresentProjectionPoseRemovalTest {

    private static final String INSTANCE = "pose-card";

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void poseLessCardDropsAPreviouslyPublishedBoundsComponent() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();

        publish(backend, 1L, CardPose.at(120f, 240f));
        EntityId entity = cardEntity();
        assertNotNull("test setup: a posed card must publish bounds", bounds(entity));

        // SAME epoch: this is the in-place PLACEMENT_CHANGED update path. A new epoch recreates the
        // projection entities, which would hide the stale-bounds defect the test exists to catch.
        publish(backend, 1L, null);

        assertNull(
                "U05b: a pose-less card must not retain the previous publish's bounds",
                bounds(cardEntity()));
    }

    private static BoundsComponent bounds(EntityId entity) {
        if (entity == null) {
            return null;
        }
        PresentationWorld world = PresentationRegistry.context("c2-projection").world();
        return world.get(entity, BoundsComponent.class);
    }

    private static EntityId cardEntity() {
        return PresentationRegistry.context("c2-projection")
                .entity(new PresentationKey("sts1.card", INSTANCE));
    }

    private static void publish(FakeSignalBackend backend, long epoch, CardPose pose) {
        CardView.Builder builder =
                CardView.builder(new CardRef(INSTANCE, "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("Strike");
        if (pose != null) {
            builder.pose(pose);
        }
        List<CardView> cards = Collections.singletonList(builder.build());
        backend.publish(
                ContextFrame.of(
                        epoch,
                        epoch,
                        "combat",
                        cards,
                        ControlsView.empty(),
                        MapView.empty(),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());
    }
}
