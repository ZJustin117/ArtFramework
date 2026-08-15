package artframework.presentation;

import artframework.component.ArtNodeTypes;
import artframework.component.EffectDecl;
import artframework.component.LayoutEngine;
import artframework.component.LayoutResult;
import artframework.component.UiNode;
import artframework.core.PresentChromeStyle;
import artframework.core.PresentResolve;
import artframework.core.SkeletonNodeBindingComponent;
import artframework.ecs.EntityId;
import artframework.skeleton.SkeletonNodeBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Materializes immutable C1 declarations into data-only ECS components. */
public final class C1Materializer {
    private C1Materializer() {}

    public static EntityId mount(PresentationContext context, UiNode declaration) {
        if (context == null || declaration == null) throw new IllegalArgumentException("context and declaration required");
        EntityId root = materialize(context, declaration, null, "", "c1", 0);
        context.world().put(root, C1DeclarationComponent.class, new C1DeclarationComponent(declaration));
        LayoutResult layout = LayoutEngine.layout(declaration);
        materializeVisuals(context, declaration, "", "", 0, 0f, layout);
        transition(context, root, true, false);
        transitionReady(context, root);
        return root;
    }

    private static EntityId materialize(PresentationContext context, UiNode declaration, EntityId parent,
            String parentPath, String source, int siblingIndex) {
        String name = declaration.id.isEmpty() ? "@" + siblingIndex : declaration.id;
        String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
        EntityId entity = context.create(new PresentationKey("ui", path), name, declaration.type, source);
        attach(context, entity, parent);
        context.world().put(entity, NodePropertiesComponent.class, new NodePropertiesComponent(declaration.props));
        List<Map<String, Object>> connections = mapList(declaration.props.get("connections"));
        List<Map<String, Object>> triggers = mapList(declaration.props.get("triggers"));
        if (!connections.isEmpty() || !triggers.isEmpty()) {
            context.world().put(entity, ConnectionDeclarationsComponent.class,
                    new ConnectionDeclarationsComponent(connections, triggers));
        }
        Object initial = ControlValueSystem.initialValue(declaration.type,
                context.world().get(entity, NodePropertiesComponent.class));
        if (initial != null) context.world().put(entity, ControlValueComponent.class,
                new ControlValueComponent(initial));
        context.world().put(entity, SignalPortsComponent.class,
                new SignalPortsComponent(declaration.signals));
        EffectsComponent effects = context.world().get(entity, EffectsComponent.class);
        for (EffectDecl effect : resolvedEffects(declaration)) {
            effects = effects.withAttachment(new EffectAttachment(effect.id, effectLayer(effect), effect.params));
        }
        context.world().put(entity, EffectsComponent.class, effects);
        if (ArtNodeTypes.SKELETON.equals(declaration.type)) putSkeletonBinding(context, entity, declaration);
        PresentChromeStyle chrome = PresentResolve.forEntity(context, entity).chrome;
        context.world().put(entity, ChromeComponent.class, new ChromeComponent(chrome.panelR, chrome.panelG,
                chrome.panelB, chrome.panelAlpha, chrome.borderR, chrome.borderG, chrome.borderB,
                chrome.borderA, chrome.borderWidth));
        for (int i = 0; i < declaration.children.size(); i++) {
            materialize(context, declaration.children.get(i), entity, path, source, i);
        }
        return entity;
    }

    private static void materializeVisuals(PresentationContext context, UiNode declaration,
            String parentPath, String parentKey, int siblingIndex, float z, LayoutResult layout) {
        String name = declaration.id.isEmpty() ? "@" + siblingIndex : declaration.id;
        String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
        EntityId entity = context.entity(new PresentationKey("ui", path));
        String key = LayoutEngine.effectKey(declaration, parentKey, siblingIndex);
        if (entity != null) {
            artframework.component.Rect bounds = layout.boundsOf(key);
            if (bounds != null) context.world().put(entity, BoundsComponent.class, new BoundsComponent(bounds, z));
            context.world().put(entity, DrawComponent.class, new DrawComponent(declaration.type,
                    declaration.propString("resource", ""), declaration.propString("text", declaration.propString("title", ""))));
            context.world().put(entity, VisibilityComponent.class, new VisibilityComponent(
                    booleanProp(declaration, "visible", true), floatProp(declaration, "opacity", 1f)));
            context.world().put(entity, HostBindingComponent.class,
                    new HostBindingComponent("SCENE2D_C1", PresentationRuntime.windowId(context) + ":" + key));
        }
        for (int i = 0; i < declaration.children.size(); i++) {
            materializeVisuals(context, declaration.children.get(i), path, key, i, z + 0.01f, layout);
        }
    }

    private static void attach(PresentationContext context, EntityId child, EntityId parent) {
        if (parent == null) return;
        NodeHierarchyComponent parentState = context.world().get(parent, NodeHierarchyComponent.class);
        List<EntityId> children = new ArrayList<EntityId>(parentState.children);
        children.add(child);
        context.world().put(parent, NodeHierarchyComponent.class,
                new NodeHierarchyComponent(parentState.parent, children));
        NodeHierarchyComponent childState = context.world().get(child, NodeHierarchyComponent.class);
        context.world().put(child, NodeHierarchyComponent.class,
                new NodeHierarchyComponent(parent, childState.children));
    }

    private static void transition(PresentationContext context, EntityId entity, boolean mounted, boolean ready) {
        context.world().put(entity, NodeLifecycleComponent.class, new NodeLifecycleComponent(mounted, ready));
        for (EntityId child : PresentationRuntime.children(context, entity)) transition(context, child, mounted, ready);
    }

    private static void transitionReady(PresentationContext context, EntityId entity) {
        for (EntityId child : PresentationRuntime.children(context, entity)) transitionReady(context, child);
        context.world().put(entity, NodeLifecycleComponent.class, new NodeLifecycleComponent(true, true));
    }

    private static void putSkeletonBinding(PresentationContext context, EntityId entity, UiNode declaration) {
        Object key = declaration.props.get("entity");
        if (key == null) key = declaration.props.get("entity_key");
        if (key == null || String.valueOf(key).trim().isEmpty()) return;
        context.world().put(entity, SkeletonNodeBindingComponent.class,
                new SkeletonNodeBindingComponent(new SkeletonNodeBinding(String.valueOf(key),
                        stringProp(declaration, "anchor", ""), floatProp(declaration, "offset_x", 0f),
                        floatProp(declaration, "offset_y", 0f), floatProp(declaration, "local_scale", 1f),
                        booleanProp(declaration, "owner", false) || booleanProp(declaration, "node_owned", false))));
    }

    private static List<EffectDecl> resolvedEffects(UiNode declaration) {
        List<EffectDecl> result = declaration.effects;
        if (result == null || result.isEmpty()) result = artframework.core.PresentPackApply.effectDefaultsForType(declaration.type);
        if (!ArtNodeTypes.SHADER_EFFECT.equals(declaration.type)) return result != null ? result : Collections.<EffectDecl>emptyList();
        String effectId = declaration.propString("effect", "");
        if (effectId.isEmpty()) return result != null ? result : Collections.<EffectDecl>emptyList();
        Map<String, Object> params = new LinkedHashMap<String, Object>(declaration.props);
        params.remove("effect");
        List<EffectDecl> merged = new ArrayList<EffectDecl>(result != null ? result : Collections.<EffectDecl>emptyList());
        merged.add(new EffectDecl(effectId, params));
        return merged;
    }

    private static String effectLayer(EffectDecl effect) {
        Object layer = effect != null && effect.params != null ? effect.params.get("layer") : null;
        return layer == null || String.valueOf(layer).trim().isEmpty() ? "ambient" : String.valueOf(layer).trim();
    }

    private static String stringProp(UiNode node, String key, String fallback) {
        Object value = node.props.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
    private static float floatProp(UiNode node, String key, float fallback) {
        Object value = node.props.get(key);
        if (value instanceof Number) return ((Number) value).floatValue();
        try { return value != null ? Float.parseFloat(String.valueOf(value)) : fallback; }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private static boolean booleanProp(UiNode node, String key, boolean fallback) {
        Object value = node.props.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object raw) {
        if (!(raw instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object value : (List<?>) raw) if (value instanceof Map) result.add((Map<String, Object>) value);
        return result;
    }
}
