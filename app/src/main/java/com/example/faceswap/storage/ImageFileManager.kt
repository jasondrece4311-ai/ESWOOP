package com.example.faceswap.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
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

    companion object {
        private const val TAG = "ImageFileManager"
    }

    /**
     * Loads a Bitmap from a content:// URI.
     *
     * This is intentionally robust for Android Photo Picker URIs.
     *
     * Loading strategy:
     *
     * 1. Try ContentResolver.openInputStream()
     * 2. Try ContentResolver.openAssetFileDescriptor()
     * 3. Try ContentResolver.openFileDescriptor()
     * 4. Try MediaStore.getBitmap() as a final fallback
     *
     * The image is downsampled before being loaded into memory.
     */
    suspend fun loadBitmapFromUri(
        uri: Uri,
        maxDimension: Int = 2048
    ): Result<Bitmap> = withContext(Dispatchers.IO) {

        try {
            Log.d(TAG, "Loading image URI: $uri")

            /*
             * Photo Picker normally grants temporary read permission.
             *
             * Some providers support persistable permissions and some do not.
             * Failure here is intentionally ignored because temporary access
             * is sufficient for the current operation.
             */
            tryPersistReadPermission(uri)

            /*
             * -------------------------------------------------------------
             * PASS 1: Read image dimensions.
             * -------------------------------------------------------------
             */

            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            val boundsRead = decodeBoundsWithFallbacks(uri, bounds)

            if (!boundsRead) {
                /*
                 * Some Android media providers don't expose dimensions
                 * correctly through BitmapFactory. Try MediaStore metadata.
                 */
                val mediaDimensions = queryMediaStoreDimensions(uri)

                if (mediaDimensions != null) {
                    bounds.outWidth = mediaDimensions.first
                    bounds.outHeight = mediaDimensions.second
                }
            }

            val originalWidth = bounds.outWidth
            val originalHeight = bounds.outHeight

            Log.d(
                TAG,
                "Image dimensions: ${originalWidth}x$originalHeight"
            )

            if (originalWidth <= 0 || originalHeight <= 0) {
                /*
                 * We couldn't obtain dimensions. Try decoding the image
                 * directly as a final fallback.
                 */
                val directBitmap = decodeDirectBitmap(uri)

                if (directBitmap != null) {
                    val resized = resizeIfNeeded(
                        directBitmap,
                        maxDimension
                    )

                    return@withContext Result.success(
                        applyExifRotation(uri, resized)
                    )
                }

                return@withContext Result.failure(
                    IllegalArgumentException(
                        "Cannot read image dimensions from Uri: $uri"
                    )
                )
            }

            /*
             * -------------------------------------------------------------
             * PASS 2: Calculate safe sampling.
             * -------------------------------------------------------------
             */

            val sampleSize = calculateInSampleSize(
                actualWidth = originalWidth,
                actualHeight = originalHeight,
                reqWidth = maxDimension,
                reqHeight = maxDimension
            )

            val decodeOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            /*
             * -------------------------------------------------------------
             * PASS 3: Decode using multiple provider fallbacks.
             * -------------------------------------------------------------
             */

            var decodedBitmap = decodeBitmapWithFallbacks(
                uri,
                decodeOptions
            )

            /*
             * Final MediaStore fallback.
             */
            if (decodedBitmap == null) {
                Log.w(
                    TAG,
                    "Primary URI decoding failed. Trying MediaStore fallback."
                )

                decodedBitmap = decodeUsingMediaStore(uri)
            }

            if (decodedBitmap == null) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Unable to decode image from Photo Picker Uri: $uri"
                    )
                )
            }

            /*
             * -------------------------------------------------------------
             * PASS 4: Additional memory-safe resize if required.
             * -------------------------------------------------------------
             */

            decodedBitmap = resizeIfNeeded(
                decodedBitmap,
                maxDimension
            )

            /*
             * -------------------------------------------------------------
             * PASS 5: Correct camera/photo EXIF rotation.
             * -------------------------------------------------------------
             */

            decodedBitmap = applyExifRotation(
                uri,
                decodedBitmap
            )

            Log.d(
                TAG,
                "Successfully decoded image: " +
                    "${decodedBitmap.width}x${decodedBitmap.height}"
            )

            Result.success(decodedBitmap)

        } catch (securityException: SecurityException) {

            Log.e(
                TAG,
                "Security exception while reading URI: $uri",
                securityException
            )

            Result.failure(
                SecurityException(
                    "Android denied access to the selected photo. " +
                        "Please select the photo again.",
                    securityException
                )
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to load image URI: $uri",
                e
            )

            Result.failure(e)
        }
    }

    /**
     * Attempts to preserve read access to a Photo Picker URI.
     *
     * Not every Photo Picker provider supports persistable permissions,
     * therefore all failures are intentionally ignored.
     */
    private fun tryPersistReadPermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            Log.d(
                TAG,
                "Persistable read permission granted for: $uri"
            )

        } catch (_: SecurityException) {
            Log.d(
                TAG,
                "URI does not support persistable permission: $uri"
            )
        } catch (_: Exception) {
            Log.d(
                TAG,
                "Could not persist URI permission: $uri"
            )
        }
    }

    /**
     * Attempts to read image bounds using several Android provider APIs.
     */
    private fun decodeBoundsWithFallbacks(
        uri: Uri,
        options: BitmapFactory.Options
    ): Boolean {

        /*
         * -------------------------------------------------------------
         * Method 1: openInputStream
         * -------------------------------------------------------------
         */
        try {
            context.contentResolver
                .openInputStream(uri)
                ?.use { stream ->
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        options
                    )

                    if (
                        options.outWidth > 0 &&
                        options.outHeight > 0
                    ) {
                        return true
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "openInputStream bounds decode failed",
                e
            )
        }

        /*
         * -------------------------------------------------------------
         * Method 2: openAssetFileDescriptor
         * -------------------------------------------------------------
         */
        try {
            context.contentResolver
                .openAssetFileDescriptor(uri, "r")
                ?.use { afd ->

                    afd.createInputStream().use { stream ->
                        BitmapFactory.decodeStream(
                            stream,
                            null,
                            options
                        )
                    }

                    if (
                        options.outWidth > 0 &&
                        options.outHeight > 0
                    ) {
                        return true
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "openAssetFileDescriptor bounds decode failed",
                e
            )
        }

        /*
         * -------------------------------------------------------------
         * Method 3: openFileDescriptor
         * -------------------------------------------------------------
         */
        try {
            context.contentResolver
                .openFileDescriptor(uri, "r")
                ?.use { pfd ->

                    BitmapFactory.decodeFileDescriptor(
                        pfd.fileDescriptor,
                        null,
                        options
                    )

                    if (
                        options.outWidth > 0 &&
                        options.outHeight > 0
                    ) {
                        return true
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "openFileDescriptor bounds decode failed",
                e
            )
        }

        return false
    }

    /**
     * Attempts actual Bitmap decoding using multiple provider APIs.
     */
    private fun decodeBitmapWithFallbacks(
        uri: Uri,
        options: BitmapFactory.Options
    ): Bitmap? {

        /*
         * -------------------------------------------------------------
         * Method 1: openInputStream
         * -------------------------------------------------------------
         */
        try {
            context.contentResolver
                .openInputStream(uri)
                ?.use { stream ->

                    val bitmap = BitmapFactory.decodeStream(
                        stream,
                        null,
                        options
                    )

                    if (bitmap != null) {
                        Log.d(
                            TAG,
                            "Decoded image using openInputStream"
                        )

                        return bitmap
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "openInputStream bitmap decode failed",
                e
            )
        }

        /*
         * -------------------------------------------------------------
         * Method 2: openAssetFileDescriptor
         * -------------------------------------------------------------
         */
        try {
            context.contentResolver
                .openAssetFileDescriptor(uri, "r")
                ?.use { afd ->

                    afd.createInputStream().use { stream ->

                        val bitmap = BitmapFactory.decodeStream(
                            stream,
                            null,
                            options
                        )

                        if (bitmap != null) {
                            Log.d(
                                TAG,
                                "Decoded image using AssetFileDescriptor"
                            )

                            return bitmap
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "AssetFileDescriptor bitmap decode failed",
                e
            )
        }

        /*
         * -------------------------------------------------------------
         * Method 3: openFileDescriptor
         * -------------------------------------------------------------
         */
        try {
            context.contentResolver
                .openFileDescriptor(uri, "r")
                ?.use { pfd ->

                    val bitmap = BitmapFactory.decodeFileDescriptor(
                        pfd.fileDescriptor,
                        null,
                        options
                    )

                    if (bitmap != null) {
                        Log.d(
                            TAG,
                            "Decoded image using ParcelFileDescriptor"
                        )

                        return bitmap
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "ParcelFileDescriptor bitmap decode failed",
                e
            )
        }

        return null
    }

    /**
     * Direct full-resolution decode fallback.
     */
    private fun decodeDirectBitmap(uri: Uri): Bitmap? {

        /*
         * InputStream
         */
        try {
            context.contentResolver
                .openInputStream(uri)
                ?.use { stream ->

                    BitmapFactory.decodeStream(stream)?.let {
                        return it
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Direct InputStream decode failed",
                e
            )
        }

        /*
         * AssetFileDescriptor
         */
        try {
            context.contentResolver
                .openAssetFileDescriptor(uri, "r")
                ?.use { afd ->

                    afd.createInputStream().use { stream ->

                        BitmapFactory.decodeStream(stream)?.let {
                            return it
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Direct AssetFileDescriptor decode failed",
                e
            )
        }

        /*
         * ParcelFileDescriptor
         */
        try {
            context.contentResolver
                .openFileDescriptor(uri, "r")
                ?.use { pfd ->

                    BitmapFactory.decodeFileDescriptor(
                        pfd.fileDescriptor
                    )?.let {
                        return it
                    }
                }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Direct ParcelFileDescriptor decode failed",
                e
            )
        }

        return null
    }

    /**
     * MediaStore fallback.
     *
     * This is mainly useful for image URIs backed by MediaStore.
     */
    @Suppress("DEPRECATION")
    private fun decodeUsingMediaStore(uri: Uri): Bitmap? {

        return try {
            MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                uri
            )
        } catch (e: Exception) {
            Log.w(
                TAG,
                "MediaStore.getBitmap fallback failed",
                e
            )

            null
        }
    }

    /**
     * Attempts to retrieve width and height from MediaStore.
     */
    private fun queryMediaStoreDimensions(
        uri: Uri
    ): Pair<Int, Int>? {

        return try {

            val projection = arrayOf(
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val widthIndex = cursor.getColumnIndex(
                        MediaStore.Images.Media.WIDTH
                    )

                    val heightIndex = cursor.getColumnIndex(
                        MediaStore.Images.Media.HEIGHT
                    )

                    if (
                        widthIndex >= 0 &&
                        heightIndex >= 0
                    ) {

                        val width = cursor.getInt(widthIndex)
                        val height = cursor.getInt(heightIndex)

                        if (width > 0 && height > 0) {
                            return Pair(width, height)
                        }
                    }
                }

                null
            }

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Could not query MediaStore dimensions",
                e
            )

            null
        }
    }

    /**
     * Resize only when the bitmap is larger than maxDimension.
     */
    private fun resizeIfNeeded(
        bitmap: Bitmap,
        maxDimension: Int
    ): Bitmap {

        if (
            bitmap.width <= maxDimension &&
            bitmap.height <= maxDimension
        ) {
            return bitmap
        }

        val scale = minOf(
            maxDimension.toFloat() / bitmap.width,
            maxDimension.toFloat() / bitmap.height
        )

        val newWidth = maxOf(
            1,
            (bitmap.width * scale).toInt()
        )

        val newHeight = maxOf(
            1,
            (bitmap.height * scale).toInt()
        )

        val resized = Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true
        )

        if (resized != bitmap) {
            bitmap.recycle()
        }

        return resized
    }

    /**
     * Corrects image rotation based on EXIF metadata.
     */
    private fun applyExifRotation(
        uri: Uri,
        bitmap: Bitmap
    ): Bitmap {

        val rotation = getExifOrientation(uri)

        if (rotation == 0) {
            return bitmap
        }

        return try {

            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
            }

            val rotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

            if (rotated != bitmap) {
                bitmap.recycle()
            }

            rotated

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Could not apply EXIF rotation",
                e
            )

            bitmap
        }
    }

    /**
     * Reads EXIF orientation.
     *
     * Uses multiple URI access methods because some Photo Picker
     * providers do not expose a normal InputStream.
     */
    private fun getExifOrientation(uri: Uri): Int {

        /*
         * InputStream
         */
        try {
            context.contentResolver
                .openInputStream(uri)
                ?.use { stream ->

                    return readExifRotation(stream)
                }
        } catch (e: Exception) {
            Log.d(
                TAG,
                "InputStream EXIF read failed",
                e
            )
        }

        /*
         * AssetFileDescriptor
         */
        try {
            context.contentResolver
                .openAssetFileDescriptor(uri, "r")
                ?.use { afd ->

                    afd.createInputStream().use { stream ->

                        return readExifRotation(stream)
                    }
                }
        } catch (e: Exception) {
            Log.d(
                TAG,
                "AssetFileDescriptor EXIF read failed",
                e
            )
        }

        return 0
    }

    /**
     * Converts EXIF orientation into degrees.
     */
    private fun readExifRotation(
        stream: InputStream
    ): Int {

        return try {

            val exif = android.media.ExifInterface(stream)

            when (
                exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
            ) {

                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90

                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180

                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270

                else -> 0
            }

        } catch (e: Exception) {

            Log.d(
                TAG,
                "Could not read EXIF orientation",
                e
            )

            0
        }
    }

    /**
     * Saves the processed bitmap to the device's public Pictures
     * directory using MediaStore.
     */
    suspend fun saveBitmapToGallery(
        bitmap: Bitmap,
        quality: QualityLevel = QualityLevel.MAXIMUM
    ): Result<Uri> = withContext(Dispatchers.IO) {

        try {

            val timestamp = SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(Date())

            val filename = "FaceSwap_$timestamp.jpg"

            val compressionQuality = when (quality) {

                QualityLevel.STANDARD -> 85

                QualityLevel.HIGH -> 95

                QualityLevel.MAXIMUM -> 100
            }

            val contentValues = ContentValues().apply {

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    filename
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES +
                            "/FaceSwap"
                    )

                    put(
                        MediaStore.Images.Media.IS_PENDING,
                        1
                    )
                }
            }

            val resolver = context.contentResolver

            val imageUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
                ?: return@withContext Result.failure(
                    IllegalStateException(
                        "Failed to create MediaStore entry"
                    )
                )

            resolver.openOutputStream(imageUri)?.use { output ->

                if (
                    !bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        compressionQuality,
                        output
                    )
                ) {

                    resolver.delete(
                        imageUri,
                        null,
                        null
                    )

                    return@withContext Result.failure(
                        IllegalStateException(
                            "Failed to compress image"
                        )
                    )
                }

            } ?: return@withContext Result.failure(
                IllegalStateException(
                    "Failed to open output stream"
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val completedValues = ContentValues().apply {
                    put(
                        MediaStore.Images.Media.IS_PENDING,
                        0
                    )
                }

                resolver.update(
                    imageUri,
                    completedValues,
                    null,
                    null
                )
            }

            Result.success(imageUri)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to save bitmap",
                e
            )

            Result.failure(e)
        }
    }

    /**
     * Creates a temporary cache file for sharing through FileProvider.
     */
    suspend fun prepareShareableUri(
        bitmap: Bitmap
    ): Result<Uri> = withContext(Dispatchers.IO) {

        try {

            val cacheFolder = File(
                context.cacheDir,
                "images"
            )

            if (!cacheFolder.exists()) {
                cacheFolder.mkdirs()
            }

            val tempFile = File(
                cacheFolder,
                "faceswap_share_${System.currentTimeMillis()}.jpg"
            )

            FileOutputStream(tempFile).use { output ->

                if (
                    !bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        95,
                        output
                    )
                ) {

                    return@withContext Result.failure(
                        IllegalStateException(
                            "Failed to create share image"
                        )
                    )
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            Result.success(uri)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to prepare shareable URI",
                e
            )

            Result.failure(e)
        }
    }

    /**
     * Deletes temporary sharing files.
     */
    suspend fun clearTemporaryFiles() =
        withContext(Dispatchers.IO) {

            try {

                val cacheFolder = File(
                    context.cacheDir,
                    "images"
                )

                if (cacheFolder.exists()) {

                    cacheFolder
                        .listFiles()
                        ?.forEach { file ->
                            file.delete()
                        }
                }

            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "Could not clear temporary files",
                    e
                )
            }
        }

    /**
     * Calculates BitmapFactory's inSampleSize.
     */
    private fun calculateInSampleSize(
        actualWidth: Int,
        actualHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {

        var inSampleSize = 1

        if (
            actualHeight > reqHeight ||
            actualWidth > reqWidth
        ) {

            val halfHeight = actualHeight / 2
            val halfWidth = actualWidth / 2

            while (
                (halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
