package artframework.c2;

import artframework.component.NativeTemplateIds;

import artframework.component.MapNodeRef;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.core.ComponentKind;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalHandler;
import artframework.core.SignalNames;
import artframework.core.SignalSubscription;
import artframework.core.UiComponent;
import artframework.ecs.EntityId;
import artframework.ops.FakeNativeOps;

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
        ArtFramework.resetForTests();
    }

    @Test
    public void componentsRegistered() {
        assertNotNull(ArtFramework.component(NativeTemplateIds.MAP));
        assertEquals(ComponentKind.NATIVE_HOST, ArtFramework.component(NativeTemplateIds.MAP).kind());
        assertEquals(5, NativeComponents.ids().size());
    }

    @Test
    public void repeatedBindAndUnbindKeepsNativeComponentStateInSync() {
        registerBind(NativeTemplateIds.MAP);
        assertTrue(NativeComponents.get(NativeTemplateIds.MAP).isMounted());

        registerBind(NativeTemplateIds.MAP);
        assertTrue(NativeTemplateRuntime.isMapBound());
        assertTrue(NativeComponents.get(NativeTemplateIds.MAP).isMounted());

        ArtFramework.unmount(NativeTemplateIds.MAP);
        assertFalse(NativeTemplateRuntime.isMapBound());
        assertFalse(NativeComponents.get(NativeTemplateIds.MAP).isMounted());
    }

    @Test
    public void bindMountsComponent() {
        ArtFramework.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        assertFalse(ArtFramework.component(NativeTemplateIds.MAP).isMounted());
        ArtFramework.bind(NativeTemplateIds.MAP);
        assertTrue(ArtFramework.component(NativeTemplateIds.MAP).isMounted());
        ArtFramework.close(NativeTemplateIds.MAP);
        assertFalse(ArtFramework.component(NativeTemplateIds.MAP).isMounted());
    }

    @Test
    public void invokeClickMapNode() {
        ArtFramework.setNativeOpsBackend(new FakeNativeOps());
        ArtFramework.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        ArtFramework.bind(NativeTemplateIds.MAP);
        UiOpResult r =
                ArtFramework.ops()
                        .invoke(
                                NativeTemplateIds.MAP,
                                "click_node",
                                new MapNodeRef(1, 2, "x"));
        assertEquals(UiOpResult.Status.OK, r.status);
    }

    @Test
    public void invokeEndTurn() {
        ArtFramework.setNativeOpsBackend(new FakeNativeOps());
        ArtFramework.register(
                new WindowDef(
                        NativeTemplateIds.END_TURN,
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.END_TURN));
        ArtFramework.bind(NativeTemplateIds.END_TURN);
        assertEquals(
                UiOpResult.Status.OK,
                ArtFramework.ops().invoke(NativeTemplateIds.END_TURN, "press").status);
    }

    @Test
    public void probeListsComponents() {
        ArtFramework.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        ArtFramework.bind(NativeTemplateIds.MAP);
        Map<String, Object> snap = ArtFramework.probe().asMap();
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
        UiComponent map = ArtFramework.component(NativeTemplateIds.MAP);
        AtomicInteger n = new AtomicInteger();
        SignalSubscription subscription = map.connect(
                SignalNames.NODE_CLICKED,
                new SignalHandler() {
                    @Override
                    public void handle(Object... args) {
                        n.incrementAndGet();
                    }
                });
        SignalDispatchResult result = map.dispatch(SignalNames.NODE_CLICKED, new MapNodeRef(0, 0, "a"));
        assertEquals(1, n.get());
        assertFalse(result.isStopped());
        subscription.disconnect();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nativeSignalConnectRejectsUndeclaredSignal() {
        ArtFramework.component(NativeTemplateIds.MAP).connect("custom", new SignalHandler() {
            @Override
            public void handle(Object... args) {}
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void nativeSignalDisconnectRejectsUndeclaredSignal() {
        ArtFramework.component(NativeTemplateIds.MAP).disconnect("custom", args -> {});
    }

    private static void registerBind(String id) {
        ArtFramework.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
        ArtFramework.bind(id);
    }
}
