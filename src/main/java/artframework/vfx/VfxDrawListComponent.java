package artframework.vfx;

/** Root-owned immutable render projection component. */
public final class VfxDrawListComponent {
    public final VfxDrawList value;

    public VfxDrawListComponent(VfxDrawList value) {
        this.value = value == null ? VfxDrawList.empty() : value;
    }
}
