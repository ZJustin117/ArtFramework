package artframework.c1.host;

import artframework.console.ProbePublisher;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StageHostProbeCadenceTest {
    @Test public void postRenderDoesNotAdvanceProbePublisher() {
        final int[] heartbeat = {0};
        ProbePublisher publisher = new ProbePublisher("test", new ProbePublisher.Clock() {
            private long now;
            @Override public long nanoTime() { now += ProbePublisher.HEARTBEAT_INTERVAL_NANOS; return now; }
        }, new ProbePublisher.Sink() {
            @Override public boolean writeFull(String line) { return true; }
            @Override public boolean writeHeartbeat(String line) { heartbeat[0]++; return true; }
        }, new ProbePublisher.FullSnapshot() {
            @Override public String line() { return "ART_PROBE {}"; }
        });
        StageHost host = new StageHost(publisher);

        host.receivePostUpdate();
        host.receivePostRender(null);
        host.receivePostRender(null);
        assertEquals(0, heartbeat[0]);

        host.receivePostUpdate();
        assertEquals(1, heartbeat[0]);
    }
}
