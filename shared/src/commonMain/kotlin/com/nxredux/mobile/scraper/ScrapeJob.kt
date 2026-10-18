package com.nxredux.mobile.scraper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ItemStatus { WAITING, SEARCHING, DOWNLOADING, DONE, PARTIAL, NOT_FOUND, ERROR, SKIPPED }

data class ScrapeItem(val gamePath: String, val tag: String, val name: String, val status: ItemStatus = ItemStatus.WAITING, val reason: String? = null)

enum class JobPhase { IDLE, RUNNING, PAUSED, DONE, CANCELLED }

data class ScrapeState(
    val phase: JobPhase = JobPhase.IDLE,
    val items: List<ScrapeItem> = emptyList(),
    val pauseReason: String? = null,
    val quota: SsQuota? = null,
    /** Source id -> why that source stopped for this job ([SourceStop.note]); persisted. */
    val notes: Map<String, String> = emptyMap(),
    /** TheGamesDB's requests left this month as last read (null = unknown or no key); not persisted. */
    val tgdbLeft: Int? = null,
) {
    /** Items with a final status (not waiting or in flight). */
    val done: Int get() = items.count { !it.status.inFlightOrWaiting }
    val total: Int get() = items.size
}

internal val ItemStatus.inFlightOrWaiting: Boolean
    get() = this == ItemStatus.WAITING || this == ItemStatus.SEARCHING || this == ItemStatus.DOWNLOADING

/** An in-flight item goes back to waiting (it is redone on resume). */
internal fun ScrapeState.inFlightToWaiting(): ScrapeState =
    copy(items = items.map { if (it.status.inFlightOrWaiting) it.copy(status = ItemStatus.WAITING, reason = null) else it })

/** The persisted job (`scrape_job.json`). A job saved while running comes back paused with in-flight items waiting. */
object ScrapeJobCodec {
    @Serializable private data class Item(val path: String, val tag: String, val name: String, val status: String, val reason: String? = null)
    @Serializable private data class Job(val version: Int = 1, val phase: String, val replace: Boolean, val pauseReason: String? = null, val items: List<Item>, val notes: Map<String, String> = emptyMap())

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(s: ScrapeState, replace: Boolean): String = json.encodeToString(
        Job.serializer(),
        Job(phase = s.phase.name, replace = replace, pauseReason = s.pauseReason, items = s.items.map { Item(it.gamePath, it.tag, it.name, it.status.name, it.reason) }, notes = s.notes),
    )

    fun decode(text: String): Pair<ScrapeState, Boolean>? = runCatching {
        val job = json.decodeFromString(Job.serializer(), text)
        val items = job.items.map { ScrapeItem(it.path, it.tag, it.name, ItemStatus.valueOf(it.status), it.reason) }
        var phase = JobPhase.valueOf(job.phase)
        if (phase == JobPhase.RUNNING) phase = JobPhase.PAUSED   // the process died mid-job
        ScrapeState(phase, items, job.pauseReason, notes = job.notes).inFlightToWaiting() to job.replace
    }.getOrNull()
}
