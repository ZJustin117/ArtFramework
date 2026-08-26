package artframework.sts1.render;

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
import artframework.sts1.patch.CombatEnergyRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatEnergyRenderPatchesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
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
    }

    @Test
    public void fullReadySuppressesNativeEnergyRender() {
        mountedCombat();
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result =
                CombatEnergyRenderPatches.ObserveNativeEnergyRender.Prefix(null, null);

        assertTrue("FULL + mounted + combat + ready executor must suppress EnergyPanel.render",
                result.isPresent());
    }

    @Test
    public void offContinuesNativeEnergyRender() {
        mountedCombat();

        SpireReturn<Void> result =
                CombatEnergyRenderPatches.ObserveNativeEnergyRender.Prefix(null, null);

        assertFalse("OFF must let EnergyPanel.render continue", result.isPresent());
    }

    @Test
    public void fullWithoutExecutorContinuesNativeEnergyRender() {
        mountedCombat();
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);

        SpireReturn<Void> result =
                CombatEnergyRenderPatches.ObserveNativeEnergyRender.Prefix(null, null);

        assertFalse("FULL without a ready executor must fall back to native render",
                result.isPresent());
    }
}
