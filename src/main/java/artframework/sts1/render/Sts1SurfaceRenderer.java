package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
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
            Set<String> activeSurfaces = new LinkedHashSet<String>();
            for (SurfaceDrawPlan.Entry entry : plan.drawOrder()) {
                activeSurfaces.add(entry.surfaceId);
            }
            prepareSurfaceVisuals(plan);
            artframework.render.RenderProjectionQueue.projectActiveSurfaces(activeSurfaces);
            RenderHosts.get().drawFrame(sb, true, RenderHost.kindsC2UnderPresent());
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
                    renderSelect(sb, e.surfaceId);
                } else if (SurfaceIds.REWARD_COMBAT.equals(e.surfaceId)
                        || SurfaceIds.REWARD_CARD.equals(e.surfaceId)
                        || SurfaceIds.REWARD_BOSS_RELIC.equals(e.surfaceId)) {
                    renderReward(sb, e.surfaceId);
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
            SurfaceIds.COMBAT_HAND, SurfaceIds.COMBAT_CARD_SLOTS, SurfaceIds.COMBAT_CONTROLS,
            SurfaceIds.MAP, SurfaceIds.EVENT, SurfaceIds.SELECT_GRID, SurfaceIds.SELECT_HAND,
            SurfaceIds.REWARD_COMBAT, SurfaceIds.REWARD_CARD, SurfaceIds.REWARD_BOSS_RELIC,
            SurfaceIds.REST, SurfaceIds.SHOP, SurfaceIds.TREASURE, SurfaceIds.COMBAT_PROCEED,
            SurfaceIds.TOP_PANEL, SurfaceIds.COMBAT_ENERGY, SurfaceIds.COMBAT_INTENTS
        };
        for (String sid : surfaces) {
            boolean active = false;
            for (SurfaceDrawPlan.Entry entry : plan.drawOrder()) {
                if (sid.equals(entry.surfaceId)) {
                    active = true;
                    break;
                }
            }
            artframework.render.RenderSurfaceComponent current =
                    artframework.render.RenderStateEcs.surfaceState(sid);
            if (current != null) {
                artframework.render.RenderStateEcs.surface(sid, current.bounds.x, current.bounds.y,
                        current.bounds.width, current.bounds.height, active);
                artframework.render.RenderStateEcs.surfaceEffects(sid, current.effects());
            }
            if (!active) {
                artframework.presentation.PresentationVisuals.removeC2Items(sid);
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
            artframework.component.Rect bounds = Sts1EndTurnChrome.bounds(sw, sh);
            x = bounds.x; y = bounds.y; w = bounds.width; h = bounds.height;
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
            artframework.render.RenderStateEcs.surface(surfaceId, x, y, w, h, false);
            // C2 surface bounds are layout regions, not pixel-precise chrome. Keep ambient
            // effects on item targets so a surface cannot paint a large fallback rectangle.
        } catch (RuntimeException ignored) {
        }
    }

    /** Materializes the current frame's C2 visual components before their effect band is drawn. */
    private static void prepareSurfaceVisuals(SurfaceDrawPlan plan) {
        for (SurfaceDrawPlan.Entry entry : plan.drawOrder()) {
            prepareSurfaceChrome(null, entry.surfaceId);
        }
        prepareHandVisuals(plan);
        prepareControlsVisuals(plan);
        prepareMapVisuals(plan);
        prepareEventVisuals(plan);
        prepareSelectVisuals(plan);
        prepareRewardVisuals(plan);
        prepareProceedVisuals(plan);
        prepareIntentVisuals(plan);
    }

    private static void prepareHandVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.COMBAT_HAND)) return;
        if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_HAND);
            return;
        }
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (HandDrawPath.DrawItem item : HandDrawPath.buildFromProjection()) {
            if (!item.visible) continue;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_HAND, item.instanceId, item.bounds(), 1f,
                    "card", item.artResourceId, item.cardId, item.visible);
            visibleItems.add(item.instanceId);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_HAND, visibleItems);
    }

    private static void prepareControlsVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.COMBAT_CONTROLS)) return;
        SurfaceDrawPlan.Entry entry = plan.find(SurfaceIds.COMBAT_CONTROLS);
        if (entry == null || entry.mode != SurfaceDrawPlan.DrawMode.DRAW) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_CONTROLS);
            return;
        }
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (ControlsDrawPath.DrawItem item : ControlsDrawPath.buildFromProjection()) {
            if (!item.visible || !ControlsViewIdEndTurn(item.id)) continue;
            artframework.component.Rect bounds = Sts1EndTurnChrome.bounds(
                    com.megacrit.cardcrawl.core.Settings.WIDTH,
                    com.megacrit.cardcrawl.core.Settings.HEIGHT);
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_CONTROLS, item.id, bounds, 2f,
                    "control", item.iconSource, item.text, item.visible);
            visibleItems.add(item.id);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_CONTROLS, visibleItems);
    }

    private static void prepareMapVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.MAP)) return;
        // NRO-03: native DungeonMapScreen.render stays the visual authority. Keep C2 input
        // items synced while the surface is in DRAW mode.
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (MapDrawPath.DrawItem item : MapDrawPath.buildFromProjection()) {
            float nodeSize = item.highlighted ? 80f : 64f;
            String itemId = "node:" + item.row + ":" + item.col;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.MAP, itemId,
                    new artframework.component.Rect(item.screenX - nodeSize / 2f,
                            item.screenY - nodeSize / 2f, nodeSize, nodeSize), 1f,
                    "map-node", item.artSource, item.symbol, true);
            visibleItems.add(itemId);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(SurfaceIds.MAP, visibleItems);
    }

    private static void prepareEventVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.EVENT)) return;
        // NRO-03: native GenericEventDialog.render stays the visual authority. Keep C2 input
        // items synced while the surface is in DRAW mode.
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (EventDrawPath.DrawItem item : EventDrawPath.buildFromProjection()) {
            if (!item.visible) continue;
            String itemId = "option:" + item.index;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.EVENT, itemId,
                    new artframework.component.Rect(item.x - item.w / 2f,
                            item.y - item.h / 2f, item.w, item.h), 1f,
                    "event-option", artframework.assets.ResourceIds.UI_EVENT_BUTTON_ENABLED,
                    item.label, item.visible);
            visibleItems.add(itemId);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(SurfaceIds.EVENT, visibleItems);
    }

    private static void prepareSelectVisuals(SurfaceDrawPlan plan) {
        // NRO-03: native GridCardSelectScreen/HandCardSelectScreen.render stay the visual
        // authority. Keep C2 input items synced while a select surface is in DRAW mode.
        boolean hasGrid = containsSurface(plan, SurfaceIds.SELECT_GRID);
        boolean hasHand = containsSurface(plan, SurfaceIds.SELECT_HAND);
        if (!hasGrid && !hasHand) {
            removeActiveItems(plan, SurfaceIds.SELECT_GRID, SurfaceIds.SELECT_HAND);
            return;
        }
        for (SurfaceDrawPlan.Entry entry : plan.drawOrder()) {
            if (!SurfaceIds.SELECT_GRID.equals(entry.surfaceId)
                    && !SurfaceIds.SELECT_HAND.equals(entry.surfaceId)) continue;
            Set<String> visibleItems = new LinkedHashSet<String>();
            for (SelectDrawPath.DrawItem item : SelectDrawPath.buildFromProjection()) {
                if (!item.visible) continue;
                String itemId = item.confirm ? "confirm" : "card:" + item.cardId;
                artframework.presentation.PresentationVisuals.syncC2Item(
                        entry.surfaceId, itemId,
                        new artframework.component.Rect(item.x - 48f, item.y - 32f, 96f, 64f), 1f,
                        item.confirm ? "select-confirm" : "select-card", "", item.cardId,
                        item.visible);
                visibleItems.add(itemId);
            }
            artframework.presentation.PresentationVisuals.retainC2Items(
                    entry.surfaceId, visibleItems);
        }
    }

    private static void prepareRewardVisuals(SurfaceDrawPlan plan) {
        // NRO-03: native CombatRewardScreen.render stays the visual authority. Keep C2 input
        // items synced while a reward surface is in DRAW mode.
        boolean hasReward = containsSurface(plan, SurfaceIds.REWARD_COMBAT)
                || containsSurface(plan, SurfaceIds.REWARD_CARD)
                || containsSurface(plan, SurfaceIds.REWARD_BOSS_RELIC);
        if (!hasReward) {
            removeActiveItems(plan, SurfaceIds.REWARD_COMBAT, SurfaceIds.REWARD_CARD,
                    SurfaceIds.REWARD_BOSS_RELIC);
            return;
        }
        for (SurfaceDrawPlan.Entry entry : plan.drawOrder()) {
            if (!SurfaceIds.REWARD_COMBAT.equals(entry.surfaceId)
                    && !SurfaceIds.REWARD_CARD.equals(entry.surfaceId)
                    && !SurfaceIds.REWARD_BOSS_RELIC.equals(entry.surfaceId)) continue;
            Set<String> visibleItems = new LinkedHashSet<String>();
            for (RewardDrawPath.DrawItem item : RewardDrawPath.buildFromProjection()) {
                if (!item.visible) continue;
                String itemId = "reward:" + item.index;
                artframework.presentation.PresentationVisuals.syncC2Item(
                        entry.surfaceId, itemId,
                        new artframework.component.Rect(item.x - item.w / 2f,
                                item.y - item.h / 2f, item.w, item.h), 1f,
                        "reward-item", artframework.assets.ResourceIds.UI_REWARD_ITEM_PANEL,
                        item.label, item.visible);
                visibleItems.add(itemId);
            }
            artframework.presentation.PresentationVisuals.retainC2Items(
                    entry.surfaceId, visibleItems);
        }
    }

    private static void prepareProceedVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.COMBAT_PROCEED)) return;
        if (!ProceedDrawPath.shouldSuppressNativeProceed()) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_PROCEED);
            return;
        }
        float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
        float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.2f;
        int i = 0;
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (ProceedDrawPath.DrawItem item : ProceedDrawPath.buildFromProjection()) {
            if (!item.visible) continue;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_PROCEED, item.id,
                    new artframework.component.Rect(x - 180f, y - i * 40f - 20f, 360f, 40f), 1f,
                    "proceed", "", item.text, item.visible);
            visibleItems.add(item.id);
            i++;
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_PROCEED, visibleItems);
    }

    private static void prepareIntentVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.COMBAT_INTENTS)) return;
        SurfaceDrawPlan.Entry entry = plan.find(SurfaceIds.COMBAT_INTENTS);
        if (entry == null || entry.mode != SurfaceDrawPlan.DrawMode.DRAW) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_INTENTS);
            return;
        }
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (IntentDrawPath.DrawItem item : IntentDrawPath.buildFromProjection()) {
            String itemId = "intent:" + item.monsterId;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_INTENTS, itemId,
                    new artframework.component.Rect(item.x - 32f, item.y - 32f, 64f, 64f), 1f,
                    "intent", item.iconResourceId, String.valueOf(item.multiAmount), true);
            visibleItems.add(itemId);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_INTENTS, visibleItems);
    }

    private static boolean containsSurface(SurfaceDrawPlan plan, String surfaceId) {
        for (SurfaceDrawPlan.Entry entry : plan.drawOrder()) {
            if (surfaceId.equals(entry.surfaceId)) return true;
        }
        return false;
    }

    private static void removeActiveItems(SurfaceDrawPlan plan, String... surfaceIds) {
        for (SurfaceDrawPlan.Entry entry : plan.drawOrder()) {
            for (String surfaceId : surfaceIds) {
                if (surfaceId.equals(entry.surfaceId)) {
                    artframework.presentation.PresentationVisuals.removeC2Items(surfaceId);
                }
            }
        }
    }

    private static void renderHand(SpriteBatch sb) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_HAND);
            return;
        }
        artframework.render.RenderStateEcs.surface(
                SurfaceIds.COMBAT_HAND,
                0f,
                0f,
                com.megacrit.cardcrawl.core.Settings.WIDTH,
                com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.46f,
                false);
        // The hand surface is only a parent for item-level effects. Drawing its ambient effect
        // across the whole upper combat region produces large fallback rectangles on devices.
        // Draw in projection order; hard-sync pose onto live card before render (16.4).
        long started = HandRenderMetrics.begin();
        Set<String> visibleItems = new LinkedHashSet<String>();
        int drawCount = 0;
        int projectedCount = 0;
        int renderedCards = 0;
        int missingArt = 0;
        int invalidBounds = 0;
        for (HandDrawPath.DrawItem item : HandDrawPath.buildFromProjection()) {
            projectedCount++;
            if (!item.visible) {
                continue;
            }
            artframework.component.Rect bounds = item.bounds();
            if (bounds.width <= 0f || bounds.height <= 0f || Float.isNaN(bounds.x)
                    || Float.isNaN(bounds.y)) invalidBounds++;
            if (!item.artFound && (item.artSource == null || item.artSource.isEmpty())) missingArt++;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_HAND,
                    item.instanceId,
                    bounds,
                    1f,
                    "card",
                    item.artResourceId,
                    item.cardId,
                    item.visible);
            visibleItems.add(item.instanceId);
            int cardDraws = Sts1HandCardRenderer.render(sb, item, find(item.instanceId));
            drawCount += cardDraws;
            if (cardDraws > 0) renderedCards++;
        }
        artframework.presentation.PresentationVisuals.retainC2Items(SurfaceIds.COMBAT_HAND, visibleItems);
        HandRenderMetrics.end(started, projectedCount, renderedCards, missingArt, invalidBounds);
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_HAND, drawCount);
    }

    /**
     * Controls surface: native EndTurnButton.render remains the visual authority. ART only
     * observes the invocation and keeps C2 input/hit items synced in prepareControlsVisuals.
     * No authoritative end-turn button pixels are drawn here.
     */
    private static void renderControls(SpriteBatch sb) {
        // NRO-02: EndTurnButton.render is the sole visual authority for the end-turn button.
    }

    private static boolean ControlsViewIdEndTurn(String id) {
        return artframework.context.ControlsView.END_TURN_ID.equals(id) || "end_turn".equals(id);
    }

    /**
     * Map surface: native DungeonMapScreen.render remains the visual authority. ART only
     * observes the invocation and keeps C2 input/hit items synced in prepareMapVisuals.
     * No authoritative map pixels are drawn here (NRO-03).
     */
    private static void renderMap(SpriteBatch sb) {
        // NRO-03: DungeonMapScreen.render is the sole visual authority for the map.
    }

    /**
     * Event surface: native GenericEventDialog.render remains the visual authority. ART only
     * observes the invocation and keeps C2 input/hit items synced in prepareEventVisuals.
     * No authoritative event pixels are drawn here (NRO-03).
     */
    private static void renderEvent(SpriteBatch sb) {
        // NRO-03: GenericEventDialog.render is the sole visual authority for events.
    }

    /**
     * Select surface: native GridCardSelectScreen/HandCardSelectScreen.render remain the visual
     * authority. ART only observes the invocation and keeps C2 input/hit items synced in
     * prepareSelectVisuals. No authoritative select pixels are drawn here (NRO-03).
     */
    private static void renderSelect(SpriteBatch sb, String surfaceId) {
        // NRO-03: native select renderers are the sole visual authority for selection screens.
    }

    /**
     * Reward surface: native CombatRewardScreen.render remains the visual authority. ART only
     * observes the invocation and keeps C2 input/hit items synced in prepareRewardVisuals.
     * No authoritative reward pixels are drawn here (NRO-03).
     */
    private static void renderReward(SpriteBatch sb, String surfaceId) {
        // NRO-03: CombatRewardScreen.render is the sole visual authority for rewards.
    }

    /**
     * Rest surface: native CampfireUI.render remains the visual authority. ART only observes
     * the invocation. No authoritative rest/campfire pixels are drawn here (NRO-03).
     */
    private static void renderRest(SpriteBatch sb) {
        // NRO-03: CampfireUI.render is the sole visual authority for the rest screen.
    }

    private static void renderSkeleton(SpriteBatch sb) {
        if (!artframework.sts1.skeleton.Sts1SkeletonBridge.shouldDraw()) {
            return;
        }
        // NRO-04: the provider renders ART-owned skeletons (or at the native slot for claimed
        // instances). No hand-drawn skeleton fallback/chrome pixels are ever drawn here.
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

    /**
     * Shop surface: native ShopScreen.render remains the visual authority. ART only observes
     * the invocation. No authoritative shop pixels are drawn here (NRO-03).
     */
    private static void renderShop(SpriteBatch sb) {
        // NRO-03: ShopScreen.render is the sole visual authority for the shop screen.
    }

    /**
     * Treasure surface: native TreasureRoom.render remains the visual authority. ART only
     * observes the invocation. No authoritative treasure pixels are drawn here (NRO-03).
     */
    private static void renderTreasure(SpriteBatch sb) {
        // NRO-03: TreasureRoom.render is the sole visual authority for treasure rooms.
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
                artframework.presentation.PresentationVisuals.syncC2Item(
                        SurfaceIds.COMBAT_PROCEED, item.id,
                        new artframework.component.Rect(x - 180f, y - i * 40f - 20f,
                                360f, 40f), 1f, "proceed", "", item.text, item.visible);
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

    /**
     * Energy surface: native EnergyPanel.render remains the visual authority. ART only observes
     * the invocation. No authoritative energy orb pixels are drawn here.
     */
    private static void renderEnergy(SpriteBatch sb) {
        // NRO-02: EnergyPanel.render is the sole visual authority for the energy orb.
    }

    /**
     * Intents surface: native AbstractMonster.renderIntent remains the visual authority. ART only
     * observes the invocation and keeps C2 input/hit items synced in prepareIntentVisuals. No
     * authoritative intent icon/amount pixels are drawn here.
     */
    private static void renderIntents(SpriteBatch sb) {
        // NRO-02: AbstractMonster.renderIntent is the sole visual authority for intents.
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
        if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null
                || AbstractDungeon.player.hand.group == null || instanceId == null) {
            return null;
        }
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
