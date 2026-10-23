package com.nxredux.mobile.storage

import android.content.Context
import com.nxredux.mobile.gametime.GameTimeService
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.settings.AndroidAppSettings
import com.nxredux.mobile.tools.CheatDbService
import com.nxredux.mobile.tools.CheatsRoot
import com.swordfish.libretrodroid.NxLog
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch

/**
 * The root saves, states, configs, game time, recents and cheats are read and written under ([StorageLayout.Kind.MOVE]
 * and [StorageLayout.Kind.CHEATS] paths): the home folder ([AndroidHomeLocator]) once the data an earlier version kept
 * in app-private storage has moved there, the private root until then, so a session never sees a split set. Caches and
 * credentials stay under [private] for good.
 *
 * The move happens once. With All files access and a file-path home it starts in the background the first time it is
 * needed; otherwise the upgrade screen runs it ([moveNow], then [markDone]). The marker survives the move: it is in
 * no-backup storage, not in either root.
 */
object DataRoot {
    private const val TAG = "DataRoot"
    /** In no-backup storage: present once private data is in the home folder (or there was none). */
    private const val MARKER = "storage-moved"
    private const val BACKUP = ".bak-"
    /** SQLite's files beside a database: they move, clash and back up with it, never alone. */
    private val DB_SIDECARS = listOf("-wal", "-shm", "-journal")
    private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    private val lock = Any()
    /** Held while files move: one move at a time, [of] never waits on it. */
    private val moving = Any()
    @Volatile private var move: CountDownLatch? = null
    @Volatile private var moveFailed = false
    /** The marker seen (it is never removed while the process lives): [of] then checks nothing else. */
    @Volatile private var done = false
    /** Private data seen: it only goes with the move, which sets the marker, so the private root is not walked again. */
    @Volatile private var privateHasData = false

    /** What [of] does: use [root]; set the marker first when [mark]; start the background move into [move] when set. */
    internal data class Decision(val root: File, val mark: Boolean = false, val move: File? = null)

    /**
     * The data root now. Cheap once the move is done (a file check and the home choice); until then it may walk the
     * private root once, so call it off the main thread.
     */
    fun of(context: Context): File {
        val private = private(context)
        val effective = AndroidHomeLocator(context).current()
        val decision = resolve(
            marker = isDone(context),
            hasData = { privateHasData || hasData(private).also { privateHasData = it } },
            saved = AndroidAppSettings(context).treeRef,
            effective = effective,
            media = MediaHome.dir(context),
            private = private,
        )
        if (decision.mark) markDone(context)
        if (decision.move != null && move == null && !moveFailed) startMove(context.applicationContext)
        return decision.root
    }

    /** [of] once a move it needs has finished: one root for a whole session. Blocks, so never on the main thread. */
    fun settled(context: Context): File {
        of(context)
        move?.await()
        return of(context)
    }

    /** App-private storage: caches, credentials, art, and the data until it moves. */
    fun private(context: Context): File = NxStorage.root(context)

    fun isDone(context: Context): Boolean = done || File(context.noBackupFilesDir, MARKER).exists().also { if (it) done = true }

    /**
     * Sets the marker, so [of] answers the home from now on. The game-time database is closed again (a store opened
     * while the move ran points at the private file, now gone) and the Cheat Database rereads its manifest at the new root.
     */
    fun markDone(context: Context) {
        val marker = File(context.noBackupFilesDir, MARKER)
        runCatching { marker.parentFile?.mkdirs(); marker.createNewFile() }
            .onSuccess { done = true }
            .onFailure { NxLog.w(TAG, "cannot mark the move done", it) }
        GameTimeService.closeForMove()
        CheatDbService.refreshIfStarted()
    }

    /**
     * Moves the private data into [target] now (the upgrade screen, which then calls [markDone]), after a background move
     * still running. Blocks. Returns the files moved. Throws an [IOException] when the move is not complete: a copy
     * failed (the private data then left whole), or a file changed while it moved and stays private ([IncompleteMove]);
     * either way the caller must not call [markDone].
     */
    fun moveNow(context: Context, target: File): Int {
        move?.await()
        synchronized(moving) {
            GameTimeService.closeForMove()
            return moveAll(private(context), target).also { privateHasData = false }
        }
    }

    /**
     * [of]'s choice from its inputs: [marker] present, [hasData] (the private root holds files that move; only asked
     * when needed), the [saved] home setting, the [effective] home ([HomeChoice.effective]), the [media] home and the
     * [private] root.
     */
    internal fun resolve(marker: Boolean, hasData: () -> Boolean, saved: ContentRef?, effective: ContentRef?, media: File, private: File): Decision {
        val home = effective?.takeIf { FileRefs.isFile(it) }?.let { File(FileRefs.path(it)) }
        if (marker) return Decision(home ?: media)   // a picker home after the upgrade only by mistake: its data has no path
        // A picker home waits for the upgrade screen, which makes it an extra folder, even with nothing to move.
        if (home == null) return Decision(private)
        if (!hasData()) return Decision(home, mark = true)
        // Case A: the saved full-access home is reachable. Any other home (access off, card out) waits with the private root.
        if (effective == saved) return Decision(private, move = home)
        return Decision(private)
    }

    /** True when [private] holds a file that moves (first found ends the walk). */
    internal fun hasData(private: File): Boolean =
        private.walkTopDown().any { it.isFile && StorageLayout.kind(it.relativeTo(private).invariantSeparatorsPath) != StorageLayout.Kind.STAY }

    private fun startMove(app: Context) {
        val latch = synchronized(lock) {
            if (move != null || done) return
            CountDownLatch(1).also { move = it }
        }
        Thread({
            try {
                synchronized(moving) {
                    val private = private(app)
                    val effective = AndroidHomeLocator(app).current()
                    val again = resolve(isDone(app), { hasData(private) }, AndroidAppSettings(app).treeRef, effective, MediaHome.dir(app), private)
                    when {
                        again.move != null -> {
                            GameTimeService.closeForMove()
                            val moved = moveAll(private, again.move)
                            privateHasData = false
                            NxLog.i(TAG, "moved $moved files into ${again.move.path}")
                            markDone(app)
                        }
                        again.mark -> markDone(app)
                    }
                }
            } catch (e: Exception) {
                moveFailed = true   // not retried in this process: the data stays private until the next start
                NxLog.w(TAG, "moving the data into the home folder failed", e)
            } finally {
                synchronized(lock) { move = null }
                latch.countDown()
            }
        }, "data-move").start()
    }

    /**
     * Moves every [StorageLayout.Kind.MOVE] file of [private] into [home] at the same relative path, then the cheats
     * ([CheatsRoot.moveFromPrivate]'s rules). On a clash, the same content drops the private copy; different content
     * keeps the newer file (by modified time) under the name and the older one beside it as `<name>.bak-<yyyyMMdd-HHmmss>`
     * (its own time; `-2`, `-3`… when taken; [now] when it has none). A database moves with its `-wal`, `-shm` and
     * `-journal` files as one set. Everything is copied before anything private is deleted, and a failed copy throws
     * with nothing deleted. A private file that changed after its copy (a store still writing the private root) is kept,
     * with the rest of its set, and the move is incomplete: after deleting what did move it throws [IncompleteMove], so
     * no caller sets the marker and the next move settles that set by the clash rule. Emptied private folders are
     * removed. Returns the files copied into [home].
     * [beforeDelete] runs between the copies and the deletes (tests).
     */
    internal fun moveAll(private: File, home: File, now: Long = System.currentTimeMillis(), beforeDelete: () -> Unit = {}): Int {
        if (private.canonicalFile == home.canonicalFile) return 0
        val moving = private.walkTopDown().filter { it.isFile }.map { it.relativeTo(private).invariantSeparatorsPath }
            .filter { StorageLayout.kind(it) == StorageLayout.Kind.MOVE }.toSet()
        // A sidecar whose database moves too goes with it.
        val sets = moving.filterNot { rel -> DB_SIDECARS.any { rel.endsWith(it) && rel.removeSuffix(it) in moving } }
        var moved = 0
        val copied = ArrayList<Pair<List<File>, List<Snapshot>>>()
        for (rel in sets) {
            val names = listOf(rel) + DB_SIDECARS.map { rel + it }
            val from = names.map { File(private, it) }.filter { it.isFile }
            val to = names.map { File(home, it) }.filter { it.exists() }
            val before = from.map(::Snapshot)   // taken before the copy: a later write shows as a change
            moved += moveSet(from, to, private, home, now)
            copied += from to before
        }
        moved += CheatsRoot.moveFromPrivate(private, home)
        beforeDelete()
        val (gone, kept) = copied.partition { (from, before) -> from.map(::Snapshot) == before }
            .let { (same, changed) -> same.flatMap { it.first } to changed.map { it.first.first() } }
        gone.forEach { it.delete() }
        gone.mapNotNull { it.parentFile }.distinct().forEach { dir ->
            var d: File? = dir
            while (d != null && d != private && d.delete()) d = d.parentFile   // only empty ones go
        }
        if (kept.isNotEmpty()) throw IncompleteMove(kept)
        return moved
    }

    /** [moveAll] kept [kept] private (each changed while it moved): the move is not done, so the marker stays unset. */
    internal class IncompleteMove(val kept: List<File>) : IOException("changed while moving: ${kept.joinToString { it.path }}")

    /** What says a file was written since: its length and modified time. */
    private data class Snapshot(val length: Long, val modified: Long) {
        constructor(file: File) : this(file.length(), file.lastModified())
    }

    /** One file (or a database and its sidecars), [from] private into [home], over what [to] already holds there. */
    private fun moveSet(from: List<File>, to: List<File>, private: File, home: File, now: Long): Int {
        fun target(f: File) = File(home, f.relativeTo(private).path)
        if (to.isEmpty()) {
            from.forEach { copy(it, target(it)) }
            return from.size
        }
        val sameSet = to.map { it.name }.toSet() == from.map { it.name }.toSet() && from.all { same(it, target(it)) }
        if (sameSet) return 0
        val privateTime = from.maxOf { it.lastModified() }
        val homeTime = to.maxOf { it.lastModified() }
        if (privateTime > homeTime) {
            // Staged beside the targets first, so a failed copy leaves the home's set as it was.
            val staged = from.map { f -> File(target(f).parentFile, ".${f.name}.moving") }
            try {
                from.zip(staged).forEach { (f, s) -> copy(f, s) }
            } catch (e: IOException) {
                staged.forEach { it.delete() }
                throw e
            }
            val stamp = stamp(homeTime, now)
            to.forEach { t -> if (!t.renameTo(backupFor(t, stamp))) throw IOException("cannot back up ${t.path}") }
            staged.zip(from).forEach { (s, f) -> if (!s.renameTo(target(f))) throw IOException("cannot place ${target(f).path}") }
        } else {
            val stamp = stamp(privateTime, now)
            from.forEach { copy(it, backupFor(target(it), stamp)) }
        }
        return from.size
    }

    private fun same(a: File, b: File) =
        b.isFile && a.length() == b.length() && runCatching { a.readBytes().contentEquals(b.readBytes()) }.getOrDefault(false)

    /** [from] to [to] with its modified time; a part copy is removed, and the failure thrown. */
    private fun copy(from: File, to: File) {
        val dir = to.parentFile
        if (dir != null && !dir.isDirectory && !dir.mkdirs()) throw IOException("cannot make ${dir.path}")
        try {
            from.copyTo(to, overwrite = true)
            to.setLastModified(from.lastModified())
        } catch (e: IOException) {
            to.delete()
            throw e
        }
    }

    /** `<name>.bak-<stamp>` beside [file], or the first free `-2`, `-3`… after it. */
    private fun backupFor(file: File, stamp: String): File {
        val first = File(file.parentFile, file.name + BACKUP + stamp)
        if (!first.exists()) return first
        return generateSequence(2) { it + 1 }.map { File(file.parentFile, "${first.name}-$it") }.first { !it.exists() }
    }

    private fun stamp(mtime: Long, now: Long) = backupStamp(if (mtime > 0) mtime else now)

    /** A backup's time as `yyyyMMdd-HHmmss` in [zone]. */
    internal fun backupStamp(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String = STAMP.format(Instant.ofEpochMilli(millis).atZone(zone))
}
