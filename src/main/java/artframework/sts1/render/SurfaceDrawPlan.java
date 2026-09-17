package artframework.sts1.render;

import artframework.sts1.FullPresentMode;
import artframework.sts1.FullPresentCapability;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.PresentLevel;
import artframework.context.SurfaceIds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure plan of which full-present surfaces draw this frame and in which mode (16.3).
 * OBSERVE = projection/probe only, no ART draw and no native suppress.
 * FULL + mounted + scene match = ART draw (and may suppress native for ART_DELEGATED
 * surfaces). Delegation is not a complete-pixel claim while the SDD records exposed
 * pixel-supply gaps.
 */
public final class SurfaceDrawPlan {

    private static final String[] READINESS_SURFACES = {
        SurfaceIds.COMBAT_HAND, SurfaceIds.COMBAT_CARD_SLOTS, SurfaceIds.COMBAT_CONTROLS,
        SurfaceIds.MAP, SurfaceIds.EVENT, SurfaceIds.SELECT_GRID, SurfaceIds.SELECT_HAND,
        SurfaceIds.REWARD_COMBAT, SurfaceIds.REST, SurfaceIds.TREASURE, SurfaceIds.SHOP,
        SurfaceIds.TOP_PANEL, SurfaceIds.COMBAT_INTENTS, SurfaceIds.COMBAT_PROCEED,
        SurfaceIds.COMBAT_ENERGY, SurfaceIds.COMBAT_TARGETING, SurfaceIds.SKELETON,
        SurfaceIds.REWARD_CARD, SurfaceIds.REWARD_BOSS_RELIC
    };

    public enum DrawMode {
        SKIP,
        OBSERVE,
        DRAW
    }

    public static final class Entry {
        public final String surfaceId;
        public final PresentLayer layer;
        public final DrawMode mode;
        public final PresentLevel level;
        public final boolean mounted;
        public final boolean suppressNative;
        public final FullPresentCapability.State effectiveState;
        public final String reason;

        public Entry(
                String surfaceId,
                PresentLayer layer,
                DrawMode mode,
                PresentLevel level,
                boolean mounted,
                boolean suppressNative,
                FullPresentCapability.State effectiveState,
                String reason) {
            this.surfaceId = surfaceId;
            this.layer = layer;
            this.mode = mode;
            this.level = level;
            this.mounted = mounted;
            this.suppressNative = suppressNative;
            this.effectiveState = effectiveState;
            this.reason = reason;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("surfaceId", surfaceId);
            m.put("layer", layer.name());
            m.put("mode", mode.name());
            m.put("level", level.name());
            m.put("mounted", Boolean.valueOf(mounted));
            m.put("suppressNative", Boolean.valueOf(suppressNative));
            m.put("effectiveState", effectiveState.name());
            m.put("reason", reason);
            return m;
        }
    }

    private final List<Entry> entries;
    private final List<Entry> drawOrder;
    private final String scene;
    private final boolean overlayObserve;

    public SurfaceDrawPlan(String scene, boolean overlayObserve, List<Entry> entries) {
        this.scene = scene != null ? scene : "";
        this.overlayObserve = overlayObserve;
        if (entries == null || entries.isEmpty()) {
            this.entries = Collections.emptyList();
        } else {
            this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
        }
        List<Entry> ordered = new ArrayList<Entry>();
        for (Entry entry : this.entries) {
            if (entry.mode == DrawMode.DRAW) ordered.add(entry);
        }
        this.drawOrder = Collections.unmodifiableList(ordered);
    }

    public String scene() {
        return scene;
    }

    public boolean overlayObserve() {
        return overlayObserve;
    }

    public List<Entry> entries() {
        return entries;
    }

    public Entry find(String surfaceId) {
        String id = SurfaceIds.canonicalize(surfaceId);
        for (Entry e : entries) {
            if (e.surfaceId.equals(id) || e.surfaceId.equals(surfaceId)) {
                return e;
            }
        }
        return null;
    }

    public boolean shouldDraw(String surfaceId) {
        Entry e = find(surfaceId);
        return e != null && e.mode == DrawMode.DRAW;
    }

    public boolean shouldSuppressNative(String surfaceId) {
        Entry e = find(surfaceId);
        return e != null && e.suppressNative;
    }

    public List<Entry> drawOrder() {
        return drawOrder;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("scene", scene);
        m.put("overlayObserve", Boolean.valueOf(overlayObserve));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Entry e : entries) {
            list.add(e.toMap());
        }
        m.put("entries", list);
        m.put("drawCount", Integer.valueOf(drawOrder().size()));
        return m;
    }

    /**
     * Build plan from present levels + mount flags + scene. {@code overlayObserve} forces DRAW
     * surfaces into OBSERVE (debug: keep native, still project).
     */
    public static SurfaceDrawPlan build(
            String scene,
            boolean handMounted,
            boolean slotsMounted,
            boolean controlsMounted,
            boolean mapMounted,
            boolean skeletonMounted,
            boolean overlayObserve) {
        return build(
                scene,
                handMounted,
                slotsMounted,
                controlsMounted,
                mapMounted,
                skeletonMounted,
                false,
                false,
                false,
                overlayObserve);
    }

    public static SurfaceDrawPlan build(
            String scene,
            boolean handMounted,
            boolean slotsMounted,
            boolean controlsMounted,
            boolean mapMounted,
            boolean skeletonMounted,
            boolean eventMounted,
            boolean selectGridMounted,
            boolean selectHandMounted,
            boolean overlayObserve) {
        return build(
                scene,
                handMounted,
                slotsMounted,
                controlsMounted,
                mapMounted,
                skeletonMounted,
                eventMounted,
                selectGridMounted,
                selectHandMounted,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                overlayObserve);
    }

    public static SurfaceDrawPlan build(
            String scene,
            boolean handMounted,
            boolean slotsMounted,
            boolean controlsMounted,
            boolean mapMounted,
            boolean skeletonMounted,
            boolean eventMounted,
            boolean selectGridMounted,
            boolean selectHandMounted,
            boolean rewardMounted,
            boolean restMounted,
            boolean treasureMounted,
            boolean shopMounted,
            boolean topPanelMounted,
            boolean intentsMounted,
            boolean proceedMounted,
            boolean energyMounted,
            boolean targetingMounted,
            boolean overlayObserve) {
        return buildFromSnapshot(
                scene, handMounted, slotsMounted, controlsMounted, mapMounted, skeletonMounted,
                eventMounted, selectGridMounted, selectHandMounted, rewardMounted, restMounted,
                treasureMounted, shopMounted, topPanelMounted, intentsMounted, proceedMounted,
                energyMounted, targetingMounted, overlayObserve, readinessFlags(),
                artframework.sts1.PresentSafety.isPanic());
    }

    /** Builds from one readiness/panic sample supplied by the render pipeline. */
    static SurfaceDrawPlan buildFromSnapshot(
            String scene,
            boolean handMounted,
            boolean slotsMounted,
            boolean controlsMounted,
            boolean mapMounted,
            boolean skeletonMounted,
            boolean eventMounted,
            boolean selectGridMounted,
            boolean selectHandMounted,
            boolean rewardMounted,
            boolean restMounted,
            boolean treasureMounted,
            boolean shopMounted,
            boolean topPanelMounted,
            boolean intentsMounted,
            boolean proceedMounted,
            boolean energyMounted,
            boolean targetingMounted,
            boolean overlayObserve,
            long readinessFlags,
            boolean panic) {
        List<Entry> list = new ArrayList<Entry>();
        list.add(
                entry(
                        SurfaceIds.MAP,
                        PresentLayer.MAP,
                        FullPresentMode.mapLevel(),
                        mapMounted,
                        "map".equals(scene),
                        overlayObserve, readinessFlags, panic));
        boolean roomScene =
                "reward".equals(scene)
                        || "rest".equals(scene)
                        || "treasure".equals(scene)
                        || "shop".equals(scene);
        list.add(
                entry(
                        SurfaceIds.REWARD_COMBAT,
                        PresentLayer.ROOM,
                        FullPresentMode.rewardLevel(),
                        rewardMounted,
                        "reward".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.REWARD_CARD,
                        PresentLayer.ROOM,
                        FullPresentMode.rewardLevel(),
                        rewardMounted,
                        "reward".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.REWARD_BOSS_RELIC,
                        PresentLayer.ROOM,
                        FullPresentMode.rewardLevel(),
                        rewardMounted,
                        "reward".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.REST,
                        PresentLayer.ROOM,
                        FullPresentMode.restLevel(),
                        restMounted,
                        "rest".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.TREASURE,
                        PresentLayer.ROOM,
                        FullPresentMode.treasureLevel(),
                        treasureMounted,
                        "treasure".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.SHOP,
                        PresentLayer.ROOM,
                        FullPresentMode.shopLevel(),
                        shopMounted,
                        "shop".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.EVENT,
                        PresentLayer.EVENT,
                        FullPresentMode.eventLevel(),
                        eventMounted,
                        "event".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.SELECT_GRID,
                        PresentLayer.SELECT,
                        FullPresentMode.selectLevel(),
                        selectGridMounted,
                        "select".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.SELECT_HAND,
                        PresentLayer.SELECT,
                        FullPresentMode.selectLevel(),
                        selectHandMounted,
                        "select".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.COMBAT_CARD_SLOTS,
                        PresentLayer.COMBAT_SLOTS,
                        FullPresentMode.combatHandLevel(),
                        slotsMounted,
                        "combat".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.COMBAT_HAND,
                        PresentLayer.COMBAT_HAND,
                        FullPresentMode.combatHandLevel(),
                        handMounted,
                        "combat".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.COMBAT_CONTROLS,
                        PresentLayer.COMBAT_CONTROLS,
                        FullPresentMode.combatControlsLevel(),
                        controlsMounted,
                        "combat".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.COMBAT_PROCEED,
                        PresentLayer.COMBAT_CONTROLS,
                        FullPresentMode.proceedLevel(),
                        proceedMounted,
                        "combat".equals(scene) || "reward".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.COMBAT_ENERGY,
                        PresentLayer.COMBAT_CONTROLS,
                        FullPresentMode.energyLevel(),
                        energyMounted,
                        "combat".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.COMBAT_INTENTS,
                        PresentLayer.COMBAT_INTENTS,
                        FullPresentMode.intentsLevel(),
                        intentsMounted,
                        "combat".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.COMBAT_TARGETING,
                        PresentLayer.COMBAT_TARGETING,
                        FullPresentMode.targetingLevel(),
                        targetingMounted,
                        "combat".equals(scene),
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.SKELETON,
                        PresentLayer.SKELETON,
                        FullPresentMode.skeletonLevel(),
                        skeletonMounted,
                        true,
                        overlayObserve, readinessFlags, panic));
        list.add(
                entry(
                        SurfaceIds.TOP_PANEL,
                        PresentLayer.TOP_PANEL,
                        FullPresentMode.topPanelLevel(),
                        topPanelMounted,
                        roomScene
                                || "combat".equals(scene)
                                || "map".equals(scene)
                                || "event".equals(scene)
                                || "select".equals(scene),
                        overlayObserve, readinessFlags, panic));
        return new SurfaceDrawPlan(scene, overlayObserve, list);
    }

    private static Entry entry(
            String surfaceId,
            PresentLayer layer,
            PresentLevel level,
            boolean mounted,
            boolean sceneOk,
            boolean overlayObserve,
            long readinessFlags,
            boolean panic) {
        // Render-only surfaces need no input executor; readiness is scene + mount only.
        boolean executorReady = SurfaceIds.COMBAT_TARGETING.equals(surfaceId)
                || SurfaceIds.SKELETON.equals(surfaceId)
                || isReady(surfaceId, readinessFlags);
        FullPresentCapability capability = FullPresentCapability.resolve(
                level,
                mounted,
                sceneOk,
                executorReady,
                overlayObserve,
                panic);
        DrawMode mode = capability.shouldDraw() ? DrawMode.DRAW
                : capability.state == FullPresentCapability.State.OBSERVING ? DrawMode.OBSERVE : DrawMode.SKIP;
        // ART_DELEGATED surfaces may suppress only through the capability gate. Minimal or
        // incomplete ART pixel supply remains visible as strict ledger gaps; do not recast those
        // surfaces as native-authoritative here.
        boolean suppressNative = capability.shouldSuppressNative() && !keepsNativePixelAuthority(surfaceId);
        return new Entry(surfaceId, layer, mode, level, mounted, suppressNative, capability.state, capability.reason);
    }

    private static long readinessFlags() {
        long flags = 0L;
        for (int i = 0; i < READINESS_SURFACES.length; i++) {
            if (CombatInputRouter.isExecutorReady(READINESS_SURFACES[i])) flags |= 1L << (18 + i);
        }
        return flags;
    }

    private static boolean isReady(String surfaceId, long flags) {
        for (int i = 0; i < READINESS_SURFACES.length; i++) {
            if (READINESS_SURFACES[i].equals(surfaceId)) {
                return (flags & (1L << (18 + i))) != 0L;
            }
        }
        return false;
    }

    static boolean keepsNativePixelAuthority(String surfaceId) {
        // Targeting is observe-first: the patch always continues native rendering, even when the
        // surface is otherwise FULL_READY for observation/projection.
        if (SurfaceIds.COMBAT_TARGETING.equals(surfaceId)) {
            return true;
        }
        // No other surface is unconditionally native-pixel-authoritative. Suppressing surfaces are
        // ART_DELEGATED with SDD-visible pixel-supply gaps where base pixels are incomplete.
        // Skeletons are handled through per-instance claims in SkeletonRenderPatches rather than
        // wholesale surface authority.
        return false;
    }

    /**
     * Captures readiness and panic into one primitive value so the same sample can drive cache
     * identity and construction without allocating a snapshot object on a cache hit.
     */
    static long captureReadinessAndPanic() {
        long flags = readinessFlags();
        return artframework.sts1.PresentSafety.isPanic() ? flags | (1L << 37) : flags;
    }
}
