package spireui.c2;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.api.WindowHandle;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatTemplatesTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void bindEventActivatesAndIntercepts() {
        register(NativeTemplateIds.EVENT);
        WindowHandle h = SpireUI.bind(NativeTemplateIds.EVENT);
        assertTrue(NativeTemplateRuntime.isEventBound());
        NativeTemplateRuntime.event().setEventId("spin_the_wheel");
        assertEquals("spin_the_wheel", NativeTemplateRuntime.event().getEventId());

        final AtomicInteger hits = new AtomicInteger();
        NativeTemplateRuntime.event().addInterceptor(new EventOptionInterceptor() {
            @Override
            public GateResult intercept(EventOptionRef option) {
                hits.incrementAndGet();
                return option.index == 0 ? GateResult.BLOCK : GateResult.ALLOW;
            }
        });
        assertEquals(
                GateResult.BLOCK,
                NativeTemplateRuntime.event().dispatchOption(new EventOptionRef(0, "Leave")));
        assertEquals(
                GateResult.ALLOW,
                NativeTemplateRuntime.event().dispatchOption(new EventOptionRef(1, "Stay")));
        assertEquals(2, hits.get());

        h.close();
        assertFalse(NativeTemplateRuntime.isEventBound());
        assertEquals("", NativeTemplateRuntime.event().getEventId());
        assertEquals(
                GateResult.ALLOW,
                NativeTemplateRuntime.event().dispatchOption(new EventOptionRef(0, "Leave")));
    }

    @Test
    public void bindSelectGridCardAndConfirm() {
        register(NativeTemplateIds.SELECT_GRID);
        SpireUI.bind(NativeTemplateIds.SELECT_GRID);
        assertTrue(NativeTemplateRuntime.isSelectGridBound());
        SelectTemplate grid = NativeTemplateRuntime.selectGrid();
        assertEquals(SelectKind.GRID, grid.kind());

        grid.addCardInterceptor(new SelectCardInterceptor() {
            @Override
            public GateResult intercept(SelectKind kind, SelectCardRef card) {
                return "Strike".equals(card.cardId) ? GateResult.BLOCK : GateResult.ALLOW;
            }
        });
        grid.addConfirmInterceptor(new SelectConfirmInterceptor() {
            @Override
            public GateResult intercept(SelectKind kind) {
                return GateResult.BLOCK;
            }
        });
        assertEquals(
                GateResult.BLOCK,
                grid.dispatchCard(new SelectCardRef("Strike", 0)));
        assertEquals(
                GateResult.ALLOW,
                grid.dispatchCard(new SelectCardRef("Defend", 1)));
        assertEquals(GateResult.BLOCK, grid.dispatchConfirm());
        SpireUI.close(NativeTemplateIds.SELECT_GRID);
        assertFalse(NativeTemplateRuntime.isSelectGridBound());
        assertEquals(GateResult.ALLOW, grid.dispatchConfirm());
    }

    @Test
    public void bindSelectHandIndependentOfGrid() {
        register(NativeTemplateIds.SELECT_GRID);
        register(NativeTemplateIds.SELECT_HAND);
        SpireUI.bind(NativeTemplateIds.SELECT_HAND);
        assertTrue(NativeTemplateRuntime.isSelectHandBound());
        assertFalse(NativeTemplateRuntime.isSelectGridBound());
        assertEquals(SelectKind.HAND, NativeTemplateRuntime.selectHand().kind());
        SpireUI.close(NativeTemplateIds.SELECT_HAND);
    }

    @Test
    public void endTurnDisabledBlocksWithoutInterceptor() {
        register(NativeTemplateIds.END_TURN);
        SpireUI.bind(NativeTemplateIds.END_TURN);
        assertTrue(NativeTemplateRuntime.isEndTurnBound());
        EndTurnTemplate et = NativeTemplateRuntime.endTurn();
        assertEquals(GateResult.ALLOW, et.dispatchPress());
        et.setButtonEnabled(false);
        assertEquals(GateResult.BLOCK, et.dispatchPress());
        et.setButtonEnabled(true);
        et.addInterceptor(new EndTurnInterceptor() {
            @Override
            public GateResult intercept() {
                return GateResult.BLOCK;
            }
        });
        assertEquals(GateResult.BLOCK, et.dispatchPress());
        SpireUI.close(NativeTemplateIds.END_TURN);
        assertFalse(NativeTemplateRuntime.isEndTurnBound());
        assertTrue(et.isButtonEnabled());
        assertEquals(GateResult.ALLOW, et.dispatchPress());
    }

    @Test
    public void openNativeDelegatesToBind() {
        register(NativeTemplateIds.EVENT);
        SpireUI.open(NativeTemplateIds.EVENT);
        assertTrue(NativeTemplateRuntime.isBound(NativeTemplateIds.EVENT));
        SpireUI.close(NativeTemplateIds.EVENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownTemplateStillThrows() {
        SpireUI.register(new WindowDef("sts.other", WindowClass.NATIVE_TEMPLATE, "sts.other"));
        SpireUI.bind("sts.other");
    }

    @Test
    public void inactiveEventAllows() {
        NativeTemplateRuntime.event().addInterceptor(new EventOptionInterceptor() {
            @Override
            public GateResult intercept(EventOptionRef option) {
                return GateResult.BLOCK;
            }
        });
        assertEquals(
                GateResult.ALLOW,
                NativeTemplateRuntime.event().dispatchOption(new EventOptionRef(0, "")));
    }

    private static void register(String id) {
        SpireUI.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
    }
}
