package com.nxredux.mobile.storage

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.nxredux.mobile.runner.ContentRef
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * The one place a ROM, a companion or a picked file is opened outside RomsSource: a SAF document through [resolver], a
 * file ref ([FileRefs]) through java.io.File, never `Uri.parse` on a path (a ROM name may hold '#', '?' or '%'). A
 * missing file throws FileNotFoundException from the opens, as the provider does. [resolver] null: file refs only (JVM
 * tests); a content ref then reads as unknown. Blocking: Dispatchers.IO.
 */
class AndroidRefs(private val resolver: ContentResolver?) {
    private fun fileOf(ref: ContentRef): File? = if (FileRefs.isFile(ref)) File(FileRefs.path(ref)) else null

    /** Opens [ref] as a file descriptor in [mode] ("r", "w", "rw", "wt"...); null when there is no resolver for a content ref. */
    fun openFd(ref: ContentRef, mode: String = "r"): ParcelFileDescriptor? {
        fileOf(ref)?.let { return ParcelFileDescriptor.open(it, ParcelFileDescriptor.parseMode(mode)) }
        return resolver?.openFileDescriptor(Uri.parse(ref.value), mode)
    }

    /** Opens [ref] for reading; null when there is no resolver for a content ref. */
    fun openInput(ref: ContentRef): InputStream? {
        fileOf(ref)?.let { return FileInputStream(it) }
        return resolver?.openInputStream(Uri.parse(ref.value))
    }

    /** The file's size; for a document the provider's reported size. Null when unknown, missing or a folder. */
    fun size(ref: ContentRef): Long? {
        fileOf(ref)?.let { return it.takeIf(File::isFile)?.length() }
        return queryLong(ref, OpenableColumns.SIZE)?.takeIf { it >= 0 }
    }

    /** Last-modified in epoch millis; null when unknown (0 included: never "1970"). */
    fun lastModified(ref: ContentRef): Long? {
        fileOf(ref)?.let { return it.lastModified().takeIf { ms -> ms > 0 } }
        return queryLong(ref, DocumentsContract.Document.COLUMN_LAST_MODIFIED)?.takeIf { it > 0 }
    }

    /** The file name of [ref]; null when unknown. */
    fun displayName(ref: ContentRef): String? {
        fileOf(ref)?.let { return it.name }
        val r = resolver ?: return null
        return runCatching {
            r.query(Uri.parse(ref.value), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun queryLong(ref: ContentRef, column: String): Long? {
        val r = resolver ?: return null
        return runCatching {
            r.query(Uri.parse(ref.value), arrayOf(column), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
            }
        }.getOrNull()
    }
}
