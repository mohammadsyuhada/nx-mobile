package com.nxredux.mobile.ui.nx

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/** The detail page's badge takes what the text leaves, between 40 and 96 dp, so the page fits. */
class AchievementBadgeFitTest {
    @Test fun roomyPageKeepsTheFullBadge() = assertEquals(96.dp, achievementBadgeSize(400.dp, 200.dp))

    @Test fun shortPageShrinksTheBadgeToFit() = assertEquals(60.dp, achievementBadgeSize(270.dp, 200.dp))   // 270 - 200 - 10 gap

    @Test fun neverSmallerThanTheMinimum() = assertEquals(40.dp, achievementBadgeSize(200.dp, 200.dp))

    @Test fun unboundedTakesTheFullBadge() = assertEquals(96.dp, achievementBadgeSize(Dp.Infinity, 200.dp))
}
