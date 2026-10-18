package com.nxredux.mobile.scraper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The art sources' ids: the written-art log's source and the key of a job's notes. */
object SourceId {
    const val SS = "screenscraper"
    const val LIBRETRO = "libretro"
    const val TGDB = "thegamesdb"
    const val SGDB = "steamgriddb"
}

/**
 * One image a source offers for a slot, best first within the source. [bytes]: the image itself when the source already
 * downloaded it to find it (libretro-thumbnails probes by GET); the engine then does not fetch [url] again.
 */
class ArtHit(val slot: ArtSlot, val url: String, val origin: ArtOrigin, val bytes: ByteArray? = null)

/** This source is done for the rest of the job ([note] shown on the Artwork page); the job goes on with the others. */
class SourceStop(val note: String) : Exception(note)

/** The whole job pauses ("No connection", storage, ...). */
class JobPause(val reason: String) : Exception(reason)

/** Spaces calls at least [gapMs] apart, from the end of one to the start of the next (one call at a time). */
class Pacer(private val gapMs: Long, private val now: () -> Long, private val sleep: suspend (Long) -> Unit) {
    private val lock = Mutex()
    private var last: Long? = null

    suspend fun <T> paced(block: suspend () -> T): T = lock.withLock {
        last?.let { val wait = gapMs - (now() - it); if (wait > 0) sleep(wait) }
        try { block() } finally { last = now() }
    }
}

/**
 * One place art comes from. The engine asks the sources in order, each only for the slots a game still lacks, and only
 * when it is [enabled] and [knows] the game. A source-wide failure is a [SourceStop] (that source is left out for the
 * rest of the job); a [JobPause] pauses the whole job.
 */
interface ArtSource {
    val id: String
    /** "ScreenScraper", "libretro-thumbnails", "TheGamesDB", "SteamGridDB". */
    val label: String
    /** Its name on a Progress row ("Done · ScreenScraper, libretro"); [label] unless shorter is needed. */
    val shortLabel: String get() = label
    /** Its credentials or key are present. */
    val enabled: Boolean
    /** The slots it can offer. */
    val slots: Set<ArtSlot>
    /**
     * Its wheels are the last resort (TheGamesDB's clear logos): in turn it is asked for the wheel only beside other slots
     * (a free extra; it spends no call on it then), and those wheels are tried once every other source has been asked;
     * when still none is written, it is asked for the wheel alone ([find] with only [ArtSlot.WHEEL]).
     */
    val wheelLast: Boolean get() = false
    /** It has a system / platform for [ref] (pure). */
    fun knows(ref: LibraryGameRef): Boolean
    /** Once at the start of each job run, before any [find] (per-job state, an up-front quota read). Throws as [find]. */
    suspend fun startJob() {}
    /** Ranked hits for [wanted]; empty = not found. Throws [SourceStop] or [JobPause]. */
    suspend fun find(ref: LibraryGameRef, wanted: Set<ArtSlot>): List<ArtHit>
}
