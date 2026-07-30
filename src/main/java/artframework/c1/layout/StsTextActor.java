package artframework.c1.layout;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;

/**
 * Text drawn via STS {@link FontHelper} (same path as native UI), not scene2d Label.
 * Shared FontHelper BitmapFonts often fail under scene2d GlyphLayout on device.
 */
public final class StsTextActor extends Widget {

    private String text;
    private final boolean centered;
    private final Color color = new Color(1f, 1f, 1f, 1f);
    private float prefH;

    public StsTextActor(String text, boolean centered) {
        this.text = text != null ? text : "";
        this.centered = centered;
        float scale = Settings.scale > 0f ? Settings.scale : 1f;
        this.prefH = 28f * scale;
        setHeight(prefH);
        setWidth(Math.max(40f * scale, estimateWidth(this.text, scale)));
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        float scale = Settings.scale > 0f ? Settings.scale : 1f;
        setWidth(Math.max(getWidth(), estimateWidth(this.text, scale)));
    }

    public String getText() {
        return text;
    }

    public void setTextColor(float r, float g, float b, float a) {
        color.set(r, g, b, a);
    }

    @Override
    public float getPrefWidth() {
        return getWidth();
    }

    @Override
    public float getPrefHeight() {
        return prefH > 0f ? prefH : getHeight();
    }

    @Override
    public float getMinWidth() {
        return getPrefWidth();
    }

    @Override
    public float getMinHeight() {
        return getPrefHeight();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (text.isEmpty() || batch == null) {
            return;
        }
        if (!(batch instanceof SpriteBatch)) {
            return;
        }
        BitmapFont font = pickFont();
        if (font == null) {
            return;
        }
        SpriteBatch sb = (SpriteBatch) batch;
        Color prev = sb.getColor();
        Color draw = color.cpy();
        draw.a *= parentAlpha;
        if (draw.a <= 0.01f) {
            return;
        }
        try {
            // Parent-local coords: Stage SpriteBatch already applies actor transform.
            // Stage-absolute coords here double-transform and text vanishes off-screen.
            float x = getX();
            float y = getY();
            float w = getWidth();
            float h = getHeight();
            float cy = y + h * 0.5f;
            if (centered) {
                FontHelper.renderFontCentered(sb, font, text, x + w * 0.5f, cy, draw);
            } else {
                FontHelper.renderFontLeft(sb, font, text, x + 6f, cy, draw);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                sb.setColor(prev);
            } catch (Throwable ignored) {
            }
        }
    }

    private static BitmapFont pickFont() {
        if (FontHelper.buttonLabelFont != null) {
            return FontHelper.buttonLabelFont;
        }
        if (FontHelper.tipBodyFont != null) {
            return FontHelper.tipBodyFont;
        }
        return FontHelper.panelNameFont;
    }

    private static float estimateWidth(String s, float scale) {
        if (s == null || s.isEmpty()) {
            return 40f * scale;
        }
        // Approximate: buttonLabel is ~0.5em per char at 26px.
        return Math.max(48f * scale, s.length() * 12f * scale);
    }
}
