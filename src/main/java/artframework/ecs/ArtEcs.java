package artframework.ecs;

/** Process-wide ART ECS world used by all registered presentation scopes. */
public final class ArtEcs {
    private static final PresentationWorld WORLD = new PresentationWorld("art");

    private ArtEcs() {}

    public static PresentationWorld world() {
        return WORLD;
    }
}
