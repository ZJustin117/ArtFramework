package artframework.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable registration descriptor for a presentation node type.
 */
public final class UiNodeType {

    private final String type;
    private final NodeKind kind;
    private final boolean allowsChildren;
    private final boolean allowsSlots;
    private final boolean builtin;
    private final List<String> defaultSignals;

    private UiNodeType(
            String type,
            NodeKind kind,
            boolean allowsChildren,
            boolean allowsSlots,
            boolean builtin,
            List<String> defaultSignals) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type required");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind required");
        }
        this.type = type;
        this.kind = kind;
        this.allowsChildren = allowsChildren;
        this.allowsSlots = allowsSlots;
        this.builtin = builtin;
        if (defaultSignals == null || defaultSignals.isEmpty()) {
            this.defaultSignals = Collections.emptyList();
        } else {
            this.defaultSignals =
                    Collections.unmodifiableList(new ArrayList<String>(defaultSignals));
        }
    }

    public String type() {
        return type;
    }

    public NodeKind kind() {
        return kind;
    }

    public boolean allowsChildren() {
        return allowsChildren;
    }

    public boolean allowsSlots() {
        return allowsSlots;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public List<String> defaultSignals() {
        return defaultSignals;
    }

    public static Builder builder(String type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final String type;
        private NodeKind kind = NodeKind.LEAF;
        private boolean allowsChildren;
        private boolean allowsSlots;
        private boolean builtin;
        private List<String> defaultSignals = Collections.emptyList();

        public Builder(String type) {
            this.type = type;
        }

        public Builder kind(NodeKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder allowsChildren(boolean v) {
            this.allowsChildren = v;
            return this;
        }

        public Builder allowsSlots(boolean v) {
            this.allowsSlots = v;
            return this;
        }

        public Builder builtin(boolean v) {
            this.builtin = v;
            return this;
        }

        public Builder defaultSignals(List<String> signals) {
            this.defaultSignals = signals != null ? signals : Collections.<String>emptyList();
            return this;
        }

        public UiNodeType build() {
            boolean children = allowsChildren;
            boolean slots = allowsSlots;
            if (kind == NodeKind.CONTAINER) {
                children = true;
            }
            if (kind == NodeKind.COMPOSITION && UiTypes.REF.equals(type)) {
                slots = true;
            }
            return new UiNodeType(type, kind, children, slots, builtin, defaultSignals);
        }
    }
}
