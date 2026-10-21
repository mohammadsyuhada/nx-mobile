package com.nxredux.mobile.runner

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Edge to edge with the system bars hidden, a swipe showing them for a moment: the game's windows and the pad editor. */
fun Window.goImmersive() {
    WindowCompat.setDecorFitsSystemWindows(this, false)
    WindowInsetsControllerCompat(this, decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
