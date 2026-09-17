package artframework.console;

import java.util.LinkedHashMap;
import java.util.Map;

/** Owns probe publication cadence and the small heartbeat metadata seam. */
public final class ProbePublisher {
    public static final long HEARTBEAT_INTERVAL_NANOS = 500000000L;
    public static final long FULL_INTERVAL_NANOS = 5000000000L;
    public static final long FULL_RETRY_NANOS = 500000000L;
    public static final long STALE_AFTER_MILLIS = 2000L;
    public static final int HEARTBEAT_SCHEMA_VERSION = 1;

    public interface Clock { long nanoTime(); }
    public interface Sink { boolean writeFull(String line); boolean writeHeartbeat(String line); }
    public interface FullSnapshot { String line(); }

    private final String writerId;
    private final Clock clock;
    private final Sink sink;
    private final FullSnapshot fullSnapshot;
    private long lastTick = Long.MIN_VALUE;
    private long nextHeartbeat;
    private long nextFull;
    private boolean fullDeadlineSet;
    private long heartbeatSequence;
    private long probeSequence;
    private long lastFullFrame = -1L;
    private long lastFullNanos = -1L;

    public ProbePublisher(String writerId, Clock clock, Sink sink, FullSnapshot fullSnapshot) {
        this.writerId = writerId;
        this.clock = clock;
        this.sink = sink;
        this.fullSnapshot = fullSnapshot;
    }

    public synchronized void tick(boolean runtimeReady, boolean hostReady, long probeFrame) {
        tick(clock.nanoTime(), runtimeReady, hostReady, probeFrame);
    }

    public synchronized void tick(long now, boolean runtimeReady, boolean hostReady, long probeFrame) {
        if (lastTick == Long.MIN_VALUE) {
            lastTick = now;
            nextHeartbeat = now + HEARTBEAT_INTERVAL_NANOS;
            if (!fullDeadlineSet) {
                nextFull = now + FULL_INTERVAL_NANOS;
                fullDeadlineSet = true;
            }
        }
        lastTick = now;
        if (now >= nextFull) {
            boolean published = publishFull(now, probeFrame).success;
            nextFull = now + (published ? FULL_INTERVAL_NANOS : FULL_RETRY_NANOS);
            fullDeadlineSet = true;
        }
        if (now >= nextHeartbeat) {
            publishHeartbeat(now, runtimeReady, hostReady, probeFrame);
            nextHeartbeat = now + HEARTBEAT_INTERVAL_NANOS;
        }
    }

    public synchronized String publishFullNow(boolean runtimeReady, boolean hostReady, long probeFrame) {
        long now = clock.nanoTime();
        FullPublication publication = publishFull(now, probeFrame);
        nextFull = now + (publication.success ? FULL_INTERVAL_NANOS : FULL_RETRY_NANOS);
        fullDeadlineSet = true;
        return publication.line;
    }

    private FullPublication publishFull(long now, long probeFrame) {
        String line;
        try {
            line = fullSnapshot.line();
        } catch (Throwable ignored) {
            return new FullPublication(null, false);
        }
        boolean success;
        try {
            success = sink.writeFull(line);
        } catch (Throwable ignored) {
            success = false;
        }
        if (success) {
            probeSequence++;
            lastFullFrame = probeFrame;
            lastFullNanos = now;
        }
        return new FullPublication(line, success);
    }

    private void publishHeartbeat(long now, boolean runtimeReady, boolean hostReady, long probeFrame) {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("heartbeatSchemaVersion", Integer.valueOf(HEARTBEAT_SCHEMA_VERSION));
        json.put("writerId", writerId);
        long sequence = heartbeatSequence + 1L;
        json.put("sequence", Long.valueOf(sequence));
        json.put("frameId", Long.valueOf(probeFrame));
        json.put("monotonicNanos", Long.valueOf(now));
        json.put("runtimeReady", Boolean.valueOf(runtimeReady));
        json.put("hostReady", Boolean.valueOf(hostReady));
        json.put("probeSequence", Long.valueOf(probeSequence));
        json.put("lastFullProbeFrame", Long.valueOf(lastFullFrame));
        json.put("lastFullProbeNanos", Long.valueOf(lastFullNanos));
        json.put("staleAfterMillis", Long.valueOf(STALE_AFTER_MILLIS));
        try {
            if (sink.writeHeartbeat("ART_HEARTBEAT " + artframework.inspect.UiInspect.toJson(json))) {
                heartbeatSequence = sequence;
            }
        } catch (Throwable ignored) {
            // A heartbeat failure is isolated from full probes and the update loop.
        }
    }

    private static final class FullPublication {
        final String line;
        final boolean success;

        FullPublication(String line, boolean success) {
            this.line = line;
            this.success = success;
        }
    }
}
