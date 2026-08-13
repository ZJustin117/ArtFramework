package artframework.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.presentation.NodeIdentityComponent;
import artframework.ecs.EntityId;

/**
 * Attaches optional {@link NodeStateMachine} instances from node {@code states} decl.
 */
public final class NodeStateMachines {

    private static final Map<String, NodeStateMachine> BY_KEY =
            new LinkedHashMap<String, NodeStateMachine>();

    private NodeStateMachines() {}

    /** Rebuild state-machine host subscriptions from ECS entity declarations. */
    public static void syncContext(PresentationContext context) {
        if (context == null) return;
        clearWindow(PresentationRuntime.windowId(context));
        for (EntityId entity : context.entities()) {
            NodeStateMachine fsm = NodeStateMachine.fromDecl(context, entity);
            NodeIdentityComponent identity = PresentationRuntime.identity(context, entity);
            if (fsm != null && identity != null && !identity.name.isEmpty()) {
                BY_KEY.put(PresentationRuntime.windowId(context) + "/" + identity.name, fsm);
                fsm.wire(context);
            }
        }
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

}
