package artframework.console;

import basemod.BaseMod;
import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.MapNodeRef;
import artframework.component.NativeTemplateIds;
import artframework.c2.SelectKind;
import artframework.inspect.UiInspect;
import artframework.inspect.UiLabListeners;
import artframework.ops.GateLab;
import artframework.sts1.inspect.StsUiReflect;
import artframework.sts1.lab.LabStateSnapshot;
import artframework.sts1.lab.StsLabNav;
import artframework.sts1.lab.StsLabRecipes;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BaseMod console: {@code art probe | open | bind | close | gate | ui | op ...}
 */
public class ArtCommand extends ConsoleCommand {

    private static long commandSequence;

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
            writeLocalProbe(line);
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
                commandResult("art open " + tokens[depth + 1], "OK", "opened " + tokens[depth + 1]);
            } catch (RuntimeException e) {
                commandResult("art open " + tokens[depth + 1], "ERROR", message(e, "open failed"));
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
                commandResult("art bind " + tokens[depth + 1], "OK", "bound " + tokens[depth + 1]);
            } catch (RuntimeException e) {
                commandResult("art bind " + tokens[depth + 1], "ERROR", message(e, "bind failed"));
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
            DevConsole.log("frames are published by the authority endpoint");
            return;
        }
        if ("present".equals(sub)) {
            cmdPresent(tokens, depth + 1);
            return;
        }
        if ("skeleton".equals(sub)) {
            cmdSkeleton(tokens, depth + 1);
            return;
        }
        if ("profile".equals(sub) || "theme".equals(sub)) {
            cmdProfile(tokens, depth + 1);
            return;
        }
        if ("lab".equals(sub)) {
            cmdLab(tokens, depth + 1);
            return;
        }
        errorMsg();
    }

    private static String message(RuntimeException e, String fallback) {
        return e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : fallback;
    }

    private static synchronized void commandResult(String command, String status, String message) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sequence", Long.valueOf(++commandSequence));
        result.put("status", status);
        result.put("command", command);
        result.put("message", message != null ? message : "");
        String line = "ART_COMMAND " + artframework.inspect.UiInspect.toJson(result);
        DevConsole.log(line);
        ProbeSidecar.writeCommand(line);
        if ("ERROR".equals(status)) {
            BaseMod.logger.error(line);
        } else {
            BaseMod.logger.info(line);
        }
    }

    private void cmdSkeleton(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log("Usage: art skeleton dev status|load|play|bone|seek|freeze|stop ... or art skeleton sts1 load <id> <atlas> <json>");
            return;
        }
        String scope = tokens[depth].toLowerCase();
        if ("sts1".equals(scope)) {
            if (tokens.length >= depth + 4 && "load".equalsIgnoreCase(tokens[depth + 1])) {
                artframework.sts1.skeleton.Sts1SkeletonBridge.sts1Load(
                        tokens[depth + 2], tokens[depth + 3], tokens[depth + 4]);
                logSkeletonDev("ART_SKELETON_STS1 loaded " + tokens[depth + 2]);
            } else if (tokens.length >= depth + 3 && "play".equalsIgnoreCase(tokens[depth + 1])) {
                boolean ok = artframework.sts1.skeleton.Sts1SkeletonBridge.setAnimation(
                        tokens[depth + 2], tokens[depth + 3], false);
                logSkeletonDev("ART_SKELETON_STS1 play " + ok);
            } else if (tokens.length >= depth + 3 && "stop".equalsIgnoreCase(tokens[depth + 1])) {
                artframework.sts1.skeleton.Sts1SkeletonBridge.stop(tokens[depth + 2]);
                logSkeletonDev("ART_SKELETON_STS1 stopped " + tokens[depth + 2]);
            } else {
                logSkeletonDev("Usage: art skeleton sts1 load <id> <atlas> <json>|play <id> <animation>|stop <id>");
            }
            return;
        }
        if (!"dev".equals(scope)) {
            DevConsole.log("Usage: art skeleton dev status|load|play|bone|seek|freeze|stop");
            return;
        }
        String action = tokens.length > depth + 1 ? tokens[depth + 1].toLowerCase() : "status";
        try {
            if ("status".equals(action)) {
                logSkeletonDev("ART_SKELETON_DEV provider=spine42 bundle="
                        + artframework.sts1.skeleton.Sts1SkeletonBridge.devBundleConfigured()
                        + " probe=" + artframework.sts1.skeleton.Sts1SkeletonBridge.probeSlice());
            } else if ("load".equals(action) && tokens.length >= depth + 5) {
                artframework.sts1.skeleton.Sts1SkeletonBridge.devLoad(
                        tokens[depth + 2], tokens[depth + 3], tokens[depth + 4]);
                logSkeletonDev("ART_SKELETON_DEV loaded " + tokens[depth + 2]);
            } else if ("play".equals(action) && tokens.length >= depth + 4) {
                artframework.sts1.skeleton.Sts1SkeletonBridge.noteDevCommand("play:" + tokens[depth + 2]);
                boolean ok = artframework.sts1.skeleton.Sts1SkeletonBridge.setAnimation(
                        tokens[depth + 2], tokens[depth + 3], false);
                logSkeletonDev("ART_SKELETON_DEV play " + ok);
            } else if ("stop".equals(action) && tokens.length >= depth + 3) {
                artframework.sts1.skeleton.Sts1SkeletonBridge.noteDevCommand("stop:" + tokens[depth + 2]);
                artframework.sts1.skeleton.Sts1SkeletonBridge.stop(tokens[depth + 2]);
                logSkeletonDev("ART_SKELETON_DEV stopped " + tokens[depth + 2]);
            } else if ("bone".equals(action) && tokens.length >= depth + 4) {
                artframework.skeleton.BoneTransform bone = artframework.sts1.skeleton.Sts1SkeletonBridge.devBone(
                        tokens[depth + 2], tokens[depth + 3]);
                logSkeletonDev("ART_SKELETON_DEV bone " + (bone != null));
            } else if ("seek".equals(action) && tokens.length >= depth + 4) {
                float seconds = Float.parseFloat(tokens[depth + 3]);
                boolean ok = artframework.sts1.skeleton.Sts1SkeletonBridge.setTrackTime(
                        tokens[depth + 2], seconds);
                logSkeletonDev("ART_SKELETON_DEV seek " + ok);
            } else if ("freeze".equals(action) && tokens.length >= depth + 3) {
                boolean ok = artframework.sts1.skeleton.Sts1SkeletonBridge.setTimeScale(
                        tokens[depth + 2], 0f);
                logSkeletonDev("ART_SKELETON_DEV freeze " + ok);
            } else {
                logSkeletonDev("Usage: art skeleton dev status|load <id> <atlasEntry> <skeletonEntry>|play <id> <animation>|bone <id> <bone>|seek <id> <seconds>|freeze <id>|stop <id>");
            }
        } catch (Throwable t) {
            logSkeletonDev("ART_SKELETON_DEV failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static void logSkeletonDev(String line) {
        DevConsole.log(line);
        BaseMod.logger.info(line);
    }

    private static void writeLocalProbe(String line) {
        ProbeSidecar.write(line);
    }

    private void cmdProfile(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: art profile list|get|set|project|resolve|surface|restyle|"
                            + "enable|select|pack|modify"
                            + "  (project="
                            + ArtFramework.projectPresent()
                            + ")");
            return;
        }
        String action = tokens[depth].toLowerCase();
        if ("list".equals(action)) {
            DevConsole.log(
                    "catalog ids="
                            + ArtFramework.presentProfileIds()
                            + " count="
                            + ArtFramework.presentProfileIds().size());
            DevConsole.log("enabled=" + ArtFramework.enabledPresentIds());
            DevConsole.log(
                    "packs="
                            + ArtFramework.presentPackIds()
                            + " active="
                            + ArtFramework.activePresentPack());
            DevConsole.log("themes: " + artframework.core.Themes.names());
            DevConsole.log("project=" + ArtFramework.projectPresent() + " (apply; register via API)");
            DevConsole.log("surfacePresent=" + artframework.core.SurfacePresent.probeSummary());
            return;
        }
        if ("get".equals(action)) {
            artframework.core.PresentProfile p = artframework.core.ProjectPresent.profile();
            DevConsole.log(
                    "project="
                            + (p != null ? p.id : "?")
                            + " theme="
                            + (p != null && p.theme.name() != null ? p.theme.name() : "")
                            + " packId="
                            + (p != null ? p.packId : "")
                            + " cardAlpha="
                            + (p != null ? p.chrome.cardAlpha : 1f));
            if (tokens.length >= depth + 2) {
                String win = tokens[depth + 1];
                artframework.core.PresentResolved r = ArtFramework.resolvePresent(win);
                DevConsole.log(
                        "resolve "
                                + win
                                + " id="
                                + r.profileId
                                + " fromProject="
                                + r.fromProject
                                + " packId="
                                + r.packId
                                + " cardAlpha="
                                + r.chrome.cardAlpha);
            }
            return;
        }
        if ("set".equals(action) || "project".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art profile set|project <sts|lightwave>");
                return;
            }
            String profileId = tokens[depth + 1];
            try {
                ArtFramework.setProjectPresent(profileId);
                commandResult(
                        "art profile " + action + " " + profileId,
                        "OK",
                        "project="
                                + ArtFramework.projectPresent()
                                + " pack="
                                + ArtFramework.activePresentPack());
            } catch (RuntimeException e) {
                commandResult(
                        "art profile " + action + " " + profileId,
                        "ERROR",
                        message(e, "profile set failed"));
            }
            return;
        }
        if ("resolve".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art profile resolve <windowId|surfaceId>");
                return;
            }
            String target = tokens[depth + 1];
            artframework.core.PresentResolved r;
            if (artframework.presentation.PresentationRuntime.context(target) != null) {
                r = ArtFramework.resolvePresent(target);
            } else {
                r = ArtFramework.resolveSurfacePresent(target);
            }
            DevConsole.log(
                    "resolve id="
                            + r.profileId
                            + " fromProject="
                            + r.fromProject
                            + " packId="
                            + r.packId
                            + " theme="
                            + (r.theme.name() != null ? r.theme.name() : "")
                            + " cardAlpha="
                            + r.chrome.cardAlpha);
            return;
        }
        if ("surface".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art profile surface <surfaceId> [profileId|clear]");
                return;
            }
            String sid = tokens[depth + 1];
            if (tokens.length < depth + 3 || "clear".equalsIgnoreCase(tokens[depth + 2])) {
                ArtFramework.unbindSurfacePresent(sid);
                DevConsole.log("surface present cleared: " + sid);
                return;
            }
            try {
                ArtFramework.bindSurfacePresent(sid, tokens[depth + 2]);
                artframework.core.PresentResolved r = ArtFramework.resolveSurfacePresent(sid);
                DevConsole.log(
                        "surface "
                                + sid
                                + " → "
                                + r.profileId
                                + " cardAlpha="
                                + r.chrome.cardAlpha);
            } catch (RuntimeException e) {
                DevConsole.log("surface bind failed: " + e.getMessage());
            }
            return;
        }
        if ("restyle".equals(action)) {
            ArtFramework.restyleOpenPresent();
            DevConsole.log("restyle open present done project=" + ArtFramework.projectPresent());
            return;
        }
        if ("enable".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art profile enable <regex>|all|clear");
                return;
            }
            String arg = tokens[depth + 1];
            if ("all".equalsIgnoreCase(arg) || "clear".equalsIgnoreCase(arg)) {
                ArtFramework.clearEnabledPresentRestriction();
                DevConsole.log("enabled presents: all (" + ArtFramework.enabledPresentIds() + ")");
                return;
            }
            int n = ArtFramework.modifyPresentsMatching(arg, true);
            DevConsole.log("enabled matching " + arg + " count=" + n + " → " + ArtFramework.enabledPresentIds());
            return;
        }
        if ("disable".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art profile disable <regex>");
                return;
            }
            int n = ArtFramework.modifyPresentsMatching(tokens[depth + 1], false);
            DevConsole.log("disabled matching count=" + n + " → " + ArtFramework.enabledPresentIds());
            return;
        }
        if ("select".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art profile select <regex>");
                return;
            }
            try {
                String id = ArtFramework.selectPresentMatching(tokens[depth + 1]);
                DevConsole.log(
                        "selected "
                                + id
                                + " pack="
                                + ArtFramework.activePresentPack());
            } catch (RuntimeException e) {
                DevConsole.log("select failed: " + e.getMessage());
            }
            return;
        }
        if ("pack".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art profile pack list|activate <id>|deactivate [id]");
                return;
            }
            String sub = tokens[depth + 1].toLowerCase();
            if ("list".equals(sub)) {
                DevConsole.log(
                        "packs="
                                + ArtFramework.presentPackIds()
                                + " active="
                                + ArtFramework.activePresentPack());
                return;
            }
            if ("activate".equals(sub)) {
                if (tokens.length < depth + 3) {
                    DevConsole.log("Usage: art profile pack activate <packId>");
                    return;
                }
                try {
                    ArtFramework.activatePresentPack(tokens[depth + 2]);
                    DevConsole.log("pack active=" + ArtFramework.activePresentPack());
                } catch (RuntimeException e) {
                    DevConsole.log("pack activate failed: " + e.getMessage());
                }
                return;
            }
            if ("deactivate".equals(sub)) {
                String id =
                        tokens.length >= depth + 3
                                ? tokens[depth + 2]
                                : ArtFramework.activePresentPack();
                ArtFramework.deactivatePresentPack(id);
                DevConsole.log("pack deactivated; active=" + ArtFramework.activePresentPack());
                return;
            }
            DevConsole.log("Unknown pack action: " + sub);
            return;
        }
        if ("modify".equals(action)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art profile modify <regex> packId <id>|clear [select]");
                return;
            }
            String regex = tokens[depth + 1];
            if (!"packid".equalsIgnoreCase(tokens[depth + 2])
                    && !"pack".equalsIgnoreCase(tokens[depth + 2])) {
                DevConsole.log("Usage: art profile modify <regex> packId <id>|clear [select]");
                return;
            }
            if (tokens.length < depth + 4) {
                DevConsole.log("Usage: art profile modify <regex> packId <id>|clear [select]");
                return;
            }
            String packArg = tokens[depth + 3];
            String newPack = "clear".equalsIgnoreCase(packArg) ? "" : packArg;
            boolean select =
                    tokens.length >= depth + 5 && "select".equalsIgnoreCase(tokens[depth + 4]);
            int n = ArtFramework.modifyPresentPackIdMatching(regex, newPack, select);
            DevConsole.log("modified packId count=" + n + " project=" + ArtFramework.projectPresent());
            return;
        }
        DevConsole.log("Unknown profile action: " + action);
    }

    private void cmdLab(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: art lab dump|clear-saves|strip-resume|open-char-select|char <id>|embark|"
                            + "seed [text]|menu-click <R>|abandon|abandon-confirm|return-menu|proceed|"
                            + "enter-event [id]|enter-room <rest|shop|treasure>|enter-select <grid|hand>|"
                            + "ensure-menu|ensure-fresh-menu|"
                            + "start-run [char] [seed=…]|entity-attach|"
                            + "entity-detach|host-recreate|reset|tick");
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
        } else if ("enter-event".equals(action) || "enter_event".equals(action)) {
            r = StsLabNav.enterEvent(tokens.length > depth + 1 ? tokens[depth + 1] : "");
        } else if ("enter-room".equals(action) || "enter_room".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art lab enter-room <rest|shop|treasure>");
                return;
            }
            r = StsLabNav.enterRoom(tokens[depth + 1]);
        } else if ("enter-select".equals(action) || "enter_select".equals(action)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: art lab enter-select <grid|hand>");
                return;
            }
            r = StsLabNav.enterSelect(tokens[depth + 1]);
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
        } else if ("entity-attach".equals(action)) {
            artframework.c2.EntityPresent entities = ArtFramework.entities();
            entities.present("art-lab-entity", "player", "ironclad",
                    artframework.c2.EntitySnapshot.playerChrome("ART Entity", 70, 80, 0),
                    320f, 720f, 1f);
            r = UiOpResult.ok("entity attached slots=" + entities.size()
                    + " draw=" + artframework.c2.EntityDrawPath.buildFromPresent().size()
                    + " target=" + (artframework.render.RenderHosts.get()
                            .getTarget("c2:entity:art-lab-entity") != null));
        } else if ("entity-detach".equals(action)) {
            ArtFramework.entities().detach("art-lab-entity");
            r = UiOpResult.ok("entity detached");
        } else if ("host-recreate".equals(action)) {
            artframework.sts1.PresentSafety.onHostRecreated();
            r = UiOpResult.ok("host caches recreated");
        } else {
            DevConsole.log("Unknown lab action: " + action);
            commandResult(labCommand(tokens, depth), "ERROR", "unknown lab action: " + action);
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
        commandResult(labCommand(tokens, depth), r.isOk() ? "OK" : "ERROR", r.message);
    }

    private static String labCommand(String[] tokens, int depth) {
        StringBuilder command = new StringBuilder("art lab");
        for (int i = depth; i < tokens.length; i++) {
            command.append(' ').append(tokens[i]);
        }
        return command.toString();
    }

    private void cmdPresent(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: art present status|panic|clear-panic|combat|map|skeleton|event|select|reward|rest|treasure|shop|top|intents|targeting|proceed|energy on|off|observe|status");
            return;
        }
        String target = tokens[depth].toLowerCase();
        if ("status".equals(target)) {
            logPresentStatus();
            return;
        }
        if ("panic".equals(target)) {
            String reason = tokens.length > depth + 1 ? tokens[depth + 1] : "console";
            artframework.sts1.PresentSafety.panic(reason);
            DevConsole.log("ART_PRESENT panic reason=" + reason);
            BaseMod.logger.info("ART_PRESENT panic reason=" + reason);
            return;
        }
        if ("clear-panic".equals(target) || "clearpanic".equals(target)) {
            artframework.sts1.PresentSafety.clearPanic();
            DevConsole.log("ART_PRESENT panic cleared");
            BaseMod.logger.info("ART_PRESENT panic cleared");
            return;
        }
        if (!"combat".equals(target)
                && !"map".equals(target)
                && !"skeleton".equals(target)
                && !"event".equals(target)
                && !"select".equals(target)
                && !"reward".equals(target)
                && !"rest".equals(target)
                && !"treasure".equals(target)
                && !"shop".equals(target)
                && !"top".equals(target)
                && !"toppanel".equals(target)
                && !"intents".equals(target)
                && !"targeting".equals(target)
                && !"proceed".equals(target)
                && !"energy".equals(target)) {
            DevConsole.log(
                    "Usage: art present status|panic|clear-panic|combat|map|skeleton|event|select|reward|rest|treasure|shop|top|intents|targeting|proceed|energy on|off|observe|status");
            return;
        }
        if (tokens.length < depth + 2) {
            DevConsole.log("Usage: art present " + target + " on|off|observe|status");
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
            DevConsole.log("Usage: art present " + target + " on|off|observe|status");
            return;
        }
        if (artframework.sts1.PresentSafety.isPanic() && level != artframework.sts1.PresentLevel.OFF) {
            DevConsole.log("ART_PRESENT blocked: panic active — art present clear-panic first");
            return;
        }
        if ("combat".equals(target)) {
            artframework.sts1.FullPresentMode.setCombatHandLevel(level);
            artframework.sts1.FullPresentMode.setCombatControlsLevel(level);
            artframework.sts1.FullPresentMode.setEnergyLevel(level);
            if (level.allowsFullPresent() || level.allowsObserve()) {
                ArtFramework.component(artframework.context.SurfaceIds.COMBAT_SURFACE)
                        .action("mount_combat");
                mountPresentAction(
                        artframework.context.SurfaceIds.COMBAT_ENERGY, "mount_energy", level);
            } else {
                unmountCombatSurfaces();
            }
        } else if ("map".equals(target)) {
            artframework.sts1.FullPresentMode.setMapLevel(level);
            artframework.core.UiComponent map =
                    ArtFramework.component(artframework.context.SurfaceIds.MAP);
            if (map != null) {
                if (level.allowsFullPresent() || level.allowsObserve()) {
                    if (!map.isMounted()) {
                        map.mount();
                    }
                } else if (map.isMounted()) {
                    map.unmount();
                }
            }
        } else if ("event".equals(target)) {
            artframework.sts1.FullPresentMode.setEventLevel(level);
            artframework.core.UiComponent event =
                    ArtFramework.component(artframework.context.SurfaceIds.EVENT);
            if (event != null) {
                if (level.allowsFullPresent() || level.allowsObserve()) {
                    if (!event.isMounted()) {
                        event.mount();
                    }
                } else if (event.isMounted()) {
                    event.unmount();
                }
            }
        } else if ("select".equals(target)) {
            artframework.sts1.FullPresentMode.setSelectLevel(level);
            artframework.core.UiComponent grid =
                    ArtFramework.component(artframework.context.SurfaceIds.SELECT_GRID);
            artframework.core.UiComponent hand =
                    ArtFramework.component(artframework.context.SurfaceIds.SELECT_HAND);
            if (level.allowsFullPresent() || level.allowsObserve()) {
                if (grid != null) {
                    grid.action("mount_select");
                }
                if (hand != null) {
                    hand.action("mount_select");
                }
            } else {
                if (grid != null && grid.isMounted()) {
                    grid.unmount();
                }
                if (hand != null && hand.isMounted()) {
                    hand.unmount();
                }
            }
        } else if ("reward".equals(target)) {
            artframework.sts1.FullPresentMode.setRewardLevel(level);
            mountPresentAction(
                    artframework.context.SurfaceIds.REWARD_COMBAT, "mount_reward", level);
            mountPresentAction(artframework.context.SurfaceIds.REWARD_CARD, "mount_reward", level);
            mountPresentAction(
                    artframework.context.SurfaceIds.REWARD_BOSS_RELIC, "mount_reward", level);
        } else if ("rest".equals(target)) {
            artframework.sts1.FullPresentMode.setRestLevel(level);
            mountPresentAction(artframework.context.SurfaceIds.REST, "mount_rest", level);
        } else if ("treasure".equals(target)) {
            artframework.sts1.FullPresentMode.setTreasureLevel(level);
            mountPresentAction(artframework.context.SurfaceIds.TREASURE, "mount_treasure", level);
        } else if ("shop".equals(target)) {
            artframework.sts1.FullPresentMode.setShopLevel(level);
            mountPresentAction(artframework.context.SurfaceIds.SHOP, "mount_shop", level);
        } else if ("top".equals(target) || "toppanel".equals(target)) {
            artframework.sts1.FullPresentMode.setTopPanelLevel(level);
            mountPresentAction(artframework.context.SurfaceIds.TOP_PANEL, "mount_top_panel", level);
        } else if ("intents".equals(target)) {
            artframework.sts1.FullPresentMode.setIntentsLevel(level);
            mountPresentAction(artframework.context.SurfaceIds.COMBAT_INTENTS, "mount_intents", level);
        } else if ("targeting".equals(target)) {
            artframework.sts1.FullPresentMode.setTargetingLevel(level);
            mountPresentAction(artframework.context.SurfaceIds.COMBAT_TARGETING, "mount_targeting", level);
        } else if ("proceed".equals(target)) {
            artframework.sts1.FullPresentMode.setProceedLevel(level);
            mountPresentAction(artframework.context.SurfaceIds.COMBAT_PROCEED, "mount_proceed", level);
        } else if ("energy".equals(target)) {
            artframework.sts1.FullPresentMode.setEnergyLevel(level);
            mountPresentAction(artframework.context.SurfaceIds.COMBAT_ENERGY, "mount_energy", level);
        } else {
            artframework.sts1.FullPresentMode.setSkeletonLevel(level);
            artframework.core.UiComponent sk =
                    ArtFramework.component(artframework.context.SurfaceIds.SKELETON);
            if (sk != null) {
                if (level.allowsFullPresent() || level.allowsObserve()) {
                    if (!sk.isMounted()) {
                        sk.mount();
                    }
                } else if (sk.isMounted()) {
                    sk.unmount();
                }
            }
        }
        String line =
                "ART_PRESENT "
                        + target
                        + " level="
                        + level.name()
                        + " full-present="
                        + level.allowsFullPresent()
                        + " panic="
                        + artframework.sts1.PresentSafety.isPanic();
        DevConsole.log(line);
        BaseMod.logger.info(line);
    }

    private void mountPresentAction(String surfaceId, String mountAction, artframework.sts1.PresentLevel level) {
        artframework.core.UiComponent c = ArtFramework.component(surfaceId);
        if (c == null) {
            return;
        }
        if (level.allowsFullPresent() || level.allowsObserve()) {
            c.action(mountAction);
        } else if (c.isMounted()) {
            c.unmount();
        }
    }

    private void logPresentStatus() {
        java.util.Map<String, Object> policy = artframework.sts1.FullPresentMode.probeSlice();
        String line =
                "ART_PRESENT combat="
                        + policy.get("combatHand")
                        + " controls="
                        + policy.get("combatControls")
                        + " map="
                        + policy.get("map")
                        + " event="
                        + policy.get("event")
                        + " select="
                        + policy.get("select")
                        + " reward="
                        + policy.get("reward")
                        + " rest="
                        + policy.get("rest")
                        + " shop="
                        + policy.get("shop")
                        + " skeleton="
                        + policy.get("skeleton")
                        + " targeting="
                        + policy.get("targeting")
                        + " panic="
                        + policy.get("panic")
                        + " suppressHand="
                        + artframework.sts1.render.Sts1SurfaceRenderer.shouldSuppressNativeHand()
                        + " suppressMap="
                        + artframework.sts1.render.MapDrawPath.shouldSuppressNativeMap()
                        + " suppressEvent="
                        + artframework.sts1.render.EventDrawPath.shouldSuppressNativeEvent()
                        + " suppressSelect="
                        + artframework.sts1.render.SelectDrawPath.shouldSuppressNativeSelect()
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
        artframework.core.UiComponent energy =
                ArtFramework.component(artframework.context.SurfaceIds.COMBAT_ENERGY);
        if (energy != null && energy.isMounted()) {
            energy.unmount();
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
                "Usage: art fx enable|disable|tint|glow|blur|glass|lightwave|lightwave-test|diag|clear|capture [args]");
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
            } else if ("lightwave".equals(action)) {
                float intensity =
                        tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 0.55f) : 0.55f;
                float angle =
                        tokens.length > depth + 2 ? parseFloat(tokens[depth + 2], 35f) : 35f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("intensity", Float.valueOf(intensity));
                p.put("angle", Float.valueOf(angle));
                p.put("width", Float.valueOf(0.18f));
                p.put("speed", Float.valueOf(0.35f));
                ArtFramework.render()
                        .bindFullFrameEffect(artframework.render.LightwaveEffect.ID, p);
                DevConsole.log("full_frame lightwave intensity=" + intensity + " angle=" + angle);
            } else if ("lightwave-test".equals(action)) {
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("intensity", Float.valueOf(1f));
                p.put("angle", Float.valueOf(35f));
                p.put("width", Float.valueOf(0.5f));
                p.put("speed", Float.valueOf(0f));
                p.put("phase", Float.valueOf(0.5f));
                p.put("freeze", Float.valueOf(1f));
                p.put("r", Float.valueOf(1f));
                p.put("g", Float.valueOf(1f));
                p.put("b", Float.valueOf(1f));
                artframework.render.RenderStateEcs.fullFrameEffects(
                        java.util.Collections.<artframework.presentation.EffectAttachment>emptyList());
                artframework.render.RenderProjectionQueue.projectNow();
                ArtFramework.render()
                        .bindFullFrameEffect(artframework.render.LightwaveEffect.ID, p);
                DevConsole.log("full_frame lightwave diagnostic: fixed high-contrast band");
            } else if ("diag".equals(action)) {
                cmdLightwaveDiagnostic(tokens, depth + 1);
            } else if ("capture".equals(action)) {
                boolean on = tokens.length <= depth + 1
                        || !"off".equalsIgnoreCase(tokens[depth + 1]);
                ArtFramework.render().setCaptureEnabled(on);
                DevConsole.log("capture " + (on ? "on" : "off"));
            } else if ("clear".equals(action)) {
                artframework.render.RenderStateEcs.fullFrameEffects(
                        java.util.Collections.<artframework.presentation.EffectAttachment>emptyList());
                artframework.render.RenderProjectionQueue.projectNow();
                DevConsole.log("full_frame effects cleared");
            } else {
                DevConsole.log("unknown fx action: " + action);
            }
        } catch (RuntimeException e) {
            DevConsole.log("fx failed: " + e.getMessage());
        }
    }

    private void cmdLightwaveDiagnostic(String[] tokens, int depth) {
        if (tokens.length <= depth || "status".equalsIgnoreCase(tokens[depth])) {
            DevConsole.log("lightwave diag=" + artframework.render.LightwaveDiagnostics.probeSummary());
            return;
        }
        if ("reset".equalsIgnoreCase(tokens[depth])) {
            artframework.render.LightwaveDiagnostics.resetForTests();
            DevConsole.log("lightwave diag reset");
            return;
        }
        if (tokens.length <= depth + 1) {
            DevConsole.log("Usage: art fx diag <c2|items|panels|fallback> <on|off>|status|reset");
            return;
        }
        boolean on = !"off".equalsIgnoreCase(tokens[depth + 1]);
        String part = tokens[depth].toLowerCase();
        if ("c2".equals(part)) {
            artframework.render.LightwaveDiagnostics.setC2EffectsEnabled(on);
        } else if ("items".equals(part)) {
            artframework.render.LightwaveDiagnostics.setC2ItemsEnabled(on);
        } else if ("panels".equals(part)) {
            artframework.render.LightwaveDiagnostics.setC2PanelsEnabled(on);
        } else if ("fallback".equals(part)) {
            artframework.render.LightwaveDiagnostics.setForceFallback(on);
        } else {
            DevConsole.log("Unknown lightwave diag part: " + part);
            return;
        }
        DevConsole.log("lightwave diag " + part + "=" + on);
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
            if (tokens.length >= depth + 2 && "first".equalsIgnoreCase(tokens[depth + 1])) {
                artframework.context.IntentResult ir =
                        artframework.sts1.input.Sts1MapIntentBridge.clickFirstPresentable(
                                tokens.length > depth + 2 ? tokens[depth + 2] : "");
                if (ir == null || ir.status == artframework.context.IntentResult.Status.REJECTED) {
                    r = UiOpResult.unavailable(ir != null ? ir.message : "map first failed");
                } else {
                    r = UiOpResult.ok(ir.message);
                }
            } else if (tokens.length < depth + 3) {
                DevConsole.log("Usage: art op map <row> <col> [roomType] | art op map first [roomType]");
                return;
            } else {
                int row = parseInt(tokens[depth + 1], 0);
                int col = parseInt(tokens[depth + 2], 0);
                String room = tokens.length > depth + 3 ? tokens[depth + 3] : "";
                r = ArtFramework.ops().clickMapNode(new MapNodeRef(row, col, room));
            }
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
                "art: probe | open|bind|close <id> | gate … | ui … | lab … | fx … | profile|theme … | assets … | frame | present combat on|off | op …");
    }
}
