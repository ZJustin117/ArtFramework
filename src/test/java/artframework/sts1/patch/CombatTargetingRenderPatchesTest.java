package artframework.sts1.patch;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CombatTargetingRenderPatchesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        artframework.sts1.render.Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void mountedCombat() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
        ArtFramework.component(SurfaceIds.COMBAT_TARGETING).mount();
    }

    @Test
    public void offContinuesNativeTargetingRender() {
        mountedCombat();

        SpireReturn<Void> result =
                CombatTargetingRenderPatches.ObservePlayerTargeting.Prefix(null, null);

        assertFalse("OFF must let renderTargetingUi continue natively", result.isPresent());
    }

    @Test
    public void observeContinuesNativeTargetingRender() {
        mountedCombat();
        FullPresentMode.setTargetingLevel(PresentLevel.OBSERVE);

        SpireReturn<Void> result =
                CombatTargetingRenderPatches.ObservePlayerTargeting.Prefix(null, null);

        assertFalse("OBSERVE must let renderTargetingUi continue natively", result.isPresent());
    }

    @Test
    public void fullContinuesNativeTargetingRender() {
        mountedCombat();
        FullPresentMode.setTargetingLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                CombatTargetingRenderPatches.ObservePlayerTargeting.Prefix(null, null);

        assertFalse("FULL must still let renderTargetingUi continue natively (zero suppression)",
                result.isPresent());
    }

    @Test
    public void exceptionFailsOpen() {
        SpireReturn<Void> result =
                CombatTargetingRenderPatches.ObservePotionTargeting.Prefix(null, null);
        assertFalse("Patch outside a mounted scene must fail open to native", result.isPresent());
    }

    @Test
    public void panicAndHostRecreationKeepTargetingNative() {
        mountedCombat();
        FullPresentMode.setTargetingLevel(PresentLevel.FULL);
        artframework.sts1.PresentSafety.panic("targeting-test");

        SpireReturn<Void> panicResult =
                CombatTargetingRenderPatches.ObservePlayerTargeting.Prefix(null, null);
        assertFalse(panicResult.isPresent());
        assertTrue(NativeRenderBridgeProbe.nativeContinuation());

        artframework.sts1.PresentSafety.clearPanic();
        artframework.sts1.PresentSafety.onHostRecreated();
        assertEquals(1, artframework.sts1.PresentSafety.recreationCount());
        SpireReturn<Void> recreatedResult =
                CombatTargetingRenderPatches.ObservePotionTargeting.Prefix(null, null);
        assertFalse(recreatedResult.isPresent());
    }

    private static final class NativeRenderBridgeProbe {
        private static boolean nativeContinuation() {
            java.util.List<artframework.sts1.render.RenderDisposition> dispositions =
                    artframework.sts1.render.NativeRenderBridge.ledger().dispositions();
            return !dispositions.isEmpty() && dispositions.get(dispositions.size() - 1).nativeContinuation;
        }
    }
}
