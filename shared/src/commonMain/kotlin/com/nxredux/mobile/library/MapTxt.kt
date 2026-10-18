package com.nxredux.mobile.library

import com.nxredux.mobile.storage.TextLines

/**
 * NX Redux map.txt: one "<file name><TAB><display name>" per line. Lines without a tab are ignored. Keys are the
 * full file name (or a disc folder's name); the handheld looks the entry's basename up the same way.
 */
object MapTxt {
    fun parse(text: String): Map<String, String> =
        TextLines.split(text).asSequence()
            .mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) null else {
                    val key = line.substring(0, tab).trim()
                    val value = line.substring(tab + 1).trim()
                    if (key.isEmpty() || value.isEmpty()) null else key to value
                }
            }
            .toMap()

    fun render(entries: Map<String, String>): String =
        entries.entries.sortedBy { it.key }.joinToString("") { "${it.key}\t${it.value}\n" }

    /** [text] (null when the file does not exist) with [key] set to [name]. A key or name with a tab or newline cannot be stored: [text] as is (or ""). */
    fun with(text: String?, key: String, name: String): String {
        if (key.isEmpty() || name.isEmpty() || listOf(key, name).any { '\t' in it || '\n' in it || '\r' in it }) return text ?: ""
        return render(parse(text ?: "") + (key to name))
    }

    /** [text] without [key]; unchanged when absent. */
    fun without(text: String?, key: String): String = render(parse(text ?: "") - key)
}
