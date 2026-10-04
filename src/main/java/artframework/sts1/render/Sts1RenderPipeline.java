package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentSafety;
import artframework.sts1.input.CombatInputRouter;
import artframework.core.UiComponent;

import java.util.Map;

/**
 * Coordinates full-present draw planning for STS1 (16.3). Host {@link Sts1SurfaceRenderer} uses
 * {@link #plan()} each frame; pure JUnit exercises the plan without SpriteBatch.
 */
public final class Sts1RenderPipeline {

    private static boolean overlayObserve;
    private static SurfaceDrawPlan lastPlan = SurfaceDrawPlan.build("", false, false, false, false, false, false);
    private static PlanKey lastKey;
    // NRM-07 diagnostic counters. Additive and behavior-neutral: they only observe whether the
    // same-frame cache returned the retained plan (`planCacheHits`) or rebuilt it
    // (`planCacheMisses`). They never influence the cache decision.
    private static long planCacheHits;
    private static long planCacheMisses;

    private Sts1RenderPipeline() {}

    public static void setOverlayObserve(boolean enabled) {
        overlayObserve = enabled;
    }

    public static boolean isOverlayObserve() {
        return overlayObserve;
    }

    public static SurfaceDrawPlan lastPlan() {
        return lastPlan;
    }

    // NRM-07 read-only diagnostics accessors. Unlike probeSlice(), these do NOT call plan(), so a
    // test can read the exact hit/miss tallies without perturbing them.
    static long planCacheHits() {
        return planCacheHits;
    }

    static long planCacheMisses() {
        return planCacheMisses;
    }

    public static SurfaceDrawPlan plan() {
        String scene = ArtFramework.projection().scene();
        long frameId = ArtFramework.projection().lastFrameId();
        boolean hand = mounted(SurfaceIds.COMBAT_HAND);
        boolean slots = mounted(SurfaceIds.COMBAT_CARD_SLOTS);
        boolean controls = mounted(SurfaceIds.COMBAT_CONTROLS);
        boolean map = mounted(SurfaceIds.MAP);
        boolean skeleton = mounted(SurfaceIds.SKELETON);
        boolean event = mounted(SurfaceIds.EVENT);
        boolean selectGrid = mounted(SurfaceIds.SELECT_GRID);
        boolean selectHand = mounted(SurfaceIds.SELECT_HAND);
        boolean reward = mounted(SurfaceIds.REWARD_COMBAT) || mounted(SurfaceIds.REWARD_CARD)
                || mounted(SurfaceIds.REWARD_BOSS_RELIC);
        boolean rest = mounted(SurfaceIds.REST);
        boolean treasure = mounted(SurfaceIds.TREASURE);
        boolean shop = mounted(SurfaceIds.SHOP);
        boolean topPanel = mounted(SurfaceIds.TOP_PANEL);
        boolean intents = mounted(SurfaceIds.COMBAT_INTENTS);
        boolean proceed = mounted(SurfaceIds.COMBAT_PROCEED);
        boolean energy = mounted(SurfaceIds.COMBAT_ENERGY);
        boolean pileDraw = mounted(SurfaceIds.COMBAT_PILE_DRAW);
        boolean targeting = mounted(SurfaceIds.COMBAT_TARGETING);
        long policyRevision = FullPresentMode.policyRevision();
        long executorRevision = CombatInputRouter.executorRevision();
        long readinessAndPanic = SurfaceDrawPlan.captureReadinessAndPanic();
        long flags = (hand ? 1L : 0L)
                | (slots ? 1L << 1 : 0L)
                | (controls ? 1L << 2 : 0L)
                | (map ? 1L << 3 : 0L)
                | (skeleton ? 1L << 4 : 0L)
                | (event ? 1L << 5 : 0L)
                | (selectGrid ? 1L << 6 : 0L)
                | (selectHand ? 1L << 7 : 0L)
                | (reward ? 1L << 8 : 0L)
                | (rest ? 1L << 9 : 0L)
                | (treasure ? 1L << 10 : 0L)
                | (shop ? 1L << 11 : 0L)
                | (topPanel ? 1L << 12 : 0L)
                | (intents ? 1L << 13 : 0L)
                | (proceed ? 1L << 14 : 0L)
                | (energy ? 1L << 15 : 0L)
                | (targeting ? 1L << 16 : 0L)
                | (overlayObserve ? 1L << 17 : 0L)
                | (pileDraw ? 1L << 38 : 0L)
                | readinessAndPanic;
        if (lastKey != null && lastKey.matches(frameId, policyRevision, executorRevision, scene, flags)) {
            planCacheHits++;
            return lastPlan;
        }
        planCacheMisses++;
        lastPlan =
                SurfaceDrawPlan.buildFromSnapshot(
                        scene,
                        hand, slots, controls, map, skeleton, event, selectGrid, selectHand,
                        reward, rest, treasure, shop, topPanel, intents, proceed, energy, targeting,
                        pileDraw,
                        overlayObserve, readinessAndPanic,
                        (readinessAndPanic & (1L << 39)) != 0L);
        lastKey = new PlanKey(frameId, policyRevision, executorRevision, scene, flags);
        return lastPlan;
    }

    public static boolean shouldDrawHand() {
        return plan().shouldDraw(SurfaceIds.COMBAT_HAND);
    }

    public static boolean shouldSuppressNativeHand() {
        return plan().shouldSuppressNative(SurfaceIds.COMBAT_HAND);
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = plan().toMap();
        m.put("overlayObserve", Boolean.valueOf(overlayObserve));
        m.put("planCacheHits", Long.valueOf(planCacheHits));
        m.put("planCacheMisses", Long.valueOf(planCacheMisses));
        m.put("nativeRender", NativeRenderBridge.probeSlice());
        m.put("nativeRenderStrict", NativeRenderBridge.strictReport());
        m.put("cardsPilesSoul", Sts1PileSoulDrawPath.probeSlice());
        // The per-instance transient-effect claim seam is family-neutral: `vfxClaim` is the
        // honest name and `aura` the retained alias for the same nested gate/ready/draws map.
        m.put("aura", VfxArtRenderer.probeSlice());
        m.put("vfxClaim", VfxArtRenderer.probeSlice());
        return m;
    }

    public static void resetForTests() {
        overlayObserve = false;
        HandDrawPath.resetForTests();
        HandRenderMetrics.resetForTests();
        lastPlan = SurfaceDrawPlan.build("", false, false, false, false, false, false);
        lastKey = null;
        planCacheHits = 0L;
        planCacheMisses = 0L;
        NativeRenderBridge.resetForTests();
    }

    private static boolean mounted(String id) {
        UiComponent c = ArtFramework.component(id);
        return c != null && c.isMounted();
    }

    private static final class PlanKey {
        private final long frameId;
        private final long policyRevision;
        private final long executorRevision;
        private final String scene;
        private final long flags;

        PlanKey(long frameId, long policyRevision, long executorRevision, String scene, long flags) {
            this.frameId = frameId;
            this.policyRevision = policyRevision;
            this.executorRevision = executorRevision;
            this.scene = scene;
            this.flags = flags;
        }

        boolean matches(long frameId, long policyRevision, long executorRevision, String scene, long flags) {
            return this.frameId == frameId
                    && this.policyRevision == policyRevision
                    && this.executorRevision == executorRevision
                    && (this.scene == scene || (this.scene != null && this.scene.equals(scene)))
                    && this.flags == flags;
        }
    }
}
