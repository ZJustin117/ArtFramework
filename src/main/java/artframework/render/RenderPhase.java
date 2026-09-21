package artframework.render;

/** Coarse ownership boundary for ART-owned render items. */
public enum RenderPhase {
    ART_BACKGROUND(100),
    NATIVE_RETAINED(200),
    C1_CONTENT(300),
    C2_CONTENT(400),
    ENTITY_CONTENT(500),
    ART_EFFECTS(600),
    VERIFY_GUIDES(1000);

    public final int rank;

    RenderPhase(int rank) {
        this.rank = rank;
    }
}
