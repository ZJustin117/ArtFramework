package artframework.sts1.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** ART-side index for transient native effect presentation entities. */
public final class TransientEffectRegistry {
    private final Map<String, String> entities = new LinkedHashMap<String, String>();

    public synchronized String present(TransientEffectIdentity identity, long frameId,
            String method) {
        requireIdentity(identity);
        String ownerId = ownerId(identity);
        NativeRenderInvocation invocation = new NativeRenderInvocation(
                -1L, frameId, "", ownerId, identity.nativeClass, method,
                "transient_effect", identity.instanceId, artframework.component.Rect.ZERO);
        String entityId = Sts1NativePresentationAdapter.present(invocation);
        entities.put(identity.instanceId, entityId);
        return entityId;
    }

    public synchronized String entity(TransientEffectIdentity identity) {
        requireIdentity(identity);
        return entities.get(identity.instanceId);
    }

    public synchronized void cleanup(TransientEffectIdentity identity) {
        if (identity == null) return;
        if (entities.remove(identity.instanceId) != null) {
            Sts1NativePresentationAdapter.remove(ownerId(identity));
        }
    }

    public synchronized void clear() {
        for (String instanceId : new java.util.ArrayList<String>(entities.keySet())) {
            Sts1NativePresentationAdapter.remove("effect:" + instanceId);
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
