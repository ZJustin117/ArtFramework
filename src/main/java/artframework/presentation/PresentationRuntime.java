package artframework.presentation;

import artframework.component.Rect;
import artframework.core.SignalHandler;
import artframework.core.SignalHub;
import artframework.core.SignalListener;
import artframework.core.SignalSubscription;
import artframework.ecs.EntityId;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.component.UiTypes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** ECS-only lookup and signal operations for presentation contexts. */
public final class PresentationRuntime {
    private static final String C1_SCOPE_PREFIX = "tree:";
    private static final Map<String, SignalHub> SIGNALS = new LinkedHashMap<String, SignalHub>();

    private PresentationRuntime() {}

    public static String c1Scope(String windowId) {
        if (windowId == null || windowId.trim().isEmpty()) {
            throw new IllegalArgumentException("windowId required");
        }
        return C1_SCOPE_PREFIX + windowId;
    }

    public static String windowId(PresentationContext context) {
        String scope = context != null ? context.scope() : "";
        return scope.startsWith(C1_SCOPE_PREFIX) ? scope.substring(C1_SCOPE_PREFIX.length()) : scope;
    }

    public static PresentationContext context(String windowId) {
        PresentationContext context = PresentationRegistry.existingContext(c1Scope(windowId));
        return context != null && !context.entities().isEmpty() ? context : null;
    }

    public static EntityId root(PresentationContext context) {
        if (context == null) return null;
        for (EntityId entity : context.entities()) {
            NodeHierarchyComponent hierarchy = hierarchy(context, entity);
            if (hierarchy != null && hierarchy.parent == null) return entity;
        }
        return null;
    }

    public static boolean isOpen(String windowId) {
        PresentationContext context = context(windowId);
        EntityId entity = root(context);
        NodeLifecycleComponent state = component(context, entity, NodeLifecycleComponent.class);
        return state != null && state.mounted;
    }

    public static List<String> openWindowIds() {
        List<String> result = new ArrayList<String>();
        for (String scope : PresentationRegistry.scopes()) {
            if (!scope.startsWith(C1_SCOPE_PREFIX)) continue;
            String windowId = scope.substring(C1_SCOPE_PREFIX.length());
            if (isOpen(windowId)) result.add(windowId);
        }
        return Collections.unmodifiableList(result);
    }

    public static Map<String, Object> probeControls(PresentationContext context) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        List<String> buttons = new ArrayList<String>();
        List<String> sliders = new ArrayList<String>();
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (EntityId entity : context != null ? context.entities() : Collections.<EntityId>emptyList()) {
            NodeIdentityComponent identity = identity(context, entity);
            if (identity == null) continue;
            if (UiTypes.BUTTON.equals(identity.type)) buttons.add(identity.name);
            if (UiTypes.SLIDER.equals(identity.type)) sliders.add(identity.name);
            ControlValueComponent value = component(context, entity, ControlValueComponent.class);
            if (value != null) values.put(identity.name, value.value);
        }
        out.put("buttonIds", buttons);
        out.put("sliderIds", sliders);
        out.put("values", values);
        return out;
    }

    public static EntityId find(PresentationContext context, String pathOrId) {
        if (context == null || pathOrId == null || pathOrId.isEmpty()) return null;
        EntityId exact = context.entity(new PresentationKey("ui", pathOrId));
        if (exact != null) return exact;
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = identity(context, entity);
            if (identity != null && (pathOrId.equals(identity.name)
                    || identity.key.localId.endsWith("/" + pathOrId))) return entity;
        }
        return null;
    }

    public static NodeIdentityComponent identity(PresentationContext context, EntityId entity) {
        return known(context, entity) ? context.world().get(entity, NodeIdentityComponent.class) : null;
    }

    public static NodeHierarchyComponent hierarchy(PresentationContext context, EntityId entity) {
        return known(context, entity) ? context.world().get(entity, NodeHierarchyComponent.class) : null;
    }

    public static List<EntityId> children(PresentationContext context, EntityId entity) {
        NodeHierarchyComponent hierarchy = hierarchy(context, entity);
        return hierarchy != null ? hierarchy.children : Collections.<EntityId>emptyList();
    }

    public static Object property(PresentationContext context, EntityId entity, String key) {
        NodePropertiesComponent props = component(context, entity, NodePropertiesComponent.class);
        return props != null ? props.get(key) : null;
    }

    public static void setProperty(PresentationContext context, EntityId entity, String key, Object value) {
        NodePropertiesComponent props = component(context, entity, NodePropertiesComponent.class);
        if (props == null) throw new IllegalArgumentException("node properties unavailable");
        context.world().put(entity, NodePropertiesComponent.class, props.with(key, value));
    }

    public static Rect bounds(PresentationContext context, EntityId entity) {
        BoundsComponent bounds = component(context, entity, BoundsComponent.class);
        return bounds != null ? bounds.rect : Rect.ZERO;
    }

    public static PresentationFrame frame(PresentationContext context) {
        return PresentationFrame.from(context);
    }

    public static artframework.component.UiNode declaration(PresentationContext context) {
        EntityId root = root(context);
        C1DeclarationComponent declaration = component(context, root, C1DeclarationComponent.class);
        return declaration != null ? declaration.root : null;
    }

    public static void tick(PresentationContext context, float deltaSeconds) {
        if (context == null) return;
        if (deltaSeconds < 0f) throw new IllegalArgumentException("deltaSeconds must be non-negative");
        EcsPipeline.run(context.world(), new EcsTick(deltaSeconds, 0L),
                Collections.<EcsSystem>singletonList(new ControlValueSystem()));
        artframework.core.AnimationPlayers.tick(windowId(context), deltaSeconds);
    }

    public static SignalSubscription connect(PresentationContext context, EntityId entity,
            String signal, SignalHandler handler) {
        requirePort(context, entity, signal);
        final String nodePath = identity(context, entity).key.localId;
        final SignalHub hub = signals(context);
        hub.connect(nodePath, signal, handler);
        return new SignalSubscription() {
            @Override public void disconnect() { hub.disconnect(nodePath, signal, handler); }
            @Override public boolean isConnected() { return hub.handlerCount(nodePath, signal) > 0; }
        };
    }

    public static SignalSubscription connectBus(PresentationContext context, String name,
            SignalListener listener) {
        return signals(context).connectBus(name, listener);
    }

    public static SignalSubscription connectBus(PresentationContext context, Pattern pattern,
            SignalListener listener) {
        return signals(context).connectBus(pattern, listener);
    }

    public static void emit(PresentationContext context, EntityId entity, String signal,
            Object... payload) {
        dispatch(context, entity, signal, payload);
    }

    public static artframework.core.SignalDispatchResult dispatch(
            PresentationContext context, EntityId entity, String signal, Object... payload) {
        requirePort(context, entity, signal);
        return signals(context).dispatch(identity(context, entity).key.localId, signal, payload);
    }

    public static void clearSignals(PresentationContext context) {
        if (context == null) return;
        SignalHub signals = SIGNALS.remove(context.scope());
        if (signals != null) signals.clear();
    }

    public static <T> T component(PresentationContext context, EntityId entity, Class<T> type) {
        return known(context, entity) ? context.world().get(entity, type) : null;
    }

    private static boolean known(PresentationContext context, EntityId entity) {
        return context != null && entity != null && context.world().contains(entity);
    }

    private static SignalHub signals(PresentationContext context) {
        String scope = context.scope();
        SignalHub hub = SIGNALS.get(scope);
        if (hub == null) {
            hub = new SignalHub(windowId(context));
            SIGNALS.put(scope, hub);
        }
        return hub;
    }

    private static void requirePort(PresentationContext context, EntityId entity, String signal) {
        SignalPortsComponent ports = component(context, entity, SignalPortsComponent.class);
        if (ports == null || !ports.canEmit(signal)) {
            throw new IllegalArgumentException("undeclared signal: " + signal);
        }
    }
}
