package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;
import artframework.sts1.render.NativeRenderBridge;

/**
 * Observe native end-turn rendering for ART recording/projection. The original STS
 * EndTurnButton.render remains the visual authority; ART does not suppress it.
 */
public final class CombatControlsRenderPatches {

    private CombatControlsRenderPatches() {}

    @SpirePatch(clz = EndTurnButton.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeEndTurnRender {
        public static SpireReturn<Void> Prefix(EndTurnButton __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.combat.controls", "com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }
}
