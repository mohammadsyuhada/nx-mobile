package com.nxredux.mobile.gametime

import com.nxredux.mobile.achievements.FakeRaStore
import com.nxredux.mobile.achievements.RaGamePointer
import com.nxredux.mobile.achievements.RaJournal
import com.nxredux.mobile.achievements.RaJson
import com.nxredux.mobile.achievements.RaLibraryView
import com.nxredux.mobile.achievements.RaPaths
import com.nxredux.mobile.achievements.setsJson
import com.nxredux.mobile.storage.GamePath
import com.nxredux.mobile.storage.GameRef
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Home's achievements card end to end below the UI: a game-time session's rom path, as GameActivity records it (the
 * session's library path), finds the RA pointer GameActivity writes for the same game, for a home and an extra game.
 */
class PlayedUnlocksTest {
    private val day = 24 * 3600_000L
    private val now = 30 * day
    private var clock = 0L
    private val store = testStore({ clock }).first
    private val ra = FakeRaStore()

    /** A game played one closed hour at [start], and its RA set with achievement [id] unlocked at [unlockedMs]. */
    private fun played(ref: GameRef, hash: String, gameId: Int, id: Int, start: Long, unlockedMs: Long) {
        val path = GamePath.of(ref)   // GameSession.gamePath: the path game time records
        clock = start
        val session = store.start(path, "GB", ref.fileName)
        clock = start + 3600_000L
        store.end(session)
        // GameActivity's pointer: the parts of GamePath.split(EXTRA_GAME_PATH).
        val parts = GamePath.split(path)!!
        ra.put(RaPaths.game(hash), RaJson.json.encodeToString(RaGamePointer.serializer(), RaGamePointer("GB", parts.sourceId, parts.relativeDir, parts.fileName, ref.fileName)))
        ra.put(RaPaths.sets(hash), setsJson(gameId, achievements = """{"ID":$id,"Title":"A"},{"ID":${id + 1},"Title":"B"}"""))
        ra.put(RaPaths.session(gameId), """{"Success":true,"Unlocks":[{"ID":$id,"When":${unlockedMs / 1000}}]}""")
    }

    private suspend fun unlocksInWindow(): Int {
        val since = now - 7 * day
        return RaLibraryView.unlocksSince(ra, RaJournal(ra, RaPaths.JOURNAL), RaJournal(ra, RaPaths.CONFIRMED), "user", since / 1000, store.playedPaths(since, now))
    }

    @Test fun aHomeGamePlayedInTheWindowCountsItsUnlock() = runTest {
        played(GameRef(GamePath.HOME, "Roms/Game Boy (GB)", "Tetris.gb"), "a".repeat(32), 42, 1, start = now - day, unlockedMs = now - day + 60_000)
        assertEquals(1, unlocksInWindow())
    }

    @Test fun anExtraGamePlayedInTheWindowCountsItsUnlock() = runTest {
        played(GameRef("Loose-d116b9e3", "", "Kirby.gba"), "b".repeat(32), 7, 9, start = now - 2 * day, unlockedMs = now - 2 * day + 60_000)
        assertEquals(1, unlocksInWindow())
    }

    @Test fun aGameLastPlayedBeforeTheWindowIsNotScanned() = runTest {
        // Its unlock falls inside the window (another device's sync, say), but it was not played here in the window.
        played(GameRef(GamePath.HOME, "Roms/Game Boy (GB)", "Tetris.gb"), "a".repeat(32), 42, 1, start = now - 10 * day, unlockedMs = now - day)
        assertEquals(0, unlocksInWindow())
    }
}
