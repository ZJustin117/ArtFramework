package artframework.api;

import artframework.c2.EventOptionRef;
import artframework.component.MapNodeRef;
import artframework.c2.NativeTemplateRuntime;
import artframework.component.NativeTemplateIds;
import artframework.c2.SelectCardRef;
import artframework.c2.SelectKind;
import artframework.c2.SelectTemplate;
import artframework.component.SliderHandler;
import artframework.component.UiTypes;
import artframework.presentation.ControlValueComponent;
import artframework.presentation.ControlValueSystem;
import artframework.presentation.NodePropertiesComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.ecs.EntityId;
import artframework.c2.NativeComponents;
import artframework.core.SignalNames;
import artframework.core.UiComponent;
import artframework.core.SignalHandler;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalPaths;
import artframework.core.SignalSubscription;
import artframework.core.UiSignal;
import artframework.ops.NativeOpsBackend;
import artframework.ops.NoOpNativeOps;
import artframework.context.IntentNames;
import artframework.context.NativeIntentLifecycleComponent;
import artframework.sts1.input.NativeInputRecords;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Imperative UI commands over C1 buttons and C2 native templates.
 * Interceptors run before {@link NativeOpsBackend} gestures.
 */
public final class UiOps {

    private final Map<String, SignalHandler> signalHandlers =
            new LinkedHashMap<String, SignalHandler>();
    private final Map<String, SignalSubscription> signalSubscriptions =
            new LinkedHashMap<String, SignalSubscription>();

    UiOps() {}

    public UiOpResult selectCard(SelectKind kind, String cardId, int index) {
        String componentId = kind == SelectKind.GRID
                ? NativeTemplateIds.SELECT_GRID : kind == SelectKind.HAND ? NativeTemplateIds.SELECT_HAND : null;
        return componentId == null
                ? UiOpResult.unavailable("kind required")
                : invoke(componentId, "select_card", cardId, Integer.valueOf(index));
    }

    /** Internal C2 dispatch called exclusively by the native component action path. */
    public UiOpResult dispatchSelectCard(SelectKind kind, String cardId, int index) {
        String surfaceId = selectSurface(kind);
        NativeInputRecords.input(IntentNames.SELECT_CARD, surfaceId);
        if (kind == null) {
            return nativeResult(surfaceId, IntentNames.SELECT_CARD,
                    UiOpResult.unavailable("kind required"), false, "kind_required");
        }
        if (selectTemplate(kind) == null || !selectTemplate(kind).isActive()) {
            return nativeResult(surfaceId, IntentNames.SELECT_CARD,
                    UiOpResult.notBound("select " + kind + " not bound"), false, "select_unbound");
        }
        if (rejected(selectName(kind), SignalNames.CARD_SELECTED, new SelectCardRef(cardId, index))) {
            return nativeResult(surfaceId, IntentNames.SELECT_CARD,
                    UiOpResult.blocked("select card blocked"), false, "signal_blocked");
        }
        return nativeResult(surfaceId, IntentNames.SELECT_CARD,
                backend().selectCard(kind, cardId != null ? cardId : "", index), true, "allowed");
    }

    public UiOpResult confirmSelect(SelectKind kind) {
        String componentId = kind == SelectKind.GRID
                ? NativeTemplateIds.SELECT_GRID : kind == SelectKind.HAND ? NativeTemplateIds.SELECT_HAND : null;
        return componentId == null
                ? UiOpResult.unavailable("kind required")
                : invoke(componentId, "confirm");
    }

    /** Internal C2 dispatch called exclusively by the native component action path. */
    public UiOpResult dispatchConfirmSelect(SelectKind kind) {
        String surfaceId = selectSurface(kind);
        NativeInputRecords.input(IntentNames.CONFIRM_SELECT, surfaceId);
        if (kind == null) {
            return nativeResult(surfaceId, IntentNames.CONFIRM_SELECT,
                    UiOpResult.unavailable("kind required"), false, "kind_required");
        }
        SelectTemplate t = selectTemplate(kind);
        if (t == null || !t.isActive()) {
            return nativeResult(surfaceId, IntentNames.CONFIRM_SELECT,
                    UiOpResult.notBound("select " + kind + " not bound"), false, "select_unbound");
        }
        if (rejected(selectName(kind), SignalNames.CONFIRMED, kind)) {
            return nativeResult(surfaceId, IntentNames.CONFIRM_SELECT,
                    UiOpResult.blocked("confirm blocked"), false, "signal_blocked");
        }
        return nativeResult(surfaceId, IntentNames.CONFIRM_SELECT,
                backend().confirmSelect(kind), true, "allowed");
    }

    public UiOpResult clickMapNode(MapNodeRef node) {
        artframework.core.UiComponent surface =
                ArtFramework.component(artframework.context.SurfaceIds.MAP);
        // Prefer full-present map surface when mounted and legacy template is not the active bind.
        if (surface != null && surface.isMounted() && !NativeTemplateRuntime.isMapBound()) {
            return surface.action(
                    artframework.context.IntentNames.CLICK_MAP_NODE, node);
        }
        return invoke(NativeTemplateIds.MAP, "click_node", node);
    }

    /** Internal C2 dispatch called exclusively by the native component action path. */
    public UiOpResult dispatchMapNode(MapNodeRef node) {
        NativeInputRecords.input(IntentNames.CLICK_MAP_NODE, artframework.context.SurfaceIds.MAP);
        if (node == null) {
            return nativeResult(artframework.context.SurfaceIds.MAP, IntentNames.CLICK_MAP_NODE,
                    UiOpResult.unavailable("node required"), false, "node_required");
        }
        if (!NativeTemplateRuntime.isMapBound()) {
            return nativeResult(artframework.context.SurfaceIds.MAP, IntentNames.CLICK_MAP_NODE,
                    UiOpResult.notBound("sts1.map not bound"), false, "map_unbound");
        }
        if (rejected(NativeTemplateIds.MAP, SignalNames.NODE_CLICKED, node)) {
            return nativeResult(artframework.context.SurfaceIds.MAP, IntentNames.CLICK_MAP_NODE,
                    UiOpResult.blocked("map node blocked"), false, "signal_blocked");
        }
        return nativeResult(artframework.context.SurfaceIds.MAP, IntentNames.CLICK_MAP_NODE,
                backend().clickMapNode(node), true, "allowed");
    }

    public UiOpResult chooseEventOption(int index, String label) {
        return invoke(NativeTemplateIds.EVENT, "choose_option", Integer.valueOf(index), label);
    }

    /** Internal C2 dispatch called exclusively by the native component action path. */
    public UiOpResult dispatchEventOption(int index, String label) {
        NativeInputRecords.input(IntentNames.CHOOSE_EVENT_OPTION, artframework.context.SurfaceIds.EVENT);
        if (!NativeTemplateRuntime.isEventBound()) {
            return nativeResult(artframework.context.SurfaceIds.EVENT, IntentNames.CHOOSE_EVENT_OPTION,
                    UiOpResult.notBound("sts1.event not bound"), false, "event_unbound");
        }
        if (rejected(NativeTemplateIds.EVENT, SignalNames.OPTION_CHOSEN,
                new EventOptionRef(index, label))) {
            return nativeResult(artframework.context.SurfaceIds.EVENT, IntentNames.CHOOSE_EVENT_OPTION,
                    UiOpResult.blocked("event option blocked"), false, "signal_blocked");
        }
        return nativeResult(artframework.context.SurfaceIds.EVENT, IntentNames.CHOOSE_EVENT_OPTION,
                backend().chooseEventOption(index, label != null ? label : ""), true, "allowed");
    }

    public UiOpResult pressEndTurn() {
        return invoke(NativeTemplateIds.END_TURN, "press");
    }

    /** Internal C2 dispatch called exclusively by the native component action path. */
    public UiOpResult dispatchEndTurn() {
        NativeInputRecords.input(IntentNames.PRESS_END_TURN, artframework.context.SurfaceIds.END_TURN);
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return nativeResult(artframework.context.SurfaceIds.END_TURN, IntentNames.PRESS_END_TURN,
                    UiOpResult.notBound("sts1.endturn not bound"), false, "endturn_unbound");
        }
        if (!NativeTemplateRuntime.endTurn().isButtonEnabled()
                || rejected(NativeTemplateIds.END_TURN, SignalNames.PRESSED, null)) {
            return nativeResult(artframework.context.SurfaceIds.END_TURN, IntentNames.PRESS_END_TURN,
                    UiOpResult.blocked("end turn blocked"), false, "endturn_blocked");
        }
        return nativeResult(artframework.context.SurfaceIds.END_TURN, IntentNames.PRESS_END_TURN,
                backend().pressEndTurn(), true, "allowed");
    }

    /**
     * Combat hand play: prefers full-present {@code play_card} on hand surface when mounted;
     * otherwise legacy gesture backend (no queue/protocol).
     */
    public UiOpResult playHandCard(String cardId, String target) {
        if (cardId == null || cardId.isEmpty()) {
            return UiOpResult.unavailable("cardId required");
        }
        UiComponent hand =
                ArtFramework.component(artframework.context.SurfaceIds.COMBAT_HAND);
        if (hand != null && hand.isMounted()) {
            artframework.context.CardEntity match = null;
            for (artframework.context.CardEntity e :
                    ArtFramework.projection().listZone(
                            artframework.context.CardZone.HAND)) {
                if (cardId.equals(e.cardId)) {
                    match = e;
                    break;
                }
            }
            if (match != null) {
                return hand.action(
                        "play_card",
                        new artframework.context.CardRef(match.instanceId, match.cardId),
                        target != null ? target : "");
            }
            return hand.action("play_card", cardId, target != null ? target : "");
        }
        return backend().playHandCard(cardId, target != null ? target : "");
    }

    /** Full-present play by stable instance id. */
    public UiOpResult playHandCardRef(artframework.context.CardRef ref, String target) {
        if (ref == null) {
            return UiOpResult.unavailable("CardRef required");
        }
        return invoke(
                artframework.context.SurfaceIds.COMBAT_HAND,
                "play_card",
                ref,
                target != null ? target : "");
    }

    /** Register a pure handler for {@link #clickButton} (C1). */
    public void onButton(String windowId, String buttonId, Runnable handler) {
        if (windowId == null || buttonId == null || handler == null) {
            throw new IllegalArgumentException("windowId, buttonId, handler required");
        }
        connectSugar(windowId, buttonId, SignalNames.PRESSED, new SignalHandler() {
            @Override
            public void handle(Object... args) {
                handler.run();
            }
        });
    }

    public void removeButtonHandler(String windowId, String buttonId) {
        if (windowId == null || buttonId == null) {
            return;
        }
        disconnectSugar(windowId, buttonId, SignalNames.PRESSED);
    }

    public void onSlider(String windowId, String sliderId, SliderHandler handler) {
        if (windowId == null || sliderId == null || handler == null) {
            throw new IllegalArgumentException("windowId, sliderId, handler required");
        }
        connectSugar(windowId, sliderId, SignalNames.VALUE_CHANGED, new SignalHandler() {
            @Override
            public void handle(Object... args) {
                if (args != null && args.length > 0 && args[0] instanceof Number) {
                    handler.onChange(((Number) args[0]).floatValue());
                }
            }
        });
    }

    public void removeSliderHandler(String windowId, String sliderId) {
        if (windowId == null || sliderId == null) {
            return;
        }
        disconnectSugar(windowId, sliderId, SignalNames.VALUE_CHANGED);
    }

    public void onHitArea(String windowId, String hitAreaId, Runnable handler) {
        if (windowId == null || hitAreaId == null || handler == null) {
            throw new IllegalArgumentException("windowId, hitAreaId, handler required");
        }
        connectSugar(windowId, hitAreaId, SignalNames.PRESSED, new SignalHandler() {
            @Override
            public void handle(Object... args) {
                handler.run();
            }
        });
    }

    public void removeHitAreaHandler(String windowId, String hitAreaId) {
        if (windowId == null || hitAreaId == null) {
            return;
        }
        disconnectSugar(windowId, hitAreaId, SignalNames.PRESSED);
    }

    public UiOpResult clickButton(String windowId, String buttonId) {
        if (windowId == null || buttonId == null) {
            return UiOpResult.unavailable("windowId and buttonId required");
        }
        WindowHandle h = ArtFramework.find(windowId);
        if (h == null || !h.isOpen() || h.windowClass() != WindowClass.SYNTHETIC) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        if (!pressableControlExists(windowId, buttonId)) {
            return UiOpResult.unavailable("button not in layout: " + buttonId);
        }
        if (!allowSignal(windowId, buttonId, SignalNames.PRESSED)) {
            return UiOpResult.blocked("button blocked: " + windowId + "/" + buttonId);
        }
        // Declared buttons always succeed after emit (animation triggers / labs may be sole listeners).
        return UiOpResult.ok();
    }

    /**
     * Set slider value on an open composition session (clamped). Invokes {@link #onSlider} if set.
     */
    public UiOpResult setSlider(String windowId, String sliderId, float value) {
        if (windowId == null || sliderId == null) {
            return UiOpResult.unavailable("windowId and sliderId required");
        }
        WindowHandle h = ArtFramework.find(windowId);
        if (h == null || !h.isOpen() || h.windowClass() != WindowClass.SYNTHETIC) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        EntityId slider = control(windowId, sliderId, UiTypes.SLIDER);
        if (slider == null) {
            return UiOpResult.unavailable("slider not in layout: " + sliderId);
        }
        if (!finite(value)) {
            return UiOpResult.unavailable("slider value must be finite");
        }
        float clamped = normalizedNumber(slider, value);
        if (!allowSignal(windowId, sliderId, SignalNames.VALUE_CHANGED, Float.valueOf(clamped))) {
            return UiOpResult.blocked("slider blocked: " + windowId + "/" + sliderId);
        }
        setControlValue(slider, Float.valueOf(clamped));
        syncNodeProperty(windowId, sliderId, "value", Float.valueOf(clamped));
        try {
            artframework.render.LightwaveControls.applyIntensity(windowId, sliderId, clamped);
        } catch (Throwable ignored) {
        }
        return UiOpResult.ok();
    }

    public UiOpResult clickHitArea(String windowId, String hitAreaId) {
        if (windowId == null || hitAreaId == null) {
            return UiOpResult.unavailable("windowId and hitAreaId required");
        }
        WindowHandle h = ArtFramework.find(windowId);
        if (h == null || !h.isOpen() || h.windowClass() != WindowClass.SYNTHETIC) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        if (!controlExists(windowId, hitAreaId, UiTypes.HITAREA)) {
            return UiOpResult.unavailable("hitarea not in layout: " + hitAreaId);
        }
        if (!allowSignal(windowId, hitAreaId, SignalNames.PRESSED)) {
            return UiOpResult.blocked("hitarea blocked: " + windowId + "/" + hitAreaId);
        }
        return UiOpResult.ok();
    }

    /**
     * Unified click for button or hitarea (Bind).
     */
    public UiOpResult click(String windowId, String controlId) {
        if (windowId == null || controlId == null) {
            return UiOpResult.unavailable("windowId and controlId required");
        }
        if (control(windowId, controlId, UiTypes.BUTTON) != null) {
            return clickButton(windowId, controlId);
        }
        if (control(windowId, controlId, UiTypes.HITAREA) != null) {
            return clickHitArea(windowId, controlId);
        }
        if (control(windowId, controlId, UiTypes.CHECKBOX) != null) {
            return toggleCheckbox(windowId, controlId);
        }
        return UiOpResult.unavailable("not clickable: " + controlId);
    }

    public UiOpResult setText(String windowId, String fieldId, String text) {
        if (windowId == null || fieldId == null) {
            return UiOpResult.unavailable("windowId and fieldId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        EntityId field = control(windowId, fieldId, UiTypes.TEXTFIELD);
        if (field == null) {
            return UiOpResult.unavailable("textfield not in layout: " + fieldId);
        }
        String t = text != null ? text : "";
        if (!allowSignal(windowId, fieldId, SignalNames.TEXT_CHANGED, t)) {
            return UiOpResult.blocked("textfield blocked: " + windowId + "/" + fieldId);
        }
        setControlValue(field, t);
        syncNodeProperty(windowId, fieldId, "text", t);
        return UiOpResult.ok();
    }

    public UiOpResult submitText(String windowId, String fieldId) {
        if (windowId == null || fieldId == null) {
            return UiOpResult.unavailable("windowId and fieldId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        EntityId field = control(windowId, fieldId, UiTypes.TEXTFIELD);
        if (field == null) {
            return UiOpResult.unavailable("textfield not in layout: " + fieldId);
        }
        String text = String.valueOf(controlValue(field));
        if (!allowSignal(windowId, fieldId, SignalNames.TEXT_SUBMITTED, text)) {
            return UiOpResult.blocked("textfield blocked: " + windowId + "/" + fieldId);
        }
        return UiOpResult.ok();
    }

    public UiOpResult setChecked(String windowId, String checkboxId, boolean checked) {
        if (windowId == null || checkboxId == null) {
            return UiOpResult.unavailable("windowId and checkboxId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        EntityId checkbox = control(windowId, checkboxId, UiTypes.CHECKBOX);
        if (checkbox == null) {
            return UiOpResult.unavailable("checkbox not in layout: " + checkboxId);
        }
        boolean v = checked;
        if (!allowSignal(windowId, checkboxId, SignalNames.TOGGLED, Boolean.valueOf(v))) {
            return UiOpResult.blocked("checkbox blocked: " + windowId + "/" + checkboxId);
        }
        setControlValue(checkbox, Boolean.valueOf(v));
        syncNodeProperty(windowId, checkboxId, "checked", Boolean.valueOf(v));
        return UiOpResult.ok();
    }

    public UiOpResult toggleCheckbox(String windowId, String checkboxId) {
        if (windowId == null || checkboxId == null) {
            return UiOpResult.unavailable("windowId and checkboxId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        EntityId checkbox = control(windowId, checkboxId, UiTypes.CHECKBOX);
        if (checkbox == null) {
            return UiOpResult.unavailable("checkbox not in layout: " + checkboxId);
        }
        boolean v = !Boolean.TRUE.equals(controlValue(checkbox));
        if (!allowSignal(windowId, checkboxId, SignalNames.TOGGLED, Boolean.valueOf(v))) {
            return UiOpResult.blocked("checkbox blocked: " + windowId + "/" + checkboxId);
        }
        setControlValue(checkbox, Boolean.valueOf(v));
        syncNodeProperty(windowId, checkboxId, "checked", Boolean.valueOf(v));
        return UiOpResult.ok();
    }

    public UiOpResult setProgress(String windowId, String progressId, float value) {
        if (windowId == null || progressId == null) {
            return UiOpResult.unavailable("windowId and progressId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        EntityId progress = control(windowId, progressId, UiTypes.PROGRESS);
        if (progress == null) {
            return UiOpResult.unavailable("progress not in layout: " + progressId);
        }
        if (!finite(value)) {
            return UiOpResult.unavailable("progress value must be finite");
        }
        float clamped = normalizedNumber(progress, value);
        if (!allowSignal(windowId, progressId, SignalNames.VALUE_CHANGED, Float.valueOf(clamped))) {
            return UiOpResult.blocked("progress blocked: " + windowId + "/" + progressId);
        }
        setControlValue(progress, Float.valueOf(clamped));
        syncNodeProperty(windowId, progressId, "value", Float.valueOf(clamped));
        return UiOpResult.ok();
    }

    private static boolean isOpenSynthetic(String windowId) {
        WindowHandle h = ArtFramework.find(windowId);
        return h != null && h.isOpen() && h.windowClass() == WindowClass.SYNTHETIC;
    }

    private static EntityId control(String windowId, String controlId, String type) {
        PresentationContext context = PresentationRuntime.context(windowId);
        EntityId entity = PresentationRuntime.find(context, controlId);
        artframework.presentation.NodeIdentityComponent identity =
                PresentationRuntime.identity(context, entity);
        return identity != null && type.equals(identity.type) ? entity : null;
    }

    private static Object controlValue(EntityId node) {
        PresentationContext context = currentContext(node);
        ControlValueComponent value = PresentationRuntime.component(context, node, ControlValueComponent.class);
        return value != null ? value.value : null;
    }

    private static void setControlValue(EntityId node, Object value) {
        PresentationContext context = currentContext(node);
        context.world().put(node, ControlValueComponent.class,
                new ControlValueComponent(value));
    }

    private static float normalizedNumber(EntityId node, float requested) {
        PresentationContext context = currentContext(node);
        artframework.presentation.ControlBoundsComponent bounds =
                PresentationRuntime.component(context, node,
                        artframework.presentation.ControlBoundsComponent.class);
        float min = bounds != null ? bounds.min : 0f;
        float max = bounds != null ? bounds.max : 1f;
        return Math.max(min, Math.min(max, requested));
    }

    private static PresentationContext currentContext(EntityId entity) {
        for (String windowId : PresentationRuntime.openWindowIds()) {
            PresentationContext context = PresentationRuntime.context(windowId);
            if (context != null && context.world().contains(entity)) return context;
        }
        throw new IllegalArgumentException("unknown presentation entity: " + entity);
    }

    private static float number(Object value, float fallback) {
        if (value instanceof Number) {
            float result = ((Number) value).floatValue();
            return finite(result) ? result : fallback;
        }
        if (value != null) {
            try {
                float result = Float.parseFloat(String.valueOf(value));
                return finite(result) ? result : fallback;
            }
            catch (NumberFormatException ignored) { }
        }
        return fallback;
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    /**
     * Route an action to a {@link UiComponent} (C2 native host or future synthetic).
     * Named C2 methods remain sugar that implement the real gate + gesture path.
     */
    public UiOpResult invoke(String componentId, String action, Object... args) {
        if (componentId == null || componentId.isEmpty()) {
            return UiOpResult.unavailable("componentId required");
        }
        if (action == null || action.isEmpty()) {
            return UiOpResult.unavailable("action required");
        }
        UiComponent c = ArtFramework.component(componentId);
        if (c == null) {
            String canon = artframework.context.SurfaceIds.canonicalize(componentId);
            c = ArtFramework.component(canon);
        }
        if (c == null) {
            return UiOpResult.unavailable("unknown component: " + componentId);
        }
        return c.action(action, args != null ? args : new Object[0]);
    }

    public UiComponent component(String componentId) {
        return NativeComponents.get(componentId);
    }

    void resetForTests() {
        for (SignalSubscription subscription : signalSubscriptions.values()) {
            subscription.disconnect();
        }
        signalSubscriptions.clear();
        signalHandlers.clear();
    }

    void onTreeMounted(String windowId) {
        for (Map.Entry<String, SignalHandler> e : signalHandlers.entrySet()) {
            if (e.getKey().startsWith(windowId + "\0")) {
                String controlId = controlForKey(e.getKey());
                String signal = signalForKey(e.getKey());
                if (signal != null) {
                    PresentationContext context = PresentationRuntime.context(windowId);
                    EntityId node = PresentationRuntime.find(context, controlId);
                    if (node != null) {
                        SignalSubscription previous = signalSubscriptions.remove(e.getKey());
                        if (previous != null) previous.disconnect();
                        signalSubscriptions.put(e.getKey(),
                                PresentationRuntime.connect(context, node, signal, e.getValue()));
                    }
                }
            }
        }
    }

    void onTreeClosed(String windowId) {
        if (windowId == null) return;
        String prefix = windowId + "\0";
        for (String key : new java.util.ArrayList<String>(signalHandlers.keySet())) {
            if (!key.startsWith(prefix)) continue;
            signalHandlers.remove(key);
            SignalSubscription subscription = signalSubscriptions.remove(key);
            if (subscription != null) subscription.disconnect();
        }
    }

    private static void syncNodeProperty(String windowId, String controlId, String property, Object value) {
        PresentationContext context = PresentationRuntime.context(windowId);
        EntityId node = PresentationRuntime.find(context, controlId);
        if (node != null) PresentationRuntime.setProperty(context, node, property, value);
    }

    private static boolean allowSignal(String windowId, String controlId, String signal, Object... args) {
        PresentationContext context = PresentationRuntime.context(windowId);
        EntityId node = PresentationRuntime.find(context, controlId);
        if (node == null) {
            return false;
        }
        SignalDispatchResult result = PresentationRuntime.dispatch(
                context, node, signal, args != null ? args : new Object[0]);
        return !result.isRejected();
    }

    private void connectSugar(String windowId, String controlId, String signal, SignalHandler handler) {
        String k = key(windowId, controlId) + "\0" + signal;
        SignalSubscription oldSubscription = signalSubscriptions.remove(k);
        if (oldSubscription != null) oldSubscription.disconnect();
        signalHandlers.put(k, handler);
        PresentationContext context = PresentationRuntime.context(windowId);
        EntityId node = PresentationRuntime.find(context, controlId);
        if (node != null) {
            signalSubscriptions.put(k, PresentationRuntime.connect(context, node, signal, handler));
        }
    }

    private void disconnectSugar(String windowId, String controlId, String signal) {
        String k = key(windowId, controlId) + "\0" + signal;
        signalHandlers.remove(k);
        SignalSubscription subscription = signalSubscriptions.remove(k);
        if (subscription != null) subscription.disconnect();
    }

    private static String signalForKey(String key) {
        int split = key.lastIndexOf('\0');
        return split < 0 ? null : key.substring(split + 1);
    }

    private static String controlForKey(String key) {
        int first = key.indexOf('\0');
        int last = key.lastIndexOf('\0');
        return first < 0 || last <= first ? null : key.substring(first + 1, last);
    }

    private static String sessionPath(String windowId, String controlId) {
        EntityId root = PresentationRuntime.root(PresentationRuntime.context(windowId));
        artframework.presentation.NodeIdentityComponent identity = PresentationRuntime.identity(
                PresentationRuntime.context(windowId), root);
        return identity != null ? identity.name + "/" + controlId : controlId;
    }

    private static boolean controlExists(String windowId, String controlId, String type) {
        return control(windowId, controlId, type) != null;
    }

    private static boolean pressableControlExists(String windowId, String controlId) {
        PresentationContext context = PresentationRuntime.context(windowId);
        EntityId node = PresentationRuntime.find(context, controlId);
        artframework.presentation.NodeIdentityComponent identity = PresentationRuntime.identity(context, node);
        return identity != null && UiTypes.BUTTON.equals(identity.type);
    }

    private static SelectTemplate selectTemplate(SelectKind kind) {
        if (kind == SelectKind.GRID) {
            return NativeTemplateRuntime.selectGrid();
        }
        if (kind == SelectKind.HAND) {
            return NativeTemplateRuntime.selectHand();
        }
        return null;
    }

    private static String selectName(SelectKind kind) {
        return kind == SelectKind.GRID ? NativeTemplateIds.SELECT_GRID : NativeTemplateIds.SELECT_HAND;
    }

    private static String selectSurface(SelectKind kind) {
        return kind == SelectKind.GRID
                ? artframework.context.SurfaceIds.SELECT_GRID
                : kind == SelectKind.HAND
                        ? artframework.context.SurfaceIds.SELECT_HAND
                        : "";
    }

    /** Record the legacy native path while preserving its synchronous compatibility result. */
    private static UiOpResult nativeResult(String surfaceId, String intentName, UiOpResult result,
            boolean allowed, String reason) {
        String sid = surfaceId != null ? surfaceId : "";
        UiOpResult outcome = result != null ? result : UiOpResult.unavailable("no result");
        NativeInputRecords.intercept(sid, allowed && outcome.status == UiOpResult.Status.OK,
                allowed && outcome.status == UiOpResult.Status.OK, reason);
        NativeIntentLifecycleComponent.State state = !allowed
                ? NativeIntentLifecycleComponent.State.REJECTED
                : outcome.status == UiOpResult.Status.OK
                        ? (outcome.message.startsWith("queued:")
                                ? NativeIntentLifecycleComponent.State.QUEUED
                                : NativeIntentLifecycleComponent.State.EXECUTED)
                        : NativeIntentLifecycleComponent.State.REJECTED;
        NativeInputRecords.intent(sid, intentName, state, outcome.message);
        return outcome;
    }

    private static boolean rejected(String componentId, String signal, Object payload) {
        SignalDispatchResult result = artframework.core.SignalGroups.nativeGroup().dispatch(
                new UiSignal(SignalPaths.component(componentId, signal), componentId, payload));
        return result.isRejected();
    }

    private static NativeOpsBackend backend() {
        NativeOpsBackend b = ArtFramework.nativeOpsBackend();
        return b != null ? b : NoOpNativeOps.INSTANCE;
    }

    private static String key(String windowId, String buttonId) {
        return windowId + "\0" + buttonId;
    }

}
