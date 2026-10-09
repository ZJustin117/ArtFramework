package artframework.ops;

import artframework.api.UiOpResult;
import artframework.component.MapNodeRef;
import artframework.c2.SelectKind;

/**
 * Engine-side gestures after interceptors ALLOW. Pure tests use {@link FakeNativeOps}.
 */
public interface NativeOpsBackend {

    UiOpResult selectCard(SelectKind kind, String cardId, int index);

    UiOpResult confirmSelect(SelectKind kind);

    UiOpResult clickMapNode(MapNodeRef node);

    /**
     * H0 hover-only injection: place the native pointer at raw screen {@code (x, y)} with no click
     * edges. Fail-open (UNAVAILABLE) when no engine adapter is installed.
     */
    UiOpResult setPointer(int x, int y);

    /** H0 hover-only injection targeted at a map node (never clicks). */
    UiOpResult hoverMapNode(MapNodeRef node);

    /** H0 hover-only injection targeted at a legend row by index (never clicks). */
    UiOpResult hoverLegend(int index);

    /** H0: clear the hover pointer. */
    UiOpResult clearPointer();

    UiOpResult chooseEventOption(int index, String label);

    UiOpResult pressEndTurn();

    UiOpResult playHandCard(String cardId, String target);
}
