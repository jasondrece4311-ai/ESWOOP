package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceswap.ui.MainViewModel
import com.example.faceswap.ui.Screen
import com.example.faceswap.ui.screens.HomeScreen
import com.example.faceswap.ui.screens.ResultScreen
import com.example.faceswap.ui.screens.SettingsScreen
import com.example.faceswap.ui.theme.FaceSwapTheme
import com.example.faceswap.ui.theme.Slate950

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceSwapTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Slate950
                ) {
                    val viewModel: MainViewModel = viewModel()
                    FaceSwapApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FaceSwapApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.currentScreen != Screen.HOME) {
        viewModel.navigateTo(Screen.HOME)
    }

    when (uiState.currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                uiState = uiState,
                onPhotoASelected = { uri -> viewModel.onPhotoASelected(uri) },
                onPhotoBSelected = { uri -> viewModel.onPhotoBSelected(uri) },
                onSelectFaceA = { faceId -> viewModel.selectFaceA(faceId) },
                onSelectFaceB = { faceId -> viewModel.selectFaceB(faceId) },
                onSwapClicked = { viewModel.startSwap() },
                onCancelSwap = { viewModel.cancelSwap() },
                onDismissError = { viewModel.dismissError() },
                onOpenSettings = { viewModel.navigateTo(Screen.SETTINGS) }
            )
        }
        Screen.RESULT -> {
            ResultScreen(
                uiState = uiState,
                onBack = { viewModel.navigateTo(Screen.HOME) },
                onSaveResult = { quality -> viewModel.saveResult(quality) },
                onShareResult = { context -> viewModel.shareResult(context) },
                onNewSwap = { viewModel.resetSession() },
                onDismissSaveNotification = { viewModel.dismissSaveNotification() }
            )
        }
        Screen.SETTINGS -> {
            SettingsScreen(
                uiState = uiState,
                onBack = { viewModel.navigateTo(Screen.HOME) },
                onUpdateConfig = { newConfig -> viewModel.updateConfig(newConfig) }
            )
        }
    }
}
