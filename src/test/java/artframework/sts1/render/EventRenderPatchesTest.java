package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.MonsterIntentView;
import artframework.context.RestView;
import artframework.context.RewardView;
import artframework.context.SelectView;
import artframework.context.ShopView;
import artframework.context.SurfaceIds;
import artframework.context.TopPanelView;
import artframework.context.TreasureView;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.patch.EventRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventRenderPatchesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void mountedEvent() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        EventView event = EventView.of("Neow",
                Arrays.asList(EventOptionView.of(0, "Talk", true), EventOptionView.of(1, "Leave", true)));
        backend.publish(
                ContextFrame.ofFull(
                        1L, 1L, "event", null, ControlsView.empty(), MapView.empty(),
                        event, SelectView.empty(), RewardView.empty(), RestView.empty(),
                        TreasureView.empty(), ShopView.empty(), TopPanelView.empty(),
                        MonsterIntentView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.EVENT).mount();
    }

    @Test
    public void fullReadySuppressesNativeEventRender() {
        mountedEvent();
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertTrue("FULL + mounted + event + ready executor must suppress GenericEventDialog.render",
                result.isPresent());
    }

    @Test
    public void offContinuesNativeEventRender() {
        mountedEvent();

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertFalse("OFF must let GenericEventDialog.render continue", result.isPresent());
    }

    @Test
    public void fullWithoutExecutorContinuesNativeEventRender() {
        mountedEvent();
        FullPresentMode.setEventLevel(PresentLevel.FULL);

        SpireReturn<Void> result = EventRenderPatches.ObserveNativeEventRender.Prefix(null, null);

        assertFalse("FULL without a ready executor must fall back to native render",
                result.isPresent());
    }
}
