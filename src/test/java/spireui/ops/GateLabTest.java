package spireui.ops;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.UiOpResult;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.c2.MapNodeRef;
import spireui.c2.NativeTemplateIds;
import spireui.c2.SelectKind;
import spireui.c2.hooks.NativeUiHooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static spireui.c2.MapNodeInterceptor.Result;

public class GateLabTest {

    @After
    public void tearDown() {
        GateLab.resetForTests();
        SpireUI.resetForTests();
    }

    @Test
    public void mapGateBlocksOpsAndHooks() {
        FakeNativeOps fake = new FakeNativeOps();
        SpireUI.setNativeOpsBackend(fake);
        SpireUI.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        SpireUI.bind(NativeTemplateIds.MAP);

        GateLab.apply("map", "block");
        assertEquals(
                UiOpResult.Status.BLOCKED,
                SpireUI.ops().clickMapNode(new MapNodeRef(1, 0, "m")).status);
        assertEquals(0, fake.mapClickCount);
        assertEquals(Result.BLOCK, NativeUiHooks.onMapNodeClick(1, 0, "m"));

        GateLab.apply("map", "clear");
        assertEquals(
                UiOpResult.Status.OK,
                SpireUI.ops().clickMapNode(new MapNodeRef(1, 0, "m")).status);
        assertEquals(1, fake.mapClickCount);
    }

    @Test
    public void endTurnAndSelectGate() {
        FakeNativeOps fake = new FakeNativeOps();
        SpireUI.setNativeOpsBackend(fake);
        SpireUI.register(
                new WindowDef(
                        NativeTemplateIds.END_TURN,
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.END_TURN));
        SpireUI.bind(NativeTemplateIds.END_TURN);
        SpireUI.register(
                new WindowDef(
                        NativeTemplateIds.SELECT_GRID,
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.SELECT_GRID));
        SpireUI.bind(NativeTemplateIds.SELECT_GRID);

        GateLab.apply("endturn", "block");
        assertEquals(UiOpResult.Status.BLOCKED, SpireUI.ops().pressEndTurn().status);

        GateLab.apply("select-grid", "block");
        assertEquals(
                UiOpResult.Status.BLOCKED,
                SpireUI.ops().selectCard(SelectKind.GRID, "Strike", 0).status);
        assertEquals(UiOpResult.Status.BLOCKED, SpireUI.ops().confirmSelect(SelectKind.GRID).status);

        String st = GateLab.apply("all", "clear");
        assertTrue(st.contains("map=false"));
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().pressEndTurn().status);
    }
}
