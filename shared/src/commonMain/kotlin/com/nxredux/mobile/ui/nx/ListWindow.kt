package com.nxredux.mobile.ui.nx

/**
 * First visible row of a list showing [perPage] whole rows out of [count], given the previous first row
 * [scroll]: scrolls the minimum needed to keep [selected] visible (NX Redux `UI_adjustListScroll`), and
 * clamps so the window never runs past the end or starts before 0.
 */
fun windowStart(selected: Int, scroll: Int, perPage: Int, count: Int): Int {
    if (count <= 0 || perPage <= 0) return 0
    val maxStart = maxOf(0, count - perPage)
    var start = scroll.coerceIn(0, maxStart)
    val sel = selected.coerceIn(0, count - 1)
    if (sel < start) start = sel
    if (sel >= start + perPage) start = sel - perPage + 1
    return start.coerceIn(0, maxStart)
}
