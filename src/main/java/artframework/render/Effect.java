package artframework.render;

/**
 * Visual effect. Pure implementations only validate; GL draw is optional via {@link #draw}.
 */
public interface Effect {

    String id();

    /** Optional shader program id from {@link ShaderRegistry}; empty if none. */
    String shaderId();

    /**
     * When true, {@link RenderHost} captures the screen before drawing this effect.
     */
    boolean requiresCapture();

    /**
     * Validate binding params. Throws {@link IllegalArgumentException} if invalid.
     */
    void validate(EffectBinding binding);

    /**
     * Draw using {@link RenderContext}. Must tolerate null batch / missing capture.
     */
    void draw(RenderTarget target, EffectBinding binding, RenderContext ctx);
}
