package com.example.faceswap.model

enum class QualityLevel(val label: String, val maxDimension: Int, val description: String) {
    STANDARD("Standard (1080p)", 1080, "Fastest generation, balanced memory usage"),
    HIGH("High (2K)", 2048, "Crisp details, ideal for social sharing"),
    MAXIMUM("Maximum (Original)", 4096, "Preserves full camera resolution of Photo B")
}

enum class HardwareMode(val label: String) {
    AUTO("Auto (NNAPI / GPU / CPU)"),
    GPU("GPU Acceleration"),
    CPU("CPU Fallback")
}

data class SwapConfiguration(
    val quality: QualityLevel = QualityLevel.MAXIMUM,
    val hardwareMode: HardwareMode = HardwareMode.AUTO,
    val preservePoseAndGeometry: Boolean = true,
    val matchExposureAndLighting: Boolean = true,
    val matchSkinTone: Boolean = true,
    val seamlessBlendingEnabled: Boolean = true,
    val detailRestorationEnabled: Boolean = true,
    val performQualityValidation: Boolean = true
)
