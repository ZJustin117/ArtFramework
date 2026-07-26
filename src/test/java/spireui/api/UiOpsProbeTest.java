package spireui.api;

import org.junit.After;
import org.junit.Test;
import spireui.c1.layout.LayoutNode;
import spireui.c2.EndTurnInterceptor;
import spireui.c2.EventOptionInterceptor;
import spireui.c2.EventOptionRef;
import spireui.c2.GateResult;
import spireui.c2.MapNodeInterceptor;
import spireui.c2.MapNodeRef;
import spireui.c2.MapPin;
import spireui.c2.NativeTemplateIds;
import spireui.c2.NativeTemplateRuntime;
import spireui.c2.SelectCardInterceptor;
import spireui.c2.SelectCardRef;
import spireui.c2.SelectConfirmInterceptor;
import spireui.c2.SelectKind;
import spireui.ops.FakeNativeOps;
import spireui.ops.NativeOpsBackend;

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
        SpireUI.resetForTests();
    }

    @Test
    public void notBoundWhenTemplateInactive() {
        FakeNativeOps fake = installFake();
        UiOpResult r = SpireUI.ops().selectCard(SelectKind.GRID, "Strike", 0);
        assertEquals(UiOpResult.Status.NOT_BOUND, r.status);
        assertEquals(0, fake.selectCardCount);
    }

    @Test
    public void selectCardBlockedDoesNotCallBackend() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.SELECT_GRID);
        NativeTemplateRuntime.selectGrid().addCardInterceptor(new SelectCardInterceptor() {
            @Override
            public GateResult intercept(SelectKind kind, SelectCardRef card) {
                return GateResult.BLOCK;
            }
        });
        UiOpResult r = SpireUI.ops().selectCard(SelectKind.GRID, "Strike", 0);
        assertEquals(UiOpResult.Status.BLOCKED, r.status);
        assertEquals(0, fake.selectCardCount);
    }

    @Test
    public void selectCardAllowCallsBackend() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.SELECT_GRID);
        UiOpResult r = SpireUI.ops().selectCard(SelectKind.GRID, "Defend", 1);
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals(1, fake.selectCardCount);
        assertEquals(SelectKind.GRID, fake.lastSelectKind);
        assertEquals("Defend", fake.lastSelectCardId);
    }

    @Test
    public void confirmSelectHand() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.SELECT_HAND);
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().confirmSelect(SelectKind.HAND).status);
        assertEquals(1, fake.confirmCount);
        assertEquals(SelectKind.HAND, fake.lastConfirmKind);
    }

    @Test
    public void mapClickBlocked() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.MAP);
        NativeTemplateRuntime.map().addInterceptor(new MapNodeInterceptor() {
            @Override
            public MapNodeInterceptor.Result intercept(MapNodeRef node) {
                return MapNodeInterceptor.Result.BLOCK;
            }
        });
        UiOpResult r = SpireUI.ops().clickMapNode(new MapNodeRef(1, 2, "monster"));
        assertEquals(UiOpResult.Status.BLOCKED, r.status);
        assertEquals(0, fake.mapClickCount);
    }

    @Test
    public void mapClickAllow() {
        FakeNativeOps fake = installFake();
        registerBind(NativeTemplateIds.MAP);
        UiOpResult r = SpireUI.ops().clickMapNode(new MapNodeRef(0, 1, "rest"));
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
                SpireUI.ops().chooseEventOption(0, "Leave").status);
        assertEquals(1, fake.eventOptionCount);

        NativeTemplateRuntime.endTurn().setButtonEnabled(false);
        assertEquals(UiOpResult.Status.BLOCKED, SpireUI.ops().pressEndTurn().status);
        NativeTemplateRuntime.endTurn().setButtonEnabled(true);
        NativeTemplateRuntime.endTurn().addInterceptor(new EndTurnInterceptor() {
            @Override
            public GateResult intercept() {
                return GateResult.BLOCK;
            }
        });
        assertEquals(UiOpResult.Status.BLOCKED, SpireUI.ops().pressEndTurn().status);
        assertEquals(0, fake.endTurnCount);
    }

    @Test
    public void playHandCardGesture() {
        FakeNativeOps fake = installFake();
        UiOpResult r = SpireUI.ops().playHandCard("Bash", "Cultist");
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals("Bash", fake.lastPlayCardId);
        assertEquals("Cultist", fake.lastPlayTarget);
    }

    @Test
    public void clickButtonC1() {
        final AtomicInteger hits = new AtomicInteger();
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.open("demo");
        SpireUI.ops().onButton("demo", "close", new Runnable() {
            @Override
            public void run() {
                hits.incrementAndGet();
            }
        });
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().clickButton("demo", "close").status);
        assertEquals(1, hits.get());
        assertEquals(UiOpResult.Status.NOT_BOUND, SpireUI.ops().clickButton("missing", "close").status);
    }

    @Test
    public void probeShape() {
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.open("demo");
        registerBind(NativeTemplateIds.MAP);
        NativeTemplateRuntime.map().putPin(new MapPin("p1", new MapNodeRef(0, 1, "m"), "vote"));
        registerBind(NativeTemplateIds.END_TURN);
        NativeTemplateRuntime.endTurn().setButtonEnabled(false);

        Map<String, Object> snap = SpireUI.probe().asMap();
        assertEquals(Integer.valueOf(1), snap.get("schemaVersion"));
        assertEquals("spireui", snap.get("modId"));
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

        String json = SpireUI.probe().toJsonLine();
        assertTrue(json.startsWith("SPIREUI_PROBE "));
        assertTrue(json.contains("\"schemaVersion\":1"));
    }

    @Test
    public void unavailableWhenBackendFails() {
        SpireUI.setNativeOpsBackend(new NativeOpsBackend() {
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
                SpireUI.ops().selectCard(SelectKind.GRID, "X", 0).status);
    }

    @Test
    public void layoutHasButtonForDemo() {
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.open("demo");
        LayoutNode root = SpireUI.layoutRoot("demo");
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
        SpireUI.setNativeOpsBackend(fake);
        return fake;
    }

    private static void registerBind(String id) {
        SpireUI.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
        SpireUI.bind(id);
    }
}
