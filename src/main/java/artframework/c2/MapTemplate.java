package artframework.c2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** C2 map lifecycle and pin state; node-click policy lives on the shared SignalBus. */
public final class MapTemplate {
    public static final String RESOURCE = NativeTemplateIds.MAP;
    private final List<MapPinDecorator> pinDecorators = new CopyOnWriteArrayList<MapPinDecorator>();
    private final Map<String, MapPin> pins = new LinkedHashMap<String, MapPin>();
    private boolean active;
    public void activate() { active = true; }
    public void deactivate() { active = false; pins.clear(); notifyPins(); }
    public boolean isActive() { return active; }
    public void addPinDecorator(MapPinDecorator decorator) { if (decorator == null) throw new IllegalArgumentException("decorator required"); pinDecorators.add(decorator); decorator.onPinsChanged(listPins()); }
    public void removePinDecorator(MapPinDecorator decorator) { pinDecorators.remove(decorator); }
    public void putPin(MapPin pin) { if (pin == null) throw new IllegalArgumentException("pin required"); if (!active) throw new IllegalStateException("map template not active"); pins.put(pin.pinId, pin); notifyPins(); }
    public void removePin(String pinId) { if (pins.remove(pinId) != null) notifyPins(); }
    public void clearPins() { if (!pins.isEmpty()) { pins.clear(); notifyPins(); } }
    public List<MapPin> listPins() { return Collections.unmodifiableList(new ArrayList<MapPin>(pins.values())); }
    public MapPin getPin(String pinId) { return pins.get(pinId); }
    public int pinDecoratorCount() { return pinDecorators.size(); }
    private void notifyPins() { List<MapPin> snapshot = listPins(); for (MapPinDecorator d : pinDecorators) d.onPinsChanged(snapshot); }
    void resetForTests() { pinDecorators.clear(); pins.clear(); active = false; }
}
