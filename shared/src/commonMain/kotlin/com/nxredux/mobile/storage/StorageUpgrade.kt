package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.settings.ExtraFolder

/**
 * The platform side of the one-time storage upgrade (installs from v0.3.0 and earlier, whose saves were in app-private
 * storage). Remove with the upgrade screen after v1.0.
 */
interface StorageUpgradePlatform {
    /** The private data has moved (or there was none): the upgrade is not due. */
    fun isDone(): Boolean

    /**
     * Makes the old picker (SAF) home [old] an extra ROM folder on its `Roms/` folder, read in place: `Bios/` and
     * `Collections/` are copied into [into] (null: the app's media home; existing files are kept), and every key of a game
     * under the old `Roms/` (recents, shortcuts, the hidden list, the copied collections, game time) becomes its extra
     * key. Runs before [moveInto], on the private data. Returns the extra to append; null when the old home is gone for
     * good (its grant or folder no longer there: nothing to adopt, nothing changed). Throws when it cannot be read now
     * (its card out, a provider error): the upgrade then waits for a later start.
     */
    suspend fun adoptPickerHome(old: ContentRef, into: ContentRef? = null): ExtraFolder?

    /** Moves the private data into [home]; false when the move is not complete (the data stays private, nothing is marked). */
    suspend fun moveInto(home: ContentRef): Boolean

    /** The upgrade is done: it is never due again. */
    fun markDone()

    /** A new install finished setup: [markDone] only when the private root holds nothing to move (else the gate or the quiet move decides). */
    fun markIfEmpty()

    /** Starts the quiet move ([UpgradeCase.QUIET]) when it can run; returns at once. Never while a game or Device Sync uses the data. */
    fun startQuietMove()

    /** The quiet move failed on several starts and the data is still in the app: the library says so. */
    fun quietMoveStuck(): Boolean

    /**
     * A file home's upgrader without All files access answered Not now ([setWaiting]): the screen is not shown again, the
     * data waits in the app, and the quiet move runs once access is back.
     */
    fun isWaiting(): Boolean
    fun setWaiting()

    /**
     * The extra [extraId] (an adopted old home's `Roms/`) is the home's own `Roms/` again: its games' keys
     * (`/Extra/<extraId>/...`) become home keys (`/Roms/...`) in the recents, shortcuts, hidden list and game time.
     * False when the game time database can't be opened (nothing then changed there).
     */
    suspend fun restoreHomeKeys(extraId: String): Boolean

    /**
     * Copies the data (saves, states, settings, cheats) of the home [from] into the home [to] by the move's clash rule
     * (newer wins, the other kept as a backup in [to]); nothing in [from] is deleted. False when a copy failed.
     */
    suspend fun copySaves(from: ContentRef, to: ContentRef): Boolean
}

/**
 * How a due upgrade runs: [QUIET], in the background (Case A, or waiting in the app until access is back); [ASK], the
 * upgrade screen (Case B); [ASK_ACCESS], the screen's first page for a file home without All files access, whose
 * Not now waits instead of moving anything to the media home.
 */
enum class UpgradeCase { QUIET, ASK, ASK_ACCESS }

object StorageUpgradeGate {
    /**
     * Whether the upgrade is due: only after onboarding ([onboardingDone] true; a new install marks it done when it
     * finishes) and before it is [done]. All files access [granted] with a file-path home ([saved]) moves quietly; a file
     * home without access asks for it once ([ASK_ACCESS][UpgradeCase.ASK_ACCESS]; [waiting] after its Not now: quiet);
     * any other home (a picker home, none) asks. Null when not due.
     */
    fun due(onboardingDone: Boolean?, done: Boolean, granted: Boolean, saved: ContentRef?, waiting: Boolean = false): UpgradeCase? = when {
        onboardingDone != true || done -> null
        saved != null && FileRefs.isFile(saved) -> when {
            granted || waiting -> UpgradeCase.QUIET
            else -> UpgradeCase.ASK_ACCESS
        }
        else -> UpgradeCase.ASK
    }
}
