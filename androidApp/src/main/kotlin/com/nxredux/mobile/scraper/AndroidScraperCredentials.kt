package com.nxredux.mobile.scraper

import com.nxredux.mobile.BuildConfig
import com.nxredux.mobile.settings.AppSettings

/**
 * ScreenScraper's dev credentials from BuildConfig, XOR-encoded there and decoded once on first use ([ScraperSecret];
 * empty = ScreenScraper off); the user's login and own SteamGridDB / TheGamesDB keys live in [settings].
 */
class AndroidScraperCredentials(private val settings: AppSettings) : ScraperCredentials {
    override val devId: String by lazy { ScraperSecret.decode(BuildConfig.SS_DEV_ID_ENC, BuildConfig.SS_DEV_ID_KEY) }
    override val devPassword: String by lazy { ScraperSecret.decode(BuildConfig.SS_DEV_PASSWORD_ENC, BuildConfig.SS_DEV_PASSWORD_KEY) }
    override var user: String?
        get() = settings.ssUsername.ifEmpty { null }
        set(value) { settings.ssUsername = value ?: "" }
    override var password: String?
        get() = settings.ssPassword
        set(value) { settings.ssPassword = value }
    override var tgdbUserKey: String?
        get() = settings.tgdbApiKey?.ifEmpty { null }
        set(value) { settings.tgdbApiKey = value?.ifEmpty { null } }
    override var sgdbKey: String?
        get() = settings.sgdbApiKey?.ifEmpty { null }
        set(value) { settings.sgdbApiKey = value?.ifEmpty { null } }
}
