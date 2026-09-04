package artframework.context;

import artframework.api.ArtFramework;
import artframework.api.FrameworkTransientFixture;
import artframework.core.SignalDecision;
import artframework.core.TransientSignalPaths;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationRegistry;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Focused synchronous authority-frame transport coverage. */
public class AuthorityProjectionSystemTest {
    private FrameworkTransientFixture transientFixture;
    @org.junit.Before public void setUp() { ArtFramework.resetForTests(); transientFixture = new FrameworkTransientFixture(); }
    @After public void tearDown() { transientFixture.close(); ArtFramework.resetForTests(); }

    @Test public void authoritySignalProjectsTheFrameImmediately() {
        ArtFramework.publishFrame(ContextFrame.of(7L, "combat", null));
        assertEquals(7L, ArtFramework.projection().lastFrameId());
        assertEquals("combat", ArtFramework.projection().scene());
    }

    @Test public void typedReplacementReachesTheAuthorityConsumer() {
        transientFixture.connect(TransientSignalPaths.AUTHORITY_FRAME, signal ->
                SignalDecision.replace(signal.replace(ContextFrame.of(8L, "map", null))));
        ArtFramework.publishFrame(ContextFrame.of(7L, "combat", null));

        assertEquals(8L, ArtFramework.projection().lastFrameId());
        assertEquals("map", ArtFramework.projection().scene());
    }

    @Test public void resetRecreatesScheduleOwnedAuthorityConsumer() {
        ArtFramework.publishFrame(ContextFrame.of(7L, "combat", null));
        ArtFramework.resetForTests();
        ArtFramework.publishFrame(ContextFrame.of(8L, "map", null));
        assertEquals(8L, ArtFramework.projection().lastFrameId());
        assertTrue(ArtFramework.projection().scene().equals("map"));
    }

    @Test public void replacementProjectsAndConfirmsTheSameEffectiveFrame() {
        ArtFramework.publishFrame(ContextFrame.of(7L, "combat", null));
        EntityId request = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(request, BusinessConfirmationComponent.class,
                new BusinessConfirmationComponent("leave", BusinessConfirmationComponent.Domain.ROOM,
                        BusinessConfirmationComponent.State.PENDING, 7L, 0L, -1L, -1L, ""));
          transientFixture.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> SignalDecision.replace(signal.replace(ContextFrame.of(8L, "map", null))));

        ArtFramework.publishFrame(ContextFrame.of(8L, "combat", null));

        BusinessConfirmationComponent confirmation = PresentationRegistry.world().get(request,
                BusinessConfirmationComponent.class);
        assertEquals(8L, ArtFramework.projection().lastFrameId());
        assertEquals("map", ArtFramework.projection().scene());
        assertEquals(BusinessConfirmationComponent.State.CONFIRMED, confirmation.state);
        assertEquals(8L, confirmation.observedFrame);
    }

    @Test public void rejectedAuthorityFrameDoesNotConfirmAgainstAnUnprojectedFrame() {
        ArtFramework.publishFrame(ContextFrame.of(7L, "combat", null));
        EntityId request = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(request, BusinessConfirmationComponent.class,
                new BusinessConfirmationComponent("leave", BusinessConfirmationComponent.Domain.ROOM,
                        BusinessConfirmationComponent.State.PENDING, 7L, 0L, -1L, -1L, ""));
          transientFixture.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> SignalDecision.stopRejected("blocked"));

        ArtFramework.publishFrame(ContextFrame.of(8L, "map", null));

        BusinessConfirmationComponent confirmation = PresentationRegistry.world().get(request,
                BusinessConfirmationComponent.class);
        assertEquals(7L, ArtFramework.projection().lastFrameId());
        assertEquals(BusinessConfirmationComponent.State.PENDING, confirmation.state);
        assertEquals(-1L, confirmation.observedFrame);
    }

    @Test public void handledAuthorityFrameDoesNotConfirmAgainstAnUnprojectedFrame() {
        ArtFramework.publishFrame(ContextFrame.of(7L, "combat", null));
        EntityId request = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(request, BusinessConfirmationComponent.class,
                new BusinessConfirmationComponent("leave", BusinessConfirmationComponent.Domain.ROOM,
                        BusinessConfirmationComponent.State.PENDING, 7L, 0L, -1L, -1L, ""));
          transientFixture.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> SignalDecision.stopHandled("handled"));

        ArtFramework.publishFrame(ContextFrame.of(8L, "map", null));

        BusinessConfirmationComponent confirmation = PresentationRegistry.world().get(request,
                BusinessConfirmationComponent.class);
        assertEquals(7L, ArtFramework.projection().lastFrameId());
        assertEquals(BusinessConfirmationComponent.State.PENDING, confirmation.state);
        assertEquals(-1L, confirmation.observedFrame);
    }

    @Test public void newerUnavailableAuthorityFrameFailsPendingConfirmation() {
        ArtFramework.publishFrame(ContextFrame.of(7L, "combat", null));
        EntityId request = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(request, BusinessConfirmationComponent.class,
                new BusinessConfirmationComponent("leave", BusinessConfirmationComponent.Domain.ROOM,
                        BusinessConfirmationComponent.State.PENDING, 7L, 0L, -1L, -1L, ""));

        ArtFramework.publishFrame(ContextFrame.unavailable(8L));

        BusinessConfirmationComponent confirmation = PresentationRegistry.world().get(request,
                BusinessConfirmationComponent.class);
        assertEquals(BusinessConfirmationComponent.State.FAILED, confirmation.state);
        assertEquals(8L, confirmation.observedFrame);
        assertEquals(0L, confirmation.observedEpoch);
        assertEquals("authority unavailable", confirmation.evidence);
        assertEquals(8L, ArtFramework.projection().lastFrameId());
        assertTrue(!ArtFramework.projection().isAvailable());
    }

    @Test public void staleUnavailableAuthorityFrameDoesNotDeliverConfirmationPair() {
        ArtFramework.publishFrame(ContextFrame.of(7L, "combat", null));
        EntityId request = PresentationRegistry.world().createEntity();
        PresentationRegistry.world().put(request, BusinessConfirmationComponent.class,
                new BusinessConfirmationComponent("leave", BusinessConfirmationComponent.Domain.ROOM,
                        BusinessConfirmationComponent.State.PENDING, 7L, 0L, -1L, -1L, ""));

        ArtFramework.publishFrame(ContextFrame.unavailable(6L));

        BusinessConfirmationComponent confirmation = PresentationRegistry.world().get(request,
                BusinessConfirmationComponent.class);
        assertEquals(BusinessConfirmationComponent.State.PENDING, confirmation.state);
        assertEquals(-1L, confirmation.observedFrame);
        assertEquals(-1L, confirmation.observedEpoch);
        assertEquals(7L, ArtFramework.projection().lastFrameId());
    }
}
