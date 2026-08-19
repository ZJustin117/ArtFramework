package artframework.core;

import artframework.component.EffectDecl;
import artframework.presentation.PresentationRegistry;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;
import artframework.render.RenderStateEcs;
import artframework.render.RenderSurfaceComponent;
import artframework.render.FullFrameRenderComponent;
import artframework.presentation.EffectAttachment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies active {@link PresentPack} ambient tables: fullFrame, surface binds, and
 * effectDefaults consulted by RenderHost when a node has empty effects. Pack data only.
 */
public final class PresentPackApply {

    private static final List<String> BOUND_SURFACES = new ArrayList<String>();
    private static final Map<String, String> PREVIOUS_SURFACE_BINDINGS =
            new LinkedHashMap<String, String>();
    private static final Map<String, String> APPLIED_SURFACE_BINDINGS =
            new LinkedHashMap<String, String>();
    private static final List<String> BOUND_C2_EFFECTS = new ArrayList<String>();
    private static final Map<String, RenderSurfaceComponent> PREVIOUS_C2_SURFACES =
            new LinkedHashMap<String, RenderSurfaceComponent>();
    private static final Map<String, RenderSurfaceComponent> APPLIED_C2_SURFACES =
            new LinkedHashMap<String, RenderSurfaceComponent>();
    private static FullFrameRenderComponent previousFullFrame;
    private static FullFrameRenderComponent appliedFullFrame;
    private static boolean hadPreviousFullFrame;
    private static boolean managedFullFrame;

    private PresentPackApply() {}

    /** After pack activate/deactivate: fullFrame + surfaces + resync open C1 effect binds. */
    public static void syncFromActivePack() {
        PresentPack pack = PresentPacks.active();
        RuntimeException cleanupFailure = clearManagedAmbient();
        if (cleanupFailure != null) throw cleanupFailure;
        if (pack == null) {
            resyncOpenC1Render();
        } else {
            applyFullFrame(pack);
            applySurfaceBinds(pack);
            applyC2SurfaceEffects(pack);
            resyncOpenC1Render();
        }
        artframework.render.RenderProjectionQueue.projectNow();
    }

    /**
     * Extra effects for a node type when layout effects are empty (active pack table).
     */
    public static List<EffectDecl> effectDefaultsForType(String type) {
        PresentPack pack = PresentPacks.active();
        if (pack == null || type == null) {
            return Collections.emptyList();
        }
        return pack.effectDefaultsFor(type);
    }

    private static void applyFullFrame(PresentPack pack) {
        List<EffectDecl> declarations = PackFullFrameEffects.effects(
                PresentationRegistry.world(), pack.id);
        if (!PackFullFrameEffects.hasContribution(PresentationRegistry.world(), pack.id)) {
            declarations = pack.fullFrameEffects;
        }
        if (declarations.isEmpty()) {
            return;
        }
        if (!hadPreviousFullFrame) {
            previousFullFrame = RenderStateEcs.fullFrameState();
            hadPreviousFullFrame = true;
        }
        try {
            RenderHost host = RenderHosts.get();
            float w = host.screenWidth();
            float h = host.screenHeight();
            if (w <= 0f) {
                w = 1920f;
            }
            if (h <= 0f) {
                h = 1080f;
            }
            List<EffectAttachment> effects = new ArrayList<EffectAttachment>();
            for (EffectDecl d : declarations) {
                Map<String, Object> params = new LinkedHashMap<String, Object>();
                if (d.params != null) {
                    params.putAll(d.params);
                }
                String layer = params.get("layer") != null
                        ? String.valueOf(params.get("layer")) : "ambient";
                effects.add(new EffectAttachment(d.id, layer, params));
            }
            RenderStateEcs.fullFrame(w, h, true, effects);
            appliedFullFrame = RenderStateEcs.fullFrameState();
            managedFullFrame = true;
        } catch (Throwable ignored) {
        }
    }

    private static void applySurfaceBinds(PresentPack pack) {
        Map<String, String> bindings = PackSurfaceBindings.forPack(
                PresentationRegistry.world(), pack.id);
        if (PackSurfaceBindings.hasContribution(PresentationRegistry.world(), pack.id)) {
            for (Map.Entry<String, String> entry : bindings.entrySet()) {
                try {
                    rememberPreviousBinding(entry.getKey());
                    SurfacePresent.bind(entry.getKey(), entry.getValue());
                    BOUND_SURFACES.add(entry.getKey());
                    APPLIED_SURFACE_BINDINGS.put(entry.getKey(), entry.getValue());
                } catch (RuntimeException ignored) {
                }
            }
            return;
        }
        String profile =
                pack.profileId != null && !pack.profileId.isEmpty() ? pack.profileId : pack.id;
        if (!PresentProfiles.contains(profile)) {
            return;
        }
        for (String sid : pack.bindSurfaces) {
            try {
                    rememberPreviousBinding(sid);
                    SurfacePresent.bind(sid, profile);
                BOUND_SURFACES.add(sid);
                APPLIED_SURFACE_BINDINGS.put(sid, profile);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void applyC2SurfaceEffects(PresentPack pack) {
        if (PackSurfaceEffects.hasContribution(PresentationRegistry.world(), pack.id)) {
                for (String surfaceId : PackSurfaceEffects.surfaceIds(PresentationRegistry.world(), pack.id)) {
                    rememberPreviousC2Surface(surfaceId);
                    List<EffectAttachment> effects = toAttachments(
                         PackSurfaceEffects.forSurface(PresentationRegistry.world(), pack.id, surfaceId));
                if (!effects.isEmpty()) {
                    RenderStateEcs.surfaceEffects(surfaceId, effects);
                    BOUND_C2_EFFECTS.add(surfaceId);
                    APPLIED_C2_SURFACES.put(surfaceId, RenderStateEcs.surfaceState(surfaceId));
                }
            }
            return;
        }
        if (pack.surfaceEffects.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<artframework.component.EffectDecl>> entry
                : pack.surfaceEffects.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            try {
                List<EffectAttachment> effects = toAttachments(entry.getValue());
                rememberPreviousC2Surface(entry.getKey());
                RenderStateEcs.surfaceEffects(entry.getKey(), effects);
                BOUND_C2_EFFECTS.add(entry.getKey());
                APPLIED_C2_SURFACES.put(entry.getKey(), RenderStateEcs.surfaceState(entry.getKey()));
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static List<EffectAttachment> toAttachments(List<artframework.component.EffectDecl> declarations) {
        List<EffectAttachment> effects = new ArrayList<EffectAttachment>();
        for (artframework.component.EffectDecl d : declarations) {
            String layer = d.params != null && d.params.get("layer") != null
                    ? String.valueOf(d.params.get("layer")) : "ambient";
            effects.add(new EffectAttachment(d.id, layer, d.params));
        }
        return effects;
    }

    private static void resyncOpenC1Render() {
        // C1 targets are derived from every open context by the complete render plan projection.
    }

    private static RuntimeException clearManagedAmbient() {
        RuntimeException failure = null;
        for (String sid : new ArrayList<String>(BOUND_SURFACES)) {
            try {
                String previous = PREVIOUS_SURFACE_BINDINGS.get(sid);
                String applied = APPLIED_SURFACE_BINDINGS.get(sid);
                if (applied == null || applied.equals(SurfacePresent.profileId(sid))) {
                    if (previous == null) SurfacePresent.unbind(sid);
                    else SurfacePresent.bind(sid, previous);
                }
                BOUND_SURFACES.remove(sid);
                PREVIOUS_SURFACE_BINDINGS.remove(sid);
                APPLIED_SURFACE_BINDINGS.remove(sid);
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        for (String sid : new ArrayList<String>(BOUND_C2_EFFECTS)) {
            if (sameSurface(RenderStateEcs.surfaceState(sid), APPLIED_C2_SURFACES.get(sid))) {
                try {
                    RenderStateEcs.restoreSurface(sid, PREVIOUS_C2_SURFACES.get(sid));
                    BOUND_C2_EFFECTS.remove(sid);
                    PREVIOUS_C2_SURFACES.remove(sid);
                    APPLIED_C2_SURFACES.remove(sid);
                } catch (RuntimeException e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                }
            } else {
                BOUND_C2_EFFECTS.remove(sid);
                PREVIOUS_C2_SURFACES.remove(sid);
                APPLIED_C2_SURFACES.remove(sid);
            }
        }
        if (hadPreviousFullFrame && sameFullFrame(RenderStateEcs.fullFrameState(), appliedFullFrame)) {
            try {
                RenderStateEcs.restoreFullFrame(previousFullFrame);
                hadPreviousFullFrame = false;
                previousFullFrame = null;
                appliedFullFrame = null;
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        } else if (hadPreviousFullFrame) {
            hadPreviousFullFrame = false;
            previousFullFrame = null;
            appliedFullFrame = null;
        }
        managedFullFrame = false;
        return failure;
    }

    private static void rememberPreviousBinding(String surfaceId) {
        if (!PREVIOUS_SURFACE_BINDINGS.containsKey(surfaceId)) {
            PREVIOUS_SURFACE_BINDINGS.put(surfaceId, SurfacePresent.profileId(surfaceId));
        }
    }

    private static void rememberPreviousC2Surface(String surfaceId) {
        if (!PREVIOUS_C2_SURFACES.containsKey(surfaceId)) {
            PREVIOUS_C2_SURFACES.put(surfaceId, RenderStateEcs.surfaceState(surfaceId));
        }
    }

    private static boolean sameSurface(RenderSurfaceComponent a, RenderSurfaceComponent b) {
        if (a == b) return true;
        if (a == null || b == null || !a.surfaceId.equals(b.surfaceId)
                || a.enabled != b.enabled || a.z != b.z
                || a.bounds.x != b.bounds.x || a.bounds.y != b.bounds.y
                || a.bounds.width != b.bounds.width || a.bounds.height != b.bounds.height) return false;
        return sameEffects(a.effects(), b.effects());
    }

    private static boolean sameFullFrame(FullFrameRenderComponent a, FullFrameRenderComponent b) {
        if (a == b) return true;
        if (a == null || b == null || a.enabled != b.enabled
                || a.bounds.x != b.bounds.x || a.bounds.y != b.bounds.y
                || a.bounds.width != b.bounds.width || a.bounds.height != b.bounds.height) return false;
        return sameEffects(a.effects(), b.effects());
    }

    private static boolean sameEffects(List<EffectAttachment> a, List<EffectAttachment> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            EffectAttachment left = a.get(i);
            EffectAttachment right = b.get(i);
            if (!left.effectId.equals(right.effectId) || !left.layer.equals(right.layer)
                    || !left.params().equals(right.params())) return false;
        }
        return true;
    }

    public static Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("boundSurfaces", new ArrayList<String>(BOUND_SURFACES));
        m.put("boundC2Effects", new ArrayList<String>(BOUND_C2_EFFECTS));
        m.put("managedFullFrame", Boolean.valueOf(managedFullFrame));
        PresentPack p = PresentPacks.active();
        m.put("activePack", p != null ? p.id : "");
        if (p != null) {
            m.put("effectDefaultTypes", new ArrayList<String>(p.effectDefaults.keySet()));
            m.put("fullFrameEffectCount", Integer.valueOf(p.fullFrameEffects.size()));
        }
        return Collections.unmodifiableMap(m);
    }

    public static void resetForTests() {
        RuntimeException failure = clearManagedAmbient();
        artframework.render.RenderProjectionQueue.projectNow();
        if (failure != null) throw failure;
    }
}
