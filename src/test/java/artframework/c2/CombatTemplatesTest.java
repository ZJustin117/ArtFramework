package artframework.c2;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.api.WindowHandle;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatTemplatesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void bindEventActivatesAndIntercepts() {
        register(NativeTemplateIds.EVENT);
        WindowHandle h = ArtFramework.bind(NativeTemplateIds.EVENT);
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
        ArtFramework.bind(NativeTemplateIds.SELECT_GRID);
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
        ArtFramework.close(NativeTemplateIds.SELECT_GRID);
        assertFalse(NativeTemplateRuntime.isSelectGridBound());
        assertEquals(GateResult.ALLOW, grid.dispatchConfirm());
    }

    @Test
    public void bindSelectHandIndependentOfGrid() {
        register(NativeTemplateIds.SELECT_GRID);
        register(NativeTemplateIds.SELECT_HAND);
        ArtFramework.bind(NativeTemplateIds.SELECT_HAND);
        assertTrue(NativeTemplateRuntime.isSelectHandBound());
        assertFalse(NativeTemplateRuntime.isSelectGridBound());
        assertEquals(SelectKind.HAND, NativeTemplateRuntime.selectHand().kind());
        ArtFramework.close(NativeTemplateIds.SELECT_HAND);
    }

    @Test
    public void endTurnDisabledBlocksWithoutInterceptor() {
        register(NativeTemplateIds.END_TURN);
        ArtFramework.bind(NativeTemplateIds.END_TURN);
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
        ArtFramework.close(NativeTemplateIds.END_TURN);
        assertFalse(NativeTemplateRuntime.isEndTurnBound());
        assertTrue(et.isButtonEnabled());
        assertEquals(GateResult.ALLOW, et.dispatchPress());
    }

    @Test
    public void openNativeDelegatesToBind() {
        register(NativeTemplateIds.EVENT);
        ArtFramework.open(NativeTemplateIds.EVENT);
        assertTrue(NativeTemplateRuntime.isBound(NativeTemplateIds.EVENT));
        ArtFramework.close(NativeTemplateIds.EVENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownTemplateStillThrows() {
        ArtFramework.register(new WindowDef("sts.other", WindowClass.NATIVE_TEMPLATE, "sts.other"));
        ArtFramework.bind("sts.other");
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
        ArtFramework.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
    }
}
