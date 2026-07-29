package artframework.context;

import artframework.api.ArtFramework;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.render.EnergyDrawPath;
import artframework.sts1.render.IntentDrawPath;
import artframework.sts1.render.ProceedDrawPath;
import artframework.sts1.render.RestDrawPath;
import artframework.sts1.render.RewardDrawPath;
import artframework.sts1.render.ShopDrawPath;
import artframework.sts1.render.TopPanelDrawPath;
import artframework.sts1.render.TreasureDrawPath;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RoomPresentViewsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        CombatInputRouter.resetForTests();
        FullPresentMode.resetForTests();
    }

    @Test
    public void rewardViewFrameAndDrawPath() {
        List<RewardItemView> items = new ArrayList<RewardItemView>();
        items.add(RewardItemView.of(0, "gold", "Gold 25"));
        items.add(RewardItemView.of(1, "card", "Card reward"));
        ContextFrame frame =
                ContextFrame.ofFull(
                        1L,
                        1L,
                        "reward",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        EventView.empty(),
                        SelectView.empty(),
                        RewardView.of("combat", "Rewards", items),
                        RestView.empty(),
                        TreasureView.empty(),
                        ShopView.empty(),
                        TopPanelView.empty(),
                        MonsterIntentView.empty(),
                        null);
        ArtFramework.publishFrame(frame);
        assertEquals(2, ArtFramework.projection().reward().itemCount());
        assertEquals(2, RewardDrawPath.buildFromProjection().size());
        Map<String, Object> probe = RewardDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("count"));
        assertEquals("combat", probe.get("kind"));
    }

    @Test
    public void restShopTreasureTopIntentsPaths() {
        List<RestView.RestOptionView> opts = new ArrayList<RestView.RestOptionView>();
        opts.add(RestView.RestOptionView.of("rest", "Rest"));
        opts.add(RestView.RestOptionView.of("smith", "Smith"));
        List<ShopView.ShopEntryView> entries = new ArrayList<ShopView.ShopEntryView>();
        entries.add(ShopView.ShopEntryView.of(0, "card", "Strike", 50));
        List<MonsterIntentView.IntentEntry> intents = new ArrayList<MonsterIntentView.IntentEntry>();
        intents.add(
                new MonsterIntentView.IntentEntry("cultist", "Cultist", "ATTACK", "", 1, 100f, 200f));
        ContextFrame frame =
                ContextFrame.ofFull(
                        2L,
                        1L,
                        "rest",
                        null,
                        ControlsView.combatWithProceed(3, 5, 10, 2, 0, true, true, true, true, false, false),
                        MapView.empty(),
                        EventView.empty(),
                        SelectView.empty(),
                        RewardView.empty(),
                        RestView.of(opts),
                        TreasureView.closed(),
                        ShopView.of(99, entries, true, 75),
                        TopPanelView.of(70, 80, 99, 3, 0, "Ironclad"),
                        MonsterIntentView.of(intents),
                        null);
        ArtFramework.publishFrame(frame);
        assertEquals(2, RestDrawPath.buildFromProjection().size());
        assertEquals(1, ShopDrawPath.buildFromProjection().size());
        assertTrue(Boolean.TRUE.equals(TreasureDrawPath.probeSlice().get("canOpen")));
        assertEquals(Integer.valueOf(70), TopPanelDrawPath.probeSlice().get("hp"));
        assertEquals(1, IntentDrawPath.buildFromProjection().size());
        assertEquals(Integer.valueOf(3), EnergyDrawPath.probeSlice().get("energy"));
        assertFalse(ProceedDrawPath.buildFromProjection().isEmpty());
    }

    @Test
    public void rewardSurfaceMountAndClaim() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        FullPresentMode.setRewardLevel(PresentLevel.FULL);
        artframework.core.UiComponent reward = ArtFramework.component(SurfaceIds.REWARD_COMBAT);
        assertNotNull(reward);
        assertEquals(artframework.api.UiOpResult.Status.OK, reward.action("mount_reward").status);
        assertTrue(reward.isMounted());
        artframework.api.UiOpResult claim = reward.action("claim", Integer.valueOf(0));
        assertEquals(artframework.api.UiOpResult.Status.OK, claim.status);
        assertFalse(backend.signalLog().isEmpty());
    }

    @Test
    public void restSurfaceChoose() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        FullPresentMode.setRestLevel(PresentLevel.FULL);
        artframework.core.UiComponent rest = ArtFramework.component(SurfaceIds.REST);
        rest.action("mount_rest");
        artframework.api.UiOpResult r = rest.action("choose", "smith");
        assertEquals(artframework.api.UiOpResult.Status.OK, r.status);
    }

    @Test
    public void proceedAndEnergySurfaces() {
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        FullPresentMode.setProceedLevel(PresentLevel.FULL);
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.COMBAT_PROCEED).action("mount_proceed");
        ArtFramework.component(SurfaceIds.COMBAT_ENERGY).action("mount_energy");
        assertTrue(ArtFramework.component(SurfaceIds.COMBAT_PROCEED).isMounted());
        assertTrue(ArtFramework.component(SurfaceIds.COMBAT_ENERGY).isMounted());
        assertEquals(
                artframework.api.UiOpResult.Status.OK,
                ArtFramework.component(SurfaceIds.COMBAT_PROCEED).action("press_proceed").status);
    }

    @Test
    public void fullPresentModeProbeIncludesNewSurfaces() {
        FullPresentMode.setRewardLevel(PresentLevel.OBSERVE);
        FullPresentMode.setShopLevel(PresentLevel.FULL);
        Map<String, Object> m = FullPresentMode.probeSlice();
        assertEquals("OBSERVE", m.get("reward"));
        assertEquals("FULL", m.get("shop"));
        assertEquals("OFF", m.get("rest"));
    }
}
