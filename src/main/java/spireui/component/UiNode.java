package spireui.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable composition AST node. Stage / GL are separate.
 */
public final class UiNode {

    public final String type;
    public final String id;
    /** Template name when {@code type} is {@link UiTypes#REF}. */
    public final String ref;
    public final Map<String, Object> props;
    public final LayoutSpec layout;
    public final List<EffectDecl> effects;
    public final List<UiNode> children;
    public final Map<String, List<UiNode>> slots;

    private UiNode(
            String type,
            String id,
            String ref,
            Map<String, Object> props,
            LayoutSpec layout,
            List<EffectDecl> effects,
            List<UiNode> children,
            Map<String, List<UiNode>> slots) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type required");
        }
        this.type = type;
        this.id = id != null ? id : "";
        this.ref = ref != null ? ref : "";
        this.props = freezeProps(props);
        this.layout = layout != null ? layout : LayoutSpec.EMPTY;
        this.effects = freezeEffects(effects);
        this.children = freezeChildren(children);
        this.slots = freezeSlots(slots);
    }

    private static Map<String, Object> freezeProps(Map<String, Object> props) {
        if (props == null || props.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(props));
    }

    private static List<EffectDecl> freezeEffects(List<EffectDecl> effects) {
        if (effects == null || effects.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<EffectDecl>(effects));
    }

    private static List<UiNode> freezeChildren(List<UiNode> children) {
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<UiNode>(children));
    }

    private static Map<String, List<UiNode>> freezeSlots(Map<String, List<UiNode>> slots) {
        if (slots == null || slots.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<UiNode>> out = new LinkedHashMap<String, List<UiNode>>();
        for (Map.Entry<String, List<UiNode>> e : slots.entrySet()) {
            List<UiNode> list = e.getValue() != null
                    ? Collections.unmodifiableList(new ArrayList<UiNode>(e.getValue()))
                    : Collections.<UiNode>emptyList();
            out.put(e.getKey(), list);
        }
        return Collections.unmodifiableMap(out);
    }

    public String propString(String key, String defaultValue) {
        Object v = props.get(key);
        if (v == null) {
            return defaultValue;
        }
        return String.valueOf(v);
    }

    public float propFloat(String key, float defaultValue) {
        Object v = props.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        throw new IllegalArgumentException("prop " + key + " must be number");
    }

    public boolean propBool(String key, boolean defaultValue) {
        Object v = props.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        throw new IllegalArgumentException("prop " + key + " must be boolean");
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder of(String type) {
        return new Builder(type);
    }

    public static final class Builder {
        private String type;
        private String id = "";
        private String ref = "";
        private final Map<String, Object> props = new LinkedHashMap<String, Object>();
        private LayoutSpec layout = LayoutSpec.EMPTY;
        private final List<EffectDecl> effects = new ArrayList<EffectDecl>();
        private final List<UiNode> children = new ArrayList<UiNode>();
        private final Map<String, List<UiNode>> slots = new LinkedHashMap<String, List<UiNode>>();

        public Builder(String type) {
            this.type = type;
        }

        private Builder(UiNode src) {
            this.type = src.type;
            this.id = src.id;
            this.ref = src.ref;
            this.props.putAll(src.props);
            this.layout = src.layout;
            this.effects.addAll(src.effects);
            this.children.addAll(src.children);
            for (Map.Entry<String, List<UiNode>> e : src.slots.entrySet()) {
                this.slots.put(e.getKey(), new ArrayList<UiNode>(e.getValue()));
            }
        }

        public Builder id(String id) {
            this.id = id != null ? id : "";
            return this;
        }

        public Builder ref(String ref) {
            this.ref = ref != null ? ref : "";
            return this;
        }

        public Builder type(String type) {
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("type required");
            }
            this.type = type;
            return this;
        }

        public Builder prop(String key, Object value) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("prop key required");
            }
            if (value == null) {
                props.remove(key);
            } else {
                props.put(key, value);
            }
            return this;
        }

        public Builder props(Map<String, Object> more) {
            if (more != null) {
                props.putAll(more);
            }
            return this;
        }

        public Builder layout(LayoutSpec layout) {
            this.layout = layout != null ? layout : LayoutSpec.EMPTY;
            return this;
        }

        public Builder effect(EffectDecl effect) {
            if (effect != null) {
                effects.add(effect);
            }
            return this;
        }

        public Builder effects(List<EffectDecl> list) {
            if (list != null) {
                effects.addAll(list);
            }
            return this;
        }

        public Builder child(UiNode child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        public Builder children(List<UiNode> list) {
            if (list != null) {
                children.addAll(list);
            }
            return this;
        }

        public Builder clearChildren() {
            children.clear();
            return this;
        }

        public Builder slot(String name, List<UiNode> nodes) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("slot name required");
            }
            slots.put(name, nodes != null ? new ArrayList<UiNode>(nodes) : new ArrayList<UiNode>());
            return this;
        }

        public Builder slots(Map<String, List<UiNode>> more) {
            if (more != null) {
                for (Map.Entry<String, List<UiNode>> e : more.entrySet()) {
                    slot(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        public UiNode build() {
            return new UiNode(type, id, ref, props, layout, effects, children, slots);
        }
    }
}
