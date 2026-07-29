package artframework.sts1.input;

import artframework.api.ArtFramework;
import artframework.context.IntentNames;
import artframework.context.IntentResult;
import artframework.context.SurfaceIds;
import artframework.context.UiIntent;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Room intents soft-reject without live STS dungeon (milestone 26.2). */
public class Sts1IntentExecutorRoomTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        CombatInputRouter.resetForTests();
    }

    @Test
    public void claimRewardWithoutDungeonRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.CLAIM_REWARD, SurfaceIds.REWARD_COMBAT, Integer.valueOf(0)));
        assertEquals(IntentResult.Status.REJECTED, r.status);
        assertTrue(r.message.length() > 0);
    }

    @Test
    public void skipRewardWithoutDungeonRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.SKIP_REWARD, SurfaceIds.REWARD_COMBAT));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void chooseRestWithoutDungeonRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.CHOOSE_REST_OPTION, SurfaceIds.REST, "smith"));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void openChestWithoutDungeonRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.OPEN_CHEST, SurfaceIds.TREASURE));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void buyShopWithoutDungeonRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.BUY_SHOP_ENTRY, SurfaceIds.SHOP, Integer.valueOf(0)));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void purgeWithoutDungeonRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.PURGE_CARD, SurfaceIds.SHOP));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }

    @Test
    public void pressCancelWithoutOverlayRejectedSoft() {
        IntentResult r =
                Sts1IntentExecutor.INSTANCE.execute(
                        UiIntent.of(IntentNames.PRESS_CANCEL, SurfaceIds.COMBAT_PROCEED));
        assertEquals(IntentResult.Status.REJECTED, r.status);
    }
}
