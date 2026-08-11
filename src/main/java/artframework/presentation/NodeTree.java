package artframework.presentation;

import artframework.component.EffectDecl;
import artframework.component.UiNode;
import artframework.core.SignalHandler;
import artframework.core.SignalHub;
import artframework.core.SignalSubscription;
import artframework.core.SignalListener;
import artframework.core.PresentResolved;
import artframework.core.PresentChromeStyle;
import artframework.core.PresentResolve;
import artframework.core.PresentProfile;
import artframework.core.PresentProfiles;
import artframework.core.ProjectPresent;
import artframework.core.Themes;
import artframework.core.SkeletonNodeBindingComponent;
import artframework.skeleton.SkeletonNodeBinding;
import artframework.component.ArtNodeTypes;
import artframework.ecs.EntityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** ECS-backed scene tree with Godot-style Node access and signal lifecycle. */
public final class NodeTree implements AutoCloseable {
    private final PresentationContext context;
    private final SignalHub signals = new SignalHub();
    private final Map<String, EntityId> paths = new LinkedHashMap<String, EntityId>();
    private EntityId root;
    private PresentResolved resolvedPresent;
    private NodeTreeLifecycle lifecycle;
    private boolean mounted;

    public NodeTree(String scope) { context = new PresentationContext(scope); }
    public PresentationContext context() { return context; }
    public Node root() { return node(root); }
    public Node find(String path) { return node(paths.get(path)); }
    public Node get(String id) {
        if (id == null) return null;
        EntityId entity = paths.get(id);
        if (entity == null) {
            for (Map.Entry<String, EntityId> entry : paths.entrySet()) {
                if (entry.getKey().endsWith("/" + id)) {
                    entity = entry.getValue();
                    break;
                }
            }
        }
        return node(entity);
    }

    public String scope() { return context.world().scope(); }
    public String windowId() { return scope().replace("tree:", ""); }
    public artframework.ecs.PresentationWorld world() { return context.world(); }
    public artframework.core.Theme theme() { return resolvePresent().theme; }
    public PresentResolved resolvePresent() {
        if (resolvedPresent != null) return resolvedPresent;
        if (root != null) {
            NodePropertiesComponent props = properties(root);
            Object profileId = props.get("present_profile");
            if (profileId == null) profileId = props.get("presentProfile");
            if (profileId != null) {
                PresentProfile profile = PresentProfiles.get(String.valueOf(profileId));
                if (profile != null) {
                    resolvedPresent = new PresentResolved(
                            profile.id, profile.theme, profile.chrome, profile.packId, false);
                    return resolvedPresent;
                }
            }
            Object themeName = props.get("theme");
            if (themeName != null) {
                artframework.core.Theme theme = Themes.get(String.valueOf(themeName));
                if (theme != null) {
                    resolvedPresent = new PresentResolved(
                            theme.name(), theme, PresentChromeStyle.fromTheme(theme), "", false);
                    return resolvedPresent;
                }
            }
        }
        return ProjectPresent.resolved();
    }
    public void refreshPresent() { resolvedPresent = null; }
    public Node node(EntityId entity) { return entity == null ? null : new Node(this, entity); }
    NodeIdentityComponent identity(EntityId entity) { return context.world().get(entity, NodeIdentityComponent.class); }
    NodeHierarchyComponent hierarchy(EntityId entity) { return context.world().get(entity, NodeHierarchyComponent.class); }
    NodePropertiesComponent properties(EntityId entity) { return context.world().get(entity, NodePropertiesComponent.class); }

    public Node create(PresentationKey key, String name, String type, String source, EntityId parent) {
        EntityId entity = context.create(key, name, type, source);
        attach(entity, parent);
        if (root == null) root = entity;
        return node(entity);
    }

    /** Materialize a C1 declaration into ECS-backed nodes. */
    public static NodeTree mount(String scope, UiNode declaration, NodeTreeLifecycle lifecycle) {
        if (declaration == null) throw new IllegalArgumentException("declaration required");
        NodeTree tree = new NodeTree(scope);
        tree.materialize(declaration, null, "", "c1");
        tree.mount(lifecycle);
        artframework.core.NodeConnections.syncTree(tree);
        artframework.core.AnimationPlayers.syncTree(tree);
        artframework.core.NodeStateMachines.syncTree(tree);
        return tree;
    }

    private EntityId materialize(UiNode declaration, EntityId parent, String parentPath, String source) {
        String name = declaration.id.isEmpty() ? "@" + paths.size() : declaration.id;
        String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
        Node node = create(new PresentationKey("ui", path), name, declaration.type, source, parent);
        EntityId entity = node.entityId();
        context.world().put(entity, NodePropertiesComponent.class,
                new NodePropertiesComponent(declaration.props));
        List<String> declaredSignals = new ArrayList<String>(declaration.signals);
        setPorts(entity, declaredSignals, declaredSignals);
        EffectsComponent effects = context.world().get(entity, EffectsComponent.class);
        for (EffectDecl effect : declaration.effects) {
            effects.put(new EffectAttachment(effect.id, "ambient", effect.params));
        }
        if (ArtNodeTypes.SKELETON.equals(declaration.type)) {
            Object entityKey = declaration.props.get("entity");
            if (entityKey == null) entityKey = declaration.props.get("entity_key");
            if (entityKey != null && !String.valueOf(entityKey).trim().isEmpty()) {
                context.world().put(entity, SkeletonNodeBindingComponent.class,
                        new SkeletonNodeBindingComponent(new SkeletonNodeBinding(
                                String.valueOf(entityKey), stringProp(declaration, "anchor", ""),
                                floatProp(declaration, "offset_x", 0f),
                                floatProp(declaration, "offset_y", 0f),
                                floatProp(declaration, "local_scale", 1f),
                                booleanProp(declaration, "owner", false)
                                        || booleanProp(declaration, "node_owned", false))));
            }
        }
        PresentChromeStyle chrome = PresentResolve.forNode(node).chrome;
        context.world().put(entity, ChromeComponent.class, new ChromeComponent(
                chrome.panelR, chrome.panelG, chrome.panelB, chrome.panelAlpha,
                chrome.borderR, chrome.borderG, chrome.borderB, chrome.borderA, chrome.borderWidth));
        paths.put(path, entity);
        for (UiNode child : declaration.children) materialize(child, entity, path, source);
        return entity;
    }

    public void attach(EntityId child, EntityId parent) {
        if (child == null || !context.world().contains(child)) throw new IllegalArgumentException("known child required");
        NodeHierarchyComponent childHierarchy = hierarchy(child);
        if (childHierarchy.parent != null) throw new IllegalStateException("child already attached");
        if (parent == null) return;
        NodeHierarchyComponent parentHierarchy = hierarchy(parent);
        List<EntityId> children = new ArrayList<EntityId>(parentHierarchy.children);
        children.add(child);
        context.world().put(parent, NodeHierarchyComponent.class, new NodeHierarchyComponent(parentHierarchy.parent, children));
        context.world().put(child, NodeHierarchyComponent.class, new NodeHierarchyComponent(parent, childHierarchy.children));
    }

    public void mount() { mount(null); }
    public void mount(NodeTreeLifecycle lifecycle) {
        if (mounted) return;
        this.lifecycle = lifecycle;
        transitionMount(root, lifecycle);
        transitionReady(root, lifecycle);
        mounted = root != null;
    }
    private void transitionMount(EntityId entity, NodeTreeLifecycle lifecycle) {
        if (entity == null) return;
        context.world().put(entity, NodeLifecycleComponent.class, new NodeLifecycleComponent(true, false));
        if (lifecycle != null) lifecycle.onMount(node(entity));
        for (EntityId child : hierarchy(entity).children) transitionMount(child, lifecycle);
    }
    private void transitionReady(EntityId entity, NodeTreeLifecycle lifecycle) {
        if (entity == null) return;
        for (EntityId child : hierarchy(entity).children) transitionReady(child, lifecycle);
        context.world().put(entity, NodeLifecycleComponent.class, new NodeLifecycleComponent(true, true));
        if (lifecycle != null) lifecycle.onReady(node(entity));
    }

    public SignalSubscription connect(EntityId entity, String signal, SignalHandler handler) {
        requirePort(entity, signal);
        String id = identity(entity).name;
        signals.connect(id, signal, handler);
        return new SignalSubscription() {
            @Override public void disconnect() { signals.disconnect(id, signal, handler); }
            @Override public boolean isConnected() { return signals.handlerCount(id, signal) > 0; }
        };
    }

    public SignalSubscription connect(String id, String signal, SignalHandler handler) {
        Node target = get(id);
        if (target == null) throw new IllegalArgumentException("unknown node: " + id);
        return connect(target.entityId(), signal, handler);
    }

    public void disconnect(String id, String signal, SignalHandler handler) {
        Node target = get(id);
        if (target != null) signals.disconnect(identity(target.entityId()).name, signal, handler);
    }

    public SignalSubscription connectBus(String name, SignalListener listener) {
        return signals.connectBus(name, listener);
    }

    public SignalSubscription connectBus(Pattern pattern, SignalListener listener) {
        return signals.connectBus(pattern, listener);
    }

    public void emit(EntityId entity, String signal, Object... payload) {
        requirePort(entity, signal);
        signals.emit(identity(entity).name, signal, payload);
    }

    public void emit(String id, String signal, Object... payload) {
        Node target = get(id);
        if (target == null) throw new IllegalArgumentException("unknown node: " + id);
        emit(target.entityId(), signal, payload);
    }

    public void setPorts(EntityId entity, List<String> emits, List<String> accepts) {
        context.world().put(entity, SignalPortsComponent.class, new SignalPortsComponent(emits, accepts));
    }

    private void requirePort(EntityId entity, String signal) {
        SignalPortsComponent ports = context.world().get(entity, SignalPortsComponent.class);
        if (ports == null || !ports.canEmit(signal)) throw new IllegalArgumentException("undeclared signal: " + signal);
    }

    public PresentationFrame frame() { return PresentationFrame.from(context); }

    /** Advance behavior nodes owned by this tree. */
    public void tick(float deltaSeconds) {
        if (deltaSeconds < 0f) throw new IllegalArgumentException("deltaSeconds must be non-negative");
        artframework.core.AnimationPlayers.tick(windowId(), deltaSeconds);
    }

    public void unmount() { close(); }

    @Override public void close() {
        if (mounted) {
            transitionUnmount(root, lifecycle);
            mounted = false;
        }
        String windowId = windowId();
        artframework.core.AnimationPlayers.clearWindow(windowId);
        artframework.core.NodeConnections.clearWindow(windowId);
        artframework.core.NodeStateMachines.clearWindow(windowId);
        signals.clear();
        paths.clear();
        context.close();
        root = null;
        lifecycle = null;
    }

    private void transitionUnmount(EntityId entity, NodeTreeLifecycle callback) {
        if (entity == null) return;
        List<EntityId> children = new ArrayList<EntityId>(hierarchy(entity).children);
        for (int i = children.size() - 1; i >= 0; i--) {
            transitionUnmount(children.get(i), callback);
        }
        context.world().put(entity, NodeLifecycleComponent.class,
                new NodeLifecycleComponent(false, false));
        if (callback != null) callback.onUnmount(node(entity));
    }

    private static String stringProp(UiNode node, String key, String fallback) {
        Object value = node.props.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static float floatProp(UiNode node, String key, float fallback) {
        Object value = node.props.get(key);
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value != null) {
            try { return Float.parseFloat(String.valueOf(value)); }
            catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static boolean booleanProp(UiNode node, String key, boolean fallback) {
        Object value = node.props.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
}
