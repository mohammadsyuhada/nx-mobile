package com.nxredux.mobile

/**
 * Every foreground service's notification id, in one place. Android keeps one notification record per (package, id):
 * two services sharing an id overwrite each other's text and progress, and the first to stop removes the other's
 * notification (and its Pause / Cancel actions) while it still runs in the foreground. A new service takes the next
 * free id here, never a private constant of its own.
 */
object NotificationIds {
    /** Artwork scraping ([com.nxredux.mobile.scraper.ScrapeService]). */
    const val SCRAPE = 4101

    /** RetroAchievements "Download all game data" ([com.nxredux.mobile.achievements.RaDownloadService]). */
    const val RA_DOWNLOAD = 4102

    /** Device Sync ([com.nxredux.mobile.sync.DeviceSyncService]). */
    const val DEVICE_SYNC = 4103

    /** The in-app update's download and install wait (the github flavor's com.nxredux.mobile.update.AppUpdateService). */
    const val APP_UPDATE = 4104

    /** All of them, for the test that keeps them distinct. */
    val all: List<Int> = listOf(SCRAPE, RA_DOWNLOAD, DEVICE_SYNC, APP_UPDATE)
}
