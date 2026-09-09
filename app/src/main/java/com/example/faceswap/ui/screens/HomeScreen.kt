package com.example.faceswap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.faceswap.model.ProcessingStage
import com.example.faceswap.ui.Screen
import com.example.faceswap.ui.UiState
import com.example.faceswap.ui.components.PhotoSlotCard
import com.example.faceswap.ui.components.ProcessingOverlay
import com.example.faceswap.ui.theme.Amber400
import com.example.faceswap.ui.theme.Cyan400
import com.example.faceswap.ui.theme.Emerald500
import com.example.faceswap.ui.theme.Indigo500
import com.example.faceswap.ui.theme.Indigo600
import com.example.faceswap.ui.theme.Slate700
import com.example.faceswap.ui.theme.Slate800
import com.example.faceswap.ui.theme.Slate850
import com.example.faceswap.ui.theme.Slate900
import com.example.faceswap.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: UiState,
    onPhotoASelected: (android.net.Uri) -> Unit,
    onPhotoBSelected: (android.net.Uri) -> Unit,
    onSelectFaceA: (String) -> Unit,
    onSelectFaceB: (String) -> Unit,
    onSwapClicked: () -> Unit,
    onCancelSwap: () -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Indigo500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "AI Face Swap",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Slate900
                )
            )
        },
        bottomBar = {
            Surface(
                color = Slate900,
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSwapClicked,
                        enabled = uiState.photoA != null && uiState.photoB != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("swap_face_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo600,
                            disabledContainerColor = Slate800
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SWAP FACE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% Offline On-Device Processing · Zero Cloud Uploads",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pipeline Info Banner
            item {
                Surface(
                    color = Slate850,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Non-Destructive Target Preservation",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Cyan400
                            )
                            Text(
                                text = "Photo B is the base image. Clothing, body, hair, pose, and background are strictly preserved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Photo A Slot
            item {
                PhotoSlotCard(
                    slotLabel = "PHOTO A",
                    roleTitle = "Source Face Identity",
                    roleSubtitle = "Select the portrait providing the facial features and identity.",
                    imageUri = uiState.photoA?.uri,
                    detectedFaces = uiState.photoA?.detectedFaces ?: emptyList(),
                    selectedFaceId = uiState.photoA?.selectedFaceId,
                    isDetecting = uiState.photoA?.isDetecting ?: false,
                    detectionError = uiState.photoA?.detectionError,
                    accentColor = Cyan400,
                    onPhotoSelected = onPhotoASelected,
                    onFaceSelected = onSelectFaceA,
                    testTagPrefix = "photo_a"
                )
            }

            // Photo B Slot
            item {
                PhotoSlotCard(
                    slotLabel = "PHOTO B",
                    roleTitle = "Target Photo (Base Image)",
                    roleSubtitle = "Select the photo providing pose, lighting, clothing, and background.",
                    imageUri = uiState.photoB?.uri,
                    detectedFaces = uiState.photoB?.detectedFaces ?: emptyList(),
                    selectedFaceId = uiState.photoB?.selectedFaceId,
                    isDetecting = uiState.photoB?.isDetecting ?: false,
                    detectionError = uiState.photoB?.detectionError,
                    accentColor = Amber400,
                    onPhotoSelected = onPhotoBSelected,
                    onFaceSelected = onSelectFaceB,
                    testTagPrefix = "photo_b"
                )
            }

            // Engine Diagnostic Status Card
            item {
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local Inference Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.modelSpec.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = if (uiState.isModelConfigured) Emerald500.copy(alpha = 0.2f) else Amber400.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (uiState.isModelConfigured) "● Engine Ready" else "▲ Phase 1 Scaffold",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isModelConfigured) Emerald500 else Amber400
                            )
                        }
                    }
                }
            }
        }
    }

    // Active Processing Dialog
    if (uiState.processingStage !is ProcessingStage.Idle && uiState.processingStage !is ProcessingStage.Completed) {
        ProcessingOverlay(
            stage = uiState.processingStage,
            onCancel = onCancelSwap
        )
    }

    // Error Dialog
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = {
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = error.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Guidance: ${error.suggestion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cyan400
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissError,
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Understood")
                }
            },
            containerColor = Slate900,
            tonalElevation = 6.dp
        )
    }
}
