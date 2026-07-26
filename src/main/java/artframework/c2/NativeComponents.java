package artframework.c2;

import artframework.api.UiOpResult;
import artframework.core.ComponentKind;
import artframework.core.SignalHandler;
import artframework.core.SignalHub;
import artframework.core.SignalNames;
import artframework.core.UiComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        return BY_ID.get(id);
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

    public static void resetForTests() {
        HUB.clear();
        for (UiComponent c : BY_ID.values()) {
            if (c.isMounted()) {
                c.unmount();
            }
        }
    }

    public static void syncMountFromRuntime() {
        for (UiComponent c : BY_ID.values()) {
            boolean should = isTemplateActive(c.id());
            if (should && !c.isMounted()) {
                c.mount();
            } else if (!should && c.isMounted()) {
                c.unmount();
            }
        }
    }

    private static boolean isTemplateActive(String id) {
        if (NativeTemplateIds.MAP.equals(id)) {
            return NativeTemplateRuntime.isMapBound();
        }
        if (NativeTemplateIds.EVENT.equals(id)) {
            return NativeTemplateRuntime.isEventBound();
        }
        if (NativeTemplateIds.SELECT_GRID.equals(id)) {
            return NativeTemplateRuntime.isSelectGridBound();
        }
        if (NativeTemplateIds.SELECT_HAND.equals(id)) {
            return NativeTemplateRuntime.isSelectHandBound();
        }
        if (NativeTemplateIds.END_TURN.equals(id)) {
            return NativeTemplateRuntime.isEndTurnBound();
        }
        return false;
    }

    abstract static class BaseNative implements UiComponent {
        private final String id;
        private boolean mounted;

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
            return mounted;
        }

        @Override
        public void mount() {
            mounted = true;
        }

        @Override
        public void unmount() {
            mounted = false;
            HUB.clearInstance(id);
        }

        @Override
        public void connect(String signal, SignalHandler handler) {
            HUB.connect(id, signal, handler);
        }

        @Override
        public void disconnect(String signal, SignalHandler handler) {
            HUB.disconnect(id, signal, handler);
        }

        @Override
        public void emit(String signal, Object... args) {
            HUB.emit(id, signal, args);
        }

        Map<String, Object> baseProbe(boolean bound) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("kind", kind().name());
            m.put("mounted", Boolean.valueOf(mounted));
            m.put("bound", Boolean.valueOf(bound));
            return m;
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
            return artframework.api.ArtFramework.ops().clickMapNode((MapNodeRef) args[0]);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(NativeTemplateRuntime.isMapBound());
            if (NativeTemplateRuntime.isMapBound()) {
                m.put("pinCount", Integer.valueOf(NativeTemplateRuntime.map().listPins().size()));
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
            return artframework.api.ArtFramework.ops().chooseEventOption(index, label);
        }

        @Override
        public Map<String, Object> probeSlice() {
            return baseProbe(NativeTemplateRuntime.isEventBound());
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
                return artframework.api.ArtFramework.ops().selectCard(kind, cardId, index);
            }
            if ("confirm".equals(name) || "confirmSelect".equals(name)) {
                return artframework.api.ArtFramework.ops().confirmSelect(kind);
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
            return artframework.api.ArtFramework.ops().pressEndTurn();
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> m = baseProbe(NativeTemplateRuntime.isEndTurnBound());
            if (NativeTemplateRuntime.isEndTurnBound()) {
                m.put("buttonEnabled", Boolean.valueOf(NativeTemplateRuntime.endTurn().isButtonEnabled()));
            }
            return m;
        }
    }
}
