package artframework.sts1.render;

import artframework.core.PresentChromeStyle;
import artframework.core.PresentResolve;
import artframework.sts1.assets.Sts1AssetMaterializer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.FontHelper;

/** ART-owned minimal STS1 card painter; it never calls or mutates {@link AbstractCard#render}. */
final class Sts1HandCardRenderer {

    private Sts1HandCardRenderer() {}

    static void render(SpriteBatch sb, HandDrawPath.DrawItem item, AbstractCard card) {
        if (sb == null || item == null) {
            return;
        }
        PresentChromeStyle chrome =
                PresentResolve.chromeForSurface(artframework.context.SurfaceIds.COMBAT_HAND);
        float alpha = chrome.cardAlpha > 0f && chrome.cardAlpha <= 1f ? chrome.cardAlpha : 1f;
        float scale = item.scale > 0f ? item.scale : 1f;
        artframework.component.Rect bounds = item.bounds();
        float x = bounds.x;
        float y = bounds.y;
        float w = bounds.width;
        float h = bounds.height;
        Texture frame = Sts1AssetMaterializer.resolveTexture(item.frameSource);
        Texture art = Sts1AssetMaterializer.resolveTexture(item.artSource);
        Color prev = sb.getColor();
        try {
            sb.setColor(1f, 1f, 1f, alpha);
            if (art != null) {
                sb.draw(art, x + 18f * scale, y + 82f * scale, w - 36f * scale, 180f * scale);
            } else if (card != null && card.portrait != null) {
                sb.draw(card.portrait, x + 18f * scale, y + 82f * scale, w - 36f * scale, 180f * scale);
            }
            if (frame != null) {
                sb.draw(frame, x, y, w, h);
            }
            if (card != null) {
                Color label =
                        new Color(chrome.labelR, chrome.labelG, chrome.labelB, chrome.labelA * alpha);
                FontHelper.renderFontCentered(
                        sb, FontHelper.cardTitleFont, card.name, item.x, y + h - 30f * scale, label);
                FontHelper.renderFontCentered(
                        sb,
                        FontHelper.cardEnergyFont_L,
                        String.valueOf(card.cost),
                        x + 32f * scale,
                        y + h - 38f * scale,
                        label);
            }
        } catch (Throwable ignored) {
            // A malformed external pack must not break the host render loop.
        } finally {
            try {
                sb.setColor(prev);
            } catch (Throwable ignored) {
            }
        }
    }

}
