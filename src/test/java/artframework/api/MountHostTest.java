package artframework.api;

import org.junit.After;
import org.junit.Test;
import artframework.c2.NativeTemplateIds;
import artframework.core.HostBackend;
import artframework.core.UiTree;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MountHostTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void mountUnmountSynthetic() {
        ArtFramework.register(
                new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        WindowHandle h = ArtFramework.mount("demo");
        assertTrue(h.isOpen());
        assertNotNull(ArtFramework.tree("demo"));
        ArtFramework.unmount("demo");
        assertNull(ArtFramework.find("demo"));
        assertNull(ArtFramework.tree("demo"));
    }

    @Test
    public void mountNativeEqualsBind() {
        ArtFramework.register(
                new WindowDef(
                        NativeTemplateIds.MAP,
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.MAP));
        ArtFramework.mount(NativeTemplateIds.MAP);
        assertTrue(ArtFramework.component(NativeTemplateIds.MAP).isMounted());
        ArtFramework.unmount(NativeTemplateIds.MAP);
    }

    @Test
    public void hostBackendAttachDetach() {
        final List<String> events = new ArrayList<String>();
        ArtFramework.setHostBackend(
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
        ArtFramework.register(
                new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.mount("demo");
        ArtFramework.unmount("demo");
        assertEquals(2, events.size());
        assertEquals("a:demo", events.get(0));
        assertEquals("d:demo", events.get(1));
    }
}
