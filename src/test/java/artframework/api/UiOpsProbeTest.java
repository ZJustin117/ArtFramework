package artframework.api;

import org.junit.After;
import org.junit.Test;
import artframework.c1.layout.LayoutNode;
import artframework.c2.EventOptionRef;
import artframework.component.MapNodeRef;
import artframework.c2.MapPin;
import artframework.component.NativeTemplateIds;
import artframework.c2.NativeTemplateRuntime;
import artframework.c2.SelectCardRef;
import artframework.c2.SelectKind;
import artframework.ops.FakeNativeOps;
import artframework.ops.NativeOpsBackend;
import artframework.presentation.PresentationRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UiOpsProbeTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void notBoundWhenTemplateInactive() {
        FakeNativeOps fake = installFake();
        UiOpResult r = ArtFramework.ops().selectCard(SelectKind.GRID, "Strike", 0);
        assertEquals(UiOpResult.Status.NOT_BOUND, r.status);
        assertEquals(0, fake.selectCardCount);
    }

    @Test
    public void selectCardBlockedDoesNotCallBackend() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.SELECT_GRID);
        ArtFramework.connect("ui/" + NativeTemplateIds.SELECT_GRID + "/card_selected",
                new artframework.core.SignalListener() {
            @Override public artframework.core.SignalDecision onSignal(artframework.core.UiSignal signal) {
                return artframework.core.SignalDecision.stopRejected("blocked");
            }
        });
        UiOpResult r = ArtFramework.ops().selectCard(SelectKind.GRID, "Strike", 0);
        assertEquals(UiOpResult.Status.BLOCKED, r.status);
        assertEquals(0, fake.selectCardCount);
    }

    @Test
    public void selectCardAllowCallsBackend() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.SELECT_GRID);
        UiOpResult r = ArtFramework.ops().selectCard(SelectKind.GRID, "Defend", 1);
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals(1, fake.selectCardCount);
        assertEquals(SelectKind.GRID, fake.lastSelectKind);
        assertEquals("Defend", fake.lastSelectCardId);
    }

    @Test
    public void confirmSelectHand() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.SELECT_HAND);
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().confirmSelect(SelectKind.HAND).status);
        assertEquals(1, fake.confirmCount);
        assertEquals(SelectKind.HAND, fake.lastConfirmKind);
    }

    @Test
    public void mapClickBlocked() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.MAP);
        ArtFramework.connect("ui/" + NativeTemplateIds.MAP + "/node_clicked",
                new artframework.core.SignalListener() {
            @Override public artframework.core.SignalDecision onSignal(artframework.core.UiSignal signal) {
                return artframework.core.SignalDecision.stopRejected("blocked");
            }
        });
        UiOpResult r = ArtFramework.ops().clickMapNode(new MapNodeRef(1, 2, "monster"));
        assertEquals(UiOpResult.Status.BLOCKED, r.status);
        assertEquals(0, fake.mapClickCount);
    }

    @Test
    public void mapClickAllow() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.MAP);
        UiOpResult r = ArtFramework.ops().clickMapNode(new MapNodeRef(0, 1, "rest"));
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals(1, fake.mapClickCount);
        assertEquals(0, fake.lastMapRow);
        assertEquals(1, fake.lastMapCol);
    }

    @Test
    public void eventOptionAndEndTurn() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.EVENT);
        registerBind(NativeTemplateIds.END_TURN);
        assertEquals(
                UiOpResult.Status.OK,
                ArtFramework.ops().chooseEventOption(0, "Leave").status);
        assertEquals(1, fake.eventOptionCount);

        NativeTemplateRuntime.endTurn().setButtonEnabled(false);
        assertEquals(UiOpResult.Status.BLOCKED, ArtFramework.ops().pressEndTurn().status);
        NativeTemplateRuntime.endTurn().setButtonEnabled(true);
        ArtFramework.connect("ui/" + NativeTemplateIds.END_TURN + "/pressed",
                new artframework.core.SignalListener() {
            @Override public artframework.core.SignalDecision onSignal(artframework.core.UiSignal signal) {
                return artframework.core.SignalDecision.stopRejected("blocked");
            }
        });
        assertEquals(UiOpResult.Status.BLOCKED, ArtFramework.ops().pressEndTurn().status);
        assertEquals(0, fake.endTurnCount);
    }

    @Test
    public void playHandCardGesture() {
        FakeNativeOps fake = installFake();
        UiOpResult r = ArtFramework.ops().playHandCard("Bash", "Cultist");
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals("Bash", fake.lastPlayCardId);
        assertEquals("Cultist", fake.lastPlayTarget);
    }

    @Test
    public void clickButtonC1() {
        final AtomicInteger hits = new AtomicInteger();
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");
        ArtFramework.ops().onButton("demo", "close", new Runnable() {
            @Override
            public void run() {
                hits.incrementAndGet();
            }
        });
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().clickButton("demo", "close").status);
        assertEquals(1, hits.get());
        assertEquals(UiOpResult.Status.NOT_BOUND, ArtFramework.ops().clickButton("missing", "close").status);
    }

    @Test
    public void replacingAndRemovingC1SugarHandlersDisconnectsPriorSubscriptions() {
        final AtomicInteger first = new AtomicInteger();
        final AtomicInteger second = new AtomicInteger();
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");

        ArtFramework.ops().onButton("demo", "close", new Runnable() {
            @Override public void run() { first.incrementAndGet(); }
        });
        ArtFramework.ops().onButton("demo", "close", new Runnable() {
            @Override public void run() { second.incrementAndGet(); }
        });
        ArtFramework.ops().clickButton("demo", "close");
        assertEquals(0, first.get());
        assertEquals(1, second.get());

        ArtFramework.ops().removeButtonHandler("demo", "close");
        ArtFramework.ops().clickButton("demo", "close");
        assertEquals(0, first.get());
        assertEquals(1, second.get());
    }

    @Test
    public void c1SugarHandlerRegisteredBeforeMountBindsWhenTheTreeOpens() {
        final AtomicInteger hits = new AtomicInteger();
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.ops().onButton("demo", "close", new Runnable() {
            @Override public void run() { hits.incrementAndGet(); }
        });

        ArtFramework.open("demo");
        ArtFramework.ops().clickButton("demo", "close");

        assertEquals(1, hits.get());
    }

    @Test
    public void syntheticControlOpsStoreTheirCurrentValueOnTheEcsEntity() {
        ArtFramework.register(new WindowDef("lightwave_demo", WindowClass.SYNTHETIC,
                "layouts/lightwave_demo.json"));
        ArtFramework.open("lightwave_demo");

        assertEquals(UiOpResult.Status.OK,
                ArtFramework.ops().setSlider("lightwave_demo", "wave_slider", 0.8f).status);
        artframework.presentation.PresentationContext context =
                artframework.presentation.PresentationRuntime.context("lightwave_demo");
        artframework.ecs.EntityId node = artframework.presentation.PresentationRuntime.find(
                context, "wave_slider");
        artframework.presentation.ControlValueComponent value = context.world().get(
                node, artframework.presentation.ControlValueComponent.class);

        assertNotNull(value);
        assertEquals(0.8f, ((Number) value.value).floatValue(), 0.001f);
    }

    @Test
    public void syntheticControlOpsRejectNonFiniteNumbers() {
        ArtFramework.register(new WindowDef("lightwave_demo", WindowClass.SYNTHETIC,
                "layouts/lightwave_demo.json"));
        ArtFramework.open("lightwave_demo");

        assertEquals(UiOpResult.Status.OK,
                ArtFramework.ops().setSlider("lightwave_demo", "wave_slider", 0.4f).status);

        assertEquals(UiOpResult.Status.UNAVAILABLE,
                ArtFramework.ops().setSlider("lightwave_demo", "wave_slider", Float.NaN).status);
        assertEquals(UiOpResult.Status.UNAVAILABLE,
                ArtFramework.ops().setSlider("lightwave_demo", "wave_slider", Float.POSITIVE_INFINITY).status);
        assertEquals(UiOpResult.Status.UNAVAILABLE,
                ArtFramework.ops().setProgress("lightwave_demo", "missing", Float.NaN).status);
        String json = ArtFramework.probe().toJsonLine();
        assertTrue(!json.contains("NaN"));
        assertTrue(!json.contains("Infinity"));
        assertTrue(json.contains("0.4"));
    }

    @Test
    public void invokeSyntheticComponentAction() {
        final AtomicInteger hits = new AtomicInteger();
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.mount("demo");
        ArtFramework.ops().onButton("demo", "close", new Runnable() {
            @Override
            public void run() {
                hits.incrementAndGet();
            }
        });

        assertEquals(
                UiOpResult.Status.OK,
                ArtFramework.ops().invoke("demo", "click_button", "close").status);
        assertEquals(1, hits.get());
        assertEquals(artframework.core.ComponentKind.SYNTHETIC, ArtFramework.component("demo").kind());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components =
                (List<Map<String, Object>>) ArtFramework.probe().asMap().get("components");
        boolean found = false;
        for (Map<String, Object> component : components) {
            if ("demo".equals(component.get("id"))) {
                assertEquals("SYNTHETIC", component.get("kind"));
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void probeShape() {
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");
        registerBind(NativeTemplateIds.MAP);
        NativeTemplateRuntime.map().putPin(new MapPin("p1", new MapNodeRef(0, 1, "m"), "vote"));
        registerBind(NativeTemplateIds.END_TURN);
        NativeTemplateRuntime.endTurn().setButtonEnabled(false);

        Map<String, Object> snap = ArtFramework.probe().asMap();
        assertEquals(Integer.valueOf(1), snap.get("schemaVersion"));
        assertEquals("artframework", snap.get("modId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> templates = (Map<String, Object>) snap.get("templates");
        assertEquals(Boolean.TRUE, templates.get("mapBound"));
        assertEquals(Boolean.TRUE, templates.get("endTurnBound"));
        @SuppressWarnings("unchecked")
        Map<String, Object> windows = (Map<String, Object>) snap.get("windows");
        @SuppressWarnings("unchecked")
        List<String> openIds = (List<String>) windows.get("openIds");
        assertTrue(openIds.contains("demo"));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) snap.get("map");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pins = (List<Map<String, Object>>) map.get("pins");
        assertEquals(1, pins.size());
        assertEquals("0_1", pins.get(0).get("nodeId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> endTurn = (Map<String, Object>) snap.get("endTurn");
        assertEquals(Boolean.FALSE, endTurn.get("buttonEnabled"));

        String json = ArtFramework.probe().toJsonLine();
        assertTrue(json.startsWith("ART_PROBE "));
        assertTrue(json.contains("\"schemaVersion\":1"));
    }

    @Test
    public void probeDoesNotCreateAbsentC2PresentationScopes() {
        PresentationRegistry.close("c2-projection");
        PresentationRegistry.close("c2-surfaces");

        ArtFramework.probe().asMap();

        assertEquals(null, PresentationRegistry.existingContext("c2-projection"));
        assertEquals(null, PresentationRegistry.existingContext("c2-surfaces"));
    }

    @Test
    public void projectionReadsDoNotCreateAbsentProjectionScope() {
        PresentationRegistry.close("c2-projection");
        artframework.context.PresentProjection projection = ArtFramework.projection();

        projection.lastFrameId();
        projection.scene();
        projection.sceneEpoch();
        projection.isAvailable();
        projection.isStale();
        projection.lastFrame();
        projection.dragInstanceId();
        projection.controls();
        projection.map();
        projection.event();
        projection.select();
        projection.reward();
        projection.rest();
        projection.treasure();
        projection.shop();
        projection.topPanel();
        projection.intents();
        projection.viewport();
        projection.probeSlice();

        assertEquals(null, PresentationRegistry.existingContext("c2-projection"));
    }

    @Test
    public void unavailableWhenBackendFails() {
        ArtFramework.setNativeOpsBackend(new NativeOpsBackend() {
            @Override
            public UiOpResult selectCard(SelectKind kind, String cardId, int index) {
                return UiOpResult.unavailable("no screen");
            }

            @Override
            public UiOpResult confirmSelect(SelectKind kind) {
                return UiOpResult.unavailable("no screen");
            }

            @Override
            public UiOpResult clickMapNode(MapNodeRef node) {
                return UiOpResult.ok();
            }

            @Override
            public UiOpResult chooseEventOption(int index, String label) {
                return UiOpResult.ok();
            }

            @Override
            public UiOpResult pressEndTurn() {
                return UiOpResult.ok();
            }

            @Override
            public UiOpResult playHandCard(String cardId, String target) {
                return UiOpResult.ok();
            }
        });
        registerBind(NativeTemplateIds.SELECT_GRID);
        assertEquals(
                UiOpResult.Status.UNAVAILABLE,
                ArtFramework.ops().selectCard(SelectKind.GRID, "X", 0).status);
    }

    @Test
    public void layoutHasButtonForDemo() {
        ArtFramework.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");
        LayoutNode root = ArtFramework.layoutRoot("demo");
        assertNotNull(root);
        boolean found = false;
        for (LayoutNode c : root.children) {
            if (c.type == LayoutNode.Type.BUTTON && "close".equals(c.id)) {
                found = true;
            }
        }
        assertTrue("demo layout should expose button id=close for C1 ops", found);
    }

    private static FakeNativeOps installFake() {
        FakeNativeOps fake = new FakeNativeOps();
        ArtFramework.setNativeOpsBackend(fake);
        return fake;
    }

    private static void registerBind(String id) {
        ArtFramework.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
        ArtFramework.bind(id);
    }
}
