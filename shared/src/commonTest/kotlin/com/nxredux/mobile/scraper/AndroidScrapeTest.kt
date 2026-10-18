package com.nxredux.mobile.scraper

import com.nxredux.mobile.library.ArtRefs
import com.nxredux.mobile.scraper.Scrape.OK_PNG
import com.nxredux.mobile.scraper.Scrape.found
import com.nxredux.mobile.scraper.Scrape.search
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tools -> Artwork for the Android games console: lookup by label, then search; only `ss`, written to the art store. */
class AndroidScrapeTest {
    private class Rig(scope: TestScope, val store: AndroidArtStore? = FakeArtStore()) {
        val http = FakeHttp().apply { clock = { scope.testScheduler.currentTime } }
        val hasher = FakeHasher()
        val arts = mutableListOf<Pair<String, ArtRefs>>()
        val written = mutableListOf<Pair<String, ArtOrigin?>>()
        val fitter = FakeFitter()
        var refreshed = 0
        val engine = ssEngine(
            http, fitter, hasher, Scrape.homeTree(), Scrape.home, { ScraperAuth("DEV", "DEVPW") }, { scope.testScheduler.currentTime }, { delay(it) },
            onArt = { p, a -> arts += p to a }, onWritten = { written += it },
            androidArt = store, onAndroidArt = { refreshed++ },
        )
        val files get() = (store as FakeArtStore).files

        suspend fun run(games: List<LibraryGameRef>, replace: Boolean = false) =
            engine.run(ScrapeState(items = games.map { Scrape.item(it) }), { p -> games.firstOrNull { it.gamePath == p } }, replace) {}

        /** The API calls in order (image downloads left out). */
        val api get() = http.calls.filter { it.startsWith(ScraperRequest.API) }
    }

    private val pgo = Scrape.androidGame("com.nianticlabs.pokemongo/com.unity.Main", "Pokémon GO")
    private val minecraft = Scrape.androidGame("com.mojang.minecraftpe/.MainActivity", "Minecraft")

    @Test fun normalisedTitles() {
        assertEquals("pokemon go", normaliseTitle("Pokémon GO™"))
        assertEquals(normaliseTitle("Monument Valley: Ida's Dream"), normaliseTitle("Monument Valley - Ida's Dream"))
        assertEquals(normaliseTitle("Cut The Rope - Experiments"), normaliseTitle("Cut the Rope : Experiments"))
        assertEquals("temple run 2", normaliseTitle("  Temple \t Run   2 "))
        assertEquals("angry birds", normaliseTitle("Angry Birds® ©"))
        assertEquals("spider-man", normaliseTitle("Spider-Man"))   // a hyphen inside a word stays
        assertNotEquals(normaliseTitle("Minecraft"), normaliseTitle("Minecraft Pocket Edition"))
        assertEquals("ida's dream", normaliseTitle("Ida\u2019s Dream"))   // a curly apostrophe is a straight one
        assertEquals(normaliseTitle("Assassin's Creed"), normaliseTitle("Assassin\u2019s Creed"))
    }

    @Test fun androidGamesUseTheAndroidSystemOnly() {
        assertEquals(ScraperSystems.ANDROID, pgo.systemId())
        assertEquals(63, ScraperSystems.ANDROID)
        assertNull(Scrape.homeGame("Thing.apk", tag = "ANDROID").systemId())   // a ROM folder tagged ANDROID is not this console
        assertNull(ScraperSystems.baseIdFor("ANDROID"))
    }

    @Test fun nameLookupHitIsUsedAndOnlyItsScreenshotStored() = runTest {
        val rig = Rig(this)
        rig.http.on("jeuInfos", found("wheel" to "https://m/pgo-wheel.png", "ss" to "https://m/pgo-ss.png", "fanart" to "https://m/pgo-fan.png"))
        rig.http.on("https://m/", OK_PNG)
        val end = rig.run(listOf(pgo))
        assertEquals(ItemStatus.DONE, end.items.single().status)
        val call = rig.api.single()
        assertTrue(call.contains("/jeuInfos.php?"), call)
        assertTrue(call.contains("romnom=Pok%C3%A9mon+GO.apk&systemeid=63&"), call)
        assertEquals(listOf("https://m/pgo-ss.png"), rig.http.calls.filter { it.startsWith("https://m/") })
        assertContentEquals(OK_PNG.body, rig.files[pgo.fileName])
        assertTrue(rig.hasher.asked.isEmpty())                                // never hashed
        assertTrue(rig.arts.isEmpty() && rig.written.isEmpty())               // not the index or the home tree
        assertEquals(1, rig.refreshed)
        assertEquals(listOf(ArtCaps.of(ArtSlot.SCREENSHOT)), rig.fitter.caps)   // fitted to the screenshot cap
    }

    @Test fun notFoundByNameThenAnExactSearchHitOnAndroid() = runTest {
        val rig = Rig(this)
        rig.http.on(
            "recherche=Minecraft&systemeid=63",
            search(listOf("Minecraft Pocket Edition") to listOf("ss" to "https://m/mcpe-ss.png"), listOf("MINECRAFT") to listOf("ss" to "https://m/mc-ss.png")),
        )
        rig.http.on("https://m/", OK_PNG)
        assertEquals(ItemStatus.DONE, rig.run(listOf(minecraft)).items.single().status)
        assertEquals(2, rig.api.size)
        assertTrue(rig.api[0].contains("/jeuInfos.php?") && rig.api[0].contains("romnom=Minecraft.apk"))
        assertTrue(rig.api[1].contains("/jeuRecherche.php?") && rig.api[1].contains("&systemeid=63"))
        assertEquals(listOf("https://m/mc-ss.png"), rig.http.calls.filter { it.startsWith("https://m/") })   // the exact one, not the first
        assertTrue(minecraft.fileName in rig.files)
    }

    @Test fun aNonExactAndroidHitIsSkippedForAnExactOneOnAnySystem() = runTest {
        val rig = Rig(this)
        rig.http.on("recherche=Minecraft&systemeid=63", search(listOf("Minecraft Pocket Edition") to listOf("ss" to "https://m/mcpe-ss.png")))
        rig.http.on("recherche=Minecraft&output", search(listOf("Minecraft: Story Mode") to listOf("ss" to "https://m/sm.png"), listOf("Minecraft") to listOf("ss" to "https://m/x360-ss.png")))
        rig.http.on("https://m/", OK_PNG)
        assertEquals(ItemStatus.DONE, rig.run(listOf(minecraft)).items.single().status)
        assertEquals(3, rig.api.size)
        assertTrue(!rig.api[2].contains("systemeid"), rig.api[2])
        assertEquals(listOf("https://m/x360-ss.png"), rig.http.calls.filter { it.startsWith("https://m/") })
    }

    @Test fun anyNameOfAnEntryMatches() = runTest {
        val rig = Rig(this)
        val rope = Scrape.androidGame("com.zeptolab.ctrexperiments/.Main", "Cut the Rope: Experiments")
        rig.http.on("systemeid=63", search(listOf("Cut The Rope - Experiments", "Cut the Rope : Experiments") to listOf("ss" to "https://m/ctr.png")))
        rig.http.on("https://m/", OK_PNG)
        assertEquals(ItemStatus.DONE, rig.run(listOf(rope)).items.single().status)
    }

    @Test fun noExactMatchAnywhereIsNoArt() = runTest {
        val rig = Rig(this)
        rig.http.on("jeuRecherche", search(listOf("Minecraft Pocket Edition") to listOf("ss" to "https://m/mcpe-ss.png")))
        val end = rig.run(listOf(minecraft))
        assertEquals(ItemStatus.NOT_FOUND, end.items.single().status)
        assertEquals(3, rig.api.size)
        assertTrue(rig.http.calls.none { it.startsWith("https://m/") })
        assertTrue(rig.files.isEmpty())
        assertEquals(0, rig.refreshed)
    }

    @Test fun aMatchWithoutAScreenshotFallsThroughToTheNextStep() = runTest {
        val rig = Rig(this)
        val mv = Scrape.androidGame("com.ustwo.monumentvalley/.Main", "Monument Valley")
        rig.http.on("jeuInfos", found("wheel" to "https://m/mv-wheel.png", "fanart" to "https://m/mv-fan.png"))
        rig.http.on("systemeid=63", search(listOf("Monument Valley") to listOf("wheel" to "https://m/mv-wheel.png")))
        rig.http.on("recherche=Monument+Valley&output", search(listOf("Monument Valley") to listOf("ss" to "https://m/switch-ss.png")))
        rig.http.on("https://m/", OK_PNG)
        assertEquals(ItemStatus.DONE, rig.run(listOf(mv)).items.single().status)
        assertEquals(listOf("https://m/switch-ss.png"), rig.http.calls.filter { it.startsWith("https://m/") })
    }

    @Test fun onlyScreenshotsCountNotTitleScreens() = runTest {
        val rig = Rig(this)
        rig.http.on("jeuInfos", found("sstitle" to "https://m/title.png", "wheel" to "https://m/w.png"))
        val end = rig.run(listOf(pgo))
        assertEquals(ItemStatus.NOT_FOUND, end.items.single().status)
        assertEquals("No artwork", end.items.single().reason)   // a match, but no screenshot
        assertTrue(rig.http.calls.none { it.startsWith("https://m/") })
    }

    @Test fun lookupsArePaced() = runTest {
        val rig = Rig(this)
        rig.http.on("jeuRecherche", search())
        rig.run(listOf(minecraft, pgo))
        val apiTimes = rig.http.calls.indices.filter { rig.http.calls[it].startsWith(ScraperRequest.API) }.map { rig.http.times[it] }
        assertEquals(6, apiTimes.size)
        apiTimes.zipWithNext().forEach { (a, b) -> assertTrue(b >= a + ScreenScraperSource.RATE_MS, "$apiTimes") }
    }

    @Test fun storedArtIsSkippedUnlessReplace() = runTest {
        val rig = Rig(this)
        rig.files[pgo.fileName] = byteArrayOf(7)
        rig.http.on("jeuInfos", found("ss" to "https://m/pgo-ss.png"))
        rig.http.on("https://m/", OK_PNG)
        val skipped = rig.run(listOf(pgo)).items.single()
        assertEquals(ItemStatus.SKIPPED to "Has art", skipped.status to skipped.reason)
        assertTrue(rig.api.isEmpty())
        assertEquals(ItemStatus.DONE, rig.run(listOf(pgo), replace = true).items.single().status)
        assertContentEquals(OK_PNG.body, rig.files[pgo.fileName])
    }

    @Test fun aFailedStoreWriteIsAnErrorAndTheJobGoesOn() = runTest {
        val rig = Rig(this, FakeArtStore(fail = true))
        rig.http.on("jeuInfos", found("ss" to "https://m/pgo-ss.png"))
        rig.http.on("https://m/", OK_PNG)
        val end = rig.run(listOf(pgo, minecraft))
        assertEquals(JobPhase.DONE, end.phase)
        assertEquals(listOf(ItemStatus.ERROR, ItemStatus.ERROR), end.items.map { it.status })
        assertEquals(0, rig.refreshed)
    }

    @Test fun withoutAStoreAndroidGamesAreSkipped() = runTest {
        val rig = Rig(this, store = null)
        val item = rig.run(listOf(pgo)).items.single()
        assertEquals(ItemStatus.SKIPPED, item.status)
        assertTrue(rig.api.isEmpty())
    }

    @Test fun searchResponsesParse() {
        val games = ScraperResponse.parseSearch(200, """{"response":{"jeux":[{},{"noms":[{"region":"ss","text":"A"},{"region":"us","text":"B"}],"medias":[{"type":"ss","region":"wor","url":"u"}]}]}}""")
        assertEquals(listOf(listOf("A", "B")), games.map { it.names })
        assertEquals(listOf(SsMedia("ss", "wor", "u")), games.single().medias)
        assertTrue(ScraperResponse.parseSearch(404, "Erreur").isEmpty())
        assertTrue(ScraperResponse.parseSearch(200, ScraperFixtures.TRUNCATED).isEmpty())
    }
}
