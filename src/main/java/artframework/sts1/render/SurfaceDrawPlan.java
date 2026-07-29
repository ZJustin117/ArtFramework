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
 * FULL + mounted + scene match = ART draw (and may suppress native).
 */
public final class SurfaceDrawPlan {

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
        List<Entry> out = new ArrayList<Entry>();
        for (Entry e : entries) {
            if (e.mode == DrawMode.DRAW) {
                out.add(e);
            }
        }
        return Collections.unmodifiableList(out);
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
            boolean overlayObserve) {
        List<Entry> list = new ArrayList<Entry>();
        list.add(
                entry(
                        SurfaceIds.MAP,
                        PresentLayer.MAP,
                        FullPresentMode.mapLevel(),
                        mapMounted,
                        "map".equals(scene),
                        overlayObserve));
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
                        "reward".equals(scene) || rewardMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.REWARD_CARD,
                        PresentLayer.ROOM,
                        FullPresentMode.rewardLevel(),
                        rewardMounted,
                        "reward".equals(scene) || rewardMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.REWARD_BOSS_RELIC,
                        PresentLayer.ROOM,
                        FullPresentMode.rewardLevel(),
                        rewardMounted,
                        "reward".equals(scene) || rewardMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.REST,
                        PresentLayer.ROOM,
                        FullPresentMode.restLevel(),
                        restMounted,
                        "rest".equals(scene) || restMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.TREASURE,
                        PresentLayer.ROOM,
                        FullPresentMode.treasureLevel(),
                        treasureMounted,
                        "treasure".equals(scene) || treasureMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.SHOP,
                        PresentLayer.ROOM,
                        FullPresentMode.shopLevel(),
                        shopMounted,
                        "shop".equals(scene) || shopMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.EVENT,
                        PresentLayer.EVENT,
                        FullPresentMode.eventLevel(),
                        eventMounted,
                        eventMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.SELECT_GRID,
                        PresentLayer.SELECT,
                        FullPresentMode.selectLevel(),
                        selectGridMounted,
                        selectGridMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.SELECT_HAND,
                        PresentLayer.SELECT,
                        FullPresentMode.selectLevel(),
                        selectHandMounted,
                        selectHandMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.COMBAT_CARD_SLOTS,
                        PresentLayer.COMBAT_SLOTS,
                        FullPresentMode.combatHandLevel(),
                        slotsMounted,
                        "combat".equals(scene),
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.COMBAT_HAND,
                        PresentLayer.COMBAT_HAND,
                        FullPresentMode.combatHandLevel(),
                        handMounted,
                        "combat".equals(scene),
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.COMBAT_CONTROLS,
                        PresentLayer.COMBAT_CONTROLS,
                        FullPresentMode.combatControlsLevel(),
                        controlsMounted,
                        "combat".equals(scene),
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.COMBAT_PROCEED,
                        PresentLayer.COMBAT_CONTROLS,
                        FullPresentMode.proceedLevel(),
                        proceedMounted,
                        "combat".equals(scene) || proceedMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.COMBAT_ENERGY,
                        PresentLayer.COMBAT_CONTROLS,
                        FullPresentMode.energyLevel(),
                        energyMounted,
                        "combat".equals(scene) || energyMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.COMBAT_INTENTS,
                        PresentLayer.COMBAT_INTENTS,
                        FullPresentMode.intentsLevel(),
                        intentsMounted,
                        "combat".equals(scene) || intentsMounted,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.SKELETON,
                        PresentLayer.SKELETON,
                        FullPresentMode.skeletonLevel(),
                        skeletonMounted,
                        true,
                        overlayObserve));
        list.add(
                entry(
                        SurfaceIds.TOP_PANEL,
                        PresentLayer.TOP_PANEL,
                        FullPresentMode.topPanelLevel(),
                        topPanelMounted,
                        topPanelMounted || roomScene || "combat".equals(scene) || "map".equals(scene),
                        overlayObserve));
        return new SurfaceDrawPlan(scene, overlayObserve, list);
    }

    private static Entry entry(
            String surfaceId,
            PresentLayer layer,
            PresentLevel level,
            boolean mounted,
            boolean sceneOk,
            boolean overlayObserve) {
        FullPresentCapability capability = FullPresentCapability.resolve(
                level,
                mounted,
                sceneOk,
                CombatInputRouter.isExecutorReady(surfaceId),
                overlayObserve,
                artframework.sts1.PresentSafety.isPanic());
        DrawMode mode = capability.shouldDraw() ? DrawMode.DRAW
                : capability.state == FullPresentCapability.State.OBSERVING ? DrawMode.OBSERVE : DrawMode.SKIP;
        return new Entry(surfaceId, layer, mode, level, mounted, capability.shouldSuppressNative(), capability.state, capability.reason);
    }
}
