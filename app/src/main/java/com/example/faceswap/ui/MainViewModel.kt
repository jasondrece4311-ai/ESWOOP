package com.example.faceswap.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceswap.engine.FaceSwapEngine
import com.example.faceswap.engine.ModelSpecification
import com.example.faceswap.engine.OfflineFaceSwapEngineImpl
import com.example.faceswap.model.DetectedFace
import com.example.faceswap.model.FaceSwapError
import com.example.faceswap.model.ProcessingStage
import com.example.faceswap.model.QualityLevel
import com.example.faceswap.model.SelectedPhoto
import com.example.faceswap.model.SwapConfiguration
import com.example.faceswap.model.SwapResult
import com.example.faceswap.storage.ImageFileManager
import com.example.faceswap.storage.ShareHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    RESULT,
    SETTINGS
}

data class UiState(
    val photoA: SelectedPhoto? = null,
    val photoB: SelectedPhoto? = null,
    val currentScreen: Screen = Screen.HOME,
    val config: SwapConfiguration = SwapConfiguration(),
    val processingStage: ProcessingStage = ProcessingStage.Idle,
    val swapResult: SwapResult? = null,
    val error: FaceSwapError? = null,
    val saveNotification: String? = null,
    val isSaving: Boolean = false,
    val isModelConfigured: Boolean = false,
    val modelSpec: ModelSpecification = ModelSpecification.MOBILE_FACE_SWAP
) {
    val canSwap: Boolean
        get() = photoA != null && photoB != null &&
                photoA.detectedFaces.isNotEmpty() && photoB.detectedFaces.isNotEmpty() &&
                processingStage is ProcessingStage.Idle
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val imageManager = ImageFileManager(application)
    private val engine: FaceSwapEngine = OfflineFaceSwapEngineImpl(application)

    private val _uiState = MutableStateFlow(
        UiState(
            isModelConfigured = engine.isModelReady(),
            modelSpec = engine.getModelSpecification()
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var swapJob: Job? = null
    private var cachedSourceBitmap: Bitmap? = null
    private var cachedTargetBitmap: Bitmap? = null

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isModelConfigured = engine.isModelReady(),
                    modelSpec = engine.getModelSpecification()
                )
            }
        }
    }

    fun onPhotoASelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    photoA = SelectedPhoto(uri = uri, isDetecting = true),
                    error = null
                )
            }

            val bitmapResult = imageManager.loadBitmapFromUri(uri, maxDimension = 2048)
            bitmapResult.onSuccess { bitmap ->
                cachedSourceBitmap = bitmap
                val detectResult = engine.detectFaces(bitmap)
                detectResult.onSuccess { faces ->
                    _uiState.update { state ->
                        state.copy(
                            photoA = state.photoA?.copy(
                                width = bitmap.width,
                                height = bitmap.height,
                                detectedFaces = faces,
                                selectedFaceId = faces.firstOrNull()?.id,
                                isDetecting = false,
                                detectionError = if (faces.isEmpty()) "No face detected in Photo A" else null
                            )
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { state ->
                        state.copy(
                            photoA = state.photoA?.copy(
                                isDetecting = false,
                                detectionError = err.message ?: "Face detection failed"
                            )
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { state ->
                    state.copy(
                        photoA = state.photoA?.copy(
                            isDetecting = false,
                            detectionError = "Could not decode Photo A: ${err.message}"
                        )
                    )
                }
            }
        }
    }

    fun onPhotoBSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    photoB = SelectedPhoto(uri = uri, isDetecting = true),
                    error = null
                )
            }

            val bitmapResult = imageManager.loadBitmapFromUri(uri, maxDimension = 2048)
            bitmapResult.onSuccess { bitmap ->
                cachedTargetBitmap = bitmap
                val detectResult = engine.detectFaces(bitmap)
                detectResult.onSuccess { faces ->
                    _uiState.update { state ->
                        state.copy(
                            photoB = state.photoB?.copy(
                                width = bitmap.width,
                                height = bitmap.height,
                                detectedFaces = faces,
                                selectedFaceId = faces.firstOrNull()?.id,
                                isDetecting = false,
                                detectionError = if (faces.isEmpty()) "No face detected in Photo B" else null
                            )
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { state ->
                        state.copy(
                            photoB = state.photoB?.copy(
                                isDetecting = false,
                                detectionError = err.message ?: "Face detection failed"
                            )
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { state ->
                    state.copy(
                        photoB = state.photoB?.copy(
                            isDetecting = false,
                            detectionError = "Could not decode Photo B: ${err.message}"
                        )
                    )
                }
            }
        }
    }

    fun selectFaceA(faceId: String) {
        _uiState.update { state ->
            state.copy(photoA = state.photoA?.copy(selectedFaceId = faceId))
        }
    }

    fun selectFaceB(faceId: String) {
        _uiState.update { state ->
            state.copy(photoB = state.photoB?.copy(selectedFaceId = faceId))
        }
    }

    fun startSwap() {
        val state = _uiState.value
        val photoA = state.photoA
        val photoB = state.photoB

        if (photoA == null || photoB == null) {
            _uiState.update { it.copy(error = FaceSwapError.NoPhotoSelected) }
            return
        }

        val sourceFace = photoA.selectedFace
        if (sourceFace == null) {
            _uiState.update { it.copy(error = FaceSwapError.NoFaceDetectedInSource) }
            return
        }

        val targetFace = photoB.selectedFace
        if (targetFace == null) {
            _uiState.update { it.copy(error = FaceSwapError.NoFaceDetectedInTarget) }
            return
        }

        val sourceBmp = cachedSourceBitmap
        val targetBmp = cachedTargetBitmap
        if (sourceBmp == null || targetBmp == null) {
            _uiState.update { it.copy(error = FaceSwapError.Generic("Image buffers not ready")) }
            return
        }

        swapJob?.cancel()
        swapJob = viewModelScope.launch {
            val result = engine.swapFace(
                sourceBitmap = sourceBmp,
                sourceFace = sourceFace,
                targetBitmap = targetBmp,
                targetFace = targetFace,
                config = state.config,
                onProgress = { stage ->
                    _uiState.update { it.copy(processingStage = stage) }
                }
            )

            result.onSuccess { swapResult ->
                _uiState.update {
                    it.copy(
                        swapResult = swapResult,
                        processingStage = ProcessingStage.Idle,
                        currentScreen = Screen.RESULT
                    )
                }
            }.onFailure { exception ->
                val swapError = if (exception is com.example.faceswap.engine.FaceSwapModelException) {
                    exception.error
                } else {
                    FaceSwapError.Generic(exception.message ?: "Swap execution failed")
                }
                _uiState.update {
                    it.copy(
                        processingStage = ProcessingStage.Idle,
                        error = swapError
                    )
                }
            }
        }
    }

    fun cancelSwap() {
        swapJob?.cancel()
        _uiState.update { it.copy(processingStage = ProcessingStage.Idle) }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun updateConfig(newConfig: SwapConfiguration) {
        _uiState.update { it.copy(config = newConfig) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSaveNotification() {
        _uiState.update { it.copy(saveNotification = null) }
    }

    fun saveResult(quality: QualityLevel = _uiState.value.config.quality) {
        val result = _uiState.value.swapResult ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val saveResult = imageManager.saveBitmapToGallery(result.swappedBitmap, quality)
            saveResult.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveNotification = "Successfully saved to device (${quality.label}) in Pictures/FaceSwap!"
                    )
                }
            }.onFailure { err ->
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        error = FaceSwapError.Generic("Failed to save image: ${err.message}")
                    )
                }
            }
        }
    }

    fun shareResult(context: Context) {
        val result = _uiState.value.swapResult ?: return
        viewModelScope.launch {
            val shareUriResult = imageManager.prepareShareableUri(result.swappedBitmap)
            shareUriResult.onSuccess { uri ->
                ShareHelper.shareImage(context, uri, "Share Face Swap Result")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(error = FaceSwapError.Generic("Failed to prepare share file: ${err.message}"))
                }
            }
        }
    }

    fun resetSession() {
        cachedSourceBitmap = null
        cachedTargetBitmap = null
        viewModelScope.launch { imageManager.clearTemporaryFiles() }
        _uiState.update {
            it.copy(
                photoA = null,
                photoB = null,
                swapResult = null,
                currentScreen = Screen.HOME,
                processingStage = ProcessingStage.Idle,
                error = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
        cachedSourceBitmap = null
        cachedTargetBitmap = null
    }
}
