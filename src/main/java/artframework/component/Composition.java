package artframework.component;

/**
 * Facade for parse → expand → layout (pure pipeline).
 */
public final class Composition {

    private Composition() {}

    public static UiNode parse(String json) {
        return UiNodeLoader.parse(json);
    }

    public static UiNode loadClasspath(String resource) {
        return UiNodeLoader.loadClasspath(resource);
    }

    public static UiNode expand(UiNode root) {
        return new TemplateExpander().expand(root);
    }

    public static UiNode expand(UiNode root, ComponentRegistry registry) {
        return new TemplateExpander(registry).expand(root);
    }

    public static LayoutResult layout(UiNode root) {
        return LayoutEngine.layout(root);
    }

    /**
     * Parse, expand with global registry, then layout.
     */
    public static LayoutResult compile(String json) {
        return layout(expand(parse(json)));
    }

    public static LayoutResult compile(String json, ComponentRegistry registry) {
        return layout(expand(parse(json), registry));
    }

    public static NodeIndex index(UiNode root) {
        return NodeIndex.of(root);
    }
}
