package spireui.c1;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.api.WindowHandle;
import spireui.c1.layout.LayoutNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StageBackendTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void openAttachesWhenBackendReady() {
        FakeStageBackend fake = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(fake);
        assertTrue(SyntheticRuntime.isStageReady());

        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = SpireUI.open("demo");
        assertTrue(h.isOpen());
        assertTrue(fake.isAttached("demo"));
        assertEquals(1, fake.attachedCount());
        LayoutNode root = fake.getAttached("demo");
        assertNotNull(root);
        assertEquals("SpireUI Demo", root.title);
    }

    @Test
    public void closeDetachesBackend() {
        FakeStageBackend fake = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(fake);
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.open("demo");
        SpireUI.close("demo");
        assertFalse(fake.isAttached("demo"));
        assertEquals(0, fake.attachedCount());
    }

    @Test
    public void notReadySkipsAttachButLogicOpenWorks() {
        FakeStageBackend fake = new FakeStageBackend();
        fake.setReady(false);
        SyntheticRuntime.installStageBackend(fake);
        assertFalse(SyntheticRuntime.isStageReady());

        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = SpireUI.open("demo");
        assertTrue(h.isOpen());
        assertTrue(WindowManager.contains("demo"));
        assertFalse(fake.isAttached("demo"));
    }

    @Test
    public void doubleOpenDetachesThenReattaches() {
        FakeStageBackend fake = new FakeStageBackend();
        SyntheticRuntime.installStageBackend(fake);
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.open("demo");
        SpireUI.open("demo");
        assertEquals(1, fake.attachedCount());
        assertTrue(fake.isAttached("demo"));
    }
}
