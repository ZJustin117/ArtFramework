package artframework.c1;

import artframework.api.UiOpResult;
import artframework.api.UiOps;
import artframework.core.ComponentKind;
import artframework.core.SignalHandler;
import artframework.core.UiComponent;
import artframework.presentation.Node;
import artframework.presentation.NodeTree;
import artframework.presentation.NodeTrees;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mounted C1 windows exposed through the same component surface as native templates. */
public final class SyntheticComponents {

    private SyntheticComponents() {}

    public static void mount(String windowId) {
        // NodeTree lifecycle is authoritative; this facade has no registry.
    }

    public static void unmount(String windowId) {
        // NodeTree lifecycle is authoritative; this facade has no registry.
    }

    public static UiComponent get(String windowId) {
        return windowId != null && NodeTrees.isOpen(windowId)
                ? new SyntheticComponent(windowId) : null;
    }

    public static List<Map<String, Object>> probeAll() {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (String windowId : NodeTrees.listOpenIds()) {
            out.add(new SyntheticComponent(windowId).probeSlice());
        }
        return out;
    }

    public static void resetForTests() {
        // NodeTrees owns tree lifecycle and test cleanup.
    }

    private static final class SyntheticComponent implements UiComponent {
        private final String id;

        SyntheticComponent(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public ComponentKind kind() {
            return ComponentKind.SYNTHETIC;
        }

        @Override
        public boolean isMounted() {
            return NodeTrees.isOpen(id);
        }

        @Override
        public void mount() {
            // SyntheticRuntime owns tree creation; this object only exposes an active tree.
        }

        @Override
        public void unmount() {
            SyntheticComponents.unmount(id);
        }

        @Override
        public void connect(String signal, SignalHandler handler) {
            tree().connect(rootId(), signal, handler);
        }

        @Override
        public void disconnect(String signal, SignalHandler handler) {
            NodeTree tree = NodeTrees.get(id);
            if (tree != null) {
                // NodeTree owns subscription cleanup; the facade keeps no second listener store.
            }
        }

        @Override
        public void emit(String signal, Object... args) {
            tree().emit(rootId(), signal, args);
        }

        @Override
        public UiOpResult action(String name, Object... args) {
            if (!isMounted()) {
                return UiOpResult.notBound("synthetic window not open: " + id);
            }
            if ("click_button".equals(name) && stringArg(args, 0) != null) {
                return UiOpsResult.clickButton(id, stringArg(args, 0));
            }
            if ("set_slider".equals(name) && stringArg(args, 0) != null && numberArg(args, 1) != null) {
                return UiOpsResult.setSlider(id, stringArg(args, 0), numberArg(args, 1).floatValue());
            }
            if ("set_text".equals(name) && stringArg(args, 0) != null) {
                return UiOpsResult.setText(id, stringArg(args, 0), stringArg(args, 1));
            }
            if ("toggle_checkbox".equals(name) && stringArg(args, 0) != null) {
                return UiOpsResult.toggleCheckbox(id, stringArg(args, 0));
            }
            return UiOpResult.unavailable("unknown synthetic action: " + name);
        }

        @Override
        public Map<String, Object> probeSlice() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", id);
            out.put("kind", kind().name());
            out.put("mounted", Boolean.valueOf(isMounted()));
            out.put("controls", tree().probeControls());
            return out;
        }

        private NodeTree tree() {
            NodeTree tree = NodeTrees.get(id);
            if (tree == null) {
                throw new IllegalStateException("synthetic window not open: " + id);
            }
            return tree;
        }

        private String rootId() {
            NodeTree tree = tree();
            return tree.root() != null ? tree.root().name() : id;
        }

        private static String stringArg(Object[] args, int index) {
            return args != null && args.length > index && args[index] != null
                    ? String.valueOf(args[index]) : null;
        }

        private static Number numberArg(Object[] args, int index) {
            return args != null && args.length > index && args[index] instanceof Number
                    ? (Number) args[index] : null;
        }
    }


    private static final class UiOpsResult {
        private UiOpsResult() {}

        static UiOpResult clickButton(String windowId, String buttonId) {
            return artframework.api.ArtFramework.ops().clickButton(windowId, buttonId);
        }

        static UiOpResult setSlider(String windowId, String sliderId, float value) {
            return artframework.api.ArtFramework.ops().setSlider(windowId, sliderId, value);
        }

        static UiOpResult setText(String windowId, String fieldId, String text) {
            return artframework.api.ArtFramework.ops().setText(windowId, fieldId, text);
        }

        static UiOpResult toggleCheckbox(String windowId, String checkboxId) {
            return artframework.api.ArtFramework.ops().toggleCheckbox(windowId, checkboxId);
        }
    }
}
