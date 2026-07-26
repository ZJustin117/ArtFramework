package spireui.c1.host;

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
import spireui.api.SpireUI;
import spireui.c1.StageBackend;
import spireui.c1.SyntheticRuntime;
import spireui.c1.layout.LayoutActors;
import spireui.c1.layout.LayoutNode;
import spireui.c1.skin.StsSkin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BaseMod-driven scene2d host: PostInitialize builds Stage+StsSkin; PostUpdate act;
 * PostRender draw. Modal input capture while any synthetic window is attached.
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
            BaseMod.logger.info("SpireUI StageHost ready (StsSkin + Stage)");
        } catch (RuntimeException e) {
            ready = false;
            BaseMod.logger.error("SpireUI StageHost init failed", e);
        }
    }

    @Override
    public void receivePostUpdate() {
        if (!ready || stage == null || actors.isEmpty()) {
            return;
        }
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        stage.act(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (!ready || stage == null || actors.isEmpty()) {
            return;
        }
        sb.end();
        stage.draw();
        sb.begin();
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
        Actor actor = LayoutActors.toActor(root, skin, new Runnable() {
            @Override
            public void run() {
                SpireUI.close(windowId);
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
