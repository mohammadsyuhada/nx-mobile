package com.nxredux.mobile.runner

/** assets/wii/controls.tsv: exact disc IDs, then the 3-character game code (another region of the same game). */
class WiiControlsTable private constructor(private val byId: Map<String, WiiControls>) {
    private val byCode: Map<String, WiiControls> = byId.entries.groupBy { it.key.take(3) }.mapValues { it.value.first().value }

    fun lookup(id: String?): WiiControls? {
        if (id == null || id.length != 6) return null
        return byId[id] ?: byCode[id.take(3)]
    }

    companion object {
        val EMPTY = WiiControlsTable(emptyMap())

        fun parse(text: String): WiiControlsTable = WiiControlsTable(
            text.lineSequence()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split('\t')
                    val id = parts[0].trim()
                    if (parts.size != 2 || id.length != 6) null else id to WiiControls.parse(parts[1])
                }
                .toMap(),
        )
    }
}
