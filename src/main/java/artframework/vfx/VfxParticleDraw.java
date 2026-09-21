package artframework.vfx;

import artframework.render.RenderOrder;
import artframework.render.RenderPhase;

/** Immutable host-neutral particle draw record. */
public final class VfxParticleDraw {
    public final String sceneId, nodeId, textureReference, blendMode;
    public final int particleIndex, definitionOrder, flipbookFrame, flipbookColumns, flipbookRows;
    public final float x, y, rotationDegrees, scaleX, scaleY, r, g, b, alpha, zIndex;
    public final boolean flipX, flipY;
    public final String stableKey;
    public final RenderOrder renderOrder;

    public VfxParticleDraw(String sceneId, String nodeId, int particleIndex, int definitionOrder, String textureReference,
            float x, float y, float rotationDegrees, float scaleX, float scaleY,
            float r, float g, float b, float alpha, String blendMode, float zIndex,
            int flipbookFrame, int flipbookColumns, int flipbookRows, boolean flipX, boolean flipY) {
        this.sceneId = sceneId; this.nodeId = nodeId; this.particleIndex = particleIndex; this.definitionOrder = definitionOrder;
        this.textureReference = textureReference; this.x = x; this.y = y;
        this.rotationDegrees = rotationDegrees; this.scaleX = scaleX; this.scaleY = scaleY;
        this.r = r; this.g = g; this.b = b; this.alpha = alpha;
        this.blendMode = blendMode; this.zIndex = zIndex; this.flipbookFrame = flipbookFrame;
        this.flipbookColumns = flipbookColumns; this.flipbookRows = flipbookRows;
        this.flipX = flipX; this.flipY = flipY;
        this.stableKey = sceneId + "/" + nodeId + "/" + definitionOrder + "/" + particleIndex;
        this.renderOrder = new RenderOrder(RenderPhase.ART_EFFECTS, zIndex, stableKey);
    }
}
