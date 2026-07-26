package artframework.api;

import artframework.c1.SyntheticRuntime;
import artframework.c1.layout.LayoutNode;
import artframework.c2.DefaultEntityPresent;
import artframework.c2.EntityPresent;
import artframework.c2.NativeTemplateRuntime;
import artframework.component.UiNode;
import artframework.component.WidgetSession;
import artframework.component.WidgetSessions;
import artframework.c2.NativeComponents;
import artframework.core.HostBackend;
import artframework.core.HostBackends;
import artframework.core.Theme;
import artframework.core.Themes;
import artframework.core.UiComponent;
import artframework.core.UiTree;
import artframework.core.UiTrees;
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
    }

    public static boolean isRegistered(String id) {
        return DEFS.containsKey(id);
    }

    /**
     * Mount a registered window (synthetic open or native bind). Preferred Godot-aligned entry.
     */
    public static WindowHandle mount(String id) {
        WindowDef def = DEFS.get(id);
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

    public static WindowHandle open(String id) {
        WindowDef def = DEFS.get(id);
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
            HostBackends.get().attach(tree);
        }
        return handle;
    }

    /**
     * Attach to an engine-owned native screen (C2). For SYNTHETIC, delegates to {@link #open}.
     */
    public static WindowHandle bind(String id) {
        WindowDef def = DEFS.get(id);
        if (def == null) {
            throw new IllegalArgumentException("not registered: " + id);
        }
        if (def.windowClass == WindowClass.SYNTHETIC) {
            return open(id);
        }
        WindowHandle previous = OPEN.get(id);
        if (previous != null && previous.isOpen()) {
            previous.close();
        }
        NativeTemplateRuntime.bind(def);
        WindowHandle handle = new TrackedHandle(def, null);
        OPEN.put(id, handle);
        return handle;
    }

    public static WindowHandle find(String id) {
        return OPEN.get(id);
    }

    public static List<String> listOpenIds() {
        return Collections.unmodifiableList(new ArrayList<String>(OPEN.keySet()));
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

    /** C2 (or future) component by canonical id, e.g. {@code sts.map}. */
    public static UiComponent component(String componentId) {
        return NativeComponents.get(componentId);
    }

    /** Track-agnostic render attach (effects / targets). */
    public static RenderHost render() {
        return RenderHosts.get();
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
        private boolean open = true;

        TrackedHandle(WindowDef def, LayoutNode root) {
            this.def = def;
            this.root = root;
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
            OPEN.remove(def.id);
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
    }
}
