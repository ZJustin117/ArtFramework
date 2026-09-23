package artframework.sts1.render;

import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.HostBindingComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.VisibilityComponent;
import artframework.render.RenderProjectionQueue;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts a delegated native invocation into host-neutral ART presentation data.
 *
 * <p>This adapter intentionally retains only stable presentation identity and data. Native STS
 * objects and SpriteBatch instances remain owned by the patch/host boundary.</p>
 */
public final class Sts1NativePresentationAdapter {
    private static final String CONTEXT_SCOPE = "nrcc-native";
    private static final String KEY_SCOPE = "sts1.native";
    private static final String HOST_KIND = "STS1_NATIVE";
    private static final String TRANSIENT_EFFECT_OWNER_PREFIX = "effect:";

    private Sts1NativePresentationAdapter() {}

    public static String present(NativeRenderInvocation invocation) {
        if (invocation == null) throw new IllegalArgumentException("invocation required");
        PresentationContext context = PresentationRegistry.context(CONTEXT_SCOPE);
        String localId = localId(invocation.ownerId);
        PresentationKey key = new PresentationKey(KEY_SCOPE, localId);
        EntityId entity = context.entity(key);
        if (entity == null) {
            entity = context.create(key, invocation.ownerId, "native_render", invocation.nativeClass);
        }
        Rect bounds = invocation.boundsHint != null ? invocation.boundsHint : Rect.ZERO;
        context.world().put(entity, BoundsComponent.class, new BoundsComponent(bounds, 0f));
        context.world().put(entity, VisibilityComponent.class,
                new VisibilityComponent(true, 1f));
        context.world().put(entity, DrawComponent.class,
                new DrawComponent(invocation.nativeMethod, "", invocation.sourceIdentity));
        context.world().put(entity, HostBindingComponent.class,
                new HostBindingComponent(HOST_KIND, invocation.ownerId));
        context.world().put(entity, NativeInvocationComponent.class,
                new NativeInvocationComponent(invocation));
        NativeRenderPolicy policy = NativeRenderBridge.policy();
        context.world().put(entity, NativeRenderExemptionComponent.class,
                new NativeRenderExemptionComponent(policy.revision(), policy.exempt(invocation),
                        policy.matchedTarget(invocation)));
        RenderProjectionQueue.request(CONTEXT_SCOPE);
        return entity.toString();
    }

    public static EntityId entity(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) return null;
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) return null;
        return context.entity(new PresentationKey(KEY_SCOPE, localId(ownerId)));
    }

    public static boolean hasEntity(String ownerId) {
        return entity(ownerId) != null;
    }

    public static void remove(String ownerId) {
        EntityId entity = entity(ownerId);
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (entity != null && context != null) {
            context.destroy(entity);
            RenderProjectionQueue.request(CONTEXT_SCOPE);
        }
    }

    public static void clear() {
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) return;
        for (EntityId entity : new java.util.ArrayList<EntityId>(context.entities())) {
            context.destroy(entity);
        }
        RenderProjectionQueue.request(CONTEXT_SCOPE);
    }

    /** Clears only transient-effect entities from the shared native presentation context. */
    public static void clearTransientEffects() {
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) return;
        for (EntityId entity : new java.util.ArrayList<EntityId>(context.entities())) {
            HostBindingComponent binding = context.world().get(entity, HostBindingComponent.class);
            if (binding != null && binding.localKey.startsWith(TRANSIENT_EFFECT_OWNER_PREFIX)) {
                context.destroy(entity);
            }
        }
        RenderProjectionQueue.request(CONTEXT_SCOPE);
    }

    /** Reprojects the immutable policy snapshot onto all existing native invocation entities. */
    public static int refreshPolicyProjection() {
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) return 0;
        NativeRenderPolicy.Snapshot snapshot = NativeRenderBridge.policy().snapshot();
        int changed = 0;
        for (EntityId entity : context.entities()) {
            if (!context.world().contains(entity)) continue;
            NativeInvocationComponent nativeInvocation = context.world().get(entity,
                    NativeInvocationComponent.class);
            if (nativeInvocation == null) continue;
            NativeRenderExemptionComponent current = context.world().get(entity,
                    NativeRenderExemptionComponent.class);
            if (current != null && current.policyRevision == snapshot.revision) continue;
            boolean exempt = snapshot.exempt(nativeInvocation.invocation);
            context.world().put(entity, NativeRenderExemptionComponent.class,
                    new NativeRenderExemptionComponent(snapshot.revision, exempt,
                            snapshot.matchedTarget(nativeInvocation.invocation)));
            changed++;
        }
        if (changed > 0) RenderProjectionQueue.request(CONTEXT_SCOPE);
        return changed;
    }

    /**
     * ECS-sourced view of the exemption state projected onto each native invocation entity. The
     * bridge reads its own policy snapshot on the render path; this reader lets the probe report the
     * projected ECS data instead of duplicating policy state on the host boundary.
     */
    public static Map<String, Object> exemptionProjectionProbeSlice() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) {
            out.put("available", Boolean.FALSE);
            out.put("entities", Integer.valueOf(0));
            out.put("exemptEntities", Integer.valueOf(0));
            out.put("revision", Long.valueOf(0L));
            return out;
        }
        int entities = 0;
        int exemptEntities = 0;
        long revision = 0L;
        for (EntityId entity : context.entities()) {
            if (!context.world().contains(entity)) continue;
            if (!context.world().has(entity, NativeInvocationComponent.class)) continue;
            NativeRenderExemptionComponent exemption = context.world().get(entity,
                    NativeRenderExemptionComponent.class);
            if (exemption == null) continue;
            entities++;
            if (exemption.exempt) exemptEntities++;
            revision = Math.max(revision, exemption.policyRevision);
        }
        out.put("available", Boolean.TRUE);
        out.put("entities", Integer.valueOf(entities));
        out.put("exemptEntities", Integer.valueOf(exemptEntities));
        out.put("revision", Long.valueOf(revision));
        return out;
    }

    private static String localId(String ownerId) {
        return ownerId.replace(':', '_').replace('/', '_');
    }
}
