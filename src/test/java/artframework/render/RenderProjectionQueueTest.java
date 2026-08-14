package artframework.render;

import artframework.api.ArtFramework;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RenderProjectionQueueTest {
    @After public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test public void requestProjectsImmediatelyOutsideProductionBatch() {
        RenderStateEcs.surface("sts1.queue", 0f, 0f, 10f, 10f, true);

        RenderProjectionQueue.request("window");

        assertNotNull(RenderHosts.get().getTarget(RenderHost.c2SurfaceTargetId("sts1.queue")));
    }

    @Test public void requestDefersProjectionUntilBatchFlush() {
        RenderStateEcs.surface("sts1.queue", 0f, 0f, 10f, 10f, true);
        RenderProjectionQueue.projectNow();
        RenderStateEcs.removeSurface("sts1.queue");

        RenderProjectionQueue.begin();
        RenderProjectionQueue.request("window");
        assertNotNull(RenderHosts.get().getTarget(RenderHost.c2SurfaceTargetId("sts1.queue")));

        RenderProjectionQueue.flush();

        assertNull(RenderHosts.get().getTarget(RenderHost.c2SurfaceTargetId("sts1.queue")));
    }
}
