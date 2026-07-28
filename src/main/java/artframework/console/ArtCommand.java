package artframework.console;

import basemod.BaseMod;
import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c2.MapNodeRef;
import artframework.c2.NativeTemplateIds;
import artframework.c2.SelectKind;
import artframework.inspect.UiInspect;
import artframework.inspect.UiLabListeners;
import artframework.ops.GateLab;
import artframework.sts1.inspect.StsUiReflect;
import artframework.sts1.lab.LabStateSnapshot;
import artframework.sts1.lab.StsLabNav;
import artframework.sts1.lab.StsLabRecipes;

import java.util.List;

/**
 * BaseMod console: {@code art probe | open | bind | close | gate | ui | op ...}
 */
public class ArtCommand extends ConsoleCommand {

    @Override
    protected void execute(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            errorMsg();
            return;
        }
        String sub = tokens[depth].toLowerCase();
        if ("ui".equals(sub) || "inspect".equals(sub)) {
            cmdUi(tokens, depth + 1);
            return;
        }
        if ("probe".equals(sub)) {
            String line = ArtFramework.probe().toJsonLine();
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("open".equals(sub)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art open <id>");
                return;
            }
            try {
                ArtFramework.open(tokens[depth + 1]);
                DevConsole.log("opened " + tokens[depth + 1]);
            } catch (RuntimeException e) {
                DevConsole.log("open failed: " + e.getMessage());
            }
            return;
        }
        if ("bind".equals(sub)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art bind <id>");
                return;
            }
            try {
                ensureNativeRegistered(tokens[depth + 1]);
                ArtFramework.bind(tokens[depth + 1]);
                DevConsole.log("bound " + tokens[depth + 1]);
            } catch (RuntimeException e) {
                DevConsole.log("bind failed: " + e.getMessage());
            }
            return;
        }
        if ("close".equals(sub)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art close <id>");
                return;
            }
            ArtFramework.close(tokens[depth + 1]);
            DevConsole.log("closed " + tokens[depth + 1]);
            return;
        }
        if ("gate".equals(sub)) {
            cmdGate(tokens, depth + 1);
            return;
        }
        if ("op".equals(sub)) {
            cmdOp(tokens, depth + 1);
            return;
        }
        if ("fx".equals(sub) || "fullframe".equals(sub)) {
            cmdFullFrame(tokens, depth + 1);
            return;
        }
        if ("assets".equals(sub) || "pack".equals(sub)) {
            cmdAssets(tokens, depth + 1);
            return;
        }
        if ("frame".equals(sub) || "sync".equals(sub)) {
            artframework.context.FrameDiff d = ArtFramework.frames().syncFromBackend();
            DevConsole.log(
                    "frame applied="
                            + d.applied
                            + " +"
                            + d.added.size()
                            + " -"
                            + d.removed.size()
                            + " ~"
                            + d.updated.size()
                            + (d.message.isEmpty() ? "" : " " + d.message));
            return;
        }
        if ("present".equals(sub)) {
            cmdPresent(tokens, depth + 1);
            return;
        }
        if ("lab".equals(sub)) {
            cmdLab(tokens, depth + 1);
            return;
        }
        errorMsg();
    }

    private void cmdLab(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: art lab dump|clear-saves|strip-resume|open-char-select|char <id>|embark|"
                            + "seed [text]|menu-click <R>|abandon|abandon-confirm|return-menu|proceed|"
                            + "ensure-menu|ensure-fresh-menu|start-run [char] [seed=…]|reset|tick");
            return;
        }
        String action = tokens[depth].toLowerCase();
        if ("dump".equals(action)) {
            LabStateSnapshot snap = StsLabNav.dump();
            java.util.Map<String, Object> map = snap.toMap();
            map.put("recipe", artframework.sts1.lab.LabRecipeRunner.statusMap());
            String line = StsLabRecipes.LOG_PREFIX + "dump " + UiInspect.toJson(map);
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("status".equals(action)) {
            String line =
                    StsLabRecipes.LOG_PREFIX
                            + "status "
                            + UiInspect.toJson(artframework.sts1.lab.LabRecipeRunner.statusMap());
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        UiOpResult r;
        if ("clear-saves".equals(action) || "clear_saves".equals(action)) {
            r = StsLabNav.clearSaves();
        } else if ("strip-resume".equals(action) || "strip_resume".equals(action)) {
            r = StsLabNav.stripResume();
        } else if ("open-char-select".equals(action) || "open_char_select".equals(action)) {
            r = StsLabNav.openCharSelect();
        } else if ("char".equals(action) || "character".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art lab char <id>");
                return;
            }
            r = StsLabNav.selectCharacter(tokens[depth + 1]);
        } else if ("embark".equals(action)) {
            r = StsLabNav.embark();
        } else if ("seed".equals(action)) {
            String seed = tokens.length > depth + 1 ? tokens[depth + 1] : "";
            if (seed.regionMatches(true, 0, "seed=", 0, 5)) {
                seed = seed.substring(5);
            }
            r = StsLabNav.setSeed(seed);
        } else if ("menu-click".equals(action) || "menu_click".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art lab menu-click <ClickResult>");
                return;
            }
            r = StsLabNav.menuClick(tokens[depth + 1]);
        } else if ("abandon".equals(action)) {
            r = StsLabNav.abandon();
        } else if ("abandon-confirm".equals(action) || "abandon_confirm".equals(action)) {
            r = StsLabNav.abandonConfirm();
        } else if ("return-menu".equals(action) || "return_menu".equals(action)) {
            r = StsLabNav.returnMenu();
        } else if ("proceed".equals(action)) {
            r = StsLabNav.proceed();
        } else if ("ensure-menu".equals(action) || "ensure_menu".equals(action)) {
            // Async: advanced each postUpdate so hitbox clicks can apply.
            r = StsLabNav.armEnsureMenu();
        } else if ("ensure-fresh-menu".equals(action)
                || "ensure_fresh_menu".equals(action)
                || "reset".equals(action)) {
            r = StsLabNav.armEnsureFreshMenu();
        } else if ("start-run".equals(action) || "start_run".equals(action)) {
            String character = "IRONCLAD";
            String seed = null;
            for (int i = depth + 1; i < tokens.length; i++) {
                String t = tokens[i];
                if (t.regionMatches(true, 0, "seed=", 0, 5)) {
                    seed = t.substring(5);
                } else if (t.regionMatches(true, 0, "char=", 0, 5)) {
                    character = t.substring(5);
                } else if (!t.isEmpty()) {
                    character = t;
                }
            }
            r = StsLabNav.armStartRun(character, seed);
        } else if ("tick".equals(action)) {
            artframework.sts1.lab.LabRecipeRunner.tick();
            r =
                    UiOpResult.ok(
                            "tick "
                                    + artframework.sts1.lab.LabRecipeRunner.statusMap().get("status")
                                    + " "
                                    + artframework.sts1.lab.LabRecipeRunner.statusMap()
                                            .get("message"));
        } else {
            DevConsole.log("Unknown lab action: " + action);
            return;
        }
        String line =
                StsLabRecipes.LOG_PREFIX
                        + action
                        + " "
                        + r.status
                        + (r.message.isEmpty() ? "" : " " + r.message);
        DevConsole.log(line);
        BaseMod.logger.info(line);
    }

    private void cmdPresent(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log("Usage: art present status|combat on|off|observe|status");
            return;
        }
        String target = tokens[depth].toLowerCase();
        if ("status".equals(target)) {
            logPresentStatus();
            return;
        }
        if (!"combat".equals(target)) {
            DevConsole.log("Usage: art present status|combat on|off|observe|status");
            return;
        }
        if (tokens.length < depth + 2) {
            DevConsole.log("Usage: art present combat on|off|observe|status");
            return;
        }
        String action = tokens[depth + 1].toLowerCase();
        if ("status".equals(action)) {
            logPresentStatus();
            return;
        }
        artframework.sts1.PresentLevel level;
        if ("on".equals(action) || "full".equals(action)) {
            level = artframework.sts1.PresentLevel.FULL;
        } else if ("off".equals(action)) {
            level = artframework.sts1.PresentLevel.OFF;
        } else if ("observe".equals(action)) {
            level = artframework.sts1.PresentLevel.OBSERVE;
        } else {
            DevConsole.log("Usage: art present combat on|off|observe|status");
            return;
        }
        artframework.sts1.FullPresentMode.setCombatHandLevel(level);
        if (level.allowsFullPresent() || level.allowsObserve()) {
            ArtFramework.component(artframework.context.SurfaceIds.COMBAT_SURFACE).action("mount_combat");
        } else {
            unmountCombatSurfaces();
        }
        String line =
                "ART_PRESENT combat level="
                        + level.name()
                        + " full-present="
                        + level.allowsFullPresent()
                        + " suppressHand="
                        + artframework.sts1.render.Sts1SurfaceRenderer.shouldSuppressNativeHand();
        DevConsole.log(line);
        BaseMod.logger.info(line);
    }

    private void logPresentStatus() {
        java.util.Map<String, Object> policy = artframework.sts1.FullPresentMode.probeSlice();
        String line =
                "ART_PRESENT combat level="
                        + policy.get("combatHand")
                        + " full-present="
                        + policy.get("combatHandFull")
                        + " suppressHand="
                        + artframework.sts1.render.Sts1SurfaceRenderer.shouldSuppressNativeHand()
                        + " scene="
                        + ArtFramework.projection().scene()
                        + " epoch="
                        + ArtFramework.projection().sceneEpoch()
                        + " handCount="
                        + ArtFramework.projection().listZone(artframework.context.CardZone.HAND).size()
                        + " mapNodes="
                        + ArtFramework.projection().map().nodeCount()
                        + " endTurn="
                        + ArtFramework.projection().controls().endTurnEnabled;
        DevConsole.log(line);
        BaseMod.logger.info(line);
    }

    private void unmountCombatSurfaces() {
        artframework.core.UiComponent hand =
                ArtFramework.component(artframework.context.SurfaceIds.COMBAT_HAND);
        if (hand != null && hand.isMounted()) {
            hand.unmount();
        }
        artframework.core.UiComponent slots =
                ArtFramework.component(artframework.context.SurfaceIds.COMBAT_CARD_SLOTS);
        if (slots != null && slots.isMounted()) {
            slots.unmount();
        }
        artframework.core.UiComponent controls =
                ArtFramework.component(artframework.context.SurfaceIds.COMBAT_CONTROLS);
        if (controls != null && controls.isMounted()) {
            controls.unmount();
        }
        artframework.core.UiComponent root =
                ArtFramework.component(artframework.context.SurfaceIds.COMBAT_SURFACE);
        if (root != null && root.isMounted()) {
            root.unmount();
        }
    }

    private void cmdAssets(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log("Usage: art assets probe|enable <packId>|disable <packId>|order <ids...>");
            return;
        }
        String action = tokens[depth].toLowerCase();
        artframework.assets.HostAssets assets = ArtFramework.assets();
        if ("probe".equals(action)) {
            String line = ArtFramework.probe().toJsonLine();
            // assets section is inside full probe; also log compact pack ids
            DevConsole.log("assets packs=" + assets.packIds() + " vanilla via probe");
            BaseMod.logger.info("ArtFramework assets " + assets.probeAssets());
            DevConsole.log(line);
            return;
        }
        if ("enable".equals(action) || "disable".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art assets enable|disable <packId>");
                return;
            }
            assets.enablePack(tokens[depth + 1], "enable".equals(action));
            DevConsole.log("pack " + tokens[depth + 1] + " enabled=" + "enable".equals(action));
            return;
        }
        if ("order".equals(action)) {
            java.util.List<String> order = new java.util.ArrayList<String>();
            for (int i = depth + 1; i < tokens.length; i++) {
                order.add(tokens[i]);
            }
            assets.setPackOrder(order);
            DevConsole.log("pack order " + order);
            return;
        }
        if ("resolve".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art assets resolve <resourceId>");
                return;
            }
            artframework.assets.AssetResolveResult r = assets.resolve(tokens[depth + 1]);
            DevConsole.log(
                    "resolve "
                            + r.resourceId
                            + " found="
                            + r.found
                            + " pack="
                            + r.packId
                            + " src="
                            + r.source
                            + (r.message.isEmpty() ? "" : " " + r.message));
            return;
        }
        DevConsole.log("Unknown assets action: " + action);
    }

    private void cmdFullFrame(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: art fx enable|disable|tint|glow|blur|glass|clear|capture [args]");
            return;
        }
        String action = tokens[depth].toLowerCase();
        try {
            if ("enable".equals(action)) {
                float w = tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 1920f) : 1920f;
                float h = tokens.length > depth + 2 ? parseFloat(tokens[depth + 2], 1080f) : 1080f;
                ArtFramework.render().enableFullFrame(w, h);
                DevConsole.log("full_frame enabled " + (int) w + "x" + (int) h);
            } else if ("disable".equals(action)) {
                ArtFramework.render().disableFullFrame();
                DevConsole.log("full_frame disabled");
            } else if ("tint".equals(action)) {
                float alpha = tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 0.12f) : 0.12f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("alpha", Float.valueOf(alpha));
                ArtFramework.render().bindFullFrameEffect(artframework.render.TintEffect.ID, p);
                DevConsole.log("full_frame tint alpha=" + alpha);
            } else if ("glow".equals(action)) {
                float intensity =
                        tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 0.4f) : 0.4f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("intensity", Float.valueOf(intensity));
                ArtFramework.render().bindFullFrameEffect(artframework.render.GlowEffect.ID, p);
                DevConsole.log("full_frame glow intensity=" + intensity);
            } else if ("blur".equals(action)) {
                float radius =
                        tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 2.5f) : 2.5f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("radius", Float.valueOf(radius));
                ArtFramework.render().setCaptureEnabled(true);
                ArtFramework.render().bindFullFrameEffect(artframework.render.BlurEffect.ID, p);
                DevConsole.log("full_frame blur radius=" + radius + " (screen capture)");
            } else if ("glass".equals(action)) {
                float radius =
                        tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 2.5f) : 2.5f;
                float tint =
                        tokens.length > depth + 2 ? parseFloat(tokens[depth + 2], 0.45f) : 0.45f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("radius", Float.valueOf(radius));
                p.put("tint", Float.valueOf(tint));
                ArtFramework.render().setCaptureEnabled(true);
                ArtFramework.render().bindFullFrameEffect(artframework.render.GlassEffect.ID, p);
                DevConsole.log("full_frame glass radius=" + radius + " tint=" + tint);
            } else if ("capture".equals(action)) {
                boolean on = tokens.length <= depth + 1
                        || !"off".equalsIgnoreCase(tokens[depth + 1]);
                ArtFramework.render().setCaptureEnabled(on);
                DevConsole.log("capture " + (on ? "on" : "off"));
            } else if ("clear".equals(action)) {
                ArtFramework.render().clearEffects(artframework.render.RenderHost.FULL_FRAME_ID);
                DevConsole.log("full_frame effects cleared");
            } else {
                DevConsole.log("unknown fx action: " + action);
            }
        } catch (RuntimeException e) {
            DevConsole.log("fx failed: " + e.getMessage());
        }
    }

    private void cmdGate(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(GateLab.status());
            DevConsole.log(
                    "Usage: art gate <map|event|endturn|select|select-grid|select-hand|all> <block|clear>");
            return;
        }
        if (tokens.length == depth + 1 && "status".equalsIgnoreCase(tokens[depth])) {
            DevConsole.log(GateLab.status());
            return;
        }
        String target = tokens[depth];
        String action = tokens.length > depth + 1 ? tokens[depth + 1] : "status";
        if ("status".equalsIgnoreCase(action)) {
            DevConsole.log(GateLab.status());
            return;
        }
        // Ensure templates registered/bound so interceptors attach to live sessions
        if ("map".equalsIgnoreCase(target) || "all".equalsIgnoreCase(target)) {
            ensureNativeRegistered(NativeTemplateIds.MAP);
            if (!ArtFramework.isRegistered(NativeTemplateIds.MAP)
                    || ArtFramework.find(NativeTemplateIds.MAP) == null) {
                try {
                    ArtFramework.bind(NativeTemplateIds.MAP);
                } catch (RuntimeException ignored) {
                }
            }
        }
        if ("event".equalsIgnoreCase(target) || "all".equalsIgnoreCase(target)) {
            ensureNativeRegistered(NativeTemplateIds.EVENT);
            try {
                if (ArtFramework.find(NativeTemplateIds.EVENT) == null) {
                    ArtFramework.bind(NativeTemplateIds.EVENT);
                }
            } catch (RuntimeException ignored) {
            }
        }
        if ("endturn".equalsIgnoreCase(target)
                || "end-turn".equalsIgnoreCase(target)
                || "all".equalsIgnoreCase(target)) {
            ensureNativeRegistered(NativeTemplateIds.END_TURN);
            try {
                if (ArtFramework.find(NativeTemplateIds.END_TURN) == null) {
                    ArtFramework.bind(NativeTemplateIds.END_TURN);
                }
            } catch (RuntimeException ignored) {
            }
        }
        if ("select".equalsIgnoreCase(target)
                || "select-grid".equalsIgnoreCase(target)
                || "grid".equalsIgnoreCase(target)
                || "all".equalsIgnoreCase(target)) {
            ensureNativeRegistered(NativeTemplateIds.SELECT_GRID);
            try {
                if (ArtFramework.find(NativeTemplateIds.SELECT_GRID) == null) {
                    ArtFramework.bind(NativeTemplateIds.SELECT_GRID);
                }
            } catch (RuntimeException ignored) {
            }
        }
        if ("select".equalsIgnoreCase(target)
                || "select-hand".equalsIgnoreCase(target)
                || "hand".equalsIgnoreCase(target)
                || "all".equalsIgnoreCase(target)) {
            ensureNativeRegistered(NativeTemplateIds.SELECT_HAND);
            try {
                if (ArtFramework.find(NativeTemplateIds.SELECT_HAND) == null) {
                    ArtFramework.bind(NativeTemplateIds.SELECT_HAND);
                }
            } catch (RuntimeException ignored) {
            }
        }
        String msg = GateLab.apply(target, action);
        DevConsole.log(msg);
        BaseMod.logger.info("ArtFramework " + msg);
    }

    private void cmdUi(String[] tokens, int depth) {
        ensureUiLabSink();
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: art ui list|tree|node|emit|invoke|listen|native …");
            return;
        }
        String action = tokens[depth].toLowerCase();
        if ("list".equals(action)) {
            String line = UiInspect.LOG_PREFIX + UiInspect.toJson(UiInspect.listSurfaces());
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("tree".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art ui tree <windowId> [depth]");
                return;
            }
            int maxDepth =
                    tokens.length > depth + 2
                            ? parseInt(tokens[depth + 2], UiInspect.DEFAULT_TREE_DEPTH)
                            : UiInspect.DEFAULT_TREE_DEPTH;
            List<String> lines = UiInspect.treeLines(tokens[depth + 1], maxDepth);
            for (String line : lines) {
                DevConsole.log(UiInspect.LOG_PREFIX + line);
                BaseMod.logger.info(UiInspect.LOG_PREFIX + line);
            }
            return;
        }
        if ("node".equals(action)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art ui node <windowId> <id|path>");
                return;
            }
            String line =
                    UiInspect.LOG_PREFIX
                            + UiInspect.toJson(UiInspect.nodeMap(tokens[depth + 1], tokens[depth + 2]));
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("emit".equals(action)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art ui emit <target|window control> <signal> [args…]");
                return;
            }
            String target;
            String signal;
            int argFrom;
            if (tokens[depth + 1].indexOf('/') >= 0
                    || tokens.length == depth + 3
                    || looksLikeSignal(tokens[depth + 2])) {
                target = tokens[depth + 1];
                signal = tokens[depth + 2];
                argFrom = depth + 3;
            } else {
                target = tokens[depth + 1] + "/" + tokens[depth + 2];
                if (tokens.length < depth + 4) {
                    DevConsole.log("Usage: art ui emit <window> <control> <signal> [args…]");
                    return;
                }
                signal = tokens[depth + 3];
                argFrom = depth + 4;
            }
            Object[] args = UiInspect.parseArgs(tokens, argFrom);
            UiOpResult r = UiInspect.emit(target, signal, args);
            String line =
                    UiInspect.LOG_PREFIX
                            + "emit "
                            + target
                            + " "
                            + signal
                            + " → "
                            + r.status
                            + (r.message.isEmpty() ? "" : " " + r.message);
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("invoke".equals(action)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art ui invoke <componentId> <action> [args…]");
                return;
            }
            String componentId = tokens[depth + 1];
            String act = tokens[depth + 2];
            Object[] args = UiInspect.parseArgs(tokens, depth + 3);
            UiOpResult r = UiInspect.invoke(componentId, act, args);
            String line =
                    UiInspect.LOG_PREFIX
                            + "invoke "
                            + componentId
                            + " "
                            + act
                            + " → "
                            + r.status
                            + (r.message.isEmpty() ? "" : " " + r.message);
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("listen".equals(action)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art ui listen <target> <signal> [off]");
                return;
            }
            String target = tokens[depth + 1];
            String signal = tokens[depth + 2];
            boolean off =
                    tokens.length > depth + 3 && "off".equalsIgnoreCase(tokens[depth + 3]);
            UiOpResult r = off ? UiLabListeners.unlisten(target, signal) : UiLabListeners.listen(target, signal);
            String line =
                    UiInspect.LOG_PREFIX
                            + "listen "
                            + r.status
                            + (r.message.isEmpty() ? "" : " " + r.message);
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("native".equals(action)) {
            cmdUiNative(tokens, depth + 1);
            return;
        }
        DevConsole.log("Unknown ui action: " + action);
    }

    private void cmdUiNative(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log("Usage: art ui native dump|click <path> [extra]");
            return;
        }
        String action = tokens[depth].toLowerCase();
        if ("dump".equals(action)) {
            String line = UiInspect.LOG_PREFIX + "native " + UiInspect.toJson(StsUiReflect.dump());
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("click".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art ui native click endturn|grid.confirm|event [index]");
                return;
            }
            String path = tokens[depth + 1];
            String extra = tokens.length > depth + 2 ? tokens[depth + 2] : null;
            UiOpResult r = StsUiReflect.click(path, extra);
            String line =
                    UiInspect.LOG_PREFIX
                            + "native click "
                            + path
                            + " → "
                            + r.status
                            + (r.message.isEmpty() ? "" : " " + r.message);
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        DevConsole.log("Unknown native action: " + action);
    }

    private static boolean looksLikeSignal(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        if (token.indexOf('/') >= 0) {
            return false;
        }
        return token.indexOf('_') >= 0
                || "pressed".equals(token)
                || "toggled".equals(token)
                || "confirmed".equals(token);
    }

    private static void ensureUiLabSink() {
        if (uiLabSinkInstalled) {
            return;
        }
        uiLabSinkInstalled = true;
        UiLabListeners.addSink(
                new UiLabListeners.LogSink() {
                    @Override
                    public void log(String line) {
                        try {
                            DevConsole.log(line);
                        } catch (Throwable ignored) {
                        }
                        try {
                            BaseMod.logger.info(line);
                        } catch (Throwable ignored) {
                        }
                    }
                });
    }

    private static boolean uiLabSinkInstalled;

    private void cmdOp(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: art op select|confirm|map|event|endturn|play|button|slider|hitarea|click ...");
            return;
        }
        String kind = tokens[depth].toLowerCase();
        UiOpResult r;
        if ("select".equals(kind)) {
            // art op select grid|hand <cardId> [index]
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art op select grid|hand <cardId> [index]");
                return;
            }
            SelectKind sk = "hand".equalsIgnoreCase(tokens[depth + 1]) ? SelectKind.HAND : SelectKind.GRID;
            String cardId = tokens[depth + 2];
            int index = tokens.length > depth + 3 ? parseInt(tokens[depth + 3], 0) : 0;
            r = ArtFramework.ops().selectCard(sk, cardId, index);
        } else if ("confirm".equals(kind)) {
            SelectKind sk = tokens.length > depth + 1 && "hand".equalsIgnoreCase(tokens[depth + 1])
                    ? SelectKind.HAND
                    : SelectKind.GRID;
            r = ArtFramework.ops().confirmSelect(sk);
        } else if ("map".equals(kind)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art op map <row> <col> [roomType]");
                return;
            }
            int row = parseInt(tokens[depth + 1], 0);
            int col = parseInt(tokens[depth + 2], 0);
            String room = tokens.length > depth + 3 ? tokens[depth + 3] : "";
            r = ArtFramework.ops().clickMapNode(new MapNodeRef(row, col, room));
        } else if ("event".equals(kind)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art op event <index> [label...]");
                return;
            }
            int index = parseInt(tokens[depth + 1], 0);
            String label = tokens.length > depth + 2 ? join(tokens, depth + 2) : "";
            r = ArtFramework.ops().chooseEventOption(index, label);
        } else if ("endturn".equals(kind)) {
            r = ArtFramework.ops().pressEndTurn();
        } else if ("play".equals(kind)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art op play <cardId> [target]");
                return;
            }
            String target = tokens.length > depth + 2 ? tokens[depth + 2] : "";
            r = ArtFramework.ops().playHandCard(tokens[depth + 1], target);
        } else if ("button".equals(kind)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art op button <windowId> <buttonId>");
                return;
            }
            r = ArtFramework.ops().clickButton(tokens[depth + 1], tokens[depth + 2]);
        } else if ("slider".equals(kind)) {
            if (tokens.length < depth + 4) {
                DevConsole.log("Usage: art op slider <windowId> <sliderId> <value>");
                return;
            }
            float value = parseFloat(tokens[depth + 3], 0f);
            r = ArtFramework.ops().setSlider(tokens[depth + 1], tokens[depth + 2], value);
        } else if ("hitarea".equals(kind)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art op hitarea <windowId> <hitAreaId>");
                return;
            }
            r = ArtFramework.ops().clickHitArea(tokens[depth + 1], tokens[depth + 2]);
        } else if ("click".equals(kind)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art op click <windowId> <controlId>");
                return;
            }
            r = ArtFramework.ops().click(tokens[depth + 1], tokens[depth + 2]);
        } else {
            DevConsole.log("Unknown op: " + kind);
            return;
        }
        DevConsole.log("op " + kind + " → " + r.status + (r.message.isEmpty() ? "" : " " + r.message));
        BaseMod.logger.info("ArtFramework op " + kind + " " + r);
    }

    private static void ensureNativeRegistered(String id) {
        if (ArtFramework.isRegistered(id)) {
            return;
        }
        if (NativeTemplateIds.MAP.equals(id)
                || NativeTemplateIds.EVENT.equals(id)
                || NativeTemplateIds.SELECT_GRID.equals(id)
                || NativeTemplateIds.SELECT_HAND.equals(id)
                || NativeTemplateIds.END_TURN.equals(id)) {
            ArtFramework.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String join(String[] tokens, int from) {
        StringBuilder sb = new StringBuilder(tokens[from]);
        for (int i = from + 1; i < tokens.length; i++) {
            sb.append(' ').append(tokens[i]);
        }
        return sb.toString();
    }

    @Override
    public void errorMsg() {
        DevConsole.log(
                "art: probe | open|bind|close <id> | gate … | ui … | lab … | fx … | assets … | frame | present combat on|off | op …");
    }
}
