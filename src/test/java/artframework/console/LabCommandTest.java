package artframework.console;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Pure-logic coverage for the {@code art lab enter-act} console argument parse. Mirrors the
 * {@code parseClaim}/{@code parseStance} precedent: a testable static helper with no game classes,
 * so these run headless.
 */
public class LabCommandTest {

    @Test
    public void parseLabActAcceptsNumericAct() {
        assertEquals(Integer.valueOf(2), ArtCommand.parseLabAct("2"));
        assertEquals(Integer.valueOf(3), ArtCommand.parseLabAct("3"));
        assertEquals(Integer.valueOf(4), ArtCommand.parseLabAct("4"));
        assertEquals(Integer.valueOf(99), ArtCommand.parseLabAct("99"));
    }

    @Test
    public void parseLabActTrimsSurroundingWhitespace() {
        assertEquals(Integer.valueOf(3), ArtCommand.parseLabAct(" 3 "));
        assertEquals(Integer.valueOf(2), ArtCommand.parseLabAct("\t2"));
    }

    @Test
    public void parseLabActReturnsNullForNonNumericInput() {
        assertNull(ArtCommand.parseLabAct("two"));
        assertNull(ArtCommand.parseLabAct(""));
        assertNull(ArtCommand.parseLabAct("   "));
        assertNull(ArtCommand.parseLabAct(null));
        assertNull(ArtCommand.parseLabAct("2.5"));
        assertNull(ArtCommand.parseLabAct("2x"));
    }
}
