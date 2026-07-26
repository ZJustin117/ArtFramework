package spireui.c2.hooks;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.c2.EndTurnInterceptor;
import spireui.c2.EventOptionInterceptor;
import spireui.c2.EventOptionRef;
import spireui.c2.GateResult;
import spireui.c2.MapNodeInterceptor;
import spireui.c2.MapNodeRef;
import spireui.c2.NativeTemplateIds;
import spireui.c2.NativeTemplateRuntime;
import spireui.c2.SelectCardInterceptor;
import spireui.c2.SelectCardRef;
import spireui.c2.SelectKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeUiHooksTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void inactiveMapAllows() {
        assertEquals(
                MapNodeInterceptor.Result.ALLOW,
                NativeUiHooks.onMapNodeClick(0, 0, "monster"));
    }

    @Test
    public void boundMapBlocks() {
        SpireUI.register(new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        SpireUI.bind(NativeTemplateIds.MAP);
        NativeTemplateRuntime.map().addInterceptor(new MapNodeInterceptor() {
            @Override
            public Result intercept(MapNodeRef node) {
                return Result.BLOCK;
            }
        });
        assertEquals(
                MapNodeInterceptor.Result.BLOCK,
                NativeUiHooks.onMapNodeClick(1, 2, "event"));
    }

    @Test
    public void eventSelectEndTurnHooks() {
        SpireUI.register(new WindowDef(NativeTemplateIds.EVENT, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.EVENT));
        SpireUI.bind(NativeTemplateIds.EVENT);
        NativeTemplateRuntime.event().addInterceptor(new EventOptionInterceptor() {
            @Override
            public GateResult intercept(EventOptionRef option) {
                return option.index == 0 ? GateResult.BLOCK : GateResult.ALLOW;
            }
        });
        assertEquals(GateResult.BLOCK, NativeUiHooks.onEventOption(0, "a"));
        assertEquals(GateResult.ALLOW, NativeUiHooks.onEventOption(1, "b"));

        SpireUI.register(new WindowDef(NativeTemplateIds.SELECT_GRID, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.SELECT_GRID));
        SpireUI.bind(NativeTemplateIds.SELECT_GRID);
        NativeTemplateRuntime.selectGrid().addCardInterceptor(new SelectCardInterceptor() {
            @Override
            public GateResult intercept(SelectKind kind, SelectCardRef card) {
                return GateResult.BLOCK;
            }
        });
        assertEquals(GateResult.BLOCK, NativeUiHooks.onSelectCard(SelectKind.GRID, "x", 0));
        assertEquals(GateResult.ALLOW, NativeUiHooks.onSelectCard(SelectKind.HAND, "x", 0));

        SpireUI.register(new WindowDef(NativeTemplateIds.END_TURN, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.END_TURN));
        SpireUI.bind(NativeTemplateIds.END_TURN);
        assertTrue(NativeUiHooks.isEndTurnEnabledHint());
        NativeTemplateRuntime.endTurn().setButtonEnabled(false);
        assertFalse(NativeUiHooks.isEndTurnEnabledHint());
        assertEquals(GateResult.BLOCK, NativeUiHooks.onEndTurnPress());
        NativeTemplateRuntime.endTurn().setButtonEnabled(true);
        NativeTemplateRuntime.endTurn().addInterceptor(new EndTurnInterceptor() {
            @Override
            public GateResult intercept() {
                return GateResult.BLOCK;
            }
        });
        assertEquals(GateResult.BLOCK, NativeUiHooks.onEndTurnPress());
    }
}
