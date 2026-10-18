package com.nxredux.mobile.ui.nx

import com.nxredux.mobile.ui.InfoKind
import com.nxredux.mobile.ui.InfoLine
import kotlin.test.Test
import kotlin.test.assertEquals

class CaptionRowsTest {
    private val time = InfoLine(InfoKind.TIME, "Today - 1m 31s")
    private val count = InfoLine(InfoKind.ACHIEVEMENTS, "0 of 35")
    private val next = InfoLine(InfoKind.NEXT, "Next: Clear the First Castle Without Taking Damage")

    @Test fun aRowThatFitsStaysOneLine() {
        assertEquals(listOf(CaptionInfoRow(listOf(count, next))), fitCaptionRows(listOf(listOf(count, next)), listOf(300f), 328f, 5))
    }

    // The vertical game-list stacks' portrait family: "Next: <name>" always on its own (up to two) lines, short or not.
    @Test fun alwaysSplitPutsTheNextGoalOnItsOwnLinesEvenWhenItFits() {
        val rows = listOf(listOf(time), listOf(count, next))
        assertEquals(
            listOf(CaptionInfoRow(listOf(time)), CaptionInfoRow(listOf(count)), CaptionInfoRow(listOf(next), CAPTION_NEXT_LINES)),
            fitCaptionRows(rows, listOf(120f, 200f), 328f, 5, alwaysSplitNext = true),
        )
        // a row without a next goal, or a next goal alone, stays as it is
        assertEquals(listOf(CaptionInfoRow(listOf(time)), CaptionInfoRow(listOf(count))), fitCaptionRows(listOf(listOf(time), listOf(count)), listOf(120f, 60f), 328f, 5, alwaysSplitNext = true))
        assertEquals(listOf(CaptionInfoRow(listOf(next))), fitCaptionRows(listOf(listOf(next)), listOf(200f), 328f, 5, alwaysSplitNext = true))
    }

    @Test fun aRowJustAtTheWidthFits() {
        assertEquals(listOf(CaptionInfoRow(listOf(count, next))), fitCaptionRows(listOf(listOf(count, next)), listOf(328f), 328f, 5))
    }

    @Test fun aTooLongTrophyRowPutsTheNextGoalOnItsOwnTwoLines() {
        val rows = listOf(listOf(time), listOf(count, next))
        assertEquals(
            listOf(CaptionInfoRow(listOf(time)), CaptionInfoRow(listOf(count)), CaptionInfoRow(listOf(next), CAPTION_NEXT_LINES)),
            fitCaptionRows(rows, listOf(120f, 500f), 328f, 5),
        )
    }

    @Test fun theCarouselsOneRowKeepsTheTimeWithTheCount() {
        assertEquals(
            listOf(CaptionInfoRow(listOf(time, count)), CaptionInfoRow(listOf(next), 2)),
            fitCaptionRows(listOf(listOf(time, count, next)), listOf(600f), 328f, 3),
        )
    }

    @Test fun theNextGoalTakesOnlyTheSpareRoom() {
        assertEquals(
            listOf(CaptionInfoRow(listOf(count)), CaptionInfoRow(listOf(next), 1)),
            fitCaptionRows(listOf(listOf(count, next)), listOf(500f), 328f, 1),
        )
    }

    @Test fun noSpareRoomKeepsTheRowAndItsEllipsis() {
        assertEquals(listOf(CaptionInfoRow(listOf(count, next))), fitCaptionRows(listOf(listOf(count, next)), listOf(500f), 328f, 0))
    }

    @Test fun aRowWithoutANextGoalNeverSplits() {
        assertEquals(listOf(CaptionInfoRow(listOf(time, count))), fitCaptionRows(listOf(listOf(time, count)), listOf(500f), 328f, 5))
    }

    // The next goal on a row of its own wraps when it is too wide, out of the room left; else one line.
    @Test fun aNextGoalAloneWrapsOnlyWhenTooWide() {
        val next = InfoLine(InfoKind.NEXT, "Next: A Very Long Achievement Name")
        val time = InfoLine(InfoKind.TIME, "Today - 1h")
        assertEquals(listOf(CaptionInfoRow(listOf(time)), CaptionInfoRow(listOf(next), 2)), fitCaptionRows(listOf(listOf(time), listOf(next)), listOf(100f, 500f), 328f, 3))
        assertEquals(listOf(CaptionInfoRow(listOf(next))), fitCaptionRows(listOf(listOf(next)), listOf(500f), 328f, 0))
        assertEquals(listOf(CaptionInfoRow(listOf(next))), fitCaptionRows(listOf(listOf(next)), listOf(200f), 328f, 3))
    }
}
