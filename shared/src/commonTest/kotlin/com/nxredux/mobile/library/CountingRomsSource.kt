package com.nxredux.mobile.library

import com.nxredux.mobile.runner.ContentRef

/** Wraps a source and records every call as "<op>:<path>" in [calls]; [onCall] sees each one as it happens. */
class CountingRomsSource(private val inner: RomsSource, private val onCall: (String) -> Unit = {}) : RomsSource {
    val calls = ArrayList<String>()

    private fun record(call: String) {
        calls += call
        onCall(call)
    }

    override suspend fun listChildren(dir: ContentRef): List<Entry> { record("listChildren:${dir.value}"); return inner.listChildren(dir) }
    override suspend fun readText(file: ContentRef): String? { record("readText:${file.value}"); return inner.readText(file) }
    override suspend fun child(dir: ContentRef, name: String): Entry? { record("child:${dir.value}/$name"); return inner.child(dir, name) }
    override suspend fun readHead(file: ContentRef, max: Int): ByteArray? { record("readHead:${file.value}"); return inner.readHead(file, max) }
    override suspend fun readRange(file: ContentRef, offset: Long, length: Int): ByteArray? { record("readRange:${file.value}"); return inner.readRange(file, offset, length) }
    override fun displayName(tree: ContentRef): String = inner.displayName(tree)   // pure: not a tree access
    override suspend fun canWrite(tree: ContentRef): Boolean { record("canWrite:${tree.value}"); return inner.canWrite(tree) }
    override suspend fun createDirectory(parent: ContentRef, name: String): Entry? { record("createDirectory:${parent.value}/$name"); return inner.createDirectory(parent, name) }
    override suspend fun writeText(dir: ContentRef, name: String, text: String): Boolean { record("writeText:${dir.value}/$name"); return inner.writeText(dir, name, text) }
    override suspend fun writeBytes(dir: ContentRef, name: String, bytes: ByteArray): Boolean { record("writeBytes:${dir.value}/$name"); return inner.writeBytes(dir, name, bytes) }
    override suspend fun delete(dir: ContentRef, name: String): Boolean { record("delete:${dir.value}/$name"); return inner.delete(dir, name) }
    override suspend fun rename(dir: ContentRef, from: String, to: String): Boolean { record("rename:${dir.value}/$from>$to"); return inner.rename(dir, from, to) }
}
