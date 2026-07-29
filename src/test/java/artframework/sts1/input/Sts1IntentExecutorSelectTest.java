package artframework.sts1.input;

import artframework.context.IntentNames;
import artframework.context.IntentResult;
import artframework.context.UiIntent;
import artframework.context.SurfaceIds;
import artframework.c2.SelectKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Soft-reject SELECT_* without live dungeon (22.4). */
public class Sts1IntentExecutorSelectTest {

    @Test
    public void selectCardWithoutDungeonRejects() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(
                                IntentNames.SELECT_CARD,
                                SurfaceIds.SELECT_GRID,
                                SelectKind.GRID,
                                "Strike_R",
                                Integer.valueOf(0)));
        assertNotNull(r);
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message != null && !r.message.isEmpty());
    }

    @Test
    public void confirmSelectWithoutDungeonRejects() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(
                                IntentNames.CONFIRM_SELECT,
                                SurfaceIds.SELECT_GRID,
                                SelectKind.GRID));
        assertNotNull(r);
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void selectCardRequiresCardId() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.SELECT_CARD, SurfaceIds.SELECT_GRID));
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message.contains("card id") || r.message.contains("required") || r.message.contains("select"));
    }
}
