package com.example.faceswap.engine

import android.graphics.Bitmap
import com.example.faceswap.model.DetectedFace
import com.example.faceswap.model.FaceBoundingBox
import com.example.faceswap.model.PoseEstimate
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real on-device face detector using Google ML Kit.
 *
 * The ML Kit bundled model runs locally on the device.
 * No photo is uploaded to a server by this class.
 */
class LocalFaceDetector {

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(
                FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
            )
            .setLandmarkMode(
                FaceDetectorOptions.LANDMARK_MODE_ALL
            )
            .setClassificationMode(
                FaceDetectorOptions.CLASSIFICATION_MODE_ALL
            )
            .build()

        FaceDetection.getClient(options)
    }

    suspend fun detectFaces(bitmap: Bitmap): Result<List<DetectedFace>> =
        withContext(Dispatchers.Default) {
            try {
                if (bitmap.isRecycled) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Bitmap has been recycled")
                    )
                }

                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Invalid bitmap dimensions")
                    )
                }

                val image = InputImage.fromBitmap(bitmap, 0)

                val faces: List<Face> = Tasks.await(
                    detector.process(image)
                )

                val detectedFaces = faces.mapIndexed { index, face ->
                    convertFace(
                        face = face,
                        index = index,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height
                    )
                }

                Result.success(detectedFaces)
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(
                        "ML Kit face detection failed: ${e.message}",
                        e
                    )
                )
            }
        }

    private fun convertFace(
        face: Face,
        index: Int,
        imageWidth: Int,
        imageHeight: Int
    ): DetectedFace {

        val box = face.boundingBox

        val left = normalize(box.left, imageWidth)
        val top = normalize(box.top, imageHeight)
        val right = normalize(box.right, imageWidth)
        val bottom = normalize(box.bottom, imageHeight)

        return DetectedFace(
            id = "face_$index",
            index = index,
            boundingBox = FaceBoundingBox(
                left = left,
                top = top,
                right = right,
                bottom = bottom
            ),
            pose = PoseEstimate(
                yaw = face.headEulerAngleY,
                pitch = face.headEulerAngleX,
                roll = face.headEulerAngleZ
            ),
            confidence = 1.0f
        )
    }

    private fun normalize(
        value: Int,
        dimension: Int
    ): Float {
        if (dimension <= 0) return 0f

        return (value.toFloat() / dimension.toFloat())
            .coerceIn(0f, 1f)
    }

    fun close() {
        detector.close()
    }
}
