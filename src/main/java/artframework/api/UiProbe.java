package artframework.api;

import artframework.c2.MapPin;
import artframework.c2.NativeTemplateRuntime;
import artframework.c2.NativeComponents;
import artframework.core.Themes;
import artframework.core.HostBackend;
import artframework.render.RenderHosts;
import artframework.ecs.EntityId;
import artframework.presentation.ControlValueComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.NodePropertiesComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.PresentationRuntime;

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
        root.put("presentation", artframework.presentation.PresentationRegistry.probeAll());
        root.put("presentationFrames", artframework.presentation.PresentationRegistry.frames().size());
        if (artframework.sts1.StsRuntimeReady.hasStarted()
                && !artframework.sts1.StsRuntimeReady.isReady()) {
            root.put("lab", labMap());
            root.put("host", hostMap());
            return root;
        }
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
        root.put("lab", labMap());
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
        m.put("handRender", artframework.sts1.render.HandRenderMetrics.probeSlice());
        m.put("input", artframework.sts1.input.CombatInputRouter.probeSlice());
        m.put("controlsDraw", artframework.sts1.render.ControlsDrawPath.probeSlice());
        m.put("mapDraw", artframework.sts1.render.MapDrawPath.probeSlice());
        m.put("mapIntent", artframework.sts1.input.Sts1MapIntentBridge.probeSlice());
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

    private static Map<String, Object> labMap() {
        // `art probe` is callable while CardCrawlGame is constructing its card library. Avoid
        // loading AbstractDungeon reflectively until STS has completed post-initialize.
        if (!artframework.sts1.StsRuntimeReady.isReady()) {
            return artframework.sts1.lab.LabStateSnapshot.builder()
                    .message("host not ready")
                    .build()
                    .toMap();
        }
        Map<String, Object> m = artframework.sts1.lab.StsLabNav.dump().toMap();
        m.put("recipe", artframework.sts1.lab.LabRecipeRunner.statusMap());
        return m;
    }

    /** Single log/console line: prefix + compact JSON. */
    public String toJsonLine() {
        return PROBE_PREFIX + toJson(asMap());
    }

    private static Map<String, Object> windowsMap() {
        Map<String, Object> w = new LinkedHashMap<String, Object>();
        List<String> openIds = new ArrayList<String>(PresentationRuntime.openWindowIds());
        for (String id : NativeComponents.ids()) {
            if (!openIds.contains(id) && NativeComponents.get(id) != null
                    && NativeComponents.get(id).isMounted()) {
                openIds.add(id);
            }
        }
        w.put("openIds", openIds);
        Map<String, Object> byId = new LinkedHashMap<String, Object>();
        for (String id : openIds) {
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("windowClass", PresentationRuntime.isOpen(id)
                    ? WindowClass.SYNTHETIC.name() : WindowClass.NATIVE_TEMPLATE.name());
            PresentationContext context = PresentationRegistry.existingContext("tree:" + id);
            if (context != null) {
                Map<String, Object> controls = controls(context);
                one.put("controls", controls);
                one.put("buttonIds", controls.get("buttonIds"));
                for (EntityId entity : context.entities()) {
                    NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
                    NodePropertiesComponent properties = context.world().get(
                            entity, NodePropertiesComponent.class);
                    if (identity != null && properties != null && "window".equals(identity.type)) {
                        Object title = properties.get("title");
                        if (title != null && !String.valueOf(title).isEmpty()) {
                            one.put("title", String.valueOf(title));
                        }
                        break;
                    }
                }
                one.put("presentationEntities", Integer.valueOf(context.entities().size()));
                artframework.core.PresentResolved present = present(context);
                one.put("theme", present.theme.probeSummary());
                one.put("present", present.probeSummary());
            }
            byId.put(id, one);
        }
        w.put("byId", byId);
        return w;
    }

    private static Map<String, Object> controls(PresentationContext context) {
        List<String> buttons = new ArrayList<String>();
        List<String> sliders = new ArrayList<String>();
        List<String> hitAreas = new ArrayList<String>();
        List<String> textFields = new ArrayList<String>();
        List<String> checkboxes = new ArrayList<String>();
        List<String> progress = new ArrayList<String>();
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        Map<String, Object> sliderValues = new LinkedHashMap<String, Object>();
        Map<String, Object> textValues = new LinkedHashMap<String, Object>();
        Map<String, Object> checkboxValues = new LinkedHashMap<String, Object>();
        Map<String, Object> progressValues = new LinkedHashMap<String, Object>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            if (identity == null) continue;
            String type = identity.type;
            String name = identity.name;
            if (artframework.component.UiTypes.BUTTON.equals(type)) buttons.add(name);
            if (artframework.component.UiTypes.HITAREA.equals(type)) hitAreas.add(name);
            if (artframework.component.UiTypes.SLIDER.equals(type)) sliders.add(name);
            if (artframework.component.UiTypes.TEXTFIELD.equals(type)) textFields.add(name);
            if (artframework.component.UiTypes.CHECKBOX.equals(type)) checkboxes.add(name);
            if (artframework.component.UiTypes.PROGRESS.equals(type)) progress.add(name);
            ControlValueComponent value = context.world().get(entity, ControlValueComponent.class);
            if (value == null) continue;
            values.put(name, value.value);
            if (artframework.component.UiTypes.SLIDER.equals(type)) sliderValues.put(name, value.value);
            if (artframework.component.UiTypes.TEXTFIELD.equals(type)) textValues.put(name, value.value);
            if (artframework.component.UiTypes.CHECKBOX.equals(type)) checkboxValues.put(name, value.value);
            if (artframework.component.UiTypes.PROGRESS.equals(type)) progressValues.put(name, value.value);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("buttonIds", buttons);
        out.put("sliderIds", sliders);
        out.put("hitAreaIds", hitAreas);
        out.put("textFieldIds", textFields);
        out.put("checkboxIds", checkboxes);
        out.put("progressIds", progress);
        out.put("values", values);
        out.put("sliders", sliderValues);
        out.put("texts", textValues);
        out.put("checkboxes", checkboxValues);
        out.put("progress", progressValues);
        return out;
    }

    /** Resolve root declaration profile data directly from the ECS context. */
    private static artframework.core.PresentResolved present(PresentationContext context) {
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            NodePropertiesComponent properties = context.world().get(entity, NodePropertiesComponent.class);
            if (identity == null || properties == null || !"window".equals(identity.type)) continue;
            Object profileId = properties.get("present_profile");
            if (profileId == null) profileId = properties.get("presentProfile");
            if (profileId != null) {
                artframework.core.PresentProfile profile = artframework.core.PresentProfiles.get(
                        String.valueOf(profileId));
                if (profile != null) return new artframework.core.PresentResolved(
                        profile.id, profile.theme, profile.chrome, profile.packId, false);
            }
            Object themeName = properties.get("theme");
            if (themeName != null) {
                artframework.core.Theme theme = Themes.get(String.valueOf(themeName));
                if (theme != null) return new artframework.core.PresentResolved(
                        theme.name(), theme, artframework.core.PresentChromeStyle.fromTheme(theme), "", false);
            }
            break;
        }
        return artframework.core.ProjectPresent.resolved();
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
            for (MapPin pin : NativeTemplateRuntime.mapPins()) {
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
            enabled = NativeTemplateRuntime.isEndTurnEnabled();
        }
        e.put("buttonEnabled", Boolean.valueOf(enabled));
        return e;
    }

    private static Map<String, Object> entitiesMap() {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        List<artframework.c2.EntitySlot> entitySlots = artframework.c2.EntityPresentViews.list();
        e.put("slotCount", Integer.valueOf(entitySlots.size()));
        List<Map<String, Object>> slots = new ArrayList<Map<String, Object>>();
        for (artframework.c2.EntitySlot slot : entitySlots) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("slotId", slot.slotId);
            row.put("kind", slot.kind.name());
            row.put("refId", slot.refId);
            row.put("laidOut", Boolean.valueOf(slot.isLaidOut()));
            row.put("x", Float.valueOf(slot.x()));
            row.put("y", Float.valueOf(slot.y()));
            row.put("scale", Float.valueOf(slot.scale()));
            artframework.c2.EntitySnapshot snap = slot.snapshot() != null
                    ? slot.snapshot() : artframework.c2.EntitySnapshot.empty();
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
