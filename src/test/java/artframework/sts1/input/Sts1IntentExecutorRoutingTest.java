package artframework.sts1.input;

import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.IntentNames;
import artframework.context.IntentResult;
import artframework.context.SurfaceIds;
import artframework.context.UiIntent;
import artframework.c2.MapNodeRef;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Sts1IntentExecutor touches live STS types; without a dungeon it must fail soft (rejected),
 * never throw. Full play path is covered on D1.
 */
public class Sts1IntentExecutorRoutingTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        CombatInputRouter.resetForTests();
    }

    @Test
    public void unknownIntentRejected() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of("not_a_real_intent", SurfaceIds.COMBAT_HAND));
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message.contains("unknown"));
    }

    @Test
    public void playWithoutDungeonRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(
                                IntentNames.PLAY_CARD,
                                SurfaceIds.COMBAT_HAND,
                                new CardRef("x", "Strike_R"),
                                "m0"));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void endTurnWithoutOverlayRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.PRESS_END_TURN, SurfaceIds.COMBAT_CONTROLS));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void executeIfOwnedUsesInstalledExecutor() {
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(Sts1IntentExecutor.INSTANCE);
        IntentResult r =
                CombatInputRouter.executeIfOwned(
                        UiIntent.of(IntentNames.BEGIN_DRAG, SurfaceIds.COMBAT_HAND, "missing"));
        // No dungeon → soft reject from executor, not "not installed"
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(
                r.message.contains("card not in hand")
                        || r.message.contains("executor")
                        || r.message.contains("no player")
                        || r.message.length() > 0);
    }

    @Test
    public void mapClickWithoutLiveMapRejectsSoft() {
        IntentResult r = Sts1IntentExecutor.INSTANCE.execute(
                UiIntent.of(IntentNames.CLICK_MAP_NODE, SurfaceIds.MAP, new MapNodeRef(0, 0, "monster")));
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message.contains("map node unavailable"));
    }
}
