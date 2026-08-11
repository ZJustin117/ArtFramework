package artframework.presentation;

import artframework.component.Rect;
import artframework.core.SignalHandler;
import artframework.core.SignalSubscription;
import artframework.ecs.EntityId;
import java.util.ArrayList;
import java.util.List;

/** Godot-shaped, state-free facade over one presentation entity. */
public final class Node {
    private final NodeTree tree;
    private final EntityId entity;

    Node(NodeTree tree, EntityId entity) { this.tree = tree; this.entity = entity; }
    public EntityId entityId() { return entity; }
    public NodeTree tree() { return tree; }
    public String name() { return tree.identity(entity).name; }
    public String id() { return name(); }
    public String type() { return tree.identity(entity).type; }
    public boolean isMounted() {
        NodeLifecycleComponent lifecycle = tree.context().world().get(entity, NodeLifecycleComponent.class);
        return lifecycle != null && lifecycle.mounted;
    }
    public boolean isReady() {
        NodeLifecycleComponent lifecycle = tree.context().world().get(entity, NodeLifecycleComponent.class);
        return lifecycle != null && lifecycle.ready;
    }
    public boolean isVisible() {
        VisibilityComponent visibility = tree.context().world().get(entity, VisibilityComponent.class);
        return visibility == null || visibility.visible;
    }
    public void setVisible(boolean visible) {
        VisibilityComponent current = tree.context().world().get(entity, VisibilityComponent.class);
        float opacity = current == null ? 1f : current.opacity;
        tree.context().world().put(entity, VisibilityComponent.class,
                new VisibilityComponent(visible, opacity));
    }
    public Object prop(String property) { return get(property); }
    public String propString(String property, String fallback) {
        Object value = get(property);
        return value == null ? fallback : String.valueOf(value);
    }
    public artframework.core.ThemeColor getThemeColor(String name, String themeType) {
        return artframework.core.ProjectPresent.theme().getColor(themeType, name);
    }
    public int getThemeConstant(String name, String themeType) {
        return artframework.core.ProjectPresent.theme().getConstant(themeType, name);
    }
    public Node parent() { return tree.node(tree.hierarchy(entity).parent); }
    public List<Node> children() {
        List<Node> out = new ArrayList<Node>();
        for (EntityId child : tree.hierarchy(entity).children) out.add(tree.node(child));
        return out;
    }
    public Object get(String property) { return tree.properties(entity).get(property); }
    public void set(String property, Object value) { tree.properties(entity).set(property, value); }
    public void setProp(String property, Object value) { set(property, value); }
    public Rect rect() { BoundsComponent bounds = tree.context().world().get(entity, BoundsComponent.class); return bounds != null ? bounds.rect : Rect.ZERO; }
    public SignalSubscription connect(String signal, SignalHandler handler) { return tree.connect(entity, signal, handler); }
    public void emitSignal(String signal, Object... payload) { tree.emit(entity, signal, payload); }
    public java.util.List<String> signals() {
        SignalPortsComponent ports = tree.context().world().get(entity, SignalPortsComponent.class);
        return ports == null ? java.util.Collections.<String>emptyList() : ports.emits;
    }
    public boolean declaresSignal(String signal) {
        SignalPortsComponent ports = tree.context().world().get(entity, SignalPortsComponent.class);
        return ports != null && ports.canEmit(signal);
    }
}
