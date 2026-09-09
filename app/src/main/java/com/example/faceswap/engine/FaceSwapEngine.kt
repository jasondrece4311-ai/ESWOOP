package com.example.faceswap.engine

import android.graphics.Bitmap
import com.example.faceswap.model.DetectedFace
import com.example.faceswap.model.FaceLandmarks
import com.example.faceswap.model.PoseEstimate
import com.example.faceswap.model.ProcessingStage
import com.example.faceswap.model.SwapConfiguration
import com.example.faceswap.model.SwapResult

/**
 * Clean architectural abstraction for local, completely offline on-device
 * face detection, pose estimation, alignment, and neural face-swapping.
 *
 * This decouples the UI and application lifecycle from the underlying inference engine
 * (ONNX Runtime, TFLite, MediaPipe, etc.) allowing modular model swapping.
 */
interface FaceSwapEngine {

    /**
     * Initializes engine resources, parses model graph, and allocates tensor memory.
     * @return Result.success if model is found and loaded, or Result.failure with status.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Detects all human faces in the given bitmap without cloud calls.
     * Returns normalized bounding boxes, confidence, and preview crops.
     */
    suspend fun detectFaces(bitmap: Bitmap): Result<List<DetectedFace>>

    /**
     * Computes 3D head pose (yaw, pitch, roll) and landmark geometry.
     */
    suspend fun estimateLandmarksAndPose(bitmap: Bitmap, face: DetectedFace): Result<Pair<FaceLandmarks, PoseEstimate>>

    /**
     * Executes the full on-device pipeline:
     * 1. Source Identity Representation
     * 2. Target Geometry & Pose Adaptation
     * 3. AI Model Neural Inference
     * 4. Adaptive Soft Mask Generation
     * 5. Color, Exposure, and Ambient Lighting Matching
     * 6. Seamless Poisson/Laplacian Gradient Blending
     * 7. Detail Restoration
     * 8. High-Resolution Compositing into Photo B
     */
    suspend fun swapFace(
        sourceBitmap: Bitmap,
        sourceFace: DetectedFace,
        targetBitmap: Bitmap,
        targetFace: DetectedFace,
        config: SwapConfiguration,
        onProgress: (stage: ProcessingStage) -> Unit
    ): Result<SwapResult>

    /**
     * Releases active native tensors, delegates, and execution providers.
     */
    fun release()

    /**
     * Queries current model availability and configuration status.
     */
    fun isModelReady(): Boolean

    /**
     * Returns details on the required model file and location.
     */
    fun getModelSpecification(): ModelSpecification
}
