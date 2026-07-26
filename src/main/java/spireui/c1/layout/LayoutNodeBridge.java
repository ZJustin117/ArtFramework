package spireui.c1.layout;

import spireui.component.UiNode;
import spireui.component.UiTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a legacy-shaped composition tree (window + label/button only) to {@link LayoutNode}
 * for Stage attach. Returns null if the tree uses advanced types.
 */
public final class LayoutNodeBridge {

    private LayoutNodeBridge() {}

    public static LayoutNode toLegacyOrNull(UiNode root) {
        if (root == null || !UiTypes.WINDOW.equals(root.type)) {
            return null;
        }
        if (!isLegacyTree(root)) {
            return null;
        }
        return toLegacy(root);
    }

    public static boolean isLegacyTree(UiNode node) {
        if (node == null) {
            return false;
        }
        if (UiTypes.WINDOW.equals(node.type)
                || UiTypes.LABEL.equals(node.type)
                || UiTypes.BUTTON.equals(node.type)) {
            for (UiNode c : node.children) {
                if (!isLegacyTree(c)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static LayoutNode toLegacy(UiNode node) {
        if (UiTypes.WINDOW.equals(node.type)) {
            String title = node.propString("title", "");
            List<LayoutNode> children = new ArrayList<LayoutNode>();
            for (UiNode c : node.children) {
                children.add(toLegacy(c));
            }
            return LayoutNode.window(
                    node.id,
                    title,
                    node.layout.width,
                    node.layout.height,
                    children);
        }
        if (UiTypes.LABEL.equals(node.type)) {
            return LayoutNode.label(node.propString("text", ""));
        }
        if (UiTypes.BUTTON.equals(node.type)) {
            return LayoutNode.button(node.id, node.propString("text", ""));
        }
        throw new IllegalArgumentException("not legacy type: " + node.type);
    }
}
