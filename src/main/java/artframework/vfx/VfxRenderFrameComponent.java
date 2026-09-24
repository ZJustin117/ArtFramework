package artframework.vfx;

/** Shared ECS publication point for the current immutable ART VFX render frame. */
public final class VfxRenderFrameComponent {
    public final VfxRenderFrame value;

    public VfxRenderFrameComponent(VfxRenderFrame value) {
        this.value = value == null ? VfxRenderFrame.empty() : value;
    }
}
