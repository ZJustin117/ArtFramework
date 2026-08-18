package artframework.ops;

import artframework.api.UiOpResult;
import artframework.component.MapNodeRef;
import artframework.c2.SelectKind;

/**
 * Default backend when no STS gesture adapter is installed — reports unavailable.
 * Interceptor gates still run; engine side effects do not.
 */
public final class NoOpNativeOps implements NativeOpsBackend {

    public static final NoOpNativeOps INSTANCE = new NoOpNativeOps();

    private NoOpNativeOps() {}

    @Override
    public UiOpResult selectCard(SelectKind kind, String cardId, int index) {
        return UiOpResult.unavailable("native select not installed");
    }

    @Override
    public UiOpResult confirmSelect(SelectKind kind) {
        return UiOpResult.unavailable("native confirm not installed");
    }

    @Override
    public UiOpResult clickMapNode(MapNodeRef node) {
        return UiOpResult.unavailable("native map click not installed");
    }

    @Override
    public UiOpResult chooseEventOption(int index, String label) {
        return UiOpResult.unavailable("native event option not installed");
    }

    @Override
    public UiOpResult pressEndTurn() {
        return UiOpResult.unavailable("native end turn not installed");
    }

    @Override
    public UiOpResult playHandCard(String cardId, String target) {
        return UiOpResult.unavailable("native hand play not installed");
    }
}
