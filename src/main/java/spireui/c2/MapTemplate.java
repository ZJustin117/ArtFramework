package spireui.c2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * C2 map template session: node click intercept chain + pin registry.
 * Bound while {@code sts.map} (or equivalent) is open via {@link NativeTemplateRuntime}.
 */
public final class MapTemplate {

    public static final String RESOURCE = NativeTemplateIds.MAP;

    private final List<MapNodeInterceptor> interceptors = new CopyOnWriteArrayList<MapNodeInterceptor>();
    private final List<MapPinDecorator> pinDecorators = new CopyOnWriteArrayList<MapPinDecorator>();
    private final Map<String, MapPin> pins = new LinkedHashMap<String, MapPin>();
    private boolean active;

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
        pins.clear();
        notifyPins();
    }

    public boolean isActive() {
        return active;
    }

    public void addInterceptor(MapNodeInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("interceptor required");
        }
        interceptors.add(interceptor);
    }

    public void removeInterceptor(MapNodeInterceptor interceptor) {
        interceptors.remove(interceptor);
    }

    public void addPinDecorator(MapPinDecorator decorator) {
        if (decorator == null) {
            throw new IllegalArgumentException("decorator required");
        }
        pinDecorators.add(decorator);
        decorator.onPinsChanged(listPins());
    }

    public void removePinDecorator(MapPinDecorator decorator) {
        pinDecorators.remove(decorator);
    }

    /**
     * Run interceptors in registration order. First {@link MapNodeInterceptor.Result#BLOCK} wins.
     * Inactive template always {@link MapNodeInterceptor.Result#ALLOW}.
     */
    public MapNodeInterceptor.Result dispatchNodeClick(MapNodeRef node) {
        if (node == null) {
            throw new IllegalArgumentException("node required");
        }
        if (!active) {
            return MapNodeInterceptor.Result.ALLOW;
        }
        for (MapNodeInterceptor interceptor : interceptors) {
            MapNodeInterceptor.Result r = interceptor.intercept(node);
            if (r == null) {
                continue;
            }
            if (r == MapNodeInterceptor.Result.BLOCK) {
                return MapNodeInterceptor.Result.BLOCK;
            }
        }
        return MapNodeInterceptor.Result.ALLOW;
    }

    public void putPin(MapPin pin) {
        if (pin == null) {
            throw new IllegalArgumentException("pin required");
        }
        if (!active) {
            throw new IllegalStateException("map template not active");
        }
        pins.put(pin.pinId, pin);
        notifyPins();
    }

    public void removePin(String pinId) {
        if (pins.remove(pinId) != null) {
            notifyPins();
        }
    }

    public void clearPins() {
        if (pins.isEmpty()) {
            return;
        }
        pins.clear();
        notifyPins();
    }

    public List<MapPin> listPins() {
        return Collections.unmodifiableList(new ArrayList<MapPin>(pins.values()));
    }

    public MapPin getPin(String pinId) {
        return pins.get(pinId);
    }

    public int interceptorCount() {
        return interceptors.size();
    }

    public int pinDecoratorCount() {
        return pinDecorators.size();
    }

    private void notifyPins() {
        List<MapPin> snapshot = listPins();
        for (MapPinDecorator d : pinDecorators) {
            d.onPinsChanged(snapshot);
        }
    }

    void resetForTests() {
        interceptors.clear();
        pinDecorators.clear();
        pins.clear();
        active = false;
    }
}
