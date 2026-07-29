package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable event surface snapshot for one present frame. */
public final class EventView {

    public final String title;
    public final List<EventOptionView> options;
    public final boolean available;

    public EventView(String title, List<EventOptionView> options, boolean available) {
        this.title = title != null ? title : "";
        if (options == null || options.isEmpty()) {
            this.options = Collections.emptyList();
        } else {
            this.options = Collections.unmodifiableList(new ArrayList<EventOptionView>(options));
        }
        this.available = available;
    }

    public static EventView empty() {
        return new EventView("", null, false);
    }

    public static EventView of(String title, List<EventOptionView> options) {
        return new EventView(title, options, options != null && !options.isEmpty());
    }

    public int optionCount() {
        return options.size();
    }

    public EventOptionView find(int index) {
        for (EventOptionView o : options) {
            if (o.index == index) {
                return o;
            }
        }
        return null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("title", title);
        m.put("available", Boolean.valueOf(available));
        m.put("optionCount", Integer.valueOf(options.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (EventOptionView o : options) {
            list.add(o.toMap());
        }
        m.put("options", list);
        return m;
    }
}
