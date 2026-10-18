package com.nxredux.mobile.scraper

import com.nxredux.mobile.runner.ContentRef

/** The Android games console's tag ([com.nxredux.mobile.ui.ANDROID_TAG]): Tools -> Artwork's Fetch and Reset scope for it. */
const val ANDROID_CONSOLE_TAG = "ANDROID"

/**
 * The Android games console's screenshots, one per app ([com.nxredux.mobile.launcher.InstalledApp.id]), kept in
 * app-private storage (never the home tree: they are not ROM data). Fetched from ScreenScraper by Tools -> Artwork.
 */
interface AndroidArtStore {
    /** [appId]'s stored screenshot, or null when none has been fetched. A quick lookup the console build calls per game. */
    fun screenshot(appId: String): ContentRef?

    /** Stores [png] as [appId]'s screenshot (replacing one); its ref, or null when the write failed. */
    suspend fun writeScreenshot(appId: String, png: ByteArray): ContentRef?

    /** Deletes every stored screenshot (Tools -> Artwork -> Reset of Android or All); how many were deleted. */
    suspend fun deleteAll(): Int
}
