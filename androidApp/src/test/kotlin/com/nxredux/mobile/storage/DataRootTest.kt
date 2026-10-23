package com.nxredux.mobile.storage

import com.nxredux.mobile.cheats.db.CheatDbManifest
import com.nxredux.mobile.runner.ContentRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.ZoneOffset

class DataRootTest {
    private val base = Files.createTempDirectory("dataroot").toFile()
    private val private = File(base, "sd").apply { mkdirs() }
    private val home = File(base, "home").apply { mkdirs() }
    private val media = File(base, "media").apply { mkdirs() }

    private fun write(root: File, rel: String, text: String, mtime: Long? = null) =
        File(root, rel).apply { parentFile!!.mkdirs(); writeText(text); mtime?.let { setLastModified(it) } }

    private fun files(root: File) = root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()

    // The move

    @Test fun movesSavesStatesAndConfigs() {
        write(private, "Saves/GBA/a.srm", "save")
        write(private, ".userdata/shared/MGBA-mgba/a.st0", "state")
        write(private, ".userdata/android/GBA-mgba/minarch.cfg", "cfg")

        assertEquals(3, DataRoot.moveAll(private, home))

        assertEquals("save", File(home, "Saves/GBA/a.srm").readText())
        assertEquals("state", File(home, ".userdata/shared/MGBA-mgba/a.st0").readText())
        assertEquals("cfg", File(home, ".userdata/android/GBA-mgba/minarch.cfg").readText())
        assertEquals(emptySet<String>(), files(private))
        assertFalse(File(private, "Saves").exists())   // emptied folders go too
        assertTrue(private.isDirectory)                 // never the root itself
    }

    @Test fun keepsTheModifiedTime() {
        write(private, "Saves/GBA/a.srm", "save", mtime = 1_000_000L)
        DataRoot.moveAll(private, home)
        assertEquals(1_000_000L, File(home, "Saves/GBA/a.srm").lastModified())
    }

    @Test fun aFileWrittenDuringTheMoveStaysPrivate() {
        write(private, "Saves/GBA/a.srm", "save", mtime = 1000L)
        write(private, ".userdata/android/hidden.txt", "a", mtime = 1000L)
        write(private, NxPaths.GAME_TIME_DB, "db", mtime = 1000L)
        write(private, NxPaths.GAME_TIME_DB + "-wal", "wal", mtime = 1000L)

        try {
            DataRoot.moveAll(private, home) {
                write(private, ".userdata/android/hidden.txt", "a\nb", mtime = 3000L)   // a store still on the private root
                write(private, NxPaths.GAME_TIME_DB + "-wal", "wal2", mtime = 3000L)
            }
            fail("an incomplete move must be reported, so the marker stays unset")
        } catch (e: DataRoot.IncompleteMove) {
            assertEquals(2, e.kept.size)
        }

        assertEquals("a\nb", File(private, ".userdata/android/hidden.txt").readText())
        assertEquals("db", File(private, NxPaths.GAME_TIME_DB).readText())   // its set stays whole
        assertEquals("wal2", File(private, NxPaths.GAME_TIME_DB + "-wal").readText())
        assertFalse(File(private, "Saves/GBA/a.srm").exists())                // the rest moved
        assertEquals("a", File(home, ".userdata/android/hidden.txt").readText())

        // The next move settles them by the clash rule: the newer private files win, the first copies become backups.
        DataRoot.moveAll(private, home)

        assertEquals(emptySet<String>(), files(private))
        assertEquals("a\nb", File(home, ".userdata/android/hidden.txt").readText())
        assertEquals("a", File(home, ".userdata/android/hidden.txt.bak-${DataRoot.backupStamp(1000L)}").readText())
        assertEquals("db", File(home, NxPaths.GAME_TIME_DB).readText())
        assertEquals("wal2", File(home, NxPaths.GAME_TIME_DB + "-wal").readText())
    }

    @Test fun leavesCachesAndCredentials() {
        write(private, ".userdata/android/ra/cache/login.json", "login")
        write(private, ".userdata/android/library.json", "lib")
        write(private, "Shaders/x", "shader")

        assertEquals(0, DataRoot.moveAll(private, home))

        assertEquals(setOf(".userdata/android/ra/cache/login.json", ".userdata/android/library.json", "Shaders/x"), files(private))
        assertEquals(emptySet<String>(), files(home))
    }

    @Test fun sameContentDropsPrivate() {
        write(home, "Saves/GB/t.sav", "same", mtime = 1_000_000L)
        write(private, "Saves/GB/t.sav", "same", mtime = 2_000_000L)

        assertEquals(0, DataRoot.moveAll(private, home))

        assertEquals(setOf("Saves/GB/t.sav"), files(home))   // no backup
        assertEquals(1_000_000L, File(home, "Saves/GB/t.sav").lastModified())
        assertFalse(File(private, "Saves/GB/t.sav").exists())
    }

    @Test fun newerWinsOlderBecomesBackup() {
        write(home, "Saves/GB/t.sav", "old", mtime = 1000L)
        write(private, "Saves/GB/t.sav", "new", mtime = 2000L)

        assertEquals(1, DataRoot.moveAll(private, home))

        assertEquals("new", File(home, "Saves/GB/t.sav").readText())
        assertEquals("old", File(home, "Saves/GB/t.sav.bak-${DataRoot.backupStamp(1000L)}").readText())
        assertEquals(2, files(home).size)
        assertFalse(File(private, "Saves/GB/t.sav").exists())
    }

    @Test fun olderPrivateBecomesTheBackup() {
        write(home, "Saves/GB/t.sav", "home", mtime = 2000L)
        write(private, "Saves/GB/t.sav", "private", mtime = 1000L)

        assertEquals(1, DataRoot.moveAll(private, home))

        assertEquals("home", File(home, "Saves/GB/t.sav").readText())
        assertEquals("private", File(home, "Saves/GB/t.sav.bak-${DataRoot.backupStamp(1000L)}").readText())
        assertFalse(File(private, "Saves/GB/t.sav").exists())
    }

    @Test fun backupStampIsDateAndTime() {
        assertEquals("19700101-000001", DataRoot.backupStamp(1000L, ZoneOffset.UTC))
        assertEquals("20261023-143005", DataRoot.backupStamp(1_792_765_805_000L, ZoneOffset.UTC))
    }

    @Test fun backupNameAvoidsCollisions() {
        val stamp = DataRoot.backupStamp(1000L)
        write(home, "Saves/GB/t.sav", "home", mtime = 2000L)
        write(home, "Saves/GB/t.sav.bak-$stamp", "earlier backup")
        write(private, "Saves/GB/t.sav", "private", mtime = 1000L)

        DataRoot.moveAll(private, home)

        assertEquals("earlier backup", File(home, "Saves/GB/t.sav.bak-$stamp").readText())
        assertEquals("private", File(home, "Saves/GB/t.sav.bak-$stamp-2").readText())
    }

    @Test fun failedCopyLeavesPrivateIntact() {
        write(private, ".userdata/shared/MGBA-mgba/a.st0", "state")
        write(private, "Saves/GBA/a.srm", "save")
        write(private, "Cheats/GBA/a.cht", "cheat")
        write(home, "Saves", "a file where the folder should be")
        val before = files(private)

        try {
            DataRoot.moveAll(private, home)
            fail("the move should report the failure")
        } catch (_: IOException) {
        }

        assertEquals(before, files(private))
        assertEquals("save", File(private, "Saves/GBA/a.srm").readText())
        assertFalse(File(home, "Cheats").exists())   // cheats wait for the rest
    }

    @Test fun movesTheGameTimeDbWithItsWal() {
        write(private, NxPaths.GAME_TIME_DB, "db")
        write(private, NxPaths.GAME_TIME_DB + "-wal", "wal")
        write(private, NxPaths.GAME_TIME_DB + "-shm", "shm")

        assertEquals(3, DataRoot.moveAll(private, home))

        assertEquals("db", File(home, NxPaths.GAME_TIME_DB).readText())
        assertEquals("wal", File(home, NxPaths.GAME_TIME_DB + "-wal").readText())
        assertEquals("shm", File(home, NxPaths.GAME_TIME_DB + "-shm").readText())
        assertEquals(emptySet<String>(), files(private))
    }

    @Test fun anOlderDbNeverMeetsTheNewerOnesWal() {
        // The home's database is newer through its WAL: the private set becomes backups, the home's set stays whole.
        write(home, NxPaths.GAME_TIME_DB, "home db", mtime = 1000L)
        write(home, NxPaths.GAME_TIME_DB + "-wal", "home wal", mtime = 5000L)
        write(private, NxPaths.GAME_TIME_DB, "private db", mtime = 2000L)
        write(private, NxPaths.GAME_TIME_DB + "-wal", "private wal", mtime = 2000L)

        DataRoot.moveAll(private, home)

        assertEquals("home db", File(home, NxPaths.GAME_TIME_DB).readText())
        assertEquals("home wal", File(home, NxPaths.GAME_TIME_DB + "-wal").readText())
        val stamp = DataRoot.backupStamp(2000L)
        assertEquals("private db", File(home, NxPaths.GAME_TIME_DB + ".bak-$stamp").readText())
        assertEquals("private wal", File(home, NxPaths.GAME_TIME_DB + "-wal.bak-$stamp").readText())
    }

    @Test fun aNewerDbTakesItsWalAndBacksUpTheHomeSet() {
        write(home, NxPaths.GAME_TIME_DB, "home db", mtime = 1000L)
        write(home, NxPaths.GAME_TIME_DB + "-wal", "home wal", mtime = 1000L)
        write(private, NxPaths.GAME_TIME_DB, "private db", mtime = 2000L)   // closed cleanly: no WAL

        DataRoot.moveAll(private, home)

        assertEquals("private db", File(home, NxPaths.GAME_TIME_DB).readText())
        assertFalse(File(home, NxPaths.GAME_TIME_DB + "-wal").exists())   // the home's WAL never meets the private DB
        val stamp = DataRoot.backupStamp(1000L)
        assertEquals("home db", File(home, NxPaths.GAME_TIME_DB + ".bak-$stamp").readText())
        assertEquals("home wal", File(home, NxPaths.GAME_TIME_DB + "-wal.bak-$stamp").readText())
    }

    @Test fun backupNameIsNoSaveCandidate() {
        val backup = Regex("""\.bak-\d{8}-\d{6}(-\d+)?$""")
        val samples = listOf(NxPaths("GBA", "mgba", "Pokemon (USA).gba"), NxPaths("PS", "swanstation", "FF7.chd", innerFileName = "FF7.bin"))
        for (paths in samples) {
            val names = paths.saveRamCandidates() + ((0 until NxPaths.SLOT_COUNT) + NxPaths.AUTO_SLOT).flatMap { paths.stateCandidates(it) }
            assertTrue(names.isNotEmpty())
            names.forEach { assertFalse(it, backup.containsMatchIn(it)) }
        }
        assertTrue(backup.containsMatchIn("Saves/GBA/x.srm.bak-${DataRoot.backupStamp(1000L)}-2"))   // the pattern is the real one
    }

    @Test fun cheatsKeepTheirRule() {
        val db = CheatDbManifest("stamp", 1L, listOf("Cheats/GB/Tetris (World).cht"), listOf("GB"))
        write(private, CheatDbManifest.PATH, db.render())
        write(private, "Cheats/GB/Tetris (World).cht", "old download")
        write(private, "Cheats/GB/Mine.cht", "mine")
        write(home, CheatDbManifest.PATH, "home manifest")
        write(home, "Cheats/GB/Tetris (World).cht", "new download")

        assertEquals(1, DataRoot.moveAll(private, home))

        assertEquals("mine", File(home, "Cheats/GB/Mine.cht").readText())
        assertEquals("new download", File(home, "Cheats/GB/Tetris (World).cht").readText())
        assertEquals("home manifest", File(home, CheatDbManifest.PATH).readText())
        assertFalse(File(private, "Cheats").exists())
        assertFalse(File(private, CheatDbManifest.PATH).exists())
        assertFalse(files(home).any { ".bak-" in it })   // no backups for cheats
    }

    @Test fun theHomeItselfIsNeverMovedIntoItself() {
        write(private, "Saves/GB/a.sav", "a")
        assertEquals(0, DataRoot.moveAll(private, private))
        assertTrue(File(private, "Saves/GB/a.sav").exists())
    }

    // The root choice

    private val fileHome = FileRefs.of(home.path)
    private val mediaRef = FileRefs.of(media.path)

    private fun resolve(marker: Boolean, hasData: Boolean, saved: ContentRef?, effective: ContentRef?) =
        DataRoot.resolve(marker, { hasData }, saved, effective, media, private)

    @Test fun withTheMarkerTheEffectiveHome() {
        assertEquals(DataRoot.Decision(home), resolve(marker = true, hasData = true, fileHome, fileHome))
        assertEquals(DataRoot.Decision(media), resolve(marker = true, hasData = true, fileHome, mediaRef))   // access off
        assertEquals(DataRoot.Decision(media), resolve(marker = true, hasData = false, null, mediaRef))
    }

    @Test fun theMarkerSkipsTheWalk() {
        DataRoot.resolve(true, { fail("walked the private root"); true }, fileHome, fileHome, media, private)
    }

    @Test fun noPrivateDataMarksAndUsesTheHome() {
        assertEquals(DataRoot.Decision(home, mark = true), resolve(marker = false, hasData = false, fileHome, fileHome))
        assertEquals(DataRoot.Decision(media, mark = true), resolve(marker = false, hasData = false, null, mediaRef))
    }

    @Test fun aSavedFullAccessHomeMovesInTheBackground() {
        assertEquals(DataRoot.Decision(private, move = home), resolve(marker = false, hasData = true, fileHome, fileHome))
    }

    @Test fun otherwiseThePrivateRootUntilTheUpgrade() {
        assertEquals(DataRoot.Decision(private), resolve(marker = false, hasData = true, fileHome, mediaRef))   // card out or access off: waits
        assertEquals(DataRoot.Decision(private), resolve(marker = false, hasData = true, null, mediaRef))       // no home
        val picker = ContentRef("content://tree/primary%3AGames")
        assertEquals(DataRoot.Decision(private), resolve(marker = false, hasData = true, picker, picker))
        assertEquals(DataRoot.Decision(private), resolve(marker = false, hasData = false, picker, picker))      // the upgrade screen adopts it
    }

    @Test fun providerFollowsAccessChange() {
        // What DataRoot.of answers after the move as All files access comes and goes (AndroidHomeLocator's choice).
        fun root(granted: Boolean) =
            resolve(marker = true, hasData = true, fileHome, HomeChoice.effective(fileHome, granted, { File(FileRefs.path(it)).isDirectory }, mediaRef)).root
        assertEquals(home, root(granted = true))
        assertEquals(media, root(granted = false))
        assertEquals(home, root(granted = true))
    }

    @Test fun findsDataOnlyInWhatMoves() {
        write(private, ".userdata/android/ra/cache/login.json", "login")
        write(private, "Shaders/x", "shader")
        assertFalse(DataRoot.hasData(private))
        write(private, ".userdata/android/hidden.txt", "x")
        assertTrue(DataRoot.hasData(private))
        File(private, ".userdata/android/hidden.txt").delete()
        write(private, "Cheats/GB/a.cht", "x")
        assertTrue(DataRoot.hasData(private))
    }

    // The media home

    @Test fun mediaHomeHasItsSkeleton() {
        MediaHome.ensure(media)
        listOf("Roms", "Bios", "Collections", "Saves").forEach { assertTrue(it, File(media, it).isDirectory) }
        val nomedia = File(media, ".nomedia")
        assertTrue(nomedia.isFile)
        assertEquals(0L, nomedia.length())
        assertNull(File(media, "Saves").list()?.firstOrNull())
    }
}
