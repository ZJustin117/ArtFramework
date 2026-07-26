package artframework;

import basemod.BaseMod;
import basemod.devcommands.ConsoleCommand;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c1.host.StageHost;
import artframework.c2.NativeTemplateIds;
import artframework.console.ArtCommand;
import artframework.ops.StsNativeOps;

/**
 * ModTheSpire entry: registers demo + native templates; installs C1 StageHost, STS ops, console.
 */
@SpireInitializer
public class ArtFrameworkMod implements PostInitializeSubscriber {

    public static final String DEMO_ID = "demo";

    public static void initialize() {
        new ArtFrameworkMod();
    }

    public ArtFrameworkMod() {
        BaseMod.subscribe(this);
        StageHost.install();
        try {
            ConsoleCommand.addCommand("art", ArtCommand.class);
        } catch (Throwable t) {
            try {
                BaseMod.logger.warn("ArtFramework: console command register failed: " + t.getMessage());
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void receivePostInitialize() {
        ArtFramework.setNativeOpsBackend(StsNativeOps.INSTANCE);
        ArtFramework.register(new WindowDef(DEMO_ID, WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.register(
                new WindowDef("comp_sample", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        ArtFramework.register(
                new WindowDef("glass_demo", WindowClass.SYNTHETIC, "layouts/glass_demo.json"));
        registerNative(NativeTemplateIds.MAP);
        registerNative(NativeTemplateIds.EVENT);
        registerNative(NativeTemplateIds.SELECT_GRID);
        registerNative(NativeTemplateIds.SELECT_HAND);
        registerNative(NativeTemplateIds.END_TURN);
        BaseMod.logger.info("ArtFramework: demo + native templates + StsNativeOps + art console");
    }

    private static void registerNative(String id) {
        ArtFramework.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
    }
}
