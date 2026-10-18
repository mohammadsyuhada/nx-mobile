package com.nxredux.mobile.cheats.db

import kotlinx.coroutines.CompletableDeferred

/** Scriptable platform for controller and AppState tests: set the fields, then read the recorded calls. */
class FakeCheatDbPlatform : CheatDbPlatform {
    var free: Long = 1L shl 30
    var remote: String? = "stamp-2"
    var downloadStamp: String? = "stamp-2"
    var downloadError: CheatDbException? = null
    var installError: CheatDbException? = null
    var installedFiles: (Map<String, List<String>>) -> List<String> = { folders -> folders.keys.map { "Cheats/$it/a.cht" } }
    var manifestText: String? = null
    /** When set, `download` / `install` suspend on it until the test completes or cancels them. */
    var downloadGate: CompletableDeferred<Unit>? = null
    var installGate: CompletableDeferred<Unit>? = null
    /** When set, `readManifest` suspends on it, so a test can start a job while a refresh is in flight. */
    var readGate: CompletableDeferred<Unit>? = null
    /** When set, `writeManifest` suspends on it before storing the text. */
    var manifestGate: CompletableDeferred<Unit>? = null
    var reportedTotal: Long = 1000L

    val discarded = ArrayList<String>()
    val deleted = ArrayList<List<String>>()
    val pruned = ArrayList<List<String>>()
    var remoteCalls = 0

    override fun freeBytes(): Long = free
    override suspend fun remoteStamp(): String? { remoteCalls++; return remote }
    override suspend fun download(onProgress: (Long, Long) -> Unit): CheatDbArchive {
        downloadError?.let { throw it }
        onProgress(100L, reportedTotal)
        downloadGate?.await()
        onProgress(reportedTotal.coerceAtLeast(200L), reportedTotal)
        return CheatDbArchive("/cache/cheats.zip", downloadStamp)
    }
    override suspend fun install(archive: CheatDbArchive, folders: Map<String, List<String>>, onProgress: (String, Int, Int) -> Unit): List<String> {
        installError?.let { throw it }
        val tags = folders.keys.toList()
        tags.forEachIndexed { i, tag -> onProgress(tag, i + 1, tags.size); if (i == 0) installGate?.await() }
        return installedFiles(folders)
    }
    override suspend fun discard(archive: CheatDbArchive) { discarded += archive.path }
    override suspend fun readManifest(): String? { readGate?.await(); return manifestText }
    override suspend fun writeManifest(text: String) { manifestGate?.await(); manifestText = text }
    override suspend fun deleteInstalled(files: List<String>) { deleted += files; manifestText = null }
    override suspend fun deleteFiles(files: List<String>) { pruned += files }
}
