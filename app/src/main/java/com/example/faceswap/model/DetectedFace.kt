package com.example.faceswap.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * Represents a single detected face in an image.
 */
data class DetectedFace(
    val id: String,
    val index: Int,
    val boundingBox: FaceBoundingBox,
    val landmarks: FaceLandmarks? = null,
    val pose: PoseEstimate = PoseEstimate(),
    val confidence: Float = 0.95f,
    val previewCrop: Bitmap? = null
)

/**
 * Represents a user-selected photo (Source A or Target B)
 */
data class SelectedPhoto(
    val uri: Uri,
    val width: Int = 0,
    val height: Int = 0,
    val detectedFaces: List<DetectedFace> = emptyList(),
    val selectedFaceId: String? = null,
    val isDetecting: Boolean = false,
    val detectionError: String? = null
) {
    val selectedFace: DetectedFace?
        get() = detectedFaces.find { it.id == selectedFaceId } ?: detectedFaces.firstOrNull()
}
