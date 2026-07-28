package artframework.inspect;

import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.c1.SyntheticComponents;
import artframework.c2.MapNodeRef;
import artframework.c2.NativeComponents;
import artframework.component.Rect;
import artframework.context.PresentSurfaces;
import artframework.core.SignalHub;
import artframework.core.UiComponent;
import artframework.core.UiInstance;
import artframework.core.UiTree;
import artframework.core.UiTrees;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure lab inspect over mounted C1 trees and UiComponents (no BaseMod / AbstractDungeon).
 */
public final class UiInspect {

    public static final String LOG_PREFIX = "ART_UI ";
    public static final int DEFAULT_TREE_DEPTH = 6;

    private UiInspect() {}

    public static Map<String, Object> listSurfaces() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("windows", new ArrayList<String>(ArtFramework.listOpenIds()));
        List<Map<String, Object>> components = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> slice : SyntheticComponents.probeAll()) {
            components.add(summaryFromSlice(slice));
        }
        for (String id : NativeComponents.ids()) {
            UiComponent c = NativeComponents.get(id);
            if (c != null) {
                components.add(componentSummary(c));
            }
        }
        for (String id : PresentSurfaces.ids()) {
            UiComponent c = PresentSurfaces.get(id);
            if (c != null) {
                components.add(componentSummary(c));
            }
        }
        out.put("components", components);
        return out;
    }

    public static Map<String, Object> treeMap(String windowId, int maxDepth) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (windowId == null || windowId.isEmpty()) {
            out.put("error", "windowId required");
            return out;
        }
        UiTree tree = UiTrees.get(windowId);
        if (tree == null || !tree.isAlive()) {
            out.put("error", "window not open: " + windowId);
            return out;
        }
        int depth = maxDepth > 0 ? maxDepth : DEFAULT_TREE_DEPTH;
        out.put("windowId", windowId);
        out.put("depth", Integer.valueOf(depth));
        out.put("root", instanceTree(tree.root(), tree.signalHub(), 0, depth));
        return out;
    }

    public static List<String> treeLines(String windowId, int maxDepth) {
        List<String> lines = new ArrayList<String>();
        Map<String, Object> map = treeMap(windowId, maxDepth);
        if (map.containsKey("error")) {
            lines.add(String.valueOf(map.get("error")));
            return lines;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) map.get("root");
        if (root != null) {
            appendTreeLines(lines, root, 0);
        }
        return lines;
    }

    public static Map<String, Object> nodeMap(String windowId, String pathOrId) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (windowId == null || windowId.isEmpty()) {
            out.put("error", "windowId required");
            return out;
        }
        if (pathOrId == null || pathOrId.isEmpty()) {
            out.put("error", "id or path required");
            return out;
        }
        UiTree tree = UiTrees.get(windowId);
        if (tree == null || !tree.isAlive()) {
            out.put("error", "window not open: " + windowId);
            return out;
        }
        UiInstance inst = tree.find(pathOrId);
        if (inst == null) {
            out.put("error", "not found: " + pathOrId);
            return out;
        }
        out.put("windowId", windowId);
        out.put("node", instanceDetail(inst, tree.signalHub()));
        return out;
    }

    /**
     * Emit on C1 control ({@code windowId/controlId} or separate window + control) or component id.
     */
    public static UiOpResult emit(String target, String signal, Object... args) {
        if (target == null || target.isEmpty()) {
            return UiOpResult.unavailable("target required");
        }
        if (signal == null || signal.isEmpty()) {
            return UiOpResult.unavailable("signal required");
        }
        Object[] payload = args != null ? args : new Object[0];
        try {
            EmitTarget et = resolveEmitTarget(target);
            if (et.kind == EmitKind.C1) {
                UiTree tree = UiTrees.get(et.windowId);
                if (tree == null || !tree.isAlive()) {
                    return UiOpResult.notBound("window not open: " + et.windowId);
                }
                UiInstance inst = tree.find(et.controlId);
                if (inst == null) {
                    return UiOpResult.notBound("control not found: " + et.controlId);
                }
                tree.emit(et.controlId, signal, payload);
                return UiOpResult.ok("emitted " + et.windowId + "/" + et.controlId + " " + signal);
            }
            UiComponent c = ArtFramework.component(et.componentId);
            if (c == null) {
                return UiOpResult.unavailable("unknown component: " + et.componentId);
            }
            c.emit(signal, payload);
            return UiOpResult.ok("emitted " + c.id() + " " + signal);
        } catch (IllegalArgumentException e) {
            return UiOpResult.unavailable(e.getMessage() != null ? e.getMessage() : "emit failed");
        } catch (RuntimeException e) {
            return UiOpResult.unavailable(
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    public static UiOpResult emit(String windowId, String controlId, String signal, Object... args) {
        if (windowId == null || windowId.isEmpty()) {
            return UiOpResult.unavailable("windowId required");
        }
        if (controlId == null || controlId.isEmpty()) {
            return emit(windowId, signal, args);
        }
        return emit(windowId + "/" + controlId, signal, args);
    }

    public static UiOpResult invoke(String componentId, String action, Object... args) {
        return ArtFramework.ops().invoke(componentId, action, args);
    }

    /** Parse console arg tokens into typed payload objects. */
    public static Object[] parseArgs(String[] tokens, int from) {
        if (tokens == null || from >= tokens.length) {
            return new Object[0];
        }
        List<Object> out = new ArrayList<Object>();
        for (int i = from; i < tokens.length; i++) {
            out.add(parseArg(tokens[i]));
        }
        return out.toArray();
    }

    public static Object parseArg(String token) {
        if (token == null) {
            return "";
        }
        if ("true".equalsIgnoreCase(token)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(token)) {
            return Boolean.FALSE;
        }
        if (token.indexOf(',') >= 0) {
            String[] parts = token.split(",", 3);
            if (parts.length >= 2) {
                try {
                    int row = Integer.parseInt(parts[0].trim());
                    int col = Integer.parseInt(parts[1].trim());
                    String room = parts.length > 2 ? parts[2].trim() : "";
                    return new MapNodeRef(row, col, room);
                } catch (NumberFormatException ignored) {
                    // fall through to string
                }
            }
        }
        try {
            if (token.indexOf('.') >= 0) {
                return Float.valueOf(Float.parseFloat(token));
            }
            return Integer.valueOf(Integer.parseInt(token));
        } catch (NumberFormatException e) {
            return token;
        }
    }

    public static String toJson(Object value) {
        return InspectJson.toJson(value);
    }

    private static Map<String, Object> summaryFromSlice(Map<String, Object> slice) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", slice.get("id"));
        m.put("kind", slice.get("kind"));
        m.put("mounted", slice.get("mounted"));
        return m;
    }

    private static Map<String, Object> componentSummary(UiComponent c) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", c.id());
        m.put("kind", c.kind().name());
        m.put("mounted", Boolean.valueOf(c.isMounted()));
        return m;
    }

    private static Map<String, Object> instanceTree(
            UiInstance inst, SignalHub hub, int depth, int maxDepth) {
        Map<String, Object> m = instanceBrief(inst, hub);
        if (depth >= maxDepth) {
            m.put("truncated", Boolean.TRUE);
            return m;
        }
        List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
        for (UiInstance child : inst.children()) {
            children.add(instanceTree(child, hub, depth + 1, maxDepth));
        }
        if (!children.isEmpty()) {
            m.put("children", children);
        }
        return m;
    }

    private static Map<String, Object> instanceBrief(UiInstance inst, SignalHub hub) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        String id = inst.id();
        m.put("id", id.isEmpty() ? inst.propString("id", "") : id);
        if (id.isEmpty()) {
            m.put("anon", Boolean.TRUE);
        }
        m.put("type", inst.type());
        List<String> signals = inst.signals();
        if (signals != null && !signals.isEmpty()) {
            m.put("signals", new ArrayList<String>(signals));
            if (!id.isEmpty()) {
                Map<String, Object> counts = new LinkedHashMap<String, Object>();
                for (String s : signals) {
                    counts.put(s, Integer.valueOf(hub.handlerCount(id, s)));
                }
                m.put("handlerCounts", counts);
            }
        }
        return m;
    }

    private static Map<String, Object> instanceDetail(UiInstance inst, SignalHub hub) {
        Map<String, Object> m = instanceBrief(inst, hub);
        m.put("mounted", Boolean.valueOf(inst.isMounted()));
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> e : inst.propsView().entrySet()) {
            Object v = e.getValue();
            if (v == null || v instanceof String || v instanceof Number || v instanceof Boolean) {
                props.put(e.getKey(), v);
            } else {
                props.put(e.getKey(), String.valueOf(v));
            }
        }
        m.put("props", props);
        Rect r = inst.rect();
        if (r != null) {
            Map<String, Object> rect = new LinkedHashMap<String, Object>();
            rect.put("x", Float.valueOf(r.x));
            rect.put("y", Float.valueOf(r.y));
            rect.put("w", Float.valueOf(r.width));
            rect.put("h", Float.valueOf(r.height));
            m.put("rect", rect);
        }
        List<String> childIds = new ArrayList<String>();
        for (UiInstance c : inst.children()) {
            childIds.add(c.id().isEmpty() ? ("@" + c.type()) : c.id());
        }
        m.put("childIds", childIds);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static void appendTreeLines(List<String> lines, Map<String, Object> node, int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        Object id = node.get("id");
        Object type = node.get("type");
        sb.append(id != null && !String.valueOf(id).isEmpty() ? id : "(anon)");
        sb.append(' ').append(type != null ? type : "?");
        Object signals = node.get("signals");
        if (signals instanceof List && !((List<?>) signals).isEmpty()) {
            sb.append(" [");
            List<?> list = (List<?>) signals;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(list.get(i));
            }
            sb.append(']');
        }
        if (Boolean.TRUE.equals(node.get("truncated"))) {
            sb.append(" …");
        }
        lines.add(sb.toString());
        Object children = node.get("children");
        if (children instanceof List) {
            for (Object child : (List<Object>) children) {
                if (child instanceof Map) {
                    appendTreeLines(lines, (Map<String, Object>) child, indent + 1);
                }
            }
        }
    }

    private enum EmitKind {
        C1,
        COMPONENT
    }

    private static final class EmitTarget {
        final EmitKind kind;
        final String windowId;
        final String controlId;
        final String componentId;

        EmitTarget(String windowId, String controlId) {
            this.kind = EmitKind.C1;
            this.windowId = windowId;
            this.controlId = controlId;
            this.componentId = null;
        }

        EmitTarget(String componentId) {
            this.kind = EmitKind.COMPONENT;
            this.windowId = null;
            this.controlId = null;
            this.componentId = componentId;
        }
    }

    private static EmitTarget resolveEmitTarget(String target) {
        int slash = target.indexOf('/');
        if (slash > 0 && slash < target.length() - 1) {
            return new EmitTarget(target.substring(0, slash), target.substring(slash + 1));
        }
        if (UiTrees.get(target) != null) {
            UiTree tree = UiTrees.get(target);
            String rootId =
                    tree.root() != null && !tree.root().id().isEmpty()
                            ? tree.root().id()
                            : target;
            return new EmitTarget(target, rootId);
        }
        return new EmitTarget(target);
    }
}
