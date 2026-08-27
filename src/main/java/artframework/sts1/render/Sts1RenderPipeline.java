package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.core.UiComponent;

import java.util.Map;

/**
 * Coordinates full-present draw planning for STS1 (16.3). Host {@link Sts1SurfaceRenderer} uses
 * {@link #plan()} each frame; pure JUnit exercises the plan without SpriteBatch.
 */
public final class Sts1RenderPipeline {

    private static boolean overlayObserve;
    private static final BatchStateGuard BATCH = new BatchStateGuard();
    private static ClipRect clip = ClipRect.none();
    private static SurfaceDrawPlan lastPlan = SurfaceDrawPlan.build("", false, false, false, false, false, false);

    private Sts1RenderPipeline() {}

    public static void setOverlayObserve(boolean enabled) {
        overlayObserve = enabled;
    }

    public static boolean isOverlayObserve() {
        return overlayObserve;
    }

    public static void setClip(ClipRect rect) {
        clip = rect != null ? rect : ClipRect.none();
    }

    public static ClipRect clip() {
        return clip;
    }

    public static BatchStateGuard batchGuard() {
        return BATCH;
    }

    public static SurfaceDrawPlan lastPlan() {
        return lastPlan;
    }

    public static SurfaceDrawPlan plan() {
        String scene = ArtFramework.projection().scene();
        lastPlan =
                SurfaceDrawPlan.build(
                        scene,
                        mounted(SurfaceIds.COMBAT_HAND),
                        mounted(SurfaceIds.COMBAT_CARD_SLOTS),
                        mounted(SurfaceIds.COMBAT_CONTROLS),
                        mounted(SurfaceIds.MAP),
                        mounted(SurfaceIds.SKELETON),
                        mounted(SurfaceIds.EVENT),
                        mounted(SurfaceIds.SELECT_GRID),
                        mounted(SurfaceIds.SELECT_HAND),
                        mounted(SurfaceIds.REWARD_COMBAT)
                                || mounted(SurfaceIds.REWARD_CARD)
                                || mounted(SurfaceIds.REWARD_BOSS_RELIC),
                        mounted(SurfaceIds.REST),
                        mounted(SurfaceIds.TREASURE),
                        mounted(SurfaceIds.SHOP),
                        mounted(SurfaceIds.TOP_PANEL),
                        mounted(SurfaceIds.COMBAT_INTENTS),
                        mounted(SurfaceIds.COMBAT_PROCEED),
                        mounted(SurfaceIds.COMBAT_ENERGY),
                        mounted(SurfaceIds.COMBAT_TARGETING),
                        overlayObserve);
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
        m.put("clipEmpty", Boolean.valueOf(clip.isEmpty()));
        m.put("batchArmed", Boolean.valueOf(BATCH.isArmed()));
        m.put("nativeRender", NativeRenderBridge.probeSlice());
        m.put("nativeRenderStrict", NativeRenderBridge.strictReport());
        return m;
    }

    public static void resetForTests() {
        overlayObserve = false;
        clip = ClipRect.none();
        BATCH.reset();
        HandDrawPath.resetForTests();
        HandRenderMetrics.resetForTests();
        lastPlan = SurfaceDrawPlan.build("", false, false, false, false, false, false);
        NativeRenderBridge.resetForTests();
    }

    private static boolean mounted(String id) {
        UiComponent c = ArtFramework.component(id);
        return c != null && c.isMounted();
    }
}
