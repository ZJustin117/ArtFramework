package artframework.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime state for one open synthetic composition tree (pure; no GL).
 */
public final class WidgetSession {

    private final String windowId;
    private final UiNode root;
    private final NodeIndex index;

    public WidgetSession(String windowId, UiNode root) {
        if (windowId == null || windowId.isEmpty()) {
            throw new IllegalArgumentException("windowId required");
        }
        if (root == null) {
            throw new IllegalArgumentException("root required");
        }
        this.windowId = windowId;
        this.root = root;
        this.index = NodeIndex.of(root);
    }

    public String windowId() {
        return windowId;
    }

    public UiNode root() {
        return root;
    }

    public NodeIndex index() {
        return index;
    }

    public boolean hasControl(String controlId) {
        return index.contains(controlId);
    }

    public boolean hasType(String controlId, String type) {
        UiNode n = index.get(controlId);
        return n != null && type.equals(n.type);
    }

    public UiNode get(String controlId) {
        return index.get(controlId);
    }

    public float getSlider(String sliderId) {
        UiNode node = requireType(sliderId, UiTypes.SLIDER);
        Object value = value(sliderId, Float.valueOf(node.propFloat("value", node.propFloat("min", 0f))));
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("not a slider: " + sliderId);
        }
        return ((Number) value).floatValue();
    }

    public boolean hasSlider(String sliderId) {
        return hasType(sliderId, UiTypes.SLIDER);
    }

    public float setSlider(String sliderId, float value) {
        UiNode n = requireType(sliderId, UiTypes.SLIDER);
        float min = n.propFloat("min", 0f);
        float max = n.propFloat("max", 1f);
        float clamped = clamp(value, min, max);
        putValue(sliderId, Float.valueOf(clamped));
        return clamped;
    }

    public boolean hasTextField(String id) {
        return hasType(id, UiTypes.TEXTFIELD);
    }

    public String getText(String id) {
        UiNode node = requireType(id, UiTypes.TEXTFIELD);
        return String.valueOf(value(id, node.propString("text", "")));
    }

    public String setText(String id, String text) {
        requireType(id, UiTypes.TEXTFIELD);
        String t = text != null ? text : "";
        putValue(id, t);
        return t;
    }

    public boolean hasCheckbox(String id) {
        return hasType(id, UiTypes.CHECKBOX);
    }

    public boolean getChecked(String id) {
        UiNode node = requireType(id, UiTypes.CHECKBOX);
        return Boolean.TRUE.equals(value(id, Boolean.valueOf(node.propBool("checked", false))));
    }

    public boolean setChecked(String id, boolean checked) {
        requireType(id, UiTypes.CHECKBOX);
        putValue(id, Boolean.valueOf(checked));
        return checked;
    }

    public boolean toggleCheckbox(String id) {
        return setChecked(id, !getChecked(id));
    }

    public boolean hasProgress(String id) {
        return hasType(id, UiTypes.PROGRESS);
    }

    public float getProgress(String id) {
        UiNode node = requireType(id, UiTypes.PROGRESS);
        Object current = value(id, Float.valueOf(node.propFloat("value", node.propFloat("progress", 0f))));
        return current instanceof Number ? ((Number) current).floatValue() : 0f;
    }

    public float setProgress(String id, float value) {
        UiNode n = requireType(id, UiTypes.PROGRESS);
        float min = n.propFloat("min", 0f);
        float max = n.propFloat("max", 1f);
        float clamped = clamp(value, min, max);
        putValue(id, Float.valueOf(clamped));
        return clamped;
    }

    public List<String> sliderIds() {
        return index.idsOfType(UiTypes.SLIDER);
    }

    public List<String> buttonIds() {
        List<String> ids = new ArrayList<String>(index.idsOfType(UiTypes.BUTTON));
        for (String id : index.ids()) {
            UiNode node = index.get(id);
            if (node != null && StsNodeTypes.isPressable(node.type)) {
                ids.add(id);
            }
        }
        return Collections.unmodifiableList(ids);
    }

    public boolean isPressable(String id) {
        UiNode node = index.get(id);
        return node != null && (UiTypes.BUTTON.equals(node.type) || StsNodeTypes.isPressable(node.type));
    }

    public List<String> hitAreaIds() {
        return index.idsOfType(UiTypes.HITAREA);
    }

    public List<String> textFieldIds() {
        return index.idsOfType(UiTypes.TEXTFIELD);
    }

    public List<String> checkboxIds() {
        return index.idsOfType(UiTypes.CHECKBOX);
    }

    public List<String> progressIds() {
        return index.idsOfType(UiTypes.PROGRESS);
    }

    public Map<String, Object> probeControls() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("buttonIds", buttonIds());
        out.put("sliderIds", sliderIds());
        out.put("hitAreaIds", hitAreaIds());
        out.put("textFieldIds", textFieldIds());
        out.put("checkboxIds", checkboxIds());
        out.put("progressIds", progressIds());
        Map<String, Object> sliders = new LinkedHashMap<String, Object>();
        for (String id : sliderIds()) {
            sliders.put(id, Float.valueOf(getSlider(id)));
        }
        out.put("sliders", sliders);
        Map<String, Object> texts = new LinkedHashMap<String, Object>();
        for (String id : textFieldIds()) {
            texts.put(id, getText(id));
        }
        out.put("texts", texts);
        Map<String, Object> checks = new LinkedHashMap<String, Object>();
        for (String id : checkboxIds()) {
            checks.put(id, Boolean.valueOf(getChecked(id)));
        }
        out.put("checkboxes", checks);
        Map<String, Object> progress = new LinkedHashMap<String, Object>();
        for (String id : progressIds()) {
            progress.put(id, Float.valueOf(getProgress(id)));
        }
        out.put("progress", progress);
        List<String> effectHosts = new ArrayList<String>();
        collectEffectHosts(root, effectHosts);
        out.put("effectHostIds", effectHosts);
        return out;
    }

    private static void collectEffectHosts(UiNode node, List<String> out) {
        if (!node.id.isEmpty() && !node.effects.isEmpty()) {
            out.add(node.id);
        }
        for (UiNode c : node.children) {
            collectEffectHosts(c, out);
        }
    }

    private static float clamp(float v, float min, float max) {
        if (min > max) {
            float t = min;
            min = max;
            max = t;
        }
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    private UiNode requireType(String id, String type) {
        UiNode node = index.get(id);
        if (node == null || !type.equals(node.type)) {
            throw new IllegalArgumentException("not a " + type + ": " + id);
        }
        return node;
    }

    private Object value(String id, Object fallback) {
        artframework.presentation.NodeTree tree = artframework.api.ArtFramework.tree(windowId);
        artframework.presentation.Node node = tree != null ? tree.get(id) : null;
        if (node == null) return fallback;
        artframework.presentation.ControlValueComponent component = tree.world().get(
                node.entityId(), artframework.presentation.ControlValueComponent.class);
        return component != null ? component.value : fallback;
    }

    private void putValue(String id, Object value) {
        artframework.presentation.NodeTree tree = artframework.api.ArtFramework.tree(windowId);
        artframework.presentation.Node node = tree != null ? tree.get(id) : null;
        if (node != null) {
            tree.world().put(node.entityId(), artframework.presentation.ControlValueComponent.class,
                    new artframework.presentation.ControlValueComponent(value));
        }
    }
}
