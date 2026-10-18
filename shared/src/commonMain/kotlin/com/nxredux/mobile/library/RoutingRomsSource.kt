package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.storage.FileRefs

/**
 * The app's RomsSource: a file ref ([FileRefs]) goes to [file], every other ref (a SAF document or tree) to [saf].
 * Each call routes on its own ref, so a library can mix a file home with SAF extras.
 */
class RoutingRomsSource(private val saf: RomsSource, private val file: RomsSource) : RomsSource {
    private fun of(ref: ContentRef): RomsSource = if (FileRefs.isFile(ref)) file else saf

    override suspend fun listChildren(dir: ContentRef): List<Entry> = of(dir).listChildren(dir)
    override suspend fun readText(file: ContentRef): String? = of(file).readText(file)
    override suspend fun child(dir: ContentRef, name: String): Entry? = of(dir).child(dir, name)
    override suspend fun readHead(file: ContentRef, max: Int): ByteArray? = of(file).readHead(file, max)
    override suspend fun readRange(file: ContentRef, offset: Long, length: Int): ByteArray? = of(file).readRange(file, offset, length)

    /** Within one kind of tree only: a move or copy between a SAF tree and a file tree is refused (false). */
    override suspend fun moveFile(fromDir: ContentRef, name: String, toDir: ContentRef): Boolean =
        FileRefs.isFile(fromDir) == FileRefs.isFile(toDir) && of(fromDir).moveFile(fromDir, name, toDir)

    override suspend fun copyFile(fromDir: ContentRef, name: String, toDir: ContentRef): Boolean =
        FileRefs.isFile(fromDir) == FileRefs.isFile(toDir) && of(fromDir).copyFile(fromDir, name, toDir)
    override fun displayName(tree: ContentRef): String = of(tree).displayName(tree)
    override suspend fun isOffline(tree: ContentRef): Boolean = of(tree).isOffline(tree)
    override suspend fun canWrite(tree: ContentRef): Boolean = of(tree).canWrite(tree)
    override suspend fun createDirectory(parent: ContentRef, name: String): Entry? = of(parent).createDirectory(parent, name)
    override suspend fun writeText(dir: ContentRef, name: String, text: String): Boolean = of(dir).writeText(dir, name, text)
    override suspend fun writeBytes(dir: ContentRef, name: String, bytes: ByteArray): Boolean = of(dir).writeBytes(dir, name, bytes)
    override suspend fun delete(dir: ContentRef, name: String): Boolean = of(dir).delete(dir, name)
    override suspend fun deleteEmptyDirectory(parent: ContentRef, name: String): Boolean = of(parent).deleteEmptyDirectory(parent, name)
    override suspend fun rename(dir: ContentRef, from: String, to: String): Boolean = of(dir).rename(dir, from, to)
}
