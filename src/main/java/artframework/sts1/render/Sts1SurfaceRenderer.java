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
                } else if (SurfaceIds.REWARD_COMBAT.equals(e.surfaceId)
                        || SurfaceIds.REWARD_CARD.equals(e.surfaceId)
                        || SurfaceIds.REWARD_BOSS_RELIC.equals(e.surfaceId)) {
                    renderReward(sb);
                } else if (SurfaceIds.REST.equals(e.surfaceId)) {
                    renderRest(sb);
                } else if (SurfaceIds.SHOP.equals(e.surfaceId)) {
                    renderShop(sb);
                } else if (SurfaceIds.TREASURE.equals(e.surfaceId)) {
                    renderTreasure(sb);
                } else if (SurfaceIds.SKELETON.equals(e.surfaceId)) {
                    renderSkeleton(sb);
                } else if (SurfaceIds.TOP_PANEL.equals(e.surfaceId)) {
                    renderTopPanel(sb);
                } else if (SurfaceIds.COMBAT_ENERGY.equals(e.surfaceId)) {
                    renderEnergy(sb);
                } else if (SurfaceIds.COMBAT_INTENTS.equals(e.surfaceId)) {
                    renderIntents(sb);
                } else if (SurfaceIds.COMBAT_PROCEED.equals(e.surfaceId)) {
                    renderProceed(sb);
                }
            }
            renderEntityChrome(sb);
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
                artframework.core.PresentChromeStyle chrome =
                        artframework.core.PresentResolve.chrome();
                Texture icon = Sts1AssetMaterializer.resolveTexture(item.iconSource);
                if (icon != null) {
                    float w = icon.getWidth() * 0.75f;
                    float h = icon.getHeight() * 0.75f;
                    if (item.enabled) {
                        sb.setColor(chrome.labelR, chrome.labelG, chrome.labelB, chrome.labelA);
                    } else {
                        sb.setColor(
                                chrome.disabledR, chrome.disabledG, chrome.disabledB, chrome.disabledA);
                    }
                    sb.draw(icon, x - w / 2f, y - h / 2f, w, h);
                    sb.setColor(Color.WHITE);
                }
                String label = item.enabled ? item.text : (item.text + " (disabled)");
                com.badlogic.gdx.graphics.Color fontColor =
                        item.enabled
                                ? new com.badlogic.gdx.graphics.Color(
                                        chrome.labelR, chrome.labelG, chrome.labelB, chrome.labelA)
                                : new com.badlogic.gdx.graphics.Color(
                                        chrome.disabledR,
                                        chrome.disabledG,
                                        chrome.disabledB,
                                        chrome.disabledA);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        label,
                        x,
                        y,
                        fontColor);
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

    private static void renderReward(SpriteBatch sb) {
        if (!RewardDrawPath.shouldSuppressNativeReward()) {
            return;
        }
        try {
            for (RewardDrawPath.DrawItem item : RewardDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        item.label,
                        item.x,
                        item.y,
                        item.enabled ? Color.WHITE : Color.GRAY);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void renderRest(SpriteBatch sb) {
        if (!RestDrawPath.shouldSuppressNativeRest()) {
            return;
        }
        try {
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.5f;
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
            int i = 0;
            for (RestDrawPath.DrawItem item : RestDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        item.label,
                        x,
                        y - i * 48f,
                        item.enabled ? Color.WHITE : Color.GRAY);
                i++;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void renderSkeleton(SpriteBatch sb) {
        if (!artframework.sts1.skeleton.Sts1SkeletonBridge.shouldDraw()) {
            return;
        }
        // Host Spine draw is provider-owned; bridge records live handles for probe.
    }

    private static void renderShop(SpriteBatch sb) {
        if (!ShopDrawPath.shouldSuppressNativeShop()) {
            return;
        }
        try {
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.55f;
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
            int gold = ArtFramework.projection().shop().gold;
            com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                    sb,
                    com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                    "Gold " + gold,
                    x,
                    y + 64f,
                    Color.GOLD);
            int i = 0;
            for (ShopDrawPath.DrawItem item : ShopDrawPath.buildFromProjection()) {
                String label =
                        item.label
                                + " ("
                                + item.cost
                                + "g)"
                                + (item.soldOut ? " SOLD" : "");
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.cardDescFont_N,
                        label,
                        x,
                        y - i * 40f,
                        item.soldOut ? Color.GRAY : Color.WHITE);
                i++;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void renderTreasure(SpriteBatch sb) {
        if (!TreasureDrawPath.shouldSuppressNativeTreasure()) {
            return;
        }
        try {
            artframework.context.TreasureView tv = ArtFramework.projection().treasure();
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.5f;
            String label =
                    tv.chestOpen
                            ? (tv.relicLabel.isEmpty() ? "Chest open" : tv.relicLabel)
                            : (tv.canOpen ? "Open chest" : "Chest");
            com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                    sb,
                    com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                    label,
                    x,
                    y,
                    Color.WHITE);
        } catch (Throwable ignored) {
        }
    }

    private static void renderProceed(SpriteBatch sb) {
        if (!ProceedDrawPath.shouldSuppressNativeProceed()) {
            return;
        }
        try {
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.2f;
            int i = 0;
            for (ProceedDrawPath.DrawItem item : ProceedDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        item.text,
                        x,
                        y - i * 40f,
                        item.enabled ? Color.WHITE : Color.GRAY);
                i++;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void renderTopPanel(SpriteBatch sb) {
        if (!TopPanelDrawPath.shouldSuppressNativeTopPanel()
                && !Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.TOP_PANEL)) {
            return;
        }
        try {
            artframework.context.TopPanelView tv = ArtFramework.projection().topPanel();
            if (!tv.available) {
                return;
            }
            String line =
                    tv.characterName
                            + "  HP "
                            + tv.hp
                            + "/"
                            + tv.maxHp
                            + "  Gold "
                            + tv.gold
                            + "  Floor "
                            + tv.floor;
            com.megacrit.cardcrawl.helpers.FontHelper.renderFontLeftTopAligned(
                    sb,
                    com.megacrit.cardcrawl.helpers.FontHelper.topPanelInfoFont,
                    line,
                    40f,
                    com.megacrit.cardcrawl.core.Settings.HEIGHT - 40f,
                    Color.WHITE);
        } catch (Throwable ignored) {
        }
    }

    private static void renderEnergy(SpriteBatch sb) {
        if (!EnergyDrawPath.shouldSuppressNativeEnergy()
                && !Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_ENERGY)) {
            return;
        }
        try {
            int energy = ArtFramework.projection().controls().energy;
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.12f;
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.18f;
            com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                    sb,
                    com.megacrit.cardcrawl.helpers.FontHelper.energyNumFontRed,
                    String.valueOf(energy),
                    x,
                    y,
                    Color.CYAN);
        } catch (Throwable ignored) {
        }
    }

    private static void renderIntents(SpriteBatch sb) {
        if (!IntentDrawPath.shouldSuppressNativeIntents()
                && !Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_INTENTS)) {
            return;
        }
        try {
            for (IntentDrawPath.DrawItem item : IntentDrawPath.buildFromProjection()) {
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.cardDescFont_N,
                        item.monsterName + ":" + item.intentType,
                        item.x,
                        item.y,
                        Color.ORANGE);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void renderEntityChrome(SpriteBatch sb) {
        try {
            for (artframework.c2.EntityDrawPath.DrawItem item :
                    artframework.c2.EntityDrawPath.buildFromPresent()) {
                if (!item.visible || item.overlayOnly) {
                    continue;
                }
                String label =
                        item.label.isEmpty()
                                ? (item.kind + ":" + item.refId)
                                : item.label;
                if (item.hp > 0 || item.maxHp > 0) {
                    label = label + " " + item.hp + "/" + item.maxHp;
                }
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.cardDescFont_N,
                        label,
                        item.x,
                        item.y,
                        Color.LIGHT_GRAY);
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
