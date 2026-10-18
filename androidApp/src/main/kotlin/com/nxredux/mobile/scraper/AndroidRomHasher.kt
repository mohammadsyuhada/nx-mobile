package com.nxredux.mobile.scraper

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.AndroidRefs
import java.util.zip.CRC32
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Streams the ROM (a SAF document or a file ref, [AndroidRefs]) through CRC32; null when unreadable or over
 * [ScreenScraperSource.HASH_LIMIT].
 */
class AndroidRomHasher(private val refs: AndroidRefs) : RomHasher {
    override suspend fun crc32AndSize(rom: ContentRef): Pair<Long, Long>? = withContext(Dispatchers.IO) {
        val declared = refs.size(rom)
        if (declared != null && declared > ScreenScraperSource.HASH_LIMIT) return@withContext null
        try {
            val input = refs.openInput(rom) ?: return@withContext null
            input.use {
                val crc = CRC32(); val buf = ByteArray(64 * 1024); var total = 0L
                while (true) {
                    val n = it.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > ScreenScraperSource.HASH_LIMIT) return@withContext null
                    crc.update(buf, 0, n)
                    if (total % (1024 * 1024) < n) ensureActive()   // about once per MB
                }
                crc.value to total
            }
        } catch (e: java.io.IOException) {
            null
        } catch (e: SecurityException) {   // the grant was revoked
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
