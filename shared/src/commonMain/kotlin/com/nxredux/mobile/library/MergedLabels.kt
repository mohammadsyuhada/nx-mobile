package com.nxredux.mobile.library

import com.nxredux.mobile.runner.CoreRegistry

/**
 * NX Redux's row labels for a console list collated from several tags (nextui content.c, the "run" labels): games sharing
 * a shown name (case-sensitive) are told apart as
 * - the same file under every tag: "<name> (<TAG>)" (per-core copies of one rom);
 * - different files, different tags: "<stem> (<TAG>)";
 * - different files, one tag: "<stem>";
 * where <TAG> is the handheld's ([CoreRegistry.handheldTag]: GPGX_SMS shows as GPGX) and the stem is the file name
 * without its extension, or the whole file name for the group when two stems collide ("Tetris.gb" and "Tetris.gbc").
 * The handheld's arcade-title labels are not carried (the scan names arcade sets). One case the handheld cannot meet
 * is left alone: copies of one file under one tag (home and extra folders), which their folder subtitles already tell
 * apart.
 */
object MergedLabels {
    /** One row of a merged list: its shown name, file name and tag. */
    data class Row(val name: String, val fileName: String, val tag: String)

    /** The label per row, in order; null keeps the shown name (no other row shares it). */
    fun labels(rows: List<Row>): List<String?> {
        val out = arrayOfNulls<String>(rows.size)
        rows.indices.groupBy { rows[it].name }.values.filter { it.size > 1 }.forEach { group ->
            val first = rows[group[0]]
            val sameFile = group.all { rows[it].fileName == first.fileName }
            val tagsDiffer = group.any { rows[it].tag != first.tag }
            when {
                sameFile && !tagsDiffer -> Unit
                sameFile -> group.forEach { out[it] = "${rows[it].name} (${CoreRegistry.handheldTag(rows[it].tag)})" }
                else -> {
                    val stems = group.map { Names.stripExtension(rows[it].fileName) }
                    val names = if (stems.toSet().size == stems.size) stems else group.map { rows[it].fileName }
                    group.forEachIndexed { k, i -> out[i] = if (tagsDiffer) "${names[k]} (${CoreRegistry.handheldTag(rows[i].tag)})" else names[k] }
                }
            }
        }
        return out.toList()
    }
}
