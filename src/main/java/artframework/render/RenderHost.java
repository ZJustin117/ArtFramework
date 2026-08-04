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

    public static final String C2_SURFACE_PREFIX = "c2:surface:";

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

    /** Create or update the render region used by a C2 surface effect. */
    public RenderTarget syncC2Surface(
            String surfaceId, float x, float y, float width, float height) {
        RenderTarget target =
                ensureTarget(c2SurfaceTargetId(surfaceId), RenderTargetKind.C2_SURFACE);
        target.setBounds(x, y, width, height);
        target.setEnabled(width > 0f && height > 0f);
        target.setZ(-10f);
        return target;
    }

    public void setC2SurfaceEnabled(String surfaceId, boolean enabled) {
        RenderTarget target = getTarget(c2SurfaceTargetId(surfaceId));
        if (target != null) {
            target.setEnabled(enabled);
        }
    }

    /** Sync an exact C2 item region and copy the owning surface's ambient effects once. */
    public RenderTarget syncC2Item(
            String surfaceId, String itemId, float x, float y, float width, float height) {
        String targetId = c2ItemTargetId(surfaceId, itemId);
        RenderTarget target = ensureTarget(targetId, RenderTargetKind.C2_SURFACE);
        target.setBounds(x, y, width, height);
        target.setEnabled(width > 0f && height > 0f);
        target.setZ(-5f);
        if (effectsOf(targetId).isEmpty()) {
            for (EffectBinding binding : effectsOf(c2SurfaceTargetId(surfaceId))) {
                bindEffect(targetId, binding.effectId, binding.paramsView());
            }
        }
        return target;
    }

    public void removeC2Items(String surfaceId) {
        if (surfaceId != null && !surfaceId.trim().isEmpty()) {
            removeTargetsWithPrefix(c2SurfaceTargetId(surfaceId) + ":item:");
        }
    }

    /** Keep the current item targets and remove only items absent from the current projection. */
    public void retainC2Items(String surfaceId, java.util.Set<String> itemIds) {
        if (surfaceId == null || surfaceId.trim().isEmpty()) {
            return;
        }
        String prefix = c2SurfaceTargetId(surfaceId) + ":item:";
        List<String> ids = new ArrayList<String>(targets.keySet());
        for (String id : ids) {
            if (!id.startsWith(prefix)) {
                continue;
            }
            String itemId = id.substring(prefix.length());
            if (itemIds == null || !itemIds.contains(itemId)) {
                removeTarget(id);
            }
        }
    }

    public void removeC2Surface(String surfaceId) {
        if (surfaceId != null && !surfaceId.trim().isEmpty()) {
            removeTarget(c2SurfaceTargetId(surfaceId));
            removeC2Items(surfaceId);
        }
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
     * Update a live effect param. Prefer ambient layer when multiple lightwave bindings exist
     * (pulse overlay is addressed via layer overload).
     */
    public boolean setEffectParam(String targetId, String effectId, String key, float value) {
        if (targetId == null || effectId == null || key == null) {
            return false;
        }
        List<EffectBinding> list = bindings.get(targetId);
        if (list == null) {
            return false;
        }
        // Prefer ambient when present so slider/anim do not stomp pulse overlay.
        EffectBinding ambient = findEffect(targetId, effectId, EffectBinding.LAYER_AMBIENT);
        if (ambient != null) {
            ambient.setParamFloat(key, value);
            return true;
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

    /**
     * Update param only on bindings whose {@link EffectBinding#layer()} equals {@code layer}.
     * Null/empty layer falls back to {@link #setEffectParam(String, String, String, float)}.
     */
    public boolean setEffectParam(
            String targetId, String effectId, String layer, String key, float value) {
        if (layer == null || layer.trim().isEmpty()) {
            return setEffectParam(targetId, effectId, key, value);
        }
        if (targetId == null || effectId == null || key == null) {
            return false;
        }
        List<EffectBinding> list = bindings.get(targetId);
        if (list == null) {
            return false;
        }
        boolean any = false;
        String want = layer.trim();
        for (EffectBinding b : list) {
            if (!effectId.equals(b.effectId)) {
                continue;
            }
            if (!want.equals(b.layer())) {
                continue;
            }
            b.setParamFloat(key, value);
            any = true;
        }
        return any;
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

    /**
     * Ensure a pulse-layer lightwave binding exists (overlay band). Returns the binding or null.
     */
    public EffectBinding ensurePulseLightwave(String targetId) {
        if (targetId == null) {
            return null;
        }
        EffectBinding existing =
                findEffect(targetId, LightwaveEffect.ID, EffectBinding.LAYER_PULSE);
        if (existing != null) {
            return existing;
        }
        if (targets.get(targetId) == null) {
            return null;
        }
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("layer", EffectBinding.LAYER_PULSE);
        params.put("intensity", Float.valueOf(0f));
        params.put("phase", Float.valueOf(0f));
        params.put("freeze", Float.valueOf(1f));
        params.put("width", Float.valueOf(0.22f));
        params.put("angle", Float.valueOf(35f));
        params.put("border", Float.valueOf(0f));
        params.put("speed", Float.valueOf(0f));
        try {
            EffectBinding b = bindEffect(targetId, LightwaveEffect.ID, params);
            b.setEnabled(false);
            return b;
        } catch (RuntimeException e) {
            return null;
        }
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
        // Title chrome (not in UiNode tree) — pack label defaults via synthetic type "label".
        ensureTitleEffectTarget(win);

        syncNodeTargets(session.root(), win, layout, 1f, "", 0);
    }

    private void ensureTitleEffectTarget(String windowId) {
        String tid = "c1:" + windowId + ":__art_title";
        RenderTarget t = ensureTarget(tid, RenderTargetKind.SYNTHETIC_WIDGET);
        t.setZ(0.5f);
        clearEffects(tid);
        // Reuse label effectDefaults (pack table) for title bar chrome.
        try {
            java.util.List<artframework.component.EffectDecl> decls =
                    artframework.core.PresentPackApply.effectDefaultsForType(UiTypes.LABEL);
            if (decls != null) {
                for (artframework.component.EffectDecl decl : decls) {
                    if (decl == null || !effects.contains(decl.id)) {
                        continue;
                    }
                    java.util.Map<String, Object> params =
                            new LinkedHashMap<String, Object>();
                    if (decl.params != null) {
                        params.putAll(decl.params);
                    }
                    // Slightly stronger title frame
                    if (!params.containsKey("borderWidth")) {
                        params.put("borderWidth", Float.valueOf(2f));
                    }
                    bindEffect(tid, decl.id, params);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void syncNodeTargets(
            UiNode node, String windowId, LayoutResult layout, float zBase, String parentKey, int index) {
        if (node == null) {
            return;
        }
        boolean shaderNode = ArtNodeTypes.SHADER_EFFECT.equals(node.type);
        String key = LayoutEngine.effectKey(node, parentKey, index);
        boolean hasDefaults = false;
        try {
            hasDefaults =
                    !artframework.core.PresentPackApply.effectDefaultsForType(node.type).isEmpty();
        } catch (Throwable ignored) {
        }
        boolean track =
                UiTypes.isLeaf(node.type)
                        || !node.effects.isEmpty()
                        || hasDefaults
                        || shaderNode
                        || UiTypes.GLASS.equals(node.type)
                        || UiTypes.PANEL.equals(node.type)
                        || UiTypes.BUTTON.equals(node.type)
                        || UiTypes.SLIDER.equals(node.type)
                        || UiTypes.LABEL.equals(node.type)
                        || UiTypes.ROW.equals(node.type)
                        || UiTypes.COL.equals(node.type)
                        || UiTypes.STACK.equals(node.type)
                        || UiTypes.SCROLL.equals(node.type)
                        || UiTypes.GRID.equals(node.type)
                        || UiTypes.TABS.equals(node.type)
                        || UiTypes.CENTER.equals(node.type)
                        || UiTypes.MARGIN.equals(node.type)
                        || UiTypes.TEXTFIELD.equals(node.type)
                        || UiTypes.CHECKBOX.equals(node.type)
                        || UiTypes.PROGRESS.equals(node.type)
                        || UiTypes.HITAREA.equals(node.type);
        // Skip pure structural fragment without defaults/effects
        if (UiTypes.FRAGMENT.equals(node.type) && node.effects.isEmpty() && !hasDefaults) {
            track = false;
        }
        if (track && key != null && !key.isEmpty()) {
            String tid = "c1:" + windowId + ":" + key;
            RenderTarget t = ensureTarget(tid, RenderTargetKind.SYNTHETIC_WIDGET);
            Rect b = layout.boundsOf(key);
            if (b == null && !node.id.isEmpty()) {
                b = layout.boundsOf(node.id);
            }
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
        List<UiNode> kids = node.children;
        for (int i = 0; i < kids.size(); i++) {
            syncNodeTargets(kids.get(i), windowId, layout, zBase + 0.01f, key, i);
        }
    }

    private void applyNodeEffects(String targetId, UiNode node) {
        List<EffectDecl> decls = node.effects;
        if (decls == null || decls.isEmpty()) {
            try {
                decls = artframework.core.PresentPackApply.effectDefaultsForType(node.type);
            } catch (Throwable t) {
                decls = java.util.Collections.emptyList();
            }
        }
        if (decls == null) {
            return;
        }
        for (EffectDecl decl : decls) {
            if (decl == null || !effects.contains(decl.id)) {
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
