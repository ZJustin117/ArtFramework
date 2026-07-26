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

    public UiOpResult clickButton(String windowId, String buttonId) {
        if (windowId == null || buttonId == null) {
            return UiOpResult.unavailable("windowId and buttonId required");
        }
        WindowHandle h = SpireUI.find(windowId);
        if (h == null || !h.isOpen() || h.windowClass() != WindowClass.SYNTHETIC) {
            return UiOpResult.notBound("synthetic window not open: " + windowId);
        }
        LayoutNode root = SpireUI.layoutRoot(windowId);
        if (root == null || !hasButton(root, buttonId)) {
            return UiOpResult.unavailable("button not in layout: " + buttonId);
        }
        Runnable run = buttonHandlers.get(key(windowId, buttonId));
        if (run == null) {
            return UiOpResult.unavailable("no handler for " + windowId + "/" + buttonId);
        }
        run.run();
        return UiOpResult.ok();
    }

    void resetForTests() {
        buttonHandlers.clear();
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
