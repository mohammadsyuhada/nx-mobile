package com.nxredux.mobile.library

/**
 * The document id of child [name] under [parentId], for providers whose ids are paths (Android's
 * external-storage provider: "primary:" is the volume root, "primary:Roms/Game Boy (GB)" a folder).
 * Null when [name] is not a single path segment (empty, contains '/', or is "." / ".."), since that
 * would address some other document.
 */
fun pathChildDocId(parentId: String, name: String): String? {
    if (name.isEmpty() || '/' in name || name == "." || name == "..") return null
    return if (parentId.endsWith(":") || parentId.endsWith("/")) parentId + name else "$parentId/$name"
}

/**
 * The path document id one level above [docId] ("primary:Roms/My Arcade" -> "primary:Roms", "primary:Roms" ->
 * "primary:"), or null when [docId] is the tree root [treeDocId] or not inside that tree.
 */
fun pathParentDocId(docId: String, treeDocId: String): String? {
    val id = docId.trimEnd('/')
    val tree = treeDocId.trimEnd('/')
    if (id == tree) return null
    val inside = if (tree.endsWith(":")) id.startsWith(tree) else id.startsWith("$tree/")
    if (!inside) return null
    val cut = id.lastIndexOf('/')
    return if (cut >= 0) id.substring(0, cut) else id.substringBefore(':') + ":"
}

/**
 * True when path document id [docId] ("4913-1501:Roms") lies on a volume whose uuid is not in [mounted] (case ignored):
 * a removed or unmounted SD card. Internal shared storage ("primary", or a root id built on it such as
 * "primary_dcim") and "home", and ids with no volume prefix, are never offline.
 */
fun volumeOffline(docId: String, mounted: Collection<String>): Boolean {
    val volume = docId.substringBefore(':', "")
    if (volume.isEmpty() || volume.startsWith("primary", ignoreCase = true) || volume.equals("home", ignoreCase = true)) return false
    return mounted.none { it.equals(volume, ignoreCase = true) }
}
