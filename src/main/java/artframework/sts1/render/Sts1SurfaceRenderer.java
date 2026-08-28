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
 * STS1 adapter renderer. ART owns full-present hand layout, projection, input, and overlays;
 * live, unpatched {@code AbstractCard.render} remains responsible for card pixels.
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
        // Targeting arrow is drawn above the normal surface list so it paints over
        // C2 stage items. The surface loop already recorded its presence; the actual
        // geometry is rendered here as a tail-slot overlay.
        renderTargetingOverlay(sb);
        renderEntityChrome(sb);
    }

    private static void disableInactiveSurfaceEffects(SurfaceDrawPlan plan) {
        String[] surfaces = {
            SurfaceIds.COMBAT_HAND, SurfaceIds.COMBAT_CARD_SLOTS, SurfaceIds.COMBAT_CONTROLS,
            SurfaceIds.MAP, SurfaceIds.EVENT, SurfaceIds.SELECT_GRID, SurfaceIds.SELECT_HAND,
            SurfaceIds.REWARD_COMBAT, SurfaceIds.REWARD_CARD, SurfaceIds.REWARD_BOSS_RELIC,
            SurfaceIds.REST, SurfaceIds.SHOP, SurfaceIds.TREASURE, SurfaceIds.COMBAT_PROCEED,
            SurfaceIds.TOP_PANEL, SurfaceIds.COMBAT_ENERGY, SurfaceIds.COMBAT_INTENTS,
            SurfaceIds.COMBAT_TARGETING
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
            artframework.component.Rect bounds = ControlsDrawPath.endTurnProjectedBounds(sw, sh);
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
        prepareRestVisuals(plan);
        prepareShopVisuals(plan);
        prepareTreasureVisuals(plan);
        prepareProceedVisuals(plan);
        prepareIntentVisuals(plan);
        prepareEnergyVisuals(plan);
        prepareTopPanelVisuals(plan);
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
            if (!item.visible || !ControlsDrawPath.isEndTurnItem(item.id)) continue;
            artframework.component.Rect bounds = ControlsDrawPath.endTurnProjectedBounds(
                    com.megacrit.cardcrawl.core.Settings.WIDTH,
                    com.megacrit.cardcrawl.core.Settings.HEIGHT);
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_CONTROLS, item.id, bounds, 2f,
                     "control", item.resourceId, item.text, item.visible);
            visibleItems.add(item.id);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_CONTROLS, visibleItems);
    }

    private static void prepareMapVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.MAP)) return;
        // Map is ART_DELEGATED when FULL_READY, but the current HostAssets node path is not full
        // native parity. Keep C2 input items synced and let strict evidence expose supply gaps.
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
        // Event dialog is ART_DELEGATED when FULL_READY; base dialog pixels are not reproduced
        // yet, so the strict ledger must keep exposing the pixel-supply gap.
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (EventDrawPath.DrawItem item : EventDrawPath.buildFromProjection()) {
            if (!item.visible) continue;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.EVENT, item.id,
                    new artframework.component.Rect(item.x - item.w / 2f,
                            item.y - item.h / 2f, item.w, item.h), 1f,
                    item.role, item.resourceId,
                    item.label, item.visible);
            visibleItems.add(item.id);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(SurfaceIds.EVENT, visibleItems);
    }

    private static void prepareSelectVisuals(SurfaceDrawPlan plan) {
        // Select screens are ART_DELEGATED when FULL_READY; base selection pixels are not fully
        // reproduced yet, so keep C2 input items synced and expose supply gaps in strict reports.
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
                String itemId = item.confirm ? "confirm" : "card:" + item.instanceId;
                artframework.presentation.PresentationVisuals.syncC2Item(
                        entry.surfaceId, itemId,
                        new artframework.component.Rect(item.x - item.w / 2f, item.y - item.h / 2f,
                                item.w, item.h), 1f,
                        item.confirm ? "select-confirm" : "select-card", item.resourceId, item.cardId,
                        item.visible);
                visibleItems.add(itemId);
            }
            artframework.presentation.PresentationVisuals.retainC2Items(
                    entry.surfaceId, visibleItems);
        }
    }

    private static void prepareRewardVisuals(SurfaceDrawPlan plan) {
        // Reward screen is ART_DELEGATED when FULL_READY; this is minimal projected item chrome,
        // not complete native reward parity, so strict evidence keeps missing base pixels visible.
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
                        "reward-item",
                        item.resourceId,
                        item.label, item.visible);
                visibleItems.add(itemId);
            }
            artframework.presentation.PresentationVisuals.retainC2Items(
                    entry.surfaceId, visibleItems);
        }
    }

    private static void prepareProceedVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.COMBAT_PROCEED)) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_PROCEED);
            return;
        }
        if (!ProceedDrawPath.shouldSuppressNativeProceed()) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_PROCEED);
            return;
        }
        float sw = com.megacrit.cardcrawl.core.Settings.WIDTH;
        float sh = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (ProceedDrawPath.DrawItem item : ProceedDrawPath.buildFromProjection()) {
            if (!item.visible) continue;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_PROCEED, item.id, item.bounds(sw, sh), 1f,
                    "proceed", item.resourceId, item.text, item.visible);
            visibleItems.add(item.id);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_PROCEED, visibleItems);
    }

    /**
     * Slice C phase 2: materializes the rest chrome rows (title + live campfire options) as C2
     * items while rest is ART_DELEGATED; clears them when native keeps the surface. This is
     * minimal text chrome, not complete campfire pixel parity.
     */
    static void prepareRestVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.REST)) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.REST);
            return;
        }
        if (!RestDrawPath.shouldSuppressNativeRest()) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.REST);
            return;
        }
        syncRoomChromeItems(
                SurfaceIds.REST, RestDrawPath.chromeLines(), "rest-title", "rest-option");
    }

    /** Slice C phase 2: shop chrome rows (title/gold/entries/purge) from the live ShopView. */
    static void prepareShopVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.SHOP)) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.SHOP);
            return;
        }
        if (!ShopDrawPath.shouldSuppressNativeShop()) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.SHOP);
            return;
        }
        syncRoomChromeItems(
                SurfaceIds.SHOP, ShopDrawPath.chromeLines(), "shop-title", "shop-entry");
    }

    /** Slice C phase 2: treasure chrome rows (title + chest/relic state) from TreasureView. */
    static void prepareTreasureVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.TREASURE)) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.TREASURE);
            return;
        }
        if (!TreasureDrawPath.shouldSuppressNativeTreasure()) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.TREASURE);
            return;
        }
        syncRoomChromeItems(
                SurfaceIds.TREASURE, TreasureDrawPath.chromeLines(),
                "treasure-title", "treasure-item");
    }

    /** Shared single-column layout for room text chrome rows. */
    private static void syncRoomChromeItems(
            String surfaceId,
            java.util.List<RoomChromeLine> lines,
            String titleRole,
            String rowRole) {
        Set<String> visibleItems = new LinkedHashSet<String>();
        int i = 0;
        for (RoomChromeLine line : lines) {
            if (!line.visible) {
                i++;
                continue;
            }
            boolean title = "title".equals(line.id);
            artframework.presentation.PresentationVisuals.syncC2Item(
                    surfaceId,
                    line.id,
                    roomLineBounds(line, i),
                    1f,
                    !line.role.isEmpty() ? line.role : (title ? titleRole : rowRole),
                    !line.resourceId.isEmpty() ? line.resourceId
                            : (title || line.enabled
                                    ? ""
                                    : artframework.assets.ResourceIds.UI_EVENT_BUTTON_DISABLED),
                    line.text,
                    true);
            visibleItems.add(line.id);
            i++;
        }
        artframework.presentation.PresentationVisuals.retainC2Items(surfaceId, visibleItems);
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
                     item.bounds, 1f,
                     "intent", item.iconResourceId, item.text, true);
            visibleItems.add(itemId);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_INTENTS, visibleItems);
    }

    private static void prepareEnergyVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.COMBAT_ENERGY)) return;
        SurfaceDrawPlan.Entry entry = plan.find(SurfaceIds.COMBAT_ENERGY);
        if (entry == null || entry.mode != SurfaceDrawPlan.DrawMode.DRAW) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_ENERGY);
            return;
        }
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (EnergyDrawPath.DrawItem item : EnergyDrawPath.buildFromProjection()) {
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_ENERGY, item.id,
                     item.bounds, 1f, "energy", item.resourceId, item.label, true);
            visibleItems.add(item.id);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_ENERGY, visibleItems);
    }

    private static void prepareTopPanelVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.TOP_PANEL)) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.TOP_PANEL);
            return;
        }
        SurfaceDrawPlan.Entry entry = plan.find(SurfaceIds.TOP_PANEL);
        if (entry == null || entry.mode != SurfaceDrawPlan.DrawMode.DRAW) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.TOP_PANEL);
            return;
        }
        Set<String> visibleItems = new LinkedHashSet<String>();
        float sw = com.megacrit.cardcrawl.core.Settings.WIDTH;
        float sh = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        for (TopPanelDrawPath.DrawItem item : TopPanelDrawPath.buildFromProjection()) {
            if (!item.visible) continue;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.TOP_PANEL, item.id, item.bounds(sw, sh), 1f,
                    "top_panel", item.resourceId, item.text, item.visible);
            visibleItems.add(item.id);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.TOP_PANEL, visibleItems);
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
     * Controls surface: ART_DELEGATED when FULL_READY. The C2 item was already synced in
     * prepareControlsVisuals and drawn by the global RenderHosts.drawFrame pass, but current
     * supply includes the projected button texture; native hover/animation parity remains pending.
     */
    private static void renderControls(SpriteBatch sb) {
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_CONTROLS,
                ControlsDrawPath.materializedDrawCount());
    }

    /**
     * Map surface: ART_DELEGATED when FULL_READY. The C2 items were synced in prepareMapVisuals
     * and are drawn by the global RenderHosts.drawFrame pass; full native parity remains tracked
     * as an exposed pixel-supply gap.
     */
    private static void renderMap(SpriteBatch sb) {
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.MAP, MapDrawPath.buildFromProjection().size());
    }

    /**
     * Event surface: ART_DELEGATED when FULL_READY. The C2 items were synced in prepareEventVisuals
     * and are drawn by the global RenderHosts.drawFrame pass; base dialog pixels remain an exposed
     * supply gap.
     */
    private static void renderEvent(SpriteBatch sb) {
        artframework.core.PresentChromeStyle chrome = resolveSurfaceChrome(SurfaceIds.EVENT);
        try {
            for (EventDrawPath.DrawItem item : EventDrawPath.buildFromProjection()) {
                if (!item.visible) continue;
                try {
                    artframework.component.Rect b = new artframework.component.Rect(
                            item.x - item.w / 2f, item.y - item.h / 2f, item.w, item.h);
                    drawResolvedTexture(sb, item.resourceId, b);
                    if (!item.label.isEmpty()) com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb, com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont, item.label,
                            item.x, item.y, item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) {
        } finally {
            NativeRenderBridge.recordSurfaceDraw(SurfaceIds.EVENT,
                    EventDrawPath.materializedDrawCount());
        }
    }

    /**
     * Select surface: ART_DELEGATED when FULL_READY. The C2 items were synced in
     * prepareSelectVisuals and are drawn by the global RenderHosts.drawFrame pass; full base-pixel
     * parity remains an exposed supply gap.
     */
    private static void renderSelect(SpriteBatch sb, String surfaceId) {
        artframework.core.PresentChromeStyle chrome = resolveSurfaceChrome(surfaceId);
        try {
            for (SelectDrawPath.DrawItem item : SelectDrawPath.buildFromProjection()) {
                if (!item.visible) continue;
                try {
                    artframework.component.Rect b = new artframework.component.Rect(
                            item.x - item.w / 2f, item.y - item.h / 2f, item.w, item.h);
                    drawResolvedTexture(sb, item.resourceId, b);
                    if (!item.confirm && !item.cardId.isEmpty()) {
                        drawResolvedTexture(sb, item.frameResourceId, b);
                    }
                    com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb, com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                            item.confirm ? "Confirm" : item.cardId, item.x, item.y,
                            item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) {
        } finally {
            NativeRenderBridge.recordSurfaceDraw(surfaceId, SelectDrawPath.materializedDrawCount());
        }
    }

    /**
     * Reward surface: ART_DELEGATED when FULL_READY. The C2 items were synced in
     * prepareRewardVisuals and are drawn by the global RenderHosts.drawFrame pass; the renderer
     * also attempts texture + label supply for visible reward rows. Base reward parity remains
     * incomplete and visible in strict evidence.
     */
    private static void renderReward(SpriteBatch sb, String surfaceId) {
        int drawn = 0;
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(surfaceId);
            for (RewardDrawPath.DrawItem item : RewardDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                artframework.component.Rect bounds = new artframework.component.Rect(
                        item.x - item.w / 2f, item.y - item.h / 2f, item.w, item.h);
                drawResolvedTexture(sb, item.resourceId, bounds);
                if (!item.label.isEmpty()) {
                    com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb,
                            com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                            item.label,
                            bounds.x + bounds.width * 0.5f,
                            bounds.y + bounds.height * 0.54f,
                            item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                }
                drawn++;
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDraw(surfaceId, RewardDrawPath.materializedDrawCount());
    }

    /**
     * Rest surface: ART_DELEGATED when FULL_READY. Paints resource-backed campfire chrome
     * projected from RestView and records the current visible chrome-row count; campfire base
     * animation/native parity remains an exposed supply gap.
     */
    private static void renderRest(SpriteBatch sb) {
        if (!RestDrawPath.shouldSuppressNativeRest()) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.REST);
            int i = 0;
            for (RoomChromeLine line : RestDrawPath.chromeLines()) {
                if (!line.visible) {
                    continue;
                }
                artframework.component.Rect bounds = roomLineBounds(line, i);
                drawResolvedTexture(sb, line.resourceId, bounds);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        line.text,
                        bounds.x + bounds.width * 0.5f,
                        bounds.y + bounds.height * 0.54f,
                        line.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                i++;
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.REST, RestDrawPath.materializedDrawCount());
    }

    private static void renderSkeleton(SpriteBatch sb) {
        if (!artframework.sts1.skeleton.Sts1SkeletonBridge.shouldDraw()) {
            return;
        }
        // The provider renders ART-owned skeletons (or at the native slot for claimed instances).
        // No hand-drawn skeleton fallback/chrome pixels are ever drawn here.
        // The skeleton bridge manages its own SpriteBatch end/begin so it does not rely on the
        // outer batch guard (which was removed in Slice E1). This self-contained lifecycle is
        // correct because skeleton rendering may need to swap to the native Spine renderer.
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
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.SKELETON, 1);
    }

    /**
     * Shop surface: ART_DELEGATED when FULL_READY. Paints minimal text chrome projected from
     * ShopView (entry prices stay whatever the view carries; G4 full projection is a later slice)
     * and records the drawn-row count; merchant/base art remains an exposed supply gap.
     */
    private static void renderShop(SpriteBatch sb) {
        if (!ShopDrawPath.shouldSuppressNativeShop()) {
            return;
        }
        int drawn = 0;
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.SHOP);
            int i = 0;
            for (RoomChromeLine line : ShopDrawPath.chromeLines()) {
                if (!line.visible) {
                    continue;
                }
                artframework.component.Rect bounds = roomLineBounds(line, i);
                drawResolvedTexture(sb, line.resourceId, bounds);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        line.text,
                        bounds.x + bounds.width * 0.5f,
                        bounds.y + bounds.height * 0.54f,
                        line.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                i++;
                drawn++;
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.SHOP, drawn);
    }

    /**
     * Treasure surface: ART_DELEGATED when FULL_READY. Paints minimal text chrome projected from
     * TreasureView and records the drawn-row count; chest art remains an exposed supply gap.
     */
    private static void renderTreasure(SpriteBatch sb) {
        if (!TreasureDrawPath.shouldSuppressNativeTreasure()) {
            return;
        }
        int drawn = 0;
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.TREASURE);
            int i = 0;
            for (RoomChromeLine line : TreasureDrawPath.chromeLines()) {
                if (!line.visible) {
                    continue;
                }
                artframework.component.Rect bounds = roomLineBounds(line, i);
                drawResolvedTexture(sb, line.resourceId, bounds);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        line.text,
                        bounds.x + bounds.width * 0.5f,
                        bounds.y + bounds.height * 0.54f,
                        line.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                i++;
                drawn++;
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.TREASURE, drawn);
    }

    private static void renderProceed(SpriteBatch sb) {
        if (!ProceedDrawPath.shouldSuppressNativeProceed()) {
            return;
        }
        java.util.List<ProceedDrawPath.DrawItem> items = ProceedDrawPath.buildFromProjection();
        int projectedDrawCount = ProceedDrawPath.materializedDrawCount();
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.COMBAT_PROCEED);
            for (ProceedDrawPath.DrawItem item : items) {
                if (!item.visible) {
                    continue;
                }
                artframework.component.Rect bounds = item.bounds(
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT);
                drawResolvedTexture(sb, item.resourceId, bounds);
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                        item.text,
                        bounds.x + bounds.width * 0.5f,
                        bounds.y + bounds.height * 0.54f,
                        item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_PROCEED, projectedDrawCount);
    }

    private static void renderTopPanel(SpriteBatch sb) {
        if (!Sts1RenderPipeline.plan().shouldDraw(SurfaceIds.TOP_PANEL)) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.TOP_PANEL);
            java.util.List<TopPanelDrawPath.DrawItem> items = TopPanelDrawPath.buildFromProjection();
            for (TopPanelDrawPath.DrawItem item : items) {
                if (!item.visible) {
                    continue;
                }
                artframework.component.Rect bounds = item.bounds(
                        com.megacrit.cardcrawl.core.Settings.WIDTH,
                        com.megacrit.cardcrawl.core.Settings.HEIGHT);
                drawResolvedTexture(sb, item.resourceId, bounds);
                if (!item.text.isEmpty()) {
                    com.megacrit.cardcrawl.helpers.FontHelper.renderFontLeftTopAligned(
                            sb,
                            com.megacrit.cardcrawl.helpers.FontHelper.topPanelInfoFont,
                            item.text,
                            bounds.x + 8f,
                            bounds.y + bounds.height - 7f,
                            colorLabel(chrome));
                }
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.TOP_PANEL,
                TopPanelDrawPath.materializedDrawCount());
    }

    private static void drawResolvedTexture(
            SpriteBatch sb, String resourceId, artframework.component.Rect bounds) {
        if (sb == null || bounds == null || bounds.width <= 0f || bounds.height <= 0f
                || resourceId == null || resourceId.isEmpty()) {
            return;
        }
        try {
            artframework.assets.AssetResolveResult result =
                    ArtFramework.assets().resolve(resourceId);
            com.badlogic.gdx.graphics.Texture texture =
                    artframework.sts1.assets.Sts1AssetMaterializer.resolveTexture(result);
            if (texture != null) {
                sb.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } catch (Throwable ignored) {
        }
    }

    private static artframework.component.Rect roomLineBounds(RoomChromeLine line, int fallbackRow) {
        if (line.w > 0f && line.h > 0f) {
            return new artframework.component.Rect(line.x, line.y, line.w, line.h);
        }
        float x = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
        float y = com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.66f;
        return new artframework.component.Rect(x - 180f, y - fallbackRow * 40f - 20f, 360f, 40f);
    }

    /**
     * Energy surface: ART_DELEGATED when FULL_READY. The C2 item was synced in
     * prepareEnergyVisuals and drawn by the global RenderHosts.drawFrame pass; current supply is
     * projected orb texture; native animation/layer parity remains pending.
     */
    private static void renderEnergy(SpriteBatch sb) {
        for (EnergyDrawPath.DrawItem item : EnergyDrawPath.buildFromProjection()) {
            drawResolvedTexture(sb, item.resourceId, item.bounds);
        }
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_ENERGY,
                EnergyDrawPath.buildFromProjection().size());
    }

    /**
     * Intents surface: ART_DELEGATED when FULL_READY. The C2 items were synced in
     * prepareIntentVisuals and drawn by the global RenderHosts.drawFrame pass; current supply is
     * projected intent textures and amounts; native animation parity remains pending.
     */
    private static void renderIntents(SpriteBatch sb) {
        for (IntentDrawPath.DrawItem item : IntentDrawPath.buildFromProjection()) {
            drawResolvedTexture(sb, item.iconResourceId, item.bounds);
        }
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_INTENTS,
                IntentDrawPath.buildFromProjection().size());
    }

    private static void renderTargetingOverlay(SpriteBatch sb) {
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        if (!plan.shouldDraw(SurfaceIds.COMBAT_TARGETING)) {
            return;
        }
        int drawn = 0;
        try {
            com.badlogic.gdx.graphics.Texture lineTexture =
                    com.megacrit.cardcrawl.helpers.ImageMaster.WHITE_SQUARE_IMG;
            for (TargetingDrawPath.DrawItem item : TargetingDrawPath.buildFromProjection()) {
                if (!item.active) {
                    continue;
                }
                float dx = item.endX - item.startX;
                float dy = item.endY - item.startY;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (length < 0.0001f) {
                    continue;
                }
                float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) - 90f;
                float thickness = 4f;
                sb.draw(
                        lineTexture,
                        item.startX - thickness * 0.5f,
                        item.startY,
                        thickness * 0.5f,
                        0f,
                        thickness,
                        length,
                        1f,
                        1f,
                        angle,
                        0,
                        0,
                        1,
                        1,
                        false,
                        false);
                com.badlogic.gdx.graphics.Texture arrowTexture = resolveTargetingArrow();
                if (arrowTexture != null) {
                    float arrowSize = 32f;
                    sb.draw(
                            arrowTexture,
                            item.endX - arrowSize * 0.5f,
                            item.endY - arrowSize * 0.5f,
                            arrowSize * 0.5f,
                            arrowSize * 0.5f,
                            arrowSize,
                            arrowSize,
                            1f,
                            1f,
                            angle,
                            0,
                            0,
                            arrowTexture.getWidth(),
                            arrowTexture.getHeight(),
                            false,
                            false);
                }
                drawn++;
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_TARGETING, Math.max(1, drawn));
    }

    private static com.badlogic.gdx.graphics.Texture resolveTargetingArrow() {
        try {
            artframework.assets.AssetResolveResult result =
                    artframework.api.ArtFramework.assets().resolve(
                            artframework.assets.ResourceIds.UI_COMBAT_TARGETING_ARROW);
            if (!result.found) {
                return null;
            }
            return artframework.sts1.assets.Sts1AssetMaterializer.resolveTexture(result);
        } catch (Throwable ignored) {
            return null;
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

    private static artframework.core.PresentChromeStyle resolveSurfaceChrome(String surfaceId) {
        try {
            return artframework.core.PresentResolve.chromeForSurface(surfaceId);
        } catch (Throwable ignored) {
            return artframework.core.PresentChromeStyle.stsDefault();
        }
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
