package artframework.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure box layout for composition trees (Nest). Ignores effects.
 * Supports min size, size flags (expand), stretch ratio, cross-axis align.
 */
public final class LayoutEngine {

    public static final float DEFAULT_LEAF_WIDTH = 120f;
    public static final float DEFAULT_LEAF_HEIGHT = 32f;
    public static final float DEFAULT_WINDOW_WIDTH = 400f;
    public static final float DEFAULT_WINDOW_HEIGHT = 240f;

    private LayoutEngine() {}

    public static LayoutResult layout(UiNode root) {
        if (root == null) {
            throw new IllegalArgumentException("root required");
        }
        if (UiTypes.REF.equals(root.type) || UiTypes.SLOT.equals(root.type)) {
            throw new IllegalArgumentException("expand ref/slot before layout");
        }
        Map<String, Rect> byId = new LinkedHashMap<String, Rect>();
        float rw = preferredWidth(root);
        float rh = preferredHeight(root);
        Rect bounds = new Rect(0f, 0f, rw, rh);
        place(root, bounds, byId);
        return new LayoutResult(root, bounds, byId);
    }

    private static void place(UiNode node, Rect bounds, Map<String, Rect> byId) {
        if (!node.id.isEmpty()) {
            byId.put(node.id, bounds);
        }
        if (UiTypes.FRAGMENT.equals(node.type)) {
            placeFragment(node, bounds, byId);
            return;
        }
        if (UiTypes.ROW.equals(node.type)) {
            placeRow(node, bounds, byId);
            return;
        }
        if (UiTypes.COL.equals(node.type)
                || UiTypes.WINDOW.equals(node.type)
                || UiTypes.PANEL.equals(node.type)
                || UiTypes.GLASS.equals(node.type)
                || UiTypes.SCROLL.equals(node.type)
                || UiTypes.MARGIN.equals(node.type)
                || ArtNodeTypes.SHADER_EFFECT.equals(node.type)) {
            placeCol(node, bounds, byId);
            return;
        }
        if (UiTypes.CENTER.equals(node.type)) {
            placeCenter(node, bounds, byId);
            return;
        }
        if (UiTypes.STACK.equals(node.type)) {
            placeStack(node, bounds, byId);
            return;
        }
        // leaves: no children placement
    }

    private static void placeCenter(UiNode node, Rect bounds, Map<String, Rect> byId) {
        float pad = node.layout.pad;
        float innerW = Math.max(0f, bounds.width - 2f * pad);
        float innerH = Math.max(0f, bounds.height - 2f * pad);
        for (UiNode c : visibleChildren(node)) {
            float cw = preferredWidth(c);
            float ch = preferredHeight(c);
            float x = bounds.x + pad + Math.max(0f, (innerW - cw) * 0.5f);
            float y = bounds.y + pad + Math.max(0f, (innerH - ch) * 0.5f);
            place(c, new Rect(x, y, cw, ch), byId);
        }
    }

    private static void placeFragment(UiNode node, Rect bounds, Map<String, Rect> byId) {
        placeCol(node, bounds, byId);
    }

    private static void placeRow(UiNode node, Rect bounds, Map<String, Rect> byId) {
        float pad = node.layout.pad;
        float gap = node.layout.gap;
        float x = bounds.x + pad;
        float yBase = bounds.y + pad;
        float innerH = Math.max(0f, bounds.height - 2f * pad);
        List<UiNode> kids = visibleChildren(node);
        float fixed = 0f;
        float ratioSum = 0f;
        for (UiNode c : kids) {
            if (c.layout.expandsH()) {
                ratioSum += c.layout.stretchRatio;
            } else {
                fixed += preferredWidth(c);
            }
        }
        float gaps = kids.isEmpty() ? 0f : gap * Math.max(0, kids.size() - 1);
        float innerW = Math.max(0f, bounds.width - 2f * pad);
        float free = Math.max(0f, innerW - fixed - gaps);

        for (UiNode c : kids) {
            float cw =
                    c.layout.expandsH()
                            ? (ratioSum > 0f ? free * (c.layout.stretchRatio / ratioSum) : 0f)
                            : preferredWidth(c);
            cw = Math.max(cw, minWidthOf(c));
            float ch = c.layout.hasHeight() ? c.layout.height : Math.min(preferredHeight(c), innerH);
            if (ch <= 0f) {
                ch = innerH;
            }
            ch = Math.max(ch, minHeightOf(c));
            if (ch > innerH && innerH > 0f) {
                ch = innerH;
            }
            float y = yBase + crossOffset(innerH, ch, c.layout.align);
            Rect childBounds = new Rect(x, y, cw, ch);
            place(c, childBounds, byId);
            x += cw + gap;
        }
    }

    private static void placeCol(UiNode node, Rect bounds, Map<String, Rect> byId) {
        float pad = node.layout.pad;
        float gap = node.layout.gap;
        float xBase = bounds.x + pad;
        float y = bounds.y + pad;
        float innerW = Math.max(0f, bounds.width - 2f * pad);
        List<UiNode> kids = visibleChildren(node);
        float fixed = 0f;
        float ratioSum = 0f;
        for (UiNode c : kids) {
            if (c.layout.expandsV()) {
                ratioSum += c.layout.stretchRatio;
            } else {
                fixed += preferredHeight(c);
            }
        }
        float gaps = kids.isEmpty() ? 0f : gap * Math.max(0, kids.size() - 1);
        float innerH = Math.max(0f, bounds.height - 2f * pad);
        float free = Math.max(0f, innerH - fixed - gaps);

        for (UiNode c : kids) {
            float ch =
                    c.layout.expandsV()
                            ? (ratioSum > 0f ? free * (c.layout.stretchRatio / ratioSum) : 0f)
                            : preferredHeight(c);
            ch = Math.max(ch, minHeightOf(c));
            float cw = c.layout.hasWidth() ? c.layout.width : Math.min(preferredWidth(c), innerW);
            if (cw <= 0f) {
                cw = innerW;
            }
            cw = Math.max(cw, minWidthOf(c));
            if (cw > innerW && innerW > 0f) {
                cw = innerW;
            }
            float x = xBase + crossOffset(innerW, cw, c.layout.align);
            Rect childBounds = new Rect(x, y, cw, ch);
            place(c, childBounds, byId);
            y += ch + gap;
        }
    }

    private static float crossOffset(float outer, float inner, String align) {
        if (inner >= outer) {
            return 0f;
        }
        if (Align.CENTER.equals(align)) {
            return (outer - inner) * 0.5f;
        }
        if (Align.END.equals(align)) {
            return outer - inner;
        }
        return 0f;
    }

    private static void placeStack(UiNode node, Rect bounds, Map<String, Rect> byId) {
        float pad = node.layout.pad;
        Rect inner = new Rect(
                bounds.x + pad,
                bounds.y + pad,
                Math.max(0f, bounds.width - 2f * pad),
                Math.max(0f, bounds.height - 2f * pad));
        for (UiNode c : visibleChildren(node)) {
            place(c, inner, byId);
        }
    }

    private static List<UiNode> visibleChildren(UiNode node) {
        List<UiNode> out = new ArrayList<UiNode>();
        for (UiNode c : node.children) {
            if (UiTypes.FRAGMENT.equals(c.type) && c.id.isEmpty()) {
                out.addAll(c.children);
            } else {
                out.add(c);
            }
        }
        return out;
    }

    static float preferredWidth(UiNode node) {
        float min = minWidthOf(node);
        float raw;
        if (node.layout.hasWidth()) {
            raw = node.layout.width;
        } else if (UiTypes.WINDOW.equals(node.type)) {
            raw = DEFAULT_WINDOW_WIDTH;
        } else if (UiTypes.FRAGMENT.equals(node.type)) {
            if (node.children.isEmpty()) {
                raw = 0f;
            } else {
                float sum = 0f;
                for (UiNode c : node.children) {
                    sum = Math.max(sum, preferredWidth(c));
                }
                raw = sum;
            }
        } else if (UiTypes.ROW.equals(node.type)) {
            float pad = node.layout.pad;
            float gap = node.layout.gap;
            float sum = 0f;
            List<UiNode> kids = visibleChildren(node);
            for (int i = 0; i < kids.size(); i++) {
                sum += preferredWidth(kids.get(i));
                if (i > 0) {
                    sum += gap;
                }
            }
            raw = sum + 2f * pad;
        } else if (UiTypes.COL.equals(node.type)
                || UiTypes.PANEL.equals(node.type)
                || UiTypes.GLASS.equals(node.type)
                || UiTypes.STACK.equals(node.type)
                || UiTypes.SCROLL.equals(node.type)
                || UiTypes.MARGIN.equals(node.type)
                || UiTypes.CENTER.equals(node.type)
                || ArtNodeTypes.SHADER_EFFECT.equals(node.type)) {
            float pad = node.layout.pad;
            float max = 0f;
            for (UiNode c : visibleChildren(node)) {
                max = Math.max(max, preferredWidth(c));
            }
            raw = max + 2f * pad;
        } else if (UiTypes.HITAREA.equals(node.type)) {
            raw = node.layout.hasWidth() ? node.layout.width : DEFAULT_LEAF_WIDTH;
        } else if (UiTypes.TEXTFIELD.equals(node.type)) {
            raw = node.layout.hasWidth() ? node.layout.width : DEFAULT_LEAF_WIDTH * 1.5f;
        } else {
            raw = DEFAULT_LEAF_WIDTH;
        }
        return Math.max(raw, min);
    }

    static float preferredHeight(UiNode node) {
        float min = minHeightOf(node);
        float raw;
        if (node.layout.hasHeight()) {
            raw = node.layout.height;
        } else if (UiTypes.WINDOW.equals(node.type)) {
            raw = DEFAULT_WINDOW_HEIGHT;
        } else if (UiTypes.FRAGMENT.equals(node.type)) {
            if (node.children.isEmpty()) {
                raw = 0f;
            } else {
                float sum = 0f;
                List<UiNode> kids = node.children;
                for (int i = 0; i < kids.size(); i++) {
                    sum += preferredHeight(kids.get(i));
                }
                raw = sum;
            }
        } else if (UiTypes.COL.equals(node.type)
                || UiTypes.PANEL.equals(node.type)
                || UiTypes.GLASS.equals(node.type)
                || UiTypes.SCROLL.equals(node.type)
                || UiTypes.MARGIN.equals(node.type)
                || ArtNodeTypes.SHADER_EFFECT.equals(node.type)) {
            float pad = node.layout.pad;
            float gap = node.layout.gap;
            float sum = 0f;
            List<UiNode> kids = visibleChildren(node);
            for (int i = 0; i < kids.size(); i++) {
                sum += preferredHeight(kids.get(i));
                if (i > 0) {
                    sum += gap;
                }
            }
            raw = sum + 2f * pad;
        } else if (UiTypes.CENTER.equals(node.type)
                || UiTypes.ROW.equals(node.type)
                || UiTypes.STACK.equals(node.type)) {
            float pad = node.layout.pad;
            float max = 0f;
            for (UiNode c : visibleChildren(node)) {
                max = Math.max(max, preferredHeight(c));
            }
            raw = max + 2f * pad;
        } else if (UiTypes.HITAREA.equals(node.type)) {
            raw = node.layout.hasHeight() ? node.layout.height : DEFAULT_LEAF_HEIGHT * 2f;
        } else if (UiTypes.SLIDER.equals(node.type)
                || UiTypes.PROGRESS.equals(node.type)
                || UiTypes.TEXTFIELD.equals(node.type)
                || UiTypes.CHECKBOX.equals(node.type)) {
            raw = DEFAULT_LEAF_HEIGHT;
        } else {
            raw = DEFAULT_LEAF_HEIGHT;
        }
        return Math.max(raw, min);
    }

    private static float minWidthOf(UiNode node) {
        return node.layout.hasMinWidth() ? node.layout.minWidth : 0f;
    }

    private static float minHeightOf(UiNode node) {
        return node.layout.hasMinHeight() ? node.layout.minHeight : 0f;
    }
}
