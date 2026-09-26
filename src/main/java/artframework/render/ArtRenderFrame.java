package artframework.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, host-neutral aggregate of the payload frame entries contributed by every ART render
 * family producer for one frame.
 *
 * <p>This is the shared aggregation point that lets several producers (today the room-shell
 * producer and the ART-authored VFX bundle runtime) publish their payload-bearing
 * {@link RenderPlan.Entry} values into one ordered frame. ART-authored VFX no longer keeps its own
 * backend frame; only its pure payload mapping remains, and the two producers are aggregated here.
 * The value is data only: it never owns or
 * references a host object, {@code EntityId}, {@code PresentationWorld}, {@code Texture}, or
 * {@code SpriteBatch}. Resolution and submission stay in the ART backend.</p>
 *
 * <p>Entries are sorted by {@code (phase.rank, z, stableKey)} using the same {@link RenderOrder}
 * contract as {@link RenderPlan} and are rejected when a stable key appears more than once, whether
 * the collision is within one producer or across producers. The payload never participates in
 * ordering, de-duplication, identity, or ownership. The frame is deterministic across repeated
 * aggregation runs: same contributions produce the same ordered entries.</p>
 *
 * <p>Each entry also carries its structured <em>owner</em>: the producer id that contributed it.
 * The owner is a plain string tag (no host type) and is kept strictly out of ordering and
 * de-duplication; it exists so a consumer can select its own family's entries without relying on
 * string prefixes of {@code stableKey}, which would let another producer leak pixels across
 * families by choosing a matching key.</p>
 */
public final class ArtRenderFrame {
    private final List<RenderPlan.Entry> entries;
    private final Map<String, String> owners;

    private ArtRenderFrame(List<RenderPlan.Entry> source, Map<String, String> owners) {
        List<RenderPlan.Entry> ordered = new ArrayList<RenderPlan.Entry>(
                source == null ? Collections.<RenderPlan.Entry>emptyList() : source);
        Set<String> stableKeys = new HashSet<String>();
        for (RenderPlan.Entry entry : ordered) {
            if (entry == null) throw new IllegalArgumentException("frame entry required");
            if (entry.stableKey == null || entry.stableKey.isEmpty()) {
                throw new IllegalArgumentException("render stable key required");
            }
            if (!stableKeys.add(entry.stableKey)) {
                throw new IllegalArgumentException("duplicate render stable key: " + entry.stableKey);
            }
        }
        Collections.sort(ordered, new Comparator<RenderPlan.Entry>() {
            @Override public int compare(RenderPlan.Entry a, RenderPlan.Entry b) {
                return RenderOrder.COMPARATOR.compare(
                        new RenderOrder(a.phase, a.z, a.stableKey),
                        new RenderOrder(b.phase, b.z, b.stableKey));
            }
        });
        this.entries = Collections.unmodifiableList(ordered);
        this.owners = owners == null || owners.isEmpty()
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, String>(owners));
    }

    /**
     * Aggregates the payload entries of every producer into one immutable, ordered frame with no
     * owner attribution. A {@code null} list is treated as empty. Any duplicate stable key, within
     * a single producer or across producers, is rejected rather than silently resolved.
     */
    public static ArtRenderFrame of(List<RenderPlan.Entry> entries) {
        return new ArtRenderFrame(entries, null);
    }

    /**
     * Aggregates the payload entries of every producer into one immutable, ordered frame, tagging
     * each entry with the producer id that contributed it. A {@code null} list is treated as empty;
     * a {@code null}/empty owner map means every entry is unattributed. Owner tags never participate
     * in ordering or duplicate-key rejection, so this overload keeps identical ordering and
     * de-duplication semantics to {@link #of(List)}.
     */
    public static ArtRenderFrame of(List<RenderPlan.Entry> entries, Map<String, String> owners) {
        return new ArtRenderFrame(entries, owners);
    }

    /** Safe empty frame: no entries; a backend consumer draws nothing. */
    public static ArtRenderFrame empty() {
        return new ArtRenderFrame(Collections.<RenderPlan.Entry>emptyList(), null);
    }

    /** Payload-bearing entries, deterministically ordered and stable-key unique. */
    public List<RenderPlan.Entry> entries() {
        return entries;
    }

    /**
     * This frame's entries contributed by {@code producerId}, in the shared frame order. Only
     * entries whose structured owner equals {@code producerId} are returned; an unattributed entry
     * (or a {@code null}/empty producer id) yields nothing. Prefix collisions on {@code stableKey}
     * across families are therefore not treated as ownership.
     */
    public List<RenderPlan.Entry> entriesFor(String producerId) {
        if (producerId == null || producerId.isEmpty() || entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<RenderPlan.Entry> out = new ArrayList<RenderPlan.Entry>();
        for (RenderPlan.Entry entry : entries) {
            String owner = entry == null ? null : owners.get(entry.stableKey);
            if (producerId.equals(owner)) out.add(entry);
        }
        return Collections.unmodifiableList(out);
    }

    /** Structured owner of {@code stableKey}, or {@code null} when unattributed/absent. */
    public String ownerOf(String stableKey) {
        return stableKey == null ? null : owners.get(stableKey);
    }

    /** Immutable stable-key to producer-id attribution for this frame. */
    public Map<String, String> owners() {
        return owners;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }
}
