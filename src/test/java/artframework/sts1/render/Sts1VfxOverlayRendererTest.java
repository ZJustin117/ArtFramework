package artframework.sts1.render;

import artframework.vfx.VfxDrawList;
import artframework.vfx.VfxParticleDraw;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Sts1VfxOverlayRendererTest {
    @Test public void overlaySubmissionSortsBySharedRenderOrder() {
        VfxParticleDraw high = draw("node-b", 2, 1, 5f);
        VfxParticleDraw lowKey = draw("node-a", 1, 0, 5f);
        VfxParticleDraw lowZ = draw("node-c", 3, 0, 1f);

        List<VfxParticleDraw> ordered = Sts1VfxOverlayRenderer.orderedDraws(
                new VfxDrawList(Arrays.asList(high, lowKey, lowZ)));

        assertEquals(lowZ, ordered.get(0));
        assertEquals(lowKey, ordered.get(1));
        assertEquals(high, ordered.get(2));
    }

    private static VfxParticleDraw draw(String node, int particle, int definition, float z) {
        return new VfxParticleDraw("scene", node, particle, definition, "texture",
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, "MIX", z,
                0, 1, 1, false, false);
    }
}
