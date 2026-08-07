package artframework.c1;

import artframework.component.ArtNodeTypes;
import artframework.component.UiTypes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of C1 scene2d node factories (builtins + third-party).
 */
public final class C1NodeFactories {

    private static final C1NodeFactories GLOBAL = new C1NodeFactories();

    static {
        GLOBAL.installBuiltins();
    }

    private final Map<String, C1NodeFactory> factories = new LinkedHashMap<String, C1NodeFactory>();

    public C1NodeFactories() {}

    public static C1NodeFactories global() {
        return GLOBAL;
    }

    public void installBuiltins() {
        BuiltinC1Factories.registerAll(this);
    }

    public void register(C1NodeFactory factory) {
        if (factory == null || factory.type() == null || factory.type().isEmpty()) {
            throw new IllegalArgumentException("factory required");
        }
        factories.put(factory.type(), factory);
    }

    public void unregister(String type) {
        if (type == null) {
            return;
        }
        if (isBuiltinType(type)) {
            throw new IllegalArgumentException("cannot unregister builtin C1 factory: " + type);
        }
        factories.remove(type);
    }

    public C1NodeFactory get(String type) {
        return type == null ? null : factories.get(type);
    }

    public boolean contains(String type) {
        return type != null && factories.containsKey(type);
    }

    public Set<String> typeNames() {
        return Collections.unmodifiableSet(factories.keySet());
    }

    public void resetBuiltinsForTests() {
        factories.clear();
        installBuiltins();
    }

    private static boolean isBuiltinType(String type) {
        return UiTypes.isContainer(type)
                || UiTypes.isLeaf(type)
                || UiTypes.WINDOW.equals(type)
                || ArtNodeTypes.ANIMATION_PLAYER.equals(type)
                || ArtNodeTypes.SHADER_EFFECT.equals(type)
                || ArtNodeTypes.SKELETON.equals(type)
                || ArtNodeTypes.PRESENT_PROFILE.equals(type)
                || ArtNodeTypes.STS_BUTTON.equals(type)
                || ArtNodeTypes.STS_PANEL.equals(type)
                || ArtNodeTypes.STS_CARD.equals(type)
                || ArtNodeTypes.STS_ENERGY_ORB.equals(type)
                || ArtNodeTypes.STS_INTENT.equals(type)
                || ArtNodeTypes.STS_TOP_PANEL.equals(type)
                || ArtNodeTypes.STS_MAP.equals(type)
                || ArtNodeTypes.STS_MAP_NODE.equals(type)
                || ArtNodeTypes.STS_EVENT_OPTION.equals(type)
                || ArtNodeTypes.STS_REWARD_ITEM.equals(type)
                || ArtNodeTypes.STS_ROOM_ACTION.equals(type);
    }
}
