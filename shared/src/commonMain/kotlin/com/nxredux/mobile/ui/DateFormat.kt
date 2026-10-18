package com.nxredux.mobile.ui

/** `d MMM yyyy` in the device locale, e.g. `25 Sep 2026`. */
expect fun formatInstallDate(epochMillis: Long): String

/** `yyyy-MM-dd HH:mm` in the device locale's zone. */
expect fun formatDateTime(epochMillis: Long): String

/** An achievement's unlock time, NX Redux style: `July 11 2026, 08:21AM` in the device locale and zone. */
expect fun formatUnlockTime(epochMillis: Long): String

/** The wall clock, epoch ms (the menu's "Today", "2 days ago"). */
expect fun nowMs(): Long
