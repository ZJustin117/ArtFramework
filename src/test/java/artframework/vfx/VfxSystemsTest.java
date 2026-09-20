package artframework.vfx;

import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VfxSystemsTest {
    @After public void cleanup() { PackSystems.resetForTests(); }

    @Test
    public void enableRemainsIdempotentAndWorksAfterPackReset() {
        VfxSystems.enable();
        VfxSystems.enable();
        assertEquals(1, PackSystems.systemsFor(PackSystemPhase.EFFECTS).size());
        assertEquals(1, PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION).size());
        PackSystems.resetForTests();
        VfxSystems.enable();
        assertEquals(1, PackSystems.systemsFor(PackSystemPhase.EFFECTS).size());
        assertEquals(1, PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION).size());
    }
}
