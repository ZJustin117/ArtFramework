package artframework.sts1.lab;

import artframework.sts1.render.VfxClaimPolicy;
import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect;
import com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect;
import com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffect;
import com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle;
import com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect;
import com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Lab/dev helper that queues native transient effects from the family-neutral per-instance claim
 * seam (current members are the {@code vfx-stance-aura} FQNs plus the cross-family
 * {@code LightFlareSEffect} and {@code FlashAtkImgEffect}) into the live STS effect containers so a
 * device-side lab run can exercise the family without combat.
 *
 * <p>This helper is fail-open by contract: no game context, an unknown kind, a non-positive count,
 * or a throwing container all yield {@code 0} rather than propagating. It never throws.
 */
public final class VfxLabSpawn {

    /** Upper bound on a single spawn request; larger counts are clamped. */
    public static final int MAX_COUNT = 20;

    private static final Queue DEFAULT_QUEUE = new DungeonQueue();

    private static volatile Queue queueOverride;

    private static final EffectFactory DEFAULT_FACTORY = new EffectFactory() {
        @Override
        public AbstractGameEffect create(String fqn) throws Exception {
            return construct(fqn);
        }
    };

    private static volatile EffectFactory factoryOverride;

    private VfxLabSpawn() {}

    /**
     * Maps a case-insensitive alias to the native FQN, or {@code null} when unknown. The FQNs reuse
     * the {@link VfxClaimPolicy} constants so the spawned family stays in sync with the claim set.
     */
    public static String classNameFor(String kind) {
        if (kind == null) {
            return null;
        }
        String value = kind.trim();
        if ("stance".equalsIgnoreCase(value) || "aura".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.STANCE_AURA_EFFECT;
        }
        if ("wrath".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.WRATH_PARTICLE_EFFECT;
        }
        if ("divinity".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT;
        }
        if ("calm".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.CALM_PARTICLE_EFFECT;
        }
        if ("divinitychange".equalsIgnoreCase(value) || "dsc".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE;
        }
        if ("flare".equalsIgnoreCase(value) || "lightflare".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SCENE_LIGHT_FLARE;
        }
        if ("flash".equalsIgnoreCase(value) || "flashatk".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FLASH_ATK_IMG;
        }
        return null;
    }

    /** Test seam: the effect-queue surface this helper writes to and scans. */
    public interface Queue {
        /** Appends the effect; returns true only when it was actually stored. */
        boolean add(AbstractGameEffect effect);

        int removeMatching(Predicate<AbstractGameEffect> match);
    }

    /** Test seam: effect construction, so counting/clamping can be isolated from GL context. */
    public interface EffectFactory {
        AbstractGameEffect create(String fqn) throws Exception;
    }

    static void setQueueForTests(Queue queue) {
        queueOverride = queue;
    }

    static void setFactoryForTests(EffectFactory factory) {
        factoryOverride = factory;
    }

    static void resetForTests() {
        queueOverride = null;
        factoryOverride = null;
    }

    /**
     * Builds {@code count} effects of {@code kind} and appends them to the queue. Returns the number
     * actually queued; {@code 0} (never throws) for a null/unknown kind, {@code count <= 0}, a
     * missing game context, or a container that drops/throws on the append.
     */
    public static int spawn(String kind, int count) {
        String fqn = classNameFor(kind);
        if (fqn == null || count <= 0) {
            return 0;
        }
        int total = Math.min(count, MAX_COUNT);
        Queue queue = currentQueue();
        int queued = 0;
        for (int i = 0; i < total; i++) {
            try {
                AbstractGameEffect effect = currentFactory().create(fqn);
                if (effect == null) {
                    continue;
                }
                if (queue.add(effect)) {
                    queued++;
                }
            } catch (Throwable error) {
                // Fail open: a missing game context or an unconstructible effect yields no queue.
            }
        }
        return queued;
    }

    /**
     * Removes queued/active claimed effects (every {@link VfxClaimPolicy} FQN) and returns how many
     * were removed; never throws.
     */
    public static int clear() {
        try {
            return currentQueue().removeMatching(new Predicate<AbstractGameEffect>() {
                @Override
                public boolean test(AbstractGameEffect effect) {
                    if (effect == null) {
                        return false;
                    }
                    return VfxClaimPolicy.supports(effect.getClass().getName());
                }
            });
        } catch (Throwable error) {
            return 0;
        }
    }

    private static AbstractGameEffect construct(String fqn) throws Exception {
        if (VfxClaimPolicy.STANCE_AURA_EFFECT.equals(fqn)) {
            return new StanceAuraEffect("Wrath");
        }
        if (VfxClaimPolicy.WRATH_PARTICLE_EFFECT.equals(fqn)) {
            return new WrathParticleEffect();
        }
        if (VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT.equals(fqn)) {
            return new DivinityParticleEffect();
        }
        if (VfxClaimPolicy.CALM_PARTICLE_EFFECT.equals(fqn)) {
            return new CalmParticleEffect();
        }
        if (VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE.equals(fqn)) {
            // Safe lab defaults: white tint at a screen-center-ish point. Native settings/GL state
            // are not required to construct; any failure is caught by spawn's fail-open guard.
            return new DivinityStanceChangeParticle(Color.WHITE, 960f, 540f);
        }
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The static imgs[] is null outside a live
            // game; the constructor may throw and spawn's fail-open guard drops it rather than
            // propagating.
            return new LightFlareSEffect(960f, 540f);
        }
        if (VfxClaimPolicy.FLASH_ATK_IMG.equals(fqn)) {
            // Safe lab defaults: screen-center-ish point and a concrete attack effect. The static
            // ImageMaster regions may be null outside a live game; any failure is caught by spawn's
            // fail-open guard rather than propagating.
            return new com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect(
                    960f, 540f,
                    com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect.BLUNT_HEAVY);
        }
        return null;
    }

    private static Queue currentQueue() {
        Queue queue = queueOverride;
        return queue != null ? queue : DEFAULT_QUEUE;
    }

    private static EffectFactory currentFactory() {
        EffectFactory factory = factoryOverride;
        return factory != null ? factory : DEFAULT_FACTORY;
    }

    /** Default sink backed by the live {@link AbstractDungeon} effect containers. */
    private static final class DungeonQueue implements Queue {
        @Override
        public boolean add(AbstractGameEffect effect) {
            if (effect == null) {
                return false;
            }
            try {
                if (AbstractDungeon.effectsQueue != null) {
                    AbstractDungeon.effectsQueue.add(effect);
                    return true;
                }
            } catch (Throwable error) {
                // No game context: drop the append rather than throw.
            }
            return false;
        }

        @Override
        public int removeMatching(Predicate<AbstractGameEffect> match) {
            if (match == null) {
                return 0;
            }
            // Each container gets its own guard: a throw in one (including the field read itself)
            // must not skip the others.
            return removeSafe(0, match)
                    + removeSafe(1, match)
                    + removeSafe(2, match)
                    + removeSafe(3, match);
        }

        private static int removeSafe(int container, Predicate<AbstractGameEffect> match) {
            try {
                List<AbstractGameEffect> list;
                if (container == 0) {
                    list = AbstractDungeon.effectsQueue;
                } else if (container == 1) {
                    list = AbstractDungeon.effectList;
                } else if (container == 2) {
                    list = AbstractDungeon.topLevelEffectsQueue;
                } else {
                    list = AbstractDungeon.topLevelEffects;
                }
                return removeFrom(list, match);
            } catch (Throwable error) {
                return 0;
            }
        }
    }

    private static int removeFrom(List<AbstractGameEffect> list, Predicate<AbstractGameEffect> match) {
        if (list == null) {
            return 0;
        }
        int removed = 0;
        Iterator<AbstractGameEffect> iterator = list.iterator();
        while (iterator.hasNext()) {
            AbstractGameEffect effect = iterator.next();
            boolean matched;
            try {
                matched = match.test(effect);
            } catch (Throwable error) {
                matched = false;
            }
            if (matched) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }
}
