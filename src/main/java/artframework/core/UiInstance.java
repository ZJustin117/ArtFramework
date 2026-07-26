package artframework.core;

import artframework.component.Rect;
import artframework.component.UiNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live node in a {@link UiTree} (Godot Control-like; pure runtime).
 */
public final class UiInstance {

    private final UiTree tree;
    private final UiNode decl;
    private final String id;
    private final String type;
    private final UiInstance parent;
    private final List<UiInstance> children = new ArrayList<UiInstance>();
    private final Map<String, Object> props = new LinkedHashMap<String, Object>();
    private final Map<String, ThemeColor> colorOverrides = new LinkedHashMap<String, ThemeColor>();
    private final Map<String, Integer> constantOverrides = new LinkedHashMap<String, Integer>();
    private Theme theme;
    private Rect rect = Rect.ZERO;
    private float minW;
    private float minH;
    private boolean mounted;

    UiInstance(UiTree tree, UiNode decl, UiInstance parent) {
        this.tree = tree;
        this.decl = decl;
        this.id = decl.id != null ? decl.id : "";
        this.type = decl.type;
        this.parent = parent;
        this.props.putAll(decl.props);
    }

    public UiTree tree() {
        return tree;
    }

    public UiNode decl() {
        return decl;
    }

    public String id() {
        return id;
    }

    public String type() {
        return type;
    }

    public UiInstance parent() {
        return parent;
    }

    public List<UiInstance> children() {
        return Collections.unmodifiableList(children);
    }

    public boolean isMounted() {
        return mounted;
    }

    public Rect rect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect != null ? rect : Rect.ZERO;
    }

    public float minWidth() {
        return minW;
    }

    public float minHeight() {
        return minH;
    }

    public void setMinSize(float w, float h) {
        this.minW = w;
        this.minH = h;
    }

    public Object prop(String key) {
        return props.get(key);
    }

    public String propString(String key, String def) {
        Object v = props.get(key);
        if (v == null) {
            return def;
        }
        return String.valueOf(v);
    }

    public void setProp(String key, Object value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            props.remove(key);
        } else {
            props.put(key, value);
        }
    }

    public Map<String, Object> propsView() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(props));
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public Theme theme() {
        return theme;
    }

    public void setThemeColorOverride(String name, float r, float g, float b, float a) {
        if (name != null) {
            colorOverrides.put(name, new ThemeColor(r, g, b, a));
        }
    }

    public void setThemeConstantOverride(String name, int value) {
        if (name != null) {
            constantOverrides.put(name, Integer.valueOf(value));
        }
    }

    public ThemeColor getThemeColor(String name, String themeType) {
        ThemeColor local = colorOverrides.get(name);
        if (local != null) {
            return local;
        }
        UiInstance cur = this;
        while (cur != null) {
            if (cur.theme != null) {
                ThemeColor c = cur.theme.getColor(themeType, name);
                if (c != null) {
                    return c;
                }
            }
            cur = cur.parent;
        }
        Theme treeTheme = tree.theme();
        if (treeTheme != null) {
            return treeTheme.getColor(themeType, name);
        }
        return null;
    }

    public int getThemeConstant(String name, String themeType) {
        Integer local = constantOverrides.get(name);
        if (local != null) {
            return local.intValue();
        }
        UiInstance cur = this;
        while (cur != null) {
            if (cur.theme != null && cur.theme.hasConstant(themeType, name)) {
                return cur.theme.getConstant(themeType, name);
            }
            cur = cur.parent;
        }
        Theme treeTheme = tree.theme();
        if (treeTheme != null && treeTheme.hasConstant(themeType, name)) {
            return treeTheme.getConstant(themeType, name);
        }
        return 0;
    }

    public List<String> signals() {
        return decl.signals;
    }

    public boolean declaresSignal(String signal) {
        return decl.declaresSignal(signal);
    }

    public void connect(String signal, SignalHandler handler) {
        tree.connect(resolveEmitId(), signal, handler);
    }

    public void disconnect(String signal, SignalHandler handler) {
        tree.disconnect(resolveEmitId(), signal, handler);
    }

    public void emit(String signal, Object... args) {
        tree.emit(resolveEmitId(), signal, args);
    }

    void addChild(UiInstance child) {
        children.add(child);
    }

    void markMounted(boolean value) {
        this.mounted = value;
    }

    private String resolveEmitId() {
        if (!id.isEmpty()) {
            return id;
        }
        return System.identityHashCode(this) + "";
    }
}
