package com.example.faceswap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.faceswap.model.QualityValidationReport
import com.example.faceswap.ui.theme.Amber400
import com.example.faceswap.ui.theme.Emerald500
import com.example.faceswap.ui.theme.Slate800

@Composable
fun QualityValidationBanner(
    report: QualityValidationReport,
    modifier: Modifier = Modifier
) {
    val hasWarnings = report.warnings.isNotEmpty()
    val borderColor = if (hasWarnings) Amber400 else Emerald500
    val icon = if (hasWarnings) Icons.Default.Warning else Icons.Default.CheckCircle
    val iconTint = if (hasWarnings) Amber400 else Emerald500

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        color = Slate800.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = if (hasWarnings) "Quality Check Notice" else "Photometric Quality Verified",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Metric pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityScorePill("Pose Alignment", "${(report.alignmentScore * 100).toInt()}%")
                QualityScorePill("Lighting Match", "${(report.lightingMatchScore * 100).toInt()}%")
                QualityScorePill("Edge Blend", "${(report.edgeTransitionScore * 100).toInt()}%")
            }

            // Warnings list if present
            if (hasWarnings) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    report.warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Amber400,
                                modifier = Modifier.size(14.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = warning,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityScorePill(label: String, score: String) {
    Surface(
        color = Slate800,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = score, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald500)
        }
    }
}
