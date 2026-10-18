package com.nxredux.mobile.ui.nx

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DecodeUtils
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.pxOrElse
import com.nxredux.mobile.ui.ABSTRACT_H
import com.nxredux.mobile.ui.ABSTRACT_SCHEME
import com.nxredux.mobile.ui.ABSTRACT_W
import com.nxredux.mobile.ui.AbstractPlan
import com.nxredux.mobile.ui.ICON_SHADOW_ALPHA
import com.nxredux.mobile.ui.PICK_A_GAME_ART_KEY
import com.nxredux.mobile.ui.abstractAccentOf
import com.nxredux.mobile.ui.abstractAppArtOf
import com.nxredux.mobile.ui.accentArtPlan
import com.nxredux.mobile.ui.abstractAppSeed
import com.nxredux.mobile.ui.abstractPlan
import com.nxredux.mobile.ui.abstractSeed
import com.nxredux.mobile.ui.abstractSeedOf
import com.nxredux.mobile.ui.iconPalette
import com.nxredux.mobile.ui.iconPlacement
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File
import kotlin.math.roundToInt

/**
 * The bitmap's scale over the plan's [ABSTRACT_W] × [ABSTRACT_H] (512 × 384): drawn at 1024 × 768, so a full-screen
 * background (the List's, Backdrop's) stays smooth; every slot crops it to fill.
 */
private const val RENDER_SCALE = 2

/**
 * Render locks, striped by file: two slots asking for the same picture at once draw it once, while different pictures
 * (a grid filling in) draw in parallel. A stripe shared by two files only serialises them.
 */
private val renderLocks = Array(16) { Mutex() }

private fun renderLockOf(file: File): Mutex = renderLocks[Math.floorMod(file.path.hashCode(), renderLocks.size)]

/**
 * The cached pictures' encoding: JPEG at the scraper's default quality ([com.nxredux.mobile.scraper.ArtCap]), as they
 * are opaque gradients and shapes: far cheaper to write than a 1024 × 768 PNG, and any decoder reads it.
 */
private const val ART_JPEG_QUALITY = 90
private const val ART_EXT = "jpg"

/**
 * An installed app's version, per package, for [ART_VERSION_TTL_MS]: the keyer asks per tile per frame on the main
 * thread, and a package manager call is a binder round trip. An updated app shows its new icon within the TTL.
 */
private val versionMemo = ExpiringMemo<String, String?>(ART_VERSION_TTL_MS) { SystemClock.elapsedRealtime() }
private const val ART_VERSION_TTL_MS = 30_000L

/** The palette's render of the icon, px square: the full adaptive icon (its layers composited, masked). */
private const val PALETTE_PX = 192

private object AbstractArtFetcherFactory : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (data.scheme != ABSTRACT_SCHEME) return null
        abstractAppArtOf(data.toString())?.let { return AppArtFetcher(options, it.appId, it.icon) }
        abstractAccentOf(data.toString())?.let { return AccentArtFetcher(options, it) }
        val seed = abstractSeedOf(data.toString()) ?: return null
        return AbstractArtFetcher(options, seed)
    }
}

/**
 * The memory cache's key for an app's picture: its URL (the package and activity, and the icon-less form's query, so
 * the two forms never share an entry) and the installed version ([appVersion]), so an updated app's new icon draws a
 * new picture; "none" once uninstalled (the plain picture). Any other model: the default key.
 */
private object AbstractArtKeyer : Keyer<Uri> {
    override fun key(data: Uri, options: Options): String? {
        if (data.scheme != ABSTRACT_SCHEME) return null
        val appId = abstractAppArtOf(data.toString())?.appId ?: return null
        return "$data#" + (cachedAppVersion(options.context, packageOf(appId)) ?: "none")
    }
}

private fun hex(v: Int) = (v.toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

private fun renderSize() = (ABSTRACT_W * RENDER_SCALE).toInt() to (ABSTRACT_H * RENDER_SCALE).toInt()

/**
 * [file], drawn by [draw] the first time (one at a time per file, through a temp file) and handed straight to the
 * loader (scaled down to the request's size when that is smaller), then read as a cached picture.
 */
private suspend fun cachedArt(file: File, options: Options, draw: () -> Bitmap): FetchResult {
    if (file.isFile) return fileResult(file)
    val drawn = renderLockOf(file).withLock {
        if (file.isFile) return@withLock null   // another request drew it while this one waited
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        val bmp = draw()
        try {
            tmp.outputStream().use { out -> check(bmp.compress(Bitmap.CompressFormat.JPEG, ART_JPEG_QUALITY, out)) { "abstract art: encode failed" } }
            if (!tmp.renameTo(file)) error("abstract art: could not keep $file")
        } catch (e: Throwable) { tmp.delete(); bmp.recycle(); throw e }
        bmp
    } ?: return fileResult(file)
    val m = DecodeUtils.computeSizeMultiplier(
        drawn.width, drawn.height,
        options.size.width.pxOrElse { drawn.width }, options.size.height.pxOrElse { drawn.height }, options.scale, options.maxBitmapSize,
    )
    if (m >= 1.0) return ImageFetchResult(drawn.asImage(), isSampled = false, dataSource = DataSource.MEMORY)
    val scaled = Bitmap.createScaledBitmap(drawn, (drawn.width * m).roundToInt().coerceAtLeast(1), (drawn.height * m).roundToInt().coerceAtLeast(1), true)
    if (scaled !== drawn) drawn.recycle()
    return ImageFetchResult(scaled.asImage(), isSampled = true, dataSource = DataSource.MEMORY)
}

private fun fileResult(file: File): FetchResult =
    SourceFetchResult(ImageSource(file.toOkioPath(), FileSystem.SYSTEM), "image/jpeg", DataSource.DISK)

private suspend fun plainArt(options: Options, seed: Int): FetchResult {
    val (w, h) = renderSize()
    return cachedArt(File(options.context.cacheDir, "abstract/${hex(seed)}-${w}x$h.$ART_EXT"), options) { renderAbstract(abstractPlan(seed, w.toDouble(), h.toDouble())) }
}

private class AbstractArtFetcher(private val options: Options, private val seed: Int) : Fetcher {
    override suspend fun fetch(): FetchResult = plainArt(options, seed)
}

/**
 * Home's "Pick a game" picture ([com.nxredux.mobile.ui.abstractAccentArtUrl]): the fixed seed's plan in [rgb]'s palette
 * ([accentArtPlan]: dark, the accent in its shapes), kept per colour (`accent-dark2-<seed>-<rrggbb>`).
 */
private class AccentArtFetcher(private val options: Options, private val rgb: Int) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val seed = abstractSeed(PICK_A_GAME_ART_KEY)
        val (w, h) = renderSize()
        // "dark2": this darkness's files, so a cache drawn by an earlier, lighter one is never shown
        val file = File(options.context.cacheDir, "abstract/accent-dark2-${hex(seed)}-${hex(rgb).takeLast(6)}-${w}x$h.$ART_EXT")
        return cachedArt(file, options) { renderAbstract(accentArtPlan(seed, rgb, w.toDouble(), h.toDouble())) }
    }
}

/**
 * An Android game's picture ([com.nxredux.mobile.ui.abstractAppArtUrl]): its seed's plan in its launcher icon's
 * palette ([iconPalette]) with the icon in the middle ([iconPlacement]), kept per installed version
 * (`app-<seed>-<id hash>-<version>`; an older version's file is dropped). [icon] false (the icon-less form, a list's
 * background): the same palette and plan without the icon, kept apart as `appbg-<seed>-<id hash>-<version>`. The app
 * gone or its icon failing: the seed's plain picture, never an error.
 */
private class AppArtFetcher(private val options: Options, private val appId: String, private val icon: Boolean) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val context = options.context
        val seed = abstractAppSeed(appId)
        val version = cachedAppVersion(context, packageOf(appId)) ?: return plainArt(options, seed)
        val drawable = runCatching { appIcon(context, appId) }.getOrNull() ?: return plainArt(options, seed)
        val (w, h) = renderSize()
        val prefix = (if (icon) "app-" else "appbg-") + "${hex(seed)}-${hex(abstractSeed(appId))}-"
        val file = File(context.cacheDir, "abstract/$prefix$version-${w}x$h.$ART_EXT")
        return try {
            cachedArt(file, options) { renderAppArt(seed, drawable, w, h, withIcon = icon) }.also {
                file.parentFile?.listFiles { f -> f.name.startsWith(prefix) && f.name != file.name }?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            plainArt(options, seed)
        }
    }
}

/** An app id's package: the part before "/" (the id is `package/activity`). */
private fun packageOf(appId: String): String = appId.substringBefore('/')

/** [appVersion] through [versionMemo]. */
private fun cachedAppVersion(context: Context, pkg: String): String? =
    synchronized(versionMemo) { versionMemo.get(pkg) { appVersion(context, it) } }

/** The installed version of [pkg]: its version code and last update time, or null when it is not installed. */
private fun appVersion(context: Context, pkg: String): String? = runCatching {
    val info = context.packageManager.getPackageInfo(pkg, 0)
    @Suppress("DEPRECATION")
    val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    "$code-${info.lastUpdateTime}"
}.getOrNull()

/** The activity's launcher icon (its app's when it has none), as the launcher shows it. */
private fun appIcon(context: Context, appId: String): Drawable {
    val pm = context.packageManager
    val parts = appId.split('/', limit = 2)
    if (parts.size == 2 && parts.all(String::isNotBlank)) {
        try { return pm.getActivityIcon(ComponentName(parts[0], parts[1])) } catch (_: PackageManager.NameNotFoundException) {}
    }
    return pm.getApplicationIcon(parts[0])
}

/** [icon] drawn into a [w] × [h] bitmap. */
private fun drawableBitmap(icon: Drawable, w: Int, h: Int): Bitmap =
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { icon.setBounds(0, 0, w, h); icon.draw(Canvas(it)) }

/**
 * The seed's plan in [icon]'s palette (the plain one when it has no opaque pixel), the icon centred with a soft shadow;
 * [withIcon] false: the palette's plan alone.
 */
private fun renderAppArt(seed: Int, icon: Drawable, w: Int, h: Int, withIcon: Boolean = true): Bitmap {
    val small = drawableBitmap(icon, PALETTE_PX, PALETTE_PX)
    val pixels = IntArray(PALETTE_PX * PALETTE_PX).also { small.getPixels(it, 0, PALETTE_PX, 0, 0, PALETTE_PX, PALETTE_PX) }
    small.recycle()
    val bmp = renderAbstract(abstractPlan(seed, w.toDouble(), h.toDouble(), iconPalette(pixels)))
    if (!withIcon) return bmp
    val iw = icon.intrinsicWidth.takeIf { it > 0 } ?: 1
    val ih = icon.intrinsicHeight.takeIf { it > 0 } ?: 1
    val at = iconPlacement(w.toDouble(), h.toDouble(), iw.toDouble(), ih.toDouble())
    val art = drawableBitmap(icon, at.width.toInt().coerceAtLeast(1), at.height.toInt().coerceAtLeast(1))
    val canvas = Canvas(bmp)
    val offset = IntArray(2)
    val shadow = art.extractAlpha(Paint().apply { maskFilter = BlurMaskFilter(at.shadowBlur.toFloat(), BlurMaskFilter.Blur.NORMAL) }, offset)
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ((ICON_SHADOW_ALPHA * 255).toInt() shl 24) }
    canvas.drawBitmap(shadow, (at.left + offset[0]).toFloat(), (at.top + at.shadowDy + offset[1]).toFloat(), shadowPaint)
    canvas.drawBitmap(art, at.left.toFloat(), at.top.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    shadow.recycle(); art.recycle()
    return bmp
}

/** [plan] on an ARGB bitmap: the linear ground through its stops, then each shape turned about its origin, anti-aliased. */
private fun renderAbstract(plan: AbstractPlan): Bitmap {
    val bmp = Bitmap.createBitmap(plan.w.toInt(), plan.h.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val (x0, y0, x1, y1) = plan.groundLine()
    val n = plan.stops.size
    val ground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(),
            IntArray(n) { plan.stops[it].argb() }, FloatArray(n) { it / (n - 1f) }, Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, plan.w.toFloat(), plan.h.toFloat(), ground)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    for (s in plan.shapes) {
        paint.color = s.color.argb()
        canvas.save()
        canvas.translate(s.x.toFloat(), s.y.toFloat())
        canvas.rotate(s.rotation.toFloat())
        val hl = (s.length / 2).toFloat()
        val ht = (s.thickness / 2).toFloat()
        when (s.kind) {
            0 -> canvas.drawCircle(0f, 0f, s.radius.toFloat(), paint)
            1 -> canvas.drawRoundRect(-hl, -ht, hl, ht, s.corner.toFloat(), s.corner.toFloat(), paint)
            else -> canvas.drawRect(-hl, -ht, hl, ht, paint)
        }
        canvas.restore()
    }
    return bmp
}

internal actual fun abstractArtFetcher(): Fetcher.Factory<Uri> = AbstractArtFetcherFactory

internal actual fun abstractArtKeyer(): Keyer<Uri>? = AbstractArtKeyer
