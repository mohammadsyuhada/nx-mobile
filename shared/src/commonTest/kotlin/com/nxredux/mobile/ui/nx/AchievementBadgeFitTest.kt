package com.nxredux.mobile.ui.nx

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nxredux.mobile.achievements.RaAchievement
import com.nxredux.mobile.achievements.RaAchievementState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The detail page's badge takes what the text leaves, between 40 and 96 dp, so the page fits. */
class AchievementBadgeFitTest {
    @Test fun roomyPageKeepsTheFullBadge() = assertEquals(96.dp, achievementBadgeSize(400.dp, 200.dp))

    @Test fun shortPageShrinksTheBadgeToFit() = assertEquals(60.dp, achievementBadgeSize(270.dp, 200.dp))   // 270 - 200 - 10 gap

    @Test fun neverSmallerThanTheMinimum() = assertEquals(40.dp, achievementBadgeSize(200.dp, 200.dp))

    @Test fun unboundedTakesTheFullBadge() = assertEquals(96.dp, achievementBadgeSize(Dp.Infinity, 200.dp))

    private fun ach(state: RaAchievementState, unlockedAt: Long? = null, progress: String? = null) =
        RaAchievement(1, "A", "d", 5, "b", state, unlockedAt, progress, 0f, 0f, null)

    @Test fun stateLineHasNoBareStateWord() {
        val time: (Long) -> String = { "on $it" }
        assertEquals("Unlocked on 2000", achievementStateLine(ach(RaAchievementState.UNLOCKED, unlockedAt = 2), time))   // the date kept
        assertNull(achievementStateLine(ach(RaAchievementState.UNLOCKED), time))
        assertNull(achievementStateLine(ach(RaAchievementState.PENDING, unlockedAt = 2), time))
        assertNull(achievementStateLine(ach(RaAchievementState.LOCKED), time))
        assertEquals("Progress: 3/10", achievementStateLine(ach(RaAchievementState.LOCKED, progress = "3/10"), time))
    }
}
