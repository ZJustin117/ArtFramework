package artframework.c2;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.api.WindowHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapTemplateTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void bindActivatesMapTemplate() {
        ArtFramework.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        assertFalse(NativeTemplateRuntime.isMapBound());
        WindowHandle h = ArtFramework.bind("sts.map");
        assertTrue(h.isOpen());
        assertTrue(NativeTemplateRuntime.isMapBound());
        h.close();
        assertFalse(NativeTemplateRuntime.isMapBound());
    }

    @Test
    public void openNativeAlsoBinds() {
        ArtFramework.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        ArtFramework.open("sts.map");
        assertTrue(NativeTemplateRuntime.isMapBound());
        ArtFramework.close("sts.map");
        assertFalse(NativeTemplateRuntime.isMapBound());
    }

    @Test
    public void interceptorBlockWins() {
        ArtFramework.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        ArtFramework.bind("sts.map");
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
        ArtFramework.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        ArtFramework.bind("sts.map");
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
        ArtFramework.register(new WindowDef("sts.other", WindowClass.NATIVE_TEMPLATE, "sts.other"));
        ArtFramework.bind("sts.other");
    }

    @Test
    public void deactivateClearsPins() {
        ArtFramework.register(new WindowDef("sts.map", WindowClass.NATIVE_TEMPLATE, "sts.map"));
        ArtFramework.bind("sts.map");
        NativeTemplateRuntime.map().putPin(
                new MapPin("p1", new MapNodeRef(3, 2, "elite"), "B"));
        assertEquals(1, NativeTemplateRuntime.map().listPins().size());
        ArtFramework.close("sts.map");
        assertTrue(NativeTemplateRuntime.map().listPins().isEmpty());
    }

    @Test
    public void runtimeAvailable() {
        assertTrue(NativeTemplateRuntime.isAvailable());
    }
}
