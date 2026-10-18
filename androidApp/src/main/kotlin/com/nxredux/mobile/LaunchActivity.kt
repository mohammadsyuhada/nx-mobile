package com.nxredux.mobile

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.SplashScreen

/**
 * The app icon's (and the artwork notification's) entry, invisible and in a throwaway task of its own. MainActivity
 * is singleTask, so launching it clears whatever sits above it in its task: outside launcher mode that is the running
 * game. So an existing menu task is brought forward as it is (a game in it stays on top); only without one is
 * MainActivity started, with this launch's intent. In launcher mode the menu lives in the home task: if
 * [ActivityManager.getAppTasks] lists it, it is brought forward; if not, MainActivity is started and hands over to the
 * home instance. Either way no second home instance stays. A game's own task (launcher mode) is never picked.
 */
class LaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tasks = runCatching { getSystemService(ActivityManager::class.java).appTasks }.getOrDefault(emptyList())
        val bases = tasks.map { t -> runCatching { t.taskInfo?.let { it.baseActivity ?: it.baseIntent.component }?.className }.getOrNull() }
        val main = mainTaskIndex(bases, MainActivity::class.java.name)
        val brought = main != null && runCatching { tasks[main].moveToFront() }.isSuccess
        if (!brought) {
            // Not this launch's own excludeFromRecents (Android sets the flag on its intent): the menu's task stays in Recents.
            val start = Intent(intent).setClass(this, MainActivity::class.java)
                .apply { removeFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) }
            // A cold start keeps the icon splash (Android 13+ would show a plain one for a start from another activity).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                startActivity(start, ActivityOptions.makeBasic().setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON).toBundle())
            } else startActivity(start)
        }
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        else @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }
}

/** The index of the app's menu task: the first whose bottom activity is [mainClass] (a game's own task is not); null when none. */
internal fun mainTaskIndex(baseClassNames: List<String?>, mainClass: String): Int? =
    baseClassNames.indexOfFirst { it == mainClass }.takeIf { it >= 0 }
