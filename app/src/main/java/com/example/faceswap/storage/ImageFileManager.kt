package com.example.faceswap.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.faceswap.model.QualityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageFileManager(private val context: Context) {

    /**
     * Efficiently decodes a Bitmap from Uri with EXIF rotation correction
     * and memory-safe downsampling.
     */
    suspend fun loadBitmapFromUri(uri: Uri, maxDimension: Int = 2048): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext Result.failure(IllegalArgumentException("Cannot open stream for Uri: $uri"))

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid image dimensions"))
            }

            // Calculate sample size to fit comfortably in memory
            options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            var decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext Result.failure(IllegalStateException("Failed to decode bitmap"))

            // Handle EXIF rotation
            val rotation = getExifOrientation(uri)
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    decodedBitmap, 0, 0,
                    decodedBitmap.width, decodedBitmap.height,
                    matrix, true
                )
                if (rotated != decodedBitmap) {
                    decodedBitmap.recycle()
                    decodedBitmap = rotated
                }
            }

            Result.success(decodedBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves the processed bitmap directly to the device's public Pictures directory via MediaStore.
     * No cloud storage involved.
     */
    suspend fun saveBitmapToGallery(
        bitmap: Bitmap,
        quality: QualityLevel = QualityLevel.MAXIMUM
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "FaceSwap_$timestamp.jpg"

            val compressionQuality = when (quality) {
                QualityLevel.STANDARD -> 85
                QualityLevel.HIGH -> 95
                QualityLevel.MAXIMUM -> 100
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceSwap")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry"))

            resolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, out)
            } ?: return@withContext Result.failure(IllegalStateException("Failed to open output stream"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Result.success(imageUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a temporary cache file for sharing via FileProvider.
     */
    suspend fun prepareShareableUri(bitmap: Bitmap): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val cacheFolder = File(context.cacheDir, "images")
            if (!cacheFolder.exists()) cacheFolder.mkdirs()

            val tempFile = File(cacheFolder, "faceswap_share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cleans up temporary cache files to maintain user privacy and free space.
     */
    suspend fun clearTemporaryFiles() = withContext(Dispatchers.IO) {
        try {
            val cacheFolder = File(context.cacheDir, "images")
            if (cacheFolder.exists()) {
                cacheFolder.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    private fun calculateInSampleSize(
        actualWidth: Int,
        actualHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (actualHeight > reqHeight || actualWidth > reqWidth) {
            val halfHeight = actualHeight / 2
            val halfWidth = actualWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = android.media.ExifInterface(stream)
                when (exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
