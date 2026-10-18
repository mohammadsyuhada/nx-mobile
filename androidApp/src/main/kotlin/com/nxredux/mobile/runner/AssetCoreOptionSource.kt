package com.nxredux.mobile.runner

import android.content.res.AssetManager
import com.nxredux.mobile.options.CoreOptionSource
import com.nxredux.mobile.options.CoreOptionTable
import com.nxredux.mobile.options.CoreOptionTables
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/** The bundled `assets/core-options/<coreId>.json` tables, parsed once per core; a missing or malformed table reads as null. */
class AssetCoreOptionSource(private val open: (path: String) -> InputStream) : CoreOptionSource {
    constructor(assets: AssetManager) : this({ assets.open(it) })

    private val cache = ConcurrentHashMap<String, Optional<CoreOptionTable>>()

    override fun table(coreId: String): CoreOptionTable? =
        cache.getOrPut(coreId) { Optional.ofNullable(load(coreId)) }.orElse(null)

    private fun load(coreId: String): CoreOptionTable? {
        val text = try {
            open("core-options/$coreId.json").bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            return null
        }
        return CoreOptionTables.parse(text)
    }
}
