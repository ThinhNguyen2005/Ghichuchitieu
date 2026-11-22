package com.notepay.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Persists custom Home background photos in app-private storage downsampled for smooth rendering.
 */
object HomeBackgroundStorage {

    private const val BACKGROUND_DIR_NAME = "home_backgrounds"
    private const val MAX_BACKGROUND_DIMENSION_PX = 1440
    private const val JPEG_QUALITY = 88

    suspend fun saveFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeScaledBitmap(context, uri)
                ?: return@withContext Result.failure(IOException("Unable to decode selected image"))

            val dir = File(context.filesDir, BACKGROUND_DIR_NAME).apply {
                if (!exists() && !mkdirs()) {
                    throw IOException("Unable to create background directory")
                }
            }
            val outputFile = File(dir, "bg_${System.currentTimeMillis()}.jpg")

            try {
                outputFile.outputStream().use { stream ->
                    val written = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
                    if (!written) {
                        throw IOException("Unable to encode background image")
                    }
                    stream.flush()
                }
            } finally {
                bitmap.recycle()
            }

            dir.listFiles()?.forEach { existing ->
                if (existing.absolutePath != outputFile.absolutePath) {
                    existing.delete()
                }
            }

            Result.success(outputFile.absolutePath)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomBackground(path: String?) {
        if (path.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                if (file.exists() && file.parentFile?.name == BACKGROUND_DIR_NAME) {
                    file.delete()
                }
            }
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val maxSide = maxOf(width, height)
                if (maxSide > MAX_BACKGROUND_DIMENSION_PX && maxSide > 0) {
                    val scale = MAX_BACKGROUND_DIMENSION_PX.toFloat() / maxSide.toFloat()
                    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
                    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
                    decoder.setTargetSize(targetWidth, targetHeight)
                }
            }
        }

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        var sampleSize = 1
        while (maxOf(rawWidth, rawHeight) / sampleSize > MAX_BACKGROUND_DIMENSION_PX * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val maxSide = maxOf(decoded.width, decoded.height)
        if (maxSide <= MAX_BACKGROUND_DIMENSION_PX) {
            return decoded
        }

        val scale = MAX_BACKGROUND_DIMENSION_PX.toFloat() / maxSide.toFloat()
        val targetWidth = (decoded.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (decoded.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = decoded.scale(targetWidth, targetHeight, filter = true)
        if (scaled !== decoded) {
            decoded.recycle()
        }
        return scaled
    }
}
