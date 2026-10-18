package com.nxredux.mobile.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class RaProgressTest {
    private val h1 = "a".repeat(32); private val h2 = "b".repeat(32)
    private val store = FakeRaStore()
    private val journal = RaJournal(store, RaPaths.JOURNAL)
    private val confirmed = RaJournal(store, RaPaths.CONFIRMED)

    private suspend fun seed(unlocks: String) {
        store.put(RaPaths.sets(h1), setsJson(42, title = "A", achievements = """{"ID":1,"Title":"First"},{"ID":2,"Title":"Second"},{"ID":3,"Title":"Third"}"""))
        store.put(RaPaths.game(h1), RaJson.json.encodeToString(RaGamePointer.serializer(), RaGamePointer("GBA", "home", "Roms/Game Boy Advance (GBA)", "A.gba", "A")))
        store.put(RaPaths.session(42), """{"Success":true,"Unlocks":[$unlocks]}""")
        store.put(RaPaths.sets(h2), setsJson(7, title = "B", achievements = """{"ID":9,"Title":"Nine"}"""))
        store.put(RaPaths.session(7), """{"Success":true,"Unlocks":[]}""")
    }

    @Test fun progressIsKeyedByTheLibraryPathOfEachPointedGame() = runTest {
        seed("""{"ID":1,"When":100}""")
        val map = RaLibraryView.progressByPath(store, journal, confirmed, "user")
        assertEquals(mapOf("/Roms/Game Boy Advance (GBA)/A.gba" to RaProgress(1, 3, "Second")), map)
    }

    @Test fun aPendingUnlockCountsAndIsNotNext() = runTest {
        seed("""{"ID":1,"When":100}""")
        journal.append(RaUnlock("user", 2, h1, 200L))
        assertEquals(RaProgress(2, 3, "Third"), RaLibraryView.progressByPath(store, journal, confirmed, "user").values.single())
    }

    @Test fun aMasteredGameHasNoNext() = runTest {
        seed("""{"ID":1,"When":100},{"ID":2,"When":101},{"ID":3,"When":102}""")
        assertEquals(RaProgress(3, 3, null), RaLibraryView.progressByPath(store, journal, confirmed, "user").values.single())
    }

    @Test fun pendingAndConfirmedEntriesAreAttributedToTheirOwnHash() = runTest {
        seed("")
        store.put(RaPaths.game(h2), RaJson.json.encodeToString(RaGamePointer.serializer(), RaGamePointer("GB", "home", "Roms/Game Boy (GB)", "B.gb", "B")))
        journal.append(RaUnlock("user", 2, h1, 200L))     // pending on A
        confirmed.append(RaUnlock("user", 9, h2, 300L))   // confirmed on B
        confirmed.append(RaUnlock("user", 1, h2, 301L))   // an id B's set does not have: not counted for A
        store.reads.clear()
        val map = RaLibraryView.progressByPath(store, journal, confirmed, "user")
        assertEquals(
            mapOf("/Roms/Game Boy Advance (GBA)/A.gba" to RaProgress(1, 3, "First"), "/Roms/Game Boy (GB)/B.gb" to RaProgress(1, 1, null)),
            map,
        )
        assertEquals(1, store.reads.count { it == RaPaths.JOURNAL })     // each journal read once, not once per game
        assertEquals(1, store.reads.count { it == RaPaths.CONFIRMED })
    }
}
