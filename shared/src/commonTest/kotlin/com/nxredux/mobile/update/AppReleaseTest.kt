package com.nxredux.mobile.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppReleaseTest {

    private fun releaseJson(
        tag: String = "v0.3.0",
        asset: String? = "NXReduxMobile-v0.3.0.apk",
        digest: String? = "sha256:ABC123",
        prerelease: Boolean = false,
        draft: Boolean = false,
    ): String {
        val body = "A new Minimal layout: just big names, no art.\\r\\n\\r\\nMore details at https://nxredux.com/reference/release-notes/mobile" +
            "\\r\\n\\r\\nSide-load NXReduxMobile-v0.3.0.apk on a phone.\\r\\n\\r\\n**Full Changelog**: https://github.com/x/y/compare/v0.2.0...v0.3.0"
        val digestField = digest?.let { """"digest": "$it",""" } ?: ""
        val assets = asset?.let {
            """{"name": "$it", "size": 1234567, $digestField "browser_download_url": "https://github.com/x/y/releases/download/$tag/$it"}"""
        } ?: ""
        return """{"tag_name": "$tag", "name": "$tag", "body": "$body", "draft": $draft, "prerelease": $prerelease, "assets": [$assets]}"""
    }

    @Test fun a_tag_becomes_a_version_code() {
        assertEquals(3_000L, tagVersionCode("v0.3.0"))
        assertEquals(1_002_003L, tagVersionCode("v1.2.3"))
        assertNull(tagVersionCode("v0.4.0-rc1"))
        assertNull(tagVersionCode("0.3.0"))
        assertNull(tagVersionCode("v1000.0.0"))
    }

    @Test fun the_first_paragraph_is_the_notes() {
        assertEquals("A new Minimal layout.", firstParagraph("A new Minimal layout.\n\nMore details at x"))
        assertEquals("A new Minimal layout.", firstParagraph("A new Minimal layout.\r\n\r\nMore details at x"))
        assertEquals("line one line two", firstParagraph("line one\nline two\n\nnext"))
    }

    @Test fun markdown_is_stripped() {
        assertEquals("Bold and docs and code", firstParagraph("**Bold** and [docs](https://x) and `code`"))
    }

    @Test fun boilerplate_first_paragraphs_give_no_notes() {
        assertEquals("", firstParagraph("Side-load NXReduxMobile-v0.4.0.apk on a phone"))
        assertEquals("", firstParagraph("## What's Changed\n* a thing"))
        assertEquals("", firstParagraph("**Full Changelog**: https://github.com/x/y/compare/a...b"))
        assertEquals("", firstParagraph("  \n\n "))
    }

    @Test fun the_live_release_shape_parses() {
        val release = assertNotNull(parseLatestRelease(releaseJson(), dev = false))
        assertEquals("0.3.0", release.version)
        assertEquals(3_000L, release.versionCode)
        assertEquals("A new Minimal layout: just big names, no art.", release.notes)
        assertEquals("https://github.com/x/y/releases/download/v0.3.0/NXReduxMobile-v0.3.0.apk", release.apkUrl)
        assertEquals(1_234_567L, release.apkSize)
        assertEquals("abc123", release.sha256)
    }

    @Test fun the_dev_flavor_wants_the_dev_asset() {
        assertNull(parseLatestRelease(releaseJson(), dev = true))
        val dev = parseLatestRelease(releaseJson(tag = "v0.3.1", asset = "NXReduxMobile-v0.3.1-dev.apk"), dev = true)
        assertEquals("0.3.1", assertNotNull(dev).version)
        assertNull(parseLatestRelease(releaseJson(tag = "v0.3.1", asset = "NXReduxMobile-v0.3.1-dev.apk"), dev = false))
    }

    @Test fun unusable_releases_give_null() {
        assertNull(parseLatestRelease(releaseJson(asset = null), dev = false))
        assertNull(parseLatestRelease(releaseJson(prerelease = true), dev = false))
        assertNull(parseLatestRelease(releaseJson(draft = true), dev = false))
        assertNull(parseLatestRelease(releaseJson(tag = "v0.4.0-rc1"), dev = false))
        assertNull(parseLatestRelease("not json", dev = false))
        assertNull(parseLatestRelease("{}", dev = false))
    }

    @Test fun a_missing_digest_leaves_the_hash_null() {
        assertNull(assertNotNull(parseLatestRelease(releaseJson(digest = null), dev = false)).sha256)
    }

    @Test fun the_check_runs_daily_and_retries_hourly_after_a_failure() {
        val now = 10_000_000_000L
        assertFalse(UpdatePolicy.checkDue(now, lastSuccess = now - 86_400_000 + 1, lastFailure = 0))
        assertTrue(UpdatePolicy.checkDue(now, lastSuccess = now - 86_400_000, lastFailure = 0))
        assertFalse(UpdatePolicy.checkDue(now, lastSuccess = 0, lastFailure = now - 59 * 60_000L))
        assertTrue(UpdatePolicy.checkDue(now, lastSuccess = 0, lastFailure = now - 60 * 60_000L))
    }

    @Test fun the_popup_wants_a_newer_release_that_was_not_silenced() {
        val release = AppRelease("0.4.0", 4_000, "", "u", 1, null)
        assertTrue(UpdatePolicy.shouldPopup(release, installedCode = 3_000, silenced = null))
        assertFalse(UpdatePolicy.shouldPopup(release, installedCode = 4_000, silenced = null))
        assertFalse(UpdatePolicy.shouldPopup(release, installedCode = 3_000, silenced = "0.4.0"))
        assertTrue(UpdatePolicy.shouldPopup(release.copy(version = "0.4.1", versionCode = 4_001), 3_000, silenced = "0.4.0"))
    }
}
