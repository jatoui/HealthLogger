package com.jadatoui.healthlogger.net

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Label text stays sharp at ~1600px on the long edge; keeps uploads around 200-400 KB. */
private const val MAX_EDGE_PX = 1600
private const val JPEG_QUALITY = 85
private const val PHOTO_DIR = "label_photos"

/** Helpers for the "scan a label" flow: camera target files and upload-ready JPEGs. */
object LabelPhoto {

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    /** A fresh cache-backed Uri for the system camera to write into. */
    fun newCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, PHOTO_DIR).apply { mkdirs() }
        val file = File.createTempFile("label_", ".jpg", dir)
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    /**
     * Downscales, fixes EXIF rotation and JPEG-encodes [uri]. Deletes the source afterwards
     * if it was one of our own camera temp files (gallery Uris aren't ours to delete).
     */
    suspend fun loadForUpload(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        try {
            encode(context, uri)
        } finally {
            if (uri.authority == authority(context)) {
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
    }

    private fun encode(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver

        // Read bounds first so we never decode a full 12MP image into memory.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE_PX) sample *= 2

        val decoded = resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Could not decode image")

        // Camera photos are often stored sideways; rotating helps the model read the label.
        val rotation = resolver.openInputStream(uri).use { stream ->
            if (stream == null) 0 else when (
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }

        val scale = MAX_EDGE_PX.toFloat() / maxOf(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val final = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        }

        return ByteArrayOutputStream().use { out ->
            final.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }
}
