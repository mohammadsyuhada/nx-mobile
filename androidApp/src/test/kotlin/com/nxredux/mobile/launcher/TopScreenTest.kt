package com.nxredux.mobile.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopScreenTest {
    @Test fun outsideDualModeAppsLaunchAsUsual() = assertNull(topScreenDisplay(dual = false, menuOnSecond = false, own = 0, second = 10))

    @Test fun noSecondDisplayLaunchesAsUsual() = assertNull(topScreenDisplay(dual = true, menuOnSecond = false, own = 0, second = null))

    @Test fun theUsualLayoutsTopScreenIsTheSecondDisplay() = assertEquals(10, topScreenDisplay(dual = true, menuOnSecond = false, own = 0, second = 10))

    @Test fun withTheMenuOnTheSecondDisplayTheTopScreenIsThisOne() = assertEquals(0, topScreenDisplay(dual = true, menuOnSecond = true, own = 0, second = 10))

    @Test fun noTopScreenIsTheUsualLaunch() = assertEquals(LaunchRoute.USUAL, launchRoute(top = null, own = 0))

    @Test fun aTopScreenOnThisDisplayLaunchesHere() = assertEquals(LaunchRoute.HERE, launchRoute(top = 0, own = 0))

    @Test fun aTopScreenOnAnotherDisplayGoesThroughAHost() = assertEquals(LaunchRoute.HOST, launchRoute(top = 10, own = 0))

    @Test fun theFirstShowingIsTheHostItselfBeforeTheAppCoversIt() {
        val watch = CoverWatch()
        assertFalse(watch.resumed())
        watch.covered()
        assertTrue(watch.resumed())
    }

    @Test fun aHostCoveredBeforeItEverShowedIsFreedOnItsFirstShowing() {
        val watch = CoverWatch()
        watch.covered()   // onStop without a resume: the app came up at once
        assertTrue(watch.resumed())
    }
}
