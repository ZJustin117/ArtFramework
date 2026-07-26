package spireui.c1.skin;

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
 * scene2d {@link Skin} built from STS {@link FontHelper} / {@link ImageMaster} / {@link Settings}.
 * Call only after game assets are initialized (e.g. PostInitialize).
 */
public final class StsSkin {

    private StsSkin() {}

    public static Skin create() {
        if (ImageMaster.WHITE_SQUARE_IMG == null) {
            throw new IllegalStateException("ImageMaster not initialized");
        }
        BitmapFont titleFont = FontHelper.panelNameFont != null
                ? FontHelper.panelNameFont
                : FontHelper.buttonLabelFont;
        BitmapFont bodyFont = FontHelper.tipBodyFont != null
                ? FontHelper.tipBodyFont
                : FontHelper.buttonLabelFont;
        BitmapFont buttonFont = FontHelper.buttonLabelFont != null
                ? FontHelper.buttonLabelFont
                : bodyFont;
        if (titleFont == null || bodyFont == null || buttonFont == null) {
            throw new IllegalStateException("FontHelper not initialized");
        }

        Skin skin = new Skin();
        TextureRegion whiteRegion = new TextureRegion(ImageMaster.WHITE_SQUARE_IMG);
        TextureRegionDrawable white = new TextureRegionDrawable(whiteRegion);

        Drawable windowBg = white.tint(new Color(0f, 0f, 0f, 0.88f));
        Drawable buttonUp = white.tint(new Color(0.25f, 0.22f, 0.18f, 0.95f));
        Drawable buttonDown = white.tint(new Color(0.45f, 0.38f, 0.22f, 0.95f));
        Drawable buttonOver = white.tint(new Color(0.35f, 0.30f, 0.22f, 0.95f));

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = windowBg;
        windowStyle.titleFont = titleFont;
        windowStyle.titleFontColor = cream().cpy();
        skin.add("default", windowStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = bodyFont;
        labelStyle.fontColor = cream().cpy();
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonUp;
        buttonStyle.down = buttonDown;
        buttonStyle.over = buttonOver;
        buttonStyle.font = buttonFont;
        buttonStyle.fontColor = cream().cpy();
        buttonStyle.downFontColor = gold().cpy();
        buttonStyle.overFontColor = gold().cpy();
        skin.add("default", buttonStyle);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = white.tint(new Color(0.15f, 0.14f, 0.12f, 0.95f));
        sliderStyle.knob = white.tint(gold().cpy());
        sliderStyle.knobOver = white.tint(cream().cpy());
        sliderStyle.knobDown = white.tint(new Color(0.9f, 0.75f, 0.2f, 1f));
        skin.add("default-horizontal", sliderStyle);
        skin.add("default-vertical", sliderStyle);

        return skin;
    }

    private static Color cream() {
        if (Settings.CREAM_COLOR != null) {
            return Settings.CREAM_COLOR;
        }
        return Color.WHITE;
    }

    private static Color gold() {
        if (Settings.GOLD_COLOR != null) {
            return Settings.GOLD_COLOR;
        }
        return Color.YELLOW;
    }
}
