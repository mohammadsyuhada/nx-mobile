package com.nxredux.mobile.runner

import android.content.Context
import android.net.Uri
import com.nxredux.mobile.storage.NxPaths
import com.nxredux.mobile.storage.PathLines
import com.nxredux.mobile.storage.Recent
import com.nxredux.mobile.storage.Recents
import com.nxredux.mobile.storage.ResumeInfo
import com.nxredux.mobile.storage.Shortcut
import com.nxredux.mobile.storage.Shortcuts
import com.nxredux.mobile.storage.UserDataStore
import com.swordfish.libretrodroid.NxLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** nx-redux user data under `filesDir/sd` (recent.txt, shortcuts.txt, hidden.txt, `.minui` slot markers and screenshots). */
class AndroidUserDataStore(context: Context) : UserDataStore {
    private val root: File = NxStorage.root(context)

    override suspend fun readRecents(): List<Recent> = withContext(Dispatchers.IO) {
        runCatching { File(root, NxPaths.RECENTS).takeIf { it.exists() }?.readText()?.let(Recents::parse) }.getOrNull() ?: emptyList()
    }

    override suspend fun writeRecents(list: List<Recent>) = withContext(Dispatchers.IO) {
        runCatching { File(root, NxPaths.RECENTS).writeAtomically(Recents.render(list).encodeToByteArray()) }
            .onFailure { NxLog.e("UserData", "writing recents failed", it) }
        Unit
    }

    override suspend fun readPins(): List<Shortcut> = withContext(Dispatchers.IO) {
        runCatching { File(root, NxPaths.SHORTCUTS).takeIf { it.exists() }?.readText()?.let(Shortcuts::parse) }.getOrNull() ?: emptyList()
    }

    override suspend fun writePins(list: List<Shortcut>): Boolean = withContext(Dispatchers.IO) {
        runCatching { File(root, NxPaths.SHORTCUTS).writeAtomically(Shortcuts.render(list).encodeToByteArray()); true }
            .onFailure { NxLog.e("UserData", "writing pins failed", it) }.getOrDefault(false)
    }

    override suspend fun readHidden(): List<String> = withContext(Dispatchers.IO) {
        runCatching { File(root, NxPaths.HIDDEN).takeIf { it.exists() }?.readText()?.let(PathLines::parse) }.getOrNull() ?: emptyList()
    }

    override suspend fun writeHidden(list: List<String>): Boolean = withContext(Dispatchers.IO) {
        runCatching { File(root, NxPaths.HIDDEN).writeAtomically(PathLines.render(list).encodeToByteArray()); true }
            .onFailure { NxLog.e("UserData", "writing hidden failed", it) }.getOrDefault(false)
    }

    override suspend fun resumeInfo(tag: String, fileName: String): ResumeInfo = withContext(Dispatchers.IO) {
        // The .minui paths depend only on tag + ROM file name; the core id is not part of them.
        val paths = NxPaths(tag, "", fileName)
        val slotText = runCatching { File(root, paths.lastSlot).takeIf { it.exists() }?.readText() }.getOrNull()
        val slot = ResumeInfo.slotFrom(slotText) ?: return@withContext ResumeInfo.NONE
        // NxPaths.thumb accepts 0..7 and 9; slot 8 has no screenshot in minarch's scheme.
        val bmp = slot.takeIf { it != 8 }?.let { File(root, paths.thumb(it)) }?.takeIf { it.exists() }
        ResumeInfo(slot, bmp?.let { Uri.fromFile(it).buildUpon().fragment(it.lastModified().toString()).build().toString() })
    }
}
