package com.nxredux.mobile.storage

/** One game path per line (hidden.txt, a collection's `.txt`): file order kept, blanks and repeats dropped. */
object PathLines {
    fun parse(text: String): List<String> =
        TextLines.split(text).filter { it.isNotBlank() }.distinct()

    fun render(list: List<String>): String = list.joinToString(separator = "") { "$it\n" }

    fun add(list: List<String>, path: String): List<String> = if (path in list) list else list + path

    fun remove(list: List<String>, path: String): List<String> = list.filter { it != path }
}
