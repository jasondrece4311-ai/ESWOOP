package com.example.faceswap.engine

/**
 * Detailed specification of the offline on-device AI model architecture
 * and requirements for local execution.
 */
data class ModelSpecification(
    val name: String,
    val format: String,
    val recommendedFileName: String,
    val expectedAssetPath: String,
    val licenseName: String,
    val licenseUrl: String,
    val isRedistributionAllowed: Boolean,
    val isCommercialUseAllowed: Boolean,
    val inputResolution: String,
    val framework: String,
    val description: String
) {
    companion object {
        /**
         * Primary Recommended Model: MobileFaceSwap
         * Highly optimized lightweight architecture designed specifically for mobile edge devices.
         * Permissive Apache 2.0 license allows redistribution and commercial use.
         */
        val MOBILE_FACE_SWAP = ModelSpecification(
            name = "MobileFaceSwap (PaddleMobile / ONNX)",
            format = "ONNX / TFLite (FP16 or INT8 Quantized)",
            recommendedFileName = "mobile_faceswap.onnx",
            expectedAssetPath = "models/mobile_faceswap.onnx",
            licenseName = "Apache 2.0 License",
            licenseUrl = "https://github.com/Seanseattle/MobileFaceSwap",
            isRedistributionAllowed = true,
            isCommercialUseAllowed = true,
            inputResolution = "256x256 RGB",
            framework = "ONNX Runtime Mobile / TensorFlow Lite",
            description = "Lightweight ID-injection architecture suitable for real-time mobile execution without server dependencies."
        )

        /**
         * Alternative Research Model: InsightFace INSwapper (128)
         * State-of-the-art identity transfer, strictly Non-Commercial / Research only.
         */
        val INSWAPPER_128 = ModelSpecification(
            name = "InsightFace Inswapper (inswapper_128.onnx)",
            format = "ONNX (FP32/FP16)",
            recommendedFileName = "inswapper_128.onnx",
            expectedAssetPath = "models/inswapper_128.onnx",
            licenseName = "InsightFace Non-Commercial Research License",
            licenseUrl = "https://github.com/deepinsight/insightface",
            isRedistributionAllowed = false,
            isCommercialUseAllowed = false,
            inputResolution = "128x128 RGB with 512-d ArcFace embedding",
            framework = "ONNX Runtime Mobile",
            description = "High-accuracy face-swapping model. Requires explicit non-commercial research attribution."
        )

        /**
         * Detection & Landmarks Model: MediaPipe / BlazeFace
         */
        val BLAZE_FACE = ModelSpecification(
            name = "BlazeFace / MediaPipe Face Landmark (Short-Range)",
            format = "TFLite",
            recommendedFileName = "face_landmarker.task",
            expectedAssetPath = "models/face_landmarker.task",
            licenseName = "Apache 2.0 License",
            licenseUrl = "https://developers.google.com/mediapipe",
            isRedistributionAllowed = true,
            isCommercialUseAllowed = true,
            inputResolution = "192x192 / 256x256 RGB",
            framework = "MediaPipe / TensorFlow Lite",
            description = "High-speed 468-point 3D landmark mesh and head pose angle estimator."
        )
    }
}
