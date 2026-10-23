package com.nxredux.mobile.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The newest published release, as the updater needs it. [version]: the tag without its `v` ("0.4.0"); [versionCode]: what
 * the build stamps for that tag ([tagVersionCode]); [notes]: the release body's first paragraph, "" when it has none;
 * [sha256]: the APK's lowercase hex digest, null when GitHub gave none (or not a `sha256:` one).
 */
data class AppRelease(
    val version: String,
    val versionCode: Long,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String?,
)

@Serializable
private data class ReleaseDto(
    val tag_name: String = "",
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
private data class AssetDto(
    val name: String = "",
    val size: Long = 0,
    val digest: String? = null,
    val browser_download_url: String = "",
)

private val lenientJson = Json { ignoreUnknownKeys = true }
private val TAG = Regex("""^v(\d+)\.(\d+)\.(\d+)$""")
private val RELEASE_ASSET = Regex("""^NXReduxMobile-v\d+\.\d+\.\d+\.apk$""")
private val DEV_ASSET = Regex("""^NXReduxMobile-.+-dev\.apk$""")

/** `vMAJOR.MINOR.PATCH` as the build's version code, `MAJOR * 1,000,000 + MINOR * 1,000 + PATCH`; null for any other tag. */
fun tagVersionCode(tag: String): Long? {
    val (major, minor, patch) = TAG.matchEntire(tag)?.destructured ?: return null
    val parts = listOf(major, minor, patch).map { it.toIntOrNull() ?: return null }
    if (parts.any { it > 999 }) return null
    return parts[0] * 1_000_000L + parts[1] * 1_000L + parts[2]
}

private val BOLD = Regex("""\*\*(.+?)\*\*""")
private val LINK = Regex("""\[([^\]]*)]\([^)]*\)""")
private val CODE = Regex("""`([^`]*)`""")

/**
 * The first paragraph of a release body as plain text: lines joined with a space, bold, links and code unwrapped. "" when
 * the body is blank or opens with boilerplate the release workflow adds (a heading, the side-load line, the changelog link).
 */
fun firstParagraph(body: String): String {
    val paragraph = body.replace("\r\n", "\n").trim().split("\n\n").first().trim()
    if (paragraph.startsWith("#") || paragraph.startsWith("Side-load") || paragraph.startsWith("**Full Changelog**")) return ""
    return paragraph.lines().joinToString(" ") { it.trim() }.replace(BOLD, "$1").replace(LINK, "$1").replace(CODE, "$1")
}

/** What GitHub's `releases/latest` answer holds for this build. */
sealed interface LatestRelease {
    data class Found(val release: AppRelease) : LatestRelease
    /** A published `vX.Y.Z` release with no APK for this flavor (the dev build against the real feed): nothing to install. */
    data object NoAsset : LatestRelease
    /** Bad JSON (a portal page), a draft or prerelease, or a tag that is not `vX.Y.Z`: the check failed. */
    data object Unusable : LatestRelease
}

/** Reads GitHub's `releases/latest` answer. [dev]: the dev flavor, which installs the `-dev` APK instead of the release one. */
fun readLatestRelease(json: String, dev: Boolean): LatestRelease {
    val dto = runCatching { lenientJson.decodeFromString<ReleaseDto>(json) }.getOrNull() ?: return LatestRelease.Unusable
    if (dto.draft || dto.prerelease) return LatestRelease.Unusable
    val code = tagVersionCode(dto.tag_name) ?: return LatestRelease.Unusable
    val pattern = if (dev) DEV_ASSET else RELEASE_ASSET
    val asset = dto.assets.firstOrNull { pattern.matches(it.name) && it.browser_download_url.isNotEmpty() } ?: return LatestRelease.NoAsset
    return LatestRelease.Found(
        AppRelease(
            version = dto.tag_name.removePrefix("v"),
            versionCode = code,
            notes = firstParagraph(dto.body.orEmpty()),
            apkUrl = asset.browser_download_url,
            apkSize = asset.size,
            // Another algorithm's digest can't be compared: null leaves the check to the APK signature.
            sha256 = asset.digest?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")?.lowercase()?.takeIf { it.isNotEmpty() },
        ),
    )
}

/** The release [readLatestRelease] found, or null when there is none to install. */
fun parseLatestRelease(json: String, dev: Boolean): AppRelease? = (readLatestRelease(json, dev) as? LatestRelease.Found)?.release

/** When the updater checks and when it interrupts. */
object UpdatePolicy {
    const val CHECK_INTERVAL_MS = 86_400_000L
    const val RETRY_MS = 3_600_000L

    /**
     * A check is due a day after the last success, and an hour after the last failure. Times are epoch millis, 0 for never;
     * a stored time in the future (the clock was set back) counts as due, or the check would wait until the clock caught up.
     */
    fun checkDue(now: Long, lastSuccess: Long, lastFailure: Long): Boolean =
        (lastSuccess > now || now - lastSuccess >= CHECK_INTERVAL_MS) && (lastFailure > now || now - lastFailure >= RETRY_MS)

    /** The popup is for a release newer than the install that the user has not silenced ([silenced]: its version). */
    fun shouldPopup(release: AppRelease, installedCode: Long, silenced: String?): Boolean =
        release.versionCode > installedCode && release.version != silenced
}
