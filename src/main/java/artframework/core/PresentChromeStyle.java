package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * C2 full-present chrome tokens derived from a {@link Theme} / {@link PresentProfile}.
 * Pure data; draw paths read the active style — does not suppress native surfaces.
 */
public final class PresentChromeStyle {

    public final float labelR;
    public final float labelG;
    public final float labelB;
    public final float labelA;

    public final float accentR;
    public final float accentG;
    public final float accentB;
    public final float accentA;

    public final float borderR;
    public final float borderG;
    public final float borderB;
    public final float borderA;
    public final float borderWidth;

    public final float cardAlpha;
    public final float panelAlpha;
    public final float disabledR;
    public final float disabledG;
    public final float disabledB;
    public final float disabledA;

    public PresentChromeStyle(
            float labelR,
            float labelG,
            float labelB,
            float labelA,
            float accentR,
            float accentG,
            float accentB,
            float accentA,
            float borderR,
            float borderG,
            float borderB,
            float borderA,
            float borderWidth,
            float cardAlpha,
            float panelAlpha,
            float disabledR,
            float disabledG,
            float disabledB,
            float disabledA) {
        this.labelR = labelR;
        this.labelG = labelG;
        this.labelB = labelB;
        this.labelA = labelA;
        this.accentR = accentR;
        this.accentG = accentG;
        this.accentB = accentB;
        this.accentA = accentA;
        this.borderR = borderR;
        this.borderG = borderG;
        this.borderB = borderB;
        this.borderA = borderA;
        this.borderWidth = borderWidth;
        this.cardAlpha = cardAlpha;
        this.panelAlpha = panelAlpha;
        this.disabledR = disabledR;
        this.disabledG = disabledG;
        this.disabledB = disabledB;
        this.disabledA = disabledA;
    }

    public static PresentChromeStyle stsDefault() {
        return new PresentChromeStyle(
                1f, 1f, 1f, 1f,
                1f, 0.9f, 0.55f, 1f,
                1f, 1f, 1f, 0.35f,
                1f,
                1f,
                0.92f,
                0.55f, 0.55f, 0.55f, 1f);
    }

    public static PresentChromeStyle fromTheme(Theme theme) {
        if (theme == null) {
            return stsDefault();
        }
        ThemeColor label = firstColor(theme, "Label", "font_color", 1f, 1f, 1f, 1f);
        ThemeColor accent = firstColor(theme, "Button", "font_hover_color", 1f, 0.9f, 0.55f, 1f);
        ThemeColor border = firstColor(theme, "Panel", "border_color", 1f, 1f, 1f, 0.9f);
        ThemeColor panel = firstColor(theme, "Panel", "bg", 0.12f, 0.11f, 0.1f, 0.92f);
        ThemeColor disabled = firstColor(theme, "Button", "font_color", 0.55f, 0.55f, 0.55f, 1f);
        float borderW = theme.hasConstant("Panel", "border_width")
                ? theme.getConstant("Panel", "border_width")
                : 2;
        float cardA = theme.hasConstant("Card", "alpha_pct")
                ? theme.getConstant("Card", "alpha_pct") / 100f
                : panel.a;
        if (cardA <= 0f || cardA > 1f) {
            cardA = 0.9f;
        }
        return new PresentChromeStyle(
                label.r, label.g, label.b, label.a,
                accent.r, accent.g, accent.b, accent.a,
                border.r, border.g, border.b, border.a,
                borderW,
                cardA,
                panel.a > 0f ? panel.a : 0.9f,
                disabled.r * 0.55f, disabled.g * 0.55f, disabled.b * 0.55f, disabled.a);
    }

    public Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("cardAlpha", Float.valueOf(cardAlpha));
        m.put("panelAlpha", Float.valueOf(panelAlpha));
        m.put("borderWidth", Float.valueOf(borderWidth));
        m.put("border", rgba(borderR, borderG, borderB, borderA));
        m.put("label", rgba(labelR, labelG, labelB, labelA));
        m.put("accent", rgba(accentR, accentG, accentB, accentA));
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, Object> rgba(float r, float g, float b, float a) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("r", Float.valueOf(r));
        m.put("g", Float.valueOf(g));
        m.put("b", Float.valueOf(b));
        m.put("a", Float.valueOf(a));
        return m;
    }

    private static ThemeColor firstColor(
            Theme theme, String type, String name, float r, float g, float b, float a) {
        ThemeColor c = theme.getColor(type, name);
        if (c != null) {
            return c;
        }
        return new ThemeColor(r, g, b, a);
    }
}
