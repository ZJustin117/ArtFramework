package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.assets.Sts1AssetMaterializer;


/**
 * STS1 adapter renderer. ART owns the full-present card pixels and uses live cards only as a
 * read-only portrait/text bridge while the public projection remains host-agnostic.
 */
public final class Sts1SurfaceRenderer {

    private Sts1SurfaceRenderer() {}

    public static boolean shouldSuppressNativeHand() {
        return Sts1RenderPipeline.shouldSuppressNativeHand();
    }

    public static boolean shouldSuppressNativeEvent() {
        return EventDrawPath.shouldSuppressNativeEvent();
    }

    public static boolean shouldSuppressNativeSelect() {
        return SelectDrawPath.shouldSuppressNativeSelect();
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
            if (artframework.sts1.PresentSafety.isPanic()) {
                return;
            }
            for (SurfaceDrawPlan.Entry e : plan.drawOrder()) {
                if (SurfaceIds.COMBAT_HAND.equals(e.surfaceId)) {
                    renderHand(sb);
                } else if (SurfaceIds.COMBAT_CONTROLS.equals(e.surfaceId)) {
                    renderControls(sb);
                } else if (SurfaceIds.MAP.equals(e.surfaceId)) {
                    renderMap(sb);
                } else if (SurfaceIds.EVENT.equals(e.surfaceId)) {
                    renderEvent(sb);
                } else if (SurfaceIds.SELECT_GRID.equals(e.surfaceId)
                        || SurfaceIds.SELECT_HAND.equals(e.surfaceId)) {
                    renderSelect(sb);
                }
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
            Sts1HandCardRenderer.render(sb, item, find(item.instanceId));
        }
    }

    /**
     * Controls chrome: when native end-turn is suppressed, draw a minimal label via FontHelper so
     * the button is not invisible. Full atlas chrome is HostAssets-driven later.
     */
    private static void renderControls(SpriteBatch sb) {
        if (!ControlsDrawPath.shouldSuppressNativeEndTurn()) {
            return;
        }
        try {
            for (ControlsDrawPath.DrawItem item : ControlsDrawPath.buildFromProjection()) {
                if (!item.visible || !ControlsViewIdEndTurn(item.id)) {
                    continue;
                }
                float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.85f;
                float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.12f;
                Texture icon = Sts1AssetMaterializer.resolveTexture(item.iconSource);
                if (icon != null) {
                    float w = icon.getWidth() * 0.75f;
                    float h = icon.getHeight() * 0.75f;
                    sb.setColor(item.enabled ? Color.WHITE : Color.GRAY);
                    sb.draw(icon, x - w / 2f, y - h / 2f, w, h);
                    sb.setColor(Color.WHITE);
                }
                String label = item.enabled ? item.text : (item.text + " (disabled)");
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        label,
                        x,
                        y,
                        item.enabled
                                ? com.badlogic.gdx.graphics.Color.WHITE
                                : com.badlogic.gdx.graphics.Color.GRAY);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean ControlsViewIdEndTurn(String id) {
        return artframework.context.ControlsView.END_TURN_ID.equals(id) || "end_turn".equals(id);
    }

    /** Map nodes as text symbols when native map is suppressed (atlas path later). */
    private static void renderMap(SpriteBatch sb) {
        if (!MapDrawPath.shouldSuppressNativeMap()) {
            return;
        }
        try {
            for (MapDrawPath.DrawItem item : MapDrawPath.buildFromProjection()) {
                String label = item.symbol != null && !item.symbol.isEmpty() ? item.symbol : "?";
                Texture art = Sts1AssetMaterializer.resolveTexture(item.artSource);
                if (art != null) {
                    float size = item.highlighted ? 80f : 64f;
                    sb.setColor(item.taken ? Color.DARK_GRAY : Color.WHITE);
                    sb.draw(art, item.screenX - size / 2f, item.screenY - size / 2f, size, size);
                    sb.setColor(Color.WHITE);
                } else {
                    if (item.highlighted) {
                        label = "[" + label + "]";
                    }
                    com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb,
                            com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                            label,
                            item.screenX,
                            item.screenY,
                            item.taken
                                    ? com.badlogic.gdx.graphics.Color.DARK_GRAY
                                    : com.badlogic.gdx.graphics.Color.WHITE);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** Event option labels when native event chrome is suppressed (22.3). */
    private static void renderEvent(SpriteBatch sb) {
        if (!EventDrawPath.shouldSuppressNativeEvent()) {
            return;
        }
        try {
            String title = ArtFramework.projection().event().title;
            if (title != null && !title.isEmpty()) {
                float tx = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
                float ty = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.55f;
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        title,
                        tx,
                        ty,
                        Color.WHITE);
            }
            for (EventDrawPath.DrawItem item : EventDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                String label = item.enabled ? item.label : (item.label + " (disabled)");
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        label,
                        item.x,
                        item.y,
                        item.enabled ? Color.WHITE : Color.GRAY);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Select pool + confirm chrome when native select is suppressed (22.3). */
    private static void renderSelect(SpriteBatch sb) {
        if (!SelectDrawPath.shouldSuppressNativeSelect()) {
            return;
        }
        try {
            for (SelectDrawPath.DrawItem item : SelectDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                String label =
                        item.confirm
                                ? item.cardId
                                : (item.selected ? "[" + item.cardId + "]" : item.cardId);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        label,
                        item.x,
                        item.y,
                        item.selected || item.confirm ? Color.GOLD : Color.WHITE);
            }
        } catch (Throwable ignored) {
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
