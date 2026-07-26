package spireui.console;

import basemod.BaseMod;
import basemod.DevConsole;
import basemod.devcommands.ConsoleCommand;
import spireui.api.SpireUI;
import spireui.api.UiOpResult;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.c2.MapNodeRef;
import spireui.c2.NativeTemplateIds;
import spireui.c2.SelectKind;
import spireui.ops.GateLab;

/**
 * BaseMod console: {@code spireui probe | open | bind | close | gate | op ...}
 */
public class SpireUiCommand extends ConsoleCommand {

    @Override
    protected void execute(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            errorMsg();
            return;
        }
        String sub = tokens[depth].toLowerCase();
        if ("probe".equals(sub)) {
            String line = SpireUI.probe().toJsonLine();
            DevConsole.log(line);
            BaseMod.logger.info(line);
            return;
        }
        if ("open".equals(sub)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: spireui open <id>");
                return;
            }
            try {
                SpireUI.open(tokens[depth + 1]);
                DevConsole.log("opened " + tokens[depth + 1]);
            } catch (RuntimeException e) {
                DevConsole.log("open failed: " + e.getMessage());
            }
            return;
        }
        if ("bind".equals(sub)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: spireui bind <id>");
                return;
            }
            try {
                ensureNativeRegistered(tokens[depth + 1]);
                SpireUI.bind(tokens[depth + 1]);
                DevConsole.log("bound " + tokens[depth + 1]);
            } catch (RuntimeException e) {
                DevConsole.log("bind failed: " + e.getMessage());
            }
            return;
        }
        if ("close".equals(sub)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: spireui close <id>");
                return;
            }
            SpireUI.close(tokens[depth + 1]);
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
        errorMsg();
    }

    private void cmdFullFrame(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: spireui fx enable|disable|tint|glow|blur|glass|clear|capture [args]");
            return;
        }
        String action = tokens[depth].toLowerCase();
        try {
            if ("enable".equals(action)) {
                float w = tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 1920f) : 1920f;
                float h = tokens.length > depth + 2 ? parseFloat(tokens[depth + 2], 1080f) : 1080f;
                SpireUI.render().enableFullFrame(w, h);
                DevConsole.log("full_frame enabled " + (int) w + "x" + (int) h);
            } else if ("disable".equals(action)) {
                SpireUI.render().disableFullFrame();
                DevConsole.log("full_frame disabled");
            } else if ("tint".equals(action)) {
                float alpha = tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 0.12f) : 0.12f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("alpha", Float.valueOf(alpha));
                SpireUI.render().bindFullFrameEffect(spireui.render.TintEffect.ID, p);
                DevConsole.log("full_frame tint alpha=" + alpha);
            } else if ("glow".equals(action)) {
                float intensity =
                        tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 0.4f) : 0.4f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("intensity", Float.valueOf(intensity));
                SpireUI.render().bindFullFrameEffect(spireui.render.GlowEffect.ID, p);
                DevConsole.log("full_frame glow intensity=" + intensity);
            } else if ("blur".equals(action)) {
                float radius =
                        tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 2.5f) : 2.5f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("radius", Float.valueOf(radius));
                SpireUI.render().setCaptureEnabled(true);
                SpireUI.render().bindFullFrameEffect(spireui.render.BlurEffect.ID, p);
                DevConsole.log("full_frame blur radius=" + radius + " (screen capture)");
            } else if ("glass".equals(action)) {
                float radius =
                        tokens.length > depth + 1 ? parseFloat(tokens[depth + 1], 2.5f) : 2.5f;
                float tint =
                        tokens.length > depth + 2 ? parseFloat(tokens[depth + 2], 0.45f) : 0.45f;
                java.util.Map<String, Object> p = new java.util.LinkedHashMap<String, Object>();
                p.put("radius", Float.valueOf(radius));
                p.put("tint", Float.valueOf(tint));
                SpireUI.render().setCaptureEnabled(true);
                SpireUI.render().bindFullFrameEffect(spireui.render.GlassEffect.ID, p);
                DevConsole.log("full_frame glass radius=" + radius + " tint=" + tint);
            } else if ("capture".equals(action)) {
                boolean on = tokens.length <= depth + 1
                        || !"off".equalsIgnoreCase(tokens[depth + 1]);
                SpireUI.render().setCaptureEnabled(on);
                DevConsole.log("capture " + (on ? "on" : "off"));
            } else if ("clear".equals(action)) {
                SpireUI.render().clearEffects(spireui.render.RenderHost.FULL_FRAME_ID);
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
                    "Usage: spireui gate <map|event|endturn|select|select-grid|select-hand|all> <block|clear>");
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
            if (!SpireUI.isRegistered(NativeTemplateIds.MAP)
                    || SpireUI.find(NativeTemplateIds.MAP) == null) {
                try {
                    SpireUI.bind(NativeTemplateIds.MAP);
                } catch (RuntimeException ignored) {
                }
            }
        }
        if ("event".equalsIgnoreCase(target) || "all".equalsIgnoreCase(target)) {
            ensureNativeRegistered(NativeTemplateIds.EVENT);
            try {
                if (SpireUI.find(NativeTemplateIds.EVENT) == null) {
                    SpireUI.bind(NativeTemplateIds.EVENT);
                }
            } catch (RuntimeException ignored) {
            }
        }
        if ("endturn".equalsIgnoreCase(target)
                || "end-turn".equalsIgnoreCase(target)
                || "all".equalsIgnoreCase(target)) {
            ensureNativeRegistered(NativeTemplateIds.END_TURN);
            try {
                if (SpireUI.find(NativeTemplateIds.END_TURN) == null) {
                    SpireUI.bind(NativeTemplateIds.END_TURN);
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
                if (SpireUI.find(NativeTemplateIds.SELECT_GRID) == null) {
                    SpireUI.bind(NativeTemplateIds.SELECT_GRID);
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
                if (SpireUI.find(NativeTemplateIds.SELECT_HAND) == null) {
                    SpireUI.bind(NativeTemplateIds.SELECT_HAND);
                }
            } catch (RuntimeException ignored) {
            }
        }
        String msg = GateLab.apply(target, action);
        DevConsole.log(msg);
        BaseMod.logger.info("SpireUI " + msg);
    }

    private void cmdOp(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log(
                    "Usage: spireui op select|confirm|map|event|endturn|play|button|slider|hitarea|click ...");
            return;
        }
        String kind = tokens[depth].toLowerCase();
        UiOpResult r;
        if ("select".equals(kind)) {
            // spireui op select grid|hand <cardId> [index]
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: spireui op select grid|hand <cardId> [index]");
                return;
            }
            SelectKind sk = "hand".equalsIgnoreCase(tokens[depth + 1]) ? SelectKind.HAND : SelectKind.GRID;
            String cardId = tokens[depth + 2];
            int index = tokens.length > depth + 3 ? parseInt(tokens[depth + 3], 0) : 0;
            r = SpireUI.ops().selectCard(sk, cardId, index);
        } else if ("confirm".equals(kind)) {
            SelectKind sk = tokens.length > depth + 1 && "hand".equalsIgnoreCase(tokens[depth + 1])
                    ? SelectKind.HAND
                    : SelectKind.GRID;
            r = SpireUI.ops().confirmSelect(sk);
        } else if ("map".equals(kind)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: spireui op map <row> <col> [roomType]");
                return;
            }
            int row = parseInt(tokens[depth + 1], 0);
            int col = parseInt(tokens[depth + 2], 0);
            String room = tokens.length > depth + 3 ? tokens[depth + 3] : "";
            r = SpireUI.ops().clickMapNode(new MapNodeRef(row, col, room));
        } else if ("event".equals(kind)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: spireui op event <index> [label...]");
                return;
            }
            int index = parseInt(tokens[depth + 1], 0);
            String label = tokens.length > depth + 2 ? join(tokens, depth + 2) : "";
            r = SpireUI.ops().chooseEventOption(index, label);
        } else if ("endturn".equals(kind)) {
            r = SpireUI.ops().pressEndTurn();
        } else if ("play".equals(kind)) {
            if (tokens.length < depth + 2) {
                DevConsole.log("Usage: spireui op play <cardId> [target]");
                return;
            }
            String target = tokens.length > depth + 2 ? tokens[depth + 2] : "";
            r = SpireUI.ops().playHandCard(tokens[depth + 1], target);
        } else if ("button".equals(kind)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: spireui op button <windowId> <buttonId>");
                return;
            }
            r = SpireUI.ops().clickButton(tokens[depth + 1], tokens[depth + 2]);
        } else if ("slider".equals(kind)) {
            if (tokens.length < depth + 4) {
                DevConsole.log("Usage: spireui op slider <windowId> <sliderId> <value>");
                return;
            }
            float value = parseFloat(tokens[depth + 3], 0f);
            r = SpireUI.ops().setSlider(tokens[depth + 1], tokens[depth + 2], value);
        } else if ("hitarea".equals(kind)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: spireui op hitarea <windowId> <hitAreaId>");
                return;
            }
            r = SpireUI.ops().clickHitArea(tokens[depth + 1], tokens[depth + 2]);
        } else if ("click".equals(kind)) {
            if (tokens.length < depth + 3) {
                DevConsole.log("Usage: spireui op click <windowId> <controlId>");
                return;
            }
            r = SpireUI.ops().click(tokens[depth + 1], tokens[depth + 2]);
        } else {
            DevConsole.log("Unknown op: " + kind);
            return;
        }
        DevConsole.log("op " + kind + " → " + r.status + (r.message.isEmpty() ? "" : " " + r.message));
        BaseMod.logger.info("SpireUI op " + kind + " " + r);
    }

    private static void ensureNativeRegistered(String id) {
        if (SpireUI.isRegistered(id)) {
            return;
        }
        if (NativeTemplateIds.MAP.equals(id)
                || NativeTemplateIds.EVENT.equals(id)
                || NativeTemplateIds.SELECT_GRID.equals(id)
                || NativeTemplateIds.SELECT_HAND.equals(id)
                || NativeTemplateIds.END_TURN.equals(id)) {
            SpireUI.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
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
                "spireui: probe | open <id> | bind <id> | close <id> | gate … | fx enable|disable|tint|glow|clear | op …");
    }
}
