package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import artframework.api.ArtFramework;
import artframework.context.CardEntity;
import artframework.context.CardZone;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

/**
 * STS1 adapter renderer. The first vertical slice delegates card visual details to the live
 * card renderer, but ART owns when and in which render phase those instances are drawn.
 */
public final class Sts1SurfaceRenderer {

    private Sts1SurfaceRenderer() {}

    public static boolean shouldSuppressNativeHand() {
        return FullPresentMode.isCombatHandEnabled()
                && ArtFramework.component(SurfaceIds.COMBAT_HAND) != null
                && ArtFramework.component(SurfaceIds.COMBAT_HAND).isMounted()
                && "combat".equals(ArtFramework.projection().scene());
    }

    /** Draw the hard-synced hand after the STS world render. */
    public static void render(SpriteBatch sb) {
        if (sb == null || !shouldSuppressNativeHand() || AbstractDungeon.player == null
                || AbstractDungeon.player.hand == null) {
            return;
        }
        for (CardEntity entity : ArtFramework.projection().listZone(CardZone.HAND)) {
            AbstractCard card = find(entity.instanceId);
            if (card != null) {
                card.render(sb);
            }
        }
    }

    private static AbstractCard find(String instanceId) {
        for (AbstractCard card : AbstractDungeon.player.hand.group) {
            if (card == null) {
                continue;
            }
            String id = card.uuid != null
                    ? card.uuid.toString()
                    : card.cardID + "@" + Integer.toHexString(System.identityHashCode(card));
            if (instanceId.equals(id)) {
                return card;
            }
        }
        return null;
    }
}
