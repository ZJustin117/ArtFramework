package artframework.presentation;

import artframework.component.EffectDecl;
import artframework.component.LayoutEngine;
import artframework.component.LayoutResult;
import artframework.component.Rect;
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
    private final String treeScope;
    private final SignalHub signals = new SignalHub();
    private final Map<String, EntityId> paths = new LinkedHashMap<String, EntityId>();
    private EntityId root;
    private PresentResolved resolvedPresent;
    private NodeTreeLifecycle lifecycle;

    public NodeTree(String scope) { this(scope, new PresentationContext(scope)); }

    private NodeTree(String scope, PresentationContext context) {
        this.treeScope = scope;
        this.context = context;
    }
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

    public String scope() { return treeScope; }
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
    void setProperty(EntityId entity, String property, Object value) {
        NodePropertiesComponent properties = properties(entity);
        if (properties == null) throw new IllegalStateException("node properties unavailable");
        context.world().put(entity, NodePropertiesComponent.class, properties.with(property, value));
    }

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
        tree.materializeVisuals(declaration);
        tree.mount(lifecycle);
        artframework.core.NodeConnections.syncTree(tree);
        artframework.core.AnimationPlayers.syncTree(tree);
        artframework.core.NodeStateMachines.syncTree(tree);
        return tree;
    }

    static NodeTree mountRegistered(String scope, UiNode declaration, NodeTreeLifecycle lifecycle) {
        if (declaration == null) throw new IllegalArgumentException("declaration required");
        NodeTree tree = new NodeTree(scope, PresentationRegistry.context(scope));
        tree.materialize(declaration, null, "", "c1");
        tree.materializeVisuals(declaration);
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
        List<Map<String, Object>> connections = mapList(declaration.props.get("connections"));
        List<Map<String, Object>> triggers = mapList(declaration.props.get("triggers"));
        if (!connections.isEmpty() || !triggers.isEmpty()) {
            context.world().put(entity, ConnectionDeclarationsComponent.class,
                    new ConnectionDeclarationsComponent(connections, triggers));
        }
        Object initialControlValue = ControlValueSystem.initialValue(
                declaration.type, context.world().get(entity, NodePropertiesComponent.class));
        if (initialControlValue != null) {
            context.world().put(entity, ControlValueComponent.class,
                    new ControlValueComponent(initialControlValue));
        }
        List<String> declaredSignals = new ArrayList<String>(declaration.signals);
        setPorts(entity, declaredSignals, declaredSignals);
        EffectsComponent effects = context.world().get(entity, EffectsComponent.class);
        for (EffectDecl effect : resolvedEffects(declaration)) {
            effects = effects.withAttachment(new EffectAttachment(
                    effect.id, effectLayer(effect), effect.params));
        }
        context.world().put(entity, EffectsComponent.class, effects);
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

    /** Resolve declaration/default effects once into ECS data; host renderers only consume it. */
    private static List<EffectDecl> resolvedEffects(UiNode declaration) {
        if (declaration == null) return Collections.emptyList();
        List<EffectDecl> result = declaration.effects;
        if (result == null || result.isEmpty()) {
            try {
                result = artframework.core.PresentPackApply.effectDefaultsForType(declaration.type);
            } catch (Throwable ignored) {
                result = Collections.emptyList();
            }
        }
        if (!ArtNodeTypes.SHADER_EFFECT.equals(declaration.type)) {
            return result != null ? result : Collections.<EffectDecl>emptyList();
        }
        String effectId = declaration.propString("effect", "");
        if (effectId.isEmpty()) return result != null ? result : Collections.<EffectDecl>emptyList();
        Map<String, Object> params = new LinkedHashMap<String, Object>(declaration.props);
        params.remove("effect");
        List<EffectDecl> withShader = new ArrayList<EffectDecl>(
                result != null ? result : Collections.<EffectDecl>emptyList());
        withShader.add(new EffectDecl(effectId, params));
        return withShader;
    }

    private static String effectLayer(EffectDecl effect) {
        if (effect == null || effect.params == null) return "ambient";
        Object layer = effect.params.get("layer");
        return layer != null && !String.valueOf(layer).trim().isEmpty()
                ? String.valueOf(layer).trim() : "ambient";
    }

    /** Converts the immutable C1 declaration layout into host-neutral visual component data. */
    private void materializeVisuals(UiNode declaration) {
        LayoutResult layout = LayoutEngine.layout(declaration);
        materializeVisuals(declaration, "", "", 0, 0f, layout);
    }

    private void materializeVisuals(
            UiNode declaration,
            String parentPath,
            String parentKey,
            int siblingIndex,
            float z,
            LayoutResult layout) {
        if (declaration == null) return;
        String name = declaration.id.isEmpty() ? "@" + siblingIndex : declaration.id;
        String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
        EntityId entity = paths.get(path);
        if (entity != null) {
            String key = LayoutEngine.effectKey(declaration, parentKey, siblingIndex);
            Rect bounds = layout.boundsOf(key);
            if (bounds != null) {
                context.world().put(entity, BoundsComponent.class, new BoundsComponent(bounds, z));
            }
            String text = declaration.propString("text", declaration.propString("title", ""));
            context.world().put(entity, DrawComponent.class,
                    new DrawComponent(declaration.type, declaration.propString("resource", ""), text));
            context.world().put(entity, VisibilityComponent.class,
                    new VisibilityComponent(booleanProp(declaration, "visible", true),
                            floatProp(declaration, "opacity", 1f)));
            context.world().put(entity, HostBindingComponent.class,
                    new HostBindingComponent("SCENE2D_C1", windowId() + ":" + key));
        }
        String key = LayoutEngine.effectKey(declaration, parentKey, siblingIndex);
        for (int i = 0; i < declaration.children.size(); i++) {
            materializeVisuals(declaration.children.get(i), path, key, i, z + 0.01f, layout);
        }
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
        if (isMounted()) return;
        this.lifecycle = lifecycle;
        transitionMount(root, lifecycle);
        transitionReady(root, lifecycle);
    }

    private boolean isMounted() {
        if (root == null || !context.world().contains(root)) return false;
        NodeLifecycleComponent state = context.world().get(root, NodeLifecycleComponent.class);
        return state != null && state.mounted;
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

    /** ECS-derived C1 control compatibility snapshot. */
    public Map<String, Object> probeControls() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        List<String> buttons = new ArrayList<String>();
        List<String> sliders = new ArrayList<String>();
        List<String> hitAreas = new ArrayList<String>();
        List<String> textFields = new ArrayList<String>();
        List<String> checkboxes = new ArrayList<String>();
        List<String> progress = new ArrayList<String>();
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        Map<String, Object> sliderValues = new LinkedHashMap<String, Object>();
        Map<String, Object> textValues = new LinkedHashMap<String, Object>();
        Map<String, Object> checkboxValues = new LinkedHashMap<String, Object>();
        Map<String, Object> progressValues = new LinkedHashMap<String, Object>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = identity(entity);
            if (identity == null) continue;
            String type = identity.type;
            String name = identity.name;
            if (artframework.component.UiTypes.BUTTON.equals(type)) buttons.add(name);
            if (artframework.component.UiTypes.HITAREA.equals(type)) hitAreas.add(name);
            if (artframework.component.UiTypes.SLIDER.equals(type)) sliders.add(name);
            if (artframework.component.UiTypes.TEXTFIELD.equals(type)) textFields.add(name);
            if (artframework.component.UiTypes.CHECKBOX.equals(type)) checkboxes.add(name);
            if (artframework.component.UiTypes.PROGRESS.equals(type)) progress.add(name);
            ControlValueComponent value = context.world().get(entity, ControlValueComponent.class);
            if (value != null) {
                values.put(name, value.value);
                if (artframework.component.UiTypes.SLIDER.equals(type)) sliderValues.put(name, value.value);
                if (artframework.component.UiTypes.TEXTFIELD.equals(type)) textValues.put(name, value.value);
                if (artframework.component.UiTypes.CHECKBOX.equals(type)) checkboxValues.put(name, value.value);
                if (artframework.component.UiTypes.PROGRESS.equals(type)) progressValues.put(name, value.value);
            }
        }
        out.put("buttonIds", buttons);
        out.put("sliderIds", sliders);
        out.put("hitAreaIds", hitAreas);
        out.put("textFieldIds", textFields);
        out.put("checkboxIds", checkboxes);
        out.put("progressIds", progress);
        out.put("values", values);
        out.put("sliders", sliderValues);
        out.put("texts", textValues);
        out.put("checkboxes", checkboxValues);
        out.put("progress", progressValues);
        return out;
    }

    /** Advance behavior nodes owned by this tree. */
    public void tick(float deltaSeconds) {
        if (deltaSeconds < 0f) throw new IllegalArgumentException("deltaSeconds must be non-negative");
        artframework.ecs.EcsPipeline.run(context.world(),
                new artframework.ecs.EcsTick(deltaSeconds, 0L),
                java.util.Collections.<artframework.ecs.EcsSystem>singletonList(new ControlValueSystem()));
        artframework.core.AnimationPlayers.tick(windowId(), deltaSeconds);
    }

    public void unmount() { close(); }

    @Override public void close() {
        if (isMounted()) {
            transitionUnmount(root, lifecycle);
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

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object raw) {
        if (!(raw instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) raw) {
            if (item instanceof Map) result.add((Map<String, Object>) item);
        }
        return result;
    }
}
