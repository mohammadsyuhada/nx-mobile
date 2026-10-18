package com.nxredux.mobile.runner

import android.content.Context
import java.io.File

/**
 * The bundled shader presets on the SD root (`Shaders/`, RetroArch `.glslp` + `.glsl` layout). The tree is
 * app-owned and replaced whole on APK update, so it is not a place for user files.
 */
object Shaders {
    const val DIR = "Shaders"

    fun file(sdRoot: File, relPath: String): File = File(File(sdRoot, DIR), relPath)

    /** The text of a shader file by path relative to `Shaders/`, or null when missing/unreadable. */
    fun read(sdRoot: File, relPath: String): String? = runCatching { file(sdRoot, relPath).takeIf { it.isFile }?.readText() }.getOrNull()

    /**
     * Every preset offered: `*.glslp` anywhere under `Shaders/`, and bare `*.glsl` directly in `Shaders/` or in
     * `Shaders/glsl/` (the nx-redux layout). Relative paths, sorted case-insensitively. Only the bundled set:
     * files added to the tree by hand are dropped on the next APK update. Blocking I/O.
     */
    fun presets(sdRoot: File): List<String> {
        val root = File(sdRoot, DIR)
        val out = mutableListOf<String>()
        root.walkTopDown().filter { it.isFile && !it.name.startsWith("._") && it.name != ".DS_Store" }.forEach { f ->
            val rel = f.relativeTo(root).path.replace('\\', '/')
            val bare = rel.endsWith(".glsl", ignoreCase = true) && (!rel.contains('/') || rel.startsWith("glsl/") && rel.count { it == '/' } == 1)
            if (rel.endsWith(".glslp", ignoreCase = true) || bare) out += rel
        }
        return out.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    /**
     * Syncs `sd/Shaders/` with the APK's shaders: the tree is app-owned and replaced whole on APK update (see
     * [BundledAssets.sync]). Blocking I/O: call off the main thread.
     */
    fun extractBundled(context: Context): Boolean = BundledAssets.sync(context, "shaders", DIR)
}
