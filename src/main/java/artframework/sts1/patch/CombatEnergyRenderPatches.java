package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import artframework.sts1.render.NativeRenderBridge;

/**
 * Observe native energy rendering for ART recording/projection. The original STS
 * EnergyPanel.render remains the visual authority; ART does not suppress it.
 */
public final class CombatEnergyRenderPatches {
    private CombatEnergyRenderPatches() {}

    @SpirePatch(clz = EnergyPanel.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeEnergyRender {
        public static SpireReturn<Void> Prefix(EnergyPanel __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.combat.energy", "com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }
}
