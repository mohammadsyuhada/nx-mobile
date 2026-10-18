package com.nxredux.mobile.options

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One core's options as probed at core-build time (cores/probe). */
data class CoreOptionTable(val core: String, val emulator: String, val options: List<CoreOption>)

/** Where a core's bundled table comes from (Android: assets/core-options/<coreId>.json). */
interface CoreOptionSource { fun table(coreId: String): CoreOptionTable? }

object NoCoreOptions : CoreOptionSource { override fun table(coreId: String): CoreOptionTable? = null }

object CoreOptionTables {
    @Serializable private data class Item(
        val key: String, val label: String = "", val labelCategorized: String = "", val info: String = "",
        val values: List<String> = emptyList(), val labels: List<String> = emptyList(), val default: String = "",
    )
    @Serializable private data class Section(val key: String = "", val label: String = "", val info: String = "", val items: List<Item> = emptyList())
    @Serializable private data class File(val core: String, val emulator: String = "", val sections: List<Section>)

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Null for anything that isn't a table (a broken or truncated file). A declared default the core doesn't list among
     * its values reads as the first value, as RetroArch does (genesis_plus_gx_overclock declares "100%" of "100", ...).
     */
    fun parse(text: String): CoreOptionTable? {
        val f = runCatching { json.decodeFromString(File.serializer(), text) }.getOrNull() ?: return null
        val options = f.sections.flatMap { s ->
            s.items.map { i ->
                val default = if (i.values.isEmpty() || i.default in i.values) i.default else i.values[0]
                CoreOption(
                    key = i.key, value = default, label = i.label, labelCategorized = i.labelCategorized, info = i.info,
                    category = s.key, categoryLabel = s.label, categoryInfo = s.info,
                    values = i.values, valueLabels = i.labels, defaultValue = default,
                )
            }
        }
        return CoreOptionTable(f.core, f.emulator.ifEmpty { f.core }, options)
    }
}
