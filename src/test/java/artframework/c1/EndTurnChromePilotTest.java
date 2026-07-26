package artframework.c1;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.core.SignalHandler;
import artframework.core.SignalNames;

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
        ArtFramework.resetForTests();
    }

    @Test
    public void syntheticEndTurnChromeEmitsPressed() {
        ArtFramework.register(
                new WindowDef(
                        "endturn_chrome",
                        WindowClass.SYNTHETIC,
                        "layouts/endturn_chrome_pilot.json"));
        ArtFramework.mount("endturn_chrome");
        assertNotNull(ArtFramework.tree("endturn_chrome").get("end_turn"));
        final AtomicInteger n = new AtomicInteger();
        ArtFramework.tree("endturn_chrome")
                .connect(
                        "end_turn",
                        SignalNames.PRESSED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                n.incrementAndGet();
                            }
                        });
        UiOpResult r = ArtFramework.ops().clickButton("endturn_chrome", "end_turn");
        assertTrue(r.isOk());
        assertEquals(1, n.get());
    }
}
