package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native energy orb rendering. When the energy surface is FULL + mounted + combat scene,
 * ART suppresses the original EnergyPanel.render and paints the orb through the synced C2
 * item. Otherwise the native renderer continues unchanged.
 */
public final class CombatEnergyRenderPatches {
    private CombatEnergyRenderPatches() {}

    @SpirePatch(clz = EnergyPanel.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeEnergyRender {
        public static SpireReturn<Void> Prefix(EnergyPanel __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.combat.energy", "com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
