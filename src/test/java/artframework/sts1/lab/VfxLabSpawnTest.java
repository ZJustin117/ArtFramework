package artframework.sts1.lab;

import artframework.sts1.render.VfxClaimPolicy;
import artframework.sts1.render.VfxDrawGeometry;
import artframework.sts1.render.VfxInitContract;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.CardTrailEffect;
import com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect;
import com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VfxLabSpawnTest {

    @After
    public void tearDown() {
        VfxLabSpawn.resetForTests();
    }

    @Test
    public void classNameForMapsAliasesToClaimConstants() {
        assertEquals(
                VfxClaimPolicy.STANCE_AURA_EFFECT, VfxLabSpawn.classNameFor("stance"));
        assertEquals(VfxClaimPolicy.STANCE_AURA_EFFECT, VfxLabSpawn.classNameFor("aura"));
        assertEquals(VfxClaimPolicy.WRATH_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("wrath"));
        assertEquals(
                VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("divinity"));
        assertEquals(VfxClaimPolicy.CALM_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("calm"));
        assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE,
                VfxLabSpawn.classNameFor("divinitychange"));
        assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE,
                VfxLabSpawn.classNameFor("dsc"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, VfxLabSpawn.classNameFor("flare"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, VfxLabSpawn.classNameFor("lightflare"));
        assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, VfxLabSpawn.classNameFor("flash"));
        assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, VfxLabSpawn.classNameFor("flashatk"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_M, VfxLabSpawn.classNameFor("flareM"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_M,
                VfxLabSpawn.classNameFor("lightflareM"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_L, VfxLabSpawn.classNameFor("flareL"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_L,
                VfxLabSpawn.classNameFor("lightflareL"));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L, VfxLabSpawn.classNameFor("torch"));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L,
                VfxLabSpawn.classNameFor("torchparticle"));
        assertEquals(VfxClaimPolicy.FIRE_BURST, VfxLabSpawn.classNameFor("fireburst"));
        assertEquals(VfxClaimPolicy.FIRE_BURST, VfxLabSpawn.classNameFor("fire"));
        assertEquals(VfxClaimPolicy.RED_FIRE_BURST, VfxLabSpawn.classNameFor("redfireburst"));
        assertEquals(VfxClaimPolicy.RED_FIRE_BURST, VfxLabSpawn.classNameFor("redfire"));
        assertEquals(VfxClaimPolicy.SMOKE_BLUR, VfxLabSpawn.classNameFor("smokeblur"));
        assertEquals(VfxClaimPolicy.SMOKE_BLUR, VfxLabSpawn.classNameFor("smoke"));
        assertEquals(VfxClaimPolicy.CEILING_DUST, VfxLabSpawn.classNameFor("ceilingdust"));
        assertEquals(VfxClaimPolicy.CEILING_DUST, VfxLabSpawn.classNameFor("dust"));
        assertEquals(VfxClaimPolicy.NEMESIS_FIRE, VfxLabSpawn.classNameFor("nemesisfire"));
        assertEquals(VfxClaimPolicy.NEMESIS_FIRE, VfxLabSpawn.classNameFor("nemesis"));
        assertEquals(VfxClaimPolicy.SHIELD_PARTICLE, VfxLabSpawn.classNameFor("shield"));
        assertEquals(VfxClaimPolicy.DEBUFF_PARTICLE, VfxLabSpawn.classNameFor("debuff"));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL, VfxLabSpawn.classNameFor("torchxl"));
        assertEquals(VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxLabSpawn.classNameFor("ghostlyfire"));
        assertEquals(VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxLabSpawn.classNameFor("ghostly"));
        assertEquals(VfxClaimPolicy.GENERIC_SMOKE, VfxLabSpawn.classNameFor("genericsmoke"));
        assertEquals(VfxClaimPolicy.GENERIC_SMOKE, VfxLabSpawn.classNameFor("gsmoke"));
        assertEquals(VfxClaimPolicy.EXHAUST_BLUR, VfxLabSpawn.classNameFor("exhaustblur"));
        assertEquals(VfxClaimPolicy.EXHAUST_BLUR, VfxLabSpawn.classNameFor("exhaust"));
        assertEquals(VfxClaimPolicy.ICE_SHATTER, VfxLabSpawn.classNameFor("iceshatter"));
        assertEquals(VfxClaimPolicy.ICE_SHATTER, VfxLabSpawn.classNameFor("ice"));
        assertEquals(VfxClaimPolicy.WEB_PARTICLE, VfxLabSpawn.classNameFor("web"));
        assertEquals(VfxClaimPolicy.WEB_PARTICLE, VfxLabSpawn.classNameFor("webparticle"));
        assertEquals(VfxClaimPolicy.ENTANGLE_EFFECT, VfxLabSpawn.classNameFor("entangle"));
        assertEquals(VfxClaimPolicy.BLOCK_IMPACT_LINE, VfxLabSpawn.classNameFor("blockline"));
        assertEquals(VfxClaimPolicy.BLOCK_IMPACT_LINE, VfxLabSpawn.classNameFor("blockimpact"));
        assertEquals(VfxClaimPolicy.EXHAUST_PILE_PARTICLE,
                VfxLabSpawn.classNameFor("exhaustpile"));
        assertEquals(VfxClaimPolicy.EXHAUST_PILE_PARTICLE,
                VfxLabSpawn.classNameFor("exhaustparticle"));
        assertEquals(VfxClaimPolicy.UNKNOWN_PARTICLE, VfxLabSpawn.classNameFor("unknown"));
        assertEquals(VfxClaimPolicy.UNKNOWN_PARTICLE,
                VfxLabSpawn.classNameFor("unknownparticle"));
        assertEquals(VfxClaimPolicy.FLAME_PARTICLE, VfxLabSpawn.classNameFor("flame"));
        assertEquals(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE,
                VfxLabSpawn.classNameFor("lightningorb"));
        assertEquals(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE,
                VfxLabSpawn.classNameFor("lightningactivate"));
        assertEquals(VfxClaimPolicy.DAMAGE_IMPACT_BLUR,
                VfxLabSpawn.classNameFor("damageblur"));
        assertEquals(VfxClaimPolicy.DAMAGE_IMPACT_BLUR,
                VfxLabSpawn.classNameFor("dmgblur"));
        assertEquals(VfxClaimPolicy.DAMAGE_IMPACT_LINE,
                VfxLabSpawn.classNameFor("damageline"));
        assertEquals(VfxClaimPolicy.DAMAGE_IMPACT_LINE,
                VfxLabSpawn.classNameFor("dmgline"));
        assertEquals(VfxClaimPolicy.DARK_ORB_PASSIVE, VfxLabSpawn.classNameFor("darkorb"));
        assertEquals(VfxClaimPolicy.DARK_ORB_PASSIVE,
                VfxLabSpawn.classNameFor("darkorbpassive"));
        assertEquals(VfxClaimPolicy.WARNING_SIGN, VfxLabSpawn.classNameFor("warning"));
        assertEquals(VfxClaimPolicy.WARNING_SIGN, VfxLabSpawn.classNameFor("warningsign"));
        assertEquals(VfxClaimPolicy.STUN_STAR, VfxLabSpawn.classNameFor("stunstar"));
        assertEquals(VfxClaimPolicy.STUN_STAR, VfxLabSpawn.classNameFor("stun"));
        assertEquals(VfxClaimPolicy.FALLING_DUST, VfxLabSpawn.classNameFor("fallingdust"));
        assertEquals(VfxClaimPolicy.FALLING_DUST, VfxLabSpawn.classNameFor("fdust"));
        assertEquals(VfxClaimPolicy.LIGHTNING_EFFECT, VfxLabSpawn.classNameFor("lightning"));
        assertEquals(VfxClaimPolicy.FLAME_BALL, VfxLabSpawn.classNameFor("flameball"));
        assertEquals(VfxClaimPolicy.SHINE_LINES, VfxLabSpawn.classNameFor("shinelines"));
        assertEquals(VfxClaimPolicy.SHINE_LINES, VfxLabSpawn.classNameFor("shine"));
        assertEquals(VfxClaimPolicy.TORCH_PARTICLE_M, VfxLabSpawn.classNameFor("torchm"));
        assertEquals(VfxClaimPolicy.TORCH_PARTICLE_S, VfxLabSpawn.classNameFor("torchs"));
        assertEquals(VfxClaimPolicy.SCENE_DUST, VfxLabSpawn.classNameFor("scenedust"));
        assertEquals(VfxClaimPolicy.SCENE_DUST, VfxLabSpawn.classNameFor("dusteffect"));
        assertEquals(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE,
                VfxLabSpawn.classNameFor("lightningorbpassive"));
        assertEquals(VfxClaimPolicy.LIGHTNING_ORB_PASSIVE, VfxLabSpawn.classNameFor("lop"));
        assertEquals(VfxClaimPolicy.GLOWY_FIRE_EYES,
                VfxLabSpawn.classNameFor("glowyfireeyes"));
        assertEquals(VfxClaimPolicy.GLOWY_FIRE_EYES, VfxLabSpawn.classNameFor("glowyeyes"));
        assertEquals(VfxClaimPolicy.FLYING_SPIKE, VfxLabSpawn.classNameFor("flyingspike"));
        assertEquals(VfxClaimPolicy.FLYING_SPIKE, VfxLabSpawn.classNameFor("spike"));
        assertEquals(VfxClaimPolicy.CONE_EFFECT, VfxLabSpawn.classNameFor("cone"));
        assertEquals(VfxClaimPolicy.FALLING_ICE, VfxLabSpawn.classNameFor("fallingice"));
        assertEquals(VfxClaimPolicy.FALLING_ICE, VfxLabSpawn.classNameFor("icefall"));
        assertEquals(VfxClaimPolicy.DAMAGE_HEART, VfxLabSpawn.classNameFor("damageheart"));
        assertEquals(VfxClaimPolicy.DAMAGE_HEART, VfxLabSpawn.classNameFor("heart"));
        assertEquals(VfxClaimPolicy.SPOOKY_CHEST, VfxLabSpawn.classNameFor("spookychest"));
        assertEquals(VfxClaimPolicy.SPOOKY_CHEST, VfxLabSpawn.classNameFor("spooky"));
        assertEquals(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME,
                VfxLabSpawn.classNameFor("victoryflame"));
        assertEquals(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME,
                VfxLabSpawn.classNameFor("ironcladvictory"));
        assertEquals(VfxClaimPolicy.SPOOKIER_CHEST,
                VfxLabSpawn.classNameFor("spookierchest"));
        assertEquals(VfxClaimPolicy.SPOOKIER_CHEST, VfxLabSpawn.classNameFor("spookier"));
        assertEquals(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER,
                VfxLabSpawn.classNameFor("campfiresleepcover"));
        assertEquals(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER,
                VfxLabSpawn.classNameFor("sleepcover"));
        assertEquals(VfxClaimPolicy.DEATH_SCREEN_FLOATY,
                VfxLabSpawn.classNameFor("deathfloaty"));
        assertEquals(VfxClaimPolicy.DEATH_SCREEN_FLOATY,
                VfxLabSpawn.classNameFor("deathscreen"));
        assertEquals(VfxClaimPolicy.WRATH_STANCE_CHANGE,
                VfxLabSpawn.classNameFor("wrathchange"));
        assertEquals(VfxClaimPolicy.WRATH_STANCE_CHANGE,
                VfxLabSpawn.classNameFor("wrathstance"));
        assertEquals(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION,
                VfxLabSpawn.classNameFor("absorption"));
        assertEquals(VfxClaimPolicy.STANCE_CHANGE_ABSORPTION,
                VfxLabSpawn.classNameFor("absorb"));
        assertEquals(VfxClaimPolicy.WATER_SPLASH, VfxLabSpawn.classNameFor("watersplash"));
        assertEquals(VfxClaimPolicy.WATER_SPLASH, VfxLabSpawn.classNameFor("splash"));
        assertEquals(VfxClaimPolicy.BUFF_PARTICLE, VfxLabSpawn.classNameFor("buffparticle"));
        assertEquals(VfxClaimPolicy.BUFF_PARTICLE, VfxLabSpawn.classNameFor("buffp"));
        assertEquals(VfxClaimPolicy.BOTTOM_FOG, VfxLabSpawn.classNameFor("bottomfog"));
        assertEquals(VfxClaimPolicy.BOTTOM_FOG, VfxLabSpawn.classNameFor("bfog"));
        assertEquals(VfxClaimPolicy.GIANT_FIRE, VfxLabSpawn.classNameFor("giantfire"));
        assertEquals(VfxClaimPolicy.GIANT_FIRE, VfxLabSpawn.classNameFor("gfire"));
        assertEquals(VfxClaimPolicy.TORCH_HEAD_FIRE,
                VfxLabSpawn.classNameFor("torchheadfire"));
        assertEquals(VfxClaimPolicy.TORCH_HEAD_FIRE, VfxLabSpawn.classNameFor("torchhead"));
        assertEquals(VfxClaimPolicy.CARD_TRAIL, VfxLabSpawn.classNameFor("cardtrail"));
        assertEquals(VfxClaimPolicy.CARD_TRAIL, VfxLabSpawn.classNameFor("trail"));
        assertEquals(VfxClaimPolicy.FLYING_ORB, VfxLabSpawn.classNameFor("flyingorb"));
        assertEquals(VfxClaimPolicy.FLYING_ORB, VfxLabSpawn.classNameFor("orb"));
        assertEquals(VfxClaimPolicy.FLICK_COIN, VfxLabSpawn.classNameFor("flickcoin"));
        assertEquals(VfxClaimPolicy.FLICK_COIN, VfxLabSpawn.classNameFor("coin"));
        assertEquals(VfxClaimPolicy.MAP_CIRCLE, VfxLabSpawn.classNameFor("mapcircle"));
        assertEquals(VfxClaimPolicy.MAP_CIRCLE, VfxLabSpawn.classNameFor("map"));
        assertEquals(VfxClaimPolicy.SPOTLIGHT, VfxLabSpawn.classNameFor("spotlight"));
    }

    @Test
    public void classNameForIsCaseInsensitiveAndTrims() {
        assertEquals(
                VfxClaimPolicy.STANCE_AURA_EFFECT, VfxLabSpawn.classNameFor("  StAnCe "));
        assertEquals(VfxClaimPolicy.WRATH_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("WRATH"));
        assertEquals(VfxClaimPolicy.CALM_PARTICLE_EFFECT, VfxLabSpawn.classNameFor("  CaLm "));
        assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE,
                VfxLabSpawn.classNameFor("  DiViNiTyChAnGe "));
        assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE,
                VfxLabSpawn.classNameFor(" DSC "));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, VfxLabSpawn.classNameFor("  FlArE "));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, VfxLabSpawn.classNameFor("LIGHTFLARE"));
        assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, VfxLabSpawn.classNameFor("  FlAsH "));
        assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, VfxLabSpawn.classNameFor("FLASHATK"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_M, VfxLabSpawn.classNameFor("  FlArEm "));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_M,
                VfxLabSpawn.classNameFor("LIGHTFLAREM"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_L, VfxLabSpawn.classNameFor("FLAREL"));
        assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE_L,
                VfxLabSpawn.classNameFor("lightflarel"));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L, VfxLabSpawn.classNameFor("  ToRcH "));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L,
                VfxLabSpawn.classNameFor("TORCHPARTICLE"));
        assertEquals(VfxClaimPolicy.FIRE_BURST, VfxLabSpawn.classNameFor("  FiReBuRsT "));
        assertEquals(VfxClaimPolicy.FIRE_BURST, VfxLabSpawn.classNameFor("FIRE"));
        assertEquals(VfxClaimPolicy.RED_FIRE_BURST, VfxLabSpawn.classNameFor("  ReDfIrE "));
        assertEquals(VfxClaimPolicy.SMOKE_BLUR, VfxLabSpawn.classNameFor("SMOKEBLUR"));
        assertEquals(VfxClaimPolicy.SMOKE_BLUR, VfxLabSpawn.classNameFor("  SmOkE "));
        assertEquals(VfxClaimPolicy.CEILING_DUST, VfxLabSpawn.classNameFor("CEILINGDUST"));
        assertEquals(VfxClaimPolicy.CEILING_DUST, VfxLabSpawn.classNameFor("DUST"));
        assertEquals(VfxClaimPolicy.NEMESIS_FIRE, VfxLabSpawn.classNameFor("NEMESISFIRE"));
        assertEquals(VfxClaimPolicy.NEMESIS_FIRE, VfxLabSpawn.classNameFor("  NeMeSiS "));
        assertEquals(VfxClaimPolicy.SHIELD_PARTICLE, VfxLabSpawn.classNameFor("  ShIeLd "));
        assertEquals(VfxClaimPolicy.DEBUFF_PARTICLE, VfxLabSpawn.classNameFor("DEBUFF"));
        assertEquals(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL, VfxLabSpawn.classNameFor("  ToRcHxL "));
        assertEquals(VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxLabSpawn.classNameFor("GHOSTLYFIRE"));
        assertEquals(VfxClaimPolicy.GHOSTLY_WEAK_FIRE, VfxLabSpawn.classNameFor("  GhOsTlY "));
        assertEquals(VfxClaimPolicy.GENERIC_SMOKE, VfxLabSpawn.classNameFor("GENERICSMOKE"));
        assertEquals(VfxClaimPolicy.GENERIC_SMOKE, VfxLabSpawn.classNameFor("  GsMoKe "));
        assertEquals(VfxClaimPolicy.EXHAUST_BLUR, VfxLabSpawn.classNameFor("EXHAUSTBLUR"));
        assertEquals(VfxClaimPolicy.EXHAUST_BLUR, VfxLabSpawn.classNameFor("  ExHaUsT "));
        assertEquals(VfxClaimPolicy.ICE_SHATTER, VfxLabSpawn.classNameFor("  IcEsHaTtEr "));
        assertEquals(VfxClaimPolicy.ICE_SHATTER, VfxLabSpawn.classNameFor("ICE"));
        assertEquals(VfxClaimPolicy.WEB_PARTICLE, VfxLabSpawn.classNameFor("  WeB "));
        assertEquals(VfxClaimPolicy.WEB_PARTICLE, VfxLabSpawn.classNameFor("WEBPARTICLE"));
        assertEquals(VfxClaimPolicy.ENTANGLE_EFFECT, VfxLabSpawn.classNameFor("  EnTaNgLe "));
        assertEquals(VfxClaimPolicy.BLOCK_IMPACT_LINE, VfxLabSpawn.classNameFor("  BlOcKlInE "));
        assertEquals(VfxClaimPolicy.BLOCK_IMPACT_LINE,
                VfxLabSpawn.classNameFor("BLOCKIMPACT"));
        assertEquals(VfxClaimPolicy.EXHAUST_PILE_PARTICLE,
                VfxLabSpawn.classNameFor("  ExHaUsTpIlE "));
        assertEquals(VfxClaimPolicy.EXHAUST_PILE_PARTICLE,
                VfxLabSpawn.classNameFor("EXHAUSTPARTICLE"));
        assertEquals(VfxClaimPolicy.UNKNOWN_PARTICLE, VfxLabSpawn.classNameFor("  UnKnOwN "));
        assertEquals(VfxClaimPolicy.UNKNOWN_PARTICLE,
                VfxLabSpawn.classNameFor("UNKNOWNPARTICLE"));
        assertEquals(VfxClaimPolicy.FLAME_PARTICLE, VfxLabSpawn.classNameFor("  FlAmE "));
        assertEquals(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE,
                VfxLabSpawn.classNameFor("  LiGhTnInGoRb "));
        assertEquals(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE,
                VfxLabSpawn.classNameFor("LIGHTNINGACTIVATE"));
        assertEquals(VfxClaimPolicy.DAMAGE_IMPACT_BLUR,
                VfxLabSpawn.classNameFor("  DaMaGeBlUr "));
        assertEquals(VfxClaimPolicy.DAMAGE_IMPACT_BLUR,
                VfxLabSpawn.classNameFor("DMGBLUR"));
        assertEquals(VfxClaimPolicy.DAMAGE_IMPACT_LINE,
                VfxLabSpawn.classNameFor("  DaMaGeLiNe "));
        assertEquals(VfxClaimPolicy.DAMAGE_IMPACT_LINE,
                VfxLabSpawn.classNameFor("DMGLINE"));
        assertEquals(VfxClaimPolicy.DARK_ORB_PASSIVE, VfxLabSpawn.classNameFor("  DaRkOrB "));
        assertEquals(VfxClaimPolicy.DARK_ORB_PASSIVE,
                VfxLabSpawn.classNameFor("DARKORBPASSIVE"));
        assertEquals(VfxClaimPolicy.WARNING_SIGN, VfxLabSpawn.classNameFor("  WaRnInG "));
        assertEquals(VfxClaimPolicy.WARNING_SIGN, VfxLabSpawn.classNameFor("WARNINGSIGN"));
        assertEquals(VfxClaimPolicy.STUN_STAR, VfxLabSpawn.classNameFor("  StUnStAr "));
        assertEquals(VfxClaimPolicy.STUN_STAR, VfxLabSpawn.classNameFor("STUN"));
        assertEquals(VfxClaimPolicy.FALLING_DUST, VfxLabSpawn.classNameFor("  FaLlInGdUsT "));
        assertEquals(VfxClaimPolicy.FALLING_DUST, VfxLabSpawn.classNameFor("FDUST"));
        assertEquals(VfxClaimPolicy.LIGHTNING_EFFECT, VfxLabSpawn.classNameFor("  LiGhTnInG "));
        assertEquals(VfxClaimPolicy.FLAME_BALL, VfxLabSpawn.classNameFor("FLAMEBALL"));
        assertEquals(VfxClaimPolicy.SHINE_LINES, VfxLabSpawn.classNameFor("  ShInElInEs "));
        assertEquals(VfxClaimPolicy.SHINE_LINES, VfxLabSpawn.classNameFor("SHINE"));
        assertEquals(VfxClaimPolicy.TORCH_PARTICLE_M, VfxLabSpawn.classNameFor("  ToRcHm "));
        assertEquals(VfxClaimPolicy.TORCH_PARTICLE_S, VfxLabSpawn.classNameFor("TORCHS"));
        assertEquals(VfxClaimPolicy.SCENE_DUST, VfxLabSpawn.classNameFor("  ScEnEdUsT "));
        assertEquals(VfxClaimPolicy.SCENE_DUST, VfxLabSpawn.classNameFor("DUSTEFFECT"));
        assertEquals(VfxClaimPolicy.FLYING_SPIKE, VfxLabSpawn.classNameFor("  FlYiNgSpIkE "));
        assertEquals(VfxClaimPolicy.FLYING_SPIKE, VfxLabSpawn.classNameFor("SPIKE"));
        assertEquals(VfxClaimPolicy.CONE_EFFECT, VfxLabSpawn.classNameFor("  CoNe "));
        assertEquals(VfxClaimPolicy.BOTTOM_FOG, VfxLabSpawn.classNameFor("  BoTtOmFoG "));
        assertEquals(VfxClaimPolicy.BOTTOM_FOG, VfxLabSpawn.classNameFor("BFOG"));
        assertEquals(VfxClaimPolicy.GIANT_FIRE, VfxLabSpawn.classNameFor("  GiAnTfIrE "));
        assertEquals(VfxClaimPolicy.GIANT_FIRE, VfxLabSpawn.classNameFor("GFIRE"));
        assertEquals(VfxClaimPolicy.TORCH_HEAD_FIRE,
                VfxLabSpawn.classNameFor("  ToRcHhEaDfIrE "));
        assertEquals(VfxClaimPolicy.TORCH_HEAD_FIRE, VfxLabSpawn.classNameFor("TORCHHEAD"));
        assertEquals(VfxClaimPolicy.CARD_TRAIL, VfxLabSpawn.classNameFor("  CaRdTrAiL "));
        assertEquals(VfxClaimPolicy.CARD_TRAIL, VfxLabSpawn.classNameFor("TRAIL"));
        assertEquals(VfxClaimPolicy.FLICK_COIN, VfxLabSpawn.classNameFor("  FlIcKcOiN "));
        assertEquals(VfxClaimPolicy.FLICK_COIN, VfxLabSpawn.classNameFor("COIN"));
        assertEquals(VfxClaimPolicy.HEAL_PANEL, VfxLabSpawn.classNameFor("  HeAlPaNeL "));
        assertEquals(VfxClaimPolicy.HEAL_PANEL, VfxLabSpawn.classNameFor("HEAL"));
        assertEquals(VfxClaimPolicy.SPOTLIGHT, VfxLabSpawn.classNameFor("  SpOtLiGhT "));
    }

    @Test
    public void classNameForRejectsUnknownAndNull() {
        assertNull(VfxLabSpawn.classNameFor(null));
        assertNull(VfxLabSpawn.classNameFor(""));
        assertNull(VfxLabSpawn.classNameFor("   "));
        assertNull(VfxLabSpawn.classNameFor("bogus"));
        assertNull(VfxLabSpawn.classNameFor("StanceAura"));
    }

    @Test
    public void spawnRejectsNullKind() {
        RecordingQueue queue = new RecordingQueue();
        VfxLabSpawn.setQueueForTests(queue);
        assertEquals(0, VfxLabSpawn.spawn(null, 3));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnRejectsUnknownKind() {
        RecordingQueue queue = new RecordingQueue();
        VfxLabSpawn.setQueueForTests(queue);
        assertEquals(0, VfxLabSpawn.spawn("bogus", 3));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnRejectsNonPositiveCount() {
        RecordingQueue queue = new RecordingQueue();
        VfxLabSpawn.setQueueForTests(queue);
        assertEquals(0, VfxLabSpawn.spawn("stance", 0));
        assertEquals(0, VfxLabSpawn.spawn("stance", -5));
        assertEquals(0, queue.added.size());
    }

    @Test
    public void spawnNeverThrowsWithoutLiveGameContext() {
        VfxLabSpawn.resetForTests();
        // Headless there is no libGDX asset context, so construction fails open to 0 rather than
        // throwing. The contract under test is "returns a non-negative count and never throws".
        int queued = VfxLabSpawn.spawn("wrath", 3);
        org.junit.Assert.assertTrue("expected fail-open count >= 0 but was " + queued, queued >= 0);
        assertEquals(0, VfxLabSpawn.spawn("divinity", 2));
    }

    @Test
    public void spawnDoesNotPropagateThrowingQueue() {
        VfxLabSpawn.setQueueForTests(new ThrowingQueue());
        try {
            int queued = VfxLabSpawn.spawn("stance", 4);
            org.junit.Assert.assertTrue("expected count >= 0 but was " + queued, queued >= 0);
            assertEquals(0, queued);
        } catch (RuntimeException unexpected) {
            org.junit.Assert.fail("spawn propagated: " + unexpected);
        }
    }

    @Test
    public void spawnDoesNotCountEffectsTheQueueDeclines() {
        // A queue whose add reports false (the live DungeonQueue no-context behavior) must not
        // over-report: queued stays 0 even though construction succeeded.
        VfxLabSpawn.setQueueForTests(new DecliningQueue());
        VfxLabSpawn.setFactoryForTests(new StubFactory());
        assertEquals(0, VfxLabSpawn.spawn("wrath", 5));
    }

    @Test
    public void spawnHappyPathQueuesExactlyClampedCount() {
        // The factory seam supplies a stub effect so the GL-backed constructors are bypassed; a
        // RecordingQueue whose add returns true then exercises the clamp/count arithmetic.
        RecordingQueue queue = new RecordingQueue();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(new StubFactory());

        assertEquals(5, VfxLabSpawn.spawn("wrath", 5));
        assertEquals(5, queue.added.size());

        assertEquals(VfxLabSpawn.MAX_COUNT, VfxLabSpawn.spawn("divinity", 99));
        assertEquals(5 + VfxLabSpawn.MAX_COUNT, queue.added.size());

        assertEquals(2, VfxLabSpawn.spawn("stance", 2));
        assertEquals(5 + VfxLabSpawn.MAX_COUNT + 2, queue.added.size());
    }

    @Test
    public void spawnHappyPathQueuesCalmThroughTheFactorySeam() {
        // Calm is the 4th claimable FQN. The factory seam captures the requested FQN, so the
        // calm -> CALM_PARTICLE_EFFECT mapping is genuinely asserted without the GL-backed
        // CalmParticleEffect constructor.
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn("calm", 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(VfxClaimPolicy.CALM_PARTICLE_EFFECT, fqn);
        }
    }

    @Test
    public void spawnHappyPathQueuesDivinityStanceChangeThroughTheFactorySeam() {
        // DivinityStanceChangeParticle is the 5th claimable FQN; the capturing factory proves the
        // divinitychange alias requests exactly that FQN without touching its GL-backed constructor.
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn("divinitychange", 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE, fqn);
        }
    }

    @Test
    public void spawnHappyPathQueuesLightFlareThroughTheFactorySeam() {
        // The first non-aura (vfx-scene-world) claimable FQN; the capturing factory proves the
        // flare/lightflare aliases request exactly that FQN without touching its GL-backed
        // constructor.
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn("flare", 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, fqn);
        }

        RecordingQueue queue2 = new RecordingQueue();
        CapturingFactory factory2 = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue2);
        VfxLabSpawn.setFactoryForTests(factory2);

        assertEquals(3, VfxLabSpawn.spawn("lightflare", 3));
        assertEquals(3, factory2.requested.size());
        for (String fqn : factory2.requested) {
            assertEquals(VfxClaimPolicy.SCENE_LIGHT_FLARE, fqn);
        }
    }

    @Test
    public void spawnHappyPathQueuesFlashAtkImgThroughTheFactorySeam() {
        // The first vfx-combat claimable FQN; the capturing factory proves the flash/flashatk aliases
        // request exactly that FQN without touching its GL/image-backed constructor.
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn("flash", 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, fqn);
        }

        RecordingQueue queue2 = new RecordingQueue();
        CapturingFactory factory2 = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue2);
        VfxLabSpawn.setFactoryForTests(factory2);

        assertEquals(3, VfxLabSpawn.spawn("flashatk", 3));
        assertEquals(3, factory2.requested.size());
        for (String fqn : factory2.requested) {
            assertEquals(VfxClaimPolicy.FLASH_ATK_IMG, fqn);
        }
    }

    @Test
    public void spawnHappyPathQueuesLaterSceneWorldMembersThroughTheFactorySeam() {
        // The three later vfx-scene-world claimable FQNs; the capturing factory proves each alias
        // requests exactly its FQN without touching the GL-backed constructors (the flare pair's
        // static imgs[] and TorchParticleLEffect's getImg() are null off-game).
        assertSpawnRequests("flareM", VfxClaimPolicy.SCENE_LIGHT_FLARE_M);
        assertSpawnRequests("lightflareM", VfxClaimPolicy.SCENE_LIGHT_FLARE_M);
        assertSpawnRequests("flareL", VfxClaimPolicy.SCENE_LIGHT_FLARE_L);
        assertSpawnRequests("lightflareL", VfxClaimPolicy.SCENE_LIGHT_FLARE_L);
        assertSpawnRequests("torch", VfxClaimPolicy.SCENE_TORCH_PARTICLE_L);
        assertSpawnRequests("torchparticle", VfxClaimPolicy.SCENE_TORCH_PARTICLE_L);
    }

    @Test
    public void spawnHappyPathQueuesTheNewestFiveMembersThroughTheFactorySeam() {
        // The two fire-burst and three ambient FQNs; the capturing factory proves each alias
        // requests exactly its FQN without touching the GL/image-backed constructors (the static
        // ImageMaster regions may be null off-game).
        assertSpawnRequests("fireburst", VfxClaimPolicy.FIRE_BURST);
        assertSpawnRequests("fire", VfxClaimPolicy.FIRE_BURST);
        assertSpawnRequests("redfireburst", VfxClaimPolicy.RED_FIRE_BURST);
        assertSpawnRequests("redfire", VfxClaimPolicy.RED_FIRE_BURST);
        assertSpawnRequests("smokeblur", VfxClaimPolicy.SMOKE_BLUR);
        assertSpawnRequests("smoke", VfxClaimPolicy.SMOKE_BLUR);
        assertSpawnRequests("ceilingdust", VfxClaimPolicy.CEILING_DUST);
        assertSpawnRequests("dust", VfxClaimPolicy.CEILING_DUST);
        assertSpawnRequests("nemesisfire", VfxClaimPolicy.NEMESIS_FIRE);
        assertSpawnRequests("nemesis", VfxClaimPolicy.NEMESIS_FIRE);
    }

    @Test
    public void spawnHappyPathQueuesTheBareTextureMembersThroughTheFactorySeam() {
        // The two bare-Texture (fixed source rect) FQNs; the capturing factory proves each alias
        // requests exactly its FQN without touching the ImageMaster/texture-backed constructors.
        assertSpawnRequests("shield", VfxClaimPolicy.SHIELD_PARTICLE);
        assertSpawnRequests("debuff", VfxClaimPolicy.DEBUFF_PARTICLE);
    }

    @Test
    public void spawnHappyPathQueuesTheFourNewestMembersThroughTheFactorySeam() {
        // The four newest claimable FQNs; the capturing factory proves each alias requests exactly
        // its FQN without touching the GL/image-backed constructors (the static imgs[] / getImg()
        // regions may be null off-game).
        assertSpawnRequests("torchxl", VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL);
        assertSpawnRequests("ghostlyfire", VfxClaimPolicy.GHOSTLY_WEAK_FIRE);
        assertSpawnRequests("ghostly", VfxClaimPolicy.GHOSTLY_WEAK_FIRE);
        assertSpawnRequests("genericsmoke", VfxClaimPolicy.GENERIC_SMOKE);
        assertSpawnRequests("gsmoke", VfxClaimPolicy.GENERIC_SMOKE);
        assertSpawnRequests("exhaustblur", VfxClaimPolicy.EXHAUST_BLUR);
        assertSpawnRequests("exhaust", VfxClaimPolicy.EXHAUST_BLUR);
    }

    @Test
    public void spawnHappyPathQueuesTheTwoNewestBareTextureMembersThroughTheFactorySeam() {
        // The two newest bare-Texture (fixed source rect) FQNs; the capturing factory proves each
        // alias requests exactly its FQN without touching the ImageMaster/texture-backed
        // constructors.
        assertSpawnRequests("iceshatter", VfxClaimPolicy.ICE_SHATTER);
        assertSpawnRequests("ice", VfxClaimPolicy.ICE_SHATTER);
        assertSpawnRequests("web", VfxClaimPolicy.WEB_PARTICLE);
        assertSpawnRequests("webparticle", VfxClaimPolicy.WEB_PARTICLE);
    }

    @Test
    public void spawnHappyPathQueuesTheF14MembersThroughTheFactorySeam() {
        // The four newest claimable FQNs; the capturing factory proves each alias requests exactly
        // its FQN without touching the ImageMaster/texture-backed constructors.
        assertSpawnRequests("entangle", VfxClaimPolicy.ENTANGLE_EFFECT);
        assertSpawnRequests("blockline", VfxClaimPolicy.BLOCK_IMPACT_LINE);
        assertSpawnRequests("blockimpact", VfxClaimPolicy.BLOCK_IMPACT_LINE);
        assertSpawnRequests("exhaustpile", VfxClaimPolicy.EXHAUST_PILE_PARTICLE);
        assertSpawnRequests("exhaustparticle", VfxClaimPolicy.EXHAUST_PILE_PARTICLE);
        assertSpawnRequests("unknown", VfxClaimPolicy.UNKNOWN_PARTICLE);
        assertSpawnRequests("unknownparticle", VfxClaimPolicy.UNKNOWN_PARTICLE);
    }

    @Test
    public void spawnHappyPathQueuesTheF15MembersThroughTheFactorySeam() {
        // The five newest claimable FQNs; the capturing factory proves each alias requests exactly
        // its FQN without touching the ImageMaster/texture-backed constructors.
        assertSpawnRequests("flame", VfxClaimPolicy.FLAME_PARTICLE);
        assertSpawnRequests("lightningorb", VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE);
        assertSpawnRequests("lightningactivate", VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE);
        assertSpawnRequests("damageblur", VfxClaimPolicy.DAMAGE_IMPACT_BLUR);
        assertSpawnRequests("dmgblur", VfxClaimPolicy.DAMAGE_IMPACT_BLUR);
        assertSpawnRequests("damageline", VfxClaimPolicy.DAMAGE_IMPACT_LINE);
        assertSpawnRequests("dmgline", VfxClaimPolicy.DAMAGE_IMPACT_LINE);
        assertSpawnRequests("darkorb", VfxClaimPolicy.DARK_ORB_PASSIVE);
        assertSpawnRequests("darkorbpassive", VfxClaimPolicy.DARK_ORB_PASSIVE);
    }

    @Test
    public void spawnHappyPathQueuesTheF16MembersThroughTheFactorySeam() {
        // The three newest claimable FQNs; the capturing factory proves each alias requests exactly
        // its FQN without touching the ImageMaster-backed constructors.
        assertSpawnRequests("warning", VfxClaimPolicy.WARNING_SIGN);
        assertSpawnRequests("warningsign", VfxClaimPolicy.WARNING_SIGN);
        assertSpawnRequests("stunstar", VfxClaimPolicy.STUN_STAR);
        assertSpawnRequests("stun", VfxClaimPolicy.STUN_STAR);
        assertSpawnRequests("fallingdust", VfxClaimPolicy.FALLING_DUST);
        assertSpawnRequests("fdust", VfxClaimPolicy.FALLING_DUST);
    }

    @Test
    public void spawnHappyPathQueuesTheF17MembersThroughTheFactorySeam() {
        // The three newest claimable FQNs; the capturing factory proves each alias requests exactly
        // its FQN without touching the ImageMaster-backed constructors.
        assertSpawnRequests("lightning", VfxClaimPolicy.LIGHTNING_EFFECT);
        assertSpawnRequests("flameball", VfxClaimPolicy.FLAME_BALL);
        assertSpawnRequests("shinelines", VfxClaimPolicy.SHINE_LINES);
        assertSpawnRequests("shine", VfxClaimPolicy.SHINE_LINES);
    }

    @Test
    public void spawnHappyPathQueuesTheF18MembersThroughTheFactorySeam() {
        // The three newest claimable FQNs; the capturing factory proves each alias requests exactly
        // its FQN without touching the ImageMaster-backed constructors (DustEffect is constructed via
        // its NO-ARG constructor).
        assertSpawnRequests("torchm", VfxClaimPolicy.TORCH_PARTICLE_M);
        assertSpawnRequests("torchs", VfxClaimPolicy.TORCH_PARTICLE_S);
        assertSpawnRequests("scenedust", VfxClaimPolicy.SCENE_DUST);
        assertSpawnRequests("dusteffect", VfxClaimPolicy.SCENE_DUST);
    }

    @Test
    public void spawnHappyPathQueuesTheF19MembersThroughTheFactorySeam() {
        // The two newest claimable FQNs (the per-instance flip members); the capturing factory proves
        // each alias requests exactly its FQN without touching the ImageMaster-backed constructors.
        assertSpawnRequests("lightningorbpassive", VfxClaimPolicy.LIGHTNING_ORB_PASSIVE);
        assertSpawnRequests("lop", VfxClaimPolicy.LIGHTNING_ORB_PASSIVE);
        assertSpawnRequests("glowyfireeyes", VfxClaimPolicy.GLOWY_FIRE_EYES);
        assertSpawnRequests("glowyeyes", VfxClaimPolicy.GLOWY_FIRE_EYES);
    }

    @Test
    public void spawnHappyPathQueuesTheF20MembersThroughTheFactorySeam() {
        // The two newest claimable FQNs; the capturing factory proves each alias requests exactly its
        // FQN without touching the ImageMaster-backed constructors (ConeEffect is constructed via its
        // NO-ARG constructor).
        assertSpawnRequests("flyingspike", VfxClaimPolicy.FLYING_SPIKE);
        assertSpawnRequests("spike", VfxClaimPolicy.FLYING_SPIKE);
        assertSpawnRequests("cone", VfxClaimPolicy.CONE_EFFECT);
    }

    @Test
    public void spawnHappyPathQueuesTheF21GuardMembersThroughTheFactorySeam() {
        // The two newest claimable FQNs (the native wait-guard members); the capturing factory proves
        // each alias requests exactly its FQN without touching the ImageMaster-backed constructors
        // (FallingIceEffect's ctor selects static FROST_* textures; DamageHeartEffect's loadImage()
        // picks its AtlasRegion from ImageMaster art — both may be null off-game).
        assertSpawnRequests("fallingice", VfxClaimPolicy.FALLING_ICE);
        assertSpawnRequests("icefall", VfxClaimPolicy.FALLING_ICE);
        assertSpawnRequests("damageheart", VfxClaimPolicy.DAMAGE_HEART);
        assertSpawnRequests("heart", VfxClaimPolicy.DAMAGE_HEART);
    }

    @Test
    public void spawnHappyPathQueuesTheF22MirrorMembersThroughTheFactorySeam() {
        // The two newest claimable FQNs (the img-path per-instance mirror members); the capturing
        // factory proves each alias requests exactly its FQN without running the NO-ARG constructors
        // (whose img comes from static ImageMaster art and may be null off-game).
        assertSpawnRequests("spookychest", VfxClaimPolicy.SPOOKY_CHEST);
        assertSpawnRequests("spooky", VfxClaimPolicy.SPOOKY_CHEST);
        assertSpawnRequests("victoryflame", VfxClaimPolicy.IRONCLAD_VICTORY_FLAME);
        assertSpawnRequests("ironcladvictory", VfxClaimPolicy.IRONCLAD_VICTORY_FLAME);
    }

    @Test
    public void spawnHappyPathQueuesTheF23MembersThroughTheFactorySeam() {
        // The three newest claimable FQNs; the capturing factory proves each alias requests exactly
        // its FQN without running the NO-ARG constructors (whose img comes from static ImageMaster
        // art and may be null off-game).
        assertSpawnRequests("spookierchest", VfxClaimPolicy.SPOOKIER_CHEST);
        assertSpawnRequests("spookier", VfxClaimPolicy.SPOOKIER_CHEST);
        assertSpawnRequests("campfiresleepcover", VfxClaimPolicy.CAMPFIRE_SLEEP_COVER);
        assertSpawnRequests("sleepcover", VfxClaimPolicy.CAMPFIRE_SLEEP_COVER);
        assertSpawnRequests("deathfloaty", VfxClaimPolicy.DEATH_SCREEN_FLOATY);
        assertSpawnRequests("deathscreen", VfxClaimPolicy.DEATH_SCREEN_FLOATY);
    }

    @Test
    public void spawnHappyPathQueuesTheF24WrathStanceChangeThroughTheFactorySeam() {
        // The newest (F24) claimable FQN; the capturing factory proves both aliases request exactly
        // that FQN without running the GL/ImageMaster-backed constructor (STRIKE_LINE may be null
        // off-game). "wrathchange"/"wrathstance" do not collide with the existing "wrath" alias.
        assertSpawnRequests("wrathchange", VfxClaimPolicy.WRATH_STANCE_CHANGE);
        assertSpawnRequests("wrathstance", VfxClaimPolicy.WRATH_STANCE_CHANGE);
    }

    @Test
    public void spawnHappyPathQueuesTheF25AbsorptionThroughTheFactorySeam() {
        // The newest (F25) claimable FQN; the capturing factory proves both aliases request exactly
        // that FQN without running the ImageMaster-backed constructor (WOBBLY_ORB_VFX is resolved at
        // draw time and may be null off-game). "absorption"/"absorb" do not collide with any existing
        // alias.
        assertSpawnRequests("absorption", VfxClaimPolicy.STANCE_CHANGE_ABSORPTION);
        assertSpawnRequests("absorb", VfxClaimPolicy.STANCE_CHANGE_ABSORPTION);
    }

    @Test
    public void spawnHappyPathQueuesTheF27MembersThroughTheFactorySeam() {
        // The two newest (F27) claimable FQNs; the capturing factory proves each alias requests
        // exactly its FQN without running the ImageMaster-backed constructors (img may be null
        // off-game). "watersplash"/"splash" and "buffparticle"/"buffp" do not collide with any
        // existing alias.
        assertSpawnRequests("watersplash", VfxClaimPolicy.WATER_SPLASH);
        assertSpawnRequests("splash", VfxClaimPolicy.WATER_SPLASH);
        assertSpawnRequests("buffparticle", VfxClaimPolicy.BUFF_PARTICLE);
        assertSpawnRequests("buffp", VfxClaimPolicy.BUFF_PARTICLE);
    }

    @Test
    public void spawnHappyPathQueuesTheF28MembersThroughTheFactorySeam() {
        // The two newest (F28) claimable FQNs; the capturing factory proves each alias requests
        // exactly its FQN without running the ImageMaster-backed constructors (BottomFogEffect's
        // boolean ctor, GiantFireEffect's NO-ARG ctor; both imgs may be null off-game).
        // "bottomfog"/"bfog" and "giantfire"/"gfire" do not collide with any existing alias.
        assertSpawnRequests("bottomfog", VfxClaimPolicy.BOTTOM_FOG);
        assertSpawnRequests("bfog", VfxClaimPolicy.BOTTOM_FOG);
        assertSpawnRequests("giantfire", VfxClaimPolicy.GIANT_FIRE);
        assertSpawnRequests("gfire", VfxClaimPolicy.GIANT_FIRE);
    }

    @Test
    public void spawnHappyPathQueuesTheF29TorchHeadFireThroughTheFactorySeam() {
        // The newest (F29) claimable FQN; the capturing factory proves both aliases request exactly
        // that FQN without running the ImageMaster-backed constructor (img may be null off-game).
        // "torchheadfire"/"torchhead" do not collide with the existing "torch"/"torchxl"/"torchm"/
        // "torchs" aliases.
        assertSpawnRequests("torchheadfire", VfxClaimPolicy.TORCH_HEAD_FIRE);
        assertSpawnRequests("torchhead", VfxClaimPolicy.TORCH_HEAD_FIRE);
    }

    @Test
    public void spawnHappyPathQueuesTheF30CardTrailThroughTheFactorySeam() {
        // The newest (F30) claimable FQN; the capturing factory proves both aliases request exactly
        // that FQN without running the ImageMaster-backed NO-ARG constructor (its static img may be
        // null off-game). "cardtrail"/"trail" do not collide with any existing alias.
        assertSpawnRequests("cardtrail", VfxClaimPolicy.CARD_TRAIL);
        assertSpawnRequests("trail", VfxClaimPolicy.CARD_TRAIL);
    }

    @Test
    public void spawnHappyPathQueuesTheB01FlyingOrbThroughTheFactorySeam() {
        // The newest (NRO-04 B01) claimable FQN; the capturing factory proves both aliases request
        // exactly that FQN without running the ctor (which reads AbstractDungeon.player.hb and whose
        // static img may be null off-game). "flyingorb"/"orb" do not collide with any existing alias.
        assertSpawnRequests("flyingorb", VfxClaimPolicy.FLYING_ORB);
        assertSpawnRequests("orb", VfxClaimPolicy.FLYING_ORB);
    }

    @Test
    public void spawnHappyPathQueuesTheB02FlickCoinThroughTheFactorySeam() {
        // The newest (NRO-04 B02) claimable FQN; the capturing factory proves both aliases request
        // exactly that FQN without running the ctor (whose static img comes from ImageMaster.vfxAtlas
        // and may be null off-game). "flickcoin"/"coin" do not collide with any existing alias.
        assertSpawnRequests("flickcoin", VfxClaimPolicy.FLICK_COIN);
        assertSpawnRequests("coin", VfxClaimPolicy.FLICK_COIN);
    }

    @Test
    public void spawnHappyPathQueuesTheB03HealPanelThroughTheFactorySeam() {
        // The newest (NRO-04 B03) claimable FQN; the capturing factory proves both aliases request
        // exactly that FQN without running the ctor (whose static img is loaded via
        // ImageMaster.loadImage(...) and which reads Settings.scale, both unavailable off-game).
        // "healpanel"/"heal" do not collide with any existing alias.
        assertSpawnRequests("healpanel", VfxClaimPolicy.HEAL_PANEL);
        assertSpawnRequests("heal", VfxClaimPolicy.HEAL_PANEL);
    }

    @Test
    public void spawnHappyPathQueuesTheB04PingHpThroughTheFactorySeam() {
        // The newest (NRO-04 B04) claimable FQN; the capturing factory proves both aliases request
        // exactly that FQN without running the ctor (which reads Settings.scale and whose static
        // ImageMaster.TP_HP texture may be null off-game). "pinghp"/"ping" do not collide with any
        // existing alias.
        assertSpawnRequests("pinghp", VfxClaimPolicy.PING_HP);
        assertSpawnRequests("ping", VfxClaimPolicy.PING_HP);
    }

    @Test
    public void spawnHappyPathQueuesTheB05RewardGlowThroughTheFactorySeam() {
        // The newest (NRO-04 B05) claimable FQN; the capturing factory proves both aliases request
        // exactly that FQN without running the ctor (which reads Settings.scale and whose static
        // ImageMaster.REWARD_SCREEN_ITEM texture may be null off-game). "rewardglow"/"reward" do not
        // collide with any existing alias.
        assertSpawnRequests("rewardglow", VfxClaimPolicy.REWARD_GLOW);
        assertSpawnRequests("reward", VfxClaimPolicy.REWARD_GLOW);
    }

    @Test
    public void spawnHappyPathQueuesTheB06MapCircleThroughTheFactorySeam() {
        // The newest (NRO-04 B06) claimable FQN; the capturing factory proves both aliases request
        // exactly that FQN without running the ctor (whose PUBLIC STATIC img = ImageMaster.MAP_CIRCLE_1
        // may be null off-game, and which update() later swaps). "mapcircle"/"map" do not collide with
        // any existing alias.
        assertSpawnRequests("mapcircle", VfxClaimPolicy.MAP_CIRCLE);
        assertSpawnRequests("map", VfxClaimPolicy.MAP_CIRCLE);
    }

    @Test
    public void spawnHappyPathQueuesTheB07SpotlightThroughTheFactorySeam() {
        // The newest (NRO-04 B07) claimable FQN; the capturing factory proves the alias requests
        // exactly that FQN without running the NO-ARG ctor (whose static ImageMaster.SPOTLIGHT_VFX
        // texture may be null off-game). "spotlight" does not collide with any existing alias.
        assertSpawnRequests("spotlight", VfxClaimPolicy.SPOTLIGHT);
    }

    @Test
    public void cardTrailLabConstructInitializesTheEffect() throws Exception {
        // CardTrailEffect is a pooled Pool.Poolable effect: its no-arg ctor only selects the static
        // img, while color/x/y/scale/duration come from init(x, y). VfxLabSpawn.construct must
        // therefore call init (F30b). Exercise the private construct directly (the real lab path,
        // not the capturing factory), presetting the static img so the ctor is GL-free. Off a live
        // game init reads AbstractDungeon.player and aborts, so we assert the init invocation via the
        // reached call frame; if a live-ish context happens to let init complete we assert the full
        // initialized contract. Never touches GL.
        Field imgField = CardTrailEffect.class.getDeclaredField("img");
        imgField.setAccessible(true);
        Object previousImg = imgField.get(null);
        imgField.set(null, dummyAtlasRegion());
        try {
            Method construct = VfxLabSpawn.class.getDeclaredMethod("construct", String.class);
            construct.setAccessible(true);
            Object result = null;
            Throwable failure = null;
            try {
                result = construct.invoke(null, VfxClaimPolicy.CARD_TRAIL);
            } catch (java.lang.reflect.InvocationTargetException wrapped) {
                failure = wrapped.getCause();
            }

            if (result != null) {
                // A live-ish context let init run to completion: the inherited color is set and x/y
                // are the lab point (960f, 540f) shifted by init's fixed -6f.
                assertTrue("the lab construct returns a CardTrailEffect",
                        result instanceof CardTrailEffect);
                assertEquals("init sets x to the lab x minus 6f", 954f,
                        ((Number) readInstanceField(result, "x")).floatValue(), 0f);
                assertEquals("init sets y to the lab y minus 6f", 534f,
                        ((Number) readInstanceField(result, "y")).floatValue(), 0f);
                assertNotNull("init sets the inherited color (the renderer requires a Color)",
                        readInstanceField(result, "color"));
            } else {
                // Off-game init aborts on the AbstractDungeon.player read; the reached frame proves
                // the lab CARD_TRAIL path invoked init (the fix). The ctor itself is GL-free here
                // because the static img is preset, so the failure can only come from init.
                assertNotNull("construct must fail only after reaching init", failure);
                assertTrue("the lab CARD_TRAIL path must invoke CardTrailEffect.init(...)",
                        stackMentions(failure, CardTrailEffect.class.getName(), "init"));
            }
        } finally {
            imgField.set(null, previousImg);
        }
    }

    @Test
    public void cardTrailLabPathDrivesInitFromTheContract() throws Exception {
        // Tie the contract to the REAL native method and prove the lab wiring chain end-to-end,
        // purely by reflection (no construct invocation, no GL). The existing
        // cardTrailLabConstructInitializesTheEffect test proves construct actually reaches init.
        VfxDrawGeometry.Kind kind = VfxDrawGeometry.Kind.CARD_TRAIL;
        String initializer = VfxInitContract.initializerMethod(kind);
        assertEquals("init", initializer);
        assertNotNull("CardTrailEffect must declare the contract's initializer method",
                CardTrailEffect.class.getMethod(initializer, float.class, float.class));

        // Lab wiring chain: alias -> policy FQN -> draw kind -> contract.
        assertEquals(VfxDrawGeometry.Kind.CARD_TRAIL,
                VfxDrawGeometry.kindFor(VfxLabSpawn.classNameFor("cardtrail")));
        assertTrue(VfxInitContract.requiresRuntimeInit(
                VfxDrawGeometry.kindFor(VfxLabSpawn.classNameFor("trail"))));
    }

    private static void assertSpawnRequests(String alias, String expectedFqn) {
        RecordingQueue queue = new RecordingQueue();
        CapturingFactory factory = new CapturingFactory();
        VfxLabSpawn.setQueueForTests(queue);
        VfxLabSpawn.setFactoryForTests(factory);

        assertEquals(4, VfxLabSpawn.spawn(alias, 4));
        assertEquals(4, queue.added.size());
        assertEquals(4, factory.requested.size());
        for (String fqn : factory.requested) {
            assertEquals(expectedFqn, fqn);
        }
    }

    @Test
    public void clearRetiresMatchingEffectsByFlagAndReturnsCount() {
        RetiringQueue queue = new RetiringQueue();
        AbstractGameEffect aura = claimable(StanceAuraEffect.class);
        AbstractGameEffect wrath = claimable(WrathParticleEffect.class);
        AbstractGameEffect other = new StubEffect();
        queue.add(aura);
        queue.add(wrath);
        queue.add(other);
        VfxLabSpawn.setQueueForTests(queue);

        assertEquals(2, VfxLabSpawn.clear());

        // Matching effects are retired through the game-native isDone flag, not structurally removed.
        assertTrue("matching aura should be marked done", aura.isDone);
        assertTrue("matching wrath should be marked done", wrath.isDone);
        assertFalse("non-claimable effect must not be retired", other.isDone);
        // No structural mutation: every effect is still present in the backing container.
        assertEquals(3, queue.backing.size());
    }

    @Test
    public void clearDoesNotRetireNonMatchingEffects() {
        RetiringQueue queue = new RetiringQueue();
        AbstractGameEffect other = new StubEffect();
        queue.add(other);
        VfxLabSpawn.setQueueForTests(queue);

        assertEquals(0, VfxLabSpawn.clear());
        assertFalse(other.isDone);
        assertEquals(1, queue.backing.size());
    }

    @Test
    public void clearReturnsZeroWhenQueueThrows() {
        VfxLabSpawn.setQueueForTests(new ThrowingQueue());
        assertEquals(0, VfxLabSpawn.clear());
    }

    /** Allocates a real claimable effect without running a GL-backed constructor. */
    private static AbstractGameEffect claimable(Class<? extends AbstractGameEffect> type) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            return (AbstractGameEffect) unsafe.getClass()
                    .getMethod("allocateInstance", Class.class)
                    .invoke(unsafe, type);
        } catch (Exception failure) {
            throw new AssertionError("could not allocate claimable effect " + type, failure);
        }
    }

    /**
     * Reads one field walking the superclass chain (so the inherited {@code color}/{@code x} on a
     * real {@code CardTrailEffect} resolve). Returns {@code null} when absent.
     */
    private static Object readInstanceField(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception failure) {
                throw new AssertionError("could not read field " + name, failure);
            }
        }
        return null;
    }

    /** True when the throwable's stack contains a frame for {@code owner.name}. */
    private static boolean stackMentions(Throwable failure, String owner, String name) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                if (owner.equals(frame.getClassName()) && name.equals(frame.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A GL-free {@code TextureAtlas.AtlasRegion} so the CardTrailEffect ctor can run headless. */
    private static Object dummyAtlasRegion() throws Exception {
        java.lang.reflect.Field unsafeField =
                sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        return unsafe.getClass().getMethod("allocateInstance", Class.class)
                .invoke(unsafe, TextureAtlas.AtlasRegion.class);
    }

    /**
     * Minimal live-container stub: keeps its backing list (so a test can assert no structural
     * mutation) and retires by setting the {@code isDone} flag exactly like {@code DungeonQueue}.
     */
    private static final class RetiringQueue implements VfxLabSpawn.Queue {
        final List<AbstractGameEffect> backing = new ArrayList<AbstractGameEffect>();

        @Override
        public boolean add(AbstractGameEffect effect) {
            backing.add(effect);
            return true;
        }

        @Override
        public int retireMatching(Predicate<AbstractGameEffect> match) {
            int retired = 0;
            for (AbstractGameEffect effect : backing) {
                boolean matched;
                try {
                    matched = match.test(effect);
                } catch (Throwable error) {
                    matched = false;
                }
                if (matched && effect != null) {
                    effect.isDone = true;
                    retired++;
                }
            }
            return retired;
        }
    }

    /** Records adds; retirement is a no-op. */
    private static final class RecordingQueue implements VfxLabSpawn.Queue {
        final List<AbstractGameEffect> added = new ArrayList<AbstractGameEffect>();

        @Override
        public boolean add(AbstractGameEffect effect) {
            added.add(effect);
            return true;
        }

        @Override
        public int retireMatching(Predicate<AbstractGameEffect> match) {
            return 0;
        }
    }

    /** Accepts nothing: mirrors the live DungeonQueue when no game context exists. */
    private static final class DecliningQueue implements VfxLabSpawn.Queue {
        @Override
        public boolean add(AbstractGameEffect effect) {
            return false;
        }

        @Override
        public int retireMatching(Predicate<AbstractGameEffect> match) {
            return 0;
        }
    }

    /** Supplies stub effects so construction never needs a live GL/asset context. */
    private static final class StubFactory implements VfxLabSpawn.EffectFactory {
        @Override
        public AbstractGameEffect create(String fqn) {
            return new StubEffect();
        }
    }

    /** Supplies stub effects and records each requested FQN so the mapping can be asserted. */
    private static final class CapturingFactory implements VfxLabSpawn.EffectFactory {
        final List<String> requested = new ArrayList<String>();

        @Override
        public AbstractGameEffect create(String fqn) {
            requested.add(fqn);
            return new StubEffect();
        }
    }

    /**
     * Minimal concrete effect; its own class name is not a claimable FQN, so it stands in for a
     * non-matching effect. The stubs never render or update it.
     */
    private static final class StubEffect extends AbstractGameEffect {
        @Override
        public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {}

        @Override
        public void dispose() {}
    }

    private static final class ThrowingQueue implements VfxLabSpawn.Queue {
        @Override
        public boolean add(AbstractGameEffect effect) {
            throw new IllegalStateException("no context");
        }

        @Override
        public int retireMatching(Predicate<AbstractGameEffect> match) {
            throw new IllegalStateException("no context");
        }
    }
}
