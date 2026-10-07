package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
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

    /**
     * V03: projection-frame id of the last delegated MAP band paint. {@link Long#MIN_VALUE} means
     * "none yet". Keying the once-guard on the projection frame id (not a plain boolean) makes the
     * band idempotent within one frame while still repainting on the next frame.
     */
    private static long mapBandDrawnFrameId = Long.MIN_VALUE;
    /** Test seam: successful/attempted MAP band paints since the last reset. */
    private static int mapBandDrawCount;
    /** Test seam: when non-null, replaces the real band paint so fail-open is provable. */
    private static Runnable mapBandDrawOverrideForTests;
    /**
     * Test seam: when non-null, every resource the map band would submit (background, then edges,
     * then legend/node submissions) is appended here in draw order, without needing real textures.
     */
    private static java.util.List<String> mapBandDrawLogForTests;

    private Sts1SurfaceRenderer() {}

    /** Test seam: the ordered resource ids the map band draws (background -> edges -> submissions). */
    static java.util.List<String> mapBandDrawLogForTests() {
        return mapBandDrawLogForTests;
    }

    /** Test seam: enables the ordered submission recorder for the current test. */
    static void recordMapBandDrawsForTests() {
        mapBandDrawLogForTests = new java.util.ArrayList<String>();
    }

    private static void recordMapBandResource(String resourceId) {
        if (mapBandDrawLogForTests != null) {
            mapBandDrawLogForTests.add(resourceId == null ? "" : resourceId);
        }
    }

    public static boolean shouldSuppressNativeHand() {
        return Sts1RenderPipeline.shouldSuppressNativeHand();
    }

    public static boolean shouldSuppressNativeEvent() {
        return EventDrawPath.shouldSuppressNativeEvent();
    }

    public static boolean shouldSuppressNativeSelect() {
        return SelectDrawPath.shouldSuppressNativeSelect();
    }

    public static Sts1RenderBoundary.BackgroundCapability backgroundCapability() {
        return Sts1RenderBoundary.backgroundCapability();
    }

    /** Draw full-present surfaces after the STS world render (PostRender). */
    public static void render(SpriteBatch sb) {
        if (sb == null) {
            return;
        }
        if (BackgroundOnlyGate.isActive()) {
            BackgroundOnlyGate.recordUncovered("art.surface_renderer");
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
        // V03: the delegated MAP band (parchment background + legend/nodes/edges) is NO LONGER drawn
        // here. It is drawn at the native map point inside {@link #drawMapBandAtNativePoint}, hooked
        // at {@code TopPanel.render} entry — i.e. exactly where native {@code DungeonMapScreen.render}
        // was skipped — so the native HUD drawn afterwards by {@code TopPanel.render} lands ON TOP.
        // See docs/design/render-z-order.md sections 6/12. Drawing it here (post-native) is what
        // covered the top HUD.
        RenderHosts.get().drawFrame(sb, true, RenderHost.kindsC2UnderPresent());
        for (SurfaceDrawPlan.Entry e : plan.drawOrder()) {
            prepareSurfaceChrome(sb, e.surfaceId);
            if (SurfaceIds.COMBAT_HAND.equals(e.surfaceId)) {
                renderHand(sb);
            } else if (SurfaceIds.COMBAT_CONTROLS.equals(e.surfaceId)) {
                renderControls(sb);
            } else if (SurfaceIds.MAP.equals(e.surfaceId)) {
                // V03: the delegated MAP band is drawn at the native map point this frame (see
                // drawMapBandAtNativePoint); the post-native loop draws it only when the top panel is
                // itself ART-suppressed (no native HUD to cover). Both routes go through the SAME
                // shared once-guarded method so the FULL band (parchment background + renderMap
                // submissions) is drawn exactly once, whichever site runs first.
                if (postNativeDrawsSurface(plan, e.surfaceId)) {
                    drawMapBandPostNative(sb);
                }
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
            } else if (SurfaceIds.COMBAT_PILE_DRAW.equals(e.surfaceId)) {
                renderPileDraw(sb);
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
        renderRelicPotionBlightOverlay(sb);
        renderPileSoulOverlay(sb);
        // Room-shell chrome now draws from payload entries in the shared aggregate frame. Make sure
        // the stateless room-shell producer and the shared aggregation pass are registered in the
        // existing RENDER_PROJECTION phase (both idempotent). The producer is installed first so it
        // publishes its contribution before the aggregation pass merges contributions.
        RoomShellRenderProjectionSystem.install();
        StanceRenderProjectionSystem.install();
        artframework.render.ArtRenderFrameAggregationSystem.install();
        Sts1RoomShellDrawPath.render(sb);
        VerifyGuideDrawPath.render(sb);
        // ART_EFFECTS is the final ART-owned band. It remains outside the native stage.draw()
        // boundary and is submitted after C2/entity content according to RenderPhase.rank.
        VfxSts1Runtime.render(sb);
    }

    /**
     * V03: draws the delegated MAP band (parchment background + legend/nodes/edges) at the NATIVE
     * map point, i.e. at {@code TopPanel.render} entry the moment just before the native HUD body —
     * reproducing native {@code AbstractDungeon.render}'s {@code dungeonMapScreen.render(sb)} then
     * {@code topPanel.render(sb)} order. The native map family ({@code DungeonMapScreen.render}) was
     * skipped by ART, so ART's map pixels must land here; the native top-panel HUD drawn afterwards
     * therefore paints ON TOP of the map instead of being covered by it (the pre-V03 post-native
     * overlay covered the HUD because {@code receivePostRender} runs after {@code topPanel.render}).
     * See docs/design/render-z-order.md sections 6/12.
     *
     * <p>Called from {@code TopPanelRenderPatches.ObserveNativeTopPanelRender.Prefix} at the
     * {@code TopPanel.render} entry. This is the native-point route; the post-native route for the
     * same frame is {@link #drawMapBandPostNative}. Both delegate to the one shared
     * {@link #drawMapBandOnce} so the FULL band (background included) is painted exactly once.
     */
    public static void drawMapBandAtNativePoint(SpriteBatch sb) {
        if (!shouldDrawMapBandAtNativePoint(Sts1RenderPipeline.plan())) {
            return;
        }
        drawMapBandOnce(sb);
    }

    /**
     * V03: the post-native route for the same delegated MAP band, used when the native-point route
     * did not run (top panel itself ART-suppressed, so there is no native HUD to sit under). It
     * draws the same FULL band via {@link #drawMapBandOnce} (parchment background + {@code renderMap}
     * submissions, identical content/order to the native-point route) so the map is never painted
     * without its background.
     */
    static void drawMapBandPostNative(SpriteBatch sb) {
        if (!shouldDrawMapBandPostNative(Sts1RenderPipeline.plan())) {
            return;
        }
        drawMapBandOnce(sb);
    }

    /**
     * Pure decision seam for the post-native route: draw the band here only when ART actually
     * delegated the map's pixels this frame. The routing (native-point vs post-native) is decided by
     * {@link #postNativeDrawsMapBand}; this guards the raw draw against being reached when the map is
     * not delegated at all (e.g. OFF/OBSERVE/unmounted).
     */
    static boolean shouldDrawMapBandPostNative(SurfaceDrawPlan plan) {
        return plan != null && plan.shouldSuppressNative(SurfaceIds.MAP);
    }

    /**
     * The single shared "draw the delegated MAP band exactly once for this projected frame" method
     * used by BOTH the native-point and post-native routes. It draws the parchment background first
     * (so it stays under the legend/nodes) then the {@code renderMap} submissions, and is idempotent
     * per frame (see {@link #claimMapBandDrawForFrame}), so whichever route runs first paints the
     * complete band and the later route is a no-op — the map can never be double-drawn or left
     * without a background.
     *
     * <p><b>Fail-open.</b> The whole band, including the once-guard and the batch-state restore, is
     * wrapped in {@code try/catch(Throwable)} so any failure leaves the native caller to continue and
     * never throws out of the band (nor out of the {@code TopPanel.render} patch).
     *
     * <p><b>Batch state.</b> The band runs inside the native render pass at {@code TopPanel.render}
     * entry (and post-native), so it must not leak batch state into the native HUD/gradient draws
     * that follow. The batch {@link SpriteBatch#getColor() color} is snapshotted at entry and
     * restored in a {@code finally}; {@link #drawResolvedTexture} additionally restores
     * {@code Color.WHITE} per draw. FontHelper's {@code renderFontCentered} mutates the shared
     * {@code BitmapFont}'s color (via {@code BitmapFont.setColor}) but not the batch color, so the
     * font tint is not captured here — see the residual note in docs/design/render-z-order.md §12.6.
     */
    private static void drawMapBandOnce(SpriteBatch sb) {
        if (sb == null) {
            return;
        }
        if (artframework.sts1.PresentSafety.isPanic()) {
            return;
        }
        if (BackgroundOnlyGate.isActive()) {
            return;
        }
        Runnable override = mapBandDrawOverrideForTests;
        long frameId = ArtFramework.projection().lastFrameId();
        // Snapshot the batch color so the band can never tint the native HUD/gradient draws that
        // follow. getColor() returns the live Color, so copy it.
        Color previousColor = null;
        try {
            previousColor = new Color(sb.getColor());
        } catch (Throwable ignored) {
        }
        try {
            if (!claimMapBandDrawForFrame(frameId)) {
                return;
            }
            if (override != null) {
                override.run();
                return;
            }
            renderMapBackground(sb);
            renderMap(sb);
        } catch (Throwable ignored) {
            // Fail-open: a map-band failure must never block the native top-panel body.
        } finally {
            try {
                if (previousColor != null) {
                    sb.setColor(previousColor);
                } else {
                    sb.setColor(Color.WHITE);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Pure decision seam: draw the native-point map band only when ART actually delegated the map's
     * pixels this frame (FULL + mounted + map scene + ready executor) AND the top panel itself is NOT
     * ART-suppressed — i.e. the native {@code TopPanel.render} body will run and its HUD must land on
     * top of the map. If the top panel is itself suppressed the native HUD is not drawn, so there is
     * nothing for the band to sit under and the band is drawn post-native instead (see
     * {@link #postNativeDrawsMapBand}).
     */
    static boolean shouldDrawMapBandAtNativePoint(SurfaceDrawPlan plan) {
        return plan != null
                && plan.shouldSuppressNative(SurfaceIds.MAP)
                && !plan.shouldSuppressNative(SurfaceIds.TOP_PANEL);
    }

    /**
     * Pure routing seam: the post-native loop draws the MAP band only when it was NOT already drawn
     * at the native map point. This keeps the map painted exactly once in every combination —
     * native-point when the top panel is native-continuing (the covered-HUD case this fix targets),
     * post-native when the top panel is itself ART-suppressed (no native HUD to cover).
     */
    static boolean postNativeDrawsMapBand(SurfaceDrawPlan plan) {
        return !shouldDrawMapBandAtNativePoint(plan);
    }

    /**
     * Pure routing seam for the post-native surface loop: every surface is still drawn post-native
     * EXCEPT the delegated MAP band, which is routed by {@link #postNativeDrawsMapBand}. Every other
     * surface keeps its previous post-native draw unchanged.
     */
    static boolean postNativeDrawsSurface(SurfaceDrawPlan plan, String surfaceId) {
        if (SurfaceIds.MAP.equals(surfaceId)) {
            return postNativeDrawsMapBand(plan);
        }
        return true;
    }

    /**
     * Pure once-per-frame guard. Returns {@code true} for the first call in a given projected
     * {@code frameId} and {@code false} for every later call in the same frame, so the band can never
     * double-draw if the native map point is entered more than once. Kept side-effect free apart from
     * the guard/counter so it is directly unit-testable.
     */
    static boolean claimMapBandDrawForFrame(long frameId) {
        if (mapBandDrawnFrameId == frameId) {
            return false;
        }
        mapBandDrawnFrameId = frameId;
        mapBandDrawCount++;
        return true;
    }

    /** Test seam: resets the once-per-frame guard and counters. */
    static void resetMapBandGuardForTests() {
        mapBandDrawnFrameId = Long.MIN_VALUE;
        mapBandDrawCount = 0;
        mapBandDrawOverrideForTests = null;
        mapBandDrawLogForTests = null;
    }

    /** Test seam: observation of how many times the native-point map band was attempted. */
    static int mapBandDrawCountForTests() {
        return mapBandDrawCount;
    }

    /** Test seam: replaces the real band paint so fail-open can be proven without a SpriteBatch. */
    static void setMapBandDrawOverrideForTests(Runnable override) {
        mapBandDrawOverrideForTests = override;
    }

    private static void disableInactiveSurfaceEffects(SurfaceDrawPlan plan) {
        String[] surfaces = {
            SurfaceIds.COMBAT_HAND, SurfaceIds.COMBAT_CARD_SLOTS, SurfaceIds.COMBAT_CONTROLS,
            SurfaceIds.MAP, SurfaceIds.EVENT, SurfaceIds.SELECT_GRID, SurfaceIds.SELECT_HAND,
            SurfaceIds.REWARD_COMBAT, SurfaceIds.REWARD_CARD, SurfaceIds.REWARD_BOSS_RELIC,
            SurfaceIds.REST, SurfaceIds.SHOP, SurfaceIds.TREASURE, SurfaceIds.COMBAT_PROCEED,
            SurfaceIds.TOP_PANEL, SurfaceIds.COMBAT_ENERGY, SurfaceIds.COMBAT_INTENTS,
            SurfaceIds.COMBAT_PILE_DRAW, SurfaceIds.COMBAT_TARGETING
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
            // Active surfaces must remain enabled so the render host can expose their target
            // state; skeleton pixels are still submitted by renderSkeleton below.
            artframework.render.RenderStateEcs.surface(surfaceId, x, y, w, h, true);
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
        preparePileDrawVisuals(plan);
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
            String itemId = "node:" + item.row + ":" + item.col;
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.MAP, itemId, item.bounds, 1f, "map-node",
                    item.artSource, item.symbol, true);
            visibleItems.add(itemId);
            if (item.outlineResourceId != null && !item.outlineResourceId.isEmpty()) {
                String outlineId = "outline:" + item.row + ":" + item.col;
                artframework.presentation.PresentationVisuals.syncC2Item(
                        SurfaceIds.MAP, outlineId, item.bounds, 2f, "map-node-outline",
                        item.outlineResourceId, "", item.reachable || item.highlighted);
                if (item.reachable || item.highlighted) visibleItems.add(outlineId);
            }
            if (item.highlightResourceId != null && !item.highlightResourceId.isEmpty()) {
                String overlayId = "overlay:" + item.row + ":" + item.col;
                artframework.presentation.PresentationVisuals.syncC2Item(
                        SurfaceIds.MAP, overlayId, item.bounds, 3f,
                        item.pinned ? "map-pin" : "map-highlight",
                        item.highlightResourceId, "", true);
                visibleItems.add(overlayId);
            }
        }
        // NRO-04 D03 map legend: panel + title + 6 room-type icon/label rows, mirrored exactly like
        // the node items above. The legend is omitted when Settings is unavailable. z stays BELOW
        // the node band because native DungeonMap.render draws Legend.render before
        // DungeonMapScreen.render draws the nodes, so nodes paint OVER the legend.
        for (MapDrawPath.LegendDrawItem legend : MapDrawPath.legendItems()) {
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.MAP, legend.id, legend.bounds, legend.z, "map-legend",
                    legend.resourceId, legend.label, true);
            visibleItems.add(legend.id);
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
            // NRO-04 D04: the whole-screen sheet/panel is synced (when Settings is available and the
            // reward is available) at a LOWER z than the rows so it sits behind them. Id/role are
            // distinct from the row items ("reward:<index>"/"reward-item").
            RewardDrawPath.DrawItem sheet = RewardDrawPath.sheetItem();
            if (sheet != null) {
                artframework.presentation.PresentationVisuals.syncC2Item(
                        entry.surfaceId, RewardDrawPath.SHEET_ITEM_ID, sheet.bounds(),
                        RewardDrawPath.SHEET_Z, RewardDrawPath.SHEET_ROLE,
                        sheet.resourceId, sheet.label, sheet.visible);
                visibleItems.add(RewardDrawPath.SHEET_ITEM_ID);
            }
            for (RewardDrawPath.DrawItem item : RewardDrawPath.buildFromProjection()) {
                if (!item.visible) continue;
                String itemId = "reward:" + item.index;
                artframework.presentation.PresentationVisuals.syncC2Item(
                        entry.surfaceId, itemId,
                        new artframework.component.Rect(item.x - item.w / 2f,
                                item.y - item.h / 2f, item.w, item.h), RewardDrawPath.ROW_Z,
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
        // NRO-04 D05: the language-specific rug background is synced at a LOWER z than the chrome
        // rows (RUG_Z < row z) so it paints behind them. Its id/role are distinct from the rows.
        ShopDrawPath.RugItem rug = ShopDrawPath.rugItem();
        Set<String> keep = new LinkedHashSet<String>();
        if (rug != null) {
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.SHOP, ShopDrawPath.RUG_ITEM_ID, rug.bounds(),
                    ShopDrawPath.RUG_Z, ShopDrawPath.RUG_ROLE, rug.resourceId, "", true);
            keep.add(ShopDrawPath.RUG_ITEM_ID);
        }
        syncRoomChromeItems(
                SurfaceIds.SHOP, ShopDrawPath.chromeLines(), "shop-title", "shop-entry", keep);
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
        // NRO-04 D08: the actual AbstractChest sprite is synced at a LOWER z than the chrome rows
        // (CHEST_Z < row z) so it paints behind them; id/role are distinct from the text rows.
        TreasureDrawPath.ChestItem chest = TreasureDrawPath.chestItem();
        Set<String> keep = new LinkedHashSet<String>();
        if (chest != null) {
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.TREASURE, TreasureDrawPath.CHEST_ITEM_ID, chest.bounds(),
                    TreasureDrawPath.CHEST_Z, TreasureDrawPath.CHEST_ROLE,
                    chest.resourceId, "", true);
            keep.add(TreasureDrawPath.CHEST_ITEM_ID);
        }
        syncRoomChromeItems(
                SurfaceIds.TREASURE, TreasureDrawPath.chromeLines(),
                "treasure-title", "treasure-item", keep);
    }

    /** Shared single-column layout for room text chrome rows. */
    private static void syncRoomChromeItems(
            String surfaceId,
            java.util.List<RoomChromeLine> lines,
            String titleRole,
            String rowRole) {
        syncRoomChromeItems(surfaceId, lines, titleRole, rowRole,
                java.util.Collections.<String>emptySet());
    }

    /**
     * Shared single-column layout for room text chrome rows, retaining extra C2 ids supplied
     * outside this loop (NRO-04 D05: the shop rug background) so they are not evicted.
     */
    private static void syncRoomChromeItems(
            String surfaceId,
            java.util.List<RoomChromeLine> lines,
            String titleRole,
            String rowRole,
            Set<String> keepIds) {
        Set<String> visibleItems = new LinkedHashSet<String>();
        if (keepIds != null) {
            visibleItems.addAll(keepIds);
        }
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

    private static void preparePileDrawVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.COMBAT_PILE_DRAW)) return;
        SurfaceDrawPlan.Entry entry = plan.find(SurfaceIds.COMBAT_PILE_DRAW);
        if (entry == null || entry.mode != SurfaceDrawPlan.DrawMode.DRAW) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_PILE_DRAW);
            return;
        }
        Set<String> visibleItems = new LinkedHashSet<String>();
        for (PileDrawDrawPath.DrawItem item : PileDrawDrawPath.buildFromProjection()) {
            artframework.presentation.PresentationVisuals.syncC2Item(
                    SurfaceIds.COMBAT_PILE_DRAW, item.id,
                    item.bounds, 1f, "pile_draw", item.resourceId, item.label, item.visible);
            visibleItems.add(item.id);
        }
        artframework.presentation.PresentationVisuals.retainC2Items(
                SurfaceIds.COMBAT_PILE_DRAW, visibleItems);
    }

    private static void prepareTopPanelVisuals(SurfaceDrawPlan plan) {
        if (!containsSurface(plan, SurfaceIds.TOP_PANEL)) {
            artframework.presentation.PresentationVisuals.removeC2Items(SurfaceIds.TOP_PANEL);
            return;
        }
        if (!TopPanelDrawPath.shouldDrawArtOverlay()) {
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
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.COMBAT_HAND, drawCount);
    }

    /**
     * Controls surface: ART_DELEGATED when FULL_READY. The C2 item was already synced in
     * prepareControlsVisuals and drawn by the global RenderHosts.drawFrame pass, but current
     * supply includes the projected button texture; native hover/animation parity remains pending.
     */
    private static void renderControls(SpriteBatch sb) {
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.COMBAT_CONTROLS,
                ControlsDrawPath.materializedDrawCount());
    }

    /**
     * Native map parchment background (D03 map background follow-up), drawn as the BOTTOM map layer.
     * {@code mapTop/mapMid/mapBot + mapBlend×2} at native rects with the live {@code baseMapColor}
     * alpha (white RGB), mirroring native {@code DungeonMap.renderNormalMap}/{@code renderMapCenters}/
     * {@code renderMapBlender} order. Called from {@link #drawMapBandAtNativePoint} (at the native
     * map point) immediately BEFORE {@link #renderMap} so the opaque parchment is under the legend
     * (drawn by {@link #renderMap}'s submission plan) and the nodes — matching native background ->
     * legend -> nodes order and preventing the wash-out where the background painted over the node
     * pixels. Every draw is fail-open; nothing else is recorded here because {@link #renderMap} owns
     * the surface evidence count.
     *
     * <p><b>Documented gap.</b> The {@code baseMapColor} fade-in alpha is applied per draw
     * ({@code bg.alpha}); the C2 item path carries no color/alpha, so the fade is not mirrored in C2
     * item state (settled alpha is 1.0, so the D1 parity frame is unaffected).
     */
    private static void renderMapBackground(SpriteBatch sb) {
        try {
            for (MapDrawPath.BackgroundDrawItem bg : MapDrawPath.backgroundItems()) {
                recordMapBandResource(bg.resourceId);
                try {
                    drawResolvedTexture(sb, bg.resourceId, bg.bounds, 1f, 1f, 1f, bg.alpha);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Map surface: ART_DELEGATED when FULL_READY. Submits the legend panel + title + 6 room-type
     * rows, then the projected node pixels (node icon, outline when reachable/highlighted, overlay
     * when pinned/highlighted) — native {@code DungeonMap.render} draws the legend before
     * {@code DungeonMapScreen.render} draws the nodes, so nodes paint OVER the legend. All draws go
     * through {@link #drawResolvedTexture}, mirroring the sibling {@code renderEvent}/
     * {@code renderSelect}/{@code renderTopPanel} pattern. Every draw is individually fail-open so
     * one bad resource never aborts the map. The recorded evidence count is the number of successful
     * submissions (legend + node/outline/overlay/ring draws).
     *
     * <p>The native parchment background is drawn EARLIER, by {@link #renderMapBackground} inside
     * {@link #drawMapBandAtNativePoint} (at the native map point) immediately before the node band,
     * so it stays under the legend/nodes instead of painting over them (D03 layering fix). It is
     * deliberately NOT drawn here.
     */
    private static void renderMap(SpriteBatch sb) {
        int drawn = 0;
        artframework.core.PresentChromeStyle chrome = resolveSurfaceChrome(SurfaceIds.MAP);
        // D03 map edges: native MapRoomNode.render draws each node's edges BEFORE that node's own
        // art, so the connection dots sit UNDER the node band. renderMapBackground painted the
        // parchment earlier in render(); here the edges paint right after it (under legend/nodes) and
        // before mapSubmissionPlan, matching paintOrder()'s `bg -> edge -> legend -> node` bands.
        for (MapDrawPath.EdgeDrawItem edge : MapDrawPath.edgeItems()) {
            recordMapBandResource(edge.resourceId);
            try {
                drawResolvedTexture(sb, edge.resourceId, edge.bounds,
                        edge.r, edge.g, edge.b, edge.a, edge.rotationDegrees);
                drawn++;
            } catch (Throwable ignored) {
            }
        }
        try {
            for (MapDrawPath.Submission item : MapDrawPath.mapSubmissionPlan()) {
                recordMapBandResource(item.resourceId);
                try {
                    drawResolvedTexture(sb, item.resourceId, item.bounds,
                            item.r, item.g, item.b, item.a);
                    if (!item.label.isEmpty()) {
                        com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                                sb,
                                com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                                item.label,
                                item.bounds.x + item.bounds.width * 0.5f,
                                item.bounds.y + item.bounds.height * 0.5f,
                                colorLabel(chrome));
                    }
                    drawn++;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.MAP, drawn);
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
                    // NRO-04 D06: the event TITLE is TEXT ONLY. Its UI_EVENT_TITLE resource is
                    // mis-mapped to images/ui/event/panel.png, so submitting it as a texture would
                    // paint a bogus panel-sized rectangle; skip the texture and draw the label.
                    if (!item.textOnly) {
                        drawResolvedTexture(sb, item.resourceId, b);
                    }
                    if (!item.label.isEmpty()) com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb, com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont, item.label,
                            item.x, item.y, item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) {
        } finally {
            NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.EVENT,
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
                            item.label, item.x, item.y,
                            item.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) {
        } finally {
            NativeRenderBridge.recordSurfaceDrawIfPending(surfaceId, SelectDrawPath.materializedDrawCount());
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
            for (RewardDrawPath.DrawItem item : RewardDrawPath.drawOrder()) {
                if (!item.visible) {
                    continue;
                }
                artframework.component.Rect bounds = item.bounds();
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
        NativeRenderBridge.recordSurfaceDrawIfPending(surfaceId, RewardDrawPath.drawSubmissionCount());
    }

    /**
     * Rest surface: ART_DELEGATED when FULL_READY. Paints resource-backed campfire option icons
     * projected from RestView and records the current visible chrome-row count; campfire base
     * animation/native parity remains an exposed supply gap.
     *
     * <p>Verified native {@code CampfireUI.render} draws NO campfire site title, so ART emits no
     * synthetic title row. Each option LABEL is anchored natively: X = option icon center X,
     * TOP-ALIGNED Y = icon center Y minus {@code RestDrawPath.LABEL_OFFSET_Y * Settings.scale}
     * (mirrors {@code AbstractCampfireOption.render}'s
     * {@code FontHelper.renderFontCenteredTopAligned(..., hb.cX, hb.cY - 105f*scale, ...)}), with
     * native colors ({@code Settings.GOLD_COLOR} usable / {@code Color.LIGHT_GRAY} disabled) so it
     * sits BELOW the icon instead of occluding it.
     */
    private static void renderRest(SpriteBatch sb) {
        if (!RestDrawPath.shouldSuppressNativeRest()) {
            return;
        }
        try {
            int drawn = 0;
            for (RoomChromeLine line : RestDrawPath.chromeLines()) {
                if (!line.visible) {
                    continue;
                }
                artframework.component.Rect bounds = roomLineBounds(line, drawn);
                drawResolvedTexture(sb, line.resourceId, bounds);
                // Native label anchor from the option's own center (never a copied magic number):
                // X = centerX, top-aligned Y = centerY - LABEL_OFFSET_Y*scale (native formula).
                float labelX = bounds.x + bounds.width * 0.5f;
                float labelY = bounds.y + bounds.height * 0.5f
                        - RestDrawPath.labelOffsetY();
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCenteredTopAligned(
                        sb,
                        com.megacrit.cardcrawl.helpers.FontHelper.topPanelInfoFont,
                        line.text,
                        labelX,
                        labelY,
                        line.enabled ? com.megacrit.cardcrawl.core.Settings.GOLD_COLOR
                                : Color.LIGHT_GRAY);
                drawn++;
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.REST, RestDrawPath.materializedDrawCount());
    }

    private static void renderSkeleton(SpriteBatch sb) {
        if (!artframework.sts1.skeleton.Sts1SkeletonBridge.shouldDraw()) {
            return;
        }
        // The provider renders ART-owned skeletons (or at the native slot for claimed instances).
        // No hand-drawn skeleton fallback/chrome pixels are ever drawn here.
        try {
            // Provider-backed Spine42 rendering submits directly to the active SpriteBatch.
            // Ending it here makes batch.draw fail while the exception is intentionally fail-open.
            artframework.sts1.skeleton.Sts1SkeletonBridge.renderAll(sb);
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.SKELETON, 1);
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
            // NRO-04 D05: rug background FIRST so it paints behind every chrome row.
            ShopDrawPath.RugItem rug = ShopDrawPath.rugItem();
            if (rug != null) {
                drawResolvedTexture(sb, rug.resourceId, rug.bounds());
                drawn++;
            }
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
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.SHOP, drawn);
    }

    /**
     * Treasure surface: ART_DELEGATED when FULL_READY. Paints the actual {@code AbstractChest}
     * sprite (D08, geometry unchanged) and then at most ONE relic chrome row when the chest is
     * open. Native {@code TreasureRoom.render} draws no room title/status text and
     * {@code AbstractChest.render} draws only the sprite, so the closed state submits the sprite
     * alone (no synthetic title/status rows — removed to fix the D1 overlap); the open-state relic
     * label is ART's minimal stand-in for the native relic-get panel (D04 reward surface). Chest
     * animation/glow/background remain an exposed supply gap.
     */
    private static void renderTreasure(SpriteBatch sb) {
        if (!TreasureDrawPath.shouldSuppressNativeTreasure()) {
            return;
        }
        int drawn = 0;
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.TREASURE);
            // NRO-04 D08: chest sprite FIRST so it paints behind every text row.
            TreasureDrawPath.ChestItem chest = TreasureDrawPath.chestItem();
            if (chest != null) {
                drawResolvedTexture(sb, chest.resourceId, chest.bounds());
                drawn++;
            }
            int i = 0;
            for (RoomChromeLine line : TreasureDrawPath.chromeLines()) {
                if (!line.visible) {
                    continue;
                }
                artframework.component.Rect bounds = roomLineBounds(line, i);
                drawResolvedTexture(sb, line.resourceId, bounds);
                if (!line.text.isEmpty()) {
                    com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb,
                            com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont,
                            line.text,
                            bounds.x + bounds.width * 0.5f,
                            bounds.y + bounds.height * 0.54f,
                            line.enabled ? colorLabel(chrome) : colorDisabled(chrome));
                }
                i++;
                drawn++;
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.TREASURE, drawn);
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
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.COMBAT_PROCEED, projectedDrawCount);
    }

    private static void renderTopPanel(SpriteBatch sb) {
        if (!TopPanelDrawPath.shouldDrawArtOverlay()) {
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
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.TOP_PANEL,
                TopPanelDrawPath.materializedDrawCount());
    }

    private static void drawResolvedTexture(
            SpriteBatch sb, String resourceId, artframework.component.Rect bounds) {
        drawResolvedTexture(sb, resourceId, bounds, 1f, 1f, 1f, 1f);
    }

    /**
     * D03: resolve and draw with a per-draw tint (native {@code sb.setColor} semantics). Sets the
     * batch color before {@code sb.draw} and restores {@link Color#WHITE} afterwards so sibling
     * draws are unaffected. Fail-open on any bad input; restores white in a finally block.
     */
    private static void drawResolvedTexture(
            SpriteBatch sb,
            String resourceId,
            artframework.component.Rect bounds,
            float r,
            float g,
            float b,
            float a) {
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
                sb.setColor(r, g, b, a);
                sb.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                sb.setColor(Color.WHITE);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * D03 map edges: resolve and draw a texture tinted, rotated about the box CENTER, mirroring native
     * {@code MapDot.render}. Native does {@code sb.draw(ImageMaster.MAP_DOT_1, dot.x-8, dot.y-8+
     * offsetY+172*scale, 8,8, 16,16, scale,scale, rotation, 0,0,16,16, false,false)} — the origin
     * (8,8) is unscaled but subtracted scaled, so the final screen box is exactly {@code bounds} with
     * the rotation pivot at its center (libGDX transforms the origin by scaleX/scaleY). Drawing with
     * origin {@code (w/2,h/2)} and 1:1 scale reproduces that box/pivot exactly. Fail-open; restores
     * white in a finally block.
     */
    private static void drawResolvedTexture(
            SpriteBatch sb,
            String resourceId,
            artframework.component.Rect bounds,
            float r,
            float g,
            float b,
            float a,
            float rotationDegrees) {
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
                sb.setColor(r, g, b, a);
                sb.draw(texture,
                        bounds.x,
                        bounds.y,
                        bounds.width * 0.5f,
                        bounds.height * 0.5f,
                        bounds.width,
                        bounds.height,
                        1f,
                        1f,
                        rotationDegrees,
                        0,
                        0,
                        texture.getWidth(),
                        texture.getHeight(),
                        false,
                        false);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                sb.setColor(Color.WHITE);
            } catch (Throwable ignored) {
            }
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
     * Energy surface: ART_DELEGATED when FULL_READY. Rebuilds the native orb layer stack
     * (bright/dim layers, native spin, centered number) instead of a single stretched glow
     * texture. The C2 item synced in prepareEnergyVisuals still supplies layout/input state.
     */
    private static void renderEnergy(SpriteBatch sb) {
        try {
            EnergyDrawPath.advanceAnimation(com.badlogic.gdx.Gdx.graphics.getDeltaTime());
        } catch (Throwable ignored) {
        }
        java.util.List<EnergyDrawPath.DrawItem> items = EnergyDrawPath.buildFromProjection();
        for (EnergyDrawPath.DrawItem item : items) {
            artframework.component.Rect b = item.bounds;
            if (sb == null || b == null) continue;
            for (EnergyDrawPath.OrbLayer layer : item.layers) {
                try {
                    artframework.assets.AssetResolveResult r =
                            ArtFramework.assets().resolve(layer.resourceId);
                    com.badlogic.gdx.graphics.Texture tex =
                            artframework.sts1.assets.Sts1AssetMaterializer.resolveEnergyTexture(
                                    layer.resourceId, r);
                    if (tex != null) {
                        sb.setColor(Color.WHITE);
                        sb.draw(tex, b.x, b.y, b.width / 2f, b.height / 2f,
                                b.width, b.height, 1f, 1f, layer.rotationDegrees,
                                0, 0, tex.getWidth(), tex.getHeight(), false, false);
                    }
                } catch (Throwable ignored) {
                }
            }
            renderEnergyNumber(sb, b);
        }
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.COMBAT_ENERGY, items.size());
    }

    /**
     * Draw-pile panel surface: ART_DELEGATED when FULL_READY. Paints the projected draw-zone
     * resource-backed chrome and records the draw-pile evidence count; full native
     * deck-button/animation parity remains an exposed supply gap.
     */
    private static void renderPileDraw(SpriteBatch sb) {
        if (!PileDrawDrawPath.shouldSuppressNativePileDraw()) {
            return;
        }
        int drawn = 0;
        try {
            artframework.core.PresentChromeStyle chrome =
                    artframework.core.PresentResolve.chromeForSurface(SurfaceIds.COMBAT_PILE_DRAW);
            for (PileDrawDrawPath.DrawItem item : PileDrawDrawPath.buildFromProjection()) {
                if (!item.visible) {
                    continue;
                }
                drawResolvedTexture(sb, item.resourceId, item.bounds);
                if (!item.label.isEmpty()) {
                    com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb,
                            com.megacrit.cardcrawl.helpers.FontHelper.turnNumFont,
                            item.label,
                            item.bounds.x + item.bounds.width * 0.5f,
                            item.bounds.y + item.bounds.height * 0.5f,
                            colorLabel(chrome));
                }
                drawn++;
            }
        } catch (Throwable ignored) {
        }
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.COMBAT_PILE_DRAW, drawn);
    }

    /** Native energy count drawn centered on the orb; safe without a live player. */
    private static void renderEnergyNumber(SpriteBatch sb, artframework.component.Rect bounds) {
        try {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null || p.energy == null) return;
            BitmapFont font = p.getEnergyNumFont();
            if (font == null) return;
            font.getData().setScale(EnergyPanel.fontScale);
            FontHelper.renderFontCentered(sb, font,
                    EnergyPanel.totalCount + "/" + p.energy.energy,
                    bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f,
                    new Color(1f, 1f, 0.86f, 1f));
        } catch (Throwable ignored) {
        }
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
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.COMBAT_INTENTS,
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
        NativeRenderBridge.recordSurfaceDrawIfPending(SurfaceIds.COMBAT_TARGETING, Math.max(1, drawn));
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

    /** Decorative resource overlay only; the native relic-family renderer always continues. */
    private static void renderRelicPotionBlightOverlay(SpriteBatch sb) {
        if (!shouldDrawNativeContinuationOverlay()) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome = artframework.core.PresentResolve.chrome();
            for (Sts1RelicPotionBlightDrawPath.DrawItem item :
                    Sts1RelicPotionBlightDrawPath.buildFromProjection()) {
                if (!item.visible) continue;
                drawResolvedTexture(sb, item.resourceId, item.bounds);
                String text = item.label;
                if (item.count != 0) text = text + " " + item.count;
                if (!text.isEmpty()) {
                    com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb, com.megacrit.cardcrawl.helpers.FontHelper.cardDescFont_N, text,
                            item.bounds.x + item.bounds.width * 0.5f,
                            item.bounds.y - 8f, item.usable ? colorLabel(chrome) : colorDisabled(chrome));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** Decorative resource overlay only; native CardGroup/Soul and AbstractCard pixels continue. */
    private static void renderPileSoulOverlay(SpriteBatch sb) {
        if (!shouldDrawNativeContinuationOverlay()) {
            return;
        }
        try {
            artframework.core.PresentChromeStyle chrome = artframework.core.PresentResolve.chrome();
            for (Sts1PileSoulDrawPath.DrawItem item : Sts1PileSoulDrawPath.buildFromProjection()) {
                if (!item.visible) continue;
                drawResolvedTexture(sb, item.resourceId, item.bounds);
                String text = item.label;
                if (item.count >= 0) text = text + " " + item.count;
                if (!text.isEmpty()) {
                    com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                            sb, com.megacrit.cardcrawl.helpers.FontHelper.cardDescFont_N, text,
                            item.bounds.x + item.bounds.width * 0.5f,
                            item.bounds.y - 8f, colorLabel(chrome));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** Native continuation owns observation-only inventory/pile/soul pixels. */
    static boolean shouldDrawNativeContinuationOverlay() {
        return false;
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
