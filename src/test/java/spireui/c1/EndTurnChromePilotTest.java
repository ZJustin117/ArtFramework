package spireui.c1;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.UiOpResult;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.core.SignalHandler;
import spireui.core.SignalNames;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 11.7 optional pilot: synthetic C1 chrome that mirrors end-turn UX via signals,
 * without replacing native {@code sts.endturn} backend.
 */
public class EndTurnChromePilotTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void syntheticEndTurnChromeEmitsPressed() {
        SpireUI.register(
                new WindowDef(
                        "endturn_chrome",
                        WindowClass.SYNTHETIC,
                        "layouts/endturn_chrome_pilot.json"));
        SpireUI.mount("endturn_chrome");
        assertNotNull(SpireUI.tree("endturn_chrome").get("end_turn"));
        final AtomicInteger n = new AtomicInteger();
        SpireUI.tree("endturn_chrome")
                .connect(
                        "end_turn",
                        SignalNames.PRESSED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                n.incrementAndGet();
                            }
                        });
        UiOpResult r = SpireUI.ops().clickButton("endturn_chrome", "end_turn");
        assertTrue(r.isOk());
        assertEquals(1, n.get());
    }
}
