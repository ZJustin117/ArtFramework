package artframework.api;

import artframework.c1.SyntheticRuntime;
import artframework.c1.layout.LayoutNode;
import artframework.c2.NativeTemplateRuntime;
import artframework.component.NativeTemplateIds;
import artframework.core.HostBackends;
import artframework.core.UiComponent;
import artframework.inspect.UiLabListeners;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationMount;
import artframework.presentation.PresentationRuntime;
import artframework.ecs.EntityId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns facade lifecycle compatibility state without becoming presentation-state authority. */
final class FrameworkLifecycleCoordinator {
    private final Map<String, WindowDef> definitions = new LinkedHashMap<String, WindowDef>();
    private final Map<String, WindowHandle> handles = new LinkedHashMap<String, WindowHandle>();
    private final UiOps ops;

    FrameworkLifecycleCoordinator(UiOps ops) {
        this.ops = ops;
        installRetirementHook();
    }

    void register(WindowDef def) {
        closeDefinitionHandles(def.id);
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            closeDefinitionHandles(NativeTemplateIds.canonicalize(def.id));
            closeDefinitionHandles(NativeTemplateIds.canonicalize(def.resource));
        }
        removeDefinitionAliases(def.id);
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            String canon = NativeTemplateIds.canonicalize(def.id);
            removeDefinitionAliasesForKey(canon);
            String resourceCanon = NativeTemplateIds.canonicalize(def.resource);
            removeDefinitionAliasesForKey(resourceCanon);
            definitions.put(def.id, def);
            if (canon != null && !canon.equals(def.id)) definitions.put(canon, def);
            if (resourceCanon != null && !resourceCanon.isEmpty()
                    && !resourceCanon.equals(def.id) && !resourceCanon.equals(canon)) {
                definitions.put(resourceCanon, def);
            }
        } else {
            definitions.put(def.id, def);
        }
    }

    boolean isRegistered(String id) {
        return definition(id) != null;
    }

    WindowDef registeredWindow(String id) {
        return definition(id);
    }

    void unregister(String id) {
        if (id == null || id.isEmpty()) return;
        close(id);
        WindowDef def = definition(id);
        if (def != null) removeDefinitionAliases(def.id);
    }

    WindowHandle mount(String id) {
        WindowDef def = requireDefinition(id);
        return def.windowClass == WindowClass.NATIVE_TEMPLATE ? bind(id) : openSynthetic(def);
    }

    WindowHandle open(String id) {
        return mount(id);
    }

    WindowHandle bind(String id) {
        WindowDef def = requireDefinition(id);
        if (def.windowClass == WindowClass.SYNTHETIC) return open(id);
        String openId = NativeTemplateIds.canonicalize(def.resource);
        if (openId == null || openId.isEmpty()) openId = NativeTemplateIds.canonicalize(def.id);
        if (openId == null || openId.isEmpty()) openId = def.id;
        closeCurrent(openId);
        closeCurrent(id);
        NativeTemplateRuntime.bind(def);
        WindowHandle handle = new TrackedHandle(def, null);
        handles.put(openId, handle);
        if (!openId.equals(id)) handles.put(id, handle);
        return handle;
    }

    WindowHandle find(String id) {
        WindowHandle handle = handles.get(id);
        if (handle != null && handle.isOpen()) return handle;
        handle = handles.get(NativeTemplateIds.canonicalize(id));
        return handle != null && handle.isOpen() ? handle : null;
    }

    List<String> listOpenIds() {
        List<String> ids = new ArrayList<String>(PresentationRuntime.openWindowIds());
        for (String id : NativeTemplateRuntime.boundIds()) if (!ids.contains(id)) ids.add(id);
        return Collections.unmodifiableList(ids);
    }

    void close(String id) {
        WindowHandle handle = find(id);
        if (handle != null) {
            handle.close();
            return;
        }
        WindowDef def = definition(id);
        if (def == null) return;
        if (def.windowClass == WindowClass.SYNTHETIC && PresentationRuntime.isOpen(def.id)) {
            closeSynthetic(def.id);
        } else if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            String nativeId = nativeKey(def);
            if (NativeTemplateRuntime.isBound(nativeId)) NativeTemplateRuntime.unbind(def);
        }
    }

    LayoutNode layoutRoot(String id) {
        WindowHandle handle = find(id);
        if (!(handle instanceof TrackedHandle)) return null;
        return ((TrackedHandle) handle).root;
    }

    void clearForTests() {
        handles.clear();
        definitions.clear();
        installRetirementHook();
    }

    private WindowDef requireDefinition(String id) {
        WindowDef def = definition(id);
        if (def == null) throw new IllegalArgumentException("not registered: " + id);
        return def;
    }

    private WindowDef definition(String id) {
        if (id == null) return null;
        WindowDef def = definitions.get(id);
        return def != null ? def : definitions.get(NativeTemplateIds.canonicalize(id));
    }

    private void removeDefinitionAliases(String id) {
        WindowDef def = definitions.get(id);
        if (def == null) {
            String canonical = NativeTemplateIds.canonicalize(id);
            def = canonical == null ? null : definitions.get(canonical);
        }
        if (def == null) return;

        List<String> aliases = new ArrayList<String>();
        for (Map.Entry<String, WindowDef> entry : definitions.entrySet()) {
            if (entry.getValue() == def) aliases.add(entry.getKey());
        }
        for (String alias : aliases) definitions.remove(alias);
    }

    private void removeDefinitionAliasesForKey(String key) {
        if (key == null || key.isEmpty()) return;
        WindowDef owner = definitions.get(key);
        if (owner != null) removeDefinitionAliases(owner.id);
    }

    private void closeDefinitionHandles(String key) {
        if (key == null || key.isEmpty()) return;
        WindowDef owner = definitions.get(key);
        if (owner == null) return;

        List<WindowHandle> owned = new ArrayList<WindowHandle>();
        for (WindowHandle handle : handles.values()) {
            if (handle instanceof TrackedHandle
                    && ((TrackedHandle) handle).def == owner
                    && !owned.contains(handle)) {
                owned.add(handle);
            }
        }
        for (WindowHandle handle : owned) handle.close();
    }

    private WindowHandle openSynthetic(WindowDef def) {
        closeCurrent(def.id);
        LayoutNode root = SyntheticRuntime.open(def);
        WindowHandle handle = new TrackedHandle(def, root);
        handles.put(def.id, handle);
        PresentationContext context = PresentationRuntime.context(def.id);
        EntityId rootEntity = PresentationRuntime.root(context);
        if (context != null && rootEntity != null) {
            ops.onTreeMounted(def.id);
            HostBackends.get().attach(new PresentationMount(context, rootEntity));
        }
        return handle;
    }

    private void closeCurrent(String id) {
        WindowHandle current = handles.get(id);
        if (current != null && current.isOpen()) current.close();
    }

    private void closeSynthetic(String id) {
        PresentationContext context = PresentationRuntime.context(id);
        EntityId root = PresentationRuntime.root(context);
        if (context != null && root != null) {
            HostBackends.get().detach(new PresentationMount(context, root));
        }
        SyntheticRuntime.onClosed(id);
    }

    private String nativeKey(WindowDef def) {
        String nativeId = NativeTemplateIds.canonicalize(def.resource);
        if (nativeId == null || nativeId.isEmpty()) {
            nativeId = NativeTemplateIds.canonicalize(def.id);
        }
        return nativeId != null ? nativeId : "";
    }

    private void installRetirementHook() {
        SyntheticRuntime.installRetirementHook(new SyntheticRuntime.RetirementHook() {
            @Override public void onRetired(String windowId) {
                try {
                    ops.onTreeClosed(windowId);
                } finally {
                    UiLabListeners.onTreeClosed(windowId);
                }
            }
        });
    }

    private final class TrackedHandle implements WindowHandle {
        private final WindowDef def;
        private final LayoutNode root;
        private final String openId;

        TrackedHandle(WindowDef def, LayoutNode root) {
            this.def = def;
            this.root = root;
            String id = def.id;
            if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
                String nativeId = NativeTemplateIds.canonicalize(def.resource);
                if (nativeId != null && !nativeId.isEmpty()) id = nativeId;
            }
            this.openId = id;
        }

        @Override public String id() { return def.id; }
        @Override public WindowClass windowClass() { return def.windowClass; }

        @Override public boolean isOpen() {
            if (!isCurrent(this)) return false;
            return def.windowClass == WindowClass.SYNTHETIC
                    ? PresentationRuntime.isOpen(def.id) : NativeTemplateRuntime.isBound(openId);
        }

        @Override public void close() {
            if (!isCurrent(this)) return;
            removeAliases(this);
            if (def.windowClass == WindowClass.SYNTHETIC) closeSynthetic(def.id);
            else NativeTemplateRuntime.unbind(def);
        }
    }

    private boolean isCurrent(WindowHandle handle) {
        for (WindowHandle current : handles.values()) if (current == handle) return true;
        return false;
    }

    private void removeAliases(WindowHandle handle) {
        List<String> aliases = new ArrayList<String>();
        for (Map.Entry<String, WindowHandle> entry : handles.entrySet()) {
            if (entry.getValue() == handle) aliases.add(entry.getKey());
        }
        for (String alias : aliases) handles.remove(alias);
    }
}
