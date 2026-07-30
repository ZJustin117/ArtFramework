package artframework.c1.skin;

import artframework.core.Theme;
import artframework.core.ThemeColor;
import artframework.core.Themes;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * scene2d {@link Skin} chrome (drawables) + Theme colors. Text is drawn by {@code StsTextActor}
 * via FontHelper — do not rely on scene2d Label/TextButton glyphs with shared STS fonts.
 */
public final class StsSkin {

    private StsSkin() {}

    public static Skin create() {
        return create(Themes.getDefault());
    }

    public static Skin create(Theme theme) {
        if (ImageMaster.WHITE_SQUARE_IMG == null) {
            throw new IllegalStateException("ImageMaster not initialized");
        }
        // Minimal font only so Window/TextField construction does not NPE; labels/buttons use StsTextActor.
        BitmapFont any =
                FontHelper.buttonLabelFont != null
                        ? FontHelper.buttonLabelFont
                        : FontHelper.tipBodyFont;
        if (any == null) {
            any = FontHelper.panelNameFont;
        }
        if (any == null) {
            throw new IllegalStateException("FontHelper not initialized");
        }

        Skin skin = new Skin();
        TextureRegion whiteRegion = new TextureRegion(ImageMaster.WHITE_SQUARE_IMG);
        TextureRegionDrawable white = new TextureRegionDrawable(whiteRegion);

        Color windowBg =
                colorOr(theme, "Window", "bg", colorOr(theme, "Panel", "bg", new Color(0f, 0f, 0f, 0.88f)));
        Color buttonUpC = colorOr(theme, "Button", "bg", new Color(0.28f, 0.32f, 0.4f, 0.98f));
        if (theme != null && theme.getColor("Button", "bg") == null) {
            ThemeColor panel = theme.getColor("Panel", "bg");
            if (panel != null) {
                buttonUpC =
                        new Color(
                                Math.min(1f, panel.r + 0.2f),
                                Math.min(1f, panel.g + 0.22f),
                                Math.min(1f, panel.b + 0.28f),
                                0.98f);
            }
        }
        Color buttonDownC =
                new Color(
                        Math.min(1f, buttonUpC.r + 0.15f),
                        Math.min(1f, buttonUpC.g + 0.18f),
                        Math.min(1f, buttonUpC.b + 0.2f),
                        1f);
        Color buttonOverC =
                new Color(
                        Math.min(1f, buttonUpC.r + 0.1f),
                        Math.min(1f, buttonUpC.g + 0.12f),
                        Math.min(1f, buttonUpC.b + 0.15f),
                        1f);

        Color titleC = opaque(colorOr(theme, "Window", "title_color", cream()));
        Color labelC = opaque(colorOr(theme, "Label", "font_color", cream()));
        Color btnFontC = opaque(Color.WHITE);
        Color btnHoverC = opaque(colorOr(theme, "Button", "font_hover_color", gold()));
        Color sliderGrab = colorOr(theme, "Slider", "grabber", gold());
        Color sliderTrack = colorOr(theme, "Slider", "track", new Color(0.15f, 0.14f, 0.12f, 0.95f));

        Drawable windowBgD = white.tint(windowBg.cpy());
        Drawable buttonUp = white.tint(buttonUpC.cpy());
        Drawable buttonDown = white.tint(buttonDownC.cpy());
        Drawable buttonOver = white.tint(buttonOverC.cpy());

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = windowBgD;
        windowStyle.titleFont = any;
        windowStyle.titleFontColor = titleC.cpy();
        skin.add("default", windowStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = any;
        labelStyle.fontColor = labelC.cpy();
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonUp;
        buttonStyle.down = buttonDown;
        buttonStyle.over = buttonOver;
        buttonStyle.checked = buttonDown;
        buttonStyle.font = any;
        buttonStyle.fontColor = btnFontC.cpy();
        buttonStyle.downFontColor = btnHoverC.cpy();
        buttonStyle.overFontColor = btnHoverC.cpy();
        buttonStyle.checkedFontColor = btnFontC.cpy();
        skin.add("default", buttonStyle);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = white.tint(sliderTrack.cpy());
        sliderStyle.knob = white.tint(sliderGrab.cpy());
        sliderStyle.knobOver = white.tint(labelC.cpy());
        sliderStyle.knobDown = white.tint(btnHoverC.cpy());
        skin.add("default-horizontal", sliderStyle);
        skin.add("default-vertical", sliderStyle);

        Color panelBgC = colorOr(theme, "Panel", "bg", new Color(0.08f, 0.1f, 0.14f, 0.45f));
        if (panelBgC.a > 0.55f) {
            panelBgC = new Color(panelBgC.r, panelBgC.g, panelBgC.b, 0.45f);
        }
        skin.add("panel-bg", white.tint(panelBgC.cpy()));
        Color borderC = colorOr(theme, "Panel", "border_color", new Color(1f, 1f, 1f, 0.9f));
        skin.add("panel-border", white.tint(borderC.cpy()));

        return skin;
    }

    public static float[] panelBgRgba(Theme theme) {
        ThemeColor c = theme != null ? theme.getColor("Panel", "bg") : null;
        if (c == null) {
            return new float[] {0f, 0f, 0f, 0.88f};
        }
        return new float[] {c.r, c.g, c.b, c.a};
    }

    public static float[] labelFontRgba(Theme theme) {
        ThemeColor c = theme != null ? theme.getColor("Label", "font_color") : null;
        if (c == null) {
            return new float[] {1f, 1f, 1f, 1f};
        }
        return new float[] {c.r, c.g, c.b, c.a};
    }

    private static Color colorOr(Theme theme, String type, String name, Color fallback) {
        if (theme == null) {
            return fallback.cpy();
        }
        ThemeColor c = theme.getColor(type, name);
        if (c == null) {
            return fallback.cpy();
        }
        return new Color(c.r, c.g, c.b, c.a);
    }

    private static Color opaque(Color c) {
        if (c == null) {
            return Color.WHITE.cpy();
        }
        return new Color(c.r, c.g, c.b, 1f);
    }

    private static Color cream() {
        if (Settings.CREAM_COLOR != null) {
            return Settings.CREAM_COLOR.cpy();
        }
        return new Color(0.95f, 0.92f, 0.85f, 1f);
    }

    private static Color gold() {
        if (Settings.GOLD_COLOR != null) {
            return Settings.GOLD_COLOR.cpy();
        }
        return new Color(1f, 0.9f, 0.45f, 1f);
    }

    public static void resetFontsForTests() {
        // no cached fonts
    }
}
