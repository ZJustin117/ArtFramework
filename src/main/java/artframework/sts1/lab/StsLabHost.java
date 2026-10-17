package artframework.sts1.lab;

import artframework.api.UiOpResult;

import java.util.ArrayList;
import java.util.List;

/**
 * STS1 lab host: whitelist menu / save / char-select / abandon actions.
 * Soft-fails; schedules hitbox clicks on the GL thread when possible.
 */
public final class StsLabHost implements LabHost {

    public static final StsLabHost INSTANCE = new StsLabHost();

    /** Label for the {@link #normalizeAfterActJump} current-node step (load-bearing). */
    private static final String ACT_JUMP_STEP_CURRENT_NODE = "current node";

    /** Label for the {@link #normalizeAfterActJump} render-scene step (load-bearing). */
    private static final String ACT_JUMP_STEP_RENDER_SCENE = "render scene";

    /** Label for the {@link #normalizeAfterActJump} screen-reset step (not load-bearing). */
    private static final String ACT_JUMP_STEP_SCREEN_RESET = "screen reset";

    /** Label for the {@link #normalizeAfterActJump} map-overlay step (load-bearing). */
    private static final String ACT_JUMP_STEP_MAP_OVERLAY = "map overlay";

    /** Cause frames {@link #actFailureMessage} renders before truncating to an omission marker. */
    private static final int ACT_FAILURE_MAX_FRAMES = 8;

    /** Hard length bound for the {@link #actFailureMessage} console line, marker included. */
    private static final int ACT_FAILURE_MAX_LENGTH = 1200;

    /**
     * Budget reserved for the frame list. The exception MESSAGE is truncated first so a verbose
     * message (concatenated mod lists are common in native failures) can never squeeze the frames
     * out of the line — the frames are the whole reason this rendering exists.
     */
    private static final int ACT_FAILURE_FRAME_BUDGET = 900;

    /** Explicit end-of-text marker once a failure line hits {@link #ACT_FAILURE_MAX_LENGTH}. */
    private static final String ACT_FAILURE_TRUNCATED_MARKER = "... (truncated)";

    /**
     * Upper bound of the {@code " | ... (+N more)"} suffix, reserved while appending frames so the
     * omission marker itself is never clipped by the length cap.
     */
    private static final int ACT_FAILURE_OMISSION_MARKER_LENGTH = 32;

    private StsLabHost() {}

    /** The {@link #normalizeAfterActJump} steps in application order, for message assembly. */
    private static List<String> actJumpSteps() {
        List<String> steps = new ArrayList<String>(4);
        steps.add(ACT_JUMP_STEP_CURRENT_NODE);
        steps.add(ACT_JUMP_STEP_RENDER_SCENE);
        steps.add(ACT_JUMP_STEP_SCREEN_RESET);
        steps.add(ACT_JUMP_STEP_MAP_OVERLAY);
        return steps;
    }

    /**
     * Whether a skipped {@link #normalizeAfterActJump} step leaves the jump unable to render the new
     * act coherently. A skipped step 3 ({@code ACT_JUMP_STEP_SCREEN_RESET}) is reported but not fatal:
     * steps 1/2/4 are what make {@code renderCombatRoomBg} dispatch without the stale map painting.
     */
    private static boolean isLoadBearingActJumpStep(String label) {
        return ACT_JUMP_STEP_CURRENT_NODE.equals(label)
                || ACT_JUMP_STEP_RENDER_SCENE.equals(label)
                || ACT_JUMP_STEP_MAP_OVERLAY.equals(label);
    }

    /** Comma-join step labels for the command message. */
    private static String joinLabels(List<String> labels) {
        StringBuilder sb = new StringBuilder();
        for (String label : labels) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(label);
        }
        return sb.toString();
    }

    @Override
    public LabStateSnapshot dump() {
        return StsLabState.dump();
    }

    @Override
    public UiOpResult clearSaves() {
        try {
            Class<?> playerClass = Class.forName("com.megacrit.cardcrawl.characters.AbstractPlayer$PlayerClass");
            Object[] values = (Object[]) playerClass.getMethod("values").invoke(null);
            Class<?> save = Class.forName("com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue");
            int deleted = 0;
            for (Object pc : values) {
                String path = String.valueOf(save.getMethod("getPlayerSavePath", playerClass).invoke(null, pc));
                if (deleteLocal(path)) {
                    deleted++;
                }
                if (deleteLocal(path + ".backUp")) {
                    deleted++;
                }
            }
            return UiOpResult.ok("cleared " + deleted);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult stripResumeButtons() {
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            @SuppressWarnings("unchecked")
            List<Object> buttons = (List<Object>) StsLabState.field(menu.getClass(), menu, "buttons");
            if (buttons == null) {
                return UiOpResult.unavailable("no buttons");
            }
            boolean hasPlay = false;
            int removed = 0;
            for (int i = buttons.size() - 1; i >= 0; i--) {
                Object button = buttons.get(i);
                if (button == null) {
                    continue;
                }
                String result = clickResultName(button);
                if ("PLAY".equals(result)) {
                    hasPlay = true;
                    continue;
                }
                if ("ABANDON_RUN".equals(result) || "RESUME_GAME".equals(result)) {
                    buttons.remove(i);
                    removed++;
                }
            }
            if (removed > 0 && !hasPlay) {
                Class<?> menuButton = Class.forName("com.megacrit.cardcrawl.screens.mainMenu.MenuButton");
                Class<?> clickResult =
                        Class.forName("com.megacrit.cardcrawl.screens.mainMenu.MenuButton$ClickResult");
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object play = Enum.valueOf((Class) clickResult, "PLAY");
                Object nb =
                        menuButton
                                .getConstructor(clickResult, int.class)
                                .newInstance(play, Integer.valueOf(buttons.size()));
                buttons.add(nb);
            }
            clearStaleNativeScreens();
            return UiOpResult.ok("stripped " + removed);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult openCharSelect() {
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            final Object charSelect = StsLabState.field(menu.getClass(), menu, "charSelectScreen");
            if (charSelect == null) {
                return UiOpResult.unavailable("no char select");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        charSelect.getClass().getMethod("open", boolean.class).invoke(charSelect, Boolean.FALSE);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("char select open scheduled");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult selectCharacter(String characterId) {
        if (characterId == null || characterId.isEmpty()) {
            return UiOpResult.unavailable("character id required");
        }
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            final Object screen = StsLabState.field(menu.getClass(), menu, "charSelectScreen");
            if (screen == null) {
                return UiOpResult.unavailable("no char select");
            }
            final Object option = findCharacterOption(screen, characterId);
            if (option == null) {
                return UiOpResult.unavailable("character not found: " + characterId);
            }
            Object locked = StsLabState.field(option.getClass(), option, "locked");
            if (Boolean.TRUE.equals(locked)) {
                return UiOpResult.unavailable("character locked: " + characterId);
            }
            final Object hb = StsLabState.field(option.getClass(), option, "hb");
            if (hb == null) {
                return UiOpResult.unavailable("no character hitbox");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Class<?> input =
                                Class.forName("com.megacrit.cardcrawl.helpers.input.InputHelper");
                        Object cx = StsLabState.field(hb.getClass(), hb, "cX");
                        Object cy = StsLabState.field(hb.getClass(), hb, "cY");
                        if (cx instanceof Number) {
                            input.getField("mX").setInt(null, Math.round(((Number) cx).floatValue()));
                        }
                        if (cy instanceof Number) {
                            input.getField("mY").setInt(null, Math.round(((Number) cy).floatValue()));
                        }
                        setBoolean(hb, "clicked", true);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("character click scheduled " + characterId);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult embark() {
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            Object screen = StsLabState.field(menu.getClass(), menu, "charSelectScreen");
            if (screen == null) {
                return UiOpResult.unavailable("no char select");
            }
            final Object confirm = StsLabState.field(screen.getClass(), screen, "confirmButton");
            if (confirm == null) {
                return UiOpResult.unavailable("no embark button");
            }
            Object disabled = StsLabState.field(confirm.getClass(), confirm, "isDisabled");
            if (Boolean.TRUE.equals(disabled)) {
                return UiOpResult.unavailable("embark disabled");
            }
            final Object hb = StsLabState.field(confirm.getClass(), confirm, "hb");
            if (hb == null) {
                return UiOpResult.unavailable("no embark hitbox");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setBoolean(hb, "clicked", true);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("embark scheduled");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult setSeed(String seedText) {
        if (seedText == null || seedText.isEmpty()) {
            return UiOpResult.ok("seed skipped");
        }
        try {
            Class<?> seedHelper = Class.forName("com.megacrit.cardcrawl.helpers.SeedHelper");
            try {
                seedHelper.getMethod("setSeed", String.class).invoke(null, seedText);
                return UiOpResult.ok("seed set " + seedText);
            } catch (NoSuchMethodException e) {
                // fall through to panel
            }
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu for seed");
            }
            Object panel = StsLabState.field(menu.getClass(), menu, "seedPanel");
            if (panel == null) {
                return UiOpResult.unavailable("no seed panel");
            }
            try {
                java.lang.reflect.Field tf = findField(panel.getClass(), "textField");
                if (tf != null) {
                    tf.setAccessible(true);
                    tf.set(panel, seedText);
                }
            } catch (Throwable ignored) {
            }
            return UiOpResult.ok("seed text applied " + seedText);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult enterEvent(String eventId) {
        final String normalized = LabEventIds.normalize(eventId);
        if (normalized.isEmpty()) {
            return UiOpResult.unavailable("unsupported event: " + eventId);
        }
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object player = StsLabState.field(dungeon, null, "player");
            if (player == null) {
                return UiOpResult.unavailable("run not ready");
            }
            Object node = StsLabState.field(dungeon, null, "currMapNode");
            if (node == null) {
                return UiOpResult.unavailable("current map node unavailable");
            }
            Object currentRoom = node.getClass().getMethod("getRoom").invoke(node);
            if (currentRoom != null) {
                Object currentPhase = StsLabState.field(currentRoom.getClass(), currentRoom, "phase");
                Object battleOver = StsLabState.field(currentRoom.getClass(), currentRoom, "isBattleOver");
                if (currentPhase != null
                        && "COMBAT".equals(String.valueOf(currentPhase))
                        && !Boolean.TRUE.equals(battleOver)) {
                    return UiOpResult.unavailable("cannot enter event during live combat");
                }
            }
            Class<?> roomClass = Class.forName("com.megacrit.cardcrawl.rooms.EventRoom");
            final Object room = roomClass.getConstructor().newInstance();
            Object event =
                    Class.forName("com.megacrit.cardcrawl.helpers.EventHelper")
                            .getMethod("getEvent", String.class)
                            .invoke(null, normalized);
            if (event == null) {
                return UiOpResult.unavailable("event unavailable: " + normalized);
            }
            StsLabState.setField(room.getClass(), room, "event", event);
            Class<?> phase = Class.forName("com.megacrit.cardcrawl.rooms.AbstractRoom$RoomPhase");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object eventPhase = Enum.valueOf((Class) phase, "EVENT");
            StsLabState.setField(room.getClass(), room, "phase", eventPhase);
            StsLabState.setField(node.getClass(), node, "room", room);
            Class<?> dungeonClass = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            StsLabState.setField(dungeonClass, null, "screen", null);
            StsLabState.setField(dungeonClass, null, "isScreenUp", Boolean.FALSE);
            event.getClass().getMethod("onEnterRoom").invoke(event);
            return UiOpResult.ok("event entered " + normalized);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    /** Enter a reachable native rest, shop, or treasure room through the map gesture path. */
    public UiOpResult enterRoom(String roomKind) {
        if (roomKind == null || roomKind.trim().isEmpty()) {
            return UiOpResult.unavailable("room required");
        }
        return LabNavigationSignals.dispatch(
                LabNavigationIntent.of(LabIntentNames.ENTER_ROOM, roomKind));
    }

    /** Open a native selection screen over the run's actual cards. */
    public UiOpResult enterSelect(String selectKind) {
        if (selectKind == null || selectKind.trim().isEmpty()) {
            return UiOpResult.unavailable("select kind required");
        }
        return LabNavigationSignals.dispatch(
                LabNavigationIntent.of(LabIntentNames.ENTER_SELECT, selectKind));
    }

    /**
     * Jump the live run to a supported non-Exordium act through the native dungeon-construction
     * path ({@code CardCrawlGame.getDungeon}). ACT-1 is deliberately out of scope.
     */
    @Override
    public UiOpResult enterAct(int act) {
        String key = actKey(act);
        if (key == null) {
            return UiOpResult.unavailable("unsupported act: " + act + " (supported acts: 2, 3, 4)");
        }
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object player = StsLabState.field(dungeon, null, "player");
            if (player == null) {
                return UiOpResult.unavailable("run not ready");
            }
            Object game = gameListener();
            if (game == null) {
                return UiOpResult.unavailable("card crawl game unavailable");
            }
            Class<?> gameClass = Class.forName("com.megacrit.cardcrawl.core.CardCrawlGame");
            java.lang.reflect.Method getDungeon =
                    gameClass.getMethod(
                            "getDungeon",
                            String.class,
                            Class.forName("com.megacrit.cardcrawl.characters.AbstractPlayer"));
            getDungeon.invoke(game, key, player);
            // getDungeon already mutated global run state (CardCrawlGame.dungeon/id/scene/map and,
            // for TheCity, the BGM). Normalize the live presentation so the new act actually
            // renders; each step below is reflective and individually guarded so one missing
            // field cannot leave the jump half-applied.
            List<String> skipped = normalizeAfterActJump(dungeon);
            List<String> skippedLoadBearing = new ArrayList<String>(skipped.size());
            for (String step : skipped) {
                if (isLoadBearingActJumpStep(step)) {
                    skippedLoadBearing.add(step);
                }
            }
            if (!skippedLoadBearing.isEmpty()) {
                // A load-bearing step (current node, render scene, or map overlay) did not apply, so
                // renderCombatRoomBg will not dispatch for the new act (or the stale map still
                // paints). Reporting plain success here would make the console lie while the
                // scenario's artDrawCount delta cannot move.
                return UiOpResult.unavailable(
                        "act "
                                + act
                                + " "
                                + key
                                + " entered but not renderable: skipped "
                                + joinLabels(skippedLoadBearing));
            }
            List<String> applied = actJumpSteps();
            applied.removeAll(skipped);
            return UiOpResult.ok(
                    "act "
                            + act
                            + " "
                            + key
                            + " entered (applied "
                            + joinLabels(applied)
                            + ")"
                            + (skipped.isEmpty() ? "" : "; " + joinLabels(skipped) + " skipped"));
        } catch (Throwable t) {
            return UiOpResult.unavailable(actFailureMessage(t));
        }
    }

    /**
     * Post-construction normalization for a native act jump. Native {@code getDungeon} builds a new
     * dungeon while the live presentation still holds the previous act's map overlay, render scene,
     * and current node, so mirror the reset/room-transition steps native itself performs. Every
     * step is reflective and guarded: a missing field or method must not abort the jump (which has
     * already committed global run state), and nothing here may throw.
     *
     * @return the labels of the steps that did not apply, in application order; empty when the jump
     *     applied every step. {@link #enterAct} classifies them with
     *     {@link #isLoadBearingActJumpStep(String)}: a skipped load-bearing step makes the jump
     *     unable to render the new act, while a skipped screen reset is only reported.
     */
    private static List<String> normalizeAfterActJump(Class<?> dungeon) {
        List<String> skipped = new ArrayList<String>(4);
        // 1. Current node. Only TheCity resets currMapNode, and both AbstractDungeon.render and
        // AbstractDungeon.update read currMapNode.room, so acts 3/4 would otherwise keep rendering
        // and updating the previous act's MonsterRoom against the fresh actionManager. Mirror
        // TheCity exactly: a fresh (0, -1) node holding a fresh EmptyRoom.
        try {
            Class<?> mapRoomNode = Class.forName("com.megacrit.cardcrawl.map.MapRoomNode");
            Object node =
                    mapRoomNode
                            .getConstructor(int.class, int.class)
                            .newInstance(Integer.valueOf(0), Integer.valueOf(-1));
            Object empty =
                    Class.forName("com.megacrit.cardcrawl.rooms.EmptyRoom")
                            .getConstructor()
                            .newInstance();
            StsLabState.setField(mapRoomNode, node, "room", empty);
            StsLabState.setField(dungeon, null, "currMapNode", node);
        } catch (Throwable ignored) {
            skipped.add(ACT_JUMP_STEP_CURRENT_NODE);
        }
        // 2. Render scene. AbstractDungeon.render dispatches scene.renderCombatRoomBg only when
        // rs == RenderScene.NORMAL, so a jump out of an EVENT/CAMPFIRE state would never draw the
        // new act's layers. Native sets rs = RenderScene.NORMAL on reset, and on room transition
        // for a non-campfire/non-event room (AbstractDungeon.nextRoomTransition sets CAMPFIRE for a
        // RestRoom, EVENT for an image event, else NORMAL); this jump targets a fresh EmptyRoom,
        // for which NORMAL is the native-correct value.
        try {
            Class<?> renderScene =
                    Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon$RenderScene");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object normal = Enum.valueOf((Class) renderScene, "NORMAL");
            StsLabState.setField(dungeon, null, "rs", normal);
        } catch (Throwable ignored) {
            skipped.add(ACT_JUMP_STEP_RENDER_SCENE);
        }
        // 3. Screen state. The non-Exordium dungeon constructor leaves screen = MAP / isScreenUp;
        // clear it so the map screen does not stay logically open over the new act. previousScreen
        // must be null, not CurrentScreen.NONE: native AbstractDungeon.reset() and
        // nextRoomTransition set it to null, and a non-null sentinel would make a later native
        // closeCurrentScreen() take its previousScreen != null branch and leave isScreenUp sticky.
        // Unlike steps 1/2/4 this step is not load-bearing, so a failure is reported, not fatal.
        try {
            Class<?> currentScreen =
                    Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon$CurrentScreen");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object none = Enum.valueOf((Class) currentScreen, "NONE");
            StsLabState.setField(dungeon, null, "screen", none);
            StsLabState.setField(dungeon, null, "previousScreen", null);
            StsLabState.setField(dungeon, null, "nextRoom", null);
            StsLabState.setField(dungeon, null, "isScreenUp", Boolean.FALSE);
        } catch (Throwable ignored) {
            skipped.add(ACT_JUMP_STEP_SCREEN_RESET);
        }
        // 4. Map overlay visibility. AbstractDungeon.render still dispatches
        // dungeonMapScreen.render(sb) for screen == NONE, and native nextRoomTransition calls
        // dungeonMapScreen.closeInstantly(). Without it, a jump taken while the map is open keeps
        // targetAlpha/baseMapColor.a at 1 and the stale map paints over the new act's ART layers.
        try {
            Object mapScreen = StsLabState.field(dungeon, null, "dungeonMapScreen");
            if (mapScreen != null) {
                mapScreen.getClass().getMethod("closeInstantly").invoke(mapScreen);
            }
        } catch (Throwable ignored) {
            skipped.add(ACT_JUMP_STEP_MAP_OVERLAY);
        }
        return skipped;
    }

    /**
     * Name the true failure for {@code enterAct}. The shared {@link #unavailable(Throwable)} helper
     * keeps its wrapper-sensitive behavior; this unwraps the reflective wrapper exceptions so the
     * console does not print a bare {@code InvocationTargetException} with a null message. The
     * rendered text carries a bounded set of fully-qualified cause frames so a native throw site
     * stays diagnosable from the single console line, and it is sanitized to one line bounded in
     * length. Package-visible so {@code ActFailureMessageTest} can pin this rendering contract.
     */
    static String actFailureMessage(Throwable t) {
        if (t == null) {
            return "null failure";
        }
        Throwable cause = t;
        while (cause.getCause() != null && isReflectiveWrapper(cause)) {
            cause = cause.getCause();
        }
        StringBuilder sb = new StringBuilder(256);
        sb.append(sanitizeActFailureText(cause.getClass().getSimpleName()));
        String message = cause.getMessage();
        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(sanitizeActFailureText(message));
        }
        // Truncate a verbose MESSAGE before appending frames: the frame list is the diagnostic
        // payload, so it gets a reserved budget instead of whatever is left after the message.
        int headBudget = ACT_FAILURE_MAX_LENGTH - ACT_FAILURE_FRAME_BUDGET;
        if (sb.length() > headBudget) {
            sb.setLength(safeCutIndex(sb, headBudget - ACT_FAILURE_TRUNCATED_MARKER.length()));
            sb.append(ACT_FAILURE_TRUNCATED_MARKER);
        }
        // Count the total printable frames first so the omission marker is always exact, even when a
        // trailing frame is dropped for overflow.
        int totalFrames = 0;
        StackTraceElement[] frames = cause.getStackTrace();
        if (frames != null) {
            for (StackTraceElement frame : frames) {
                if (!isReflectiveFrame(frame.getClassName())) {
                    totalFrames++;
                }
            }
        }
        // Reserve room for the marker itself so a full frame list cannot push it over the cap.
        int markerReserve = ACT_FAILURE_OMISSION_MARKER_LENGTH;
        int emitted = 0;
        if (frames != null) {
            for (StackTraceElement frame : frames) {
                if (isReflectiveFrame(frame.getClassName())) {
                    continue;
                }
                if (emitted >= ACT_FAILURE_MAX_FRAMES) {
                    break;
                }
                String rendered = " | " + sanitizeActFailureText(renderFrame(frame));
                // Keep each frame whole: drop trailing frames rather than cutting one in half.
                if (sb.length() + rendered.length() + markerReserve
                        > ACT_FAILURE_MAX_LENGTH - ACT_FAILURE_TRUNCATED_MARKER.length()) {
                    break;
                }
                sb.append(rendered);
                emitted++;
            }
        }
        int omitted = totalFrames - emitted;
        if (omitted > 0) {
            sb.append(" | ... (+").append(omitted).append(" more)");
        }
        // Defensive final bound: with the head budget and the marker reserve above, the rendered
        // line tops out well under the cap, so this clamp is not expected to run. It stays as the
        // single last resort in case a future edit changes the budgets.
        if (sb.length() > ACT_FAILURE_MAX_LENGTH) {
            sb.setLength(
                    safeCutIndex(sb, ACT_FAILURE_MAX_LENGTH - ACT_FAILURE_TRUNCATED_MARKER.length()));
            sb.append(ACT_FAILURE_TRUNCATED_MARKER);
        }
        return sb.toString();
    }

    /**
     * Cut {@code sb} to at most {@code limit} chars without splitting a surrogate pair, so the
     * UTF-8 log line never carries a lone surrogate.
     */
    private static int safeCutIndex(StringBuilder sb, int limit) {
        if (limit <= 0) {
            return 0;
        }
        if (limit >= sb.length()) {
            return sb.length();
        }
        return Character.isLowSurrogate(sb.charAt(limit)) ? limit - 1 : limit;
    }

    /** One cause frame as {@code Class.method(File.java:NN)}, without the {@code at } prefix. */
    private static String renderFrame(StackTraceElement frame) {
        String rendered = frame.toString();
        return rendered.startsWith("at ") ? rendered.substring(3) : rendered;
    }

    /** Collapse control characters so the console line stays one line and its JSON stays valid. */
    private static String sanitizeActFailureText(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Every C0 control (and DEL) would either break the line or make InspectJson's
            // quote() output unparseable; InspectJson escapes only " \ \n \r \t.
            sb.append(c < 0x20 || c == 0x7f ? ' ' : c);
        }
        return sb.toString();
    }

    /** JVM reflection plumbing frames add no location information to a failure line. */
    private static boolean isReflectiveFrame(String className) {
        return className != null
                && (className.startsWith("java.lang.reflect.")
                        || className.startsWith("sun.reflect.")
                        || className.startsWith("jdk.internal.reflect."));
    }

    private static boolean isReflectiveWrapper(Throwable t) {
        return t instanceof java.lang.reflect.InvocationTargetException
                || t instanceof ExceptionInInitializerError;
    }

    /** Native dungeon key for a supported act, or null when the act is out of scope. */
    static String actKey(int act) {
        if (act == 2) {
            return "TheCity";
        }
        if (act == 3) {
            return "TheBeyond";
        }
        if (act == 4) {
            return "TheEnding";
        }
        return null;
    }

    /** The process {@code ApplicationListener}; {@code CardCrawlGame} is the Gdx listener. */
    private static Object gameListener() {
        try {
            Class<?> gdx = Class.forName("com.badlogic.gdx.Gdx");
            Object app = gdx.getField("app").get(null);
            if (app != null) {
                return app.getClass().getMethod("getApplicationListener").invoke(app);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public UiOpResult menuClick(String clickResult) {
        if (clickResult == null || clickResult.isEmpty()) {
            return UiOpResult.unavailable("ClickResult required");
        }
        try {
            Object menu = mainMenu();
            if (menu == null) {
                return UiOpResult.unavailable("no main menu");
            }
            @SuppressWarnings("unchecked")
            List<Object> buttons = (List<Object>) StsLabState.field(menu.getClass(), menu, "buttons");
            if (buttons == null) {
                return UiOpResult.unavailable("no buttons");
            }
            String want = clickResult.toUpperCase();
            for (Object button : buttons) {
                if (button == null) {
                    continue;
                }
                if (!want.equals(clickResultName(button))) {
                    continue;
                }
                final Object hb = StsLabState.field(button.getClass(), button, "hb");
                if (hb == null) {
                    return UiOpResult.unavailable("no hitbox for " + want);
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setBoolean(hb, "clicked", true);
                        } catch (Throwable ignored) {
                        }
                    }
                });
                return UiOpResult.ok("menu-click scheduled " + want);
            }
            return UiOpResult.unavailable("button not found: " + want);
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult abandon() {
        try {
            LabStateSnapshot s = dump();
            if (s.abandonConfirmOpen) {
                return abandonConfirm();
            }
            if (s.onMainMenu() && s.hasAbandon) {
                return menuClick("ABANDON_RUN");
            }
            if (s.endScreen) {
                return returnToMenu();
            }
            if (!s.inGame) {
                if (s.onMainMenu()) {
                    return UiOpResult.ok("already menu");
                }
                return UiOpResult.unavailable("not in run");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Class.forName("com.megacrit.cardcrawl.core.CardCrawlGame")
                                .getMethod("startOver")
                                .invoke(null);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("start-over scheduled");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult abandonConfirm() {
        try {
            Object menu = mainMenu();
            if (menu != null) {
                Object popup = StsLabState.field(menu.getClass(), menu, "abandonPopup");
                if (popup == null) {
                    popup = StsLabState.field(menu.getClass(), menu, "confirmPopup");
                }
                if (popup != null) {
                    Object yes = StsLabState.field(popup.getClass(), popup, "yesHb");
                    if (yes == null) {
                        yes = StsLabState.field(popup.getClass(), popup, "confirmHb");
                    }
                    if (yes == null) {
                        Object yesBtn = StsLabState.field(popup.getClass(), popup, "yes");
                        if (yesBtn != null) {
                            yes = StsLabState.field(yesBtn.getClass(), yesBtn, "hb");
                        }
                    }
                    if (yes != null) {
                        final Object hb = yes;
                        post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    setBoolean(hb, "clicked", true);
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                        return UiOpResult.ok("abandon confirm scheduled");
                    }
                    try {
                        popup.getClass().getMethod("yes").invoke(popup);
                        return UiOpResult.ok("abandon confirm yes()");
                    } catch (Throwable ignored) {
                    }
                }
            }
            return menuClick("ABANDON_RUN");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult returnToMenu() {
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object death = StsLabState.field(dungeon, null, "deathScreen");
            if (death != null) {
                Object btn = StsLabState.field(death.getClass(), death, "returnButton");
                if (btn == null) {
                    btn = StsLabState.field(death.getClass(), death, "button");
                }
                if (btn != null) {
                    final Object hb = StsLabState.field(btn.getClass(), btn, "hb");
                    if (hb != null) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    setBoolean(hb, "clicked", true);
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                        return UiOpResult.ok("return-menu scheduled");
                    }
                }
            }
            return UiOpResult.unavailable("no return-to-menu control");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    @Override
    public UiOpResult proceed() {
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object overlay = StsLabState.field(dungeon, null, "overlayMenu");
            if (overlay == null) {
                return UiOpResult.unavailable("no overlayMenu");
            }
            Object proceed = StsLabState.field(overlay.getClass(), overlay, "proceedButton");
            if (proceed == null) {
                return UiOpResult.unavailable("no proceed button");
            }
            final Object hb = StsLabState.field(proceed.getClass(), proceed, "hb");
            if (hb == null) {
                return UiOpResult.unavailable("no proceed hitbox");
            }
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setBoolean(hb, "clicked", true);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return UiOpResult.ok("proceed scheduled");
        } catch (Throwable t) {
            return unavailable(t);
        }
    }

    private static Object mainMenu() throws Exception {
        Class<?> game = Class.forName("com.megacrit.cardcrawl.core.CardCrawlGame");
        return StsLabState.field(game, null, "mainMenuScreen");
    }

    /**
     * A previous lab run can leave a native select/map screen alive after STS returns to the menu.
     * Clear only presentation-screen state here; the fresh-menu recipe already owns save cleanup.
     */
    private static void clearStaleNativeScreens() {
        try {
            Class<?> dungeon = Class.forName("com.megacrit.cardcrawl.dungeons.AbstractDungeon");
            Object grid = StsLabState.field(dungeon, null, "gridSelectScreen");
            if (grid != null) {
                try {
                    grid.getClass().getMethod("hide").invoke(grid);
                } catch (Throwable ignored) {
                }
            }
            Object hand = StsLabState.field(dungeon, null, "handCardSelectScreen");
            if (hand != null) {
                StsLabState.setField(hand.getClass(), hand, "numSelected", Integer.valueOf(0));
            }
            StsLabState.setField(dungeon, null, "screen", null);
            StsLabState.setField(dungeon, null, "previousScreen", null);
            StsLabState.setField(dungeon, null, "nextRoom", null);
            StsLabState.setField(dungeon, null, "isScreenUp", Boolean.FALSE);
        } catch (Throwable ignored) {
        }
    }

    private static String clickResultName(Object button) throws Exception {
        Object result = StsLabState.field(button.getClass(), button, "result");
        if (result == null) {
            return "";
        }
        String name = String.valueOf(result);
        if (name.contains(".")) {
            name = name.substring(name.lastIndexOf('.') + 1);
        }
        return name;
    }

    private static Object findCharacterOption(Object screen, String characterId) throws Exception {
        List<Object> options = new ArrayList<Object>();
        Object visible = StsLabState.field(screen.getClass(), screen, "options");
        Object all = null;
        try {
            all = StsLabState.field(screen.getClass(), screen, "allOptions");
        } catch (Throwable ignored) {
        }
        if (all instanceof List) {
            for (Object o : (List<?>) all) {
                options.add(o);
            }
        } else if (visible instanceof List) {
            for (Object o : (List<?>) visible) {
                options.add(o);
            }
        }
        String want = characterId.trim();
        for (Object opt : options) {
            if (opt == null) {
                continue;
            }
            String desc = StsLabState.describeCharacter(opt);
            if (desc.equalsIgnoreCase(want) || desc.toUpperCase().contains(want.toUpperCase())) {
                // page if needed
                pageToOption(screen, options, opt);
                return opt;
            }
            Object name = StsLabState.field(opt.getClass(), opt, "name");
            if (name != null && String.valueOf(name).equalsIgnoreCase(want)) {
                pageToOption(screen, options, opt);
                return opt;
            }
        }
        return null;
    }

    private static void pageToOption(Object screen, List<Object> allOptions, Object target) {
        try {
            int index = allOptions.indexOf(target);
            if (index < 0) {
                return;
            }
            int per = 4;
            try {
                Object v = StsLabState.field(screen.getClass(), screen, "optionsPerIndex");
                if (v instanceof Number) {
                    per = ((Number) v).intValue();
                }
            } catch (Throwable ignored) {
            }
            if (per <= 0) {
                return;
            }
            int targetPage = index / per;
            int current = 0;
            try {
                Object v = StsLabState.field(screen.getClass(), screen, "selectIndex");
                if (v instanceof Number) {
                    current = ((Number) v).intValue();
                }
            } catch (Throwable ignored) {
            }
            java.lang.reflect.Method setCurrent =
                    screen.getClass().getDeclaredMethod("setCurrentOptions", boolean.class);
            setCurrent.setAccessible(true);
            while (current < targetPage) {
                setCurrent.invoke(screen, Boolean.TRUE);
                current++;
            }
            while (current > targetPage) {
                setCurrent.invoke(screen, Boolean.FALSE);
                current--;
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean deleteLocal(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        try {
            Class<?> gdx = Class.forName("com.badlogic.gdx.Gdx");
            Object files = gdx.getField("files").get(null);
            Object handle = files.getClass().getMethod("local", String.class).invoke(files, path);
            Boolean exists = (Boolean) handle.getClass().getMethod("exists").invoke(handle);
            if (exists != null && exists.booleanValue()) {
                Boolean deleted = (Boolean) handle.getClass().getMethod("delete").invoke(handle);
                return deleted != null && deleted.booleanValue();
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void setBoolean(Object instance, String name, boolean value) throws Exception {
        Class<?> c = instance.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.setBoolean(instance, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void post(Runnable r) {
        try {
            Class<?> gdx = Class.forName("com.badlogic.gdx.Gdx");
            Object app = gdx.getField("app").get(null);
            if (app != null) {
                app.getClass().getMethod("postRunnable", Runnable.class).invoke(app, r);
                return;
            }
        } catch (Throwable ignored) {
        }
        r.run();
    }

    @Override
    public void yieldFrame() {
        // Console/recipe runs on a non-render thread; postRunnable work needs real frames.
        try {
            Thread.sleep(350L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static UiOpResult unavailable(Throwable t) {
        return UiOpResult.unavailable(
                t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
    }
}
