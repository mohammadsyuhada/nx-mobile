package com.nxredux.mobile.options

import com.nxredux.mobile.runner.NdsRenderer

/** A core option as the frontend shows it (platform-neutral mirror of the runner's option data). */
data class CoreOption(
    val key: String,
    val value: String,
    val label: String = "",
    val labelCategorized: String = "",
    val info: String = "",
    val category: String = "",
    val categoryLabel: String = "",
    val categoryInfo: String = "",
    val values: List<String> = emptyList(),
    val valueLabels: List<String> = emptyList(),
    val defaultValue: String = "",
    val visible: Boolean = true,
    /** Values of [values] the app never offers ([com.nxredux.mobile.runner.CoreDescriptor.hiddenValues]): not cycled to. */
    val hiddenValues: Set<String> = emptySet(),
)

data class OptionCategory(val key: String, val label: String, val info: String)

/** Which options the in-game menu shows, how they group, and how values cycle (minarch rules). */
object CoreOptionFilter {
    private val RESTART_WORDS = listOf("restart", "reload")

    /**
     * The libretro API has no restart flag; cores mark such options with these words (minarch's audit). Keys in
     * [NdsRenderer.APPLY_ON_RESTART] are exempt: the app holds those itself and applies them at the next launch.
     */
    fun requiresRestart(o: CoreOption): Boolean =
        o.key !in NdsRenderer.APPLY_ON_RESTART &&
            listOf(o.label, o.labelCategorized, o.info).any { text -> RESTART_WORDS.any { text.contains(it, ignoreCase = true) } }

    /** [options] with the [pending] values (set in the menu, kept from the running core until a restart) in place of the core's. */
    fun withPending(options: List<CoreOption>, pending: Map<String, String>): List<CoreOption> =
        if (pending.isEmpty()) options else options.map { o -> pending[o.key]?.let { o.copy(value = it) } ?: o }

    /** [options] with each one's [CoreOption.hiddenValues] set from [hidden] (key -> values); unchanged without any. */
    fun withHiddenValues(options: List<CoreOption>, hidden: Map<String, Set<String>>): List<CoreOption> =
        if (hidden.isEmpty()) options else options.map { o -> hidden[o.key]?.let { o.copy(hiddenValues = it) } ?: o }

    /** The choices the menu offers: [CoreOption.values] less the hidden ones. */
    fun visibleValues(o: CoreOption): List<String> = o.values.filter { it !in o.hiddenValues }

    fun live(options: List<CoreOption>, locked: Set<String>): List<CoreOption> =
        options.filter { it.visible && it.key !in locked && !requiresRestart(it) }

    /** Outside a game (Emulator Settings): like [live], but options that apply at the next launch are offered too. */
    fun offline(options: List<CoreOption>, locked: Set<String>): List<CoreOption> =
        options.filter { it.visible && it.key !in locked }

    /** Categories with at least one of [live], in order of first appearance. */
    fun categories(live: List<CoreOption>): List<OptionCategory> =
        live.filter { it.category.isNotEmpty() }.distinctBy { it.category }
            .map { OptionCategory(it.category, it.categoryLabel.ifEmpty { it.category }, it.categoryInfo) }

    fun uncategorized(live: List<CoreOption>): List<CoreOption> = live.filter { it.category.isEmpty() }

    fun inCategory(live: List<CoreOption>, key: String): List<CoreOption> = live.filter { it.category == key }

    fun rowLabel(o: CoreOption, inCategory: Boolean): String =
        if (inCategory && o.labelCategorized.isNotEmpty()) o.labelCategorized else o.label.ifEmpty { o.key }

    fun valueLabel(o: CoreOption): String {
        val i = o.values.indexOf(o.value)
        return if (i >= 0) o.valueLabels.getOrNull(i)?.ifEmpty { null } ?: o.value else o.value
    }

    /**
     * The next visible value [delta] steps away (wrapping), skipping [CoreOption.hiddenValues]; from a hidden current value
     * (the core's own default), its visible neighbour that way; from an unknown value, the first/last choice. Null
     * without a visible choice.
     */
    fun cycle(o: CoreOption, delta: Int): String? {
        val visible = visibleValues(o)
        if (visible.isEmpty()) return null
        val n = visible.size
        val i = visible.indexOf(o.value)
        if (i >= 0) return visible[((i + delta) % n + n) % n]
        val at = o.values.indexOf(o.value)
        if (at < 0 || delta == 0) return if (delta >= 0) visible.first() else visible.last()
        // A hidden value: walk the full list from it, wrapping, to the first visible value that way.
        val step = if (delta > 0) 1 else -1
        val size = o.values.size
        return (1 until size).map { o.values[((at + it * step) % size + size) % size] }.first { it !in o.hiddenValues }
    }
}
