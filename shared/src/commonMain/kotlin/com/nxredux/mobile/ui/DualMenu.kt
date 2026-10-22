package com.nxredux.mobile.ui

/** A list shown on the second display: its [source], its [entries] and the selected [index]. */
data class SecondList(val source: ListSource, val entries: List<Listed>, val index: Int)

/** What the second display's top area shows while the menu is there: a [Backdrop], a [Preview] of a list, or the [GameList]. */
sealed interface TopContent {
    data object Backdrop : TopContent
    data class Preview(val list: SecondList) : TopContent
    data object GameList : TopContent
}

/** What the bottom area shows: the usual menu ([Normal]) or the selected game's [Details]. */
enum class BottomContent { Normal, Details }

/** The main menu on the second display: which content goes where, from the screen alone. */
object DualMenu {
    /** The game list on a game list; the [preview] on the main menu when it has one; else the backdrop. */
    fun top(screen: Screen, preview: SecondList?): TopContent = when {
        screen is Screen.Games -> TopContent.GameList
        screen is Screen.Systems && preview != null -> TopContent.Preview(preview)
        else -> TopContent.Backdrop
    }

    /** The game's details on a game list while [active]; the usual menu otherwise. */
    fun bottom(screen: Screen, active: Boolean): BottomContent =
        if (active && screen is Screen.Games) BottomContent.Details else BottomContent.Normal
}
