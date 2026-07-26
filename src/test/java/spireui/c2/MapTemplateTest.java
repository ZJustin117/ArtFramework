package spireui.c2;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.api.WindowHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapTemplateTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void bindActivatesMapTemplate() {
        SpireUI.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        assertFalse(NativeTemplateRuntime.isMapBound());
        WindowHandle h = SpireUI.bind("sts.map");
        assertTrue(h.isOpen());
        assertTrue(NativeTemplateRuntime.isMapBound());
        h.close();
        assertFalse(NativeTemplateRuntime.isMapBound());
    }

    @Test
    public void openNativeAlsoBinds() {
        SpireUI.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        SpireUI.open("sts.map");
        assertTrue(NativeTemplateRuntime.isMapBound());
        SpireUI.close("sts.map");
        assertFalse(NativeTemplateRuntime.isMapBound());
    }

    @Test
    public void interceptorBlockWins() {
        SpireUI.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        SpireUI.bind("sts.map");
        MapTemplate map = NativeTemplateRuntime.map();
        final AtomicInteger seen = new AtomicInteger();
        map.addInterceptor(new MapNodeInterceptor() {
            @Override
            public Result intercept(MapNodeRef node) {
                seen.incrementAndGet();
                return Result.ALLOW;
            }
        });
        map.addInterceptor(new MapNodeInterceptor() {
            @Override
            public Result intercept(MapNodeRef node) {
                seen.incrementAndGet();
                return Result.BLOCK;
            }
        });
        map.addInterceptor(new MapNodeInterceptor() {
            @Override
            public Result intercept(MapNodeRef node) {
                seen.incrementAndGet();
                return Result.ALLOW;
            }
        });
        MapNodeInterceptor.Result r = map.dispatchNodeClick(new MapNodeRef(2, 1, "monster"));
        assertEquals(MapNodeInterceptor.Result.BLOCK, r);
        assertEquals(2, seen.get());
    }

    @Test
    public void inactiveAllowsClicks() {
        MapTemplate map = NativeTemplateRuntime.map();
        map.addInterceptor(new MapNodeInterceptor() {
            @Override
            public Result intercept(MapNodeRef node) {
                return Result.BLOCK;
            }
        });
        assertEquals(
                MapNodeInterceptor.Result.ALLOW,
                map.dispatchNodeClick(new MapNodeRef(0, 0, "")));
    }

    @Test
    public void pinsNotifyDecorators() {
        SpireUI.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        SpireUI.bind("sts.map");
        final List<Integer> sizes = new ArrayList<Integer>();
        NativeTemplateRuntime.map().addPinDecorator(new MapPinDecorator() {
            @Override
            public void onPinsChanged(List<MapPin> pins) {
                sizes.add(Integer.valueOf(pins.size()));
            }
        });
        assertEquals(1, sizes.size());
        assertEquals(0, sizes.get(0).intValue());

        NativeTemplateRuntime.map().putPin(
                new MapPin("p1", new MapNodeRef(1, 0, "rest"), "A"));
        assertEquals(2, sizes.size());
        assertEquals(1, sizes.get(1).intValue());

        NativeTemplateRuntime.map().removePin("p1");
        assertEquals(3, sizes.size());
        assertEquals(0, sizes.get(2).intValue());
    }

    @Test(expected = IllegalStateException.class)
    public void pinRequiresActiveMap() {
        NativeTemplateRuntime.map().putPin(
                new MapPin("x", new MapNodeRef(0, 0, ""), ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownNativeTemplateThrows() {
        SpireUI.register(new WindowDef("sts.other", WindowClass.NATIVE_TEMPLATE, "sts.other"));
        SpireUI.bind("sts.other");
    }

    @Test
    public void deactivateClearsPins() {
        SpireUI.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        SpireUI.bind("sts.map");
        NativeTemplateRuntime.map().putPin(
                new MapPin("p1", new MapNodeRef(3, 2, "elite"), "B"));
        assertEquals(1, NativeTemplateRuntime.map().listPins().size());
        SpireUI.close("sts.map");
        assertTrue(NativeTemplateRuntime.map().listPins().isEmpty());
    }

    @Test
    public void runtimeAvailable() {
        assertTrue(NativeTemplateRuntime.isAvailable());
    }
}
