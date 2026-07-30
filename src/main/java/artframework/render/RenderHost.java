package artframework.render;

import artframework.component.ArtNodeTypes;
import artframework.component.EffectDecl;
import artframework.component.LayoutEngine;
import artframework.component.LayoutResult;
import artframework.component.Rect;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.component.WidgetSession;
import artframework.c2.EntitySlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Track-agnostic render attach surface. Pure bookkeeping + effect dispatch;
 * GL compile/draw optional via {@link #drawFrame}.
 */
public final class RenderHost {

    private final EffectRegistry effects = new EffectRegistry();
    private final ShaderRegistry shaders = new ShaderRegistry();
    private final ShaderRuntime shaderRuntime = new ShaderRuntime();
    private final FrameCapture frameCapture = new FrameCapture();
    private final Map<String, RenderTarget> targets = new LinkedHashMap<String, RenderTarget>();
    private final Map<String, List<EffectBinding>> bindings = new LinkedHashMap<String, List<EffectBinding>>();
    private HostRenderBackend hostBackend = DirectHostRenderBackend.INSTANCE;
    private boolean fullFrameEnabled;
    private boolean captureEnabled;
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
        return captureEnabled;
    }

    /**
     * When true (or when any bound effect requires capture), screen is sampled before post FX.
     */
    public void setCaptureEnabled(boolean enabled) {
        this.captureEnabled = enabled;
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
        this.fullFrameEnabled = enabled;
        if (enabled) {
            RenderTarget t = ensureTarget(FULL_FRAME_ID, RenderTargetKind.FULL_FRAME);
            t.setEnabled(true);
            t.setZ(1000f);
        } else {
            RenderTarget t = targets.get(FULL_FRAME_ID);
            if (t != null) {
                t.setEnabled(false);
            }
            clearEffects(FULL_FRAME_ID);
        }
    }

    public boolean isFullFrameEnabled() {
        return fullFrameEnabled;
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
        if (!fullFrameEnabled) {
            return targets.get(FULL_FRAME_ID);
        }
        RenderTarget t = ensureTarget(FULL_FRAME_ID, RenderTargetKind.FULL_FRAME);
        t.setBounds(0f, 0f, width, height);
        t.setZ(1000f);
        t.setEnabled(true);
        return t;
    }

    public float screenWidth() {
        return screenW;
    }

    public float screenHeight() {
        return screenH;
    }

    public boolean needsCapture() {
        if (captureEnabled) {
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
        if (!fullFrameEnabled) {
            setFullFrameEnabled(true);
        }
        ensureTarget(FULL_FRAME_ID, RenderTargetKind.FULL_FRAME);
        return bindEffect(FULL_FRAME_ID, effectId, params);
    }

    public RenderTarget ensureTarget(String id, RenderTargetKind kind) {
        RenderTarget t = targets.get(id);
        if (t == null) {
            t = new RenderTarget(id, kind);
            targets.put(id, t);
            bindings.put(id, new ArrayList<EffectBinding>());
        } else if (t.kind != kind) {
            throw new IllegalArgumentException(
                    "target kind mismatch for " + id + ": " + t.kind + " vs " + kind);
        }
        return t;
    }

    public RenderTarget getTarget(String id) {
        return targets.get(id);
    }

    public void removeTarget(String id) {
        if (id == null) {
            return;
        }
        targets.remove(id);
        bindings.remove(id);
    }

    public void removeTargetsWithPrefix(String prefix) {
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

    public void clearTargets() {
        targets.clear();
        bindings.clear();
    }

    /**
     * Bind effect to target. Unknown effect id → IllegalArgumentException.
     */
    public EffectBinding bindEffect(String targetId, String effectId, Map<String, Object> params) {
        RenderTarget target = targets.get(targetId);
        if (target == null) {
            throw new IllegalArgumentException("unknown target: " + targetId);
        }
        if (target.kind == RenderTargetKind.FULL_FRAME && !fullFrameEnabled) {
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

    public void clearEffects(String targetId) {
        List<EffectBinding> list = bindings.get(targetId);
        if (list != null) {
            list.clear();
        }
    }

    /**
     * Update a live effect param (e.g. lightwave intensity from a C1 slider). No-op if missing.
     */
    public boolean setEffectParam(String targetId, String effectId, String key, float value) {
        if (targetId == null || effectId == null || key == null) {
            return false;
        }
        List<EffectBinding> list = bindings.get(targetId);
        if (list == null) {
            return false;
        }
        boolean any = false;
        for (EffectBinding b : list) {
            if (effectId.equals(b.effectId)) {
                b.setParamFloat(key, value);
                any = true;
            }
        }
        return any;
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

    /**
     * Sync C1 widget session: window + nodes with effects / interactive ids.
     */
    public void syncWidgetSession(WidgetSession session) {
        if (session == null) {
            return;
        }
        String win = session.windowId();
        removeTargetsWithPrefix("c1:" + win + ":");
        removeTarget("c1:" + win);

        LayoutResult layout = LayoutEngine.layout(session.root());
        String winTargetId = "c1:" + win;
        RenderTarget winTarget = ensureTarget(winTargetId, RenderTargetKind.SYNTHETIC_WINDOW);
        winTarget.setBounds(layout.rootBounds);
        winTarget.setZ(0f);
        clearEffects(winTargetId);
        applyNodeEffects(winTargetId, session.root());

        syncNodeTargets(session.root(), win, layout, 1f);
    }

    private void syncNodeTargets(UiNode node, String windowId, LayoutResult layout, float zBase) {
        boolean shaderNode = ArtNodeTypes.SHADER_EFFECT.equals(node.type);
        boolean track = !node.id.isEmpty()
                && (UiTypes.isLeaf(node.type)
                        || !node.effects.isEmpty()
                        || shaderNode
                        || UiTypes.GLASS.equals(node.type)
                        || UiTypes.PANEL.equals(node.type));
        if (track) {
            String tid = "c1:" + windowId + ":" + node.id;
            RenderTarget t = ensureTarget(tid, RenderTargetKind.SYNTHETIC_WIDGET);
            Rect b = layout.boundsOf(node.id);
            if (b != null) {
                t.setBounds(b);
            }
            t.setZ(zBase);
            clearEffects(tid);
            applyNodeEffects(tid, node);
            if (shaderNode) {
                applyShaderEffectNode(tid, node);
            }
        }
        for (UiNode c : node.children) {
            syncNodeTargets(c, windowId, layout, zBase + 0.01f);
        }
    }

    private void applyNodeEffects(String targetId, UiNode node) {
        for (EffectDecl decl : node.effects) {
            if (!effects.contains(decl.id)) {
                continue;
            }
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            if (decl.params != null) {
                params.putAll(decl.params);
            }
            if (!params.containsKey("screenW")) {
                params.put("screenW", Float.valueOf(screenW));
            }
            if (!params.containsKey("screenH")) {
                params.put("screenH", Float.valueOf(screenH));
            }
            bindEffect(targetId, decl.id, params);
        }
    }

    /**
     * {@code art.shader_effect} uses prop {@code effect} (+ other props as params).
     */
    private void applyShaderEffectNode(String targetId, UiNode node) {
        String effectId = node.propString("effect", "");
        if (effectId.isEmpty()) {
            return;
        }
        if (!effects.contains(effectId)) {
            return;
        }
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> e : node.props.entrySet()) {
            if ("effect".equals(e.getKey())) {
                continue;
            }
            params.put(e.getKey(), e.getValue());
        }
        if (!params.containsKey("screenW")) {
            params.put("screenW", Float.valueOf(screenW));
        }
        if (!params.containsKey("screenH")) {
            params.put("screenH", Float.valueOf(screenH));
        }
        bindEffect(targetId, effectId, params);
    }

    public void detachWidgetSession(String windowId) {
        if (windowId == null) {
            return;
        }
        removeTargetsWithPrefix("c1:" + windowId + ":");
        removeTarget("c1:" + windowId);
    }

    /**
     * Sync C2 entity slot as overlay target (kind-aware default bounds, milestone 24).
     */
    public void syncEntitySlot(EntitySlot slot) {
        if (slot == null) {
            return;
        }
        String tid = "c2:entity:" + slot.slotId;
        RenderTarget t = ensureTarget(tid, RenderTargetKind.ENTITY_SLOT);
        float scale = slot.scale() > 0f ? slot.scale() : 1f;
        float[] size = artframework.c2.EntityDrawPath.defaultSize(slot.kind, scale);
        float w = size[0];
        float h = size[1];
        t.setBounds(slot.x() - w * 0.5f, slot.y() - h * 0.5f, w, h);
        t.setZ(10f);
        t.setEnabled(slot.isLaidOut());
    }

    public void detachEntitySlot(String slotId) {
        if (slotId != null) {
            removeTarget("c2:entity:" + slotId);
        }
    }

    public void tick(float deltaSeconds) {
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
        RenderContext ctx = new RenderContext(
                spriteBatch,
                timeSeconds,
                needsCapture() || frameCapture.hasTexture() ? frameCapture : null);
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
            if (target.kind == RenderTargetKind.FULL_FRAME && !fullFrameEnabled) {
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
        for (RenderTarget target : targets.values()) {
            if (!target.isEnabled()) {
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

    public Map<String, Object> probeMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("targetCount", Integer.valueOf(targetCount()));
        out.put("bindingCount", Integer.valueOf(bindingCount()));
        out.put("fullFrameEnabled", Boolean.valueOf(fullFrameEnabled));
        out.put("captureEnabled", Boolean.valueOf(captureEnabled));
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

    /** Dot-path safe key: {@code c1:win:node} → {@code c1_win_node}. */
    static String safeTargetKey(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        return id.replace(':', '_');
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
        clearTargets();
        shaderRuntime.disposeAll();
        frameCapture.dispose();
        effects.clear();
        shaders.clear();
        hostBackend = DirectHostRenderBackend.INSTANCE;
        fullFrameEnabled = false;
        captureEnabled = false;
        shadersReady = false;
        timeSeconds = 0f;
        screenW = 1920f;
        screenH = 1080f;
        installBuiltins();
    }
}
