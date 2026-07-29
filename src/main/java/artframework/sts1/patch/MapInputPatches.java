package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import artframework.sts1.input.Sts1MapIntentBridge;

/** Re-apply sticky ART map click after InputHelper clears per-frame click edges. */
@SuppressWarnings("unused")
public final class MapInputPatches {

    private MapInputPatches() {}

    @SpirePatch(clz = InputHelper.class, method = "updateFirst", paramtypez = {})
    public static class AfterInputUpdateFirst {
        @SpirePostfixPatch
        public static void Postfix() {
            Sts1MapIntentBridge.onAfterInputUpdate();
        }
    }
}
