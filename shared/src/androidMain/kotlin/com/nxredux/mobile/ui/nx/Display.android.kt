package com.nxredux.mobile.ui.nx

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Re-read on every configuration change: a fold or unfold handled without recreating the activity moves the window to
 * the other screen, whose dpi differs.
 */
@Composable
actual fun rememberDisplayYdpi(): Float {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    return remember(configuration, context) { context.resources.displayMetrics.ydpi }
}

@Composable
actual fun rememberHasTouchscreen(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) }
}
