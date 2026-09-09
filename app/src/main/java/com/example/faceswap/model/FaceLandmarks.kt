package com.example.faceswap.model

import android.graphics.PointF

/**
 * 2D/3D Facial landmarks representation.
 */
data class FaceLandmarks(
    val leftEye: PointF,
    val rightEye: PointF,
    val noseTip: PointF,
    val mouthLeft: PointF,
    val mouthRight: PointF,
    val contourPoints: List<PointF> = emptyList()
) {
    /**
     * Inter-ocular distance in normalized coordinate space.
     */
    val eyeDistance: Float
        get() {
            val dx = rightEye.x - leftEye.x
            val dy = rightEye.y - leftEye.y
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }
}

/**
 * 3D Pose angles in degrees (Euler angles).
 * Yaw: left-right rotation (-90..+90)
 * Pitch: up-down tilt (-90..+90)
 * Roll: head tilt left/right (-180..+180)
 */
data class PoseEstimate(
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val fovEstimate: Float = 60f
) {
    fun isExtremeAngle(): Boolean {
        return kotlin.math.abs(yaw) > 45f || kotlin.math.abs(pitch) > 35f
    }

    val readableDescription: String
        get() = "Yaw: ${yaw.toInt()}°, Pitch: ${pitch.toInt()}°, Roll: ${roll.toInt()}°"
}
