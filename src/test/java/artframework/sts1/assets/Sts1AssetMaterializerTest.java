package artframework.sts1.assets;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import com.badlogic.gdx.graphics.Texture;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;
import artframework.render.RenderStateEcs;
import artframework.sts1.PresentSafety;
import org.junit.After;
import org.junit.Test;

import java.util.Map;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Sts1AssetMaterializerTest {

    @After
    public void restoreDefaultCache() {
        Sts1AssetMaterializer.setCacheForTests(null);
        Sts1AssetMaterializer.resetForTests();
        ArtFramework.resetForTests();
    }

    @Test
    public void classifiesVanillaFileAndLogicalCardSourcesWithoutGl() {
        assertEquals("images/512/frame_attack_red.png",
                Sts1AssetMaterializer.normalize("sts1:images/512/frame_attack_red.png"));
        assertTrue(Sts1AssetMaterializer.isFileBacked("sts1:images/512/frame_attack_red.png"));
        assertTrue(Sts1AssetMaterializer.isLogicalCardArt("sts1:card/art/Strike_R"));
        assertFalse(Sts1AssetMaterializer.isFileBacked("sts1:card/art/Strike_R"));
        assertTrue(Sts1AssetMaterializer.isFileBacked("sts1:images/ui/topPanel/endTurnButton.png"));
        assertTrue(Sts1AssetMaterializer.isFileBacked("sts1:images/ui/map/monster.png"));
        assertFalse(Sts1AssetMaterializer.isFileBacked("pack://custom/node.png"));
        assertFalse(Sts1AssetMaterializer.isFileBacked(""));
    }

    @Test
    public void recognizesCardUiAtlasFramesAndSelectsByTypeAndRarityWithoutGl() {
        assertTrue(Sts1AssetMaterializer.isCardFrameAtlas("sts1:cardui/frame"));
        assertFalse(Sts1AssetMaterializer.isFileBacked("sts1:cardui/frame"));
        assertEquals("attack.common", Sts1AssetMaterializer.cardFrameAtlasKey("ATTACK", "BASIC"));
        assertEquals("skill.uncommon", Sts1AssetMaterializer.cardFrameAtlasKey("SKILL", "UNCOMMON"));
        assertEquals("power.rare", Sts1AssetMaterializer.cardFrameAtlasKey("POWER", "RARE"));
        assertEquals("attack.common", Sts1AssetMaterializer.cardFrameAtlasKey("STATUS", "SPECIAL"));
    }

    @Test
    public void boundedCacheDisposesExactlyOnceAndCanBeRecreated() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<AutoCloseable> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<AutoCloseable>();
        AutoCloseable value = new AutoCloseable() {
            public void close() { disposals[0]++; }
        };
        cache.put("images/a.png", value);
        assertTrue(cache.get("images/a.png") == value);
        cache.clear();
        cache.clear();
        assertEquals(1, disposals[0]);
        cache.put("images/a.png", value);
        cache.dispose();
        cache.dispose();
        assertEquals(2, disposals[0]);
    }

    @Test
    public void boundedCacheEvictsAndReplacesWithoutDoubleDisposal() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(1,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) { disposals[0]++; }
                        });
        Object first = new Object();
        Object replacement = new Object();
        cache.put("a", first);
        cache.put("a", replacement);
        assertEquals(1, disposals[0]);
        cache.put("b", new Object());
        assertEquals(2, disposals[0]);
        cache.clear();
        assertEquals(3, disposals[0]);
        cache.clear();
        assertEquals(3, disposals[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void disposedCacheCannotBeReused() {
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>();
        cache.dispose();
        cache.put("a", new Object());
    }

    @Test
    public void cacheClearResetsFallbackAndDisposesSharedValueOnce() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(2,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) { disposals[0]++; }
                        });
        Object shared = new Object();
        cache.put("a", shared);
        cache.put("b", shared);
        cache.markMissing("missing");
        assertTrue(cache.isMissing("missing"));
        cache.clear();
        assertEquals(1, disposals[0]);
        assertFalse(cache.isMissing("missing"));
        cache.put("a", new Object());
        assertEquals(1, cache.size());
    }

    @Test
    public void hostRecreationLifecycleClearsDisposableStateWithoutChangingCacheContract() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(2,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) { disposals[0]++; }
                        });
        Object beforeRecreation = new Object();
        cache.put("images/before.png", beforeRecreation);
        cache.markMissing("images/missing.png");

        cache.clear();

        assertEquals(1, disposals[0]);
        assertEquals(null, cache.get("images/before.png"));
        assertFalse(cache.isMissing("images/missing.png"));
        Object afterRecreation = new Object();
        cache.put("images/before.png", afterRecreation);
        assertTrue(cache.get("images/before.png") == afterRecreation);
    }

    @Test
    public void missingEntriesUseTheSameFixedCapacityAsValues() {
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(2,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) { }
                        });
        cache.markMissing("a");
        cache.markMissing("b");
        assertTrue(cache.isMissing("a"));
        cache.markMissing("c");

        assertEquals(2, missingSize(cache, "a", "b", "c"));
        assertTrue(cache.isMissing("a"));
        assertFalse(cache.isMissing("b"));
        assertTrue(cache.isMissing("c"));
    }

    @Test
    public void disposalUsesIdentityRatherThanEqualsForCachedValues() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<EqualValue> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<EqualValue>(2,
                        new Sts1AssetMaterializer.Disposer<EqualValue>() {
                            public void dispose(EqualValue value) { disposals[0]++; }
                        });
        EqualValue cached = new EqualValue();
        EqualValue replacement = new EqualValue();
        cache.put("cached", cached);
        cache.put("replacement", replacement);
        cache.put("replacement", new EqualValue());

        assertEquals(1, disposals[0]);
    }

    @Test
    public void clearDetachesEverythingAndAttemptsEachResidentAfterDisposerFailure() {
        final java.util.List<Object> attempted = new java.util.ArrayList<Object>();
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(3,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) {
                                attempted.add(value);
                                if (attempted.size() == 1) throw new IllegalStateException("first");
                            }
                        });
        Object first = new Object();
        Object second = new Object();
        cache.put("a", first);
        cache.put("b", second);
        cache.markMissing("missing");

        try {
            cache.clear();
            throw new AssertionError("cleanup failure must be reported");
        } catch (IllegalStateException expected) {
            assertEquals("first", expected.getMessage());
        }
        assertEquals(2, attempted.size());
        assertEquals(null, cache.get("a"));
        assertEquals(null, cache.get("b"));
        assertFalse(cache.isMissing("missing"));
        cache.put("rebuilt", new Object());
    }

    @Test
    public void clearAttemptsEachIdentityOnceAndPreservesLaterFailuresAsSuppressed() {
        final java.util.List<Object> attempted = new java.util.ArrayList<Object>();
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(3,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) {
                                attempted.add(value);
                                throw new IllegalStateException(String.valueOf(attempted.size()));
                            }
                        });
        Object shared = new Object();
        Object other = new Object();
        cache.put("a", shared);
        cache.put("b", shared);
        cache.put("c", other);

        try {
            cache.clear();
            throw new AssertionError("cleanup failure must be reported");
        } catch (IllegalStateException expected) {
            assertEquals("1", expected.getMessage());
            assertEquals(1, expected.getSuppressed().length);
        }
        assertEquals(2, attempted.size());
        cache.clear();
        assertEquals("failed cleanup must not retain or retry detached residents",
                2, attempted.size());
    }

    @Test
    public void disposeRemainsTerminalWhenDisposerFails() {
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(1,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) { throw new AssertionError("dispose"); }
                        });
        cache.put("a", new Object());
        try {
            cache.dispose();
            throw new AssertionError("cleanup failure must be reported");
        } catch (AssertionError expected) {
            assertEquals("dispose", expected.getMessage());
        }
        try {
            cache.put("b", new Object());
            throw new AssertionError("disposed cache must remain closed");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void disposeAttemptsEachResidentIdentityEvenWhenDisposerReusesOneFailure() {
        final java.util.List<Object> attempted = new java.util.ArrayList<Object>();
        final AssertionError failure = new AssertionError("shared failure");
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(3,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) {
                                attempted.add(value);
                                throw failure;
                            }
                        });
        Object shared = new Object();
        Object other = new Object();
        cache.put("a", shared);
        cache.put("b", shared);
        cache.put("c", other);

        try {
            cache.dispose();
            throw new AssertionError("cleanup failure must be reported");
        } catch (AssertionError expected) {
            assertTrue(expected == failure);
        }
        assertEquals(2, attempted.size());
        assertEquals(0, failure.getSuppressed().length);
        try {
            cache.put("rebuilt", new Object());
            throw new AssertionError("dispose must remain terminal after cleanup failure");
        } catch (IllegalStateException expected) {
            // expected
        }
        cache.clear();
        assertEquals(2, attempted.size());
    }

    @Test
    public void productionHostRecreationClearsMaterializerAndStillProjectsAfterCleanupFailure() {
        RenderStateEcs.surface("sts1.materializer-host-recreate", 4f, 5f, 40f, 50f, true);
        final int[] clears = {0};
        Sts1AssetMaterializer.setCacheForTests(new Sts1AssetMaterializer.TextureCache<Texture>() {
            public Texture get(String key) { return null; }
            public void put(String key, Texture value) { }
            public boolean isMissing(String key) { return false; }
            public void markMissing(String key) { }
            public void clear() {
                clears[0]++;
                throw new IllegalStateException("materializer cleanup");
            }
            public int size() { return 0; }
        });

        PresentSafety.onHostRecreated();

        assertEquals(1, clears[0]);
        assertEquals(1, RenderStateEcs.context().entities().size());
        assertTrue(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.materializer-host-recreate")) != null);
    }


    @Test
    public void probeCountsDistinctResidentsAndClearAttempts() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache = countingCache(2, disposals);
        Object first = new Object();
        Object second = new Object();
        cache.put("a", first);
        cache.put("b", second);
        cache.markMissing("missing");

        Map<String, Object> beforeClear = cache.probeSlice();
        assertEquals(Integer.valueOf(2), beforeClear.get("residentCount"));
        assertEquals(Integer.valueOf(1), beforeClear.get("missingCount"));
        assertEquals(Integer.valueOf(2), beforeClear.get("putCount"));
        assertEquals(Integer.valueOf(0), beforeClear.get("disposeCount"));

        cache.clear();

        assertEquals(2, disposals[0]);
        Map<String, Object> afterClear = cache.probeSlice();
        assertEquals(Integer.valueOf(0), afterClear.get("residentCount"));
        assertEquals(Integer.valueOf(0), afterClear.get("missingCount"));
        assertEquals(Integer.valueOf(2), afterClear.get("putCount"));
        assertEquals(Integer.valueOf(2), afterClear.get("disposeCount"));
    }

    @Test
    public void probePutCountIncreasesWhenAClearedIdentityIsRematerialized() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache = countingCache(2, disposals);
        cache.put("a", new Object());
        cache.put("b", new Object());
        cache.clear();
        int putCountAfterClear = ((Integer) cache.probeSlice().get("putCount")).intValue();

        cache.put("a", new Object());

        Map<String, Object> rematerialized = cache.probeSlice();
        assertEquals(Integer.valueOf(1), rematerialized.get("residentCount"));
        assertEquals(Integer.valueOf(putCountAfterClear + 1), rematerialized.get("putCount"));
        assertEquals(2, disposals[0]);
    }

    @Test
    public void replacementAndEvictionIncrementDisposeCountForUnreferencedIdentities() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache = countingCache(1, disposals);
        Object first = new Object();
        Object replacement = new Object();
        Object evictedReplacement = new Object();
        cache.put("a", first);
        cache.put("a", replacement);
        assertEquals(1, disposals[0]);
        assertEquals(Integer.valueOf(1), cache.probeSlice().get("disposeCount"));

        cache.put("b", evictedReplacement);

        assertEquals(2, disposals[0]);
        Map<String, Object> afterEviction = cache.probeSlice();
        assertEquals(Integer.valueOf(1), afterEviction.get("residentCount"));
        assertEquals(Integer.valueOf(2), afterEviction.get("disposeCount"));
        assertEquals(Integer.valueOf(3), afterEviction.get("putCount"));
        assertTrue(cache.get("b") == evictedReplacement);
    }

    @Test
    public void disposerFailureStillRecordsEachAttemptedIdentityAndDetachesResidents() {
        final java.util.List<Object> attempted = new java.util.ArrayList<Object>();
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Object>(3,
                        new Sts1AssetMaterializer.Disposer<Object>() {
                            public void dispose(Object value) {
                                attempted.add(value);
                                throw new IllegalStateException("first");
                            }
                        });
        Object first = new Object();
        Object second = new Object();
        cache.put("a", first);
        cache.put("b", second);
        cache.markMissing("missing");

        try {
            cache.clear();
            throw new AssertionError("cleanup failure must be reported");
        } catch (IllegalStateException expected) {
            assertEquals("first", expected.getMessage());
        }

        assertEquals(2, attempted.size());
        Map<String, Object> afterFailedClear = cache.probeSlice();
        assertEquals(Integer.valueOf(0), afterFailedClear.get("residentCount"));
        assertEquals(Integer.valueOf(0), afterFailedClear.get("missingCount"));
        assertEquals(Integer.valueOf(2), afterFailedClear.get("disposeCount"));
        assertEquals(null, cache.get("a"));
        assertEquals(null, cache.get("b"));
        assertFalse(cache.isMissing("missing"));
    }

    @Test
    public void probeDoesNotMutateCacheOrCounters() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache = countingCache(2, disposals);
        Object first = new Object();
        Object second = new Object();
        cache.put("a", first);
        cache.put("b", second);
        cache.markMissing("missing");
        assertTrue(cache.get("a") == first);
        Map<String, Object> before = copyProbe(cache.probeSlice());
        assertEquals(Integer.valueOf(1), before.get("hitCount"));

        Map<String, Object> probed = cache.probeSlice();
        Map<String, Object> probedAgain = cache.probeSlice();

        assertEquals(before, probed);
        assertEquals(before, probedAgain);
        assertEquals(Integer.valueOf(1), probedAgain.get("hitCount"));
        assertEquals(0, disposals[0]);
        assertTrue(cache.get("a") == first);
        assertTrue(cache.get("b") == second);
        assertTrue(cache.isMissing("missing"));
    }

    @Test
    public void probeHitCountIncrementsOnlyOnNonNullGetAndSurvivesClear() {
        final int[] disposals = {0};
        Sts1AssetMaterializer.BoundedTextureCache<Object> cache = countingCache(2, disposals);
        Object resident = new Object();
        cache.put("a", resident);
        cache.markMissing("missing");

        assertEquals(Integer.valueOf(0), cache.probeSlice().get("hitCount"));
        assertEquals(null, cache.get("absent"));
        assertEquals(Integer.valueOf(0), cache.probeSlice().get("hitCount"));
        assertEquals(null, cache.get("missing"));
        assertEquals(Integer.valueOf(0), cache.probeSlice().get("hitCount"));

        Map<String, Object> beforeProbe = copyProbe(cache.probeSlice());
        cache.probeSlice();
        cache.probeSlice();
        assertEquals(beforeProbe.get("hitCount"), cache.probeSlice().get("hitCount"));

        assertTrue(cache.get("a") == resident);
        assertEquals(Integer.valueOf(1), cache.probeSlice().get("hitCount"));
        assertTrue(cache.get("a") == resident);
        assertEquals(Integer.valueOf(2), cache.probeSlice().get("hitCount"));

        cache.clear();

        Map<String, Object> afterClear = cache.probeSlice();
        assertEquals(Integer.valueOf(0), afterClear.get("residentCount"));
        assertEquals(Integer.valueOf(0), afterClear.get("missingCount"));
        assertEquals(Integer.valueOf(2), afterClear.get("hitCount"));
        assertEquals(1, disposals[0]);
        assertEquals(null, cache.get("a"));
        assertEquals(Integer.valueOf(2), cache.probeSlice().get("hitCount"));
    }

    @Test
    public void energyAttributionIsOnlyRecordedAfterSuccessfulResolutionAndClearedWithCache() {
        assertFalse(Sts1AssetMaterializer.energyAttributionProbe().containsKey("resourceId"));
        Sts1AssetMaterializer.BoundedTextureCache<Texture> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Texture>(2,
                        new Sts1AssetMaterializer.Disposer<Texture>() {
                            public void dispose(Texture value) { }
                        });
        Sts1AssetMaterializer.setCacheForTests(cache);
        cache.put("images/ui/topPanel/red/layer1.png", null);
        // A null materialization is not evidence of a rendered/resident energy resource.
        assertEquals(null, Sts1AssetMaterializer.resolveEnergyTexture(
                "ui.energy.red.layer1",
                artframework.assets.AssetResolveResult.hit("ui.energy.red.layer1", "sts1", "images/ui/topPanel/red/layer1.png")));
        assertFalse(Sts1AssetMaterializer.energyAttributionProbe().containsKey("resourceId"));
        assertTrue(Sts1AssetMaterializer.energyAttributionProbe().get("attributed") == Boolean.FALSE);
    }

    @Test
    public void energyResolutionDoesNotAttributeCachedPathForNonEnergyResult() {
        Sts1AssetMaterializer.BoundedTextureCache<Texture> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Texture>(2,
                        new Sts1AssetMaterializer.Disposer<Texture>() {
                            public void dispose(Texture value) { }
                        });
        cache.put("images/ui/topPanel/red/layer1.png", null);
        Sts1AssetMaterializer.setCacheForTests(cache);
        seedValidEnergyAttribution(cache, "ui.energy.blue.layer1", "images/ui/topPanel/blue/layer1.png");
        int hitCountBeforeRejectedResolution = ((Integer) cache.probeSlice().get("hitCount")).intValue();

        assertEquals(null, Sts1AssetMaterializer.resolveEnergyTexture(
                "ui.energy.red.layer1",
                AssetResolveResult.hit("cards.some-art", "sts1", "images/ui/topPanel/red/layer1.png")));
        assertEquals(Integer.valueOf(hitCountBeforeRejectedResolution), cache.probeSlice().get("hitCount"));
        assertEquals("ui.energy.blue.layer1",
                Sts1AssetMaterializer.energyAttributionProbe().get("resourceId"));
    }

    @Test
    public void energyResolutionRequiresResultIdentityToMatchRequestedEnergyId() {
        Sts1AssetMaterializer.BoundedTextureCache<Texture> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Texture>(2,
                        new Sts1AssetMaterializer.Disposer<Texture>() {
                            public void dispose(Texture value) { }
                        });
        cache.put("images/ui/topPanel/red/layer1.png", null);
        Sts1AssetMaterializer.setCacheForTests(cache);
        seedValidEnergyAttribution(cache, "ui.energy.blue.layer1", "images/ui/topPanel/blue/layer1.png");
        int hitCountBeforeRejectedResolution = ((Integer) cache.probeSlice().get("hitCount")).intValue();

        assertEquals(null, Sts1AssetMaterializer.resolveEnergyTexture(
                "ui.energy.red.layer1",
                AssetResolveResult.hit("ui.energy.blue.layer1", "sts1", "images/ui/topPanel/red/layer1.png")));
        assertEquals(Integer.valueOf(hitCountBeforeRejectedResolution), cache.probeSlice().get("hitCount"));
        assertEquals("ui.energy.blue.layer1",
                Sts1AssetMaterializer.energyAttributionProbe().get("resourceId"));
    }

    @Test
    public void energyResolutionRejectsUnresolvedAndNonFileBackedResultsBeforeCacheAccess() {
        Sts1AssetMaterializer.BoundedTextureCache<Texture> validCache =
                new Sts1AssetMaterializer.BoundedTextureCache<Texture>(2,
                        new Sts1AssetMaterializer.Disposer<Texture>() {
                            public void dispose(Texture value) { }
                        });
        Sts1AssetMaterializer.setCacheForTests(validCache);
        seedValidEnergyAttribution(validCache, "ui.energy.blue.layer1", "images/ui/topPanel/blue/layer1.png");

        final int[] gets = {0};
        Sts1AssetMaterializer.setCacheForTests(new Sts1AssetMaterializer.TextureCache<Texture>() {
            public Texture get(String key) { gets[0]++; throw new AssertionError("cache must not be consulted"); }
            public void put(String key, Texture value) { }
            public boolean isMissing(String key) { return false; }
            public void markMissing(String key) { }
            public void clear() { }
            public int size() { return 0; }
        });

        assertEquals(null, Sts1AssetMaterializer.resolveEnergyTexture(
                "ui.energy.red.layer1", AssetResolveResult.fallback("ui.energy.red.layer1", "fallback")));
        assertEquals(null, Sts1AssetMaterializer.resolveEnergyTexture(
                "ui.energy.red.layer1",
                AssetResolveResult.hit("ui.energy.red.layer1", "sts1", "card/art/Strike_R")));
        assertEquals(0, gets[0]);
        assertEquals("ui.energy.blue.layer1",
                Sts1AssetMaterializer.energyAttributionProbe().get("resourceId"));
    }

    @Test
    public void energyAttributionRecorderIsNotExternallyCallable() throws Exception {
        java.lang.reflect.Method recorder = Sts1AssetMaterializer.class.getDeclaredMethod(
                "recordSuccessfulEnergyResolution", AssetResolveResult.class);
        assertTrue(Modifier.isPrivate(recorder.getModifiers()));
    }

    @Test
    public void energyAttributionProbeIsReadOnlyAndClearRemovesLifecycleClaim() {
        Map<String, Object> before = Sts1AssetMaterializer.energyAttributionProbe();
        Map<String, Object> again = Sts1AssetMaterializer.energyAttributionProbe();
        assertEquals(before, again);
        Sts1AssetMaterializer.clearCache();
        assertEquals(Boolean.FALSE, Sts1AssetMaterializer.energyAttributionProbe().get("attributed"));
    }

    @Test
    public void materializerProbeNestsUnderBackendAndReportsZeroResidentsAfterProductionClear() {
        Sts1AssetMaterializer.BoundedTextureCache<Texture> cache =
                new Sts1AssetMaterializer.BoundedTextureCache<Texture>(2,
                        new Sts1AssetMaterializer.Disposer<Texture>() {
                            public void dispose(Texture value) { }
                        });
        cache.put("images/a.png", null);
        cache.put("images/b.png", null);
        cache.markMissing("images/missing.png");
        Sts1AssetMaterializer.setCacheForTests(cache);

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) ((Map<String, Object>) ArtFramework.probe().asMap()
                .get("backend")).get("materializer");
        assertEquals(Integer.valueOf(2), nested.get("residentCount"));
        assertEquals(Integer.valueOf(1), nested.get("missingCount"));
        assertEquals(cache.probeSlice(), Sts1AssetMaterializer.probeSlice());

        cache.clear();

        Map<String, Object> afterClear = Sts1AssetMaterializer.probeSlice();
        assertEquals(Integer.valueOf(0), afterClear.get("residentCount"));
        assertEquals(Integer.valueOf(0), afterClear.get("missingCount"));
        assertEquals(afterClear, cache.probeSlice());
    }

    private static Sts1AssetMaterializer.BoundedTextureCache<Object> countingCache(
            int capacity, final int[] disposals) {
        return new Sts1AssetMaterializer.BoundedTextureCache<Object>(capacity,
                new Sts1AssetMaterializer.Disposer<Object>() {
                    public void dispose(Object value) { disposals[0]++; }
                });
    }

    private static Map<String, Object> copyProbe(Map<String, Object> probe) {
        return new java.util.LinkedHashMap<String, Object>(probe);
    }

    private static int missingSize(Sts1AssetMaterializer.TextureCache<?> cache, String... keys) {
        int size = 0;
        for (String key : keys) {
            if (cache.isMissing(key)) size++;
        }
        return size;
    }

    private static void seedValidEnergyAttribution(
            Sts1AssetMaterializer.BoundedTextureCache<Texture> cache,
            String resourceId, String source) {
        Texture texture;
        try {
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            texture = (Texture) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(Texture.class);
        } catch (Exception failure) {
            throw new AssertionError("could not create no-GL texture test double", failure);
        }
        cache.put(Sts1AssetMaterializer.normalize(source), texture);
        assertTrue(Sts1AssetMaterializer.resolveEnergyTexture(
                resourceId, AssetResolveResult.hit(resourceId, "sts1", source)) == texture);
    }

    private static final class EqualValue {
        @Override
        public boolean equals(Object other) { return other instanceof EqualValue; }

        @Override
        public int hashCode() { return 1; }
    }
}
