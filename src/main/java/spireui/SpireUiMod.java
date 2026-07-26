package spireui;

import basemod.BaseMod;
import basemod.devcommands.ConsoleCommand;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import spireui.api.SpireUI;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.c1.host.StageHost;
import spireui.c2.NativeTemplateIds;
import spireui.console.SpireUiCommand;
import spireui.ops.StsNativeOps;

/**
 * ModTheSpire entry: registers demo + native templates; installs C1 StageHost, STS ops, console.
 */
@SpireInitializer
public class SpireUiMod implements PostInitializeSubscriber {

    public static final String DEMO_ID = "demo";

    public static void initialize() {
        new SpireUiMod();
    }

    public SpireUiMod() {
        BaseMod.subscribe(this);
        StageHost.install();
        try {
            ConsoleCommand.addCommand("spireui", SpireUiCommand.class);
        } catch (Throwable t) {
            try {
                BaseMod.logger.warn("SpireUI: console command register failed: " + t.getMessage());
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void receivePostInitialize() {
        SpireUI.setNativeOpsBackend(StsNativeOps.INSTANCE);
        SpireUI.register(new WindowDef(DEMO_ID, WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.register(
                new WindowDef("comp_sample", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.register(
                new WindowDef("glass_demo", WindowClass.SYNTHETIC, "layouts/glass_demo.json"));
        registerNative(NativeTemplateIds.MAP);
        registerNative(NativeTemplateIds.EVENT);
        registerNative(NativeTemplateIds.SELECT_GRID);
        registerNative(NativeTemplateIds.SELECT_HAND);
        registerNative(NativeTemplateIds.END_TURN);
        BaseMod.logger.info("SpireUI: demo + native templates + StsNativeOps + spireui console");
    }

    private static void registerNative(String id) {
        SpireUI.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
    }
}
