package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.assets.Sts1AssetMaterializer;
import artframework.render.RenderHosts;
import artframework.render.RenderHost;

import java.util.LinkedHashSet;
import java.util.Set;


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
            disableInactiveSurfaceEffects(plan);
            for (SurfaceDrawPlan.Entry e : plan.drawOrder()) {
                prepareSurfaceChrome(sb, e.surfaceId);
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

    private static void disableInactiveSurfaceEffects(SurfaceDrawPlan plan) {
        String[] surfaces = {
            SurfaceIds.COMBAT_HAND, SurfaceIds.COMBAT_CONTROLS, SurfaceIds.MAP, SurfaceIds.EVENT,
            SurfaceIds.SELECT_GRID, SurfaceIds.REWARD_COMBAT, SurfaceIds.REST, SurfaceIds.SHOP,
            SurfaceIds.TREASURE, SurfaceIds.COMBAT_PROCEED, SurfaceIds.TOP_PANEL,
            SurfaceIds.COMBAT_ENERGY, SurfaceIds.COMBAT_INTENTS
        };
        for (String sid : surfaces) {
            boolean active = false;
            for (SurfaceDrawPlan.Entry entry : plan.drawOrder()) {
                if (sid.equals(entry.surfaceId)) {
                    active = true;
                    break;
                }
            }
            RenderHosts.get().setC2SurfaceEnabled(sid, active);
            if (!active) {
                RenderHosts.get().removeC2Items(sid);
            }
        }
    }

    private static void prepareSurfaceChrome(SpriteBatch sb, String surfaceId) {
        if (surfaceId == null) {
            return;
        }
        float sw = com.megacrit.cardcrawl.core.Settings.WIDTH;
        float sh = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float x = sw * 0.18f;
        float y = sh * 0.18f;
        float w = sw * 0.64f;
        float h = sh * 0.64f;
        if (SurfaceIds.COMBAT_HAND.equals(surfaceId)) {
            x = 0f; y = 0f; w = sw; h = sh * 0.46f;
        } else if (SurfaceIds.COMBAT_CONTROLS.equals(surfaceId)) {
            x = sw * 0.76f; y = sh * 0.06f; w = sw * 0.2f; h = sh * 0.14f;
        } else if (SurfaceIds.TOP_PANEL.equals(surfaceId)) {
            x = 20f; y = sh - 82f; w = sw * 0.48f; h = 58f;
        } else if (SurfaceIds.COMBAT_ENERGY.equals(surfaceId)) {
            x = sw * 0.06f; y = sh * 0.11f; w = sw * 0.13f; h = 70f;
        } else if (SurfaceIds.COMBAT_INTENTS.equals(surfaceId)) {
            x = sw * 0.62f; y = sh * 0.52f; w = sw * 0.28f; h = sh * 0.28f;
        } else if (SurfaceIds.MAP.equals(surfaceId)) {
            x = sw * 0.08f; y = sh * 0.12f; w = sw * 0.84f; h = sh * 0.72f;
        }
        artframework.core.PresentChromeStyle chrome =
                artframework.core.PresentResolve.chromeForSurface(surfaceId);
        try {
            RenderHost host = RenderHosts.get();
            host.syncC2Surface(surfaceId, x, y, w, h);
            if (SurfaceIds.COMBAT_HAND.equals(surfaceId)
                    || SurfaceIds.COMBAT_CONTROLS.equals(surfaceId)) {
                host.setC2SurfaceEnabled(surfaceId, false);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static void renderHand(SpriteBatch sb) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
            RenderHosts.get().removeC2Items(SurfaceIds.COMBAT_HAND);
            return;
        }
        RenderHost host = RenderHosts.get();
        host.syncC2Surface(
                SurfaceIds.COMBAT_HAND,
                0f,
                0f,
                com.megacrit.cardcrawl.core.Settings.WIDTH,
                com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.46f);
        // Draw in projection order; hard-sync pose onto live card before render (16.4).
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (HandDrawPath.DrawItem item : HandDrawPath.buildFromProjection()) {
            if (!item.visible) {
                continue;
            }
            artframework.component.Rect bounds = item.bounds();
            host.syncC2Item(
                    SurfaceIds.COMBAT_HAND,
                    item.instanceId,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height);
            visibleItems.add(item.instanceId);
            Sts1HandCardRenderer.render(sb, item, find(item.instanceId));
        }
        host.retainC2Items(SurfaceIds.COMBAT_HAND, visibleItems);
    }

    /**
     * Controls chrome: when native end-turn is suppressed, draw a minimal label via FontHelper so
     * the button is not invisible. Full atlas chrome is HostAssets-driven later.
     */
    private static void renderControls(SpriteBatch sb) {
        if (!ControlsDrawPath.shouldSuppressNativeEndTurn()) {
            RenderHosts.get().removeC2Items(SurfaceIds.COMBAT_CONTROLS);
            return;
        }
        try {
            Set<String> visibleItems = new LinkedHashSet<String>();
            for (ControlsDrawPath.DrawItem item : ControlsDrawPath.buildFromProjection()) {
                if (!item.visible || !ControlsViewIdEndTurn(item.id)) {
                    continue;
                }
                artframework.component.Rect bounds = Sts1EndTurnChrome.bounds(
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT);
                RenderHosts.get().syncC2Item(
                        SurfaceIds.COMBAT_CONTROLS,
                        item.id,
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height);
                visibleItems.add(item.id);
                float x = bounds.x + bounds.width / 2f;
                float y = bounds.y + bounds.height / 2f;
                artframework.core.PresentChromeStyle chrome =
                        artframework.core.PresentResolve.chromeForSurface(SurfaceIds.COMBAT_CONTROLS);
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
                String nativeLabel = Sts1EndTurnChrome.label(item.text);
                String label = nativeLabel;
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
            RenderHosts.get().retainC2Items(SurfaceIds.COMBAT_CONTROLS, visibleItems);
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
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.MAP);
            Sts1VanillaDraw.draw(sb, artframework.assets.ResourceIds.MAP_BG_PREFIX + "act1",
                    0f, 0f, com.megacrit.cardcrawl.core.Settings.WIDTH,
                    com.megacrit.cardcrawl.core.Settings.HEIGHT);
            for (MapDrawPath.DrawItem item : MapDrawPath.buildFromProjection()) {
                String label = item.symbol != null && !item.symbol.isEmpty() ? item.symbol : "?";
                Texture art = Sts1AssetMaterializer.resolveTexture(item.artSource);
                if (art != null) {
                    float size = item.highlighted ? 80f : 64f;
                    if (item.taken) {
                        sb.setColor(chrome.disabledR, chrome.disabledG, chrome.disabledB, chrome.disabledA);
                    } else {
                        sb.setColor(chrome.labelR, chrome.labelG, chrome.labelB, chrome.labelA);
                    }
                    sb.draw(art, item.screenX - size / 2f, item.screenY - size / 2f, size, size);
                    if (item.highlighted) {
                        Sts1VanillaDraw.draw(sb,
                                artframework.assets.ResourceIds.mapOutline(item.roomKind),
                                item.screenX - size / 2f, item.screenY - size / 2f, size, size);
                    }
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
                            item.taken ? colorDisabled(chrome) : colorLabel(chrome));
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
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.EVENT);
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
                        colorLabel(chrome));
            }
            Sts1VanillaDraw.draw(sb, artframework.assets.ResourceIds.UI_EVENT_PANEL,
                    com.megacrit.cardcrawl.core.Settings.WIDTH * 0.18f,
                    com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.48f,
                    com.megacrit.cardcrawl.core.Settings.WIDTH * 0.64f,
                    com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.34f);
            for (EventDrawPath.DrawItem item : EventDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                String label = item.enabled ? item.label : (item.label + " (disabled)");
                Sts1VanillaDraw.draw(sb,
                        item.enabled ? artframework.assets.ResourceIds.UI_EVENT_BUTTON_ENABLED
                                : artframework.assets.ResourceIds.UI_EVENT_BUTTON_DISABLED,
                        item.x - item.w / 2f, item.y - item.h / 2f, item.w, item.h);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        label,
                        item.x,
                        item.y,
                        item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
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
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.SELECT_GRID);
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
                        item.selected || item.confirm ? colorAccent(chrome) : colorLabel(chrome));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void renderReward(SpriteBatch sb) {
        if (!RewardDrawPath.shouldSuppressNativeReward()) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.REWARD_COMBAT);
            for (RewardDrawPath.DrawItem item : RewardDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                Sts1VanillaDraw.draw(sb, artframework.assets.ResourceIds.UI_REWARD_ITEM_PANEL,
                        item.x - item.w / 2f, item.y - item.h / 2f, item.w, item.h);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        item.label,
                        item.x,
                        item.y,
                        item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void renderRest(SpriteBatch sb) {
        if (!RestDrawPath.shouldSuppressNativeRest()) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.REST);
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.5f;
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
            int i = 0;
            for (RestDrawPath.DrawItem item : RestDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                String icon = item.id.toLowerCase().contains("smith")
                        ? artframework.assets.ResourceIds.UI_CAMPFIRE_SMITH
                        : artframework.assets.ResourceIds.UI_CAMPFIRE_SLEEP;
                Sts1VanillaDraw.draw(sb, icon, x - 36f, y - i * 48f - 22f, 44f, 44f);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        item.label,
                        x,
                        y - i * 48f,
                        item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                i++;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void renderSkeleton(SpriteBatch sb) {
        if (!artframework.sts1.skeleton.Sts1SkeletonBridge.shouldDraw()) {
            return;
        }
        // Provider owns Spine type details. Preserve the host SpriteBatch contract around it.
        boolean drawing = false;
        try {
            drawing = sb.isDrawing();
            if (drawing) {
                sb.end();
            }
            artframework.sts1.skeleton.Sts1SkeletonBridge.renderAll(sb);
        } catch (Throwable ignored) {
        } finally {
            if (drawing) {
                try {
                    sb.begin();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void renderShop(SpriteBatch sb) {
        if (!ShopDrawPath.shouldSuppressNativeShop()) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.SHOP);
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.55f;
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
            int gold = ArtFramework.projection().shop().gold;
            com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                    sb,
                    com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                    "Gold " + gold,
                    x,
                    y + 64f,
                    colorAccent(chrome));
            int i = 0;
            for (ShopDrawPath.DrawItem item : ShopDrawPath.buildFromProjection()) {
                String label =
                        item.label
                                + " ("
                                + item.cost
                                + "g)"
                                + (item.soldOut ? " SOLD" : "");
                Sts1VanillaDraw.draw(sb, artframework.assets.ResourceIds.UI_REWARD_ITEM_PANEL,
                        x - 230f, y - i * 40f - 20f, 460f, 36f);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.cardDescFont_N,
                        label,
                        x,
                        y - i * 40f,
                        item.soldOut ? colorDisabled(chrome) : colorLabel(chrome));
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
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.TREASURE);
            artframework.context.TreasureView tv = ArtFramework.projection().treasure();
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.5f;
            String label =
                    tv.chestOpen
                            ? (tv.relicLabel.isEmpty() ? "Chest open" : tv.relicLabel)
                            : (tv.canOpen ? "Open chest" : "Chest");
            Sts1VanillaDraw.draw(sb, tv.chestOpen
                            ? artframework.assets.ResourceIds.UI_REWARD_CARD
                            : artframework.assets.ResourceIds.MAP_NODE_TREASURE,
                    x - 64f, y - 64f, 128f, 128f);
            com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                    sb,
                    com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                    label,
                    x,
                    y,
                    colorLabel(chrome));
        } catch (Throwable ignored) {
        }
    }

    private static void renderProceed(SpriteBatch sb) {
        if (!ProceedDrawPath.shouldSuppressNativeProceed()) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.COMBAT_PROCEED);
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
                        item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
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
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.TOP_PANEL);
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
                    colorLabel(chrome));
        } catch (Throwable ignored) {
        }
    }

    private static void renderEnergy(SpriteBatch sb) {
        if (!EnergyDrawPath.shouldSuppressNativeEnergy()
                && !Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_ENERGY)) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.COMBAT_ENERGY);
            int energy = ArtFramework.projection().controls().energy;
            float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.12f;
            float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.18f;
            com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                    sb,
                    com.megacrit.cardcrawl.helpers.FontHelper.energyNumFontRed,
                    String.valueOf(energy),
                    x,
                    y,
                    colorAccent(chrome));
        } catch (Throwable ignored) {
        }
    }

    private static void renderIntents(SpriteBatch sb) {
        if (!IntentDrawPath.shouldSuppressNativeIntents()
                && !Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.COMBAT_INTENTS)) {
            return;
        }
        for (IntentDrawPath.DrawItem item : IntentDrawPath.buildFromProjection()) {
            String key = item.iconResourceId == null || item.iconResourceId.isEmpty()
                    ? artframework.assets.ResourceIds.UI_COMBAT_INTENT_UNKNOWN
                    : item.iconResourceId;
            Sts1VanillaDraw.draw(sb, key, item.x - 32f, item.y - 32f, 64f, 64f);
            if (item.multiAmount > 0) {
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        String.valueOf(item.multiAmount), item.x, item.y - 42f, colorAccent(
                                artframework.core.PresentResolve.chromeForSurface(SurfaceIds.COMBAT_INTENTS)));
            }
        }
    }

    private static void renderEntityChrome(SpriteBatch sb) {
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chrome();
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
                        colorDisabled(chrome));
            }
        } catch (Throwable ignored) {
        }
    }

    private static Color colorLabel(artframework.core.PresentChromeStyle c) {
        return new Color(c.labelR, c.labelG, c.labelB, c.labelA);
    }

    private static Color colorDisabled(artframework.core.PresentChromeStyle c) {
        return new Color(c.disabledR, c.disabledG, c.disabledB, c.disabledA);
    }

    private static Color colorAccent(artframework.core.PresentChromeStyle c) {
        return new Color(c.accentR, c.accentG, c.accentB, c.accentA);
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
