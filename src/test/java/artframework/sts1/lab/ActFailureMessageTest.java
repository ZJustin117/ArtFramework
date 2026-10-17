package artframework.sts1.lab;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Slice L02: {@code art lab enter-act} failures must be diagnosable from the console line alone.
 *
 * <p>A device run once reported only {@code enter-act UNAVAILABLE IndexOutOfBoundsException: index
 * can't be >= size: 1 >= 0} with no stack frames anywhere in {@code latest.log}, so the throw site
 * was unrecoverable. The contract pinned here is the rendered failure string: simple name and
 * message, then a bounded set of fully-qualified frames, then an explicit omission marker, all on
 * one line and under a fixed length.
 *
 * <p>These tests reach the same-package seam directly:
 * {@code StsLabHost.actFailureMessage(Throwable)} is package-visible so the rendering contract can be
 * pinned without going through a live act jump. (That visibility was itself the Red step of this
 * slice; it is now part of the shipped shape.)
 */
public class ActFailureMessageTest {

    private static final int MAX_FRAMES = 8;
    private static final int MAX_LENGTH = 1200;

    /** Rendered frame: {@code pkg.Class.method(File.java:42)} with no leading "at ". */
    private static final Pattern RENDERED_FRAME =
            Pattern.compile("[\\w.$]+\\([\\w$]+\\.java:\\d+\\)");

    private static final Pattern REFLECTIVE_FRAME =
            Pattern.compile("^(java\\.lang\\.reflect|sun\\.reflect|jdk\\.internal\\.reflect)\\.");

    @Test
    public void plainThrowableNamesThrowerWithFrames() {
        IndexOutOfBoundsException failure = failureFromPlainThrow();

        String message = StsLabHost.actFailureMessage(failure);

        assertNotNull(message);
        assertTrue(
                "prefix must name the thrower and keep the native message, was: " + message,
                message.startsWith(
                        "IndexOutOfBoundsException: index can't be >= size: 1 >= 0"));
        assertTrue(
                "frames must be joined with the documented separator, was: " + message,
                message.contains(" | "));
        assertTrue(
                "the helper frame must be reported fully qualified, was: " + message,
                message.contains(
                        "artframework.sts1.lab.ActFailureThrowers.throwPlainIndexOutOfBounds("));
        assertTrue(
                "the helper frame must carry a source line number, was: " + message,
                message.contains(
                        "ActFailureThrowers.throwPlainIndexOutOfBounds(ActFailureThrowers.java:"));
        assertTrue(
                "the reported text must contain the thrower class, was: " + message,
                message.contains("ActFailureThrowers."));
        assertFalse(
                "rendered frames must not keep StackTraceElement's 'at ' prefix, was: " + message,
                message.contains(" | at "));
    }

    @Test
    public void invocationTargetCauseIsReportedNotWrapper() throws Exception {
        InvocationTargetException wrapper = invocationTargetFromHelper();
        assertEquals(
                "test setup must catch the reflective wrapper, not the cause",
                InvocationTargetException.class,
                wrapper.getClass());

        String message = StsLabHost.actFailureMessage(wrapper);

        assertTrue(
                "the cause class must be reported, was: " + message,
                message.contains("IllegalStateException"));
        assertTrue(
                "the cause message must survive unwrapping, was: " + message,
                message.contains("act jump rejected"));
        assertTrue(
                "the cause's own helper frame must be reported, was: " + message,
                message.contains(
                        "artframework.sts1.lab.ActFailureThrowers.throwIllegalState("));
        assertFalse(
                "the reflective wrapper must not be the reported failure, was: " + message,
                message.contains("InvocationTargetException"));
        assertFalse(
                "reflection plumbing frames add no location and must be filtered, was: " + message,
                message.contains("java.lang.reflect."));
    }

    @Test
    public void emptyMessageFallsBackToSimpleNameLikeNull() {
        // L02-01: the code treats an EMPTY message like a missing one (no trailing "': '"), but only
        // the null case was asserted. An empty string must render the same shape as null.
        IllegalStateException failure = new IllegalStateException("");

        String message = StsLabHost.actFailureMessage(failure);

        assertTrue(
                "an empty message must not render a dangling separator, was: " + message,
                message.startsWith("IllegalStateException"));
        assertFalse(
                "an empty message must not render ': ', was: " + message,
                message.startsWith("IllegalStateException: "));
        assertFalse(
                "no newline may survive, was: " + message,
                message.indexOf('\n') >= 0);
    }

    /**
     * L02-02: the cap must keep the INNERMOST frames (the throw site), not the outermost ones. A
     * count-only assertion would still pass an implementation that kept the caller chain, which is
     * the one thing the message exists to avoid.
     */
    @Test
    public void capKeepsInnermostFramesNotTheCallerChain() {
        IllegalStateException failure = deepFailure();

        String message = StsLabHost.actFailureMessage(failure);

        assertTrue(
                "the throw site frame must be kept, was: " + message,
                message.contains("ActFailureThrowers.throwDeep(ActFailureThrowers.java:"));
        // The kept frames must be the INNERMOST ones, in throw order: the FIRST rendered frame is the
        // exception's own innermost frame, not a caller further up the stack.
        int marker = message.indexOf(" | ... (+");
        assertTrue("the omission marker must be present, was: " + message, marker > 0);
        String keptFrames = message.substring(0, marker);
        String innermost = renderFrameForTest(failure.getStackTrace()[0]);
        String firstRendered = keptFrames.substring(keptFrames.indexOf(" | ") + 3);
        assertTrue(
                "the FIRST kept frame must be the innermost throw site "
                        + innermost
                        + ", was: "
                        + firstRendered,
                firstRendered.startsWith(innermost));
        // The test method that CALLED the thrower is deeper in the stack than the cap allows, so it
        // must be one of the omitted frames rather than a rendered one.
        assertFalse(
                "outermost caller frames must be omitted, was: " + message,
                message.contains("ActFailureMessageTest.deepFailure(ActFailureMessageTest.java:"));
    }

    /**
     * L02-01 (review): a verbose MESSAGE must not squeeze the frames out. The frames are the whole
     * diagnostic payload, so the message is truncated first and the thrower frame must survive.
     */
    @Test
    public void longMessageStillRendersTheThrowSite() {
        IllegalArgumentException failure = longMessageFailure(4000);

        String message = StsLabHost.actFailureMessage(failure);

        assertTrue(
                "a bounded line is still required, was length " + message.length(),
                message.length() <= MAX_LENGTH);
        assertTrue(
                "the throw site must survive a verbose message, was: " + message,
                message.contains("ActFailureThrowers.throwWithLongMessage(ActFailureThrowers.java:"));
        assertFalse(
                "no newline may survive, was: " + message,
                message.indexOf('\n') >= 0);
    }

    /**
     * L02-02/L02-03 (review): the omission marker must count EVERY filtered-but-unrendered frame,
     * including frames dropped because a long frame would overflow, and it must survive the cap.
     *
     * <p>The default `throwDeep(30)` frames are short (~85 chars), so 8 of them fit and the
     * overflow-drop path never runs — a count-only assertion on that fixture cannot distinguish
     * `totalFrames - emitted` from the old `kept - MAX_FRAMES`. This test installs a SYNTHETIC trace
     * of long class names so only a few frames fit, forcing the drop path.
     */
    @Test
    public void omissionMarkerCountsEveryUnrenderedFrame() {
        IllegalStateException failure = new IllegalStateException("synthetic deep failure");
        StackTraceElement[] synthetic = new StackTraceElement[12];
        StringBuilder longName = new StringBuilder("artframework.synthetic");
        while (longName.length() < 240) {
            longName.append(".verylongpackagesegment");
        }
        for (int i = 0; i < synthetic.length; i++) {
            synthetic[i] =
                    new StackTraceElement(
                            longName.toString(), "frame" + i, "LongFile" + i + ".java", 100 + i);
        }
        failure.setStackTrace(synthetic);
        int filtered = filteredFrameCount(failure);

        String message = StsLabHost.actFailureMessage(failure);

        assertTrue(
                "the line must stay within the cap, was length " + message.length(),
                message.length() <= MAX_LENGTH);
        int rendered = renderedFrameCount(message);
        assertTrue(
                "test setup must force the overflow-drop path (fewer than "
                        + MAX_FRAMES
                        + " frames rendered), rendered="
                        + rendered,
                rendered < MAX_FRAMES);
        assertTrue(
                "the omission marker must count every unrendered filtered frame ("
                        + filtered
                        + " filtered - "
                        + rendered
                        + " rendered), was: "
                        + message,
                message.contains(" | ... (+" + (filtered - rendered) + " more)"));
    }

    @Test
    public void framesAreCappedWithMoreMarker() {
        IllegalStateException failure = deepFailure();
        int omitted = filteredFrameCount(failure) - MAX_FRAMES;
        assertTrue(
                "test setup must overflow the frame cap, filtered trace had "
                        + filteredFrameCount(failure),
                omitted > 0);

        String message = StsLabHost.actFailureMessage(failure);

        assertEquals(
                "exactly " + MAX_FRAMES + " frames must be rendered, was: " + message,
                MAX_FRAMES,
                renderedFrameCount(message));
        assertTrue(
                "the omitted-frame count must be explicit, expected (+" + omitted + " more), was: "
                        + message,
                message.contains("(+" + omitted + " more)"));
        assertTrue(
                "the omission marker must follow the rendered frames, was: " + message,
                message.contains("... (+" + omitted + " more)"));
    }

    @Test
    public void messageIsSingleLineAndSanitisesControlChars() {
        IllegalStateException failure = controlCharFailure();

        String message = StsLabHost.actFailureMessage(failure);

        assertFalse("no newline may survive, was: " + message, message.indexOf('\n') >= 0);
        assertFalse("no carriage return may survive, was: " + message, message.indexOf('\r') >= 0);
        assertFalse("no tab may survive, was: " + message, message.indexOf('\t') >= 0);
        assertTrue(
                "message text must be kept, only its control chars replaced, was: " + message,
                message.contains("first line") && message.contains("second"));
        assertTrue(
                "frames must still be reported, was: " + message,
                message.contains(
                        "artframework.sts1.lab.ActFailureThrowers.throwWithControlChars("));
        // L02-06 (review): InspectJson escapes only " \\n \\r \\t, so every other C0 control (and
        // DEL) would make the ART_COMMAND JSON line unparseable. None may survive.
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            assertTrue(
                    "control character U+" + Integer.toHexString(c) + " survived, was: " + message,
                    c >= 0x20 && c != 0x7f);
        }
    }

    @Test
    public void messageLengthIsBounded() {
        IllegalArgumentException failure = longMessageFailure(4000);

        String message = StsLabHost.actFailureMessage(failure);

        assertNotNull(message);
        assertTrue(
                "message must stay within " + MAX_LENGTH + " characters, was: " + message.length(),
                message.length() <= MAX_LENGTH);
        assertTrue(
                "truncation must be explicit, was: " + message, message.contains("(truncated)"));
        assertTrue(
                "the bounded message must still name the thrower, was: " + message,
                message.startsWith("IllegalArgumentException: "));
        assertFalse("no newline may survive, was: " + message, message.indexOf('\n') >= 0);
    }

    @Test
    public void nullMessageFallsBackToSimpleName() {
        ActFailureThrowers.NullMessageThrowable failure = nullMessageFailure();
        assertTrue("test setup requires a null message", failure.getMessage() == null);

        String message = StsLabHost.actFailureMessage(failure);

        assertNotNull(message);
        assertFalse("message must not be empty", message.isEmpty());
        assertEquals(
                "no newline may survive, was: " + message,
                -1,
                message.indexOf('\n'));
        assertEquals("no tab may survive, was: " + message, -1, message.indexOf('\t'));
        assertTrue(
                "the simple name must lead the message, was: " + message,
                message.startsWith("NullMessageThrowable"));
        assertFalse(
                "a null message must not be rendered as ': null', was: " + message,
                message.contains(": null"));
        assertTrue(
                "frames must still be reported, was: " + message,
                message.contains(
                        "artframework.sts1.lab.ActFailureThrowers.throwNullMessage("));
    }

    // -- fixtures: real throw sites so the reported frames are the thrower's own ----------------

    private static IndexOutOfBoundsException failureFromPlainThrow() {
        try {
            // Two nesting levels keep the thrower method itself on the trace.
            ActFailureThrowers.throwPlainIndexOutOfBounds(2);
            throw new AssertionError("thrower did not throw");
        } catch (IndexOutOfBoundsException e) {
            return e;
        }
    }

    private static InvocationTargetException invocationTargetFromHelper() throws Exception {
        Method thrower =
                ActFailureThrowers.class.getMethod("throwIllegalState");
        try {
            thrower.invoke(null);
            throw new AssertionError("thrower did not throw");
        } catch (InvocationTargetException e) {
            return e;
        }
    }

    private static IllegalStateException deepFailure() {
        try {
            ActFailureThrowers.throwDeep(30);
            throw new AssertionError("thrower did not throw");
        } catch (IllegalStateException e) {
            return e;
        }
    }

    private static IllegalStateException controlCharFailure() {
        try {
            ActFailureThrowers.throwWithControlChars();
            throw new AssertionError("thrower did not throw");
        } catch (IllegalStateException e) {
            return e;
        }
    }

    private static IllegalArgumentException longMessageFailure(int length) {
        try {
            ActFailureThrowers.throwWithLongMessage(length);
            throw new AssertionError("thrower did not throw");
        } catch (IllegalArgumentException e) {
            return e;
        }
    }

    private static ActFailureThrowers.NullMessageThrowable nullMessageFailure() {
        try {
            ActFailureThrowers.throwNullMessage();
            throw new AssertionError("thrower did not throw");
        } catch (ActFailureThrowers.NullMessageThrowable e) {
            return e;
        }
    }

    /** Frames the contract keeps: everything except JVM reflection plumbing. */
    private static int filteredFrameCount(Throwable t) {
        int kept = 0;
        for (StackTraceElement frame : t.getStackTrace()) {
            if (!REFLECTIVE_FRAME.matcher(frame.getClassName()).find()) {
                kept++;
            }
        }
        return kept;
    }

    private static int renderedFrameCount(String message) {
        Matcher matcher = RENDERED_FRAME.matcher(message);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** Mirror of the production rendering, used to derive the EXPECTED frame text. */
    private static String renderFrameForTest(StackTraceElement frame) {
        String rendered = frame.toString();
        return rendered.startsWith("at ") ? rendered.substring(3) : rendered;
    }
}
