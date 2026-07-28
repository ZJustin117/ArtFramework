package artframework.api;

import artframework.c1.C1NodeFactories;
import artframework.c1.SyntheticRuntime;
import artframework.c1.layout.LayoutNode;
import artframework.c2.DefaultEntityPresent;
import artframework.c2.EntityPresent;
import artframework.c2.NativeTemplateRuntime;
import artframework.component.UiNode;
import artframework.component.UiNodeRegistry;
import artframework.component.WidgetSession;
import artframework.component.WidgetSessions;
import artframework.c2.NativeComponents;
import artframework.core.AnimationPlayer;
import artframework.core.AnimationPlayers;
import artframework.core.HostBackend;
import artframework.core.HostBackends;
import artframework.core.Theme;
import artframework.core.Themes;
import artframework.core.UiComponent;
import artframework.core.UiTree;
import artframework.core.UiTrees;
import artframework.core.UiSignalInterceptor;
import artframework.assets.HostAssets;
import artframework.assets.HostAssetsHolder;
import artframework.context.ContextFrame;
import artframework.context.FrameDiff;
import artframework.context.FrameRuntime;
import artframework.context.FrameRuntimes;
import artframework.context.IntentResult;
import artframework.context.PresentBackends;
import artframework.context.PresentProjection;
import artframework.context.PresentSurfaces;
import artframework.context.PresentationBackend;
import artframework.context.UiIntent;
import artframework.ops.GateLab;
import artframework.ops.NativeOpsBackend;
import artframework.ops.NoOpNativeOps;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade for dual-track windows. Prefer {@link #mount} / {@link #unmount}; {@link #open} /
 * {@link #bind} remain compatibility aliases. C1 SYNTHETIC → SyntheticRuntime; C2 NATIVE_TEMPLATE
 * → NativeTemplateRuntime + {@link UiComponent}. UiOps/UiProbe for commands/query.
 */
public final class ArtFramework {

    private static final Map<String, WindowDef> DEFS = new LinkedHashMap<String, WindowDef>();
    private static final Map<String, WindowHandle> OPEN = new LinkedHashMap<String, WindowHandle>();
    private static final UiOps OPS = new UiOps();
    private static final UiProbe PROBE = new UiProbe();
    private static NativeOpsBackend nativeOpsBackend = NoOpNativeOps.INSTANCE;

    private ArtFramework() {}

    public static void register(WindowDef def) {
        if (def == null) {
            throw new IllegalArgumentException("def required");
        }
        DEFS.put(def.id, def);
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            String canon = artframework.c2.NativeTemplateIds.canonicalize(def.id);
            if (canon != null && !canon.equals(def.id)) {
                DEFS.put(canon, def);
            }
            String resCanon = artframework.c2.NativeTemplateIds.canonicalize(def.resource);
            if (resCanon != null && !resCanon.isEmpty() && !resCanon.equals(def.id) && !resCanon.equals(canon)) {
                DEFS.put(resCanon, def);
            }
        }
    }

    public static boolean isRegistered(String id) {
        if (DEFS.containsKey(id)) {
            return true;
        }
        String canon = artframework.c2.NativeTemplateIds.canonicalize(id);
        return canon != null && DEFS.containsKey(canon);
    }

    /**
     * Mount a registered window (synthetic open or native bind). Preferred Godot-aligned entry.
     */
    public static WindowHandle mount(String id) {
        WindowDef def = defOf(id);
        if (def == null) {
            throw new IllegalArgumentException("not registered: " + id);
        }
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            return bind(id);
        }
        return openSynthetic(def);
    }

    /** Unmount / close an open window or native template. */
    public static void unmount(String id) {
        close(id);
    }

    public static void addSignalInterceptor(String windowId, UiSignalInterceptor interceptor) {
        UiTree tree = UiTrees.get(windowId);
        if (tree == null) {
            throw new IllegalArgumentException("synthetic window not open: " + windowId);
        }
        tree.addSignalInterceptor(interceptor);
    }

    public static void removeSignalInterceptor(String windowId, UiSignalInterceptor interceptor) {
        UiTree tree = UiTrees.get(windowId);
        if (tree != null) {
            tree.removeSignalInterceptor(interceptor);
        }
    }

    public static WindowHandle open(String id) {
        WindowDef def = defOf(id);
        if (def == null) {
            throw new IllegalArgumentException("not registered: " + id);
        }
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            return bind(id);
        }
        return openSynthetic(def);
    }

    private static WindowHandle openSynthetic(WindowDef def) {
        WindowHandle previous = OPEN.get(def.id);
        if (previous != null && previous.isOpen()) {
            previous.close();
        }
        LayoutNode root = SyntheticRuntime.open(def);
        WindowHandle handle = new TrackedHandle(def, root);
        OPEN.put(def.id, handle);
        UiTree tree = UiTrees.get(def.id);
        if (tree != null) {
            OPS.onTreeMounted(def.id);
            HostBackends.get().attach(tree);
        }
        return handle;
    }

    /**
     * Attach to an engine-owned native screen (C2). For SYNTHETIC, delegates to {@link #open}.
     */
    private static WindowDef defOf(String id) {
        if (id == null) {
            return null;
        }
        WindowDef def = DEFS.get(id);
        if (def != null) {
            return def;
        }
        return DEFS.get(artframework.c2.NativeTemplateIds.canonicalize(id));
    }

    public static WindowHandle bind(String id) {
        WindowDef def = defOf(id);
        if (def == null) {
            throw new IllegalArgumentException("not registered: " + id);
        }
        if (def.windowClass == WindowClass.SYNTHETIC) {
            return open(id);
        }
        String openId = artframework.c2.NativeTemplateIds.canonicalize(def.resource);
        if (openId == null || openId.isEmpty()) {
            openId = artframework.c2.NativeTemplateIds.canonicalize(def.id);
        }
        if (openId == null || openId.isEmpty()) {
            openId = def.id;
        }
        WindowHandle previous = OPEN.get(openId);
        if (previous != null && previous.isOpen()) {
            previous.close();
        }
        previous = OPEN.get(id);
        if (previous != null && previous.isOpen()) {
            previous.close();
        }
        NativeTemplateRuntime.bind(def);
        WindowHandle handle = new TrackedHandle(def, null);
        OPEN.put(openId, handle);
        if (!openId.equals(id)) {
            OPEN.put(id, handle);
        }
        return handle;
    }

    public static WindowHandle find(String id) {
        WindowHandle h = OPEN.get(id);
        if (h != null) {
            return h;
        }
        return OPEN.get(artframework.c2.NativeTemplateIds.canonicalize(id));
    }

    public static List<String> listOpenIds() {
        List<String> ids = new ArrayList<String>();
        List<WindowHandle> handles = new ArrayList<WindowHandle>();
        for (WindowHandle handle : OPEN.values()) {
            if (handles.contains(handle)) {
                continue;
            }
            handles.add(handle);
            if (handle instanceof TrackedHandle) {
                ids.add(((TrackedHandle) handle).openId);
            } else {
                ids.add(handle.id());
            }
        }
        return Collections.unmodifiableList(ids);
    }

    public static void close(String id) {
        WindowHandle h = OPEN.get(id);
        if (h != null) {
            h.close();
        }
    }

    public static void resetForTests() {
        GateLab.resetForTests();
        OPEN.clear();
        DEFS.clear();
        OPS.resetForTests();
        nativeOpsBackend = NoOpNativeOps.INSTANCE;
        SyntheticRuntime.resetForTests();
        NativeTemplateRuntime.resetForTests();
        RenderHosts.resetForTests();
        Themes.resetForTests();
        HostBackends.resetForTests();
        UiNodeRegistry.global().resetBuiltinsForTests();
        C1NodeFactories.global().resetBuiltinsForTests();
        AnimationPlayers.resetForTests();
        artframework.skeleton.SkeletonProviders.global().resetForTests();
        PresentSurfaces.resetForTests();
        FrameRuntimes.resetForTests();
        HostAssetsHolder.resetForTests();
        artframework.sts1.FullPresentMode.resetForTests();
        artframework.inspect.UiLabListeners.resetForTests();
    }

    public static HostBackend host() {
        return HostBackends.get();
    }

    public static void setHostBackend(HostBackend backend) {
        HostBackends.set(backend);
    }

    /** Process default theme for new synthetic trees. */
    public static Theme theme() {
        return Themes.getDefault();
    }

    public static void setTheme(Theme theme) {
        Themes.setDefault(theme);
    }

    /** Imperative UI commands (C1 + C2). */
    public static UiOps ops() {
        return OPS;
    }

    /** Read-only UI snapshot. */
    public static UiProbe probe() {
        return PROBE;
    }

    public static void setNativeOpsBackend(NativeOpsBackend backend) {
        nativeOpsBackend = backend != null ? backend : NoOpNativeOps.INSTANCE;
    }

    public static NativeOpsBackend nativeOpsBackend() {
        return nativeOpsBackend;
    }

    /**
     * Layout root for an open synthetic window, or null if not synthetic / not tracked.
     */
    public static LayoutNode layoutRoot(String id) {
        WindowHandle h = OPEN.get(id);
        if (h instanceof TrackedHandle) {
            return ((TrackedHandle) h).root;
        }
        return null;
    }

    /**
     * Composition tree for an open synthetic window (expanded), or null.
     */
    public static UiNode uiRoot(String id) {
        WidgetSession s = WidgetSessions.get(id);
        return s != null ? s.root() : null;
    }

    /** Widget session (sliders / control index) for an open synthetic window. */
    public static WidgetSession widgets(String id) {
        return WidgetSessions.get(id);
    }

    /** Mounted instance tree for an open synthetic window, or null. */
    public static UiTree tree(String id) {
        return UiTrees.get(id);
    }

    /** Advance all mounted synthetic trees and the configured host. */
    public static void tick(float deltaSeconds) {
        if (deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be non-negative");
        }
        for (UiTree tree : UiTrees.listOpen()) {
            tree.tick(deltaSeconds);
        }
        HostBackends.get().tick(deltaSeconds);
    }

    /**
     * C2 / full-present / synthetic component by id.
     * <ul>
     *   <li>Mounted full-present wins over native (same id, e.g. map).
     *   <li>Mounted native wins when present is not mounted (legacy bind path).
     *   <li>If neither mounted, prefer full-present so {@code component(id).mount()} targets
     *       PresentSurfaces; legacy code should {@code bind()} which mounts native via runtime.
     * </ul>
     */
    public static UiComponent component(String componentId) {
        UiComponent synthetic = artframework.c1.SyntheticComponents.get(componentId);
        if (synthetic != null) {
            return synthetic;
        }
        UiComponent present = PresentSurfaces.get(componentId);
        UiComponent nativeC = NativeComponents.get(componentId);
        if (present != null && present.isMounted()) {
            return present;
        }
        if (nativeC != null && nativeC.isMounted()) {
            return nativeC;
        }
        if (present != null) {
            return present;
        }
        return nativeC;
    }

    /** Primary presentation backend (context frames + intents). */
    public static PresentationBackend presentationBackend() {
        return PresentBackends.get();
    }

    /**
     * Bind Primary Backend; resets frame projection. Re-scopes assets view for tests via
     * caller if needed.
     */
    public static void bindPresentationBackend(PresentationBackend backend) {
        FrameRuntimes.get().projection().reset();
        PresentBackends.bind(backend);
    }

    public static FrameRuntime frames() {
        return FrameRuntimes.get();
    }

    public static PresentProjection projection() {
        return FrameRuntimes.get().projection();
    }

    /** Apply an authority frame (or pull via {@link FrameRuntime#syncFromBackend()}). */
    public static FrameDiff applyFrame(ContextFrame frame) {
        return FrameRuntimes.get().applyFrame(frame);
    }

    public static IntentResult submitIntent(UiIntent intent) {
        return FrameRuntimes.get().submitIntent(intent);
    }

    /** Host-managed unified asset library. */
    public static HostAssets assets() {
        return HostAssetsHolder.get();
    }

    /** Track-agnostic render attach (effects / targets). */
    public static RenderHost render() {
        return RenderHosts.get();
    }

    /** Registered presentation node types (builtins + third-party). */
    public static UiNodeRegistry nodes() {
        return UiNodeRegistry.global();
    }

    /** C1 scene2d node factories (builtins + third-party). */
    public static C1NodeFactories c1Nodes() {
        return C1NodeFactories.global();
    }

    /** Animation player for a mounted {@code art.animation_player} node, or null. */
    public static AnimationPlayer animation(String windowId, String nodeId) {
        return AnimationPlayers.get(windowId, nodeId);
    }

    /** Skeleton provider registry. */
    public static artframework.skeleton.SkeletonProviders skeletons() {
        return artframework.skeleton.SkeletonProviders.global();
    }

    /** C2 entity presenter slots (attach/sync/layout/detach). */
    public static EntityPresent entities() {
        return NativeTemplateRuntime.entities();
    }

    /** Same as {@link #entities()} with listener registration helpers. */
    public static DefaultEntityPresent entityPresent() {
        return NativeTemplateRuntime.entities();
    }

    private static final class TrackedHandle implements WindowHandle {
        private final WindowDef def;
        private final LayoutNode root;
        private final String openId;
        private boolean open = true;

        TrackedHandle(WindowDef def, LayoutNode root) {
            this.def = def;
            this.root = root;
            String id = def.id;
            if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
                String nativeId = artframework.c2.NativeTemplateIds.canonicalize(def.resource);
                if (nativeId != null && !nativeId.isEmpty()) {
                    id = nativeId;
                }
            }
            this.openId = id;
        }

        @Override
        public String id() {
            return def.id;
        }

        @Override
        public WindowClass windowClass() {
            return def.windowClass;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            if (!open) {
                return;
            }
            open = false;
            removeHandleAliases(this);
            if (def.windowClass == WindowClass.SYNTHETIC) {
                UiTree tree = UiTrees.get(def.id);
                if (tree != null) {
                    HostBackends.get().detach(tree);
                }
                SyntheticRuntime.onClosed(def.id);
            } else if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
                NativeTemplateRuntime.unbind(def);
            }
        }

        private void removeHandleAliases(WindowHandle handle) {
            List<String> aliases = new ArrayList<String>();
            for (Map.Entry<String, WindowHandle> entry : OPEN.entrySet()) {
                if (entry.getValue() == handle) {
                    aliases.add(entry.getKey());
                }
            }
            for (String alias : aliases) {
                OPEN.remove(alias);
            }
        }
    }
}
