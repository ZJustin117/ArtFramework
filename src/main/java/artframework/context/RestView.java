package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Campfire / rest site options. */
public final class RestView {

    public final List<RestOptionView> options;
    public final boolean available;

    public RestView(List<RestOptionView> options, boolean available) {
        if (options == null || options.isEmpty()) {
            this.options = Collections.emptyList();
        } else {
            this.options = Collections.unmodifiableList(new ArrayList<RestOptionView>(options));
        }
        this.available = available;
    }

    public static RestView empty() {
        return new RestView(null, false);
    }

    public static RestView of(List<RestOptionView> options) {
        return new RestView(options, options != null && !options.isEmpty());
    }

    public int optionCount() {
        return options.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("available", Boolean.valueOf(available));
        m.put("optionCount", Integer.valueOf(options.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (RestOptionView o : options) {
            list.add(o.toMap());
        }
        m.put("options", list);
        return m;
    }

    public static final class RestOptionView {
        public final String id;
        public final String label;
        public final boolean enabled;
        public final boolean visible;

        public RestOptionView(String id, String label, boolean enabled, boolean visible) {
            this.id = id != null ? id : "";
            this.label = label != null ? label : "";
            this.enabled = enabled;
            this.visible = visible;
        }

        public static RestOptionView of(String id, String label) {
            return new RestOptionView(id, label, true, true);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("label", label);
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("visible", Boolean.valueOf(visible));
            return m;
        }
    }
}
