package artframework.vfx;

public final class VfxFlipbookDefinition {
    public final int hFrames, vFrames;
    public final boolean loop;
    public final float animationSpeedMin, animationSpeedMax;

    public VfxFlipbookDefinition(int hFrames, int vFrames, boolean loop,
                                 float animationSpeedMin, float animationSpeedMax) {
        this.hFrames = hFrames; this.vFrames = vFrames; this.loop = loop;
        this.animationSpeedMin = animationSpeedMin; this.animationSpeedMax = animationSpeedMax;
    }
}
