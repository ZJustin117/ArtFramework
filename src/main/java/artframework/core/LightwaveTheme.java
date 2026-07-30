package artframework.core;

/**
 * Lightwave showcase theme: STS resource ids, semi-transparent panels, white bounds border,
 * cool accent for diagonal light-wave FX.
 */
public final class LightwaveTheme {

    private LightwaveTheme() {}

    public static Theme createDefault() {
        Theme t = new Theme();
        t.setName("lightwave");
        // Label — cool near-white
        t.setColor("Label", "font_color", 1f, 1f, 1f, 1f);
        t.setFontSize("Label", "font_size", 22);
        t.setFont("Label", "font", "sts.font.desc");
        // Button
        t.setColor("Button", "font_color", 1f, 1f, 1f, 1f);
        t.setColor("Button", "font_hover_color", 0.55f, 0.85f, 1f, 1f);
        t.setFontSize("Button", "font_size", 22);
        t.setFont("Button", "font", "sts.font.button");
        t.setStyleBox("Button", "normal", "sts.button.normal");
        t.setStyleBox("Button", "hover", "sts.button.hover");
        t.setStyleBox("Button", "pressed", "sts.button.pressed");
        // PrimaryButton variation (themeType)
        t.setColor("PrimaryButton", "font_color", 0.7f, 0.95f, 1f, 1f);
        t.setColor("PrimaryButton", "font_hover_color", 0.4f, 0.9f, 1f, 1f);
        // Box / containers
        t.setConstant("Box", "separation", 8);
        t.setConstant("Margin", "margin_left", 10);
        t.setConstant("Margin", "margin_right", 10);
        t.setConstant("Margin", "margin_top", 10);
        t.setConstant("Margin", "margin_bottom", 10);
        // Slider
        t.setColor("Slider", "grabber", 0.55f, 0.85f, 1f, 1f);
        t.setColor("Slider", "track", 0.2f, 0.28f, 0.35f, 0.85f);
        // Panel — semi-transparent + white border tokens
        t.setStyleBox("Panel", "panel", "sts.panel");
        // Translucent so lightwave under scene2d remains visible through the panel.
        t.setColor("Panel", "bg", 0.06f, 0.1f, 0.16f, 0.45f);
        t.setColor("Panel", "border_color", 1f, 1f, 1f, 0.95f);
        t.setConstant("Panel", "border_width", 2);
        // Window
        t.setColor("Window", "title_color", 0.75f, 0.92f, 1f, 1f);
        t.setColor("Window", "bg", 0.05f, 0.08f, 0.12f, 0.78f);
        t.setFontSize("Window", "title_font_size", 26);
        t.setFont("Window", "title_font", "sts.font.title");
        // Card (C2 chrome)
        t.setConstant("Card", "alpha_pct", 88);
        t.setColor("Card", "border_color", 1f, 1f, 1f, 0.9f);
        // Lightwave accent
        t.setColor("Lightwave", "band", 0.55f, 0.88f, 1f, 0.55f);
        t.setColor("Lightwave", "glow", 0.7f, 0.95f, 1f, 0.4f);
        return t;
    }
}
