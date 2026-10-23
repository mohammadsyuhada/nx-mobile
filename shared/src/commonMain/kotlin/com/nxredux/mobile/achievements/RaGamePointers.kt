package com.nxredux.mobile.achievements

import com.nxredux.mobile.library.Names
import com.nxredux.mobile.storage.GamePath

/**
 * nx-redux's `rom.txt`: the ROM last loaded with a hash, relative to the card root (`Roms/<Folder>/…/<file>`, no leading
 * slash, no newline). Older nx-redux wrote it absolute (`/mnt/SDCARD/Roms/…`); both read.
 */
object RaRomRecord {
    /** [p]'s `rom.txt`; null for an extra-folder game or a dir outside `Roms/`. */
    fun text(p: RaGamePointer): String? {
        if (p.source != GamePath.HOME) return null
        val parts = p.dir.split('/').filter { it.isNotEmpty() }
        if (parts.size < 2 || parts[0] != "Roms") return null
        return (parts + p.file).joinToString("/")
    }

    /** A home pointer from a `rom.txt` (title empty: callers fall back to the set's title); null when not under `Roms/<Name (TAG)>/`. */
    fun pointer(text: String): RaGamePointer? {
        val t = text.trim().replace('\\', '/')
        val at = if (t.startsWith("Roms/")) 0 else t.indexOf("/Roms/").takeIf { it >= 0 }?.plus(1) ?: return null
        val parts = t.substring(at).split('/').filter { it.isNotEmpty() }
        if (parts.size < 3 || parts.any { it == "." || it == ".." }) return null
        val tag = Names.parseSystemFolder(parts[1])?.second ?: return null
        return RaGamePointer(tag.uppercase(), GamePath.HOME, parts.dropLast(1).joinToString("/"), parts.last(), "")
    }
}

/** A cached game's library pointer: mobile's `game.json`, else the `rom.txt` a handheld wrote (Device Sync brings it). */
object RaGamePointers {
    suspend fun read(store: RaStore, hash: String): RaGamePointer? =
        store.readJson(RaPaths.game(hash), RaGamePointer.serializer())
            ?: store.read(RaPaths.romRecord(hash))?.decodeToString()?.let(RaRomRecord::pointer)

    /** Writes `game.json` and, for a home game, `rom.txt` (prefetch: the walked entry is authoritative). */
    suspend fun write(store: RaStore, hash: String, p: RaGamePointer) {
        store.writeAtomic(RaPaths.game(hash), RaJson.json.encodeToString(RaGamePointer.serializer(), p).encodeToByteArray())
        writeRecord(store, hash, p)
    }

    /** A played game: an existing `game.json` (from prefetch) is kept; `rom.txt` follows the ROM just loaded. */
    suspend fun remember(store: RaStore, hash: String, p: RaGamePointer) {
        if (!store.exists(RaPaths.game(hash))) store.writeAtomic(RaPaths.game(hash), RaJson.json.encodeToString(RaGamePointer.serializer(), p).encodeToByteArray())
        writeRecord(store, hash, p)
    }

    private suspend fun writeRecord(store: RaStore, hash: String, p: RaGamePointer) {
        val text = RaRomRecord.text(p) ?: return
        if (store.read(RaPaths.romRecord(hash))?.decodeToString() != text) store.writeAtomic(RaPaths.romRecord(hash), text.encodeToByteArray())
    }
}
