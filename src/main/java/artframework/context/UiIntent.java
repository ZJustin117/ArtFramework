package artframework.context;

/**
 * Finite request from ART return channel to Primary Backend. Not a free callback.
 */
public final class UiIntent {

    public final String name;
    public final String surfaceId;
    public final Object[] args;

    public UiIntent(String name, String surfaceId, Object... args) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name required");
        }
        this.name = name;
        this.surfaceId = surfaceId != null ? surfaceId : "";
        this.args = args != null ? args : new Object[0];
    }

    public static UiIntent of(String name, String surfaceId, Object... args) {
        return new UiIntent(name, surfaceId, args);
    }
}
