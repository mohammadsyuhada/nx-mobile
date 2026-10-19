package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.Console
import com.nxredux.mobile.runner.CoreDescriptor
import com.nxredux.mobile.runner.CoreRegistry

/**
 * RetroArch keeps saves and states in a subfolder named by the core's libretro `library_name` (`saves/mGBA`); this maps
 * such a name to our core and the consoles it serves.
 */
object RetroArchCores {
    /** Library names that differ from [CoreDescriptor.name], lowercase, to core id. */
    private val ALIASES = mapOf("beetle supafaust" to "mednafen_supafaust", "dolphin-emu" to "dolphin")

    /** The core a RetroArch save/state subfolder [folder] belongs to (case-insensitive); null when none of ours. */
    fun coreOf(folder: String): CoreDescriptor? {
        val f = folder.trim().lowercase()
        ALIASES[f]?.let { id -> return CoreRegistry.all.firstOrNull { it.id == id } }
        return CoreRegistry.all.firstOrNull { it.name.lowercase() == f }
    }

    /** The phone tags whose core is [core], in [CoreRegistry.tags] order. */
    fun tagsOf(core: CoreDescriptor): List<String> = CoreRegistry.tags.filter { CoreRegistry.forTag(it)?.id == core.id }

    /**
     * The consoles [folder] hints at, or null when it names nothing. A NextUI tag wins over a core name; a shared
     * handheld tag (`GPGX`) gives every console it serves.
     */
    fun consolesOf(folder: String): List<Console>? {
        val tag = folder.trim().uppercase()
        val byTag = if (tag in CoreRegistry.SHARED_HANDHELD_TAGS) {
            CoreRegistry.consoles.filter { c -> c.tags.any { CoreRegistry.handheldTag(it) == tag } }
        } else listOfNotNull(CoreRegistry.consoleOf(tag))
        if (byTag.isNotEmpty()) return byTag
        val core = coreOf(folder) ?: return null
        val tags = tagsOf(core).toSet()
        return CoreRegistry.consoles.filter { c -> c.tags.any { it.uppercase() in tags } }.ifEmpty { null }
    }
}
