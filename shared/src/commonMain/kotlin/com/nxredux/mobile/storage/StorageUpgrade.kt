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
}

/** How a due upgrade runs: [QUIET], in the background (Case A); [ASK], the upgrade screen (Case B). */
enum class UpgradeCase { QUIET, ASK }

object StorageUpgradeGate {
    /**
     * Whether the upgrade is due: only after onboarding ([onboardingDone] true; a new install marks it done when it
     * finishes) and before it is [done]. All files access [granted] with a file-path home ([saved]) moves quietly; any
     * other home (a picker home, a file home without access, none) asks. Null when not due.
     */
    fun due(onboardingDone: Boolean?, done: Boolean, granted: Boolean, saved: ContentRef?): UpgradeCase? = when {
        onboardingDone != true || done -> null
        granted && saved != null && FileRefs.isFile(saved) -> UpgradeCase.QUIET
        else -> UpgradeCase.ASK
    }
}
