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
    private final Map<String, Float> sliderValues = new LinkedHashMap<String, Float>();
    private final Map<String, String> textValues = new LinkedHashMap<String, String>();
    private final Map<String, Boolean> checkboxValues = new LinkedHashMap<String, Boolean>();
    private final Map<String, Float> progressValues = new LinkedHashMap<String, Float>();

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
        seedControls(root);
    }

    private void seedControls(UiNode node) {
        if (!node.id.isEmpty()) {
            if (UiTypes.SLIDER.equals(node.type)) {
                float min = node.propFloat("min", 0f);
                float max = node.propFloat("max", 1f);
                float value = node.propFloat("value", min);
                sliderValues.put(node.id, Float.valueOf(clamp(value, min, max)));
            } else if (UiTypes.TEXTFIELD.equals(node.type)) {
                textValues.put(node.id, node.propString("text", ""));
            } else if (UiTypes.CHECKBOX.equals(node.type)) {
                checkboxValues.put(node.id, Boolean.valueOf(node.propBool("checked", false)));
            } else if (UiTypes.PROGRESS.equals(node.type)) {
                float min = node.propFloat("min", 0f);
                float max = node.propFloat("max", 1f);
                float value = node.propFloat("value", node.propFloat("progress", min));
                progressValues.put(node.id, Float.valueOf(clamp(value, min, max)));
            }
        }
        for (UiNode c : node.children) {
            seedControls(c);
        }
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
        Float v = sliderValues.get(sliderId);
        if (v == null) {
            throw new IllegalArgumentException("not a slider: " + sliderId);
        }
        return v.floatValue();
    }

    public boolean hasSlider(String sliderId) {
        return sliderValues.containsKey(sliderId);
    }

    public float setSlider(String sliderId, float value) {
        UiNode n = index.get(sliderId);
        if (n == null || !UiTypes.SLIDER.equals(n.type)) {
            throw new IllegalArgumentException("not a slider: " + sliderId);
        }
        float min = n.propFloat("min", 0f);
        float max = n.propFloat("max", 1f);
        float clamped = clamp(value, min, max);
        sliderValues.put(sliderId, Float.valueOf(clamped));
        return clamped;
    }

    public boolean hasTextField(String id) {
        return textValues.containsKey(id);
    }

    public String getText(String id) {
        String v = textValues.get(id);
        if (v == null) {
            throw new IllegalArgumentException("not a textfield: " + id);
        }
        return v;
    }

    public String setText(String id, String text) {
        UiNode n = index.get(id);
        if (n == null || !UiTypes.TEXTFIELD.equals(n.type)) {
            throw new IllegalArgumentException("not a textfield: " + id);
        }
        String t = text != null ? text : "";
        textValues.put(id, t);
        return t;
    }

    public boolean hasCheckbox(String id) {
        return checkboxValues.containsKey(id);
    }

    public boolean getChecked(String id) {
        Boolean v = checkboxValues.get(id);
        if (v == null) {
            throw new IllegalArgumentException("not a checkbox: " + id);
        }
        return v.booleanValue();
    }

    public boolean setChecked(String id, boolean checked) {
        UiNode n = index.get(id);
        if (n == null || !UiTypes.CHECKBOX.equals(n.type)) {
            throw new IllegalArgumentException("not a checkbox: " + id);
        }
        checkboxValues.put(id, Boolean.valueOf(checked));
        return checked;
    }

    public boolean toggleCheckbox(String id) {
        return setChecked(id, !getChecked(id));
    }

    public boolean hasProgress(String id) {
        return progressValues.containsKey(id);
    }

    public float getProgress(String id) {
        Float v = progressValues.get(id);
        if (v == null) {
            throw new IllegalArgumentException("not a progress: " + id);
        }
        return v.floatValue();
    }

    public float setProgress(String id, float value) {
        UiNode n = index.get(id);
        if (n == null || !UiTypes.PROGRESS.equals(n.type)) {
            throw new IllegalArgumentException("not a progress: " + id);
        }
        float min = n.propFloat("min", 0f);
        float max = n.propFloat("max", 1f);
        float clamped = clamp(value, min, max);
        progressValues.put(id, Float.valueOf(clamped));
        return clamped;
    }

    public List<String> sliderIds() {
        return Collections.unmodifiableList(new ArrayList<String>(sliderValues.keySet()));
    }

    public List<String> buttonIds() {
        return index.idsOfType(UiTypes.BUTTON);
    }

    public List<String> hitAreaIds() {
        return index.idsOfType(UiTypes.HITAREA);
    }

    public List<String> textFieldIds() {
        return Collections.unmodifiableList(new ArrayList<String>(textValues.keySet()));
    }

    public List<String> checkboxIds() {
        return Collections.unmodifiableList(new ArrayList<String>(checkboxValues.keySet()));
    }

    public List<String> progressIds() {
        return Collections.unmodifiableList(new ArrayList<String>(progressValues.keySet()));
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
        for (Map.Entry<String, Float> e : sliderValues.entrySet()) {
            sliders.put(e.getKey(), e.getValue());
        }
        out.put("sliders", sliders);
        Map<String, Object> texts = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, String> e : textValues.entrySet()) {
            texts.put(e.getKey(), e.getValue());
        }
        out.put("texts", texts);
        Map<String, Object> checks = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Boolean> e : checkboxValues.entrySet()) {
            checks.put(e.getKey(), e.getValue());
        }
        out.put("checkboxes", checks);
        Map<String, Object> progress = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Float> e : progressValues.entrySet()) {
            progress.put(e.getKey(), e.getValue());
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
}
