package artframework.sts1.patch;

import artframework.sts1.render.EnergyDrawPath;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

/** Skip the native current/max energy orb while ART owns the FULL energy surface. */
public final class CombatEnergyRenderPatches {
    private CombatEnergyRenderPatches() {}

    @SpirePatch(clz = EnergyPanel.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeEnergyRender {
        public static SpireReturn<Void> Prefix(EnergyPanel __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return EnergyDrawPath.shouldSuppressNativeEnergy()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
