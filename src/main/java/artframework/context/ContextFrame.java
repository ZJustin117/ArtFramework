package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable authority snapshot for one present frame. Hard-sync source for ART projection.
 * Milestone 16.0: controls and map are strong-typed views; 22.1 adds event/select views.
 * Legacy Map accessors remain as probe bridges.
 */
public final class ContextFrame {

    public final long frameId;
    /** Increments when Backend enters a new scene; old projection must not survive it. */
    public final long sceneEpoch;
    public final String scene;
    public final List<CardView> cards;
    public final ControlsView controlsView;
    public final MapView mapView;
    public final EventView eventView;
    public final SelectView selectView;
    public final RewardView rewardView;
    public final RestView restView;
    public final TreasureView treasureView;
    public final ShopView shopView;
    public final TopPanelView topPanelView;
    public final MonsterIntentView intentsView;
    public final boolean available;
    public final ViewportView viewport;

    /**
     * @deprecated Prefer {@link #controlsView}; retained as probe/legacy map bridge.
     */
    public final Map<String, Object> controls;

    /**
     * @deprecated Prefer {@link #mapView}; retained as probe/legacy map bridge.
     */
    public final Map<String, Object> map;

    public ContextFrame(
            long frameId,
            long sceneEpoch,
            String scene,
            List<CardView> cards,
            ControlsView controlsView,
            MapView mapView,
            EventView eventView,
            SelectView selectView,
            boolean available,
            ViewportView viewport) {
        this(
                frameId,
                sceneEpoch,
                scene,
                cards,
                controlsView,
                mapView,
                eventView,
                selectView,
                RewardView.empty(),
                RestView.empty(),
                TreasureView.empty(),
                ShopView.empty(),
                TopPanelView.empty(),
                MonsterIntentView.empty(),
                available,
                viewport);
    }

    public ContextFrame(
            long frameId,
            long sceneEpoch,
            String scene,
            List<CardView> cards,
            ControlsView controlsView,
            MapView mapView,
            EventView eventView,
            SelectView selectView,
            RewardView rewardView,
            RestView restView,
            TreasureView treasureView,
            ShopView shopView,
            TopPanelView topPanelView,
            MonsterIntentView intentsView,
            boolean available,
            ViewportView viewport) {
        this.frameId = frameId;
        this.sceneEpoch = sceneEpoch;
        this.scene = scene != null ? scene : "";
        if (cards == null || cards.isEmpty()) {
            this.cards = Collections.emptyList();
        } else {
            this.cards = Collections.unmodifiableList(new ArrayList<CardView>(cards));
        }
        this.controlsView = controlsView != null ? controlsView : ControlsView.empty();
        this.mapView = mapView != null ? mapView : MapView.empty();
        this.eventView = eventView != null ? eventView : EventView.empty();
        this.selectView = selectView != null ? selectView : SelectView.empty();
        this.rewardView = rewardView != null ? rewardView : RewardView.empty();
        this.restView = restView != null ? restView : RestView.empty();
        this.treasureView = treasureView != null ? treasureView : TreasureView.empty();
        this.shopView = shopView != null ? shopView : ShopView.empty();
        this.topPanelView = topPanelView != null ? topPanelView : TopPanelView.empty();
        this.intentsView = intentsView != null ? intentsView : MonsterIntentView.empty();
        this.available = available;
        this.viewport = viewport != null ? viewport : ViewportView.unavailable();
        this.controls =
                Collections.unmodifiableMap(new LinkedHashMap<String, Object>(this.controlsView.toMap()));
        this.map = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(this.mapView.toMap()));
    }

    /** Strong controls/map without event/select (defaults empty). */
    public ContextFrame(
            long frameId,
            long sceneEpoch,
            String scene,
            List<CardView> cards,
            ControlsView controlsView,
            MapView mapView,
            boolean available,
            ViewportView viewport) {
        this(
                frameId,
                sceneEpoch,
                scene,
                cards,
                controlsView,
                mapView,
                EventView.empty(),
                SelectView.empty(),
                available,
                viewport);
    }

    /** Compatibility: map-shaped controls/map (coerced into strong views). */
    public ContextFrame(
            long frameId,
            long sceneEpoch,
            String scene,
            List<CardView> cards,
            Map<String, Object> controls,
            Map<String, Object> map,
            boolean available,
            ViewportView viewport) {
        this(
                frameId,
                sceneEpoch,
                scene,
                cards,
                coerceControls(controls),
                coerceMap(map),
                EventView.empty(),
                SelectView.empty(),
                available,
                viewport);
    }

    /** Compatibility constructor for callers without an explicit scene epoch. */
    public ContextFrame(
            long frameId,
            String scene,
            List<CardView> cards,
            Map<String, Object> controls,
            Map<String, Object> map,
            boolean available) {
        this(frameId, 0L, scene, cards, controls, map, available, null);
    }

    public static ContextFrame unavailable(long frameId) {
        return new ContextFrame(
                frameId,
                0L,
                "",
                null,
                ControlsView.empty(),
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                false,
                null);
    }

    public static ContextFrame of(long frameId, String scene, List<CardView> cards) {
        return new ContextFrame(
                frameId,
                0L,
                scene,
                cards,
                ControlsView.empty(),
                MapView.empty(),
                EventView.empty(),
                SelectView.empty(),
                true,
                null);
    }

    public static ContextFrame of(
            long frameId,
            long sceneEpoch,
            String scene,
            List<CardView> cards,
            ControlsView controls,
            MapView map,
            ViewportView viewport) {
        return new ContextFrame(
                frameId,
                sceneEpoch,
                scene,
                cards,
                controls,
                map,
                EventView.empty(),
                SelectView.empty(),
                true,
                viewport);
    }

    public static ContextFrame of(
            long frameId,
            long sceneEpoch,
            String scene,
            List<CardView> cards,
            ControlsView controls,
            MapView map,
            EventView event,
            SelectView select,
            ViewportView viewport) {
        return new ContextFrame(
                frameId, sceneEpoch, scene, cards, controls, map, event, select, true, viewport);
    }

    public static ContextFrame ofFull(
            long frameId,
            long sceneEpoch,
            String scene,
            List<CardView> cards,
            ControlsView controls,
            MapView map,
            EventView event,
            SelectView select,
            RewardView reward,
            RestView rest,
            TreasureView treasure,
            ShopView shop,
            TopPanelView topPanel,
            MonsterIntentView intents,
            ViewportView viewport) {
        return new ContextFrame(
                frameId,
                sceneEpoch,
                scene,
                cards,
                controls,
                map,
                event,
                select,
                reward,
                rest,
                treasure,
                shop,
                topPanel,
                intents,
                true,
                viewport);
    }

    public List<CardView> cardsIn(CardZone zone) {
        if (zone == null || cards.isEmpty()) {
            return Collections.emptyList();
        }
        List<CardView> out = new ArrayList<CardView>();
        for (CardView c : cards) {
            if (c.zone == zone) {
                out.add(c);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public CardView findByInstance(String instanceId) {
        if (instanceId == null) {
            return null;
        }
        for (CardView c : cards) {
            if (instanceId.equals(c.ref.instanceId)) {
                return c;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ControlsView coerceControls(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return ControlsView.empty();
        }
        int energy = intVal(raw.get("energy"), 0);
        int handSize = intVal(raw.get("handSize"), 0);
        int drawSize = intVal(raw.get("drawSize"), 0);
        int discardSize = intVal(raw.get("discardSize"), 0);
        int exhaustSize = intVal(raw.get("exhaustSize"), 0);
        boolean endTurnEnabled = boolVal(raw.get("endTurnEnabled"), false);
        boolean endTurnVisible = boolVal(raw.get("endTurnVisible"), true);
        List<ControlView> list = new ArrayList<ControlView>();
        Object controlsObj = raw.get("controls");
        if (controlsObj instanceof List) {
            for (Object row : (List<?>) controlsObj) {
                if (!(row instanceof Map)) {
                    continue;
                }
                Map<String, Object> m = (Map<String, Object>) row;
                String id = str(m.get("id"));
                if (id.isEmpty()) {
                    continue;
                }
                list.add(
                        new ControlView(
                                id,
                                str(m.get("text")),
                                str(m.get("iconResourceId")),
                                boolVal(m.get("visible"), true),
                                boolVal(m.get("enabled"), true)));
            }
        }
        if (list.isEmpty() && (raw.containsKey("endTurnEnabled") || raw.containsKey("energy"))) {
            return ControlsView.combat(
                    energy, handSize, drawSize, discardSize, exhaustSize, endTurnEnabled, endTurnVisible);
        }
        return new ControlsView(
                list, energy, handSize, drawSize, discardSize, exhaustSize, endTurnEnabled, endTurnVisible);
    }

    @SuppressWarnings("unchecked")
    private static MapView coerceMap(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return MapView.empty();
        }
        int vw = intVal(raw.get("viewportWidth"), 0);
        int vh = intVal(raw.get("viewportHeight"), 0);
        List<MapNodeView> nodes = new ArrayList<MapNodeView>();
        Object nodesObj = raw.get("nodes");
        if (nodesObj instanceof List) {
            for (Object row : (List<?>) nodesObj) {
                if (!(row instanceof Map)) {
                    continue;
                }
                Map<String, Object> m = (Map<String, Object>) row;
                nodes.add(
                        new MapNodeView(
                                intVal(m.get("row"), 0),
                                intVal(m.get("col"), 0),
                                floatVal(m.get("x"), 0f),
                                floatVal(m.get("y"), 0f),
                                boolVal(m.get("taken"), false),
                                boolVal(m.get("highlighted"), false),
                                str(m.get("symbol")),
                                str(m.get("roomKind")),
                                str(m.get("resourceId"))));
            }
        }
        return new MapView(nodes, vw, vh);
    }

    private static int intVal(Object o, int def) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o != null) {
            try {
                return Integer.parseInt(String.valueOf(o));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static float floatVal(Object o, float def) {
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        if (o != null) {
            try {
                return Float.parseFloat(String.valueOf(o));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static boolean boolVal(Object o, boolean def) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        if (o != null) {
            return Boolean.parseBoolean(String.valueOf(o));
        }
        return def;
    }

    private static String str(Object o) {
        return o != null ? String.valueOf(o) : "";
    }
}
