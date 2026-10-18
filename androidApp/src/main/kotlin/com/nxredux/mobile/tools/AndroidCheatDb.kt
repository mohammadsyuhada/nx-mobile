package com.nxredux.mobile.tools

import android.content.Context
import android.os.StatFs
import com.nxredux.mobile.cheats.db.CheatDatabase
import com.nxredux.mobile.cheats.db.CheatDbArchive
import com.nxredux.mobile.cheats.db.CheatDbException
import com.nxredux.mobile.cheats.db.CheatDbManifest
import com.nxredux.mobile.cheats.db.CheatDbPlatform
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.runner.writeAtomically
import com.swordfish.libretrodroid.NxLog
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The Cheat Database tool on Android: HttpURLConnection into the cache, java.util.zip into `sd/Cheats/<TAG>/`. */
class AndroidCheatDb(context: Context) : CheatDbPlatform {
    private val app = context.applicationContext
    private val sdRoot: File get() = NxStorage.root(app)
    private val cacheDir: File get() = File(app.cacheDir, "cheatdb").apply { mkdirs() }

    override fun freeBytes(): Long = runCatching { StatFs(sdRoot.path).availableBytes }.getOrDefault(Long.MAX_VALUE)

    override suspend fun remoteStamp(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val c = open("HEAD")
            // A present-but-blank header is as good as none: the manifest cannot round-trip an empty stamp.
            try {
                if (c.responseCode in 200..299) c.getHeaderField("Last-Modified")?.takeIf { it.isNotBlank() } else null
            } finally { c.disconnect() }
        }.onFailure { if (it is CancellationException) throw it; NxLog.w(TAG, "update check failed", it) }.getOrNull()
    }

    override suspend fun download(onProgress: (Long, Long) -> Unit): CheatDbArchive = withContext(Dispatchers.IO) {
        // Each attempt owns its file names, so a cancelled attempt still unwinding can never delete a newer
        // attempt's files. Sweep what earlier attempts or a killed process left: unlinking a file an old
        // attempt still holds open is harmless, that attempt is cancelled and only ever touches its own names.
        cacheDir.listFiles()?.forEach { it.delete() }
        val id = Random.nextInt().toUInt().toString(16)
        val partFile = File(cacheDir, "cheats.$id.zip.part")
        val zipFile = File(cacheDir, "cheats.$id.zip")
        val c = try { open("GET") } catch (e: IOException) { throw CheatDbException(CheatDbException.Kind.NETWORK, e) }
        // A read blocked on a stalled socket never sees ensureActive(), so a cancel closes the socket from this
        // watcher and the read throws at once. (Job.invokeOnCompletion would only fire after the body returns.)
        // UNDISPATCHED: the body reaches awaitCancellation() before launch returns, so a cancel can never skip the finally.
        val watchdog = launch(start = CoroutineStart.UNDISPATCHED) { try { awaitCancellation() } finally { c.disconnect() } }
        try {
            if (c.responseCode !in 200..299) throw CheatDbException(CheatDbException.Kind.NETWORK, IOException("HTTP ${c.responseCode}"))
            val total = c.contentLengthLong   // -1 when the server sends no Content-Length
            val stamp = c.getHeaderField("Last-Modified")?.takeIf { it.isNotBlank() }
            var done = 0L
            var reported = 0L
            c.inputStream.use { input ->
                partFile.outputStream().use { out ->
                    val buffer = ByteArray(CHUNK)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n
                        // Every callback becomes a main-thread state update, so report per 256 KB, not per chunk.
                        if (done - reported >= PROGRESS_STEP) { reported = done; onProgress(done, total) }
                    }
                    out.fd.sync()
                }
            }
            onProgress(done, total)   // the final done == total always reaches the UI
            currentCoroutineContext().ensureActive()   // a cancelled attempt must not publish an archive
            if (!partFile.renameTo(zipFile)) throw IOException("rename failed")
            CheatDbArchive(zipFile.path, stamp)
        } catch (e: CheatDbException) {
            partFile.delete(); throw e
        } catch (e: IOException) {
            partFile.delete()
            currentCoroutineContext().ensureActive()   // the watchdog's disconnect surfaces as cancellation, not NETWORK
            throw CheatDbException(CheatDbException.Kind.NETWORK, e)
        } catch (e: Throwable) {
            partFile.delete(); throw e   // cancellation included: never leave a .part behind
        } finally {
            watchdog.cancel()
            c.disconnect()
        }
    }

    override suspend fun install(archive: CheatDbArchive, folders: Map<String, List<String>>, onProgress: (String, Int, Int) -> Unit): List<String> = withContext(Dispatchers.IO) {
        val zip = try { ZipFile(archive.path) } catch (e: ZipException) { throw CheatDbException(CheatDbException.Kind.CORRUPT, e) } catch (e: IOException) { throw CheatDbException(CheatDbException.Kind.CORRUPT, e) }
        val written = ArrayList<String>()
        // Targets that did not exist before this attempt wrote them: the only files a failure may take back.
        val created = ArrayList<File>()
        try {
            zip.use {
                val entries = zip.entries().toList().filter { !it.isDirectory }
                val tags = folders.entries.toList()
                tags.forEachIndexed { i, (tag, tagFolders) ->
                    onProgress(tag, i + 1, tags.size)
                    val taken = HashSet<String>()   // a name in two of the tag's folders: the first folder's file stays
                    for (folder in tagFolders) {
                        val prefix = "$folder/"
                        for (entry in entries) {
                            currentCoroutineContext().ensureActive()
                            val name = entry.name
                            if (!name.startsWith(prefix)) continue
                            val base = name.removePrefix(prefix)
                            if (base.contains('/') || !base.endsWith(".cht", ignoreCase = true)) continue   // flat files only
                            if (!taken.add(base)) continue
                            val relative = "Cheats/$tag/$base"
                            val target = File(sdRoot, relative)
                            val existed = target.exists()
                            val bytes = try {
                                zip.getInputStream(entry).use { it.readBytes() }
                            } catch (e: ZipException) {
                                throw CheatDbException(CheatDbException.Kind.CORRUPT, e)   // before IOException: a ZipException is one
                            } catch (e: IOException) {
                                throw CheatDbException(CheatDbException.Kind.STORAGE, e)
                            }
                            try {
                                target.writeAtomically(bytes)
                            } catch (e: IOException) {
                                throw CheatDbException(CheatDbException.Kind.STORAGE, e)
                            }
                            if (!existed) created += target
                            written += relative
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            // Cancellation included. Pre-existing files (a previous install, or same-named user files) stay.
            rollBack(created)
            throw e
        }
        written
    }

    /** Best-effort delete of [files], then the `Cheats/<TAG>` folders they emptied (delete only succeeds when empty). */
    private fun rollBack(files: List<File>) {
        val dirs = LinkedHashSet<File>()
        for (f in files) {
            if (f.exists() && !f.delete()) NxLog.w(TAG, "rollback could not delete ${f.path}")
            f.parentFile?.let(dirs::add)
        }
        dirs.forEach { it.delete() }
    }

    override suspend fun discard(archive: CheatDbArchive) = withContext(Dispatchers.IO) { File(archive.path).delete(); Unit }

    override suspend fun readManifest(): String? = withContext(Dispatchers.IO) {
        File(sdRoot, CheatDbManifest.PATH).takeIf { it.exists() }?.let { f -> runCatching { f.readText() }.getOrNull() }
    }

    override suspend fun writeManifest(text: String) = withContext(Dispatchers.IO) {
        try { File(sdRoot, CheatDbManifest.PATH).writeAtomically(text.encodeToByteArray()) } catch (e: IOException) { throw CheatDbException(CheatDbException.Kind.STORAGE, e) }
    }

    override suspend fun deleteInstalled(files: List<String>) = withContext(Dispatchers.IO) {
        deleteFiles(files)
        File(sdRoot, CheatDbManifest.PATH).delete()
        Unit
    }

    override suspend fun deleteFiles(files: List<String>) = withContext(Dispatchers.IO) {
        val dirs = LinkedHashSet<File>()
        for (rel in files) {
            val f = File(sdRoot, rel)
            if (f.exists() && !f.delete()) NxLog.w(TAG, "could not delete $rel")
            f.parentFile?.let(dirs::add)
        }
        dirs.forEach { it.delete() }   // only succeeds when empty: the user's own files keep the folder
    }

    private fun open(method: String): HttpURLConnection = (URL(CheatDatabase.URL).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
    }

    private companion object {
        const val TAG = "CheatDb"
        const val CHUNK = 64 * 1024
        const val PROGRESS_STEP = 256 * 1024L
    }
}
