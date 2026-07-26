package spireui.c1.layout;

import java.util.Collections;
import java.util.List;

/**
 * Immutable layout AST node (C1 DSL). Stage rendering is separate.
 */
public final class LayoutNode {

    public enum Type {
        WINDOW,
        LABEL,
        BUTTON
    }

    public final Type type;
    public final String id;
    public final String title;
    public final String text;
    public final float width;
    public final float height;
    public final List<LayoutNode> children;

    private LayoutNode(
            Type type,
            String id,
            String title,
            String text,
            float width,
            float height,
            List<LayoutNode> children) {
        this.type = type;
        this.id = id != null ? id : "";
        this.title = title != null ? title : "";
        this.text = text != null ? text : "";
        this.width = width;
        this.height = height;
        this.children = children != null
                ? Collections.unmodifiableList(children)
                : Collections.<LayoutNode>emptyList();
    }

    public static LayoutNode window(
            String id, String title, float width, float height, List<LayoutNode> children) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("window title required");
        }
        return new LayoutNode(Type.WINDOW, id, title, null, width, height, children);
    }

    public static LayoutNode label(String text) {
        return new LayoutNode(Type.LABEL, null, null, text != null ? text : "", 0f, 0f, null);
    }

    public static LayoutNode button(String id, String text) {
        return new LayoutNode(Type.BUTTON, id, null, text != null ? text : "", 0f, 0f, null);
    }
}
