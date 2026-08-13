package artframework.c2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;

/** C2 map lifecycle and pin state; node-click policy lives on the shared SignalBus. */
public final class MapTemplate {
    public static final String RESOURCE = NativeTemplateIds.MAP;
    private final List<MapPinDecorator> pinDecorators = new CopyOnWriteArrayList<MapPinDecorator>();
    private final PresentationContext context = PresentationRegistry.context("c2-templates");
    private final PresentationWorld world = context.world();
    public void activate() {}
    public void deactivate() { clearPinEntities(); notifyPins(); }
    public boolean isActive() { return NativeTemplateRuntime.isMapBound(); }
    public void addPinDecorator(MapPinDecorator decorator) { if (decorator == null) throw new IllegalArgumentException("decorator required"); pinDecorators.add(decorator); decorator.onPinsChanged(listPins()); }
    public void removePinDecorator(MapPinDecorator decorator) { pinDecorators.remove(decorator); }
    public void putPin(MapPin pin) { if (pin == null) throw new IllegalArgumentException("pin required"); if (!isActive()) throw new IllegalStateException("map template not active"); world.put(pinEntity(pin.pinId), MapPinComponent.class, new MapPinComponent(pin.pinId, pin.node, pin.label)); notifyPins(); }
    public void removePin(String pinId) { EntityId entity = pinEntityIfPresent(pinId); if (entity != null) { context.destroy(entity); notifyPins(); } }
    public void clearPins() { if (!listPins().isEmpty()) { clearPinEntities(); notifyPins(); } }
    public List<MapPin> listPins() { List<MapPin> out = new ArrayList<MapPin>(); for (EntityId entity : context.entities()) { MapPinComponent pin = world.get(entity, MapPinComponent.class); if (pin != null) out.add(pin.toPin()); } return Collections.unmodifiableList(out); }
    public MapPin getPin(String pinId) { EntityId entity = pinEntityIfPresent(pinId); MapPinComponent pin = entity == null ? null : world.get(entity, MapPinComponent.class); return pin == null ? null : pin.toPin(); }
    public int pinDecoratorCount() { return pinDecorators.size(); }
    private void notifyPins() { List<MapPin> snapshot = listPins(); for (MapPinDecorator d : pinDecorators) d.onPinsChanged(snapshot); }
    void resetForTests() { pinDecorators.clear(); clearPinEntities(); }

    private EntityId pinEntity(String pinId) { EntityId entity = pinEntityIfPresent(pinId); return entity != null ? entity : context.create(new PresentationKey("sts1.map.pin", pinId), pinId, "map-pin", "c2"); }
    private EntityId pinEntityIfPresent(String pinId) { return pinId == null ? null : context.entity(new PresentationKey("sts1.map.pin", pinId)); }
    private void clearPinEntities() { for (EntityId entity : new ArrayList<EntityId>(context.entities())) if (world.get(entity, MapPinComponent.class) != null) context.destroy(entity); }
}
