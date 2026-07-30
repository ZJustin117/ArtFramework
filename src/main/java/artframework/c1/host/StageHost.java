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
            BaseMod.logger.error("ArtFramework StageHost init failed", e);
        }
    }

    @Override
    public void receivePostUpdate() {
        if (!ready || stage == null) {
            return;
        }
        float dt = Gdx.graphics != null ? Gdx.graphics.getDeltaTime() : 0f;
        // The STS1 backend is observational until an individual full-present surface is enabled.
        // Snapshot after native update so ART sees a coherent authority frame for this render pass.
        try {
            ArtFramework.publishFrame(
                    artframework.sts1.backend.Sts1PresentationBackend.INSTANCE.publishFrame());
        } catch (Throwable t) {
            try {
                BaseMod.logger.warn("ArtFramework frame sync skipped: " + t.getMessage());
            } catch (Throwable ignored) {
            }
        }
        try {
            artframework.sts1.lab.LabRecipeRunner.tick();
        } catch (Throwable ignored) {
        }
        RenderHosts.get().tick(dt);
        try {
            ArtFramework.tick(dt);
            artframework.render.LightwaveControls.tickPulses(dt);
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
        syncActorPropsFromTree();
        // LayoutEngine targets are local (0,0); scene2d windows are centered/draggable — realign FX.
        syncEffectTargetBounds();
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (!ready) {
            return;
        }
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
        syncEffectTargetBounds();
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
        syncEffectTargetBounds();
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
        // Active pulse owns intensity/phase; do not overwrite from tree each frame.
        // Enter animation still uses applyIntensity via AnimationPlayer → prop → here only when idle.
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
     * Map RenderHost c1 targets from pure LayoutEngine space onto live stage actor bounds.
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
            syncNamedActors(host, winId, root, tmp);
        }
    }

    private static void syncNamedActors(RenderHost host, String winId, Actor actor, Vector2 tmp) {
        if (actor == null) {
            return;
        }
        String name = null;
        try {
            name = actor.getName();
        } catch (Throwable ignored) {
        }
        if (name != null && !name.isEmpty()) {
            updateTargetFromActor(host, "c1:" + winId + ":" + name, actor, tmp);
        }
        if (actor instanceof Group) {
            Group g = (Group) actor;
            for (Actor child : g.getChildren()) {
                syncNamedActors(host, winId, child, tmp);
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
            tmp.set(0f, 0f);
            actor.localToStageCoordinates(tmp);
            float w = actor.getWidth();
            float h = actor.getHeight();
            if (w > 0f && h > 0f) {
                t.setBounds(tmp.x, tmp.y, w, h);
            }
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
