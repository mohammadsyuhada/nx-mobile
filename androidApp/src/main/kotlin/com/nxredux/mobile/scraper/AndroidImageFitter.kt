package com.nxredux.mobile.scraper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The pure numbers the fitter hands to android.graphics (unit-tested; the Bitmap work needs a device). */
internal object FitterGeometry {
    /**
     * The largest power-of-two decode sample that keeps a [w]x[h] image at least [minW]x[minH] (the fitted size), so a
     * huge download is decoded small and only then scaled exactly; never below what the cap needs.
     */
    fun sampleSize(w: Int, h: Int, minW: Int, minH: Int): Int {
        var s = 1
        while (w / (s * 2) >= minW && h / (s * 2) >= minH) s *= 2
        return s
    }
}

/**
 * [ImageFitter] and the mix's [PixelCodec] on android.graphics: bounds first; a download within its cap and already in
 * its target encoding is written as downloaded ([ArtFit.passThrough], no re-encode); otherwise decoded with a
 * power-of-two sample, scaled to [ArtFit.size] and encoded per [ArtFit.encoding] (alpha from the decoded bitmap). An
 * image with alpha encoded as JPEG (grid, hero) is first drawn over opaque black: JPEG has no alpha, and black is what
 * the dimmed background those slots are drawn as shows anyway, so transparent parts come out black on purpose, with
 * semi-transparent edges blended rather than left to the encoder. An undecodable download, a failed encode or running
 * out of memory is null. Every bitmap is recycled before returning. The codec decodes to straight-alpha ARGB
 * (`getPixels` unpremultiplies) and encodes a full-colour RGBA PNG.
 */
class AndroidImageFitter : ImageFitter, PixelCodec {
    override suspend fun decode(image: ByteArray): ArgbImage? = withContext(Dispatchers.Default) {
        safely {
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val b = BitmapFactory.decodeByteArray(image, 0, image.size, opts)?.takeIf { it.width > 0 && it.height > 0 } ?: return@safely null
            try {
                val argb = if (b.config == Bitmap.Config.ARGB_8888) b else b.copy(Bitmap.Config.ARGB_8888, false) ?: return@safely null
                try {
                    ArgbImage(argb.width, argb.height).also { argb.getPixels(it.pixels, 0, argb.width, 0, 0, argb.width, argb.height) }
                } finally {
                    if (argb !== b) argb.recycle()
                }
            } finally {
                b.recycle()
            }
        }
    }

    override suspend fun encodePng(image: ArgbImage): ByteArray? = withContext(Dispatchers.Default) {
        safely {
            val b = Bitmap.createBitmap(image.pixels, image.width, image.height, Bitmap.Config.ARGB_8888)
            try {
                val out = ByteArrayOutputStream()
                if (b.compress(Bitmap.CompressFormat.PNG, 100, out)) out.toByteArray() else null
            } finally {
                b.recycle()
            }
        }
    }

    override suspend fun fit(image: ByteArray, cap: ArtCap): FittedImage? = withContext(Dispatchers.Default) {
        safely { fitNow(image, cap) }
    }

    private fun fitNow(image: ByteArray, cap: ArtCap): FittedImage? {
        if (image.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(image, 0, image.size, bounds)
        val w = bounds.outWidth; val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null
        val type = imageTypeOf(image)
        // A PNG keeps PNG and a JPEG (never alpha) keeps JPEG unless the cap forces one: alpha does not change the pass.
        if (ArtFit.passThrough(type, w, h, hasAlpha = type != ImageType.JPEG, cap)) {
            return FittedImage(image, if (type == ImageType.JPEG) ArtEncoding.JPEG else ArtEncoding.PNG)
        }
        val (fw, fh) = ArtFit.size(w, h, cap)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = FitterGeometry.sampleSize(w, h, fw, fh)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(image, 0, image.size, opts)?.takeIf { it.width > 0 && it.height > 0 } ?: return null
        try {
            val scaled = if (decoded.width == fw && decoded.height == fh) decoded else Bitmap.createScaledBitmap(decoded, fw, fh, true)
            try {
                val encoding = ArtFit.encoding(type, scaled.hasAlpha(), cap)
                val out = ByteArrayOutputStream()
                val ok = when (encoding) {
                    ArtEncoding.PNG -> scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                    ArtEncoding.JPEG -> if (!scaled.hasAlpha()) scaled.compress(Bitmap.CompressFormat.JPEG, cap.jpegQuality, out) else {
                        val opaque = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
                        try {
                            Canvas(opaque).apply { drawColor(Color.BLACK); drawBitmap(scaled, 0f, 0f, null) }
                            opaque.setHasAlpha(false)
                            opaque.compress(Bitmap.CompressFormat.JPEG, cap.jpegQuality, out)
                        } finally {
                            opaque.recycle()
                        }
                    }
                }
                return if (ok) FittedImage(out.toByteArray(), encoding) else null
            } finally {
                if (scaled !== decoded) scaled.recycle()
            }
        } finally {
            decoded.recycle()
        }
    }

    /** A decode, scale or encode that fails (including running out of memory on a huge download) produces nothing. */
    private inline fun <T> safely(block: () -> T?): T? = try {
        block()
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}
