package artframework.sts1.render;

import java.util.ArrayDeque;
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
    private final Map<String, String> entities = new LinkedHashMap<String, String>();
    private final ArrayDeque<PendingProjection> pending = new ArrayDeque<PendingProjection>();

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
        pending.add(new PendingProjection(identity.instanceId, ownerId(identity), false,
                new NativeRenderInvocation(-1L, frameId, "", ownerId(identity),
                        identity.nativeClass, method, "transient_effect",
                        identity.instanceId, artframework.component.Rect.ZERO)));
    }

    /** Pops every pending projection event; only the projection system consumes these. */
    public synchronized List<PendingProjection> drainPendingProjections() {
        List<PendingProjection> drained = new ArrayList<PendingProjection>(pending);
        pending.clear();
        return drained;
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
        pending.add(new PendingProjection(identity.instanceId, ownerId(identity), true, null));
    }

    public synchronized void clear() {
        for (String instanceId : entities.keySet()) {
            pending.add(new PendingProjection(instanceId, "effect:" + instanceId, true, null));
        }
        entities.clear();
    }

    public synchronized int activeCount() { return entities.size(); }

    public synchronized Map<String, String> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, String>(entities));
    }

    private static String ownerId(TransientEffectIdentity identity) {
        return "effect:" + identity.instanceId;
    }

    private static void requireIdentity(TransientEffectIdentity identity) {
        if (identity == null || identity.instanceId.isEmpty()) {
            throw new IllegalArgumentException("effect identity required");
        }
    }
}
