package com.nxredux.mobile.runner

import kotlin.test.Test
import kotlin.test.assertEquals

class SecondScreenScaleTest {
    @Test fun a_4k_monitor_scales_to_720_dp_short_side() = assertEquals(3f, SecondScreenScale.density(3840, 2160, 2f))
    @Test fun a_low_density_large_display_scales_up() = assertEquals(1600 / 720f, SecondScreenScale.density(2560, 1600, 1f))
    @Test fun the_thor_bottom_screen_is_unchanged() = assertEquals(3f, SecondScreenScale.density(1080, 1240, 3f))
    @Test fun a_handheld_second_screen_is_unchanged() = assertEquals(2f, SecondScreenScale.density(1280, 960, 2f))
    @Test fun an_unknown_size_keeps_the_density() = assertEquals(2f, SecondScreenScale.density(0, 0, 2f))
}
