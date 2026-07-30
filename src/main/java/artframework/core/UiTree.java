package artframework.core;

import artframework.component.UiNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One mounted synthetic composition tree (Godot SceneTree local to a window).
 */
public final class UiTree {

    private final String windowId;
    private final SignalHub signalHub = new SignalHub();
    private final Map<String, UiInstance> byId = new LinkedHashMap<String, UiInstance>();
    private final TreeLifecycle lifecycle;
    private Theme theme;
    private UiInstance root;
    private boolean alive = true;
    private int anonymousSequence;

    private UiTree(String windowId, TreeLifecycle lifecycle) {
        if (windowId == null || windowId.isEmpty()) {
            throw new IllegalArgumentException("windowId required");
        }
        this.windowId = windowId;
        this.lifecycle = lifecycle;
        this.theme = ProjectPresent.theme();
    }

    public static UiTree mount(String windowId, UiNode expandedRoot) {
        return mount(windowId, expandedRoot, null);
    }

    public static UiTree mount(String windowId, UiNode expandedRoot, TreeLifecycle lifecycle) {
        if (expandedRoot == null) {
            throw new IllegalArgumentException("expandedRoot required");
        }
        UiTree tree = new UiTree(windowId, lifecycle);
        tree.root = tree.build(expandedRoot, null);
        tree.applyResolvedPresent();
        tree.fireMount(tree.root, lifecycle);
        tree.fireReady(tree.root, lifecycle);
        AnimationPlayers.syncTree(tree);
        return tree;
    }

    /**
     * Tree theme from present cascade at root (node present_profile / art.present_profile /
     * theme name), else {@link ProjectPresent}. Instance themes are stamped only on nodes with
     * an explicit {@link PresentBinding} (not project fallback), so tree.setTheme still cascades.
     */
    private void applyResolvedPresent() {
        if (root == null) {
            this.theme = ProjectPresent.theme();
            return;
        }
        this.theme = PresentResolve.forNode(root).theme;
        stampPresentInstanceThemes(root);
    }

    private static void stampPresentInstanceThemes(UiInstance n) {
        if (n == null) {
            return;
        }
        PresentBinding b = n.presentBinding();
        if (b != null) {
            PresentProfile p = b.resolveResource();
            if (p != null) {
                n.setTheme(p.theme);
            }
        } else {
            Object themeName = n.prop("theme");
            if (themeName != null) {
                String name = String.valueOf(themeName).trim();
                if (!name.isEmpty()) {
                    Theme named = Themes.get(name);
                    if (named != null) {
                        n.setTheme(named);
                    }
                }
            }
        }
        for (UiInstance c : n.children()) {
            stampPresentInstanceThemes(c);
        }
    }

    public Theme theme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme != null ? theme : ProjectPresent.theme();
    }

    public PresentResolved resolvePresent() {
        return PresentResolve.forTree(this);
    }

    /** Re-apply present cascade theme stamps after project or binding change (35.1). */
    public void refreshPresent() {
        if (!alive || root == null) {
            return;
        }
        applyResolvedPresent();
    }

    public String windowId() {
        return windowId;
    }

    public UiInstance root() {
        return root;
    }

    public SignalHub signalHub() {
        return signalHub;
    }

    public boolean isAlive() {
        return alive;
    }

    public UiInstance get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return byId.get(id);
    }

    /**
     * Find by id, or by slash path of child ids from root (e.g. {@code main_col/actions/ok}).
     */
    public UiInstance find(String pathOrId) {
        if (pathOrId == null || pathOrId.isEmpty()) {
            return null;
        }
        if (pathOrId.indexOf('/') < 0) {
            return get(pathOrId);
        }
        String[] parts = pathOrId.split("/");
        UiInstance cur = root;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            UiInstance next = childById(cur, part);
            if (next == null) {
                return null;
            }
            cur = next;
        }
        return cur;
    }

    public void connect(String instanceId, String signal, SignalHandler handler) {
        requireDeclaredSignal(instanceId, signal);
        signalHub.connect(instanceId, signal, handler);
    }

    public void disconnect(String instanceId, String signal, SignalHandler handler) {
        signalHub.disconnect(instanceId, signal, handler);
    }

    public void emit(String instanceId, String signal, Object... args) {
        requireDeclaredSignal(instanceId, signal);
        signalHub.emit(instanceId, signal, args);
    }

    /**
     * When {@code instanceId} maps to a mounted instance, signal must be declared on its
     * {@link UiNode}. Unknown ids skip the check (legacy hub-only listeners).
     */
    private void requireDeclaredSignal(String instanceId, String signal) {
        if (instanceId == null || instanceId.isEmpty()) {
            return;
        }
        UiInstance inst = byId.get(instanceId);
        if (inst == null) {
            return;
        }
        if (signal == null || signal.isEmpty()) {
            throw new IllegalArgumentException("signal required");
        }
        if (!inst.declaresSignal(signal)) {
            throw new IllegalArgumentException(
                    "undeclared signal \"" + signal + "\" on instance \"" + instanceId + "\"");
        }
    }

    public void unmount() {
        if (!alive) {
            return;
        }
        AnimationPlayers.clearWindow(windowId);
        fireUnmount(root, lifecycle);
        signalHub.clear();
        byId.clear();
        alive = false;
    }

    /** Advance animation players for this tree. */
    public void tick(float deltaSeconds) {
        if (!alive) {
            return;
        }
        AnimationPlayers.tick(windowId, deltaSeconds);
    }

    private UiInstance build(UiNode node, UiInstance parent) {
        UiInstance inst = new UiInstance(this, node, parent, ++anonymousSequence);
        if (!inst.id().isEmpty()) {
            if (byId.containsKey(inst.id())) {
                throw new IllegalArgumentException("duplicate id: " + inst.id());
            }
            byId.put(inst.id(), inst);
        }
        for (UiNode c : node.children) {
            inst.addChild(build(c, inst));
        }
        return inst;
    }

    private void fireMount(UiInstance n, TreeLifecycle lifecycle) {
        n.markMounted(true);
        if (lifecycle != null) {
            lifecycle.onMount(n);
        }
        for (UiInstance c : n.children()) {
            fireMount(c, lifecycle);
        }
    }

    private void fireReady(UiInstance n, TreeLifecycle lifecycle) {
        for (UiInstance c : n.children()) {
            fireReady(c, lifecycle);
        }
        if (lifecycle != null) {
            lifecycle.onReady(n);
        }
    }

    private void fireUnmount(UiInstance n, TreeLifecycle lifecycle) {
        List<UiInstance> kids = new ArrayList<UiInstance>(n.children());
        for (int i = kids.size() - 1; i >= 0; i--) {
            fireUnmount(kids.get(i), lifecycle);
        }
        if (lifecycle != null) {
            lifecycle.onUnmount(n);
        }
        n.markMounted(false);
    }

    private static UiInstance childById(UiInstance parent, String id) {
        for (UiInstance c : parent.children()) {
            if (id.equals(c.id())) {
                return c;
            }
        }
        return null;
    }
}
