package artframework.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expands {@link UiTypes#REF} / {@link UiTypes#SLOT} into a concrete tree.
 */
public final class TemplateExpander {

    private final ComponentRegistry registry;

    public TemplateExpander(ComponentRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry required");
        }
        this.registry = registry;
    }

    public TemplateExpander() {
        this(ComponentRegistry.global());
    }

    /**
     * Fully expand refs/slots. Fails if a slot remains without fill or ref is missing.
     */
    public UiNode expand(UiNode root) {
        if (root == null) {
            throw new IllegalArgumentException("root required");
        }
        return expandNode(root, CollectionsEmpty.<String, List<UiNode>>emptyMap(), null);
    }

    private UiNode expandNode(
            UiNode node, Map<String, List<UiNode>> activeSlots, Map<String, Object> activeProps) {
        if (UiTypes.REF.equals(node.type)) {
            return expandRef(node);
        }
        if (UiTypes.SLOT.equals(node.type)) {
            return expandSlot(node, activeSlots);
        }

        UiNode.Builder b = UiNode.of(node.type)
                .id(node.id)
                .ref(node.ref)
                .layout(node.layout)
                .effects(node.effects)
                .props(overlayProps(node.props, activeProps))
                .signals(node.signals);

        List<UiNode> expandedChildren = new ArrayList<UiNode>();
        for (UiNode child : node.children) {
            UiNode expanded = expandNode(child, activeSlots, activeProps);
            if (expanded != null) {
                if (UiTypes.FRAGMENT.equals(expanded.type) && expanded.id.isEmpty()) {
                    expandedChildren.addAll(expanded.children);
                } else {
                    expandedChildren.add(expanded);
                }
            }
        }
        b.children(expandedChildren);

        Map<String, List<UiNode>> expandedSlots = new LinkedHashMap<String, List<UiNode>>();
        for (Map.Entry<String, List<UiNode>> e : node.slots.entrySet()) {
            List<UiNode> list = new ArrayList<UiNode>();
            for (UiNode s : e.getValue()) {
                UiNode expanded = expandNode(s, activeSlots, activeProps);
                if (expanded != null) {
                    list.add(expanded);
                }
            }
            expandedSlots.put(e.getKey(), list);
        }
        b.slots(expandedSlots);
        return b.build();
    }

    private UiNode expandRef(UiNode refNode) {
        String name = refNode.ref;
        if (name == null || name.isEmpty()) {
            name = refNode.propString("ref", "");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("ref name required");
        }
        UiNode template = registry.get(name);
        if (template == null) {
            throw new IllegalArgumentException("unknown template: " + name);
        }

        Map<String, List<UiNode>> callSlots = new LinkedHashMap<String, List<UiNode>>();
        for (Map.Entry<String, List<UiNode>> e : refNode.slots.entrySet()) {
            List<UiNode> expanded = new ArrayList<UiNode>();
            for (UiNode s : e.getValue()) {
                expanded.add(expandNode(s, CollectionsEmpty.<String, List<UiNode>>emptyMap(), null));
            }
            callSlots.put(e.getKey(), expanded);
        }
        if (!refNode.children.isEmpty()) {
            List<UiNode> defaultSlot = callSlots.get("default");
            if (defaultSlot == null) {
                defaultSlot = new ArrayList<UiNode>();
                callSlots.put("default", defaultSlot);
            }
            for (UiNode c : refNode.children) {
                defaultSlot.add(expandNode(c, CollectionsEmpty.<String, List<UiNode>>emptyMap(), null));
            }
        }

        Map<String, Object> callProps = new LinkedHashMap<String, Object>(refNode.props);
        UiNode body = expandNode(template, callSlots, callProps);

        UiNode.Builder out = body.toBuilder();
        if (!refNode.id.isEmpty()) {
            out.id(refNode.id);
        }
        if (!refNode.effects.isEmpty()) {
            out.effects(refNode.effects);
        }
        if (refNode.layout != LayoutSpec.EMPTY) {
            out.layout(mergeLayout(body.layout, refNode.layout));
        }
        if (!refNode.signals.isEmpty()) {
            List<String> merged = new ArrayList<String>(body.signals);
            for (String s : refNode.signals) {
                if (!merged.contains(s)) {
                    merged.add(s);
                }
            }
            out.signals(merged);
        }
        return out.build();
    }

    private UiNode expandSlot(UiNode slotNode, Map<String, List<UiNode>> activeSlots) {
        String name = slotNode.propString("name", slotNode.id);
        if (name == null || name.isEmpty()) {
            name = "default";
        }
        List<UiNode> fill = activeSlots != null ? activeSlots.get(name) : null;
        if (fill == null || fill.isEmpty()) {
            if (!slotNode.children.isEmpty()) {
                List<UiNode> fallback = new ArrayList<UiNode>();
                for (UiNode c : slotNode.children) {
                    fallback.add(expandNode(c, activeSlots, null));
                }
                return UiNode.of(UiTypes.FRAGMENT).children(fallback).build();
            }
            return UiNode.of(UiTypes.FRAGMENT).build();
        }
        List<UiNode> expanded = new ArrayList<UiNode>();
        for (UiNode n : fill) {
            expanded.add(expandNode(n, CollectionsEmpty.<String, List<UiNode>>emptyMap(), null));
        }
        if (expanded.size() == 1) {
            return expanded.get(0);
        }
        return UiNode.of(UiTypes.FRAGMENT).children(expanded).build();
    }

    private static Map<String, Object> overlayProps(
            Map<String, Object> base, Map<String, Object> activeProps) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (base != null) {
            for (Map.Entry<String, Object> e : base.entrySet()) {
                out.put(e.getKey(), substitute(e.getValue(), activeProps));
            }
        }
        return out;
    }

    private static Object substitute(Object value, Map<String, Object> activeProps) {
        if (activeProps == null || !(value instanceof String)) {
            return value;
        }
        String s = (String) value;
        if (s.length() >= 3 && s.startsWith("${") && s.endsWith("}")) {
            String key = s.substring(2, s.length() - 1);
            if (activeProps.containsKey(key)) {
                return activeProps.get(key);
            }
            return s;
        }
        return s;
    }

    private static LayoutSpec mergeLayout(LayoutSpec base, LayoutSpec over) {
        float w = over.hasWidth() ? over.width : base.width;
        float h = over.hasHeight() ? over.height : base.height;
        float minW = over.hasMinWidth() ? over.minWidth : base.minWidth;
        float minH = over.hasMinHeight() ? over.minHeight : base.minHeight;
        float pad = over.pad > 0f ? over.pad : base.pad;
        float gap = over.gap > 0f ? over.gap : base.gap;
        int sfH = over.sizeFlagsH != SizeFlags.DEFAULT ? over.sizeFlagsH : base.sizeFlagsH;
        int sfV = over.sizeFlagsV != SizeFlags.DEFAULT ? over.sizeFlagsV : base.sizeFlagsV;
        if (over.grow) {
            sfH = SizeFlags.fromGrow(true);
            sfV = SizeFlags.fromGrow(true);
        }
        float ratio = over.stretchRatio != 1f ? over.stretchRatio : base.stretchRatio;
        String align =
                over.align != null && !Align.BEGIN.equals(over.align) ? over.align : base.align;
        return new LayoutSpec(w, h, minW, minH, pad, gap, sfH, sfV, ratio, align);
    }

    /** Avoid importing Collections.emptyMap raw issues on older patterns. */
    private static final class CollectionsEmpty {
        static <K, V> Map<K, V> emptyMap() {
            return java.util.Collections.emptyMap();
        }
    }
}
