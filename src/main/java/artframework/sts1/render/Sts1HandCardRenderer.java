package artframework.sts1.render;

import artframework.sts1.assets.Sts1AssetMaterializer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.FontHelper;

/** ART-owned minimal STS1 card painter; it never calls or mutates {@link AbstractCard#render}. */
final class Sts1HandCardRenderer {

    private static final float WIDTH = 250f;
    private static final float HEIGHT = 350f;

    private Sts1HandCardRenderer() {}

    static void render(SpriteBatch sb, HandDrawPath.DrawItem item, AbstractCard card) {
        if (sb == null || item == null) {
            return;
        }
        float scale = item.scale > 0f ? item.scale : 1f;
        float x = item.x - WIDTH * scale / 2f;
        float y = item.y - HEIGHT * scale / 2f;
        Texture frame = Sts1AssetMaterializer.resolveTexture(item.frameSource);
        Texture art = Sts1AssetMaterializer.resolveTexture(item.artSource);
        try {
            if (art != null) {
                sb.draw(art, x + 18f * scale, y + 82f * scale, WIDTH * scale - 36f * scale, 180f * scale);
            } else if (card != null && card.portrait != null) {
                sb.draw(card.portrait, x + 18f * scale, y + 82f * scale, WIDTH * scale - 36f * scale, 180f * scale);
            }
            if (frame != null) {
                sb.draw(frame, x, y, WIDTH * scale, HEIGHT * scale);
            }
            if (card != null) {
                FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, card.name, item.x, y + HEIGHT * scale - 30f * scale);
                FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L, String.valueOf(card.cost), x + 32f * scale, y + HEIGHT * scale - 38f * scale);
            }
        } catch (Throwable ignored) {
            // A malformed external pack must not break the host render loop.
        }
    }
}
