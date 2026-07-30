package artframework.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Host-managed unified asset library: vanilla catalog + packs + config + resolve.
 */
public final class HostAssets {

    public static final String VANILLA_PACK_ID = "vanilla";

    private final Map<String, AssetPack> packs = new LinkedHashMap<String, AssetPack>();
    private final Map<String, AssetRef> vanilla = new LinkedHashMap<String, AssetRef>();
    private final Map<String, String> aliases = new LinkedHashMap<String, String>();
    private final List<String> registrationOrder = new ArrayList<String>();
    private final AssetsConfig config = new AssetsConfig();
    private final List<String> lastConflicts = new ArrayList<String>();

    public AssetsConfig config() {
        return config;
    }

    public void registerVanillaCatalog(Map<String, String> resourceIdToSource) {
        vanilla.clear();
        if (resourceIdToSource == null) {
            return;
        }
        for (Map.Entry<String, String> e : resourceIdToSource.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                vanilla.put(e.getKey(), new AssetRef(e.getValue(), VANILLA_PACK_ID));
            }
        }
    }

    /** Load minimal built-in vanilla keys ({@link ResourceIds#minimalVanillaKeys()}). */
    public void loadMinimalVanillaCatalog() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (String key : ResourceIds.minimalVanillaKeys()) {
            map.put(key, "vanilla:" + key);
        }
        registerVanillaCatalog(map);
    }

    public void registerPack(AssetPack pack) {
        if (pack == null) {
            throw new IllegalArgumentException("pack required");
        }
        if (VANILLA_PACK_ID.equals(pack.id)) {
            throw new IllegalArgumentException("pack id reserved: " + VANILLA_PACK_ID);
        }
        boolean existed = packs.containsKey(pack.id);
        packs.put(pack.id, pack);
        if (!existed) {
            registrationOrder.add(pack.id);
        }
        if (!config.packOrder().contains(pack.id)) {
            // leave order empty until set; resolve uses registration + priority
        }
    }

    public void unregisterPack(String packId) {
        if (packId == null) {
            return;
        }
        packs.remove(packId);
        registrationOrder.remove(packId);
    }

    public void enablePack(String packId, boolean enabled) {
        config.setPackEnabled(packId, enabled);
    }

    public void setPackOrder(List<String> order) {
        config.setPackOrder(order);
    }

    /**
     * Prefer a present-profile pack for resolve: enable it and place last in packOrder (wins).
     * Empty packId clears preferred present pack from order head (keeps other order).
     */
    public void preferPresentPack(String packId) {
        String preferred = packId != null ? packId.trim() : "";
        config.setPresentPackId(preferred);
        if (preferred.isEmpty()) {
            return;
        }
        if (!packs.containsKey(preferred) && !VANILLA_PACK_ID.equals(preferred)) {
            // Pack may register later; still record preference for probe / order when present.
        }
        enablePack(preferred, true);
        List<String> order = new ArrayList<String>(config.packOrder());
        order.remove(preferred);
        if (packs.containsKey(preferred) || VANILLA_PACK_ID.equals(preferred)) {
            order.add(preferred);
        } else if (!order.contains(preferred)) {
            order.add(preferred);
        }
        config.setPackOrder(order);
    }

    public String presentPackId() {
        return config.presentPackId();
    }

    public void registerAlias(String from, String to) {
        if (from != null && to != null && !from.isEmpty() && !to.isEmpty()) {
            aliases.put(from, to);
        }
    }

    public AssetResolveResult resolve(String resourceId) {
        lastConflicts.clear();
        if (resourceId == null || resourceId.isEmpty()) {
            return AssetResolveResult.missing("", "resourceId required");
        }
        String key = resourceId;
        String alias = aliases.get(key);
        if (alias != null) {
            key = alias;
        }

        AssetDomain domain = inferDomain(key);
        if (domain != null && !config.isDomainEnabled(domain)) {
            return fallbackOrMissing(key, "domain disabled: " + domain);
        }

        AssetRef winner = null;
        String winnerPack = null;
        List<String> order = config.packOrder();
        if (!order.isEmpty()) {
            // Later entries in packOrder win: scan from end to start.
            for (int i = order.size() - 1; i >= 0; i--) {
                String packId = order.get(i);
                AssetRef ref = entryFromPack(packId, key, domain);
                if (ref == null) {
                    continue;
                }
                if (winner != null) {
                    lastConflicts.add(key + ":" + winnerPack + " covered " + packId);
                    continue;
                }
                winner = ref;
                winnerPack = packId;
            }
        } else {
            winner = pickByPriority(key, domain);
            if (winner != null) {
                winnerPack = winner.packId;
            }
        }

        if (winner != null) {
            return AssetResolveResult.hit(key, winnerPack, winner.source);
        }

        AssetRef van = vanilla.get(key);
        if (van != null) {
            return AssetResolveResult.hit(key, VANILLA_PACK_ID, van.source);
        }

        return fallbackOrMissing(key, "not found: " + key);
    }

    private AssetRef entryFromPack(String packId, String key, AssetDomain domain) {
        if (!config.isPackEnabled(packId)) {
            return null;
        }
        AssetPack pack = packs.get(packId);
        if (pack == null) {
            return null;
        }
        if (domain != null && !pack.domains.contains(domain)) {
            return null;
        }
        AssetRef ref = pack.entries.get(key);
        if (ref == null) {
            return null;
        }
        return new AssetRef(ref.source, pack.id);
    }

    private AssetRef pickByPriority(String key, AssetDomain domain) {
        AssetRef best = null;
        int bestPri = Integer.MIN_VALUE;
        String bestId = null;
        for (String packId : registrationOrder) {
            AssetRef ref = entryFromPack(packId, key, domain);
            if (ref == null) {
                continue;
            }
            AssetPack pack = packs.get(packId);
            int pri = pack != null ? pack.priority : 0;
            if (best == null || pri >= bestPri) {
                if (best != null) {
                    lastConflicts.add(key + ":" + bestId + "<-" + packId);
                }
                best = ref;
                bestPri = pri;
                bestId = packId;
            }
        }
        return best;
    }

    private AssetResolveResult fallbackOrMissing(String key, String message) {
        if (config.isStrictMissing()) {
            return AssetResolveResult.missing(key, message);
        }
        return AssetResolveResult.fallback(key, message);
    }

    static AssetDomain inferDomain(String key) {
        if (key == null) {
            return null;
        }
        if (key.startsWith("card.")) {
            return AssetDomain.CARD;
        }
        if (key.startsWith("char.")) {
            return AssetDomain.CHAR;
        }
        if (key.startsWith("map.")) {
            return AssetDomain.MAP;
        }
        if (key.startsWith("ui.")) {
            return AssetDomain.UI;
        }
        if (key.startsWith("fx.") || key.startsWith("shader.")) {
            return AssetDomain.FX;
        }
        if (key.startsWith("audio.")) {
            return AssetDomain.AUDIO;
        }
        return null;
    }

    public List<String> packIds() {
        return Collections.unmodifiableList(new ArrayList<String>(registrationOrder));
    }

    public AssetPack getPack(String id) {
        return packs.get(id);
    }

    public Map<String, Object> probeAssets() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("vanillaCount", Integer.valueOf(vanilla.size()));
        m.put("packIds", packIds());
        m.put("config", config.probeSummary());
        m.put("aliases", new LinkedHashMap<String, String>(aliases));
        m.put("lastConflicts", new ArrayList<String>(lastConflicts));
        List<Map<String, Object>> packList = new ArrayList<Map<String, Object>>();
        for (String id : registrationOrder) {
            AssetPack p = packs.get(id);
            if (p == null) {
                continue;
            }
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("id", p.id);
            one.put("version", p.version);
            one.put("provider", p.provider);
            one.put("priority", Integer.valueOf(p.priority));
            one.put("enabled", Boolean.valueOf(config.isPackEnabled(p.id)));
            one.put("entryCount", Integer.valueOf(p.entries.size()));
            List<String> doms = new ArrayList<String>();
            for (AssetDomain d : p.domains) {
                doms.add(d.name());
            }
            one.put("domains", doms);
            packList.add(one);
        }
        m.put("packs", packList);
        return m;
    }

    public void reset() {
        packs.clear();
        vanilla.clear();
        aliases.clear();
        registrationOrder.clear();
        lastConflicts.clear();
        config.setActiveProfile("default");
        config.setPresentPackId("");
        config.setPackOrder(Collections.<String>emptyList());
        config.setStrictMissing(false);
        for (AssetDomain d : AssetDomain.values()) {
            config.setDomainEnabled(d, true);
        }
    }
}
