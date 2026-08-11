package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;

/** ART positions the hand while STS renders each card's complete native chrome. */
final class Sts1HandCardRenderer {

    private Sts1HandCardRenderer() {}

    static void render(SpriteBatch sb, HandDrawPath.DrawItem item, AbstractCard card) {
        if (sb == null || item == null || card == null) {
            return;
        }
        try {
            card.current_x = item.x;
            card.current_y = item.y;
            card.angle = item.rotation;
            card.drawScale = item.scale > 0f ? item.scale : 1f;
            card.render(sb);
        } catch (Throwable ignored) {
            // A malformed external card must not break the host render loop.
        }
    }

}
