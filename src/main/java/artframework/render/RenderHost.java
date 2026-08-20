package artframework.render;

import artframework.component.Rect;
import artframework.presentation.EffectAttachment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Track-agnostic render attach surface. Pure bookkeeping + effect dispatch;
 * GL compile/draw optional via {@link #drawFrame}.
 */
public final class RenderHost {

    public static final String C2_SURFACE_PREFIX = "c2:surface:";

    private final EffectRegistry effects = new EffectRegistry();
    private final ShaderRegistry shaders = new ShaderRegistry();
    private final ShaderRuntime shaderRuntime = new ShaderRuntime();
    private final FrameCapture frameCapture = new FrameCapture();
    // Console/profile changes may update effects while BaseMod's post-render callback reads them.
    // Draw order is derived by z below, so insertion ordering is not a render contract here.
    private final Map<String, RenderTarget> targets = new ConcurrentHashMap<String, RenderTarget>();
    private final Map<String, List<EffectBinding>> bindings =
            new ConcurrentHashMap<String, List<EffectBinding>>();
    private HostRenderBackend hostBackend = DirectHostRenderBackend.INSTANCE;
    private boolean shadersReady;
    private float timeSeconds;
    private float screenW = 1920f;
    private float screenH = 1080f;

    public RenderHost() {
        installBuiltins();
    }

    public HostRenderBackend hostBackend() {
        return hostBackend;
    }

    public void setHostBackend(HostRenderBackend backend) {
        this.hostBackend = backend != null ? backend : DirectHostRenderBackend.INSTANCE;
    }

    private void installBuiltins() {
        effects.clear();
        effects.register(new TintEffect());
        effects.register(new GlowEffect(shaderRuntime));
        effects.register(new BlurEffect(shaderRuntime));
        effects.register(new GlassEffect(shaderRuntime));
        effects.register(new LightwaveEffect(shaderRuntime));
        shaders.register(
                GlowEffect.SHADER_ID,
                "shaders/glow.vert",
                "shaders/glow.frag");
        shaders.register(
                BlurEffect.SHADER_ID,
                "shaders/blur.vert",
                "shaders/blur.frag");
        shaders.register(
                GlassEffect.SHADER_ID,
                "shaders/glass.vert",
                "shaders/glass.frag");
        shaders.register(
                LightwaveEffect.SHADER_ID,
                "shaders/lightwave.vert",
                "shaders/lightwave.frag");
    }

    public FrameCapture frameCapture() {
        return frameCapture;
    }

    public boolean isCaptureEnabled() {
        return RenderStateEcs.captureEnabled();
    }

    /**
     * When true (or when any bound effect requires capture), screen is sampled before post FX.
     */
    public void setCaptureEnabled(boolean enabled) {
        RenderStateEcs.captureEnabled(enabled);
    }

    public EffectRegistry effects() {
        return effects;
    }

    public ShaderRegistry shaders() {
        return shaders;
    }

    public ShaderRuntime shaderRuntime() {
        return shaderRuntime;
    }

    /**
     * Compile registered GLSL on the GL thread. Safe to call multiple times.
     *
     * @return successful compile count
     */
    public int compileShaders() {
        int n = shaderRuntime.compileAll(shaders);
        shadersReady = n > 0 || shaders.ids().isEmpty();
        // Re-bind glow so it sees runtime (already does via shared instance)
        return n;
    }

    public boolean areShadersReady() {
        return shadersReady;
    }

    /** Canonical full-screen overlay target id. */
    public static final String FULL_FRAME_ID = "full_frame";

    public void setFullFrameEnabled(boolean enabled) {
        FullFrameRenderComponent current = RenderStateEcs.fullFrameState();
        RenderStateEcs.fullFrame(
                current != null ? current.bounds.width : screenW,
                current != null ? current.bounds.height : screenH,
                enabled,
                current != null ? current.effects() : Collections.<EffectAttachment>emptyList());
        rebuildFromEcsPlan();
    }

    public boolean isFullFrameEnabled() {
        FullFrameRenderComponent state = RenderStateEcs.fullFrameState();
        return state != null && state.enabled;
    }

    /**
     * Enable full-frame overlay and set screen bounds (logic units / pixels).
     */
    public RenderTarget enableFullFrame(float width, float height) {
        setFullFrameEnabled(true);
        return syncScreenBounds(width, height);
    }

    public void disableFullFrame() {
        setFullFrameEnabled(false);
        removeTarget(FULL_FRAME_ID);
    }

    /**
     * Update full-frame target bounds to the current screen. No-op if full-frame disabled.
     */
    public RenderTarget syncScreenBounds(float width, float height) {
        if (width <= 0f) {
            width = 1920f;
        }
        if (height <= 0f) {
            height = 1080f;
        }
        this.screenW = width;
        this.screenH = height;
        if (RenderStateEcs.fullFrameState() == null) {
            return null;
        }
        FullFrameRenderComponent current = RenderStateEcs.fullFrameState();
        if (current != null) {
            RenderStateEcs.fullFrame(width, height, current.enabled, current.effects());
            rebuildFromEcsPlan();
        }
        return targets.get(FULL_FRAME_ID);
    }

    public float screenWidth() {
        return screenW;
    }

    public float screenHeight() {
        return screenH;
    }

    public boolean needsCapture() {
        if (RenderStateEcs.captureEnabled()) {
            return true;
        }
        for (List<EffectBinding> list : bindings.values()) {
            if (list == null) {
                continue;
            }
            for (EffectBinding b : list) {
                if (!b.isEnabled()) {
                    continue;
                }
                Effect e = effects.get(b.effectId);
                if (e != null && e.requiresCapture()) {
                    return true;
                }
            }
        }
        return false;
    }

    public RenderTarget fullFrameTarget() {
        return targets.get(FULL_FRAME_ID);
    }

    /**
     * Bind effect on full-frame target (enables full-frame if needed).
     */
    public EffectBinding bindFullFrameEffect(String effectId, Map<String, Object> params) {
        FullFrameRenderComponent current = RenderStateEcs.fullFrameState();
        List<EffectAttachment> next = new ArrayList<EffectAttachment>();
        if (current != null) next.addAll(current.effects());
        next.add(new EffectAttachment(effectId,
                params != null && params.get("layer") != null
                        ? String.valueOf(params.get("layer")) : EffectBinding.LAYER_AMBIENT,
                params));
        RenderStateEcs.fullFrame(screenW, screenH, true, next);
        rebuildFromEcsPlan();
        return findEffect(FULL_FRAME_ID, effectId,
                params != null && params.get("layer") != null
                        ? String.valueOf(params.get("layer")) : EffectBinding.LAYER_AMBIENT);
    }

    RenderTarget ensureTarget(String id, RenderTargetKind kind) {
        RenderTarget t = targets.get(id);
        if (t == null) {
            t = new RenderTarget(id, kind);
            targets.put(id, t);
            bindings.put(id, new CopyOnWriteArrayList<EffectBinding>());
        } else if (t.kind != kind) {
            throw new IllegalArgumentException(
                    "target kind mismatch for " + id + ": " + t.kind + " vs " + kind);
        }
        return t;
    }

    public RenderTarget getTarget(String id) {
        return targets.get(id);
    }

    public static String c2SurfaceTargetId(String surfaceId) {
        if (surfaceId == null || surfaceId.trim().isEmpty()) {
            throw new IllegalArgumentException("surfaceId required");
        }
        return C2_SURFACE_PREFIX + surfaceId.trim();
    }

    public static String c2ItemTargetId(String surfaceId, String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId required");
        }
        return c2SurfaceTargetId(surfaceId) + ":item:" + itemId.trim();
    }

    /** Rebuild all ECS-owned C2/full-frame/entity targets from one immutable render plan. */
    public void recreateFromEcs() {
        rebuildFromEcsPlan();
    }

    /** Drop disposable host targets and bindings; retained presentation data stays in ECS. */
    public void clearHostCacheForRecreation() {
        clearTargets();
    }

    /** Release disposable host resources while retaining ECS render state and configuration. */
    public void recreateHostCache() {
        clearTargets();
        shaderRuntime.disposeAll();
        frameCapture.dispose();
        effects.clear();
        shaders.clear();
        shadersReady = false;
        installBuiltins();
    }

    void rebuildFromEcsPlan() {
        rebuildFromEcsPlan(null);
    }

    void rebuildFromEcsPlan(java.util.Set<String> activeSurfaceIds) {
        RenderPlan plan = RenderPlan.fromEcs(activeSurfaceIds);
        clearTargets();
        for (RenderPlan.Entry entry : plan.entries()) {
            RenderTarget target = ensureTarget(entry.id, entry.kind);
            target.setBounds(entry.bounds);
            target.setZ(entry.z);
            target.setEnabled(entry.enabled);
            clearEffects(entry.id);
            for (EffectAttachment attachment : entry.effects) {
                if (!effects.contains(attachment.effectId)) continue;
                bindEffect(entry.id, attachment.effectId, attachment.params());
                EffectBinding binding = findEffect(entry.id, attachment.effectId, attachment.layer);
                if (binding != null) binding.setEnabled(attachment.isEnabled());
            }
        }
    }

    private void removeTarget(String id) {
        if (id == null) {
            return;
        }
        targets.remove(id);
        bindings.remove(id);
    }

    private void removeTargetsWithPrefix(String prefix) {
        if (prefix == null) {
            return;
        }
        List<String> ids = new ArrayList<String>(targets.keySet());
        for (String id : ids) {
            if (id.startsWith(prefix)) {
                removeTarget(id);
            }
        }
    }

    void clearTargets() {
        targets.clear();
        bindings.clear();
    }

    /**
     * Bind effect to target. Unknown effect id → IllegalArgumentException.
     */
    EffectBinding bindEffect(String targetId, String effectId, Map<String, Object> params) {
        RenderTarget target = targets.get(targetId);
        if (target == null) {
            throw new IllegalArgumentException("unknown target: " + targetId);
        }
        if (target.kind == RenderTargetKind.FULL_FRAME && !isFullFrameEnabled()) {
            throw new IllegalArgumentException("FULL_FRAME disabled");
        }
        Effect effect = effects.get(effectId);
        if (effect == null) {
            throw new IllegalArgumentException("unknown effect: " + effectId);
        }
        EffectBinding binding = new EffectBinding(effectId, params);
        effect.validate(binding);
        List<EffectBinding> list = bindings.get(targetId);
        if (list == null) {
            list = new ArrayList<EffectBinding>();
            bindings.put(targetId, list);
        }
        list.add(binding);
        return binding;
    }

    void clearEffects(String targetId) {
        List<EffectBinding> list = bindings.get(targetId);
        if (list != null) {
            list.clear();
        }
    }

    /** First binding for effect+layer, or null. */
    public EffectBinding findEffect(String targetId, String effectId, String layer) {
        if (targetId == null || effectId == null) {
            return null;
        }
        List<EffectBinding> list = bindings.get(targetId);
        if (list == null) {
            return null;
        }
        String want =
                layer == null || layer.isEmpty() ? EffectBinding.LAYER_AMBIENT : layer.trim();
        for (EffectBinding b : list) {
            if (effectId.equals(b.effectId) && want.equals(b.layer())) {
                return b;
            }
        }
        return null;
    }

    public List<EffectBinding> effectsOf(String targetId) {
        List<EffectBinding> list = bindings.get(targetId);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    public int targetCount() {
        return targets.size();
    }

    public int bindingCount() {
        int n = 0;
        for (List<EffectBinding> list : bindings.values()) {
            n += list.size();
        }
        return n;
    }

    public List<String> listTargetIds() {
        return Collections.unmodifiableList(new ArrayList<String>(targets.keySet()));
    }

    public List<String> targetIdsWithPrefix(String prefix) {
        List<String> out = new ArrayList<String>();
        if (prefix == null) {
            return out;
        }
        for (String id : targets.keySet()) {
            if (id != null && id.startsWith(prefix)) {
                out.add(id);
            }
        }
        return out;
    }

    void tick(float deltaSeconds) {
        if (deltaSeconds > 0f) {
            timeSeconds += deltaSeconds;
        }
    }

    public float timeSeconds() {
        return timeSeconds;
    }

    /**
     * Capture screen (if needed) then draw all enabled targets (z-order).
     * Call with SpriteBatch begun; capture requires batch ended first (StageHost does that).
     */
    public void drawFrame(Object spriteBatch) {
        drawFrame(spriteBatch, false);
    }

    /**
     * @param alreadyCaptured if true, skip {@link FrameCapture#captureScreen}
     */
    public void drawFrame(Object spriteBatch, boolean alreadyCaptured) {
        drawFrame(spriteBatch, alreadyCaptured, null);
    }

    /**
     * @param kinds if non-null, only draw targets whose kind is in the set (e.g. C1 under UI vs
     *     FULL_FRAME over everything).
     */
    public void drawFrame(Object spriteBatch, boolean alreadyCaptured, java.util.Set<RenderTargetKind> kinds) {
        if (!alreadyCaptured && needsCapture() && hostBackend.supportsCapture()) {
            hostBackend.captureScreen(frameCapture, (int) screenW, (int) screenH);
        }
        prepareBlurChain();
        RenderContext ctx = new RenderContext(
                spriteBatch,
                timeSeconds,
                needsCapture() || frameCapture.hasTexture() ? frameCapture : null);
        // Snapshot values — StageHost may mutate targets during the same frame.
        List<RenderTarget> ordered = new ArrayList<RenderTarget>(targets.values());
        Collections.sort(ordered, new Comparator<RenderTarget>() {
            @Override
            public int compare(RenderTarget a, RenderTarget b) {
                return Float.compare(a.z(), b.z());
            }
        });
        for (RenderTarget target : ordered) {
            if (!target.isEnabled()) {
                continue;
            }
            if (kinds != null && !kinds.contains(target.kind)) {
                continue;
            }
            if (target.kind == RenderTargetKind.C2_SURFACE
                    && !LightwaveDiagnostics.c2EffectsEnabled()) {
                continue;
            }
            if (target.kind == RenderTargetKind.FULL_FRAME && !isFullFrameEnabled()) {
                continue;
            }
            List<EffectBinding> list = bindings.get(target.id);
            if (list == null) {
                continue;
            }
            for (EffectBinding binding : list) {
                if (!binding.isEnabled()) {
                    continue;
                }
                Effect effect = effects.get(binding.effectId);
                if (effect != null) {
                    hostBackend.drawEffect(effect, target, binding, ctx);
                }
            }
        }
    }

    /** C1 synthetic targets — draw under scene2d so labels/buttons stay readable. */
    public static java.util.Set<RenderTargetKind> kindsC1UnderUi() {
        java.util.EnumSet<RenderTargetKind> s = java.util.EnumSet.noneOf(RenderTargetKind.class);
        s.add(RenderTargetKind.SYNTHETIC_WINDOW);
        s.add(RenderTargetKind.SYNTHETIC_WIDGET);
        return s;
    }

    /** C2 surface/item effects draw below the ART-owned C2 chrome and labels. */
    public static java.util.Set<RenderTargetKind> kindsC2UnderPresent() {
        return java.util.EnumSet.of(RenderTargetKind.C2_SURFACE);
    }

    /** Overlay / full-frame / entity — draw after stage. */
    public static java.util.Set<RenderTargetKind> kindsOverUi() {
        java.util.EnumSet<RenderTargetKind> s = java.util.EnumSet.noneOf(RenderTargetKind.class);
        s.add(RenderTargetKind.FULL_FRAME);
        s.add(RenderTargetKind.OVERLAY);
        s.add(RenderTargetKind.ENTITY_SLOT);
        return s;
    }

    /**
     * White borders for C1 lightwave targets — must run <b>after</b> stage.draw so panel chrome
     * does not cover the stroke.
     */
    public void drawC1LightwaveBorders(Object spriteBatch) {
        RenderContext ctx =
                new RenderContext(
                        spriteBatch,
                        timeSeconds,
                        needsCapture() || frameCapture.hasTexture() ? frameCapture : null);
        // Copy to avoid ConcurrentModificationException if sync mutates targets mid-draw.
        java.util.List<RenderTarget> snapshot =
                new java.util.ArrayList<RenderTarget>(targets.values());
        for (int ti = 0; ti < snapshot.size(); ti++) {
            RenderTarget target = snapshot.get(ti);
            if (target == null || !target.isEnabled()) {
                continue;
            }
            if (target.kind != RenderTargetKind.SYNTHETIC_WIDGET
                    && target.kind != RenderTargetKind.SYNTHETIC_WINDOW) {
                continue;
            }
            List<EffectBinding> list = bindings.get(target.id);
            if (list == null) {
                continue;
            }
            for (EffectBinding binding : list) {
                if (!binding.isEnabled() || !LightwaveEffect.ID.equals(binding.effectId)) {
                    continue;
                }
                Effect effect = effects.get(LightwaveEffect.ID);
                if (effect instanceof LightwaveEffect) {
                    ((LightwaveEffect) effect).drawBorderOnly(target, binding, ctx);
                }
            }
        }
    }

    /**
     * Capture then draw. Prefer when batch is ended (GL copy from default FB).
     */
    public void captureAndDraw(Object spriteBatch) {
        if (needsCapture() && hostBackend.supportsCapture()) {
            hostBackend.captureScreen(frameCapture, (int) screenW, (int) screenH);
        }
        drawFrame(spriteBatch, true);
    }

    private void prepareBlurChain() {
        if (!frameCapture.hasTexture() || !hostBackend.supportsShaders()) {
            return;
        }
        boolean blurNeeded = false;
        for (List<EffectBinding> list : bindings.values()) {
            if (list == null) continue;
            for (EffectBinding binding : list) {
                if (binding.isEnabled()
                        && (BlurEffect.ID.equals(binding.effectId)
                                || GlassEffect.ID.equals(binding.effectId))) {
                    blurNeeded = true;
                    break;
                }
            }
            if (blurNeeded) break;
        }
        if (blurNeeded) {
            frameCapture.prepareBlur(shaderRuntime.get(BlurEffect.SHADER_ID), 2f);
        }
    }

    public Map<String, Object> probeMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("targetCount", Integer.valueOf(targetCount()));
        out.put("bindingCount", Integer.valueOf(bindingCount()));
        out.put("fullFrameEnabled", Boolean.valueOf(isFullFrameEnabled()));
        out.put("captureEnabled", Boolean.valueOf(isCaptureEnabled()));
        out.put("needsCapture", Boolean.valueOf(needsCapture()));
        out.put("hostSupportsCapture", Boolean.valueOf(hostBackend.supportsCapture()));
        out.put("hostSupportsShaders", Boolean.valueOf(hostBackend.supportsShaders()));
        out.put("shadersReady", Boolean.valueOf(shadersReady));
        out.put("captureStatus", captureStatus());
        out.put("shaderStatus", shaderStatus());
        out.put("shaderProgramCount", Integer.valueOf(shaderRuntime.programCount()));
        out.put("effectIds", new ArrayList<String>(effects.ids()));
        out.put("shaderIds", new ArrayList<String>(shaders.ids()));
        FrameCapture.MapProbe cap = frameCapture.probe();
        Map<String, Object> capMap = new LinkedHashMap<String, Object>();
        capMap.put("hasTexture", Boolean.valueOf(cap.hasTexture));
        capMap.put("lastOk", Boolean.valueOf(cap.lastOk));
        capMap.put("width", Integer.valueOf(cap.width));
        capMap.put("height", Integer.valueOf(cap.height));
        capMap.put("scale", Integer.valueOf(cap.scale));
        capMap.put("blurPasses", Integer.valueOf(cap.blurPasses));
        capMap.put("hasBlurredTexture", Boolean.valueOf(cap.hasBlurredTexture));
        capMap.put("lastError", cap.lastError);
        out.put("capture", capMap);
        List<Map<String, Object>> shaderStatus = new ArrayList<Map<String, Object>>();
        for (String sid : shaders.ids()) {
            ShaderRegistry.ShaderDef def = shaders.get(sid);
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("id", sid);
            one.put("compiled", Boolean.valueOf(def != null && def.isCompiled()));
            one.put("failed", Boolean.valueOf(def != null && def.isCompileFailed()));
            one.put("hasProgram", Boolean.valueOf(shaderRuntime.has(sid)));
            if (def != null && def.isCompileFailed()) {
                one.put("message", def.compileMessage());
            }
            shaderStatus.add(one);
        }
        out.put("shaders", shaderStatus);
        List<Map<String, Object>> tlist = new ArrayList<Map<String, Object>>();
        Map<String, Object> bySafeId = new LinkedHashMap<String, Object>();
        for (RenderTarget t : targets.values()) {
            Map<String, Object> one = probeTarget(t);
            tlist.add(one);
            bySafeId.put(safeTargetKey(t.id), one);
        }
        out.put("targets", tlist);
        out.put("targetsById", bySafeId);
        out.put("demoEffects", probeDemoEffects());
        out.put("c2SurfaceEffects", probeC2SurfaceEffects());
        out.put("c2ItemEffects", probeC2ItemEffects());
        out.put("lightwaveDiagnostics", LightwaveDiagnostics.probeSummary());
        return out;
    }

    private Map<String, Object> probeC2SurfaceEffects() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (RenderTarget t : targets.values()) {
            if (t.kind == RenderTargetKind.C2_SURFACE) {
                out.put(safeTargetKey(t.id.substring(C2_SURFACE_PREFIX.length())), probeTarget(t));
            }
        }
        return out;
    }

    private Map<String, Object> probeC2ItemEffects() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (RenderTarget target : targets.values()) {
            if (target.kind != RenderTargetKind.C2_SURFACE) {
                continue;
            }
            int marker = target.id.indexOf(":item:");
            if (marker < 0 || !target.id.startsWith(C2_SURFACE_PREFIX)) {
                continue;
            }
            String surface = target.id.substring(C2_SURFACE_PREFIX.length(), marker);
            String key = safeTargetKey(surface);
            @SuppressWarnings("unchecked")
            Map<String, Object> group = (Map<String, Object>) out.get(key);
            if (group == null) {
                group = new LinkedHashMap<String, Object>();
                group.put("count", Integer.valueOf(0));
                group.put("targets", new ArrayList<Map<String, Object>>());
                out.put(key, group);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) group.get("targets");
            list.add(probeTarget(target));
            group.put("count", Integer.valueOf(list.size()));
        }
        return out;
    }

    private Map<String, Object> probeTarget(RenderTarget t) {
        Map<String, Object> one = new LinkedHashMap<String, Object>();
        one.put("id", t.id);
        one.put("kind", t.kind.name());
        one.put("enabled", Boolean.valueOf(t.isEnabled()));
        one.put("x", Float.valueOf(t.x()));
        one.put("y", Float.valueOf(t.y()));
        one.put("w", Float.valueOf(t.width()));
        one.put("h", Float.valueOf(t.height()));
        List<EffectBinding> efs = effectsOf(t.id);
        one.put("effectCount", Integer.valueOf(efs.size()));
        List<String> ids = new ArrayList<String>();
        List<Map<String, Object>> detail = new ArrayList<Map<String, Object>>();
        boolean borderDrawn = false;
        for (EffectBinding b : efs) {
            ids.add(b.effectId);
            Map<String, Object> em = new LinkedHashMap<String, Object>();
            em.put("id", b.effectId);
            em.put("enabled", Boolean.valueOf(b.isEnabled()));
            if (LightwaveEffect.ID.equals(b.effectId)) {
                em.put("intensity", Float.valueOf(b.paramFloat("intensity", 0.55f)));
                em.put("angle", Float.valueOf(b.paramFloat("angle", 35f)));
                boolean bd = LightwaveEffect.shouldDrawBorder(b);
                em.put("borderDrawn", Boolean.valueOf(bd));
                if (bd) {
                    borderDrawn = true;
                }
            }
            detail.add(em);
        }
        one.put("effectIds", ids);
        one.put("effects", detail);
        one.put("borderDrawn", Boolean.valueOf(borderDrawn));
        return one;
    }

    /**
     * Showcase slice for YAML-friendly paths (no colon keys): lightwave_demo panel contract.
     */
    private Map<String, Object> probeDemoEffects() {
        Map<String, Object> demo = new LinkedHashMap<String, Object>();
        String panelId = "c1:lightwave_demo:panel";
        RenderTarget panel = targets.get(panelId);
        Map<String, Object> lw = new LinkedHashMap<String, Object>();
        lw.put("windowId", "lightwave_demo");
        lw.put("targetId", panelId);
        if (panel == null) {
            lw.put("bound", Boolean.FALSE);
            demo.put("lightwave_demo", lw);
            return demo;
        }
        Map<String, Object> slice = probeTarget(panel);
        lw.put("bound", Boolean.TRUE);
        lw.put("x", slice.get("x"));
        lw.put("y", slice.get("y"));
        lw.put("w", slice.get("w"));
        lw.put("h", slice.get("h"));
        lw.put("effectIds", slice.get("effectIds"));
        lw.put("effectCount", slice.get("effectCount"));
        lw.put("borderDrawn", slice.get("borderDrawn"));
        @SuppressWarnings("unchecked")
        List<String> eids = (List<String>) slice.get("effectIds");
        lw.put(
                "hasLightwave",
                Boolean.valueOf(eids != null && eids.contains(LightwaveEffect.ID)));
        demo.put("lightwave_demo", lw);
        return demo;
    }

    /** Dot-path safe key for render target ids. */
    static String safeTargetKey(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        return id.replace(':', '_').replace('.', '_');
    }

    private String captureStatus() {
        if (!needsCapture()) {
            return "disabled";
        }
        if (!hostBackend.supportsCapture()) {
            return "unsupported";
        }
        FrameCapture.MapProbe probe = frameCapture.probe();
        if (probe.lastOk) {
            return "ready";
        }
        return probe.lastError != null ? "failed" : "pending";
    }

    private String shaderStatus() {
        if (!hostBackend.supportsShaders()) {
            return "unsupported";
        }
        if (shaders.ids().isEmpty()) {
            return "none";
        }
        if (shadersReady) {
            return "ready";
        }
        for (String id : shaders.ids()) {
            ShaderRegistry.ShaderDef def = shaders.get(id);
            if (def != null && def.isCompileFailed()) {
                return "failed";
            }
        }
        return "pending";
    }

    public void resetForTests() {
        RenderStateEcs.resetForTests();
        clearTargets();
        shaderRuntime.disposeAll();
        frameCapture.dispose();
        effects.clear();
        shaders.clear();
        hostBackend = DirectHostRenderBackend.INSTANCE;
        RenderStateEcs.captureEnabled(false);
        shadersReady = false;
        timeSeconds = 0f;
        screenW = 1920f;
        screenH = 1080f;
        installBuiltins();
    }
}
