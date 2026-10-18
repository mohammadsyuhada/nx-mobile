package com.nxredux.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainTaskIndexTest {
    private val main = "com.nxredux.mobile.MainActivity"
    private val game = "com.nxredux.mobile.runner.GameActivity"

    @Test fun noTasksIsAColdStart() = assertNull(mainTaskIndex(emptyList(), main))

    @Test fun aGamesOwnTaskIsNotTheMenus() = assertNull(mainTaskIndex(listOf(game, null), main))

    @Test fun theMenuTaskIsFoundAmongOthers() = assertEquals(1, mainTaskIndex(listOf(game, main, null), main))
}
