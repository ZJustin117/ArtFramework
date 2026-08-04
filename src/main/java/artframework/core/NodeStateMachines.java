package artframework.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Attaches optional {@link NodeStateMachine} instances from node {@code states} decl.
 */
public final class NodeStateMachines {

    private static final Map<String, NodeStateMachine> BY_KEY =
            new LinkedHashMap<String, NodeStateMachine>();

    private NodeStateMachines() {}

    public static void syncTree(UiTree tree) {
        if (tree == null) {
            return;
        }
        clearWindow(tree.windowId());
        walk(tree, tree.root());
    }

    public static NodeStateMachine get(String windowId, String nodeId) {
        if (windowId == null || nodeId == null) {
            return null;
        }
        return BY_KEY.get(windowId + "/" + nodeId);
    }

    public static void clearWindow(String windowId) {
        if (windowId == null) {
            return;
        }
        String prefix = windowId + "/";
        List<String> remove = new ArrayList<String>();
        for (Map.Entry<String, NodeStateMachine> e : BY_KEY.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                e.getValue().clearSubscriptions();
                remove.add(e.getKey());
            }
        }
        for (String k : remove) {
            BY_KEY.remove(k);
        }
    }

    public static void resetForTests() {
        for (NodeStateMachine m : BY_KEY.values()) {
            m.clearSubscriptions();
        }
        BY_KEY.clear();
    }

    private static void walk(UiTree tree, UiInstance inst) {
        if (inst == null) {
            return;
        }
        if (!inst.id().isEmpty() && inst.prop("states") != null) {
            NodeStateMachine fsm = NodeStateMachine.fromDecl(inst);
            if (fsm != null) {
                BY_KEY.put(tree.windowId() + "/" + inst.id(), fsm);
                fsm.wire(tree);
            }
        }
        for (UiInstance c : inst.children()) {
            walk(tree, c);
        }
    }
}
