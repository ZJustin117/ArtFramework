package artframework.sts1.backend;

import org.junit.Test;

import artframework.context.PresentSurfaces;

/** Host snapshots stay soft outside a live dungeon. Live map hitbox geometry is D1-covered. */
public class Sts1PresentationBackendTest {

    @Test
    public void snapshotOutsideDungeonDoesNotThrow() {
        Sts1PresentationBackend.INSTANCE.publishFrame();
    }

    @Test
    public void presentSurfacesStartUnmounted() {
        PresentSurfaces.resetForTests();
        org.junit.Assert.assertFalse(PresentSurfaces.anyMounted());
    }

    @Test
    public void readTargetingSessionFailsOpenOutsideDungeon() {
        artframework.context.TargetingSessionComponent session =
                Sts1PresentationBackend.readTargetingSession();
        org.junit.Assert.assertNotNull(session);
        org.junit.Assert.assertFalse(session.active);
    }
}
