package com.nxredux.mobile.storage

import java.io.File

/** The game's battery save under [root] (`filesDir/sd`): the configured name, else the newest other name; null when none exists. */
fun NxPaths.findSave(root: File): File? =
    SaveLookup.pick(saveRamCandidates()) { File(root, it).takeIf { f -> f.isFile }?.lastModified() }?.let { File(root, it) }

/** The save's bytes with RZIP decoded; null when the file is RZIP but malformed; an unreadable file throws. */
fun File.readSaveDecoded(): ByteArray? = Rzip.decode(readBytes())
