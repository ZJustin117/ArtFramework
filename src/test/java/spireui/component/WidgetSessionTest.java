package spireui.component;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.UiOpResult;
import spireui.api.WindowClass;
import spireui.api.WindowDef;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WidgetSessionTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void openCompositionSeedsSlidersAndProbe() {
        SpireUI.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.open("comp");
        WidgetSession s = SpireUI.widgets("comp");
        assertNotNull(s);
        assertTrue(s.hasSlider("intensity"));
        assertEquals(0.5f, s.getSlider("intensity"), 0.001f);
        assertTrue(s.buttonIds().contains("ok"));
        assertTrue(s.hitAreaIds().contains("card_zone"));

        Map<String, Object> snap = SpireUI.probe().asMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> windows = (Map<String, Object>) snap.get("windows");
        @SuppressWarnings("unchecked")
        Map<String, Object> byId = (Map<String, Object>) windows.get("byId");
        @SuppressWarnings("unchecked")
        Map<String, Object> one = (Map<String, Object>) byId.get("comp");
        @SuppressWarnings("unchecked")
        Map<String, Object> controls = (Map<String, Object>) one.get("controls");
        assertNotNull(controls);
        @SuppressWarnings("unchecked")
        List<String> buttons = (List<String>) one.get("buttonIds");
        assertTrue(buttons.contains("ok"));
        @SuppressWarnings("unchecked")
        Map<String, Object> sliders = (Map<String, Object>) controls.get("sliders");
        assertEquals(0.5d, ((Number) sliders.get("intensity")).doubleValue(), 0.001);
    }

    @Test
    public void setSliderClampsAndFiresHandler() {
        SpireUI.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.open("comp");
        final AtomicReference<Float> last = new AtomicReference<Float>();
        SpireUI.ops().onSlider("comp", "intensity", new SliderHandler() {
            @Override
            public void onChange(float value) {
                last.set(Float.valueOf(value));
            }
        });
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().setSlider("comp", "intensity", 2f).status);
        assertEquals(1f, SpireUI.widgets("comp").getSlider("intensity"), 0.001f);
        assertEquals(1f, last.get().floatValue(), 0.001f);
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().setSlider("comp", "intensity", -1f).status);
        assertEquals(0f, SpireUI.widgets("comp").getSlider("intensity"), 0.001f);
    }

    @Test
    public void clickHitAreaAndUnifiedClick() {
        SpireUI.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.open("comp");
        final AtomicInteger hits = new AtomicInteger();
        SpireUI.ops().onHitArea("comp", "card_zone", new Runnable() {
            @Override
            public void run() {
                hits.incrementAndGet();
            }
        });
        SpireUI.ops().onButton("comp", "ok", new Runnable() {
            @Override
            public void run() {
                hits.addAndGet(10);
            }
        });
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().clickHitArea("comp", "card_zone").status);
        assertEquals(1, hits.get());
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().click("comp", "ok").status);
        assertEquals(11, hits.get());
        assertEquals(UiOpResult.Status.UNAVAILABLE, SpireUI.ops().click("comp", "intensity").status);
    }

    @Test
    public void demoStillHasButtonViaSession() {
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.open("demo");
        assertNotNull(SpireUI.widgets("demo"));
        assertTrue(SpireUI.widgets("demo").hasType("close", UiTypes.BUTTON));
        final AtomicInteger hits = new AtomicInteger();
        SpireUI.ops().onButton("demo", "close", new Runnable() {
            @Override
            public void run() {
                hits.incrementAndGet();
            }
        });
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().clickButton("demo", "close").status);
        assertEquals(1, hits.get());
    }

    @Test
    public void closeClearsSession() {
        SpireUI.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.open("comp");
        SpireUI.close("comp");
        assertNull(SpireUI.widgets("comp"));
        assertFalse(WidgetSessions.isOpen("comp"));
    }

    @Test
    public void setSliderUnavailableWithoutControl() {
        SpireUI.register(new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        SpireUI.open("demo");
        assertEquals(
                UiOpResult.Status.UNAVAILABLE,
                SpireUI.ops().setSlider("demo", "nope", 0.5f).status);
    }
}
