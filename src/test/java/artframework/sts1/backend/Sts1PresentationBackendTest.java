package artframework.sts1.backend;

import org.junit.Test;

/** Host snapshots stay soft outside a live dungeon. Live map hitbox geometry is D1-covered. */
public class Sts1PresentationBackendTest {

    @Test
    public void snapshotOutsideDungeonDoesNotThrow() {
        Sts1PresentationBackend.INSTANCE.publishFrame();
    }
}
