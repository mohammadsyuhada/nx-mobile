package com.nxredux.mobile.netplay

/**
 * netplay: the peer's address as the engine takes it, canonical dotted IPv4 ("10.0.0.2"). A dual-stack socket may report
 * an IPv4 client as IPv4-mapped IPv6 (`::ffff:10.0.0.2`, also in its long or hex form): that becomes the IPv4 it holds.
 * Anything else (a real IPv6 address, garbage) is null, and the session then fails closed.
 */
object NetplayAddress {
    private val MAPPED_PREFIX = ByteArray(10) + byteArrayOf(-1, -1)

    /** From an address's raw bytes (4, or 16 when IPv4-mapped). */
    fun ipv4(bytes: ByteArray?): String? = when {
        bytes == null -> null
        bytes.size == 4 -> bytes.joinToString(".") { (it.toInt() and 0xff).toString() }
        bytes.size == 16 && bytes.copyOfRange(0, 12).contentEquals(MAPPED_PREFIX) -> ipv4(bytes.copyOfRange(12, 16))
        else -> null
    }

    /** From an address's text form (a "%zone" suffix is ignored). */
    fun ipv4(text: String?): String? {
        val t = text?.substringBefore('%') ?: return null
        dotted(t)?.let { return ipv4(it) }
        if (':' !in t) return null
        val lastColon = t.lastIndexOf(':')
        val head = t.substring(0, lastColon + 1).lowercase()
        val tail = t.substring(lastColon + 1)
        // ::ffff:a.b.c.d or 0:0:0:0:0:ffff:a.b.c.d
        if (head == "::ffff:" || head == "0:0:0:0:0:ffff:") return dotted(tail)?.let(::ipv4)
        // the hex form: ::ffff:c0a8:114 / 0:0:0:0:0:ffff:c0a8:114
        val groups = t.lowercase().let { if (it.startsWith("::ffff:")) listOf("0", "0", "0", "0", "0") + it.removePrefix("::").split(':') else it.split(':') }
        if (groups.size != 8 || groups.any { it.isEmpty() || it.length > 4 || !it.all { c -> c in "0123456789abcdef" } }) return null
        val words = groups.map { it.toInt(16) }
        if (words.take(5).any { it != 0 } || words[5] != 0xffff) return null
        return ipv4(byteArrayOf((words[6] shr 8).toByte(), words[6].toByte(), (words[7] shr 8).toByte(), words[7].toByte()))
    }

    /** a.b.c.d with each part 0..255 in plain decimal, as 4 bytes; null otherwise. */
    private fun dotted(s: String): ByteArray? {
        val parts = s.split('.')
        if (parts.size != 4) return null
        val out = ByteArray(4)
        for ((i, p) in parts.withIndex()) {
            if (p.isEmpty() || p.length > 3 || !p.all { it in '0'..'9' }) return null
            val v = p.toInt()
            if (v > 255) return null
            out[i] = v.toByte()
        }
        return out
    }
}
