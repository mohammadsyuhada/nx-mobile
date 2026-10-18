package com.nxredux.mobile.ui.nx

data class ListSelection(val index: Int, val count: Int) {
    /** One row on; past the last, the first ([wrap] false, as for a held key: it stays on the last). */
    fun down(wrap: Boolean = true): ListSelection =
        if (count == 0 || (!wrap && index >= count - 1)) this else copy(index = (index + 1) % count)
    /** One row back; before the first, the last ([wrap] false: it stays on the first). */
    fun up(wrap: Boolean = true): ListSelection =
        if (count == 0 || (!wrap && index <= 0)) this else copy(index = (index - 1 + count) % count)
    fun clampedTo(newCount: Int): ListSelection =
        ListSelection(index = if (newCount == 0) 0 else index.coerceIn(0, newCount - 1), count = newCount)
}
