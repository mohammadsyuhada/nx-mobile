package com.nxredux.mobile.storage

/**
 * Lines of a user-editable text file (map.txt, systems.txt, collections, hidden.txt, recent.txt, a `.cfg`). Windows
 * editors may start a UTF-8 file with a byte order mark, which decodes to U+FEFF and would otherwise stick to the
 * first line's key.
 */
object TextLines {
    private const val BOM = "﻿"

    /** [text] without a leading byte order mark. */
    fun stripBom(text: String): String = text.removePrefix(BOM)

    /** [text] without a leading byte order mark, split on `\n`, `\r\n` or a lone `\r`. */
    fun split(text: String): List<String> = stripBom(text).replace("\r\n", "\n").replace('\r', '\n').split('\n')
}
