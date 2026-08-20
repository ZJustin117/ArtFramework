package artframework.api;

import artframework.context.CardView;
import artframework.context.ContextFrame;
import artframework.core.HostBackend;
import artframework.core.HostCapabilities;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.PresentationMount;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PresentationScheduleTest {
    @After public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test public void productionPhasesHaveOneDeclaredOrder() {
        assertEquals(Arrays.asList(
                PresentationSchedule.Phase.SURFACE_INTENT_EXECUTION,
                PresentationSchedule.Phase.AUTHORITY_PROJECTION_AND_CONFIRMATION,
                PresentationSchedule.Phase.WORLD_NORMALIZATION,
                PresentationSchedule.Phase.ANIMATION,
                PresentationSchedule.Phase.EFFECTS,
                PresentationSchedule.Phase.HOST_PRESENTATION,
                PresentationSchedule.Phase.RENDER_PROJECTION,
                PresentationSchedule.Phase.RENDER_CLOCK,
                PresentationSchedule.Phase.HOST_BACKEND),
                new PresentationSchedule().phases());
    }

    @Test public void backendObservesProjectedAuthorityAndAdvancedRenderClock() {
        final boolean[] observed = new boolean[1];
        final float before = ArtFramework.render().timeSeconds();
        ArtFramework.setHostBackend(new HostBackend() {
            @Override public boolean isReady() { return true; }
            @Override public void attach(PresentationMount mount) {}
            @Override public void detach(PresentationMount mount) {}
            @Override public void applyLayout(PresentationMount mount) {}
            @Override public HostCapabilities capabilities() { return HostCapabilities.none(); }

            @Override public void tick(float deltaSeconds) {
                observed[0] = ArtFramework.projection().lastFrameId() == 9L
                        && ArtFramework.render().timeSeconds() > before;
            }
        });

        ArtFramework.advanceFrame(0.25f,
                ContextFrame.of(9L, 2L, "combat", null, null, null, null));

        assertTrue(observed[0]);
        assertEquals(before + 0.25f, ArtFramework.render().timeSeconds(), 0.0001f);
    }

    @Test public void hostPresentationRunsBeforeRenderClockAndBackend() {
        final List<String> order = new ArrayList<String>();
        final float before = ArtFramework.render().timeSeconds();
        ArtFramework.setHostPresentationSystem(new HostPresentationSystem() {
            @Override public void tick(float deltaSeconds) {
                assertEquals(before, ArtFramework.render().timeSeconds(), 0.0001f);
                order.add("host-presentation");
            }
        });
        ArtFramework.setHostBackend(new HostBackend() {
            @Override public boolean isReady() { return true; }
            @Override public void attach(PresentationMount mount) {}
            @Override public void detach(PresentationMount mount) {}
            @Override public void applyLayout(PresentationMount mount) {}
            @Override public void tick(float deltaSeconds) {
                assertTrue(ArtFramework.render().timeSeconds() > before);
                order.add("host-backend");
            }
        });

        ArtFramework.tick(0.1f);

        assertEquals(Arrays.asList("host-presentation", "host-backend"), order);
    }

    @Test public void failedAuthorityProjectionDoesNotRetainItsPendingFrame() {
        try {
            ArtFramework.publishFrame(ContextFrame.of(1L, "combat",
                    Arrays.<CardView>asList((CardView) null)));
            fail("expected malformed card view to fail projection");
        } catch (NullPointerException expected) {
            // The malformed external frame is rejected after its temporary command is cleaned up.
        }

        assertTrue(PresentationRegistry.context("authority-input").entities().isEmpty());
        ArtFramework.tick(0f);
        assertTrue(PresentationRegistry.context("authority-input").entities().isEmpty());
    }

    @Test public void failedAdvanceFrameDoesNotRetainItsPendingAuthorityFrame() {
        try {
            ArtFramework.advanceFrame(0f, ContextFrame.of(1L, "combat",
                    Arrays.<CardView>asList((CardView) null)));
            fail("expected malformed card view to fail projection");
        } catch (NullPointerException expected) {
            // The malformed external frame is rejected after its temporary command is cleaned up.
        }

        assertTrue(PresentationRegistry.context("authority-input").entities().isEmpty());
        ArtFramework.tick(0f);
        assertTrue(PresentationRegistry.context("authority-input").entities().isEmpty());
    }

    @Test public void facadePublishFrameUsesTheScheduleProjectionBridge() {
        ContextFrame frame = ContextFrame.of(9L, 2L, "combat", null, null, null, null);

        ArtFramework.publishFrame(frame);

        assertEquals(9L, ArtFramework.projection().lastFrameId());
        assertEquals("combat", ArtFramework.projection().scene());
    }
}
