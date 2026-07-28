package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import artframework.sts1.input.CombatInputRouter;

/**
 * When ART owns combat hand input (FULL + suppress flag), skip native hand card update so
 * hitboxes do not double-drive the engine (16.5b).
 */
public final class CombatHandInputPatches {

    private CombatHandInputPatches() {}

    /**
     * Opt-in: only when {@link CombatInputRouter#setSuppressNativeInput(true)} and FULL+mounted.
     * Default off so D1 remain playable while ART draw/observe is tested. Blocking whole
     * {@code updateInput} is coarse; refine to hand-only when ART touch router lands.
     */
    @SpirePatch(clz = AbstractPlayer.class, method = "updateInput")
    public static class SuppressNativeHandInput {
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance) {
            return CombatInputRouter.shouldSuppressNativeInput()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
