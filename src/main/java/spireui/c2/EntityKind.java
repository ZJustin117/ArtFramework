package spireui.c2;

/**
 * Presenter entity categories. Draw uses STS helpers later; no combat authority.
 */
public enum EntityKind {
    PLAYER,
    CARD,
    RELIC,
    MONSTER;

    /**
     * Parse kind string (case-insensitive). Accepts enum names.
     *
     * @throws IllegalArgumentException if unknown
     */
    public static EntityKind parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("kind required");
        }
        try {
            return EntityKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown entity kind: " + raw);
        }
    }
}
