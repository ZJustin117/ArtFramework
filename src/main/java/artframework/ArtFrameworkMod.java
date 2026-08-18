package artframework;

import basemod.BaseMod;
import basemod.devcommands.ConsoleCommand;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c1.host.StageHost;
import artframework.component.NativeTemplateIds;
import artframework.console.ArtCommand;
import artframework.ops.StsNativeOps;
import artframework.sts1.backend.Sts1PresentationBackend;

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
        artframework.sts1.StsRuntimeReady.setStarted(true);
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
        Sts1PresentationBackend.INSTANCE.installSignals();
        try {
            ArtFramework.skeletons().register(new artframework.sts1.skeleton.Sts1Spine34Provider());
            artframework.sts1.skeleton.Sts1SkeletonBridge.installPresentationSignals();
        } catch (Throwable t) {
            BaseMod.logger.warn("ArtFramework: Spine34 provider skipped: " + t.getMessage());
        }
        artframework.sts1.lab.StsLabNativeNavigator.install();
        try {
            artframework.sts1.assets.Sts1HostAssets.install();
        } catch (Throwable t) {
            BaseMod.logger.warn("ArtFramework: STS1 HostAssets install skipped: " + t.getMessage());
        }
        try {
            artframework.sts1.skeleton.Sts1Spine42Dev.install();
        } catch (Throwable t) {
            BaseMod.logger.warn("ArtFramework: optional Spine42 developer bundle skipped: " + t.getMessage());
        }
        try {
            // Executor handles programmatic intents; native input suppress stays off until
            // ART owns a real touch path (flag via CombatInputRouter / future console).
            artframework.sts1.input.CombatInputRouter.setExecutor(
                    artframework.sts1.input.Sts1IntentExecutor.INSTANCE);
        } catch (Throwable t) {
            BaseMod.logger.warn("ArtFramework: STS1 intent executor install skipped: " + t.getMessage());
        }
        ArtFramework.register(new WindowDef(DEMO_ID, WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.register(
                new WindowDef("comp_sample", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        ArtFramework.register(
                new WindowDef("glass_demo", WindowClass.SYNTHETIC, "layouts/glass_demo.json"));
        ArtFramework.register(
                new WindowDef("grid_tabs_demo", WindowClass.SYNTHETIC, "layouts/grid_tabs_sample.json"));
        ArtFramework.register(
                new WindowDef("sts1_vanilla_demo", WindowClass.SYNTHETIC, "layouts/sts1_vanilla_demo.json"));
        ArtFramework.register(
                new WindowDef(
                        "lightwave_components_demo",
                        WindowClass.SYNTHETIC,
                        "layouts/lightwave_components_demo.json"));
        ArtFramework.register(
                new WindowDef("lightwave_demo", WindowClass.SYNTHETIC, "layouts/lightwave_demo.json"));
        ArtFramework.register(
                new WindowDef(
                        "glass_lightwave_demo",
                        WindowClass.SYNTHETIC,
                        "layouts/glass_lightwave_demo.json"));
        try {
            artframework.core.PresentPacks.installBuiltinLightwavePack();
        } catch (Throwable t) {
            BaseMod.logger.warn("ArtFramework: lightwave present pack skipped: " + t.getMessage());
        }
        registerNative(NativeTemplateIds.MAP);
        registerNative(NativeTemplateIds.EVENT);
        registerNative(NativeTemplateIds.SELECT_GRID);
        registerNative(NativeTemplateIds.SELECT_HAND);
        registerNative(NativeTemplateIds.END_TURN);
        BaseMod.logger.info(
                "ArtFramework: demo + native templates + StsNativeOps + STS1 frame backend + art console");
    }

    private static void registerNative(String id) {
        ArtFramework.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
    }
}
