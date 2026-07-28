package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import artframework.context.SurfaceIds;


/**
 * STS1 adapter renderer. Draw plan from {@link Sts1RenderPipeline}; card pixels still delegate to
 * live {@link AbstractCard#render} until 16.4 HostAssets path. ART owns phase and suppression.
 */
public final class Sts1SurfaceRenderer {

    private Sts1SurfaceRenderer() {}

    public static boolean shouldSuppressNativeHand() {
        return Sts1RenderPipeline.shouldSuppressNativeHand();
    }

    /** Draw full-present surfaces after the STS world render (PostRender). */
    public static void render(SpriteBatch sb) {
        if (sb == null) {
            return;
        }
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        BatchStateGuard guard = Sts1RenderPipeline.batchGuard();
        boolean drawing = false;
        try {
            drawing = sb.isDrawing();
        } catch (Throwable ignored) {
        }
        guard.beginCapture(drawing);
        try {
            for (SurfaceDrawPlan.Entry e : plan.drawOrder()) {
                if (SurfaceIds.COMBAT_HAND.equals(e.surfaceId)) {
                    renderHand(sb);
                }
                // slots / controls / map draw paths land in later 16.x slices
            }
        } finally {
            guard.endCapture();
        }
    }

    private static void renderHand(SpriteBatch sb) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
            return;
        }
        // Draw in projection order; hard-sync pose onto live card before render (16.4).
        for (HandDrawPath.DrawItem item : HandDrawPath.buildFromProjection()) {
            if (!item.visible) {
                continue;
            }
            AbstractCard card = find(item.instanceId);
            if (card == null) {
                continue;
            }
            card.current_x = item.x;
            card.current_y = item.y;
            card.angle = item.rotation;
            card.drawScale = item.scale;
            card.render(sb);
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
