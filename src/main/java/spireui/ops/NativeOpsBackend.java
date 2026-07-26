package spireui.ops;

import spireui.api.UiOpResult;
import spireui.c2.MapNodeRef;
import spireui.c2.SelectKind;

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
