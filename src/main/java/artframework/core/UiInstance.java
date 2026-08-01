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
    private final String emitId;
    private final String type;
    private final UiInstance parent;
    private final List<UiInstance> children = new ArrayList<UiInstance>();
    private final Map<String, Object> props = new LinkedHashMap<String, Object>();
    private final Map<String, ThemeColor> colorOverrides = new LinkedHashMap<String, ThemeColor>();
    private final Map<String, Integer> constantOverrides = new LinkedHashMap<String, Integer>();
    private Theme theme;
    private PresentBinding presentBinding;
    private Rect rect = Rect.ZERO;
    private float minW;
    private float minH;
    private boolean mounted;

    UiInstance(UiTree tree, UiNode decl, UiInstance parent, int anonymousSequence) {
        this.tree = tree;
        this.decl = decl;
        this.id = decl.id != null ? decl.id : "";
        this.emitId = this.id.isEmpty() ? "@anon:" + anonymousSequence : this.id;
        this.type = decl.type;
        this.parent = parent;
        this.props.putAll(decl.props);
        this.presentBinding = PresentResolve.parseBindingFromProps(decl.type, decl.props);
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

    public PresentBinding presentBinding() {
        return presentBinding;
    }

    public void setPresentBinding(PresentBinding binding) {
        this.presentBinding = binding;
    }

    /** Resolved present (profile cascade + project fallback) at this node. */
    public PresentResolved resolvePresent() {
        return PresentResolve.forNode(this);
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

    /** Declared theme type variation (prop themeType), else component type. */
    public String themeType() {
        Object v = props.get("themeType");
        if (v == null) {
            v = props.get("theme_type");
        }
        if (v != null) {
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return type;
    }

    public ThemeColor getThemeColor(String name, String themeType) {
        ThemeColor local = colorOverrides.get(name);
        if (local != null) {
            return local;
        }
        String variation = themeType != null ? themeType : themeType();
        String baseType = type;
        UiInstance cur = this;
        while (cur != null) {
            if (cur.theme != null) {
                ThemeColor c = lookupColor(cur.theme, variation, baseType, name);
                if (c != null) {
                    return c;
                }
            }
            cur = cur.parent;
        }
        Theme treeTheme = tree.theme();
        if (treeTheme != null) {
            return lookupColor(treeTheme, variation, baseType, name);
        }
        return null;
    }

    public int getThemeConstant(String name, String themeType) {
        Integer local = constantOverrides.get(name);
        if (local != null) {
            return local.intValue();
        }
        String variation = themeType != null ? themeType : themeType();
        String baseType = type;
        UiInstance cur = this;
        while (cur != null) {
            if (cur.theme != null) {
                Integer v = lookupConstant(cur.theme, variation, baseType, name);
                if (v != null) {
                    return v.intValue();
                }
            }
            cur = cur.parent;
        }
        Theme treeTheme = tree.theme();
        if (treeTheme != null) {
            Integer v = lookupConstant(treeTheme, variation, baseType, name);
            if (v != null) {
                return v.intValue();
            }
        }
        return 0;
    }

    public String getThemeFont(String name, String themeType) {
        String variation = themeType != null ? themeType : themeType();
        String baseType = type;
        UiInstance cur = this;
        while (cur != null) {
            if (cur.theme != null) {
                String f = lookupFont(cur.theme, variation, baseType, name);
                if (f != null) {
                    return f;
                }
            }
            cur = cur.parent;
        }
        Theme treeTheme = tree.theme();
        if (treeTheme != null) {
            return lookupFont(treeTheme, variation, baseType, name);
        }
        return null;
    }

    public int getThemeFontSize(String name, String themeType) {
        String variation = themeType != null ? themeType : themeType();
        String baseType = type;
        UiInstance cur = this;
        while (cur != null) {
            if (cur.theme != null) {
                Integer v = lookupFontSize(cur.theme, variation, baseType, name);
                if (v != null) {
                    return v.intValue();
                }
            }
            cur = cur.parent;
        }
        Theme treeTheme = tree.theme();
        if (treeTheme != null) {
            Integer v = lookupFontSize(treeTheme, variation, baseType, name);
            if (v != null) {
                return v.intValue();
            }
        }
        return 0;
    }

    public String getThemeIcon(String name, String themeType) {
        String variation = themeType != null ? themeType : themeType();
        String baseType = type;
        UiInstance cur = this;
        while (cur != null) {
            if (cur.theme != null) {
                String icon = lookupIcon(cur.theme, variation, baseType, name);
                if (icon != null) {
                    return icon;
                }
            }
            cur = cur.parent;
        }
        Theme treeTheme = tree.theme();
        if (treeTheme != null) {
            return lookupIcon(treeTheme, variation, baseType, name);
        }
        return null;
    }

    public String getThemeStyle(String name, String themeType) {
        String variation = themeType != null ? themeType : themeType();
        String baseType = type;
        UiInstance cur = this;
        while (cur != null) {
            if (cur.theme != null) {
                String style = lookupStyle(cur.theme, variation, baseType, name);
                if (style != null) {
                    return style;
                }
            }
            cur = cur.parent;
        }
        Theme treeTheme = tree.theme();
        if (treeTheme != null) {
            return lookupStyle(treeTheme, variation, baseType, name);
        }
        return null;
    }

    private static ThemeColor lookupColor(Theme theme, String variation, String baseType, String name) {
        if (variation != null && !variation.isEmpty()) {
            ThemeColor c = theme.getColor(variation, name);
            if (c != null) {
                return c;
            }
        }
        if (baseType != null && !baseType.isEmpty() && !baseType.equals(variation)) {
            ThemeColor c = theme.getColor(baseType, name);
            if (c != null) {
                return c;
            }
        }
        return theme.getColor(variation != null ? variation : baseType, name);
    }

    private static Integer lookupConstant(Theme theme, String variation, String baseType, String name) {
        if (variation != null && !variation.isEmpty() && theme.hasConstant(variation, name)) {
            return Integer.valueOf(theme.getConstant(variation, name));
        }
        if (baseType != null && !baseType.isEmpty() && theme.hasConstant(baseType, name)) {
            return Integer.valueOf(theme.getConstant(baseType, name));
        }
        return null;
    }

    private static String lookupFont(Theme theme, String variation, String baseType, String name) {
        if (variation != null && !variation.isEmpty()) {
            String f = theme.getFont(variation, name);
            if (f != null) {
                return f;
            }
        }
        if (baseType != null && !baseType.isEmpty()) {
            return theme.getFont(baseType, name);
        }
        return null;
    }

    private static Integer lookupFontSize(Theme theme, String variation, String baseType, String name) {
        if (variation != null && !variation.isEmpty() && theme.hasFontSize(variation, name)) {
            return Integer.valueOf(theme.getFontSize(variation, name));
        }
        if (baseType != null && !baseType.isEmpty() && theme.hasFontSize(baseType, name)) {
            return Integer.valueOf(theme.getFontSize(baseType, name));
        }
        return null;
    }

    private static String lookupIcon(Theme theme, String variation, String baseType, String name) {
        if (variation != null && !variation.isEmpty()) {
            String icon = theme.getIcon(variation, name);
            if (icon != null) {
                return icon;
            }
        }
        if (baseType != null && !baseType.isEmpty()) {
            return theme.getIcon(baseType, name);
        }
        return null;
    }

    private static String lookupStyle(Theme theme, String variation, String baseType, String name) {
        if (variation != null && !variation.isEmpty()) {
            String style = theme.getStyleBox(variation, name);
            if (style != null) {
                return style;
            }
        }
        if (baseType != null && !baseType.isEmpty()) {
            return theme.getStyleBox(baseType, name);
        }
        return null;
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

    String resolveEmitId() {
        return emitId;
    }
}
