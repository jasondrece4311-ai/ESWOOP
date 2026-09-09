package com.example.faceswap.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.faceswap.model.ProcessingStage
import com.example.faceswap.ui.theme.Cyan400
import com.example.faceswap.ui.theme.Emerald500
import com.example.faceswap.ui.theme.Indigo400
import com.example.faceswap.ui.theme.Slate700
import com.example.faceswap.ui.theme.Slate800
import com.example.faceswap.ui.theme.Slate900

@Composable
fun ProcessingOverlay(
    stage: ProcessingStage,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { /* Prevent dismiss during inference */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Indigo400.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("processing_overlay_dialog"),
            color = Slate900,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with CPU/NPU pulse icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Indigo400.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Indigo400,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "Local AI Processing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stage.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Cyan400,
                    fontWeight = FontWeight.Medium
                )

                // Progress Bar
                val progressFraction = when (stage) {
                    is ProcessingStage.Idle -> 0f
                    is ProcessingStage.Completed -> 1f
                    is ProcessingStage.Failed -> 0f
                    is ProcessingStage.RunningLocalInference -> 0.40f + (stage.progress * 0.25f)
                    else -> (stage.stepIndex.toFloat() / stage.totalSteps.toFloat()).coerceIn(0.05f, 0.95f)
                }
                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    label = "progress_animation"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Indigo400,
                        trackColor = Slate800
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Step ${stage.stepIndex} of ${stage.totalSteps}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan400
                        )
                    }
                }

                // Stage Checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate800.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PipelineStepItem("Input Validation & Landmark Detection", isCompleted = stage.stepIndex > 3, isCurrent = stage.stepIndex in 1..3)
                    PipelineStepItem("3D Pose & Facial Angle Alignment", isCompleted = stage.stepIndex > 4, isCurrent = stage.stepIndex == 4)
                    PipelineStepItem("Local On-Device AI Neural Inference", isCompleted = stage.stepIndex > 5, isCurrent = stage.stepIndex == 5)
                    PipelineStepItem("Lighting, Exposure & Skin Tone Match", isCompleted = stage.stepIndex > 6, isCurrent = stage.stepIndex == 6)
                    PipelineStepItem("Adaptive Soft-Mask Seamless Blending", isCompleted = stage.stepIndex > 8, isCurrent = stage.stepIndex in 7..8)
                    PipelineStepItem("High-Resolution Detail Reconstruction", isCompleted = stage.stepIndex > 10, isCurrent = stage.stepIndex in 9..11)
                }

                // Privacy Indicator
                Text(
                    text = "🔒 100% On-Device · Zero Network Calls",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Processing")
                }
            }
        }
    }
}

@Composable
private fun PipelineStepItem(
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            isCompleted -> {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Emerald500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            isCurrent -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Cyan400
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(1.dp, Slate700, CircleShape)
                )
            }
        }

        Text(
            text = label,
            fontSize = 12.sp,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.onSurface
                isCurrent -> Cyan400
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
