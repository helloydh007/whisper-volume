package com.whispervolume.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.whispervolume.app.audio.VolumeEngine
import com.whispervolume.app.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemAudioState(
    val volumeIndex: Int = 1,
    val minVolume: Int = 1,
    val maxVolume: Int = 15,
    val isMuted: Boolean = false,
    val bluetoothConnected: Boolean = false,
    val bluetoothActive: Boolean = false,
    val bluetoothName: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val audioManager = app.getSystemService(AudioManager::class.java)

    private val _systemAudio = MutableStateFlow(readSystemAudio())
    val systemAudio = _systemAudio.asStateFlow()

    private val _autoFloor = MutableStateFlow(Settings.isAutoFloor(app))
    val autoFloor = _autoFloor.asStateFlow()

    private val _overlayEnabled = MutableStateFlow(Settings.isOverlayEnabled(app))
    val overlayEnabled = _overlayEnabled.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(Settings.canDrawOverlays(app))
    val hasOverlayPermission = _hasOverlayPermission.asStateFlow()

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshSystemAudio()
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshSystemAudio()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshSystemAudio()
        }
    }

    init {
        ContextCompat.registerReceiver(
            app,
            volumeReceiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            ContextCompat.RECEIVER_EXPORTED,
        )
        audioManager.registerAudioDeviceCallback(
            deviceCallback,
            Handler(Looper.getMainLooper()),
        )
    }

    fun refreshOnResume() {
        refreshSystemAudio()
        _autoFloor.value = Settings.isAutoFloor(app)
        _overlayEnabled.value = Settings.isOverlayEnabled(app)
        _hasOverlayPermission.value = Settings.canDrawOverlays(app)
    }

    fun setSystemVolume(index: Int) {
        val min = safeMinVolume()
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            index.coerceIn(min, max),
            0,
        )
        refreshSystemAudio()
    }

    fun floorSystemVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safeMinVolume(), 0)
        refreshSystemAudio()
    }

    fun setAutoFloor(enabled: Boolean) {
        Settings.setAutoFloor(app, enabled)
        _autoFloor.value = enabled
    }

    fun setOverlayEnabled(enabled: Boolean) {
        Settings.setOverlayEnabled(app, enabled)
        _overlayEnabled.value = enabled
        _hasOverlayPermission.value = Settings.canDrawOverlays(app)
    }

    fun refreshOverlayPermission() {
        _hasOverlayPermission.value = Settings.canDrawOverlays(app)
    }

    fun setAttenuation(db: Float) {
        VolumeEngine.setAttenuation(db)
    }

    fun adjustAttenuation(deltaDb: Float) {
        VolumeEngine.adjust(deltaDb)
    }

    private fun safeMinVolume(): Int =
        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    private fun refreshSystemAudio() {
        _systemAudio.value = readSystemAudio()
    }

    private fun readSystemAudio(): SystemAudioState {
        val min = safeMinVolume()
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val bluetoothDevices = devices.filter { isBluetoothType(it.type) }
        val firstName = bluetoothDevices
            .asSequence()
            .mapNotNull { runCatching { it.productName?.toString() }.getOrNull() }
            .firstOrNull { it.isNotBlank() }

        @Suppress("DEPRECATION")
        val a2dpActive = runCatching { audioManager.isBluetoothA2dpOn }.getOrDefault(false)
        @Suppress("DEPRECATION")
        val scoActive = runCatching { audioManager.isBluetoothScoOn }.getOrDefault(false)

        return SystemAudioState(
            volumeIndex = current,
            minVolume = min,
            maxVolume = max,
            isMuted = runCatching { audioManager.isStreamMute(AudioManager.STREAM_MUSIC) }
                .getOrDefault(false),
            bluetoothConnected = bluetoothDevices.isNotEmpty(),
            bluetoothActive = a2dpActive || scoActive,
            bluetoothName = firstName,
        )
    }

    private fun isBluetoothType(type: Int): Boolean = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_HEARING_AID,
        26, // TYPE_BLE_HEADSET (API 31)
        27, // TYPE_BLE_SPEAKER (API 31)
        -> true

        else -> false
    }

    override fun onCleared() {
        runCatching { app.unregisterReceiver(volumeReceiver) }
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        super.onCleared()
    }
}
