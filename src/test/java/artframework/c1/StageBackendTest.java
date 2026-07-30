package artframework.c1;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.api.WindowHandle;
import artframework.c1.layout.LayoutNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StageBackendTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void openAttachesWhenBackendReady() {
        FakeStageBackend fake = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(fake);
        assertTrue(SyntheticRuntime.isStageReady());

        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = ArtFramework.open("demo");
        assertTrue(h.isOpen());
        assertTrue(fake.isAttached("demo"));
        assertEquals(1, fake.attachedCount());
        // Always composition attach (EffectTargetActors); not legacy LayoutNode path.
        assertNull(fake.getAttached("demo"));
        assertNotNull(fake.getAttachedComposition("demo"));
        assertEquals(
                "ArtFramework Demo",
                fake.getAttachedComposition("demo").propString("title", ""));
    }

    @Test
    public void closeDetachesBackend() {
        FakeStageBackend fake = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(fake);
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");
        ArtFramework.close("demo");
        assertFalse(fake.isAttached("demo"));
        assertEquals(0, fake.attachedCount());
    }

    @Test
    public void notReadySkipsAttachButLogicOpenWorks() {
        FakeStageBackend fake = new FakeStageBackend();
        fake.setReady(false);
        SyntheticRuntime.installStageBackend(fake);
        assertFalse(SyntheticRuntime.isStageReady());

        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = ArtFramework.open("demo");
        assertTrue(h.isOpen());
        assertTrue(WindowManager.contains("demo"));
        assertFalse(fake.isAttached("demo"));
    }

    @Test
    public void doubleOpenDetachesThenReattaches() {
        FakeStageBackend fake = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(fake);
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");
        ArtFramework.open("demo");
        assertEquals(1, fake.attachedCount());
        assertTrue(fake.isAttached("demo"));
    }

    @Test
    public void compositionOpenAttachesCompositionTree() {
        FakeStageBackend fake = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(fake);
        ArtFramework.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        ArtFramework.open("comp");
        assertTrue(fake.isAttached("comp"));
        assertNotNull(fake.getAttachedComposition("comp"));
        assertEquals("Composition Sample", fake.getAttachedComposition("comp").propString("title", ""));
        assertNull(fake.getAttached("comp"));
    }
}
