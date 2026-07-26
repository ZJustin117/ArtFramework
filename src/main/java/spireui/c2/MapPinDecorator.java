package spireui.c2;

import java.util.List;

/**
 * Notified when map pins change. Later Stage/post-render layers may draw; v1 is logic-only.
 */
public interface MapPinDecorator {

    void onPinsChanged(List<MapPin> pins);
}
