package artframework.skeleton;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Process registry of {@link SkeletonProvider} implementations.
 */
public final class SkeletonProviders {

    private static final SkeletonProviders GLOBAL = new SkeletonProviders();

    private final Map<String, SkeletonProvider> providers = new LinkedHashMap<String, SkeletonProvider>();

    public SkeletonProviders() {}

    public static SkeletonProviders global() {
        return GLOBAL;
    }

    public void register(SkeletonProvider provider) {
        if (provider == null || provider.id() == null || provider.id().isEmpty()) {
            throw new IllegalArgumentException("provider required");
        }
        providers.put(provider.id(), provider);
    }

    public void unregister(String id) {
        if (id != null) {
            providers.remove(id);
        }
    }

    public SkeletonProvider get(String id) {
        return id == null ? null : providers.get(id);
    }

    public boolean contains(String id) {
        return id != null && providers.containsKey(id);
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    public SkeletonHandle load(String providerId, SkeletonSource source) {
        SkeletonProvider p = get(providerId);
        if (p == null) {
            throw new IllegalArgumentException("unknown skeleton provider: " + providerId);
        }
        if (source == null) {
            throw new IllegalArgumentException("source required");
        }
        return p.load(source);
    }

    public void resetForTests() {
        providers.clear();
    }
}
