package com.nxredux.mobile.storage

/**
 * In-memory [LocalFiles]: [dirs] and [files] are absolute paths (each volume's root is a folder); a folder lists the
 * entries whose parent it is; any other path lists empty, as an unreadable one does.
 */
class FakeLocalFiles(
    var volumes: List<LocalVolume> = listOf(LocalVolume("Internal storage", "/storage/emulated/0")),
    dirs: List<String> = emptyList(),
    files: List<String> = emptyList(),
) : LocalFiles {
    val dirs: MutableSet<String> = (volumes.map { it.path } + dirs).toMutableSet()
    val files: MutableSet<String> = files.toMutableSet()
    /** Folders that take no new files ([canWrite] false). */
    val unwritable = mutableSetOf<String>()
    var mkdirFails = false

    private fun parentOf(path: String) = path.substringBeforeLast('/')

    override suspend fun volumes(): List<LocalVolume> = volumes
    override suspend fun list(path: String): List<LocalEntry> =
        dirs.filter { it != path && parentOf(it) == path }.map { LocalEntry(it.substringAfterLast('/'), true) } +
            files.filter { parentOf(it) == path }.map { LocalEntry(it.substringAfterLast('/'), false) }
    override suspend fun mkdir(parent: String, name: String): Boolean {
        val path = "$parent/$name"
        if (mkdirFails || path in dirs || path in files) return false
        dirs += path
        return true
    }
    override suspend fun isDirectory(path: String): Boolean = path in dirs
    override suspend fun canWrite(path: String): Boolean = path in dirs && path !in unwritable
}
