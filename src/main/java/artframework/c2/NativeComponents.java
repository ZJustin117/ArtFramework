package artframework.c2;

import artframework.component.MapNodeRef;
import artframework.component.NativeTemplateIds;

import artframework.api.UiOpResult;
import artframework.core.ComponentKind;
import artframework.core.SignalHandler;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalHub;
import artframework.core.SignalNames;
import artframework.core.SignalSubscription;
import artframework.core.UiComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Registry of C2 native host components ({@code sts.*}).
 */
public final class NativeComponents {

    private static final Map<String, UiComponent> BY_ID = new LinkedHashMap<String, UiComponent>();
    private static final SignalHub HUB = new SignalHub();

    static {
        register(new MapNativeComponent());
        register(new EventNativeComponent());
        register(new SelectNativeComponent(SelectKind.GRID, NativeTemplateIds.SELECT_GRID));
        register(new SelectNativeComponent(SelectKind.HAND, NativeTemplateIds.SELECT_HAND));
        register(new EndTurnNativeComponent());
    }

    private NativeComponents() {}

    private static void register(UiComponent c) {
        BY_ID.put(c.id(), c);
    }

    public static UiComponent get(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(NativeTemplateIds.canonicalize(id));
    }

    public static List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<String>(BY_ID.keySet()));
    }

    public static List<Map<String, Object>> probeAll() {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (UiComponent c : BY_ID.values()) {
            out.add(c.probeSlice());
        }
        return out;
    }

    public static SignalHub hub() {
        return HUB;
    }

    public static void emit(String id, String signal, Object... args) {
        UiComponent c = get(id);
        if (c != null) {
            c.emit(signal, args);
        }
    }

    public static void resetForTests() {
        HUB.clear();
    }

    static void clearSignals(String id) {
        HUB.clearInstance(NativeTemplateIds.canonicalize(id));
    }

    public static void syncMountFromRuntime() {
        // Native template bind state is already represented by NativeTemplateStateComponent.
    }

    abstract static class BaseNative implements UiComponent {
        private final String id;

        BaseNative(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public ComponentKind kind() {
            return ComponentKind.NATIVE_HOST;
        }

        @Override
        public boolean isMounted() {
            return NativeTemplateRuntime.isBound(id);
        }

        @Override
        public void mount() {
            // Binding is owned by NativeTemplateRuntime and reflected in ECS state.
        }

        @Override
        public void unmount() {
            HUB.clearInstance(id);
        }

        @Override
        public SignalSubscription connect(String signal, SignalHandler handler) {
            requireSignal(signal);
            return HUB.connect(id, signal, handler);
        }

        @Override
        public void disconnect(String signal, SignalHandler handler) {
            requireSignal(signal);
            HUB.disconnect(id, signal, handler);
        }

        @Override
        public SignalDispatchResult emit(String signal, Object... args) {
            requireSignal(signal);
            return HUB.dispatch(id, signal, args);
        }

        private void requireSignal(String signal) {
            if (signal == null || !declaredSignals().contains(signal)) {
                throw new IllegalArgumentException(
                        "undeclared signal \"" + signal + "\" on component \"" + id + "\"");
            }
        }

        private Set<String> declaredSignals() {
            if (NativeTemplateIds.MAP.equals(id)) {
                return new HashSet<String>(Arrays.asList(SignalNames.NODE_CLICKED));
            }
            if (NativeTemplateIds.EVENT.equals(id)) {
                return new HashSet<String>(Arrays.asList(SignalNames.OPTION_CHOSEN));
            }
            if (NativeTemplateIds.SELECT_GRID.equals(id)
                    || NativeTemplateIds.SELECT_HAND.equals(id)) {
                return new HashSet<String>(
                        Arrays.asList(SignalNames.CARD_SELECTED, SignalNames.CONFIRMED));
            }
            return new HashSet<String>(Arrays.asList(SignalNames.PRESSED));
        }

        Map<String, Object> baseProbe(boolean bound) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("kind", kind().name());
            m.put("mounted", Boolean.valueOf(isMounted()));
            m.put("bound", Boolean.valueOf(bound));
            return m;
        }

        void addContract(Map<String, Object> target, String action, String signal) {
            List<String> actions = new ArrayList<String>();
            actions.add(action);
            target.put("actions", actions);
            addSignal(target, signal);
        }

        void addSignal(Map<String, Object> target, String signal) {
            List<String> signals = new ArrayList<String>();
            Object existing = target.get("signals");
            if (existing instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> old = (List<String>) existing;
                signals.addAll(old);
            }
            if (!signals.contains(signal)) {
                signals.add(signal);
            }
            target.put("signals", signals);
        }
    }

    static final class MapNativeComponent extends BaseNative {
        MapNativeComponent() {
            super(NativeTemplateIds.MAP);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!"click_node".equals(name) && !"clickMapNode".equals(name)) {
                return UiOpResult.unavailable("unknown action: " + name);
            }
            if (args == null || args.length < 1 || !(args[0] instanceof MapNodeRef)) {
                return UiOpResult.unavailable("MapNodeRef required");
            }
            return artframework.api.ArtFramework.ops().dispatchMapNode((MapNodeRef) args[0]);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(NativeTemplateRuntime.isMapBound());
            addContract(m, "click_node", SignalNames.NODE_CLICKED);
            if (NativeTemplateRuntime.isMapBound()) {
                m.put("pinCount", Integer.valueOf(NativeTemplateRuntime.mapPins().size()));
            }
            return m;
        }
    }

    static final class EventNativeComponent extends BaseNative {
        EventNativeComponent() {
            super(NativeTemplateIds.EVENT);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!"choose_option".equals(name) && !"chooseEventOption".equals(name)) {
                return UiOpResult.unavailable("unknown action: " + name);
            }
            int index = 0;
            String label = "";
            if (args != null && args.length > 0 && args[0] instanceof Number) {
                index = ((Number) args[0]).intValue();
            }
            if (args != null && args.length > 1 && args[1] != null) {
                label = String.valueOf(args[1]);
            }
            return artframework.api.ArtFramework.ops().dispatchEventOption(index, label);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(NativeTemplateRuntime.isEventBound());
            addContract(m, "choose_option", SignalNames.OPTION_CHOSEN);
            return m;
        }
    }

    static final class SelectNativeComponent extends BaseNative {
        private final SelectKind kind;

        SelectNativeComponent(SelectKind kind, String id) {
            super(id);
            this.kind = kind;
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if ("select_card".equals(name) || "selectCard".equals(name)) {
                String cardId = "";
                int index = 0;
                if (args != null && args.length > 0 && args[0] != null) {
                    cardId = String.valueOf(args[0]);
                }
                if (args != null && args.length > 1 && args[1] instanceof Number) {
                    index = ((Number) args[1]).intValue();
                }
                return artframework.api.ArtFramework.ops().dispatchSelectCard(kind, cardId, index);
            }
            if ("confirm".equals(name) || "confirmSelect".equals(name)) {
                return artframework.api.ArtFramework.ops().dispatchConfirmSelect(kind);
            }
            return UiOpResult.unavailable("unknown action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m =
                    baseProbe(
                            kind == SelectKind.GRID
                                    ? NativeTemplateRuntime.isSelectGridBound()
                                    : NativeTemplateRuntime.isSelectHandBound());
            m.put("selectKind", kind.name());
            addContract(m, "select_card", SignalNames.CARD_SELECTED);
            addSignal(m, SignalNames.CONFIRMED);
            return m;
        }
    }

    static final class EndTurnNativeComponent extends BaseNative {
        EndTurnNativeComponent() {
            super(NativeTemplateIds.END_TURN);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!"press".equals(name) && !"pressEndTurn".equals(name)) {
                return UiOpResult.unavailable("unknown action: " + name);
            }
            return artframework.api.ArtFramework.ops().dispatchEndTurn();
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(NativeTemplateRuntime.isEndTurnBound());
            addContract(m, "press", SignalNames.PRESSED);
            if (NativeTemplateRuntime.isEndTurnBound()) {
                m.put("buttonEnabled", Boolean.valueOf(NativeTemplateRuntime.isEndTurnEnabled()));
            }
            return m;
        }

    }
}
