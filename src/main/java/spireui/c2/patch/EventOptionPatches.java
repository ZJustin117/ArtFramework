package spireui.c2.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.events.AbstractEvent;
import spireui.c2.GateResult;
import spireui.c2.hooks.NativeUiHooks;

/** Gates {@code AbstractEvent.buttonEffect} when {@code sts.event} is bound. */
@SuppressWarnings("unused")
public final class EventOptionPatches {

    private EventOptionPatches() {}

    @SpirePatch(clz = AbstractEvent.class, method = "buttonEffect", paramtypez = {int.class})
    public static class GateButtonEffect {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractEvent __instance, int buttonPressed) {
            GateResult r = NativeUiHooks.onEventOption(buttonPressed, "");
            if (r == GateResult.BLOCK) {
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }
}
