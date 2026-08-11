package artframework.presentation;

/** Stable, namespaced identity for one runtime presentation node. */
public final class PresentationKey {
    public final String scope;
    public final String localId;

    public PresentationKey(String scope, String localId) {
        if (scope == null || scope.trim().isEmpty()) {
            throw new IllegalArgumentException("scope required");
        }
        if (localId == null || localId.trim().isEmpty()) {
            throw new IllegalArgumentException("localId required");
        }
        this.scope = scope.trim();
        this.localId = localId.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PresentationKey)) return false;
        PresentationKey that = (PresentationKey) other;
        return scope.equals(that.scope) && localId.equals(that.localId);
    }

    @Override
    public int hashCode() {
        return 31 * scope.hashCode() + localId.hashCode();
    }

    @Override
    public String toString() {
        return scope + ":" + localId;
    }
}
