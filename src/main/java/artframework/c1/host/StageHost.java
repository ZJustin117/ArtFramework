package artframework.c1.host;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import artframework.api.ArtFramework;
import artframework.c1.StageBackend;
import artframework.c1.SyntheticRuntime;
import artframework.c1.layout.ComponentActors;
import artframework.c1.layout.LayoutActors;
import artframework.c1.layout.LayoutNode;
import artframework.c1.skin.StsSkin;
import artframework.component.UiNode;
import artframework.core.Theme;
import artframework.core.Themes;
import artframework.core.UiInstance;
import artframework.core.UiTree;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;
import artframework.render.RenderTarget;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BaseMod-driven scene2d host: PostInitialize builds Stage+StsSkin; PostUpdate act;
 * PostRender draw + effect frame. Modal input while any synthetic window is attached.
 * No SpirePatch — uses BaseMod publish hooks only.
 */
public final class StageHost
        implements StageBackend, PostInitializeSubscriber, PostUpdateSubscriber, PostRenderSubscriber {

    private static StageHost instance;

    private Stage stage;
    private Skin skin;
    private boolean ready;
    private int probeSidecarTicks;
    private boolean probeSidecarWarned;
    private final Map<String, Actor> actors = new LinkedHashMap<String, Actor>();
    private InputProcessor previousInput;
    private boolean inputCaptured;

    private StageHost() {}

    public static StageHost install() {
        if (instance == null) {
            instance = new StageHost();
            BaseMod.subscribe(instance);
        }
        return instance;
    }

    public static StageHost get() {
        return instance;
    }

    /** Test / shutdown helper — not for normal game flow. */
    public static void resetInstanceForTests() {
        if (instance != null) {
            instance.releaseInput();
            instance.actors.clear();
            instance.ready = false;
            artframework.sts1.StsRuntimeReady.setReady(false);
            artframework.sts1.StsRuntimeReady.setStarted(false);
            instance.stage = null;
            instance.skin = null;
        }
        instance = null;
    }

    @Override
    public void receivePostInitialize() {
        try {
            skin = StsSkin.create(Themes.getDefault());
            stage = new Stage(new ScreenViewport());
            ready = true;
            artframework.sts1.StsRuntimeReady.setReady(true);
            SyntheticRuntime.installStageBackend(this);
            int shaders = 0;
            try {
                shaders = RenderHosts.get().compileShaders();
            } catch (Throwable t) {
                BaseMod.logger.warn("ArtFramework shader compile skipped: " + t.getMessage());
            }
            BaseMod.logger.info(
                    "ArtFramework StageHost ready (StsSkin + Stage; shadersCompiled=" + shaders + ")");
        } catch (RuntimeException e) {
            ready = false;
            artframework.sts1.StsRuntimeReady.setReady(false);
            BaseMod.logger.error("ArtFramework StageHost init failed", e);
        }
    }

    @Override
    public void receivePostUpdate() {
        if (!ready || stage == null) {
            return;
        }
        float dt = Gdx.graphics != null ? Gdx.graphics.getDeltaTime() : 0f;
        try {
            artframework.sts1.skeleton.Sts1SkeletonBridge.tick(dt);
        } catch (Throwable t) {
            try { BaseMod.logger.warn("ArtFramework skeleton tick skipped: " + t.getMessage()); }
            catch (Throwable ignored) { }
        }
        // The STS1 backend is observational until an individual full-present surface is enabled.
        // Snapshot after native update so ART sees a coherent authority frame for this render pass.
        // Native STS remains the owner until a C2 surface is mounted or lab observation is
        // requested. Avoid rebuilding the full authority snapshot on every native-only frame.
        if (artframework.context.PresentSurfaces.anyMounted()
                || artframework.sts1.render.Sts1RenderPipeline.isOverlayObserve()) {
            try {
                ArtFramework.publishFrame(
                        artframework.sts1.backend.Sts1PresentationBackend.INSTANCE.publishFrame());
            } catch (Throwable t) {
                try {
                    BaseMod.logger.warn("ArtFramework frame sync skipped: " + t.getMessage());
                } catch (Throwable ignored) {
                }
            }
        }
        try {
            artframework.sts1.lab.LabRecipeRunner.tick();
        } catch (Throwable ignored) {
        }
        writeProbeSidecarOnInterval();
        RenderHosts.get().tick(dt);
        try {
            ArtFramework.tick(dt);
            // EffectPulse advances inside ArtFramework.tick (do not double-tick here).
        } catch (Throwable ignored) {
        }
        if (Gdx.graphics != null) {
            // Always track screen size for capture UV mapping
            RenderHosts.get()
                    .syncScreenBounds(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        if (actors.isEmpty()) {
            return;
        }
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        stage.act(dt);
        // Force table/window layout so child getWidth/Height are non-zero before FX sync.
        layoutAndSyncFx();
        syncActorPropsFromTree();
    }

    private void writeProbeSidecarOnInterval() {
        probeSidecarTicks++;
        if (probeSidecarTicks < 30) {
            return;
        }
        probeSidecarTicks = 0;
        try {
            Gdx.files.local("art_probe_latest.log")
                    .writeString(ArtFramework.probe().toJsonLine() + "\n", false, "UTF-8");
        } catch (Throwable t) {
            if (!probeSidecarWarned) {
                probeSidecarWarned = true;
                try {
                    BaseMod.logger.warn("ArtFramework probe sidecar skipped: " + t.getMessage());
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (!ready) {
            return;
        }
        writeProbeSidecarOnInterval();
        boolean hasStage = stage != null && !actors.isEmpty();
        boolean hasFx = RenderHosts.get().bindingCount() > 0 || RenderHosts.get().targetCount() > 0;
        boolean hasPresentDraw = !artframework.sts1.render.Sts1RenderPipeline.plan().drawOrder().isEmpty();
        if (!hasStage && !hasFx && !hasPresentDraw) {
            return;
        }
        // End batch: capture game FB, draw C1 FX *under* scene2d (so labels stay readable),
        // then stage UI, then C2 + full-frame overlays.
        sb.end();
        if (RenderHosts.get().needsCapture()) {
            try {
                RenderHosts.get()
                        .frameCapture()
                        .captureScreen(
                                (int) RenderHosts.get().screenWidth(),
                                (int) RenderHosts.get().screenHeight());
            } catch (Throwable ignored) {
            }
        }
        sb.begin();
        // Band under UI text; white border after stage so panel bg does not cover it.
        RenderHosts.get().drawFrame(sb, true, artframework.render.RenderHost.kindsC1UnderUi());
        sb.end();
        if (hasStage) {
            stage.draw();
        }
        sb.begin();
        RenderHosts.get().drawC1LightwaveBorders(sb);
        // C2 bands are placed below ART's own C2 chrome, matching C1's readable-label ordering.
        RenderHosts.get().drawFrame(
                sb, true, artframework.render.RenderHost.kindsC2UnderPresent());
        artframework.sts1.render.Sts1SurfaceRenderer.render(sb);
        RenderHosts.get().drawFrame(sb, true, artframework.render.RenderHost.kindsOverUi());
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void attach(String id, LayoutNode root) {
        if (!ready || stage == null) {
            return;
        }
        if (id == null || root == null) {
            throw new IllegalArgumentException("id and root required");
        }
        detach(id);
        final String windowId = id;
        Skin useSkin = skinForWindow(windowId);
        if (useSkin == null) {
            return;
        }
        Actor actor = LayoutActors.toActor(windowId, root, useSkin, new Runnable() {
            @Override
            public void run() {
                ArtFramework.close(windowId);
            }
        });
        actors.put(id, actor);
        stage.addActor(actor);
        captureInput();
        layoutAndSyncFx();
    }

    @Override
    public void attachComposition(String id, UiNode root) {
        if (!ready || stage == null) {
            return;
        }
        if (id == null || root == null) {
            throw new IllegalArgumentException("id and root required");
        }
        detach(id);
        final String windowId = id;
        Skin useSkin = skinForWindow(windowId);
        if (useSkin == null) {
            return;
        }
        Actor actor = ComponentActors.toActor(windowId, root, useSkin, new Runnable() {
            @Override
            public void run() {
                ArtFramework.close(windowId);
            }
        });
        actors.put(id, actor);
        stage.addActor(actor);
        captureInput();
        layoutAndSyncFx();
    }

    /** Validate scene2d layout then map all C1 effect targets to stage coordinates. */
    private void layoutAndSyncFx() {
        for (Actor root : actors.values()) {
            layoutActorTree(root);
        }
        syncEffectTargetBounds();
    }

    private static void layoutActorTree(Actor root) {
        if (root == null) {
            return;
        }
        try {
            if (root instanceof com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup) {
                com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup wg =
                        (com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup) root;
                wg.invalidateHierarchy();
                wg.validate();
            } else if (root instanceof com.badlogic.gdx.scenes.scene2d.ui.Widget) {
                ((com.badlogic.gdx.scenes.scene2d.ui.Widget) root).validate();
            }
        } catch (Throwable ignored) {
        }
        if (root instanceof Group) {
            for (Actor child : ((Group) root).getChildren()) {
                layoutActorTree(child);
            }
        }
    }

    /**
     * Prefer resolved present theme on the mounted tree, else project fallback.
     * Builds a fresh Skin so Lightwave tokens actually paint (not the PostInitialize STS skin).
     */
    private Skin skinForWindow(String windowId) {
        Theme theme = artframework.core.ProjectPresent.theme();
        try {
            UiTree tree = ArtFramework.tree(windowId);
            if (tree != null) {
                theme = artframework.core.PresentResolve.themeFor(tree);
            }
        } catch (Throwable ignored) {
        }
        try {
            return StsSkin.create(theme);
        } catch (Throwable t) {
            return skin != null ? skin : null;
        }
    }

    /** Push UiInstance props (e.g. opacity from animation_player) onto named stage actors. */
    private void syncActorPropsFromTree() {
        for (Map.Entry<String, Actor> e : actors.entrySet()) {
            String winId = e.getKey();
            Actor root = e.getValue();
            if (root == null) {
                continue;
            }
            UiTree tree = null;
            try {
                tree = ArtFramework.tree(winId);
            } catch (Throwable ignored) {
            }
            if (tree == null) {
                continue;
            }
            syncNamedActorProps(tree, root);
        }
    }

    private static void syncNamedActorProps(UiTree tree, Actor actor) {
        if (actor == null || tree == null) {
            return;
        }
        String name = null;
        try {
            name = actor.getName();
        } catch (Throwable ignored) {
        }
        if (name != null && !name.isEmpty()) {
            UiInstance inst = tree.get(name);
            if (inst != null) {
                // Drive lightwave intensity from anim/slider props — do NOT setColor on Groups
                // (multiplies child Label/TextButton glyphs and looks like missing letters).
                syncFxIntensity(tree.windowId(), name, inst);
            }
        }
        if (actor instanceof Group) {
            for (Actor child : ((Group) actor).getChildren()) {
                syncNamedActorProps(tree, child);
            }
        }
    }

    private static void syncFxIntensity(String windowId, String nodeId, UiInstance inst) {
        // Ambient intensity only — pulse overlay is a separate binding layer.
        if (artframework.core.EffectPulse.isActive(windowId, null)) {
            return;
        }
        Object raw = inst.prop("fx_intensity");
        if (!(raw instanceof Number)) {
            return;
        }
        float v = ((Number) raw).floatValue();
        try {
            artframework.render.LightwaveControls.applyIntensity(windowId, "panel", v);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Map C1 FX targets to stage space from {@link EffectTargetActors} (inflate-time registry).
     * Never use LayoutEngine Y-order for widgets — it disagrees with scene2d Table + title row
     * (Hello lightwave sat on the title).
     */
    private void syncEffectTargetBounds() {
        if (stage == null || actors.isEmpty()) {
            return;
        }
        RenderHost host = RenderHosts.get();
        Vector2 tmp = new Vector2();
        for (Map.Entry<String, Actor> e : actors.entrySet()) {
            String winId = e.getKey();
            Actor root = e.getValue();
            if (root == null) {
                continue;
            }
            updateTargetFromActor(host, "c1:" + winId, root, tmp);
            java.util.Map<String, Actor> mapped = EffectTargetActors.entriesForWindow(winId);
            for (Map.Entry<String, Actor> me : mapped.entrySet()) {
                String effectKey = me.getKey();
                Actor a = me.getValue();
                if (effectKey == null || a == null) {
                    continue;
                }
                try {
                    if (a.getStage() == null) {
                        continue;
                    }
                } catch (Throwable ignored) {
                }
                // Title uses dedicated key; still sync so pack can frame it.
                updateTargetFromActor(host, "c1:" + winId + ":" + effectKey, a, tmp);
            }
        }
    }

    private static void updateTargetFromActor(
            RenderHost host, String targetId, Actor actor, Vector2 tmp) {
        if (host == null || targetId == null || actor == null) {
            return;
        }
        RenderTarget t = host.getTarget(targetId);
        if (t == null) {
            return;
        }
        try {
            float w = actor.getWidth();
            float h = actor.getHeight();
            if (w < 1f) {
                w = 32f;
            }
            if (h < 1f) {
                h = 28f;
            }
            // Labels: prefer pref width but pad so the lightwave frame is readable (not hairline).
            try {
                if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.Widget) {
                    com.badlogic.gdx.scenes.scene2d.ui.Widget wg =
                            (com.badlogic.gdx.scenes.scene2d.ui.Widget) actor;
                    float pw = wg.getPrefWidth();
                    float ph = wg.getPrefHeight();
                    if (pw > 8f && pw < w) {
                        w = Math.max(pw + 24f, 96f);
                    }
                    if (ph > 8f) {
                        h = Math.max(ph + 12f, 32f);
                    }
                }
            } catch (Throwable ignored) {
            }
            tmp.set(0f, 0f);
            actor.localToStageCoordinates(tmp);
            float x0 = tmp.x;
            float y0 = tmp.y;
            tmp.set(w, h);
            actor.localToStageCoordinates(tmp);
            float x1 = tmp.x;
            float y1 = tmp.y;
            float x = Math.min(x0, x1);
            float y = Math.min(y0, y1);
            float bw = Math.abs(x1 - x0);
            float bh = Math.abs(y1 - y0);
            if (bw < 1f) {
                bw = w;
            }
            if (bh < 1f) {
                bh = h;
            }
            t.setBounds(x, y, bw, bh);
        } catch (Throwable ignored) {
        }
    }

    /** Rebuild process default skin after profile switch (open windows keep their attach skin). */
    public void refreshDefaultSkin() {
        if (!ready) {
            return;
        }
        try {
            skin = StsSkin.create(Themes.getDefault());
        } catch (Throwable t) {
            try {
                BaseMod.logger.warn("ArtFramework skin refresh skipped: " + t.getMessage());
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void detach(String id) {
        EffectTargetActors.clearWindow(id);
        Actor actor = actors.remove(id);
        if (actor != null) {
            actor.remove();
        }
        if (actors.isEmpty()) {
            releaseInput();
        }
    }

    @Override
    public boolean isAttached(String id) {
        return actors.containsKey(id);
    }

    @Override
    public int attachedCount() {
        return actors.size();
    }

    private void captureInput() {
        if (inputCaptured || Gdx.input == null) {
            return;
        }
        previousInput = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(stage);
        inputCaptured = true;
    }

    private void releaseInput() {
        if (!inputCaptured) {
            return;
        }
        if (Gdx.input != null) {
            Gdx.input.setInputProcessor(previousInput);
        }
        previousInput = null;
        inputCaptured = false;
        try {
            InputHelper.regainInputFocus();
        } catch (Throwable ignored) {
        }
    }
}
