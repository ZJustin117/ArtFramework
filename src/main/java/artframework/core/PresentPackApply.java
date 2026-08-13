package artframework.core;

import artframework.component.EffectDecl;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;
import artframework.render.RenderStateEcs;
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
    private static final List<String> BOUND_C2_EFFECTS = new ArrayList<String>();
    private static boolean managedFullFrame;

    private PresentPackApply() {}

    /** After pack activate/deactivate: fullFrame + surfaces + resync open C1 effect binds. */
    public static void syncFromActivePack() {
        PresentPack pack = PresentPacks.active();
        clearManagedAmbient();
        if (pack == null) {
            resyncOpenC1Render();
            return;
        }
        applyFullFrame(pack);
        applySurfaceBinds(pack);
        applyC2SurfaceEffects(pack);
        resyncOpenC1Render();
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
        if (pack.fullFrameEffects.isEmpty()) {
            return;
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
            for (EffectDecl d : pack.fullFrameEffects) {
                Map<String, Object> params = new LinkedHashMap<String, Object>();
                if (d.params != null) {
                    params.putAll(d.params);
                }
                String layer = params.get("layer") != null
                        ? String.valueOf(params.get("layer")) : "ambient";
                effects.add(new EffectAttachment(d.id, layer, params));
            }
            RenderStateEcs.fullFrame(w, h, true, effects);
            host.syncRenderState();
            managedFullFrame = true;
        } catch (Throwable ignored) {
        }
    }

    private static void applySurfaceBinds(PresentPack pack) {
        String profile =
                pack.profileId != null && !pack.profileId.isEmpty() ? pack.profileId : pack.id;
        if (!PresentProfiles.contains(profile)) {
            return;
        }
        for (String sid : pack.bindSurfaces) {
            try {
                SurfacePresent.bind(sid, profile);
                BOUND_SURFACES.add(sid);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void applyC2SurfaceEffects(PresentPack pack) {
        if (pack.surfaceEffects.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<artframework.component.EffectDecl>> entry
                : pack.surfaceEffects.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            try {
                List<EffectAttachment> effects = new ArrayList<EffectAttachment>();
                for (artframework.component.EffectDecl d : entry.getValue()) {
                    String layer = d.params != null && d.params.get("layer") != null
                            ? String.valueOf(d.params.get("layer")) : "ambient";
                    effects.add(new EffectAttachment(d.id, layer, d.params));
                }
                RenderStateEcs.surfaceEffects(entry.getKey(), effects);
                RenderHosts.get().syncRenderState();
                BOUND_C2_EFFECTS.add(entry.getKey());
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void resyncOpenC1Render() {
        for (String winId : artframework.presentation.NodeTrees.listOpenIds()) {
            artframework.presentation.NodeTree tree = artframework.presentation.NodeTrees.get(winId);
            if (tree == null) {
                continue;
            }
            try {
                RenderHosts.get().syncFrame(
                        tree.frame(), artframework.render.RenderTargetKind.SYNTHETIC_WIDGET, winId);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void clearManagedAmbient() {
        for (String sid : new ArrayList<String>(BOUND_SURFACES)) {
            try {
                SurfacePresent.unbind(sid);
            } catch (RuntimeException ignored) {
            }
        }
        BOUND_SURFACES.clear();
        for (String sid : new ArrayList<String>(BOUND_C2_EFFECTS)) {
            RenderStateEcs.removeSurface(sid);
        }
        BOUND_C2_EFFECTS.clear();
        RenderHosts.get().syncRenderState();
        if (managedFullFrame) {
            RenderStateEcs.removeFullFrame();
            RenderHosts.get().syncRenderState();
            managedFullFrame = false;
        }
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
        clearManagedAmbient();
    }
}
