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

/**
 * BaseMod console: {@code spireui probe | open | bind | close | op ...}
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
        if ("op".equals(sub)) {
            cmdOp(tokens, depth + 1);
            return;
        }
        errorMsg();
    }

    private void cmdOp(String[] tokens, int depth) {
        if (tokens.length <= depth) {
            DevConsole.log("Usage: spireui op select|confirm|map|event|endturn|play|button ...");
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
                "spireui: probe | open <id> | bind <id> | close <id> | op select|confirm|map|event|endturn|play|button ...");
    }
}
