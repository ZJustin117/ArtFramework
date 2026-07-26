package artframework.ops;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c2.MapNodeRef;
import artframework.c2.NativeTemplateIds;
import artframework.c2.SelectKind;
import artframework.c2.hooks.NativeUiHooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static artframework.c2.MapNodeInterceptor.Result;

public class GateLabTest {

    @After
    public void tearDown() {
        GateLab.resetForTests();
        ArtFramework.resetForTests();
    }

    @Test
    public void mapGateBlocksOpsAndHooks() {
        FakeNativeOps fake = new FakeNativeOps();
        ArtFramework.setNativeOpsBackend(fake);
        ArtFramework.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        ArtFramework.bind(NativeTemplateIds.MAP);

        GateLab.apply("map", "block");
        assertEquals(
                UiOpResult.Status.BLOCKED,
                ArtFramework.ops().clickMapNode(new MapNodeRef(1, 0, "m")).status);
        assertEquals(0, fake.mapClickCount);
        assertEquals(Result.BLOCK, NativeUiHooks.onMapNodeClick(1, 0, "m"));

        GateLab.apply("map", "clear");
        assertEquals(
                UiOpResult.Status.OK,
                ArtFramework.ops().clickMapNode(new MapNodeRef(1, 0, "m")).status);
        assertEquals(1, fake.mapClickCount);
    }

    @Test
    public void endTurnAndSelectGate() {
        FakeNativeOps fake = new FakeNativeOps();
        ArtFramework.setNativeOpsBackend(fake);
        ArtFramework.register(
                new WindowDef(
                        NativeTemplateIds.END_TURN,
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.END_TURN));
        ArtFramework.bind(NativeTemplateIds.END_TURN);
        ArtFramework.register(
                new WindowDef(
                        NativeTemplateIds.SELECT_GRID,
                        WindowClass.NATIVE_TEMPLATE,
                        NativeTemplateIds.SELECT_GRID));
        ArtFramework.bind(NativeTemplateIds.SELECT_GRID);

        GateLab.apply("endturn", "block");
        assertEquals(UiOpResult.Status.BLOCKED, ArtFramework.ops().pressEndTurn().status);

        GateLab.apply("select-grid", "block");
        assertEquals(
                UiOpResult.Status.BLOCKED,
                ArtFramework.ops().selectCard(SelectKind.GRID, "Strike", 0).status);
        assertEquals(UiOpResult.Status.BLOCKED, ArtFramework.ops().confirmSelect(SelectKind.GRID).status);

        String st = GateLab.apply("all", "clear");
        assertTrue(st.contains("map=false"));
        assertEquals(UiOpResult.Status.OK, ArtFramework.ops().pressEndTurn().status);
    }
}
