package spireui.api;

import spireui.c1.layout.LayoutNode;
import spireui.c2.EventOptionRef;
import spireui.c2.GateResult;
import spireui.c2.MapNodeInterceptor;
import spireui.c2.MapNodeRef;
import spireui.c2.NativeTemplateRuntime;
import spireui.c2.SelectCardRef;
import spireui.c2.SelectKind;
import spireui.c2.SelectTemplate;
import spireui.component.SliderHandler;
import spireui.component.UiTypes;
import spireui.component.WidgetSession;
import spireui.component.WidgetSessions;
import spireui.c2.NativeComponents;
import spireui.core.SignalNames;
import spireui.core.UiComponent;
import spireui.core.UiTree;
import spireui.core.UiTrees;
import spireui.ops.NativeOpsBackend;
import spireui.ops.NoOpNativeOps;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Imperative UI commands over C1 buttons and C2 native templates.
 * Interceptors run before {@link NativeOpsBackend} gestures.
 */
public final class UiOps {

    private final Map<String, Runnable> buttonHandlers = new LinkedHashMap<String, Runnable>();
    private final Map<String, SliderHandler> sliderHandlers = new LinkedHashMap<String, SliderHandler>();
    private final Map<String, Runnable> hitAreaHandlers = new LinkedHashMap<String, Runnable>();

    UiOps() {}

    public UiOpResult selectCard(SelectKind kind, String cardId, int index) {
        if (kind == null) {
            return UiOpResult.unavailable("kind required");
        }
        SelectTemplate t = selectTemplate(kind);
        if (t == null || !t.isActive()) {
            return UiOpResult.notBound("select " + kind + " not bound");
        }
        GateResult gate = t.dispatchCard(new SelectCardRef(cardId, index));
        if (gate == GateResult.BLOCK) {
            return UiOpResult.blocked("select card blocked");
        }
        return backend().selectCard(kind, cardId != null ? cardId : "", index);
    }

    public UiOpResult confirmSelect(SelectKind kind) {
        if (kind == null) {
            return UiOpResult.unavailable("kind required");
        }
        SelectTemplate t = selectTemplate(kind);
        if (t == null || !t.isActive()) {
            return UiOpResult.notBound("select " + kind + " not bound");
        }
        GateResult gate = t.dispatchConfirm();
        if (gate == GateResult.BLOCK) {
            return UiOpResult.blocked("confirm blocked");
        }
        return backend().confirmSelect(kind);
    }

    public UiOpResult clickMapNode(MapNodeRef node) {
        if (node == null) {
            return UiOpResult.unavailable("node required");
        }
        if (!NativeTemplateRuntime.isMapBound()) {
            return UiOpResult.notBound("sts.map not bound");
        }
        MapNodeInterceptor.Result r = NativeTemplateRuntime.map().dispatchNodeClick(node);
        if (r == MapNodeInterceptor.Result.BLOCK) {
            return UiOpResult.blocked("map node blocked");
        }
        return backend().clickMapNode(node);
    }

    public UiOpResult chooseEventOption(int index, String label) {
        if (!NativeTemplateRuntime.isEventBound()) {
            return UiOpResult.notBound("sts.event not bound");
        }
        GateResult gate =
                NativeTemplateRuntime.event().dispatchOption(new EventOptionRef(index, label));
        if (gate == GateResult.BLOCK) {
            return UiOpResult.blocked("event option blocked");
        }
        return backend().chooseEventOption(index, label != null ? label : "");
    }

    public UiOpResult pressEndTurn() {
        if (!NativeTemplateRuntime.isEndTurnBound()) {
            return UiOpResult.notBound("sts.endturn not bound");
        }
        GateResult gate = NativeTemplateRuntime.endTurn().dispatchPress();
        if (gate == GateResult.BLOCK) {
            return UiOpResult.blocked("end turn blocked");
        }
        return backend().pressEndTurn();
    }

    /**
     * Combat hand play <strong>gesture</strong> only — no queue/protocol.
     * Does not require a template bind (optional consumer intercept later).
     */
    public UiOpResult playHandCard(String cardId, String target) {
        if (cardId == null || cardId.isEmpty()) {
            return UiOpResult.unavailable("cardId required");
        }
        return backend().playHandCard(cardId, target != null ? target : "");
    }

    /** Register a pure handler for {@link #clickButton} (C1). */
    public void onButton(String windowId, String buttonId, Runnable handler) {
        if (windowId == null || buttonId == null || handler == null) {
            throw new IllegalArgumentException("windowId, buttonId, handler required");
        }
        buttonHandlers.put(key(windowId, buttonId), handler);
    }

    public void removeButtonHandler(String windowId, String buttonId) {
        if (windowId == null || buttonId == null) {
            return;
        }
        buttonHandlers.remove(key(windowId, buttonId));
    }

    public void onSlider(String windowId, String sliderId, SliderHandler handler) {
        if (windowId == null || sliderId == null || handler == null) {
            throw new IllegalArgumentException("windowId, sliderId, handler required");
        }
        sliderHandlers.put(key(windowId, sliderId), handler);
    }

    public void removeSliderHandler(String windowId, String sliderId) {
        if (windowId == null || sliderId == null) {
            return;
        }
        sliderHandlers.remove(key(windowId, sliderId));
    }

    public void onHitArea(String windowId, String hitAreaId, Runnable handler) {
        if (windowId == null || hitAreaId == null || handler == null) {
            throw new IllegalArgumentException("windowId, hitAreaId, handler required");
        }
        hitAreaHandlers.put(key(windowId, hitAreaId), handler);
    }

    public void removeHitAreaHandler(String windowId, String hitAreaId) {
        if (windowId == null || hitAreaId == null) {
            return;
        }
        hitAreaHandlers.remove(key(windowId, hitAreaId));
    }

    public UiOpResult clickButton(String windowId, String buttonId) {
        if (windowId == null || buttonId == null) {
            return UiOpResult.unavailable("windowId and buttonId required");
        }
        WindowHandle h = SpireUI.find(windowId);
        if (h == null || !h.isOpen() || h.windowClass() != WindowClass.SYNTHETIC) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        if (!controlExists(windowId, buttonId, UiTypes.BUTTON)) {
            return UiOpResult.unavailable("button not in layout: " + buttonId);
        }
        Runnable run = buttonHandlers.get(key(windowId, buttonId));
        emitSignal(windowId, buttonId, SignalNames.PRESSED);
        if (run == null) {
            UiTree tree = UiTrees.get(windowId);
            if (tree == null || tree.signalHub().handlerCount(buttonId, SignalNames.PRESSED) == 0) {
                return UiOpResult.unavailable("no handler for " + windowId + "/" + buttonId);
            }
            return UiOpResult.ok();
        }
        run.run();
        return UiOpResult.ok();
    }

    /**
     * Set slider value on an open composition session (clamped). Invokes {@link #onSlider} if set.
     */
    public UiOpResult setSlider(String windowId, String sliderId, float value) {
        if (windowId == null || sliderId == null) {
            return UiOpResult.unavailable("windowId and sliderId required");
        }
        WindowHandle h = SpireUI.find(windowId);
        if (h == null || !h.isOpen() || h.windowClass() != WindowClass.SYNTHETIC) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        WidgetSession session = WidgetSessions.get(windowId);
        if (session == null || !session.hasSlider(sliderId)) {
            return UiOpResult.unavailable("slider not in layout: " + sliderId);
        }
        float clamped = session.setSlider(sliderId, value);
        emitSignal(windowId, sliderId, SignalNames.VALUE_CHANGED, Float.valueOf(clamped));
        SliderHandler handler = sliderHandlers.get(key(windowId, sliderId));
        if (handler != null) {
            handler.onChange(clamped);
        }
        return UiOpResult.ok();
    }

    public UiOpResult clickHitArea(String windowId, String hitAreaId) {
        if (windowId == null || hitAreaId == null) {
            return UiOpResult.unavailable("windowId and hitAreaId required");
        }
        WindowHandle h = SpireUI.find(windowId);
        if (h == null || !h.isOpen() || h.windowClass() != WindowClass.SYNTHETIC) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        if (!controlExists(windowId, hitAreaId, UiTypes.HITAREA)) {
            return UiOpResult.unavailable("hitarea not in layout: " + hitAreaId);
        }
        Runnable run = hitAreaHandlers.get(key(windowId, hitAreaId));
        emitSignal(windowId, hitAreaId, SignalNames.PRESSED);
        if (run == null) {
            UiTree tree = UiTrees.get(windowId);
            if (tree == null || tree.signalHub().handlerCount(hitAreaId, SignalNames.PRESSED) == 0) {
                return UiOpResult.unavailable("no handler for " + windowId + "/" + hitAreaId);
            }
            return UiOpResult.ok();
        }
        run.run();
        return UiOpResult.ok();
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
        String t = session.setText(fieldId, text);
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
        emitSignal(windowId, fieldId, SignalNames.TEXT_SUBMITTED, session.getText(fieldId));
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
        boolean v = session.setChecked(checkboxId, checked);
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
        boolean v = session.toggleCheckbox(checkboxId);
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
        float clamped = session.setProgress(progressId, value);
        emitSignal(windowId, progressId, SignalNames.VALUE_CHANGED, Float.valueOf(clamped));
        return UiOpResult.ok();
    }

    private static boolean isOpenSynthetic(String windowId) {
        WindowHandle h = SpireUI.find(windowId);
        return h != null && h.isOpen() && h.windowClass() == WindowClass.SYNTHETIC;
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
        UiComponent c = NativeComponents.get(componentId);
        if (c == null) {
            return UiOpResult.unavailable("unknown component: " + componentId);
        }
        return c.action(action, args != null ? args : new Object[0]);
    }

    public UiComponent component(String componentId) {
        return NativeComponents.get(componentId);
    }

    void resetForTests() {
        buttonHandlers.clear();
        sliderHandlers.clear();
        hitAreaHandlers.clear();
    }

    private static void emitSignal(String windowId, String controlId, String signal, Object... args) {
        UiTree tree = UiTrees.get(windowId);
        if (tree != null) {
            tree.emit(controlId, signal, args);
        }
    }

    private static boolean controlExists(String windowId, String controlId, String type) {
        WidgetSession session = WidgetSessions.get(windowId);
        if (session != null) {
            return session.hasType(controlId, type);
        }
        if (UiTypes.BUTTON.equals(type)) {
            LayoutNode root = SpireUI.layoutRoot(windowId);
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

    private static NativeOpsBackend backend() {
        NativeOpsBackend b = SpireUI.nativeOpsBackend();
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
