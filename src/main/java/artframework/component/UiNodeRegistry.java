package artframework.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Process registry of presentation node types (builtins + namespaced third-party).
 */
public final class UiNodeRegistry {

    private static final UiNodeRegistry GLOBAL = new UiNodeRegistry();

    static {
        GLOBAL.installBuiltins();
    }

    private final Map<String, UiNodeType> types = new LinkedHashMap<String, UiNodeType>();

    public UiNodeRegistry() {}

    public static UiNodeRegistry global() {
        return GLOBAL;
    }

    public void installBuiltins() {
        registerBuiltin(container(UiTypes.WINDOW));
        registerBuiltin(container(UiTypes.ROW));
        registerBuiltin(container(UiTypes.COL));
        registerBuiltin(container(UiTypes.STACK));
        registerBuiltin(container(UiTypes.PANEL));
        registerBuiltin(container(UiTypes.GLASS));
        registerBuiltin(container(UiTypes.FRAGMENT));
        registerBuiltin(container(UiTypes.SCROLL));
        registerBuiltin(container(UiTypes.CENTER));
        registerBuiltin(container(UiTypes.MARGIN));
        registerBuiltin(container(UiTypes.GRID));
        registerBuiltin(container(UiTypes.TABS));
        registerBuiltin(leaf(UiTypes.LABEL));
        registerBuiltin(leaf(UiTypes.BUTTON));
        registerBuiltin(leaf(UiTypes.SLIDER));
        registerBuiltin(leaf(UiTypes.HITAREA));
        registerBuiltin(leaf(UiTypes.TEXTFIELD));
        registerBuiltin(leaf(UiTypes.CHECKBOX));
        registerBuiltin(leaf(UiTypes.PROGRESS));
        registerBuiltin(
                UiNodeType.builder(UiTypes.REF)
                        .kind(NodeKind.COMPOSITION)
                        .allowsSlots(true)
                        .builtin(true)
                        .build());
        registerBuiltin(
                UiNodeType.builder(UiTypes.SLOT)
                        .kind(NodeKind.COMPOSITION)
                        .allowsChildren(true)
                        .builtin(true)
                        .build());
        registerBuiltin(
                UiNodeType.builder(ArtNodeTypes.ANIMATION_PLAYER)
                        .kind(NodeKind.BEHAVIOR)
                        .allowsChildren(false)
                        .builtin(true)
                        .defaultSignals(
                                java.util.Arrays.asList(
                                        artframework.core.AnimationPlayer.SIGNAL_STARTED,
                                        artframework.core.AnimationPlayer.SIGNAL_FINISHED,
                                        artframework.core.AnimationPlayer.SIGNAL_CANCELLED,
                                        artframework.core.AnimationPlayer.SIGNAL_PAUSED,
                                        artframework.core.AnimationPlayer.SIGNAL_RESUMED,
                                        artframework.core.AnimationPlayer.SIGNAL_LOOPED,
                                        artframework.core.SignalNames.STATE_CHANGED))
                        .build());
        registerBuiltin(
                UiNodeType.builder(ArtNodeTypes.SHADER_EFFECT)
                        .kind(NodeKind.VISUAL)
                        .allowsChildren(true)
                        .builtin(true)
                        .defaultSignals(
                                java.util.Arrays.asList(
                                        artframework.core.SignalNames.EFFECT_READY,
                                        artframework.core.SignalNames.EFFECT_FAILED))
                        .build());
        registerBuiltin(
                UiNodeType.builder(ArtNodeTypes.SKELETON)
                        .kind(NodeKind.VISUAL)
                        .allowsChildren(false)
                        .builtin(true)
                        .defaultSignals(
                                java.util.Arrays.asList(
                                        artframework.core.SignalNames.SKELETON_EVENT,
                                        artframework.core.SignalNames.ASSET_FAILED))
                        .build());
        registerBuiltin(
                UiNodeType.builder(ArtNodeTypes.PRESENT_PROFILE)
                        .kind(NodeKind.BEHAVIOR)
                        .allowsChildren(true)
                        .builtin(true)
                        .build());
        registerStsBuiltin(ArtNodeTypes.STS_BUTTON, NodeKind.LEAF, false,
                java.util.Arrays.asList(artframework.core.SignalNames.PRESSED));
        registerStsBuiltin(ArtNodeTypes.STS_PANEL, NodeKind.CONTAINER, true,
                java.util.Collections.<String>emptyList());
        registerStsBuiltin(ArtNodeTypes.STS_CARD, NodeKind.VISUAL, false,
                java.util.Arrays.asList(artframework.core.SignalNames.PRESSED));
        registerStsBuiltin(ArtNodeTypes.STS_ENERGY_ORB, NodeKind.VISUAL, false,
                java.util.Collections.<String>emptyList());
        registerStsBuiltin(ArtNodeTypes.STS_INTENT, NodeKind.VISUAL, false,
                java.util.Collections.<String>emptyList());
        registerStsBuiltin(ArtNodeTypes.STS_TOP_PANEL, NodeKind.VISUAL, false,
                java.util.Collections.<String>emptyList());
        registerStsBuiltin(ArtNodeTypes.STS_MAP, NodeKind.CONTAINER, true,
                java.util.Collections.<String>emptyList());
        registerStsBuiltin(ArtNodeTypes.STS_MAP_NODE, NodeKind.VISUAL, false,
                java.util.Arrays.asList(artframework.core.SignalNames.PRESSED));
        registerStsBuiltin(ArtNodeTypes.STS_EVENT_OPTION, NodeKind.LEAF, false,
                java.util.Arrays.asList(artframework.core.SignalNames.PRESSED));
        registerStsBuiltin(ArtNodeTypes.STS_REWARD_ITEM, NodeKind.LEAF, false,
                java.util.Arrays.asList(artframework.core.SignalNames.PRESSED));
        registerStsBuiltin(ArtNodeTypes.STS_ROOM_ACTION, NodeKind.LEAF, false,
                java.util.Arrays.asList(artframework.core.SignalNames.PRESSED));
    }

    private void registerStsBuiltin(
            String type, NodeKind kind, boolean children, java.util.List<String> signals) {
        registerBuiltin(UiNodeType.builder(type)
                .kind(kind)
                .allowsChildren(children)
                .builtin(true)
                .defaultSignals(signals)
                .build());
    }

    private static UiNodeType container(String type) {
        return UiNodeType.builder(type)
                .kind(NodeKind.CONTAINER)
                .allowsChildren(true)
                .builtin(true)
                .defaultSignals(UiTypes.defaultSignals(type))
                .build();
    }

    private static UiNodeType leaf(String type) {
        return UiNodeType.builder(type)
                .kind(NodeKind.LEAF)
                .allowsChildren(false)
                .builtin(true)
                .defaultSignals(UiTypes.defaultSignals(type))
                .build();
    }

    private void registerBuiltin(UiNodeType t) {
        types.put(t.type(), t);
    }

    public void register(UiNodeType type) {
        if (type == null) {
            throw new IllegalArgumentException("type required");
        }
        String id = type.type();
        UiNodeType existing = types.get(id);
        if (existing != null && existing.isBuiltin()) {
            throw new IllegalArgumentException("cannot overwrite builtin type: " + id);
        }
        if (!type.isBuiltin() && !isValidThirdPartyType(id)) {
            throw new IllegalArgumentException(
                    "third-party type requires namespace (e.g. my_mod.name): " + id);
        }
        types.put(id, type);
    }

    public void unregister(String type) {
        if (type == null) {
            return;
        }
        UiNodeType existing = types.get(type);
        if (existing != null && existing.isBuiltin()) {
            throw new IllegalArgumentException("cannot unregister builtin type: " + type);
        }
        types.remove(type);
    }

    public UiNodeType get(String type) {
        return type == null ? null : types.get(type);
    }

    public boolean contains(String type) {
        return type != null && types.containsKey(type);
    }

    public Set<String> typeNames() {
        return Collections.unmodifiableSet(types.keySet());
    }

    public void validate(UiNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node required");
        }
        UiNodeType def = types.get(node.type);
        if (def == null) {
            throw new IllegalArgumentException("unknown layout type: " + node.type);
        }
        if (!def.allowsChildren() && !node.children.isEmpty()) {
            throw new IllegalArgumentException(
                    "type " + node.type + " does not allow children");
        }
        if (!def.allowsSlots() && !node.slots.isEmpty()) {
            throw new IllegalArgumentException("type " + node.type + " does not allow slots");
        }
        for (UiNode c : node.children) {
            validate(c);
        }
        for (java.util.List<UiNode> list : node.slots.values()) {
            for (UiNode c : list) {
                validate(c);
            }
        }
    }

    public static boolean isValidThirdPartyType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        int dot = type.indexOf('.');
        if (dot <= 0 || dot >= type.length() - 1) {
            return false;
        }
        if (type.indexOf('.') != type.lastIndexOf('.')) {
            // allow my_mod.sub.name — only require at least one dot with non-empty sides
            if (type.startsWith(".") || type.endsWith(".")) {
                return false;
            }
        }
        return true;
    }

    /** Test helper: drop third-party entries and reinstall builtins. */
    public void resetBuiltinsForTests() {
        types.clear();
        installBuiltins();
    }
}
