package artframework.api;

import org.junit.After;
import org.junit.Test;
import artframework.c2.NativeTemplateIds;
import artframework.core.HostBackend;
import artframework.core.HostCapabilities;
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
        assertNull(ArtFramework.find("sts.map"));
        assertTrue(ArtFramework.listOpenIds().isEmpty());
    }

    @Test
    public void legacyNativeAliasIsCanonicalInOpenListAndRemovedOnClose() {
        ArtFramework.register(
                new WindowDef(
                        "map_alias",
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.LEGACY_MAP));

        WindowHandle handle = ArtFramework.mount("map_alias");

        assertEquals(1, ArtFramework.listOpenIds().size());
        assertEquals(NativeTemplateIds.MAP, ArtFramework.listOpenIds().get(0));
        assertTrue(ArtFramework.find("map_alias") == handle);
        assertTrue(ArtFramework.find(NativeTemplateIds.LEGACY_MAP) == handle);

        handle.close();

        assertNull(ArtFramework.find("map_alias"));
        assertNull(ArtFramework.find(NativeTemplateIds.MAP));
        assertNull(ArtFramework.find(NativeTemplateIds.LEGACY_MAP));
        assertTrue(ArtFramework.listOpenIds().isEmpty());
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

    @Test
    public void tickDelegatesToHostAndRejectsNegativeDelta() {
        final int[] ticks = new int[1];
        ArtFramework.setHostBackend(
                new HostBackend() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void attach(UiTree tree) {}

                    @Override
                    public void detach(UiTree tree) {}

                    @Override
                    public void applyLayout(UiTree tree) {}

                    @Override
                    public void tick(float deltaSeconds) {
                        ticks[0]++;
                    }

                    @Override
                    public HostCapabilities capabilities() {
                        return HostCapabilities.of(HostCapabilities.INPUT);
                    }
                });
        ArtFramework.tick(0.016f);
        assertEquals(1, ticks[0]);
        assertTrue(ArtFramework.probe().toJsonLine().contains("\"input\""));
        try {
            ArtFramework.tick(-1f);
        } catch (IllegalArgumentException expected) {
            assertEquals(1, ticks[0]);
            return;
        }
        throw new AssertionError("negative delta should fail");
    }
}
