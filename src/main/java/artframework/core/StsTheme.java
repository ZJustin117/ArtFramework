package artframework.core;

/**
 * Default STS-flavored theme tokens (cream/gold palette). Host maps ids to FontHelper/ImageMaster.
 */
public final class StsTheme {

    private StsTheme() {}

    public static Theme createDefault() {
        Theme t = new Theme();
        // Label
        t.setColor("Label", "font_color", 0.95f, 0.92f, 0.85f, 1f);
        t.setFontSize("Label", "font_size", 22);
        t.setFont("Label", "font", "sts.font.desc");
        // Button
        t.setColor("Button", "font_color", 1f, 0.96f, 0.85f, 1f);
        t.setColor("Button", "font_hover_color", 1f, 0.9f, 0.5f, 1f);
        t.setFontSize("Button", "font_size", 22);
        t.setFont("Button", "font", "sts.font.button");
        t.setStyleBox("Button", "normal", "sts.button.normal");
        t.setStyleBox("Button", "hover", "sts.button.hover");
        t.setStyleBox("Button", "pressed", "sts.button.pressed");
        // Box / containers
        t.setConstant("Box", "separation", 6);
        t.setConstant("Margin", "margin_left", 8);
        t.setConstant("Margin", "margin_right", 8);
        t.setConstant("Margin", "margin_top", 8);
        t.setConstant("Margin", "margin_bottom", 8);
        // Slider / Range
        t.setColor("Slider", "grabber", 0.9f, 0.75f, 0.35f, 1f);
        t.setColor("Slider", "track", 0.3f, 0.28f, 0.25f, 1f);
        // Panel
        t.setStyleBox("Panel", "panel", "sts.panel");
        t.setColor("Panel", "bg", 0.12f, 0.11f, 0.1f, 0.92f);
        // Window
        t.setColor("Window", "title_color", 1f, 0.9f, 0.55f, 1f);
        t.setFontSize("Window", "title_font_size", 26);
        t.setFont("Window", "title_font", "sts.font.title");
        return t;
    }
}
