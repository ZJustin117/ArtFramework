package artframework.sts1.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ART-side index for transient native effect presentation entities and the pending
 * projection queue drained by {@link TransientEffectProjectionSystem}.
 */
public final class TransientEffectRegistry {
    public static final int DEFAULT_PENDING_CAPACITY = 256;

    private final Map<String, String> entities = new LinkedHashMap<String, String>();
    private final LinkedHashMap<String, PendingProjection> pending =
            new LinkedHashMap<String, PendingProjection>();
    private final int pendingCapacity;
    private boolean clearAllPending;

    public TransientEffectRegistry() {
        this(DEFAULT_PENDING_CAPACITY);
    }

    TransientEffectRegistry(int pendingCapacity) {
        if (pendingCapacity < 1) throw new IllegalArgumentException("pending capacity required");
        this.pendingCapacity = pendingCapacity;
    }

    /** One deferred projection event; data-only value consumed by the projection system. */
    public static final class PendingProjection {
        public final String instanceId;
        public final String ownerId;
        public final boolean removal;
        public final NativeRenderInvocation invocation;

        PendingProjection(String instanceId, String ownerId, boolean removal,
                NativeRenderInvocation invocation) {
            this.instanceId = instanceId;
            this.ownerId = ownerId;
            this.removal = removal;
            this.invocation = invocation;
        }
    }

    public synchronized void present(TransientEffectIdentity identity, long frameId,
            String method) {
        requireIdentity(identity);
        enqueue(new PendingProjection(identity.instanceId, ownerId(identity), false,
                new NativeRenderInvocation(-1L, frameId, "", ownerId(identity),
                        identity.nativeClass, method, "vfx-misc-root",
                        identity.instanceId, artframework.component.Rect.ZERO)));
    }

    /** Pops every pending projection event; only the projection system consumes these. */
    public synchronized List<PendingProjection> drainPendingProjections() {
        List<PendingProjection> drained = new ArrayList<PendingProjection>(pending.values());
        pending.clear();
        return drained;
    }

    /**
     * Consumes the deferred clear-all marker. The marker is separate from the bounded per-instance
     * queue so cleanup remains complete even when more entities are active than its capacity.
     */
    synchronized boolean consumeClearAll() {
        boolean requested = clearAllPending;
        clearAllPending = false;
        return requested;
    }

    /** Write-back after the projection system presented one entity. */
    public synchronized void recordProjected(String instanceId, String entityId) {
        if (instanceId == null || instanceId.isEmpty()) return;
        entities.put(instanceId, entityId);
    }

    public synchronized String entity(TransientEffectIdentity identity) {
        requireIdentity(identity);
        return entities.get(identity.instanceId);
    }

    public synchronized void cleanup(TransientEffectIdentity identity) {
        if (identity == null) return;
        entities.remove(identity.instanceId);
        enqueue(new PendingProjection(identity.instanceId, ownerId(identity), true, null));
    }

    public synchronized void clear() {
        pending.clear();
        clearAllPending = true;
        entities.clear();
    }

    public synchronized int activeCount() { return entities.size(); }

    public synchronized Map<String, String> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, String>(entities));
    }

    private static String ownerId(TransientEffectIdentity identity) {
        return "effect:" + identity.instanceId;
    }

    /**
     * Keep only the latest operation for an instance. Lifecycle callbacks can run repeatedly
     * before the schedule-owned projection phase; retaining every intermediate render/cleanup
     * would make this queue grow with callback volume rather than current presentation state.
     */
    private void enqueue(PendingProjection projection) {
        pending.remove(projection.instanceId);
        while (pending.size() >= pendingCapacity) {
            pending.remove(pending.keySet().iterator().next());
        }
        pending.put(projection.instanceId, projection);
    }

    private static void requireIdentity(TransientEffectIdentity identity) {
        if (identity == null || identity.instanceId.isEmpty()) {
            throw new IllegalArgumentException("effect identity required");
        }
    }
}
