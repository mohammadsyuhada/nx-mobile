package com.nxredux.mobile.scraper

import com.swordfish.libretrodroid.NxLog
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A response body read up to a cap: a huge or endless answer never fills the heap. */
internal object BoundedBody {
    /** The most a body may be (images included; the largest art is a few MB). */
    const val MAX_BYTES = 20 * 1024 * 1024

    /** [input] read whole, or null once it passes [max] bytes (reading stops there). */
    fun read(input: InputStream, max: Int = MAX_BYTES): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) return out.toByteArray()
            total += n
            if (total > max) return null
            out.write(buf, 0, n)
        }
    }
}

/**
 * HttpURLConnection GET with AndroidRaHttp's timeouts (gzip is handled by the platform). The URL and the headers carry
 * credentials: neither is ever logged, and neither is an exception message (which may quote them): only the host and
 * status.
 * A body over [BoundedBody.MAX_BYTES] is not kept: the answer is [HttpResult.TOO_LARGE] (a failed image tries the next
 * candidate; an API answer reads as not found).
 */
class AndroidScraperHttp(private val userAgent: String) : ScraperHttp {
    override suspend fun get(url: String, headers: Map<String, String>): HttpResult = withContext(Dispatchers.IO) {
        var c: HttpURLConnection? = null
        try {
            c = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                for ((name, value) in headers) setRequestProperty(name, value)
            }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            val body = stream?.use { BoundedBody.read(it) }
            if (stream != null && body == null) {
                NxLog.i(TAG, "${hostOf(url)} -> $status, body over the cap")
                return@withContext HttpResult(HttpResult.TOO_LARGE, null)
            }
            if (status != 200) NxLog.i(TAG, "${hostOf(url)} -> $status")
            HttpResult(status, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {   // IOException (offline, timeout, TLS) or a malformed URL: no HTTP answer
            NxLog.i(TAG, "${hostOf(url)} -> network error (${e::class.java.simpleName})")
            HttpResult(-1, null)
        } finally {
            c?.disconnect()
        }
    }

    private fun hostOf(url: String): String = runCatching { URL(url).host }.getOrNull() ?: "?"

    private companion object { const val TAG = "NxScraperHttp" }
}
