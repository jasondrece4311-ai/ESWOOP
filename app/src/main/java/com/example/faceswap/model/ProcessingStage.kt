package com.example.faceswap.model

import android.graphics.Bitmap

sealed interface ProcessingStage {
    object Idle : ProcessingStage
    object ValidatingInputs : ProcessingStage
    data class DetectingFaces(val photoName: String) : ProcessingStage
    object EstimatingLandmarksAndPose : ProcessingStage
    object AligningFacialGeometry : ProcessingStage
    data class RunningLocalInference(val progress: Float) : ProcessingStage
    object MatchingColorAndLighting : ProcessingStage
    object GeneratingAdaptiveMask : ProcessingStage
    object SeamlessBlending : ProcessingStage
    object RestoringFacialDetails : ProcessingStage
    object ValidatingQuality : ProcessingStage
    object CompositingOriginal : ProcessingStage
    object Completed : ProcessingStage
    data class Failed(val error: FaceSwapError) : ProcessingStage

    val stepIndex: Int
        get() = when (this) {
            is Idle -> 0
            is ValidatingInputs -> 1
            is DetectingFaces -> 2
            is EstimatingLandmarksAndPose -> 3
            is AligningFacialGeometry -> 4
            is RunningLocalInference -> 5
            is MatchingColorAndLighting -> 6
            is GeneratingAdaptiveMask -> 7
            is SeamlessBlending -> 8
            is RestoringFacialDetails -> 9
            is ValidatingQuality -> 10
            is CompositingOriginal -> 11
            is Completed -> 12
            is Failed -> -1
        }

    val totalSteps: Int get() = 12

    val displayTitle: String
        get() = when (this) {
            is Idle -> "Ready"
            is ValidatingInputs -> "Validating input images..."
            is DetectingFaces -> "Detecting faces in $photoName..."
            is EstimatingLandmarksAndPose -> "Estimating 3D head pose & landmarks..."
            is AligningFacialGeometry -> "Aligning facial orientation..."
            is RunningLocalInference -> "Running local AI inference (${(progress * 100).toInt()}%)..."
            is MatchingColorAndLighting -> "Matching skin tone & environmental lighting..."
            is GeneratingAdaptiveMask -> "Computing adaptive soft edge mask..."
            is SeamlessBlending -> "Performing seamless gradient blending..."
            is RestoringFacialDetails -> "Restoring high-frequency skin textures..."
            is ValidatingQuality -> "Running automated quality checks..."
            is CompositingOriginal -> "Compositing back into original Photo B..."
            is Completed -> "Face swap complete!"
            is Failed -> "Processing stopped: ${error.title}"
        }
}

sealed class FaceSwapError(val title: String, val message: String, val suggestion: String) {
    object NoPhotoSelected : FaceSwapError(
        "Photos Missing",
        "Both Photo A (Source Face) and Photo B (Target Photo) must be selected.",
        "Please select both photographs before initiating swap."
    )

    object NoFaceDetectedInSource : FaceSwapError(
        "No Face in Photo A",
        "Could not locate a clear human face in Photo A.",
        "Please select a well-lit photo showing the source face clearly."
    )

    object NoFaceDetectedInTarget : FaceSwapError(
        "No Face in Photo B",
        "Could not locate a target face in Photo B.",
        "Please select a photograph containing a visible person's face."
    )

    data class ExtremePoseAngle(val angle: Float) : FaceSwapError(
        "Extreme Head Angle ($angle°)",
        "The target face is turned too sharply (> 60°) away from the camera.",
        "Choose a target photograph with moderate head rotation (-45° to +45°) for natural integration."
    )

    data class FaceTooSmall(val dimensionPx: Int) : FaceSwapError(
        "Face Too Small ($dimensionPx px)",
        "The detected face is too distant or low-resolution for quality identity transfer.",
        "Crop or select a closer portrait photograph."
    )

    data class ModelNotConfigured(
        val modelName: String,
        val expectedPath: String,
        val license: String
    ) : FaceSwapError(
        "AI Model File Required",
        "The offline AI neural weights ($modelName) have not been placed in the app directory yet.",
        "Expected model file at: $expectedPath. Model license: $license."
    )

    object OutOfMemory : FaceSwapError(
        "Insufficient Memory",
        "Device ran out of memory while allocating full-resolution image buffers.",
        "Try selecting 'Standard' quality in Settings or close other background applications."
    )

    data class Generic(val msg: String) : FaceSwapError(
        "Processing Error",
        msg,
        "Please check your input photos and try again."
    )
}

data class QualityValidationReport(
    val isValid: Boolean = true,
    val alignmentScore: Float = 0.94f,
    val lightingMatchScore: Float = 0.91f,
    val edgeTransitionScore: Float = 0.96f,
    val warnings: List<String> = emptyList()
)

data class SwapResult(
    val originalTargetBitmap: Bitmap,
    val swappedBitmap: Bitmap,
    val processingDurationMs: Long,
    val validationReport: QualityValidationReport = QualityValidationReport(),
    val targetWidth: Int,
    val targetHeight: Int
)
