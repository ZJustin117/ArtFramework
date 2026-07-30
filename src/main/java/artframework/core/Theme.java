package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Godot-like theme resource: items keyed by type name + item name.
 * Pure data; drawing is host responsibility.
 */
public final class Theme {

    private String name = "";
    private final Map<String, ThemeColor> colors = new LinkedHashMap<String, ThemeColor>();
    private final Map<String, Integer> constants = new LinkedHashMap<String, Integer>();
    private final Map<String, Integer> fontSizes = new LinkedHashMap<String, Integer>();
    private final Map<String, String> fonts = new LinkedHashMap<String, String>();
    private final Map<String, String> icons = new LinkedHashMap<String, String>();
    private final Map<String, String> styleBoxes = new LinkedHashMap<String, String>();

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String name() {
        return name;
    }

    public void setColor(String type, String name, float r, float g, float b, float a) {
        colors.put(itemKey(type, name), new ThemeColor(r, g, b, a));
    }

    public void setColor(String type, String name, ThemeColor color) {
        if (color != null) {
            colors.put(itemKey(type, name), color);
        }
    }

    public ThemeColor getColor(String type, String name) {
        return colors.get(itemKey(type, name));
    }

    public void setConstant(String type, String name, int value) {
        constants.put(itemKey(type, name), Integer.valueOf(value));
    }

    public int getConstant(String type, String name) {
        Integer v = constants.get(itemKey(type, name));
        return v == null ? 0 : v.intValue();
    }

    public boolean hasConstant(String type, String name) {
        return constants.containsKey(itemKey(type, name));
    }

    public void setFontSize(String type, String name, int size) {
        fontSizes.put(itemKey(type, name), Integer.valueOf(size));
    }

    public int getFontSize(String type, String name) {
        Integer v = fontSizes.get(itemKey(type, name));
        return v == null ? 0 : v.intValue();
    }

    public boolean hasFontSize(String type, String name) {
        return fontSizes.containsKey(itemKey(type, name));
    }

    public void setFont(String type, String name, String fontId) {
        if (fontId != null) {
            fonts.put(itemKey(type, name), fontId);
        }
    }

    public String getFont(String type, String name) {
        return fonts.get(itemKey(type, name));
    }

    public void setIcon(String type, String name, String iconId) {
        if (iconId != null) {
            icons.put(itemKey(type, name), iconId);
        }
    }

    public String getIcon(String type, String name) {
        return icons.get(itemKey(type, name));
    }

    /**
     * Resolve a theme icon id through HostAssets (milestone 15.4). Returns the resource key
     * when found or fallback; empty when missing under strict mode.
     */
    public String resolveIconAsset(String type, String name) {
        String iconId = getIcon(type, name);
        if (iconId == null || iconId.isEmpty()) {
            iconId = artframework.assets.ResourceIds.UI_BUTTON_DEFAULT;
        }
        artframework.assets.AssetResolveResult r =
                artframework.assets.HostAssetsHolder.get().resolve(iconId);
        if (r.found || r.fallback) {
            return r.resourceId.isEmpty() ? iconId : r.resourceId;
        }
        return "";
    }

    public artframework.assets.AssetResolveResult resolveStyleAsset(String type, String name) {
        String styleId = getStyleBox(type, name);
        if (styleId == null || styleId.isEmpty()) {
            styleId = artframework.assets.ResourceIds.UI_PANEL_DEFAULT;
        }
        return artframework.assets.HostAssetsHolder.get().resolve(styleId);
    }

    public void setStyleBox(String type, String name, String styleId) {
        if (styleId != null) {
            styleBoxes.put(itemKey(type, name), styleId);
        }
    }

    public String getStyleBox(String type, String name) {
        return styleBoxes.get(itemKey(type, name));
    }

    public Map<String, Object> probeSummary() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (name != null && !name.isEmpty()) {
            out.put("name", name);
        }
        out.put("colorCount", Integer.valueOf(colors.size()));
        out.put("constantCount", Integer.valueOf(constants.size()));
        out.put("fontSizeCount", Integer.valueOf(fontSizes.size()));
        out.put("fontCount", Integer.valueOf(fonts.size()));
        out.put("iconCount", Integer.valueOf(icons.size()));
        out.put("styleBoxCount", Integer.valueOf(styleBoxes.size()));
        return Collections.unmodifiableMap(out);
    }

    private static String itemKey(String type, String name) {
        String t = type != null ? type : "";
        String n = name != null ? name : "";
        return t + "\0" + n;
    }
}
