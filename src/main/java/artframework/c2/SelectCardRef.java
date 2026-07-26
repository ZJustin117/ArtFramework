package artframework.c2;

/** Card identity for select-screen intercepts (no AbstractCard dependency). */
public final class SelectCardRef {

    public final String cardId;
    public final int index;

    public SelectCardRef(String cardId, int index) {
        this.cardId = cardId != null ? cardId : "";
        this.index = index;
    }

    @Override
    public String toString() {
        return "SelectCardRef{" + cardId + "@" + index + "}";
    }
}
