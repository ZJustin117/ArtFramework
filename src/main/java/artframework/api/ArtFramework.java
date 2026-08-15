package artframework.api;

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

    private static final Map<String, WindowDef> DEFS = new LinkedHashMap<String, WindowDef>();
    private static final Map<String, WindowHandle> OPEN = new LinkedHashMap<String, WindowHandle>();
    private static final UiOps OPS = new UiOps();
    private static final UiProbe PROBE = new UiProbe();
    private static final PresentationSchedule SCHEDULE = new PresentationSchedule();
    private static NativeOpsBackend nativeOpsBackend = NoOpNativeOps.INSTANCE;

    private ArtFramework() {}

    public static void register(WindowDef def) {
        if (def == null) {
            throw new IllegalArgumentException("def required");
        }
        DEFS.put(def.id, def);
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            String canon = artframework.c2.NativeTemplateIds.canonicalize(def.id);
            if (canon != null && !canon.equals(def.id)) {
                DEFS.put(canon, def);
            }
            String resCanon = artframework.c2.NativeTemplateIds.canonicalize(def.resource);
            if (resCanon != null && !resCanon.isEmpty() && !resCanon.equals(def.id) && !resCanon.equals(canon)) {
                DEFS.put(resCanon, def);
            }
        }
    }

    public static boolean isRegistered(String id) {
        if (DEFS.containsKey(id)) {
            return true;
        }
        String canon = artframework.c2.NativeTemplateIds.canonicalize(id);
        return canon != null && DEFS.containsKey(canon);
    }

    /** Remove a window def (PresentPack deactivate). Closes if open. */
    public static void unregisterWindow(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        close(id);
        DEFS.remove(id);
    }

    /**
     * Mount a registered window (synthetic open or native bind). Preferred Godot-aligned entry.
     */
    public static WindowHandle mount(String id) {
        WindowDef def = defOf(id);
        if (def == null) {
            throw new IllegalArgumentException("not registered: " + id);
        }
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            return bind(id);
        }
        return openSynthetic(def);
    }

    /** Unmount / close an open window or native template. */
    public static void unmount(String id) {
        close(id);
    }

    public static WindowHandle open(String id) {
        WindowDef def = defOf(id);
        if (def == null) {
            throw new IllegalArgumentException("not registered: " + id);
        }
        if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
            return bind(id);
        }
        return openSynthetic(def);
    }

    private static WindowHandle openSynthetic(WindowDef def) {
        WindowHandle previous = OPEN.get(def.id);
        if (previous != null && previous.isOpen()) {
            previous.close();
        }
        LayoutNode root = SyntheticRuntime.open(def);
        WindowHandle handle = new TrackedHandle(def, root);
        OPEN.put(def.id, handle);
        artframework.presentation.PresentationContext context =
                artframework.presentation.PresentationRuntime.context(def.id);
        artframework.ecs.EntityId rootEntity = artframework.presentation.PresentationRuntime.root(context);
        if (context != null && rootEntity != null) {
            OPS.onTreeMounted(def.id);
            HostBackends.get().attach(new artframework.presentation.PresentationMount(context, rootEntity));
        }
        return handle;
    }

    /**
     * Attach to an engine-owned native screen (C2). For SYNTHETIC, delegates to {@link #open}.
     */
    private static WindowDef defOf(String id) {
        if (id == null) {
            return null;
        }
        WindowDef def = DEFS.get(id);
        if (def != null) {
            return def;
        }
        return DEFS.get(artframework.c2.NativeTemplateIds.canonicalize(id));
    }

    public static WindowHandle bind(String id) {
        WindowDef def = defOf(id);
        if (def == null) {
            throw new IllegalArgumentException("not registered: " + id);
        }
        if (def.windowClass == WindowClass.SYNTHETIC) {
            return open(id);
        }
        String openId = artframework.c2.NativeTemplateIds.canonicalize(def.resource);
        if (openId == null || openId.isEmpty()) {
            openId = artframework.c2.NativeTemplateIds.canonicalize(def.id);
        }
        if (openId == null || openId.isEmpty()) {
            openId = def.id;
        }
        WindowHandle previous = OPEN.get(openId);
        if (previous != null && previous.isOpen()) {
            previous.close();
        }
        previous = OPEN.get(id);
        if (previous != null && previous.isOpen()) {
            previous.close();
        }
        NativeTemplateRuntime.bind(def);
        WindowHandle handle = new TrackedHandle(def, null);
        OPEN.put(openId, handle);
        if (!openId.equals(id)) {
            OPEN.put(id, handle);
        }
        return handle;
    }

    public static WindowHandle find(String id) {
        WindowHandle h = OPEN.get(id);
        if (h != null) {
            return h;
        }
        return OPEN.get(artframework.c2.NativeTemplateIds.canonicalize(id));
    }

    public static List<String> listOpenIds() {
        List<String> ids = new ArrayList<String>(
                artframework.presentation.PresentationRuntime.openWindowIds());
        for (String id : NativeTemplateRuntime.boundIds()) {
            if (!ids.contains(id)) ids.add(id);
        }
        return Collections.unmodifiableList(ids);
    }

    public static void close(String id) {
        WindowHandle h = OPEN.get(id);
        if (h != null) {
            h.close();
        }
    }

    public static void resetForTests() {
        artframework.core.SignalGroups.resetForTests();
        SCHEDULE.resetForTests();
        GateLab.resetForTests();
        OPEN.clear();
        DEFS.clear();
        OPS.resetForTests();
        nativeOpsBackend = NoOpNativeOps.INSTANCE;
        SyntheticRuntime.resetForTests();
        NativeTemplateRuntime.resetForTests();
        RenderHosts.resetForTests();
        Themes.resetForTests();
        artframework.core.PresentPackApply.resetForTests();
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
        artframework.skeleton.SkeletonProviders.global().resetForTests();
        PresentSurfaces.resetForTests();
        PresentProjections.resetForTests();
        HostAssetsHolder.resetForTests();
        artframework.sts1.FullPresentMode.resetForTests();
        artframework.sts1.assets.Sts1HostAssets.resetForTests();
        artframework.sts1.render.Sts1RenderPipeline.resetForTests();
        artframework.sts1.render.MapDrawPath.resetForTests();
        artframework.sts1.input.CombatInputRouter.resetForTests();
        artframework.sts1.audio.ArtAudioBridge.resetForTests();
        artframework.sts1.skeleton.Sts1SkeletonBridge.resetForTests();
        artframework.sts1.PresentSafety.resetForTests();
        artframework.inspect.UiLabListeners.resetForTests();
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
        return signals().emit(signal);
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
        WindowHandle h = OPEN.get(id);
        if (h instanceof TrackedHandle) {
            return ((TrackedHandle) h).root;
        }
        return null;
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
        SCHEDULE.advance(deltaSeconds, authorityFrame);
    }

    /** Install the host-specific presentation phase used by the production frame schedule. */
    public static void setHostPresentationSystem(HostPresentationSystem system) {
        SCHEDULE.setHostPresentationSystem(system);
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
        return PresentProjections.publish(frame);
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

    private static final class TrackedHandle implements WindowHandle {
        private final WindowDef def;
        private final LayoutNode root;
        private final String openId;
        private boolean open = true;

        TrackedHandle(WindowDef def, LayoutNode root) {
            this.def = def;
            this.root = root;
            String id = def.id;
            if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
                String nativeId = artframework.c2.NativeTemplateIds.canonicalize(def.resource);
                if (nativeId != null && !nativeId.isEmpty()) {
                    id = nativeId;
                }
            }
            this.openId = id;
        }

        @Override
        public String id() {
            return def.id;
        }

        @Override
        public WindowClass windowClass() {
            return def.windowClass;
        }

        @Override
        public boolean isOpen() {
            if (!open) return false;
            if (def.windowClass == WindowClass.SYNTHETIC) {
                return artframework.presentation.PresentationRuntime.isOpen(def.id);
            }
            return NativeTemplateRuntime.isBound(openId);
        }

        @Override
        public void close() {
            if (!open) {
                return;
            }
            open = false;
            removeHandleAliases(this);
            if (def.windowClass == WindowClass.SYNTHETIC) {
                artframework.presentation.PresentationContext context =
                        artframework.presentation.PresentationRuntime.context(def.id);
                artframework.ecs.EntityId root = artframework.presentation.PresentationRuntime.root(context);
                if (context != null && root != null) HostBackends.get().detach(
                        new artframework.presentation.PresentationMount(context, root));
                SyntheticRuntime.onClosed(def.id);
            } else if (def.windowClass == WindowClass.NATIVE_TEMPLATE) {
                NativeTemplateRuntime.unbind(def);
            }
        }

        private void removeHandleAliases(WindowHandle handle) {
            List<String> aliases = new ArrayList<String>();
            for (Map.Entry<String, WindowHandle> entry : OPEN.entrySet()) {
                if (entry.getValue() == handle) {
                    aliases.add(entry.getKey());
                }
            }
            for (String alias : aliases) {
                OPEN.remove(alias);
            }
        }
    }
}
