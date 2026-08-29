package artframework.api;

import artframework.assets.HostAssetsHolder;
import artframework.c1.C1NodeFactories;
import artframework.c1.SyntheticRuntime;
import artframework.component.UiNodeRegistry;
import artframework.context.PresentProjections;
import artframework.context.PresentSurfaces;
import artframework.core.AnimationPlayers;
import artframework.core.HostBackends;
import artframework.core.PackSystems;
import artframework.core.SignalGroups;
import artframework.core.Themes;
import artframework.ecs.ArtEcs;
import artframework.presentation.PresentationRegistry;
import artframework.render.RenderHosts;
import artframework.skeleton.SkeletonProviders;

/** Test-only reset coordinator; never used by production lifecycle. */
final class FrameworkTestReset {
    private FrameworkTestReset() {}

    static void reset(FrameworkLifecycleCoordinator lifecycle,
                      FrameworkScheduleBridge schedule,
                      UiOps ops) {
        SignalGroups.resetForTests();
        artframework.presentation.PresentationRuntime.resetSignalsForTests();
        schedule.resetForTests();
        artframework.ops.GateLab.resetForTests();
        lifecycle.clearForTests();
        ops.resetForTests();
        SyntheticRuntime.resetForTests();
        NativeTemplateRuntimeReset.reset();
        RenderHosts.resetForTests();
        Themes.resetForTests();
        artframework.core.PresentPackApply.resetForTests();
        artframework.core.PresentPackRuntime.resetForTests();
        artframework.core.PresentPacks.resetForTests();
        artframework.core.EnabledPresents.resetForTests();
        try {
            artframework.c1.host.EffectTargetActors.resetForTests();
        } catch (Throwable ignored) {
        }
        artframework.core.PresentProfiles.resetForTests();
        artframework.core.ProjectPresent.resetForTests();
        artframework.core.SurfacePresent.resetForTests();
        artframework.render.LightwaveControls.resetForTests();
        artframework.render.LightwaveDiagnostics.resetForTests();
        artframework.core.EffectPulse.resetForTests();
        artframework.core.NodeConnections.resetForTests();
        artframework.core.NodeStateMachines.resetForTests();
        artframework.core.UiActions.resetForTests();
        artframework.c1.skin.StsSkin.resetFontsForTests();
        HostBackends.resetForTests();
        UiNodeRegistry.global().resetBuiltinsForTests();
        C1NodeFactories.global().resetBuiltinsForTests();
        AnimationPlayers.resetForTests();
        SkeletonProviders.global().resetForTests();
        PresentSurfaces.resetForTests();
        PresentProjections.resetForTests();
        artframework.sts1.input.NativeInputRecords.resetForTests();
        artframework.sts1.input.Sts1MapIntentBridge.resetForTests();
        HostAssetsHolder.resetForTests();
        artframework.sts1.FullPresentMode.resetForTests();
        artframework.sts1.assets.Sts1HostAssets.resetForTests();
        artframework.sts1.render.Sts1RenderPipeline.resetForTests();
        artframework.sts1.backend.Sts1OrbStanceProjection.resetForTests();
        artframework.sts1.backend.Sts1RoomShellProjection.resetForTests();
        artframework.sts1.render.MapDrawPath.resetForTests();
        artframework.sts1.input.CombatInputRouter.resetForTests();
        artframework.sts1.audio.ArtAudioBridge.resetForTests();
        artframework.sts1.skeleton.Sts1SkeletonBridge.resetForTests();
        artframework.sts1.PresentSafety.resetForTests();
        artframework.inspect.UiLabListeners.resetForTests();
        PresentationRegistry.resetForTests();
    }

    private static final class NativeTemplateRuntimeReset {
        private static void reset() {
            artframework.c2.NativeTemplateRuntime.resetForTests();
        }
    }
}
