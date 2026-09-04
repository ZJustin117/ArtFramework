package artframework.context;

import artframework.api.ArtFramework;
import artframework.api.FrameworkTransientFixture;
import artframework.core.SignalGroups;
import artframework.core.TransientSignalPaths;
import artframework.core.UiSignal;
import artframework.ecs.EntityId;
import org.junit.After;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Focused behavior coverage for schedule-owned synchronous surface lifecycle transport. */
public class SurfaceLifecycleSystemTest {
    private FrameworkTransientFixture transientFixture;
    @org.junit.Before public void setUp() { ArtFramework.resetForTests(); transientFixture = new FrameworkTransientFixture(); }
    @After public void tearDown() { transientFixture.close(); ArtFramework.resetForTests(); }

    @Test public void lifecycleSignalUpdatesCurrentKeyedSurfaceSynchronously() {
        final AtomicReference<UiSignal> delivered = new AtomicReference<UiSignal>();
        transientFixture.connect(TransientSignalPaths.SURFACE_LIFECYCLE,
                signal -> {
                    delivered.set(signal);
                    return artframework.core.SignalDecision.continueSignal();
                });
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        EntityId entity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);
        assertTrue(PresentSurfaces.world().get(entity, SurfaceLifecycleComponent.class).mounted);

        UiSignal signal = delivered.get();
        assertTrue(signal != null);
        assertTrue("c2-surfaces".equals(signal.source));
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) signal.payload;
        assertTrue(payload.size() == 2);
        assertTrue(SurfaceIds.COMBAT_HAND.equals(payload.get("surfaceId")));
        assertTrue(Boolean.TRUE.equals(payload.get("mounted")));

        ArtFramework.dispatchSurfaceLifecycle(SurfaceIds.COMBAT_HAND, false);
        assertFalse(PresentSurfaces.world().get(entity, SurfaceLifecycleComponent.class).mounted);
    }

    @Test public void lifecycleSignalResolvesTheCurrentSurfaceEntityByIdentity() {
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        EntityId entity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);
        PresentSurfaces.world().put(entity, SurfaceIdentityComponent.class,
                new SurfaceIdentityComponent("incorrect", artframework.core.ComponentKind.NATIVE_HOST));
        ArtFramework.dispatchSurfaceLifecycle(SurfaceIds.COMBAT_HAND, false);
        assertTrue(PresentSurfaces.world().get(entity, SurfaceLifecycleComponent.class).mounted);
    }
}
