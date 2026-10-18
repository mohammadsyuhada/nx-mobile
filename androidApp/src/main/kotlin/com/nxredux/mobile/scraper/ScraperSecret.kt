package com.nxredux.mobile.scraper

/**
 * The build-time secrets' XOR decoding (androidApp/build.gradle.kts encodes each UTF-8 value with a SHA-256 key of the
 * same build, repeated). Keeps the plain text out of the dex; obfuscation against casual extraction, not secrecy.
 */
internal object ScraperSecret {
    /** [data] XOR [key] (repeated); its own inverse. An empty [key] gives [data] back unchanged. */
    fun xor(data: ByteArray, key: ByteArray): ByteArray =
        if (key.isEmpty()) data.copyOf() else ByteArray(data.size) { i -> (data[i].toInt() xor key[i % key.size].toInt()).toByte() }

    /** The plain value of [enc] under [key]; an empty secret decodes to "". */
    fun decode(enc: ByteArray, key: ByteArray): String = if (enc.isEmpty()) "" else xor(enc, key).toString(Charsets.UTF_8)
}
