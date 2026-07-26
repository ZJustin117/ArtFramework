package spireui.ops;

import spireui.api.UiOpResult;
import spireui.c2.MapNodeRef;
import spireui.c2.SelectKind;

/** In-memory backend for pure JUnit — records calls, always OK. */
public final class FakeNativeOps implements NativeOpsBackend {

    public int selectCardCount;
    public int confirmCount;
    public int mapClickCount;
    public int eventOptionCount;
    public int endTurnCount;
    public int playHandCount;

    public SelectKind lastSelectKind;
    public String lastSelectCardId;
    public int lastSelectIndex;
    public SelectKind lastConfirmKind;
    public int lastMapRow;
    public int lastMapCol;
    public int lastEventIndex;
    public String lastEventLabel;
    public String lastPlayCardId;
    public String lastPlayTarget;

    @Override
    public UiOpResult selectCard(SelectKind kind, String cardId, int index) {
        selectCardCount++;
        lastSelectKind = kind;
        lastSelectCardId = cardId;
        lastSelectIndex = index;
        return UiOpResult.ok();
    }

    @Override
    public UiOpResult confirmSelect(SelectKind kind) {
        confirmCount++;
        lastConfirmKind = kind;
        return UiOpResult.ok();
    }

    @Override
    public UiOpResult clickMapNode(MapNodeRef node) {
        mapClickCount++;
        if (node != null) {
            lastMapRow = node.row;
            lastMapCol = node.col;
        }
        return UiOpResult.ok();
    }

    @Override
    public UiOpResult chooseEventOption(int index, String label) {
        eventOptionCount++;
        lastEventIndex = index;
        lastEventLabel = label;
        return UiOpResult.ok();
    }

    @Override
    public UiOpResult pressEndTurn() {
        endTurnCount++;
        return UiOpResult.ok();
    }

    @Override
    public UiOpResult playHandCard(String cardId, String target) {
        playHandCount++;
        lastPlayCardId = cardId;
        lastPlayTarget = target;
        return UiOpResult.ok();
    }
}
