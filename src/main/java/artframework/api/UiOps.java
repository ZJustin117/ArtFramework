package artframework.api;

import artframework.c1.layout.LayoutNode;
import artframework.c2.EventOptionRef;
import artframework.c2.MapNodeRef;
import artframework.c2.NativeTemplateRuntime;
import artframework.c2.NativeTemplateIds;
import artframework.c2.SelectCardRef;
import artframework.c2.SelectKind;
import artframework.c2.SelectTemplate;
import artframework.component.SliderHandler;
import artframework.component.UiTypes;
import artframework.component.WidgetSession;
import artframework.component.WidgetSessions;
import artframework.c2.NativeComponents;
import artframework.core.SignalNames;
import artframework.core.UiComponent;
import artframework.core.UiTree;
import artframework.core.UiTrees;
import artframework.core.SignalHandler;
import artframework.core.SignalBuses;
import artframework.core.SignalDispatchResult;
import artframework.core.UiSignal;
import artframework.ops.NativeOpsBackend;
import artframework.ops.NoOpNativeOps;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Imperative UI commands over C1 buttons and C2 native templates.
 * Interceptors run before {@link NativeOpsBackend} gestures.
 */
public final class UiOps {

    private final Map<String, SignalHandler> signalHandlers =
            new LinkedHashMap<String, SignalHandler>();
    private final Map<String, UiOpResult> lastResults = new LinkedHashMap<String, UiOpResult>();

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
        if (kind == null) {
            return UiOpResult.unavailable("kind required");
        }
        if (selectTemplate(kind) == null || !selectTemplate(kind).isActive()) {
            return UiOpResult.notBound("select " + kind + " not bound");
        }
        if (rejected(selectName(kind), SignalNames.CARD_SELECTED, new SelectCardRef(cardId, index))) {
            return UiOpResult.blocked("select card blocked");
        }
        return backend().selectCard(kind, cardId != null ? cardId : "", index);
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
        if (kind == null) {
            return UiOpResult.unavailable("kind required");
        }
        SelectTemplate t = selectTemplate(kind);
        if (t == null || !t.isActive()) {
            return UiOpResult.notBound("select " + kind + " not bound");
        }
        if (rejected(selectName(kind), SignalNames.CONFIRMED, kind)) {
            return UiOpResult.blocked("confirm blocked");
        }
        return backend().confirmSelect(kind);
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
        if (node == null) {
            return UiOpResult.unavailable("node required");
        }
        if (!NativeTemplateRuntime.isMapBound()) {
            return UiOpResult.notBound("sts1.map not bound");
        }
        if (rejected(NativeTemplateIds.MAP, SignalNames.NODE_CLICKED, node)) {
            return UiOpResult.blocked("map node blocked");
        }
        return backend().clickMapNode(node);
    }

    public UiOpResult chooseEventOption(int index, String label) {
        return invoke(NativeTemplateIds.EVENT, "choose_option", Integer.valueOf(index), label);
    }

    /** Internal C2 dispatch called exclusively by the native component action path. */
    public UiOpResult dispatchEventOption(int index, String label) {
        if (!NativeTemplateRuntime.isEventBound()) {
            return UiOpResult.notBound("sts1.event not bound");
        }
        if (rejected(NativeTemplateIds.EVENT, SignalNames.OPTION_CHOSEN,
                new EventOptionRef(index, label))) {
            return UiOpResult.blocked("event option blocked");
        }
        return backend().chooseEventOption(index, label != null ? label : "");
    }

    public UiOpResult pressEndTurn() {
        return invoke(NativeTemplateIds.END_TURN, "press");
    }

    /** Internal C2 dispatch called exclusively by the native component action path. */
    public UiOpResult dispatchEndTurn() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return UiOpResult.notBound("sts1.endturn not bound");
        }
        if (!NativeTemplateRuntime.endTurn().isButtonEnabled()
                || rejected(NativeTemplateIds.END_TURN, SignalNames.PRESSED, null)) {
            return UiOpResult.blocked("end turn blocked");
        }
        return backend().pressEndTurn();
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
        if (!controlExists(windowId, buttonId, UiTypes.BUTTON)) {
            return UiOpResult.unavailable("button not in layout: " + buttonId);
        }
        if (!allowSignal(windowId, buttonId, SignalNames.PRESSED)) {
            return UiOpResult.blocked("button blocked: " + windowId + "/" + buttonId);
        }
        emitSignal(windowId, buttonId, SignalNames.PRESSED);
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
        WidgetSession session = WidgetSessions.get(windowId);
        if (session == null || !session.hasSlider(sliderId)) {
            return UiOpResult.unavailable("slider not in layout: " + sliderId);
        }
        float previous = session.getSlider(sliderId);
        float clamped = session.setSlider(sliderId, value);
        session.setSlider(sliderId, previous);
        if (!allowSignal(windowId, sliderId, SignalNames.VALUE_CHANGED, Float.valueOf(clamped))) {
            return UiOpResult.blocked("slider blocked: " + windowId + "/" + sliderId);
        }
        session.setSlider(sliderId, value);
        emitSignal(windowId, sliderId, SignalNames.VALUE_CHANGED, Float.valueOf(clamped));
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
        emitSignal(windowId, hitAreaId, SignalNames.PRESSED);
        UiTree tree = UiTrees.get(windowId);
        return tree != null && tree.signalHub().handlerCount(hitAreaId, SignalNames.PRESSED) > 0
                ? UiOpResult.ok()
                : UiOpResult.unavailable("no handler for " + windowId + "/" + hitAreaId);
    }

    /**
     * Unified click for button or hitarea (Bind).
     */
    public UiOpResult click(String windowId, String controlId) {
        if (windowId == null || controlId == null) {
            return UiOpResult.unavailable("windowId and controlId required");
        }
        WidgetSession session = WidgetSessions.get(windowId);
        if (session != null) {
            if (session.hasType(controlId, UiTypes.BUTTON)) {
                return clickButton(windowId, controlId);
            }
            if (session.hasType(controlId, UiTypes.HITAREA)) {
                return clickHitArea(windowId, controlId);
            }
            if (session.hasType(controlId, UiTypes.CHECKBOX)) {
                return toggleCheckbox(windowId, controlId);
            }
            return UiOpResult.unavailable("not clickable: " + controlId);
        }
        return clickButton(windowId, controlId);
    }

    public UiOpResult setText(String windowId, String fieldId, String text) {
        if (windowId == null || fieldId == null) {
            return UiOpResult.unavailable("windowId and fieldId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        WidgetSession session = WidgetSessions.get(windowId);
        if (session == null || !session.hasTextField(fieldId)) {
            return UiOpResult.unavailable("textfield not in layout: " + fieldId);
        }
        String t = text != null ? text : "";
        if (!allowSignal(windowId, fieldId, SignalNames.TEXT_CHANGED, t)) {
            return UiOpResult.blocked("textfield blocked: " + windowId + "/" + fieldId);
        }
        t = session.setText(fieldId, t);
        emitSignal(windowId, fieldId, SignalNames.TEXT_CHANGED, t);
        return UiOpResult.ok();
    }

    public UiOpResult submitText(String windowId, String fieldId) {
        if (windowId == null || fieldId == null) {
            return UiOpResult.unavailable("windowId and fieldId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        WidgetSession session = WidgetSessions.get(windowId);
        if (session == null || !session.hasTextField(fieldId)) {
            return UiOpResult.unavailable("textfield not in layout: " + fieldId);
        }
        String text = session.getText(fieldId);
        if (!allowSignal(windowId, fieldId, SignalNames.TEXT_SUBMITTED, text)) {
            return UiOpResult.blocked("textfield blocked: " + windowId + "/" + fieldId);
        }
        emitSignal(windowId, fieldId, SignalNames.TEXT_SUBMITTED, text);
        return UiOpResult.ok();
    }

    public UiOpResult setChecked(String windowId, String checkboxId, boolean checked) {
        if (windowId == null || checkboxId == null) {
            return UiOpResult.unavailable("windowId and checkboxId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        WidgetSession session = WidgetSessions.get(windowId);
        if (session == null || !session.hasCheckbox(checkboxId)) {
            return UiOpResult.unavailable("checkbox not in layout: " + checkboxId);
        }
        boolean v = checked;
        if (!allowSignal(windowId, checkboxId, SignalNames.TOGGLED, Boolean.valueOf(v))) {
            return UiOpResult.blocked("checkbox blocked: " + windowId + "/" + checkboxId);
        }
        v = session.setChecked(checkboxId, checked);
        emitSignal(windowId, checkboxId, SignalNames.TOGGLED, Boolean.valueOf(v));
        return UiOpResult.ok();
    }

    public UiOpResult toggleCheckbox(String windowId, String checkboxId) {
        if (windowId == null || checkboxId == null) {
            return UiOpResult.unavailable("windowId and checkboxId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        WidgetSession session = WidgetSessions.get(windowId);
        if (session == null || !session.hasCheckbox(checkboxId)) {
            return UiOpResult.unavailable("checkbox not in layout: " + checkboxId);
        }
        boolean v = !session.getChecked(checkboxId);
        if (!allowSignal(windowId, checkboxId, SignalNames.TOGGLED, Boolean.valueOf(v))) {
            return UiOpResult.blocked("checkbox blocked: " + windowId + "/" + checkboxId);
        }
        v = session.setChecked(checkboxId, v);
        emitSignal(windowId, checkboxId, SignalNames.TOGGLED, Boolean.valueOf(v));
        return UiOpResult.ok();
    }

    public UiOpResult setProgress(String windowId, String progressId, float value) {
        if (windowId == null || progressId == null) {
            return UiOpResult.unavailable("windowId and progressId required");
        }
        if (!isOpenSynthetic(windowId)) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        WidgetSession session = WidgetSessions.get(windowId);
        if (session == null || !session.hasProgress(progressId)) {
            return UiOpResult.unavailable("progress not in layout: " + progressId);
        }
        float previous = session.getProgress(progressId);
        float clamped = session.setProgress(progressId, value);
        session.setProgress(progressId, previous);
        if (!allowSignal(windowId, progressId, SignalNames.VALUE_CHANGED, Float.valueOf(clamped))) {
            return UiOpResult.blocked("progress blocked: " + windowId + "/" + progressId);
        }
        session.setProgress(progressId, value);
        emitSignal(windowId, progressId, SignalNames.VALUE_CHANGED, Float.valueOf(clamped));
        return UiOpResult.ok();
    }

    private static boolean isOpenSynthetic(String windowId) {
        WindowHandle h = ArtFramework.find(windowId);
        return h != null && h.isOpen() && h.windowClass() == WindowClass.SYNTHETIC;
    }

    /**
     * Route an action to a {@link UiComponent} (C2 native host or future synthetic).
     * Named C2 methods remain sugar that implement the real gate + gesture path.
     */
    public UiOpResult invoke(String componentId, String action, Object... args) {
        if (componentId == null || componentId.isEmpty()) {
            return remember(componentId, UiOpResult.unavailable("componentId required"));
        }
        if (action == null || action.isEmpty()) {
            return remember(componentId, UiOpResult.unavailable("action required"));
        }
        UiComponent c = ArtFramework.component(componentId);
        if (c == null) {
            String canon = artframework.context.SurfaceIds.canonicalize(componentId);
            c = ArtFramework.component(canon);
        }
        if (c == null) {
            return remember(componentId, UiOpResult.unavailable("unknown component: " + componentId));
        }
        return remember(c.id(), c.action(action, args != null ? args : new Object[0]));
    }

    public UiOpResult lastResult(String componentId) {
        return lastResults.get(componentId);
    }

    public UiComponent component(String componentId) {
        return NativeComponents.get(componentId);
    }

    void resetForTests() {
        signalHandlers.clear();
        lastResults.clear();
    }

    private UiOpResult remember(String componentId, UiOpResult result) {
        if (componentId != null && result != null) {
            lastResults.put(componentId, result);
        }
        return result;
    }

    void onTreeMounted(String windowId) {
        for (Map.Entry<String, SignalHandler> e : signalHandlers.entrySet()) {
            if (e.getKey().startsWith(windowId + "\0")) {
                String controlId = e.getKey().substring(windowId.length() + 1);
                String signal = signalForKey(e.getKey());
                if (signal != null) {
                    UiTree tree = UiTrees.get(windowId);
                    if (tree != null) {
                        tree.connect(controlId, signal, e.getValue());
                    }
                }
            }
        }
    }

    private static void emitSignal(String windowId, String controlId, String signal, Object... args) {
        // Delivery already occurred during allowSignal so state mutation can be cancelled first.
    }

    private static boolean allowSignal(String windowId, String controlId, String signal, Object... args) {
        artframework.core.SignalDispatchResult result = artframework.core.SignalBuses.get().emit(
                new artframework.core.UiSignal(
                        artframework.core.SignalHub.name(controlId, signal), controlId,
                        args != null ? args : new Object[0]));
        return !result.isRejected();
    }

    private void connectSugar(String windowId, String controlId, String signal, SignalHandler handler) {
        String k = key(windowId, controlId) + "\0" + signal;
        SignalHandler old = signalHandlers.put(k, handler);
        UiTree tree = UiTrees.get(windowId);
        if (tree != null) {
            if (old != null) {
                tree.disconnect(controlId, signal, old);
            }
            tree.connect(controlId, signal, handler);
        }
    }

    private void disconnectSugar(String windowId, String controlId, String signal) {
        String k = key(windowId, controlId) + "\0" + signal;
        SignalHandler old = signalHandlers.remove(k);
        UiTree tree = UiTrees.get(windowId);
        if (tree != null && old != null) {
            tree.disconnect(controlId, signal, old);
        }
    }

    private static String signalForKey(String key) {
        int split = key.lastIndexOf('\0');
        return split < 0 ? null : key.substring(split + 1);
    }

    private static boolean controlExists(String windowId, String controlId, String type) {
        WidgetSession session = WidgetSessions.get(windowId);
        if (session != null) {
            return session.hasType(controlId, type);
        }
        if (UiTypes.BUTTON.equals(type)) {
            LayoutNode root = ArtFramework.layoutRoot(windowId);
            return root != null && hasButton(root, controlId);
        }
        return false;
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

    private static boolean rejected(String componentId, String signal, Object payload) {
        SignalDispatchResult result = SignalBuses.get().emit(
                new UiSignal("ui/" + componentId + "/" + signal, componentId, payload));
        return result.isRejected();
    }

    private static NativeOpsBackend backend() {
        NativeOpsBackend b = ArtFramework.nativeOpsBackend();
        return b != null ? b : NoOpNativeOps.INSTANCE;
    }

    private static String key(String windowId, String buttonId) {
        return windowId + "\0" + buttonId;
    }

    private static boolean hasButton(LayoutNode node, String buttonId) {
        if (node == null) {
            return false;
        }
        if (node.type == LayoutNode.Type.BUTTON && buttonId.equals(node.id)) {
            return true;
        }
        for (LayoutNode c : node.children) {
            if (hasButton(c, buttonId)) {
                return true;
            }
        }
        return false;
    }
}
