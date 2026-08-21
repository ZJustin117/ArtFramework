package artframework.api;

import artframework.component.NativeTemplateIds;

import artframework.c1.C1NodeFactories;
import artframework.c1.SyntheticRuntime;
import artframework.c1.layout.LayoutNode;
import artframework.c2.DefaultEntityPresent;
import artframework.c2.EntityPresent;
import artframework.c2.NativeTemplateRuntime;
import artframework.component.UiNode;
import artframework.component.UiNodeRegistry;
import artframework.component.WidgetSession;
import artframework.c2.NativeComponents;
import artframework.core.AnimationPlayer;
import artframework.core.AnimationPlayers;
import artframework.core.HostBackend;
import artframework.core.HostBackends;
import artframework.core.Theme;
import artframework.core.Themes;
import artframework.core.UiComponent;
import artframework.core.SignalGroup;
import artframework.core.SignalGroups;
import artframework.core.SignalDispatchResult;
import artframework.core.SignalListener;
import artframework.core.SignalSubscription;
import artframework.core.UiSignal;
import artframework.assets.HostAssets;
import artframework.assets.HostAssetsHolder;
import artframework.context.ContextFrame;
import artframework.context.FrameDiff;
import artframework.context.PresentProjection;
import artframework.context.PresentProjections;
import artframework.context.PresentSurfaces;
import artframework.ops.GateLab;
import artframework.ops.NativeOpsBackend;
import artframework.ops.NoOpNativeOps;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;

/**
 * Facade for dual-track windows. Prefer {@link #mount} / {@link #unmount}; {@link #open} /
 * {@link #bind} remain compatibility aliases. C1 SYNTHETIC → SyntheticRuntime; C2 NATIVE_TEMPLATE
 * → NativeTemplateRuntime + {@link UiComponent}. UiOps/UiProbe for commands/query.
 */
public final class ArtFramework {

    private static final UiOps OPS = new UiOps();
    private static final UiProbe PROBE = new UiProbe();
    private static final PresentationSchedule SCHEDULE = new PresentationSchedule();
    private static final FrameworkScheduleBridge SCHEDULE_BRIDGE =
            new FrameworkScheduleBridge(SCHEDULE);
    private static final FrameworkLifecycleCoordinator LIFECYCLE =
            new FrameworkLifecycleCoordinator(OPS);
    private static NativeOpsBackend nativeOpsBackend = NoOpNativeOps.INSTANCE;

    private ArtFramework() {}

    public static void register(WindowDef def) {
        if (def == null) {
            throw new IllegalArgumentException("def required");
        }
        LIFECYCLE.register(def);
    }

    public static boolean isRegistered(String id) {
        return LIFECYCLE.isRegistered(id);
    }

    /** Registered definition lookup for reversible host registration operations. */
    public static WindowDef registeredWindow(String id) {
        return LIFECYCLE.registeredWindow(id);
    }

    /** Remove a window def (PresentPack deactivate). Closes if open. */
    public static void unregisterWindow(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        LIFECYCLE.unregister(id);
    }

    /**
     * Mount a registered window (synthetic open or native bind). Preferred Godot-aligned entry.
     */
    public static WindowHandle mount(String id) {
        return LIFECYCLE.mount(id);
    }

    /** Unmount / close an open window or native template. */
    public static void unmount(String id) {
        LIFECYCLE.close(id);
    }

    public static WindowHandle open(String id) {
        return LIFECYCLE.open(id);
    }

    /**
     * Attach to an engine-owned native screen (C2). For SYNTHETIC, delegates to {@link #open}.
     */
    public static WindowHandle bind(String id) {
        return LIFECYCLE.bind(id);
    }

    public static WindowHandle find(String id) {
        return LIFECYCLE.find(id);
    }

    public static List<String> listOpenIds() {
        return LIFECYCLE.listOpenIds();
    }

    public static void close(String id) {
        LIFECYCLE.close(id);
    }

    public static void resetForTests() {
        FrameworkTestReset.reset(LIFECYCLE, SCHEDULE_BRIDGE, OPS);
        nativeOpsBackend = NoOpNativeOps.INSTANCE;
    }

    /** The default native operation group. */
    public static SignalGroup signals() {
        return SignalGroups.nativeGroup();
    }

    public static SignalSubscription connect(String name, SignalListener listener) {
        return signals().connect(name, listener);
    }

    public static SignalSubscription connect(Pattern pattern, SignalListener listener) {
        return signals().connect(pattern, listener);
    }

    public static SignalDispatchResult emit(UiSignal signal) {
        return dispatch(signal);
    }

    public static SignalDispatchResult dispatch(UiSignal signal) {
        return signals().dispatch(signal);
    }

    public static HostBackend host() {
        return HostBackends.get();
    }

    public static void setHostBackend(HostBackend backend) {
        HostBackends.set(backend);
    }

    /** Project fallback theme for trees without a present-profile node. */
    public static Theme theme() {
        return artframework.core.ProjectPresent.theme();
    }

    public static void setTheme(Theme theme) {
        if (theme == null) {
            artframework.core.ProjectPresent.set(artframework.core.PresentProfiles.STS);
            return;
        }
        String name = theme.name();
        if (name != null && !name.isEmpty() && artframework.core.PresentProfiles.get(name) != null) {
            artframework.core.ProjectPresent.set(name);
        } else {
            Themes.setDefault(theme);
            refreshDefaultSkinQuiet();
        }
    }

    /** Project fallback present profile id ({@code sts}, {@code lightwave}, …). */
    public static String presentProfile() {
        return artframework.core.ProjectPresent.id();
    }

    /**
     * Global present-profile (skin) catalog. Register resources here; apply with {@link
     * #setProjectPresent} / {@link #bindSurfacePresent}.
     */
    public static artframework.core.PresentProfileCatalog presentProfiles() {
        return artframework.core.PresentProfileCatalog.get();
    }

    /** Register a present profile resource (does not change project fallback). */
    public static void registerPresentProfile(artframework.core.PresentProfile profile) {
        artframework.core.PresentProfiles.register(profile);
    }

    public static void registerPresentProfile(String id, Theme theme) {
        artframework.core.PresentProfiles.register(new artframework.core.PresentProfile(id, theme));
    }

    public static void registerPresentProfile(String id, Theme theme, String packId) {
        artframework.core.PresentProfiles.register(
                new artframework.core.PresentProfile(
                        id,
                        theme,
                        artframework.core.PresentChromeStyle.fromTheme(theme),
                        packId != null ? packId : ""));
    }

    public static artframework.core.PresentProfile getPresentProfile(String id) {
        return artframework.core.PresentProfiles.get(id);
    }

    public static java.util.List<String> presentProfileIds() {
        return artframework.core.PresentProfiles.ids();
    }

    /**
     * Sets project fallback present and hot-restyles open project-fallback C1 windows (35.1).
     * Trees with node {@code present_profile} / {@code art.present_profile} override keep their
     * resolve; only from-project trees re-attach Stage.
     */
    public static void setPresentProfile(String id) {
        artframework.core.ProjectPresent.set(id);
    }

    public static void setProjectPresent(String id) {
        setPresentProfile(id);
    }

    public static String projectPresent() {
        return artframework.core.ProjectPresent.id();
    }

    public static artframework.core.PresentChromeStyle presentChrome() {
        return artframework.core.ProjectPresent.chrome();
    }

    public static artframework.core.PresentResolved resolvePresent(String windowId) {
        artframework.presentation.PresentationContext context =
                artframework.presentation.PresentationRuntime.context(windowId);
        return artframework.core.PresentResolve.forEntity(context,
                artframework.presentation.PresentationRuntime.root(context));
    }

    /** Bind a C2 full-present surface to a PresentProfile (35.2). */
    public static void bindSurfacePresent(String surfaceId, String profileId) {
        artframework.core.SurfacePresent.bind(surfaceId, profileId);
        artframework.core.PresentRestyle.applyPresentPack(
                artframework.core.SurfacePresent.resolve(surfaceId));
    }

    public static void unbindSurfacePresent(String surfaceId) {
        artframework.core.SurfacePresent.unbind(surfaceId);
    }

    public static artframework.core.PresentResolved resolveSurfacePresent(String surfaceId) {
        return artframework.core.SurfacePresent.resolve(surfaceId);
    }

    public static artframework.core.PresentChromeStyle surfaceChrome(String surfaceId) {
        return artframework.core.PresentResolve.chromeForSurface(surfaceId);
    }

    /** Force re-apply present cascade + Stage reattach for project-fallback windows. */
    public static void restyleOpenPresent() {
        artframework.core.PresentRestyle.onProjectPresentChanged();
    }

    /** Present UI packs (LML/JSON templates + windows). */
    public static void registerPresentPack(artframework.core.PresentPack pack) {
        artframework.core.PresentPacks.register(pack);
    }

    public static void registerPresentPackClasspath(String manifestResource) {
        artframework.core.PresentPacks.registerClasspath(manifestResource);
    }

    public static void activatePresentPack(String packId) {
        artframework.core.PresentPacks.activate(packId);
    }

    public static void deactivatePresentPack(String packId) {
        artframework.core.PresentPacks.deactivate(packId);
    }

    public static java.util.List<String> presentPackIds() {
        return artframework.core.PresentPacks.ids();
    }

    public static String activePresentPack() {
        return artframework.core.PresentPacks.activeId();
    }

    public static artframework.core.PresentPack getPresentPack(String id) {
        return artframework.core.PresentPacks.get(id);
    }

    /** Profile ids whose id matches regex (catalog). */
    public static java.util.List<String> presentIdsMatching(String regex) {
        return artframework.core.EnabledPresents.idsMatching(regex);
    }

    public static java.util.List<String> enabledPresentIds() {
        return artframework.core.EnabledPresents.enabledIds();
    }

    public static void setEnabledPresentProfiles(java.util.List<String> ids) {
        artframework.core.EnabledPresents.setEnabled(ids);
    }

    public static void clearEnabledPresentRestriction() {
        artframework.core.EnabledPresents.clearRestriction();
    }

    /** Enable/disable all catalog profiles matching regex. */
    public static int modifyPresentsMatching(String regex, boolean enable) {
        return artframework.core.EnabledPresents.modifyMatching(regex, enable);
    }

    /** Select first enabled profile matching regex (applies project + pack). */
    public static String selectPresentMatching(String regex) {
        return artframework.core.EnabledPresents.selectMatching(regex);
    }

    /**
     * Set packId on every profile matching regex; optional select first match.
     *
     * @return number of profiles updated
     */
    public static int modifyPresentPackIdMatching(
            String regex, String newPackId, boolean selectFirst) {
        return artframework.core.EnabledPresents.modifyProfilesMatching(
                regex, newPackId, selectFirst);
    }

    public static java.util.List<String> presentPackIdsMatching(String regex) {
        return artframework.core.PresentPacks.idsMatching(regex);
    }

    private static void refreshDefaultSkinQuiet() {
        try {
            artframework.c1.host.StageHost host = artframework.c1.host.StageHost.get();
            if (host != null) {
                host.refreshDefaultSkin();
            }
        } catch (Throwable ignored) {
        }
    }

    /** Imperative UI commands (C1 + C2). */
    public static UiOps ops() {
        return OPS;
    }

    /** Read-only UI snapshot. */
    public static UiProbe probe() {
        return PROBE;
    }

    public static void setNativeOpsBackend(NativeOpsBackend backend) {
        nativeOpsBackend = backend != null ? backend : NoOpNativeOps.INSTANCE;
    }

    public static NativeOpsBackend nativeOpsBackend() {
        return nativeOpsBackend;
    }

    /**
     * Layout root for an open synthetic window, or null if not synthetic / not tracked.
     */
    public static LayoutNode layoutRoot(String id) {
        WindowHandle h = find(id);
        return LIFECYCLE.layoutRoot(id);
    }

    /**
     * Composition tree for an open synthetic window (expanded), or null.
     */
    public static UiNode uiRoot(String id) {
        return artframework.presentation.PresentationRuntime.declaration(
                artframework.presentation.PresentationRuntime.context(id));
    }

    /** Widget session (sliders / control index) for an open synthetic window. */
    public static WidgetSession widgets(String id) {
        UiNode root = uiRoot(id);
        return root != null ? new WidgetSession(id, root) : null;
    }


    /** Advance all mounted synthetic trees and the configured host. */
    public static void tick(float deltaSeconds) {
        advanceFrame(deltaSeconds, null);
    }

    /** Advance one production presentation frame, optionally ingesting an authority snapshot first. */
    public static void advanceFrame(float deltaSeconds, ContextFrame authorityFrame) {
        SCHEDULE_BRIDGE.advance(deltaSeconds, authorityFrame);
    }

    /** Internal compatibility bridge to the schedule-owned surface command phase. */
    public static void executeSurfaceIntents() {
        SCHEDULE_BRIDGE.executeSurfaceIntents();
    }

    /** Internal compatibility bridge to the schedule-owned surface lifecycle command. */
    public static void executeSurfaceLifecycle() {
        SCHEDULE_BRIDGE.executeSurfaceLifecycle();
    }

    /** Internal compatibility bridge for synchronous native host hooks. */
    public static void processNativeIntentLifecycle() {
        SCHEDULE_BRIDGE.processNativeIntentLifecycle();
    }

    /** Internal compatibility bridge for pulse-only helpers. */
    public static void executeEffectPulses(float deltaSeconds) {
        SCHEDULE_BRIDGE.executeEffectPulses(deltaSeconds);
    }

    /** Install the host-specific presentation phase used by the production frame schedule. */
    public static void setHostPresentationSystem(HostPresentationSystem system) {
        SCHEDULE_BRIDGE.setHostPresentationSystem(system);
    }

    /**
     * C2 / full-present / synthetic component by id.
     * <ul>
     *   <li>Mounted full-present wins over native (same id, e.g. map).
     *   <li>Mounted native wins when present is not mounted (legacy bind path).
     *   <li>If neither mounted, prefer full-present so {@code component(id).mount()} targets
     *       PresentSurfaces; legacy code should {@code bind()} which mounts native via runtime.
     * </ul>
     */
    public static UiComponent component(String componentId) {
        UiComponent synthetic = artframework.c1.SyntheticComponents.get(componentId);
        if (synthetic != null) {
            return synthetic;
        }
        UiComponent present = PresentSurfaces.get(componentId);
        UiComponent nativeC = NativeComponents.get(componentId);
        if (present != null && present.isMounted()) {
            return present;
        }
        if (nativeC != null && nativeC.isMounted()) {
            return nativeC;
        }
        if (present != null) {
            return present;
        }
        return nativeC;
    }

    public static PresentProjection projection() {
        return PresentProjections.get();
    }

    /** Publishes an ordinary context-frame signal. */
    public static FrameDiff publishFrame(ContextFrame frame) {
        return SCHEDULE_BRIDGE.publishFrame(frame);
    }

    /** Host-managed unified asset library. */
    public static HostAssets assets() {
        return HostAssetsHolder.get();
    }

    /** Track-agnostic render attach (effects / targets). */
    public static RenderHost render() {
        return RenderHosts.get();
    }

    /** Registered presentation node types (builtins + third-party). */
    public static UiNodeRegistry nodes() {
        return UiNodeRegistry.global();
    }

    /** C1 scene2d node factories (builtins + third-party). */
    public static C1NodeFactories c1Nodes() {
        return C1NodeFactories.global();
    }

    /** Animation player for a mounted {@code art.animation_player} node, or null. */
    public static AnimationPlayer animation(String windowId, String nodeId) {
        return AnimationPlayers.get(windowId, nodeId);
    }

    /** Register a UI action id for declarative {@code connections} (and Java invoke). */
    public static void registerUiAction(String id, artframework.core.UiAction action) {
        artframework.core.UiActions.register(id, action);
    }

    /** Lookup registered UI action, or null. */
    public static artframework.core.UiAction getUiAction(String id) {
        return artframework.core.UiActions.get(id);
    }

    /** Registered UI action ids (builtins + third-party). */
    public static java.util.List<String> uiActionIds() {
        return artframework.core.UiActions.ids();
    }

    /** Node state machine from {@code states} decl, or null. */
    public static artframework.core.NodeStateMachine nodeState(
            String windowId, String nodeId) {
        return artframework.core.NodeStateMachines.get(windowId, nodeId);
    }

    /** Skeleton provider registry. */
    public static artframework.skeleton.SkeletonProviders skeletons() {
        return artframework.skeleton.SkeletonProviders.global();
    }

    /** C2 entity presenter slots (attach/sync/layout/detach). */
    public static EntityPresent entities() {
        return NativeTemplateRuntime.entities();
    }

    /** Same as {@link #entities()} with listener registration helpers. */
    public static DefaultEntityPresent entityPresent() {
        return NativeTemplateRuntime.entities();
    }

}
