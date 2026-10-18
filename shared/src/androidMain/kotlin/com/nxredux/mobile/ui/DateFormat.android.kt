package com.nxredux.mobile.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatInstallDate(epochMillis: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

actual fun formatDateTime(epochMillis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMillis))

actual fun formatUnlockTime(epochMillis: Long): String = SimpleDateFormat("MMMM d yyyy, hh:mma", Locale.getDefault()).format(Date(epochMillis))

actual fun nowMs(): Long = System.currentTimeMillis()
