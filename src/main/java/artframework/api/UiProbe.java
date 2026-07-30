package artframework.api;

import artframework.c1.layout.LayoutNode;
import artframework.c2.MapPin;
import artframework.c2.NativeTemplateRuntime;
import artframework.component.WidgetSession;
import artframework.component.WidgetSessions;
import artframework.c2.NativeComponents;
import artframework.core.Themes;
import artframework.core.HostBackend;
import artframework.core.UiTree;
import artframework.core.UiTrees;
import artframework.render.RenderHosts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only UI snapshot (C1 open windows + C2 bound templates). Not co-op GameStateProbe.
 */
public final class UiProbe {

    public static final int SCHEMA_VERSION = 1;
    public static final String MOD_ID = "artframework";
    public static final String PROBE_PREFIX = "ART_PROBE ";

    UiProbe() {}

    public Map<String, Object> asMap() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("schemaVersion", Integer.valueOf(SCHEMA_VERSION));
        root.put("modId", MOD_ID);
        root.put("windows", windowsMap());
        root.put("templates", templatesMap());
        root.put("map", mapMap());
        root.put("endTurn", endTurnMap());
        root.put("entities", entitiesMap());
        root.put("render", RenderHosts.get().probeMap());
        root.put("theme", artframework.core.ProjectPresent.theme().probeSummary());
        root.put("presentProfile", artframework.core.ProjectPresent.probeSummary());
        root.put("projectPresent", artframework.core.ProjectPresent.probeSummary());
        root.put("presentProfiles", artframework.core.PresentProfiles.catalogProbeSummary());
        root.put("presentPacks", artframework.core.PresentPacks.probeSummary());
        root.put("enabledPresents", artframework.core.EnabledPresents.probeSummary());
        root.put("surfacePresent", artframework.core.SurfacePresent.probeSummary());
        List<Map<String, Object>> components = new ArrayList<Map<String, Object>>();
        components.addAll(artframework.c1.SyntheticComponents.probeAll());
        components.addAll(NativeComponents.probeAll());
        root.put("components", components);
        root.put("present", artframework.context.PresentSurfaces.probeAll());
        root.put("projection", ArtFramework.projection().probeSlice());
        root.put("backend", backendMap());
        root.put("assets", ArtFramework.assets().probeAssets());
        root.put("host", hostMap());
        return root;
    }

    private static Map<String, Object> backendMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", "signals");
        m.put("mode", "LISTENER");
        // Prefer last applied projection frame — endpoint dispatch is transactional.
        artframework.context.PresentProjection proj = ArtFramework.projection();
        m.put("frameId", Long.valueOf(proj.lastFrameId()));
        m.put("sceneEpoch", Long.valueOf(proj.sceneEpoch()));
        m.put("available", Boolean.valueOf(proj.isAvailable()));
        m.put("scene", proj.scene());
        m.put(
                "combatHandPresent",
                Boolean.valueOf(artframework.sts1.FullPresentMode.isCombatHandEnabled()));
        m.put(
                "suppressNativeHand",
                Boolean.valueOf(
                        artframework.sts1.render.Sts1SurfaceRenderer.shouldSuppressNativeHand()));
        m.put("fullPresent", artframework.sts1.FullPresentMode.probeSlice());
        m.put("controls", ArtFramework.projection().controls().toMap());
        m.put("mapView", ArtFramework.projection().map().toMap());
        m.put("renderPlan", artframework.sts1.render.Sts1RenderPipeline.probeSlice());
        m.put("handDraw", artframework.sts1.render.HandDrawPath.probeSlice());
        m.put("input", artframework.sts1.input.CombatInputRouter.probeSlice());
        m.put("controlsDraw", artframework.sts1.render.ControlsDrawPath.probeSlice());
        m.put("mapDraw", artframework.sts1.render.MapDrawPath.probeSlice());
        m.put("eventDraw", artframework.sts1.render.EventDrawPath.probeSlice());
        m.put("selectDraw", artframework.sts1.render.SelectDrawPath.probeSlice());
        m.put("rewardDraw", artframework.sts1.render.RewardDrawPath.probeSlice());
        m.put("restDraw", artframework.sts1.render.RestDrawPath.probeSlice());
        m.put("treasureDraw", artframework.sts1.render.TreasureDrawPath.probeSlice());
        m.put("shopDraw", artframework.sts1.render.ShopDrawPath.probeSlice());
        m.put("topPanelDraw", artframework.sts1.render.TopPanelDrawPath.probeSlice());
        m.put("intentDraw", artframework.sts1.render.IntentDrawPath.probeSlice());
        m.put("energyDraw", artframework.sts1.render.EnergyDrawPath.probeSlice());
        m.put("proceedDraw", artframework.sts1.render.ProceedDrawPath.probeSlice());
        m.put("entityDraw", artframework.c2.EntityDrawPath.probeSlice());
        m.put("audio", artframework.sts1.audio.ArtAudioBridge.probeSlice());
        m.put("skeleton", artframework.sts1.skeleton.Sts1SkeletonBridge.probeSlice());
        m.put("safety", artframework.sts1.PresentSafety.probeSlice());
        return m;
    }

    private static Map<String, Object> hostMap() {
        HostBackend host = ArtFramework.host();
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ready", Boolean.valueOf(host.isReady()));
        out.put("capabilities", new ArrayList<String>(host.capabilities().values()));
        return out;
    }

    /** Single log/console line: prefix + compact JSON. */
    public String toJsonLine() {
        return PROBE_PREFIX + toJson(asMap());
    }

    private static Map<String, Object> windowsMap() {
        Map<String, Object> w = new LinkedHashMap<String, Object>();
        List<String> openIds = new ArrayList<String>(ArtFramework.listOpenIds());
        w.put("openIds", openIds);
        Map<String, Object> byId = new LinkedHashMap<String, Object>();
        for (String id : openIds) {
            WindowHandle h = ArtFramework.find(id);
            if (h == null) {
                continue;
            }
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("windowClass", h.windowClass().name());
            LayoutNode root = ArtFramework.layoutRoot(id);
            if (root != null) {
                one.put("title", root.title);
                List<String> buttons = new ArrayList<String>();
                collectButtons(root, buttons);
                one.put("buttonIds", buttons);
            }
            WidgetSession session = WidgetSessions.get(id);
            if (session != null) {
                Map<String, Object> controls = session.probeControls();
                one.put("controls", controls);
                @SuppressWarnings("unchecked")
                List<String> fromSession = (List<String>) controls.get("buttonIds");
                if (fromSession != null && !fromSession.isEmpty()) {
                    one.put("buttonIds", fromSession);
                }
                if (session.root() != null) {
                    String title = session.root().propString("title", null);
                    if (title != null && !title.isEmpty()) {
                        one.put("title", title);
                    }
                }
            }
            UiTree tree = UiTrees.get(id);
            if (tree != null) {
                if (tree.theme() != null) {
                    one.put("theme", tree.theme().probeSummary());
                }
                one.put("present", tree.resolvePresent().probeSummary());
            }
            byId.put(id, one);
        }
        w.put("byId", byId);
        return w;
    }

    private static void collectButtons(LayoutNode node, List<String> out) {
        if (node == null) {
            return;
        }
        if (node.type == LayoutNode.Type.BUTTON && node.id != null && !node.id.isEmpty()) {
            out.add(node.id);
        }
        for (LayoutNode c : node.children) {
            collectButtons(c, out);
        }
    }

    private static Map<String, Object> templatesMap() {
        Map<String, Object> t = new LinkedHashMap<String, Object>();
        t.put("mapBound", Boolean.valueOf(NativeTemplateRuntime.isMapBound()));
        t.put("eventBound", Boolean.valueOf(NativeTemplateRuntime.isEventBound()));
        t.put("selectGridBound", Boolean.valueOf(NativeTemplateRuntime.isSelectGridBound()));
        t.put("selectHandBound", Boolean.valueOf(NativeTemplateRuntime.isSelectHandBound()));
        t.put("endTurnBound", Boolean.valueOf(NativeTemplateRuntime.isEndTurnBound()));
        return t;
    }

    private static Map<String, Object> mapMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> pins = new ArrayList<Map<String, Object>>();
        if (NativeTemplateRuntime.isMapBound()) {
            for (MapPin pin : NativeTemplateRuntime.map().listPins()) {
                Map<String, Object> p = new LinkedHashMap<String, Object>();
                p.put("pinId", pin.pinId);
                p.put("nodeId", pin.node.row + "_" + pin.node.col);
                p.put("label", pin.label);
                pins.add(p);
            }
        }
        m.put("pins", pins);
        return m;
    }

    private static Map<String, Object> endTurnMap() {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        boolean enabled = true;
        if (NativeTemplateRuntime.isEndTurnBound()) {
            enabled = NativeTemplateRuntime.endTurn().isButtonEnabled();
        }
        e.put("buttonEnabled", Boolean.valueOf(enabled));
        return e;
    }

    private static Map<String, Object> entitiesMap() {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        artframework.c2.EntityPresent present = NativeTemplateRuntime.entities();
        List<String> ids = present.listSlotIds();
        e.put("slotCount", Integer.valueOf(ids.size()));
        List<Map<String, Object>> slots = new ArrayList<Map<String, Object>>();
        for (String id : ids) {
            artframework.c2.EntitySlot slot = present.get(id);
            if (slot == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("slotId", slot.slotId);
            row.put("kind", slot.kind.name());
            row.put("refId", slot.refId);
            row.put("laidOut", Boolean.valueOf(slot.isLaidOut()));
            row.put("x", Float.valueOf(slot.x()));
            row.put("y", Float.valueOf(slot.y()));
            row.put("scale", Float.valueOf(slot.scale()));
            artframework.c2.EntitySnapshot snap = artframework.c2.EntitySnapshot.from(slot.snapshot());
            row.put("snapshot", snap.toMap());
            slots.add(row);
        }
        e.put("slots", slots);
        e.put("cardOverlayOnly", Boolean.valueOf(artframework.c2.EntityDrawPath.cardOverlayOnly()));
        return e;
    }

    /** Minimal JSON (no external lib). Values: String, Number, Boolean, Map, List, null. */
    @SuppressWarnings("unchecked")
    static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return quote((String) value);
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(quote(e.getKey())).append(':').append(toJson(e.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(toJson(list.get(i)));
            }
            sb.append(']');
            return sb.toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
