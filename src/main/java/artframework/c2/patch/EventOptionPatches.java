package artframework.c2.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.events.AbstractEvent;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import artframework.c2.GateResult;
import artframework.c2.hooks.NativeUiHooks;

/**
 * Gates native {@code buttonEffect} calls from {@code AbstractEvent.update}.
 * Direct Prefix on protected abstract {@code buttonEffect} crashes MTS ParamInfo — use instrument.
 */
@SuppressWarnings("unused")
public final class EventOptionPatches {

    private EventOptionPatches() {}

    /** Package-visible for instrument-generated call sites. */
    public static boolean allowOption(int index) {
        return NativeUiHooks.onEventOption(index, "") != GateResult.BLOCK;
    }

    @SpirePatch(clz = AbstractEvent.class, method = "update", paramtypez = {})
    public static class GateButtonEffectCalls {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"buttonEffect".equals(call.getMethodName())) {
                        return;
                    }
                    call.replace(
                            "{"
                                    + "if (artframework.c2.patch.EventOptionPatches.allowOption($1)) {"
                                    + "  $_ = $proceed($$);"
                                    + "}"
                                    + "}");
                }
            };
        }
    }
}
