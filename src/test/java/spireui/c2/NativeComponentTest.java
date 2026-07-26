package spireui.c2;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.UiOpResult;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.core.ComponentKind;
import spireui.core.SignalHandler;
import spireui.core.SignalNames;
import spireui.core.UiComponent;
import spireui.ops.FakeNativeOps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NativeComponentTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void componentsRegistered() {
        assertNotNull(SpireUI.component(NativeTemplateIds.MAP));
        assertEquals(ComponentKind.NATIVE_HOST, SpireUI.component(NativeTemplateIds.MAP).kind());
        assertEquals(5, NativeComponents.ids().size());
    }

    @Test
    public void bindMountsComponent() {
        SpireUI.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        assertFalse(SpireUI.component(NativeTemplateIds.MAP).isMounted());
        SpireUI.bind(NativeTemplateIds.MAP);
        assertTrue(SpireUI.component(NativeTemplateIds.MAP).isMounted());
        SpireUI.close(NativeTemplateIds.MAP);
        assertFalse(SpireUI.component(NativeTemplateIds.MAP).isMounted());
    }

    @Test
    public void invokeClickMapNode() {
        SpireUI.setNativeOpsBackend(new FakeNativeOps());
        SpireUI.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        SpireUI.bind(NativeTemplateIds.MAP);
        UiOpResult r =
                SpireUI.ops()
                        .invoke(
                                NativeTemplateIds.MAP,
                                "click_node",
                                new MapNodeRef(1, 2, "x"));
        assertEquals(UiOpResult.Status.OK, r.status);
    }

    @Test
    public void invokeEndTurn() {
        SpireUI.setNativeOpsBackend(new FakeNativeOps());
        SpireUI.register(
                new WindowDef(
                        NativeTemplateIds.END_TURN,
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.END_TURN));
        SpireUI.bind(NativeTemplateIds.END_TURN);
        assertEquals(
                UiOpResult.Status.OK,
                SpireUI.ops().invoke(NativeTemplateIds.END_TURN, "press").status);
    }

    @Test
    public void probeListsComponents() {
        SpireUI.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        SpireUI.bind(NativeTemplateIds.MAP);
        Map<String, Object> snap = SpireUI.probe().asMap();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> comps = (List<Map<String, Object>>) snap.get("components");
        assertNotNull(comps);
        assertEquals(5, comps.size());
        boolean mapBound = false;
        for (Map<String, Object> c : comps) {
            if (NativeTemplateIds.MAP.equals(c.get("id"))) {
                assertEquals(Boolean.TRUE, c.get("bound"));
                assertEquals(Boolean.TRUE, c.get("mounted"));
                mapBound = true;
            }
        }
        assertTrue(mapBound);
    }

    @Test
    public void nativeSignalConnect() {
        UiComponent map = SpireUI.component(NativeTemplateIds.MAP);
        AtomicInteger n = new AtomicInteger();
        map.connect(
                SignalNames.NODE_CLICKED,
                new SignalHandler() {
                    @Override
                    public void handle(Object... args) {
                        n.incrementAndGet();
                    }
                });
        map.emit(SignalNames.NODE_CLICKED, new MapNodeRef(0, 0, "a"));
        assertEquals(1, n.get());
    }
}
