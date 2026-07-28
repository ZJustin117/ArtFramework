package artframework.assets;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Replaceable skin/pack contributing ResourceId → AssetRef entries. */
public final class AssetPack {

    public final String id;
    public final String version;
    public final String provider;
    public final int priority;
    public final Set<AssetDomain> domains;
    public final Map<String, AssetRef> entries;

    public AssetPack(
            String id,
            String version,
            String provider,
            int priority,
            Set<AssetDomain> domains,
            Map<String, AssetRef> entries) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("pack id required");
        }
        this.id = id;
        this.version = version != null ? version : "0";
        this.provider = provider != null ? provider : "";
        this.priority = priority;
        if (domains == null || domains.isEmpty()) {
            this.domains = Collections.unmodifiableSet(EnumSet.allOf(AssetDomain.class));
        } else {
            this.domains = Collections.unmodifiableSet(EnumSet.copyOf(domains));
        }
        if (entries == null || entries.isEmpty()) {
            this.entries = Collections.emptyMap();
        } else {
            Map<String, AssetRef> copy = new LinkedHashMap<String, AssetRef>();
            for (Map.Entry<String, AssetRef> e : entries.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    copy.put(e.getKey(), e.getValue());
                }
            }
            this.entries = Collections.unmodifiableMap(copy);
        }
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String version = "1";
        private String provider = "";
        private int priority;
        private Set<AssetDomain> domains = EnumSet.noneOf(AssetDomain.class);
        private final Map<String, AssetRef> entries = new LinkedHashMap<String, AssetRef>();

        Builder(String id) {
            this.id = id;
        }

        public Builder version(String v) {
            this.version = v;
            return this;
        }

        public Builder provider(String p) {
            this.provider = p;
            return this;
        }

        public Builder priority(int p) {
            this.priority = p;
            return this;
        }

        public Builder domain(AssetDomain d) {
            if (d != null) {
                domains.add(d);
            }
            return this;
        }

        public Builder entry(String resourceId, String source) {
            if (resourceId != null && source != null) {
                entries.put(resourceId, new AssetRef(source, id));
            }
            return this;
        }

        public AssetPack build() {
            Set<AssetDomain> d =
                    domains.isEmpty() ? EnumSet.allOf(AssetDomain.class) : domains;
            return new AssetPack(id, version, provider, priority, d, entries);
        }
    }
}
