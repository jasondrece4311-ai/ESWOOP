package com.example.faceswap.model

import android.graphics.Rect
import android.graphics.RectF

/**
 * Normalized bounding box [0.0 .. 1.0] relative to image dimensions.
 */
data class FaceBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f

    fun toPixelRect(imageWidth: Int, imageHeight: Int): Rect {
        return Rect(
            (left * imageWidth).toInt().coerceIn(0, imageWidth),
            (top * imageHeight).toInt().coerceIn(0, imageHeight),
            (right * imageWidth).toInt().coerceIn(0, imageWidth),
            (bottom * imageHeight).toInt().coerceIn(0, imageHeight)
        )
    }

    fun toPixelRectF(imageWidth: Float, imageHeight: Float): RectF {
        return RectF(
            (left * imageWidth).coerceIn(0f, imageWidth),
            (top * imageHeight).coerceIn(0f, imageHeight),
            (right * imageWidth).coerceIn(0f, imageWidth),
            (bottom * imageHeight).coerceIn(0f, imageHeight)
        )
    }
}
