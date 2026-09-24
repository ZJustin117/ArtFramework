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
import artframework.render.NativeRenderFamilyClassifier;
import artframework.render.NativeRenderInputComponent;
import artframework.render.NativeRenderOwnership;
import artframework.render.RenderPhase;
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
    /**
     * Explicit, stable surface family namespace. A surface invocation carries its surface owner id
     * in {@code surfaceFamily}, not an NRCC VFX family, so the projected native render family is
     * {@code surface:<surfaceId>}. It is deliberately distinct from vfx families and is never
     * produced by {@link NativeRenderFamilyClassifier}.
     */
    private static final String SURFACE_FAMILY_PREFIX = "surface:";

    private Sts1NativePresentationAdapter() {}

    public static String present(NativeRenderInvocation invocation) {
        // Untyped projection: the transient-effect observer path. Effect owners project
        // OBSERVED input; every other owner keeps the legacy behavior of carrying no input.
        return present(invocation, null);
    }

    /**
     * Projects one bridged native invocation with the ownership disposition resolved by
     * {@link NativeRenderBridge}. Surface owners keep an ECS native render input whose
     * ownership equals the bridge result (delegated pixels are DELEGATED_TO_ART, retained native
     * pixels are NATIVE_WITH_ART_OVERLAY, no claim is OBSERVED); transient-effect owners stay
     * observation-only (OBSERVED) exactly as before.
     */
    public static String present(NativeRenderInvocation invocation, NativeRenderOwnership ownership) {
        if (invocation == null) throw new IllegalArgumentException("invocation required");
        PresentationContext context = PresentationRegistry.context(CONTEXT_SCOPE);
        EntityId entity = ensureEntity(context, invocation);
        projectInvocationComponents(context, entity, invocation);
        NativeRenderOwnership effective = isTransientEffectOwner(invocation)
                ? NativeRenderOwnership.OBSERVED
                : ownership;
        if (effective != null) {
            projectNativeRenderInput(context, entity, invocation, effective);
        } else {
            context.world().remove(entity, NativeRenderInputComponent.class);
        }
        projectExemption(context, entity, invocation);
        RenderProjectionQueue.request(CONTEXT_SCOPE);
        return entity.toString();
    }

    /**
     * Ownership recorded for one bridge disposition. Only {@code DELEGATE_TO_ART} declares ART
     * pixel ownership; capture/pass dispositions keep native pixels authoritative and merely
     * record an overlay input; blocked/fail-open claim nothing.
     */
    public static NativeRenderOwnership ownershipFor(RenderDisposition.Mode mode) {
        if (mode == null) return NativeRenderOwnership.OBSERVED;
        switch (mode) {
            case DELEGATE_TO_ART:
                return NativeRenderOwnership.DELEGATED_TO_ART;
            case CAPTURE_AND_PASS:
            case PASS_THROUGH:
                return NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY;
            case BLOCKED:
            case FAIL_OPEN:
            default:
                return NativeRenderOwnership.OBSERVED;
        }
    }

    /**
     * Re-projects the native render input ownership of an already-projected bridge owner. Used
     * when the authoritative ledger disposition differs from the initially proposed disposition
     * (for example a recovery fail-open winning the race) so the ECS input never overstates
     * ownership. Strictly reconcile-only: when the entity carries no input this is a no-op, so
     * pass-through / fail-open frames never fabricate a pixel claim for an unprojected owner.
     */
    public static void projectInput(NativeRenderInvocation invocation,
            NativeRenderOwnership ownership) {
        if (invocation == null) throw new IllegalArgumentException("invocation required");
        if (ownership == null) throw new IllegalArgumentException("ownership required");
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) return;
        EntityId entity = context.entity(new PresentationKey(KEY_SCOPE, localId(invocation.ownerId)));
        if (entity == null) return;
        if (!context.world().has(entity, NativeRenderInputComponent.class)) return;
        NativeRenderOwnership effective = isTransientEffectOwner(invocation)
                ? NativeRenderOwnership.OBSERVED
                : ownership;
        NativeRenderInputComponent current =
                context.world().get(entity, NativeRenderInputComponent.class);
        if (current != null && current.ownership() == effective) return;
        projectNativeRenderInput(context, entity, invocation, effective);
        RenderProjectionQueue.request(CONTEXT_SCOPE);
    }

    /** Stable surface family id used when no NRCC vfx family can be derived for a surface. */
    static String surfaceNativeRenderFamily(NativeRenderInvocation invocation) {
        String key = invocation.surfaceFamily;
        if (key == null || key.trim().isEmpty()) key = invocation.ownerId;
        return SURFACE_FAMILY_PREFIX + key.trim();
    }

    private static EntityId ensureEntity(PresentationContext context,
            NativeRenderInvocation invocation) {
        PresentationKey key = new PresentationKey(KEY_SCOPE, localId(invocation.ownerId));
        EntityId entity = context.entity(key);
        if (entity == null) {
            entity = context.create(key, invocation.ownerId, "native_render", invocation.nativeClass);
        }
        return entity;
    }

    private static void projectInvocationComponents(PresentationContext context, EntityId entity,
            NativeRenderInvocation invocation) {
        Rect bounds = boundsOf(invocation);
        context.world().put(entity, BoundsComponent.class, new BoundsComponent(bounds, 0f));
        context.world().put(entity, VisibilityComponent.class,
                new VisibilityComponent(true, 1f));
        context.world().put(entity, DrawComponent.class,
                new DrawComponent(invocation.nativeMethod, "", invocation.sourceIdentity));
        context.world().put(entity, HostBindingComponent.class,
                new HostBindingComponent(HOST_KIND, invocation.ownerId));
        context.world().put(entity, NativeInvocationComponent.class,
                new NativeInvocationComponent(invocation));
    }

    private static void projectExemption(PresentationContext context, EntityId entity,
            NativeRenderInvocation invocation) {
        NativeRenderPolicy policy = NativeRenderBridge.policy();
        context.world().put(entity, NativeRenderExemptionComponent.class,
                new NativeRenderExemptionComponent(policy.revision(), policy.exempt(invocation),
                        policy.matchedTarget(invocation)));
    }

    private static void projectNativeRenderInput(PresentationContext context, EntityId entity,
            NativeRenderInvocation invocation, NativeRenderOwnership ownership) {
        boolean effect = isTransientEffectOwner(invocation);
        // Effect family comes from the host-neutral classifier (a controlled subset of the
        // authoritative tools/nrcc/families.py rules); unmatched classes fail open to the
        // vfx-misc-root effect-container fallback, matching the bridge's fail-open semantics.
        // Surfaces have no NRCC vfx family: they use the explicit surface namespace instead.
        String family = effect
                ? NativeRenderFamilyClassifier.classify(invocation.nativeClass)
                : surfaceNativeRenderFamily(invocation);
        RenderPhase phase = effect ? RenderPhase.ART_EFFECTS : RenderPhase.NATIVE_RETAINED;
        context.world().put(entity, NativeRenderInputComponent.class,
                new NativeRenderInputComponent(family, invocation.ownerId, phase, 0f,
                        invocation.ownerId, boundsOf(invocation), true, ownership,
                        invocation.scene, invocation.frameId));
    }

    private static boolean isTransientEffectOwner(NativeRenderInvocation invocation) {
        return invocation.ownerId.startsWith(TRANSIENT_EFFECT_OWNER_PREFIX);
    }

    private static Rect boundsOf(NativeRenderInvocation invocation) {
        return invocation.boundsHint != null ? invocation.boundsHint : Rect.ZERO;
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

    /**
     * Withdraws the projected native render input of surface-owned (non {@code effect:}) bridge
     * owners. A surface entity itself is retained presentation state, so only its
     * {@link NativeRenderInputComponent} is removed; the entity falls back to the live presentation
     * frame exactly as a never-projected owner does. This drops a stale retained ownership claim
     * (frame/scene/bounds/ownership) after a recovery or scene boundary without touching the
     * transient-effect entities or effect inputs that {@link #clearTransientEffects()} owns.
     */
    public static void clearSurfaceInputs() {
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) return;
        boolean changed = false;
        for (EntityId entity : new java.util.ArrayList<EntityId>(context.entities())) {
            if (!context.world().has(entity, NativeRenderInputComponent.class)) continue;
            HostBindingComponent binding = context.world().get(entity, HostBindingComponent.class);
            if (binding == null) continue;
            if (binding.localKey.startsWith(TRANSIENT_EFFECT_OWNER_PREFIX)) continue;
            context.world().remove(entity, NativeRenderInputComponent.class);
            changed = true;
        }
        if (changed) RenderProjectionQueue.request(CONTEXT_SCOPE);
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
