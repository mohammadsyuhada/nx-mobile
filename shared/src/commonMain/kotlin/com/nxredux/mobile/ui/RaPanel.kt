package com.nxredux.mobile.ui

import com.nxredux.mobile.achievements.RaAchievement
import com.nxredux.mobile.achievements.RaAchievementState

/** What the achievements panel shows for its game ([RaPanel]). */
sealed interface RaPanelContent {
    /** The game's cached achievements are being read. */
    data object Loading : RaPanelContent
    /** No RetroAchievements account: the panel offers the sign-in page. */
    data object SignedOut : RaPanelContent
    /** No game highlighted, or one RetroAchievements has no console for. */
    data object NoData : RaPanelContent
    /** The game had no cached data: its achievements are downloading in the background. */
    data object Downloading : RaPanelContent
    /** That download could not finish: [reason] ("no connection"); the panel opened again tries once more. */
    data class Failed(val reason: String) : RaPanelContent
    /** RetroAchievements does not know the game's file ([hash]): not downloaded again, the panel says why. */
    data class Unrecognised(val hash: String) : RaPanelContent
    /** The game's file could not be read to identify it: not tried again this run. */
    data object Unreadable : RaPanelContent
    /** The game's achievements (sorted as Settings says) and its badge listing. */
    data class Game(val hash: String, val achievements: List<RaAchievement>, val badges: Map<String, String>) : RaPanelContent
}

/**
 * The achievements panel over a game list (the trophy icon, or X): the highlighted game's achievements,
 * following the list's selection. [path] / [title]: the game shown (null: no game highlighted); [row] the highlighted
 * achievement among the [visible] ones; [detail] that one's details open in the panel.
 */
data class RaPanel(
    val path: String?, val title: String, val content: RaPanelContent,
    val row: Int = 0, val detail: Boolean = false,
) {
    /** The rows the panel lists: every achievement of its game. */
    val visible: List<RaAchievement>
        get() = (content as? RaPanelContent.Game)?.achievements.orEmpty()

    /** "12 of 35" (pending counted as unlocked, as the menus' info); null without a game's list. */
    val progress: String?
        get() = (content as? RaPanelContent.Game)?.achievements?.takeIf { it.isNotEmpty() }?.let { all ->
            "${all.count { it.state != RaAchievementState.LOCKED }} of ${all.size}"
        }
}
