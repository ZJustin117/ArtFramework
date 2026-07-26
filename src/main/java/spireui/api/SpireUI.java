package spireui.api;

import spireui.c1.SyntheticRuntime;
import spireui.c1.layout.LayoutNode;
import spireui.c2.DefaultEntityPresent;
import spireui.c2.EntityPresent;
import spireui.c2.NativeTemplateRuntime;
import spireui.ops.NativeOpsBackend;
import spireui.ops.NoOpNativeOps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade for dual-track windows. C1 SYNTHETIC → SyntheticRuntime; C2 NATIVE_TEMPLATE →
 * NativeTemplateRuntime (map/event/select/end-turn bind + hooks). UiOps/UiProbe for commands/query.
 */
public final class SpireUI {

    private static final Map<String, WindowDef> DEFS = new LinkedHashMap<String, WindowDef>();
    private static final Map<String, WindowHandle> OPEN = new LinkedHashMap<String, WindowHandle>();
    private static final UiOps OPS = new UiOps();
    private static final UiProbe PROBE = new UiProbe();
    private static NativeOpsBackend nativeOpsBackend = NoOpNativeOps.INSTANCE;

    private SpireUI() {}

    public static void register(WindowDef def) {
        if (def == null) {
            throw new IllegalArgumentException("def required");
        }
        DEFS.put(def.id, def);
    }

    public static boolean isRegistered(String id) {
        return DEFS.containsKey(id);
    }

    public static WindowHandle open(String id) {
        WindowDef def = DEFS.get(id);
        if (def == null) {
            throw new IllegalArgumentException("not registered: " + id);
        }
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            return bind(id);
        }
        WindowHandle previous = OPEN.get(id);
        if (previous != null && previous.isOpen()) {
            previous.close();
        }
        LayoutNode root = SyntheticRuntime.open(def);
        WindowHandle handle = new TrackedHandle(def, root);
        OPEN.put(id, handle);
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
        OPEN.clear();
        DEFS.clear();
        OPS.resetForTests();
        nativeOpsBackend = NoOpNativeOps.INSTANCE;
        SyntheticRuntime.resetForTests();
        NativeTemplateRuntime.resetForTests();
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
                SyntheticRuntime.onClosed(def.id);
            } else if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
                NativeTemplateRuntime.unbind(def);
            }
        }
    }
}
