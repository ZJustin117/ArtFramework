package artframework.core;

import artframework.api.ArtFramework;
import artframework.api.WindowHandle;
import artframework.render.LightwaveEffect;

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
        if (ctx.tree == null) {
            return false;
        }
        String playerId = ctx.argString("player", "");
        if (playerId.isEmpty() && ctx.owner != null) {
            playerId = ctx.owner.id();
        }
        String name = ctx.argString("name", ctx.argString("play", ""));
        if (playerId.isEmpty() || name.isEmpty()) {
            return false;
        }
        AnimationPlayer player = AnimationPlayers.get(ctx.tree.windowId(), playerId);
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
        if (ctx.tree == null) {
            return false;
        }
        String playerId = ctx.argString("player", "");
        if (playerId.isEmpty() && ctx.owner != null) {
            playerId = ctx.owner.id();
        }
        if (playerId.isEmpty()) {
            return false;
        }
        AnimationPlayer player = AnimationPlayers.get(ctx.tree.windowId(), playerId);
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
        if (ctx.tree == null) {
            return false;
        }
        String playerId = ctx.argString("player", "");
        if (playerId.isEmpty() && ctx.owner != null) {
            playerId = ctx.owner.id();
        }
        if (playerId.isEmpty()) {
            return false;
        }
        AnimationPlayer player = AnimationPlayers.get(ctx.tree.windowId(), playerId);
        if (player == null) {
            return false;
        }
        player.resume();
        return true;
    }

    private static boolean setProp(UiActionContext ctx) {
        if (ctx.tree == null) {
            return false;
        }
        String targetId = ctx.argString("target", "");
        String prop = ctx.argString("prop", ctx.argString("property", ""));
        if (targetId.isEmpty() || prop.isEmpty()) {
            return false;
        }
        UiInstance target = ctx.tree.get(targetId);
        if (target == null) {
            target = ctx.tree.find(targetId);
        }
        if (target == null) {
            return false;
        }
        Object value = ctx.resolveValue();
        String fromSlider = ctx.argString("from_slider", ctx.argString("fromSlider", ""));
        if (value == null && !fromSlider.isEmpty()) {
            try {
                artframework.component.WidgetSession session =
                        artframework.component.WidgetSessions.get(ctx.tree.windowId());
                if (session != null && session.hasSlider(fromSlider)) {
                    value = Float.valueOf(session.getSlider(fromSlider));
                }
            } catch (Throwable ignored) {
            }
        }
        if (value == null) {
            return false;
        }
        PropEffectBridge.applyProp(ctx.tree, target, prop, value);
        return true;
    }

    private static boolean pulseEffect(UiActionContext ctx) {
        if (ctx.tree == null) {
            return false;
        }
        String target = ctx.argString("target", "panel");
        String effect = ctx.argString("effect", LightwaveEffect.ID);
        float duration = ctx.argFloat("duration", 0.45f);
        EffectPulse.pulse(ctx.tree.windowId(), target, effect, duration);
        return true;
    }

    private static boolean emit(UiActionContext ctx) {
        if (ctx.tree == null) {
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
                ctx.tree.emit(target, signal, value);
            } else {
                ctx.tree.emit(target, signal);
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
                        ctx.tree != null ? ctx.tree.windowId() : "");
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
