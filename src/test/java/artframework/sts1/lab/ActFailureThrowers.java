package artframework.sts1.lab;

/**
 * Throw sites for {@link ActFailureMessageTest}. Each method throws from a named frame so the test
 * can assert that the reported message carries the fully-qualified thrower frame instead of a bare
 * exception name.
 */
public final class ActFailureThrowers {

    private ActFailureThrowers() {}

    /**
     * Throw from {@code nesting} recursive helper frames so the trace the message renders is
     * unambiguous about where the failure originated.
     */
    public static void throwPlainIndexOutOfBounds(int nesting) {
        if (nesting > 0) {
            throwPlainIndexOutOfBounds(nesting - 1);
            return;
        }
        throw new IndexOutOfBoundsException("index can't be >= size: 1 >= 0");
    }

    /** Real cause for the reflective-wrapper case. */
    public static void throwIllegalState() {
        throw new IllegalStateException("act jump rejected");
    }

    /** Recursion well past the rendered-frame cap. */
    public static void throwDeep(int depth) {
        if (depth <= 0) {
            throw new IllegalStateException("deep failure");
        }
        throwDeep(depth - 1);
    }

    /** Message carries every control character the console must never print. */
    public static void throwWithControlChars() {
        // \n \t \r are the ones a naive "replace CR/LF/TAB" sanitizer handles; \u0000 \u000b \u007f
        // are the ones it silently lets through into the ART_COMMAND JSON line.
        throw new IllegalStateException(
                "first line\nsecond\tline\rthird\u0000fourth\u000bfifth\u007fsixth");
    }

    /** Message far longer than the bounded output. */
    public static void throwWithLongMessage(int length) {
        StringBuilder message = new StringBuilder(length);
        while (message.length() < length) {
            message.append('x');
        }
        throw new IllegalArgumentException(message.toString());
    }

    /** Throwable whose {@link Throwable#getMessage()} is genuinely {@code null}. */
    public static void throwNullMessage() {
        throw new NullMessageThrowable();
    }

    /** Custom throwable that passes a null message and overrides nothing. */
    public static final class NullMessageThrowable extends RuntimeException {
        private static final long serialVersionUID = 1L;

        NullMessageThrowable() {
            super((String) null);
        }
    }
}
