package artframework.ops;

import artframework.api.UiOpResult;
import artframework.c2.MapNodeRef;
import artframework.c2.SelectKind;

/**
 * Engine-side gestures after interceptors ALLOW. Pure tests use {@link FakeNativeOps}.
 */
public interface NativeOpsBackend {

    UiOpResult selectCard(SelectKind kind, String cardId, int index);

    UiOpResult confirmSelect(SelectKind kind);

    UiOpResult clickMapNode(MapNodeRef node);

    UiOpResult chooseEventOption(int index, String label);

    UiOpResult pressEndTurn();

    UiOpResult playHandCard(String cardId, String target);
}
