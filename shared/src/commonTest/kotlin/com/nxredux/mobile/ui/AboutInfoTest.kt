package com.nxredux.mobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AboutInfoTest {

    private val info = AppInfo(
        "0.1.0", 1, "9d6ae9a", "2026-10-01T09:15:00+08:00", "Android 15 (API 35)", "Samsung SM-F956B", "Qualcomm SM8650", 16,
    )

    @Test fun the_copy_text_is_the_name_line_then_one_label_value_per_row() {
        val released = formatInstallDate(isoEpochMillis("2026-10-01T09:15:00+08:00")!!)
        assertEquals(
            """
            NX Redux 0.1.0 (1) 9d6ae9a
            Released: $released
            Platform: Android 15 (API 35)
            Device: Samsung SM-F956B
            Chip: Qualcomm SM8650
            GPU: Adreno (TM) 750
            Page size: 16 KB
            """.trimIndent(),
            aboutCopyText(info, "Adreno (TM) 750"),
        )
    }

    @Test fun unknown_parts_read_unknown_and_a_pending_gpu_the_ellipsis() {
        val rows = aboutRows(AppInfo("0.1.0", commit = "unknown"), null).toMap()
        assertEquals("unknown", rows["Build"])
        assertEquals("Unknown", rows["Released"])
        assertEquals("Unknown", rows["Page size"])
        assertEquals("…", rows["GPU"])
    }

    @Test fun iso_dates_with_an_offset_parse_to_epoch_millis() {
        assertEquals(0L, isoEpochMillis("1970-01-01T00:00:00Z"))
        assertEquals(1_790_817_300_000L, isoEpochMillis("2026-10-01T09:15:00+08:00"))   // 2026-10-01T01:15:00Z
        assertEquals(1_790_817_300_000L, isoEpochMillis("2026-09-30T20:15:00.250-05:00"))
        assertEquals(951_782_400_000L, isoEpochMillis("2000-02-29T00:00:00Z"))
        assertNull(isoEpochMillis(""))
        assertNull(isoEpochMillis("2026-13-01T00:00:00Z"))
        assertNull(isoEpochMillis("2026-10-01"))
    }
}
