package artframework.core;

import artframework.api.ArtFramework;
import artframework.api.WindowHandle;
import artframework.render.LightwaveEffect;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationRuntime;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.ControlValueComponent;

/**
 * Built-in {@link UiAction} implementations for declarative connections.
 */
final class BuiltinUiActions {

    private BuiltinUiActions() {}

    static void install() {
        UiActions.register(UiActions.PLAY, new UiAction() {
            @Override
            public boolean run(UiActionContext ctx) {
                return play(ctx);
            }
        });
        UiActions.register(UiActions.PAUSE, new UiAction() {
            @Override
            public boolean run(UiActionContext ctx) {
                return pauseOrStop(ctx, true);
            }
        });
        UiActions.register(UiActions.STOP, new UiAction() {
            @Override
            public boolean run(UiActionContext ctx) {
                return pauseOrStop(ctx, false);
            }
        });
        UiActions.register(UiActions.RESUME, new UiAction() {
            @Override
            public boolean run(UiActionContext ctx) {
                return resume(ctx);
            }
        });
        UiActions.register(UiActions.SET_PROP, new UiAction() {
            @Override
            public boolean run(UiActionContext ctx) {
                return setProp(ctx);
            }
        });
        UiActions.register(UiActions.PULSE_EFFECT, new UiAction() {
            @Override
            public boolean run(UiActionContext ctx) {
                return pulseEffect(ctx);
            }
        });
        UiActions.register(UiActions.EMIT, new UiAction() {
            @Override
            public boolean run(UiActionContext ctx) {
                return emit(ctx);
            }
        });
        UiActions.register(UiActions.CLOSE_WINDOW, new UiAction() {
            @Override
            public boolean run(UiActionContext ctx) {
                return closeWindow(ctx);
            }
        });
    }

    private static boolean play(UiActionContext ctx) {
        if (ctx.context == null) {
            return false;
        }
        String playerId = ctx.argString("player", "");
        if (playerId.isEmpty() && ctx.ownerEntity != null) {
            NodeIdentityComponent owner = PresentationRuntime.identity(ctx.context, ctx.ownerEntity);
            playerId = owner != null ? owner.name : "";
        }
        String name = ctx.argString("name", ctx.argString("play", ""));
        if (playerId.isEmpty() || name.isEmpty()) {
            return false;
        }
        AnimationPlayer player = AnimationPlayers.get(PresentationRuntime.windowId(ctx.context), playerId);
        if (player == null || !player.has(name)) {
            return false;
        }
        String mode = ctx.argString("mode", "");
        if (!mode.isEmpty()) {
            player.play(name, mode);
        } else {
            player.play(name);
        }
        return true;
    }

    private static boolean pauseOrStop(UiActionContext ctx, boolean pause) {
        if (ctx.context == null) {
            return false;
        }
        String playerId = ctx.argString("player", "");
        if (playerId.isEmpty() && ctx.ownerEntity != null) {
            NodeIdentityComponent owner = PresentationRuntime.identity(ctx.context, ctx.ownerEntity);
            playerId = owner != null ? owner.name : "";
        }
        if (playerId.isEmpty()) {
            return false;
        }
        AnimationPlayer player = AnimationPlayers.get(PresentationRuntime.windowId(ctx.context), playerId);
        if (player == null) {
            return false;
        }
        if (pause) {
            player.pause();
        } else {
            player.stop();
        }
        return true;
    }

    private static boolean resume(UiActionContext ctx) {
        if (ctx.context == null) {
            return false;
        }
        String playerId = ctx.argString("player", "");
        if (playerId.isEmpty() && ctx.ownerEntity != null) {
            NodeIdentityComponent owner = PresentationRuntime.identity(ctx.context, ctx.ownerEntity);
            playerId = owner != null ? owner.name : "";
        }
        if (playerId.isEmpty()) {
            return false;
        }
        AnimationPlayer player = AnimationPlayers.get(PresentationRuntime.windowId(ctx.context), playerId);
        if (player == null) {
            return false;
        }
        player.resume();
        return true;
    }

    private static boolean setProp(UiActionContext ctx) {
        if (ctx.context == null) {
            return false;
        }
        String targetId = ctx.argString("target", "");
        String prop = ctx.argString("prop", ctx.argString("property", ""));
        if (targetId.isEmpty() || prop.isEmpty()) {
            return false;
        }
        EntityId target = PresentationRuntime.find(ctx.context, targetId);
        if (target == null) {
            return false;
        }
        Object value = ctx.resolveValue();
        String fromSlider = ctx.argString("from_slider", ctx.argString("fromSlider", ""));
        if (value == null && !fromSlider.isEmpty()) {
            try {
                 EntityId slider = PresentationRuntime.find(ctx.context, fromSlider);
                 if (slider != null) {
                     ControlValueComponent control = PresentationRuntime.component(
                             ctx.context, slider, ControlValueComponent.class);
                    if (control != null) value = control.value;
                }
            } catch (Throwable ignored) {
            }
        }
        if (value == null) {
            return false;
        }
        PropEffectBridge.applyProp(ctx.context, target, prop, value);
        return true;
    }

    private static boolean pulseEffect(UiActionContext ctx) {
        if (ctx.context == null) {
            return false;
        }
        String target = ctx.argString("target", "panel");
        String effect = ctx.argString("effect", LightwaveEffect.ID);
        float duration = ctx.argFloat("duration", 0.45f);
        EffectPulse.pulse(PresentationRuntime.windowId(ctx.context), target, effect, duration);
        return true;
    }

    private static boolean emit(UiActionContext ctx) {
        if (ctx.context == null) {
            return false;
        }
        String target = ctx.argString("target", "");
        String signal = ctx.argString("signal", "");
        if (target.isEmpty() || signal.isEmpty()) {
            return false;
        }
        Object value = ctx.resolveValue();
        try {
            if (value != null) {
                EntityId targetEntity = PresentationRuntime.find(ctx.context, target);
                PresentationRuntime.emit(ctx.context, targetEntity, signal, value);
            } else {
                EntityId targetEntity = PresentationRuntime.find(ctx.context, target);
                PresentationRuntime.emit(ctx.context, targetEntity, signal);
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean closeWindow(UiActionContext ctx) {
        String win =
                ctx.argString(
                        "window",
                        ctx.context != null ? PresentationRuntime.windowId(ctx.context) : "");
        if (win.isEmpty()) {
            return false;
        }
        String target = ctx.argString("target", "panel");
        final String windowId = win;
        Runnable close =
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            WindowHandle h = ArtFramework.find(windowId);
                            if (h != null && h.isOpen()) {
                                ArtFramework.close(windowId);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                };
        if (ctx.argBool("fx", true)) {
            EffectPulse.closeWithFx(windowId, target, close);
        } else {
            close.run();
        }
        return true;
    }

}
