package artframework.c2.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;
import artframework.c2.hooks.HostPatchResults;
import artframework.c2.hooks.NativeUiHooks;

/**
 * UI-only end-turn gates. Does <strong>not</strong> broadcast multiplayer messages.
 */
@SuppressWarnings("unused")
public final class EndTurnButtonPatches {

    private EndTurnButtonPatches() {}

    @SpirePatch(clz = EndTurnButton.class, method = "enable", paramtypez = {})
    public static class GateEnable {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(EndTurnButton __instance) {
            if (!NativeUiHooks.isEndTurnEnabledHint()) {
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = EndTurnButton.class, method = "disable", paramtypez = {boolean.class})
    public static class GateDisable {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(EndTurnButton __instance, boolean isEnemyTurn) {
            if (!isEnemyTurn) {
                return SpireReturn.Continue();
            }
            return HostPatchResults.voidPrefix(NativeUiHooks.onEndTurnPress());
        }
    }
}
