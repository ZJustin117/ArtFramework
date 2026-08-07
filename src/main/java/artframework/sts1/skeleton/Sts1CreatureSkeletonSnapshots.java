package artframework.sts1.skeleton;

import artframework.skeleton.SkeletonPresentationFrames;
import artframework.skeleton.SkeletonPresentationView;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.util.ArrayList;
import java.util.List;

/** STS1-only combat creature extraction, kept out of the backend's pure snapshot classpath. */
public final class Sts1CreatureSkeletonSnapshots {
    private Sts1CreatureSkeletonSnapshots() {}

    public static void publish(long frameId) {
        List<SkeletonPresentationView> views = new ArrayList<SkeletonPresentationView>();
        append(views, AbstractDungeon.player);
        try {
            if (AbstractDungeon.getMonsters() != null && AbstractDungeon.getMonsters().monsters != null) {
                for (com.megacrit.cardcrawl.monsters.AbstractMonster monster
                        : AbstractDungeon.getMonsters().monsters) {
                    append(views, monster);
                }
            }
        } catch (Throwable ignored) {
        }
        SkeletonPresentationFrames.publish(frameId, views);
    }

    private static void append(List<SkeletonPresentationView> views, AbstractCreature creature) {
        if (creature == null || creature.isDeadOrEscaped()) return;
        SkeletonPresentationView view = Sts1SkeletonBridge.presentationView(creature);
        if (view != null) views.add(view);
    }
}
