package com.nxredux.mobile.gametime

import kotlin.test.Test
import kotlin.test.assertEquals

class GameTimeFormatTest {
    @Test fun seconds() = assertEquals("45s", GameTimeFormat.duration(45_900))
    @Test fun minutes() = assertEquals("3m 7s", GameTimeFormat.duration(187_000))
    @Test fun hours() = assertEquals("2h 5m", GameTimeFormat.duration(2 * 3_600_000L + 5 * 60_000L + 59_000))
    @Test fun zero() = assertEquals("0s", GameTimeFormat.duration(0))
    @Test fun rowLine() = assertEquals(
        "1h 0m total  ·  30m 0s avg  ·  2 plays",
        GameTimeFormat.rowLine(GameTotal(1, "Tetris", "GB", "/p", 3_600_000, 2, 0)),
    )
    @Test fun rowLineOnePlay() = assertEquals(
        "10s total  ·  10s avg  ·  1 play",
        GameTimeFormat.rowLine(GameTotal(1, "Tetris", "GB", "/p", 10_000, 1, 0)),
    )
    @Test fun rowPartsEmphasiseValuesOnly() {
        val parts = GameTimeFormat.rowParts(GameTotal(1, "Tetris", "GB", "/p", 10_000, 1, 0))
        assertEquals(listOf("10s", "10s", "1"), parts.filter { it.second }.map { it.first })
        assertEquals(listOf(" total", "  ·  ", " avg", "  ·  ", " play"), parts.filterNot { it.second }.map { it.first })
    }
}
