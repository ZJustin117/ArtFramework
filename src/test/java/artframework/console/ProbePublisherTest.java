package artframework.console;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ProbePublisherTest {
    private static final class Sink implements ProbePublisher.Sink {
        final List<String> full = new ArrayList<String>();
        final List<String> heartbeat = new ArrayList<String>();
        boolean fullSuccess = true;
        boolean heartbeatSuccess = true;
        @Override public boolean writeFull(String line) { full.add(line); return fullSuccess; }
        @Override public boolean writeHeartbeat(String line) { heartbeat.add(line); return heartbeatSuccess; }
    }

    @Test public void cadenceAndHeartbeatAreSeparate() {
        Sink sink = new Sink();
        ProbePublisher publisher = new ProbePublisher("writer", new ProbePublisher.Clock() {
            @Override public long nanoTime() { return 0L; }
        }, sink, new ProbePublisher.FullSnapshot() {
            @Override public String line() { return "ART_PROBE {\"schemaVersion\":1}"; }
        });
        publisher.tick(0L, true, true, 7L);
        publisher.tick(499999999L, true, true, 7L);
        assertEquals(0, sink.heartbeat.size());
        publisher.tick(500000000L, true, true, 7L);
        assertEquals(1, sink.heartbeat.size());
        assertTrue(sink.heartbeat.get(0).startsWith("ART_HEARTBEAT "));
        assertFalse(sink.heartbeat.get(0).contains("windows"));
        assertTrue(sink.heartbeat.get(0).contains("\"heartbeatSchemaVersion\":1"));
        assertTrue(sink.heartbeat.get(0).contains("\"writerId\":\"writer\""));
        assertTrue(sink.heartbeat.get(0).contains("\"frameId\":7"));
        assertTrue(sink.heartbeat.get(0).contains("\"runtimeReady\":true"));
        assertTrue(sink.heartbeat.get(0).contains("\"hostReady\":true"));
        assertTrue(sink.heartbeat.get(0).contains("\"staleAfterMillis\":2000"));
        publisher.tick(5000000000L, true, true, 8L);
        assertEquals(1, sink.full.size());
        assertEquals(2, sink.heartbeat.size());
        assertTrue(sink.heartbeat.get(1).contains("\"probeSequence\":1"));
        assertTrue(sink.heartbeat.get(1).contains("\"lastFullProbeFrame\":8"));
    }

    @Test public void explicitFullIsImmediateAndFreshnessIsUpdated() {
        Sink sink = new Sink();
        ProbePublisher publisher = new ProbePublisher("writer", new ProbePublisher.Clock() {
            @Override public long nanoTime() { return 9000000000L; }
        }, sink, new ProbePublisher.FullSnapshot() {
            @Override public String line() { return "ART_PROBE full"; }
        });
        assertEquals("ART_PROBE full", publisher.publishFullNow(true, false, 42L));
        publisher.tick(9000000000L, true, false, 42L);
        publisher.tick(9500000000L, true, false, 42L);
        assertEquals(1, sink.full.size());
        assertEquals(1, sink.heartbeat.size());
        assertTrue(sink.heartbeat.get(0).contains("\"lastFullProbeFrame\":42"));
        assertTrue(sink.heartbeat.get(0).contains("\"lastFullProbeNanos\":9000000000"));
        publisher.tick(14000000000L, true, false, 43L);
        assertEquals(2, sink.full.size()); // explicit publication reset the automatic full deadline
    }

    @Test public void sinkFailureDoesNotCrossContaminateCadences() {
        final int[] heartbeats = {0};
        ProbePublisher.Sink sink = new ProbePublisher.Sink() {
            @Override public boolean writeFull(String line) { throw new RuntimeException("full"); }
            @Override public boolean writeHeartbeat(String line) {
                heartbeats[0]++;
                throw new RuntimeException("heartbeat");
            }
        };
        ProbePublisher publisher = new ProbePublisher("writer", new ProbePublisher.Clock() {
            @Override public long nanoTime() { return 0L; }
        }, sink, new ProbePublisher.FullSnapshot() {
            @Override public String line() { return "ART_PROBE full"; }
        });
        publisher.tick(0L, true, true, 1L);
        publisher.tick(5000000000L, true, true, 1L);
        assertEquals(1, heartbeats[0]); // publication attempts remain independent and fail-open
    }

    @Test public void heartbeatDoesNotConstructFullSnapshot() {
        final AtomicInteger snapshots = new AtomicInteger();
        Sink sink = new Sink();
        ProbePublisher publisher = new ProbePublisher("writer", fixedClock(0L), sink,
                new ProbePublisher.FullSnapshot() {
                    @Override public String line() {
                        snapshots.incrementAndGet();
                        return "ART_PROBE full";
                    }
                });
        publisher.tick(0L, true, true, 1L);
        publisher.tick(ProbePublisher.HEARTBEAT_INTERVAL_NANOS, true, true, 2L);
        assertEquals(0, snapshots.get());
        assertEquals(1, sink.heartbeat.size());
    }

    @Test public void totalSinkFailureDoesNotAdvanceMetadataAndRetriesAfterShortDelay() {
        final AtomicInteger snapshots = new AtomicInteger();
        Sink sink = new Sink();
        sink.fullSuccess = false;
        ProbePublisher publisher = new ProbePublisher("writer", fixedClock(0L), sink,
                countingSnapshot(snapshots));
        publisher.tick(0L, true, true, 1L);
        publisher.tick(ProbePublisher.FULL_INTERVAL_NANOS, true, true, 2L);
        assertEquals(1, snapshots.get());
        assertTrue(last(sink.heartbeat).contains("\"probeSequence\":0"));
        assertTrue(last(sink.heartbeat).contains("\"lastFullProbeFrame\":-1"));
        publisher.tick(ProbePublisher.FULL_INTERVAL_NANOS
                + ProbePublisher.FULL_RETRY_NANOS - 1L, true, true, 3L);
        assertEquals(1, snapshots.get());
        publisher.tick(ProbePublisher.FULL_INTERVAL_NANOS
                + ProbePublisher.FULL_RETRY_NANOS, true, true, 3L);
        assertEquals(2, snapshots.get());
    }

    @Test public void successfulSinkAdvancesFullMetadata() {
        Sink sink = new Sink();
        ProbePublisher publisher = new ProbePublisher(
                "writer", fixedClock(12L), sink, constantSnapshot("ART_PROBE full"));
        assertEquals("ART_PROBE full", publisher.publishFullNow(true, true, 9L));
        publisher.tick(12L, true, true, 9L);
        publisher.tick(ProbePublisher.HEARTBEAT_INTERVAL_NANOS + 12L, true, true, 10L);
        assertTrue(last(sink.heartbeat).contains("\"probeSequence\":1"));
        assertTrue(last(sink.heartbeat).contains("\"lastFullProbeFrame\":9"));
        assertTrue(last(sink.heartbeat).contains("\"lastFullProbeNanos\":12"));
    }

    @Test public void snapshotFailureDoesNotWriteOrAdvanceMetadata() {
        final AtomicInteger snapshots = new AtomicInteger();
        Sink sink = new Sink();
        ProbePublisher publisher = new ProbePublisher("writer", fixedClock(20L), sink,
                new ProbePublisher.FullSnapshot() {
                    @Override public String line() {
                        snapshots.incrementAndGet();
                        throw new IllegalStateException("snapshot");
                    }
                });
        assertNull(publisher.publishFullNow(true, true, 4L));
        assertEquals(1, snapshots.get());
        assertEquals(0, sink.full.size());
        publisher.tick(20L, true, true, 4L);
        publisher.tick(ProbePublisher.HEARTBEAT_INTERVAL_NANOS + 20L, true, true, 5L);
        assertEquals(2, snapshots.get()); // first update preserved the explicit failure retry deadline
        assertTrue(last(sink.heartbeat).contains("\"probeSequence\":0"));
        assertTrue(last(sink.heartbeat).contains("\"lastFullProbeFrame\":-1"));
    }

    @Test public void explicitSnapshotAndAutomaticPublicationAreSerialized() throws Exception {
        final CountDownLatch snapshotEntered = new CountDownLatch(1);
        final CountDownLatch releaseSnapshot = new CountDownLatch(1);
        final AtomicInteger snapshots = new AtomicInteger();
        final AtomicReference<String> explicitLine = new AtomicReference<String>();
        final Sink sink = new Sink();
        final ProbePublisher publisher = new ProbePublisher("writer", fixedClock(10000000000L), sink,
                new ProbePublisher.FullSnapshot() {
                    @Override public String line() {
                        int number = snapshots.incrementAndGet();
                        if (number == 1) {
                            snapshotEntered.countDown();
                            try {
                                assertTrue(releaseSnapshot.await(5L, TimeUnit.SECONDS));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new AssertionError(e);
                            }
                        }
                        return "ART_PROBE " + number;
                    }
                });
        publisher.tick(0L, true, true, 0L);
        Thread explicit = new Thread(new Runnable() {
            @Override public void run() {
                explicitLine.set(publisher.publishFullNow(true, true, 1L));
            }
        });
        explicit.start();
        assertTrue(snapshotEntered.await(5L, TimeUnit.SECONDS));
        Thread automatic = new Thread(new Runnable() {
            @Override public void run() { publisher.tick(20000000000L, true, true, 2L); }
        });
        automatic.start();
        releaseSnapshot.countDown();
        explicit.join(5000L);
        automatic.join(5000L);
        assertFalse(explicit.isAlive());
        assertFalse(automatic.isAlive());
        assertEquals("ART_PROBE 1", explicitLine.get());
        assertEquals("ART_PROBE 1", sink.full.get(0));
        assertEquals("ART_PROBE 2", sink.full.get(1));
        assertTrue(last(sink.heartbeat).contains("\"probeSequence\":2"));
        assertTrue(last(sink.heartbeat).contains("\"lastFullProbeFrame\":2"));
    }

    private static ProbePublisher.Clock fixedClock(final long value) {
        return new ProbePublisher.Clock() {
            @Override public long nanoTime() { return value; }
        };
    }

    private static ProbePublisher.FullSnapshot constantSnapshot(final String line) {
        return new ProbePublisher.FullSnapshot() {
            @Override public String line() { return line; }
        };
    }

    private static ProbePublisher.FullSnapshot countingSnapshot(final AtomicInteger count) {
        return new ProbePublisher.FullSnapshot() {
            @Override public String line() {
                count.incrementAndGet();
                return "ART_PROBE full";
            }
        };
    }

    private static String last(List<String> values) {
        return values.get(values.size() - 1);
    }
}
