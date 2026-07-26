package artframework.c2.hooks;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c2.EndTurnInterceptor;
import artframework.c2.EventOptionInterceptor;
import artframework.c2.EventOptionRef;
import artframework.c2.GateResult;
import artframework.c2.MapNodeInterceptor;
import artframework.c2.MapNodeRef;
import artframework.c2.NativeTemplateIds;
import artframework.c2.NativeTemplateRuntime;
import artframework.c2.SelectCardInterceptor;
import artframework.c2.SelectCardRef;
import artframework.c2.SelectKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeUiHooksTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void inactiveMapAllows() {
        assertEquals(
                MapNodeInterceptor.Result.ALLOW,
                NativeUiHooks.onMapNodeClick(0, 0, "monster"));
    }

    @Test
    public void boundMapBlocks() {
        ArtFramework.register(new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        ArtFramework.bind(NativeTemplateIds.MAP);
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
        ArtFramework.register(new WindowDef(NativeTemplateIds.EVENT, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.EVENT));
        ArtFramework.bind(NativeTemplateIds.EVENT);
        NativeTemplateRuntime.event().addInterceptor(new EventOptionInterceptor() {
            @Override
            public GateResult intercept(EventOptionRef option) {
                return option.index == 0 ? GateResult.BLOCK : GateResult.ALLOW;
            }
        });
        assertEquals(GateResult.BLOCK, NativeUiHooks.onEventOption(0, "a"));
        assertEquals(GateResult.ALLOW, NativeUiHooks.onEventOption(1, "b"));

        ArtFramework.register(new WindowDef(NativeTemplateIds.SELECT_GRID, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.SELECT_GRID));
        ArtFramework.bind(NativeTemplateIds.SELECT_GRID);
        NativeTemplateRuntime.selectGrid().addCardInterceptor(new SelectCardInterceptor() {
            @Override
            public GateResult intercept(SelectKind kind, SelectCardRef card) {
                return GateResult.BLOCK;
            }
        });
        assertEquals(GateResult.BLOCK, NativeUiHooks.onSelectCard(SelectKind.GRID, "x", 0));
        assertEquals(GateResult.ALLOW, NativeUiHooks.onSelectCard(SelectKind.HAND, "x", 0));

        ArtFramework.register(new WindowDef(NativeTemplateIds.END_TURN, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.END_TURN));
        ArtFramework.bind(NativeTemplateIds.END_TURN);
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
