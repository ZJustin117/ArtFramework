package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;
import artframework.sts1.render.ControlsDrawPath;

/** Skip native end-turn chrome while ART controls full-present owns display. */
public final class CombatControlsRenderPatches {

    private CombatControlsRenderPatches() {}

    @SpirePatch(clz = EndTurnButton.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeEndTurnRender {
        public static SpireReturn<Void> Prefix(EndTurnButton __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return ControlsDrawPath.shouldSuppressNativeEndTurn()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
