package artframework.c1.host;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import artframework.render.RenderHosts;

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
            skin = StsSkin.create();
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
            ArtFramework.frames().syncFromBackend();
        } catch (Throwable t) {
            try {
                BaseMod.logger.warn("ArtFramework frame sync skipped: " + t.getMessage());
            } catch (Throwable ignored) {
            }
        }
        RenderHosts.get().tick(dt);
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
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (!ready) {
            return;
        }
        boolean hasStage = stage != null && !actors.isEmpty();
        boolean hasFx = RenderHosts.get().bindingCount() > 0 || RenderHosts.get().targetCount() > 0;
        if (!hasStage && !hasFx) {
            return;
        }
        // End batch so default FB is complete, then copy screen for glass/blur, then draw FX.
        sb.end();
        if (hasStage) {
            // Stage draws after capture so glass samples the *game* scene, not our UI chrome.
            // Capture first while only game content is on screen.
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
            stage.draw();
        } else if (RenderHosts.get().needsCapture()) {
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
        // C2 full-present surfaces draw after native world/UI rendering. The matching native
        // render patch suppresses only the enabled surface, so this has a safe native fallback.
        artframework.sts1.render.Sts1SurfaceRenderer.render(sb);
        RenderHosts.get().drawFrame(sb, true);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void attach(String id, LayoutNode root) {
        if (!ready || stage == null || skin == null) {
            return;
        }
        if (id == null || root == null) {
            throw new IllegalArgumentException("id and root required");
        }
        detach(id);
        final String windowId = id;
        Actor actor = LayoutActors.toActor(windowId, root, skin, new Runnable() {
            @Override
            public void run() {
                ArtFramework.close(windowId);
            }
        });
        actors.put(id, actor);
        stage.addActor(actor);
        captureInput();
    }

    @Override
    public void attachComposition(String id, UiNode root) {
        if (!ready || stage == null || skin == null) {
            return;
        }
        if (id == null || root == null) {
            throw new IllegalArgumentException("id and root required");
        }
        detach(id);
        final String windowId = id;
        Actor actor = ComponentActors.toActor(windowId, root, skin, new Runnable() {
            @Override
            public void run() {
                ArtFramework.close(windowId);
            }
        });
        actors.put(id, actor);
        stage.addActor(actor);
        captureInput();
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
