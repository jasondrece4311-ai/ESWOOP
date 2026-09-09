package com.example.faceswap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.faceswap.engine.ModelSpecification
import com.example.faceswap.model.HardwareMode
import com.example.faceswap.model.SwapConfiguration
import com.example.faceswap.ui.UiState
import com.example.faceswap.ui.theme.Amber400
import com.example.faceswap.ui.theme.Cyan400
import com.example.faceswap.ui.theme.Emerald500
import com.example.faceswap.ui.theme.Indigo500
import com.example.faceswap.ui.theme.Slate700
import com.example.faceswap.ui.theme.Slate800
import com.example.faceswap.ui.theme.Slate850
import com.example.faceswap.ui.theme.Slate900
import com.example.faceswap.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: UiState,
    onBack: () -> Unit,
    onUpdateConfig: (SwapConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.config
    val spec = uiState.modelSpec

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Engine & Pipeline Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Model Architecture & License Section (Requirement 12)
            SectionHeader(icon = Icons.Default.Memory, title = "ON-DEVICE AI MODEL SPECIFICATION")

            Surface(
                color = Slate850,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = spec.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Indigo500
                        )
                        Surface(
                            color = if (uiState.isModelConfigured) Emerald500.copy(alpha = 0.2f) else Amber400.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (uiState.isModelConfigured) "READY" else "PENDING MODEL FILE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isModelConfigured) Emerald500 else Amber400
                            )
                        }
                    }

                    ModelSpecRow("Format", spec.format)
                    ModelSpecRow("Expected Location", spec.expectedAssetPath)
                    ModelSpecRow("License", spec.licenseName)
                    ModelSpecRow(
                        "Redistribution",
                        if (spec.isRedistributionAllowed) "Permitted (Apache 2.0)" else "Restricted / Non-Commercial"
                    )
                    ModelSpecRow(
                        "Commercial Use",
                        if (spec.isCommercialUseAllowed) "Permitted" else "Non-Commercial Research Only"
                    )
                    ModelSpecRow("Inference Resolution", spec.inputResolution)
                    ModelSpecRow("Engine Framework", spec.framework)

                    Text(
                        text = spec.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pipeline Adaptation Features (Requirements 4, 6, 7, 8, 9, 10)
            SectionHeader(icon = Icons.Default.Tune, title = "PIPELINE ADAPTATION MODULES")

            Surface(
                color = Slate850,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingToggle(
                        title = "Automatic Pose & Angle Alignment",
                        description = "Estimates 3D yaw, pitch, and roll to naturally follow Photo B's head orientation.",
                        checked = config.preservePoseAndGeometry,
                        onCheckedChange = { onUpdateConfig(config.copy(preservePoseAndGeometry = it)) }
                    )

                    SettingToggle(
                        title = "Lighting & Exposure Matching",
                        description = "Analyzes ambient light, temperature, and shadows to match Photo B environment.",
                        checked = config.matchExposureAndLighting,
                        onCheckedChange = { onUpdateConfig(config.copy(matchExposureAndLighting = it)) }
                    )

                    SettingToggle(
                        title = "Skin Tone & Chroma Harmonization",
                        description = "Adapts skin tone seamlessly without erasing natural skin texture and identity.",
                        checked = config.matchSkinTone,
                        onCheckedChange = { onUpdateConfig(config.copy(matchSkinTone = it)) }
                    )

                    SettingToggle(
                        title = "Adaptive Soft-Edge Blending",
                        description = "Computes landmark-guided soft transition masks to avoid visible seams or halos.",
                        checked = config.seamlessBlendingEnabled,
                        onCheckedChange = { onUpdateConfig(config.copy(seamlessBlendingEnabled = it)) }
                    )

                    SettingToggle(
                        title = "High-Frequency Detail Restoration",
                        description = "Restores fine pore structure, eyes, and micro-textures from original Photo B.",
                        checked = config.detailRestorationEnabled,
                        onCheckedChange = { onUpdateConfig(config.copy(detailRestorationEnabled = it)) }
                    )

                    SettingToggle(
                        title = "Automated Quality Validation",
                        description = "Runs sanity checks for severe misalignment, extreme angles, and mask leaks.",
                        checked = config.performQualityValidation,
                        onCheckedChange = { onUpdateConfig(config.copy(performQualityValidation = it)) }
                    )
                }
            }

            // Privacy & Architecture Card
            SectionHeader(icon = Icons.Default.Lock, title = "PRIVACY & LOCAL SECURITY")

            Surface(
                color = Slate850,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Complete Local Isolation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Zero internet connectivity required.\n• No photographs or face embeddings leave this physical handset.\n• No cloud APIs or remote processing servers are contacted.\n• Temporary image caches are purged upon exiting or resetting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Cyan400,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Cyan400,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ModelSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Indigo500,
                uncheckedTrackColor = Slate800,
                uncheckedThumbColor = Slate700
            )
        )
    }
}
