package spireui.api;

import org.junit.After;
import org.junit.Test;
import spireui.c2.NativeTemplateIds;
import spireui.core.HostBackend;
import spireui.core.UiTree;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MountHostTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void mountUnmountSynthetic() {
        SpireUI.register(
                new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = SpireUI.mount("demo");
        assertTrue(h.isOpen());
        assertNotNull(SpireUI.tree("demo"));
        SpireUI.unmount("demo");
        assertNull(SpireUI.find("demo"));
        assertNull(SpireUI.tree("demo"));
    }

    @Test
    public void mountNativeEqualsBind() {
        SpireUI.register(
                new WindowDef(
                        NativeTemplateIds.MAP,
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.MAP));
        SpireUI.mount(NativeTemplateIds.MAP);
        assertTrue(SpireUI.component(NativeTemplateIds.MAP).isMounted());
        SpireUI.unmount(NativeTemplateIds.MAP);
    }

    @Test
    public void hostBackendAttachDetach() {
        final List<String> events = new ArrayList<String>();
        SpireUI.setHostBackend(
                new HostBackend() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void attach(UiTree tree) {
                        events.add("a:" + tree.windowId());
                    }

                    @Override
                    public void detach(UiTree tree) {
                        events.add("d:" + tree.windowId());
                    }

                    @Override
                    public void applyLayout(UiTree tree) {}
                });
        SpireUI.register(
                new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.mount("demo");
        SpireUI.unmount("demo");
        assertEquals(2, events.size());
        assertEquals("a:demo", events.get(0));
        assertEquals("d:demo", events.get(1));
    }
}
