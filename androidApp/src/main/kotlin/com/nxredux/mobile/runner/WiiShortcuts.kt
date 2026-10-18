package com.nxredux.mobile.runner

import android.view.KeyEvent

/**
 * A physical controller's Wii shortcuts. The right-stick click is Home (Dolphin reads R3 as Home on every Wii controller):
 * the app takes it, as it does the pad's HOME, to save a resume point before the game's HOME menu opens. The left-stick
 * click recentres the phone's motion: no Wii controller has a stick click, and Dolphin's own L3 hotkey (sideways toggle)
 * is locked off, so the click never reaches the core. The in-game menu has no Home or Recenter row: the pad and these
 * shortcuts cover both.
 */
object WiiShortcuts {
    fun isRecenter(wii: Boolean, keyCode: Int): Boolean = wii && keyCode == KeyEvent.KEYCODE_BUTTON_THUMBL

    fun isHome(wii: Boolean, keyCode: Int): Boolean = wii && keyCode == KeyEvent.KEYCODE_BUTTON_THUMBR
}
