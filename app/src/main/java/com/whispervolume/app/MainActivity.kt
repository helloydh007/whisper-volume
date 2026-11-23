package com.whispervolume.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whispervolume.app.audio.VolumeEngine
import com.whispervolume.app.data.Settings
import com.whispervolume.app.service.VolumeService
import com.whispervolume.app.ui.HomeScreen
import com.whispervolume.app.ui.WhisperTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            WhisperTheme {
                WhisperRoot(viewModel)
            }
        }
    }
}

@Composable
private fun WhisperRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val engine by VolumeEngine.state.collectAsStateWithLifecycle()
    val systemAudio by viewModel.systemAudio.collectAsStateWithLifecycle()
    val autoFloor by viewModel.autoFloor.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()

    var pendingOverlayEnable by rememberSaveable { mutableStateOf(false) }

    fun startEngineNow() {
        if (VolumeEngine.state.value.running) return
        if (Settings.isAutoFloor(context)) {
            viewModel.floorSystemVolume()
        }
        VolumeService.start(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        VolumeService.start(context)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshOverlayPermission()
        if (!pendingOverlayEnable) return@rememberLauncherForActivityResult
        pendingOverlayEnable = false

        if (Settings.canDrawOverlays(context)) {
            viewModel.setOverlayEnabled(true)
            if (VolumeEngine.state.value.running) {
                VolumeService.showOverlay(context)
            } else {
                startEngineNow()
            }
        } else {
            viewModel.setOverlayEnabled(false)
        }
    }

    fun requestStart() {
        if (Settings.isAutoFloor(context)) {
            viewModel.floorSystemVolume()
        }

        val needsNotificationPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED

        if (needsNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            VolumeService.start(context)
        }
    }

    fun onToggleEngine() {
        if (engine.running) {
            VolumeService.requestStop(context)
        } else {
            requestStart()
        }
    }

    fun onOverlayChange(enabled: Boolean) {
        if (!enabled) {
            viewModel.setOverlayEnabled(false)
            if (VolumeEngine.state.value.running) {
                VolumeService.hideOverlay(context)
            }
            return
        }

        if (!Settings.canDrawOverlays(context)) {
            pendingOverlayEnable = true
            val intent = Intent(
                AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
            runCatching { overlayPermissionLauncher.launch(intent) }
                .onFailure {
                    pendingOverlayEnable = false
                    viewModel.setOverlayEnabled(false)
                }
            return
        }

        viewModel.setOverlayEnabled(true)
        if (VolumeEngine.state.value.running) {
            VolumeService.showOverlay(context)
        } else {
            startEngineNow()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshOnResume()
                if (VolumeEngine.state.value.running) {
                    val overlayWanted = Settings.isOverlayEnabled(context)
                    val canDraw = Settings.canDrawOverlays(context)
                    if (overlayWanted && canDraw) {
                        VolumeService.showOverlay(context)
                    } else if (!overlayWanted) {
                        VolumeService.hideOverlay(context)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeScreen(
        engine = engine,
        system = systemAudio,
        autoFloor = autoFloor,
        overlayEnabled = overlayEnabled,
        hasOverlayPermission = hasOverlayPermission,
        onToggleEngine = ::onToggleEngine,
        onAttenuationChange = viewModel::setAttenuation,
        onSystemVolumeChange = viewModel::setSystemVolume,
        onFloorSystemVolume = viewModel::floorSystemVolume,
        onAutoFloorChange = viewModel::setAutoFloor,
        onOverlayChange = ::onOverlayChange,
    )
}
