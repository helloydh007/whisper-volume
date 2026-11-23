package com.whispervolume.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.whispervolume.app.MainActivity
import com.whispervolume.app.R
import com.whispervolume.app.audio.EngineMode
import com.whispervolume.app.audio.EngineState
import com.whispervolume.app.audio.VolumeEngine
import com.whispervolume.app.data.Settings
import com.whispervolume.app.util.formatAttenuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class VolumeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: VolumeOverlay? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val initialNotification = buildNotification(VolumeEngine.state.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        if (!VolumeEngine.start()) {
            Log.e(TAG, "Audio effect could not be started")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        scope.launch {
            VolumeEngine.state.collectLatest { state ->
                if (!state.running) return@collectLatest
                overlay?.update(state.attenuationDb)
                delay(NOTIFICATION_REFRESH_MS)
                notify(state)
            }
        }

        if (Settings.isOverlayEnabled(this)) {
            showOverlay()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_ADJUST -> {
                VolumeEngine.adjust(intent.getFloatExtra(EXTRA_DELTA_DB, 0f))
                overlay?.update(VolumeEngine.state.value.attenuationDb)
                notify(VolumeEngine.state.value)
            }

            ACTION_SHOW_OVERLAY -> {
                Settings.setOverlayEnabled(this, true)
                showOverlay()
            }

            ACTION_HIDE_OVERLAY -> {
                Settings.setOverlayEnabled(this, false)
                hideOverlay()
            }

            ACTION_REFRESH_NOTIFICATION -> notify(VolumeEngine.state.value)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        hideOverlay()
        scope.cancel()
        // Keep the explanatory error visible if the effect never started.
        if (VolumeEngine.state.value.running) {
            VolumeEngine.stop()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission is not granted")
            Settings.setOverlayEnabled(this, false)
            return
        }

        if (overlay == null) {
            overlay = VolumeOverlay(
                service = this,
                onAdjust = { delta ->
                    VolumeEngine.adjust(delta)
                    overlay?.update(VolumeEngine.state.value.attenuationDb)
                    notify(VolumeEngine.state.value)
                },
                onClose = {
                    Settings.setOverlayEnabled(this, false)
                    hideOverlay()
                },
            )
        }

        val shown = overlay?.show() == true
        if (shown) {
            overlay?.update(VolumeEngine.state.value.attenuationDb)
        } else {
            Settings.setOverlayEnabled(this, false)
            Log.e(TAG, "Unable to add overlay window")
        }
    }

    private fun hideOverlay() {
        overlay?.hide()
        overlay = null
    }

    private fun notify(state: EngineState) {
        runCatching {
            getSystemService(NotificationManager::class.java)
                ?.notify(NOTIFICATION_ID, buildNotification(state))
        }
    }

    private fun buildNotification(state: EngineState): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val lighterIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, VolumeService::class.java).apply {
                action = ACTION_ADJUST
                putExtra(EXTRA_DELTA_DB, -3f)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val louderIntent = PendingIntent.getService(
            this,
            12,
            Intent(this, VolumeService::class.java).apply {
                action = ACTION_ADJUST
                putExtra(EXTRA_DELTA_DB, 3f)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            13,
            Intent(this, VolumeService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val volumeLabel = systemVolumeLabel()
        val text = when {
            !state.hasControl -> getString(R.string.notification_control_lost)
            state.mode == EngineMode.COMPAT -> getString(
                R.string.notification_compat,
                formatAttenuation(state.attenuationDb),
                volumeLabel,
            )

            else -> getString(
                R.string.notification_dsp,
                formatAttenuation(state.attenuationDb),
                volumeLabel,
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_volume)
            .setColor(0xFFA8E8CC.toInt())
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setContentIntent(openIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.ic_stat_volume,
                getString(R.string.notification_lower),
                lighterIntent,
            )
            .addAction(
                R.drawable.ic_stat_volume,
                getString(R.string.notification_raise),
                louderIntent,
            )
            .addAction(R.drawable.ic_stat_volume, getString(R.string.notification_stop), stopIntent)
            .build()
    }

    private fun systemVolumeLabel(): String {
        val audioManager = getSystemService(AudioManager::class.java)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return String.format(Locale.US, "%d/%d", current, max)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "WhisperService"
        private const val CHANNEL_ID = "whisper_volume_running"
        private const val NOTIFICATION_ID = 4101
        private const val NOTIFICATION_REFRESH_MS = 100L

        const val ACTION_START = "com.whispervolume.app.action.START"
        const val ACTION_STOP = "com.whispervolume.app.action.STOP"
        const val ACTION_ADJUST = "com.whispervolume.app.action.ADJUST"
        const val ACTION_SHOW_OVERLAY = "com.whispervolume.app.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.whispervolume.app.action.HIDE_OVERLAY"
        const val ACTION_REFRESH_NOTIFICATION =
            "com.whispervolume.app.action.REFRESH_NOTIFICATION"
        const val EXTRA_DELTA_DB = "delta_db"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, VolumeService::class.java).setAction(ACTION_START),
            )
        }

        fun requestStop(context: Context) {
            context.stopService(Intent(context, VolumeService::class.java))
        }

        fun showOverlay(context: Context) {
            context.startService(
                Intent(context, VolumeService::class.java).setAction(ACTION_SHOW_OVERLAY),
            )
        }

        fun hideOverlay(context: Context) {
            context.startService(
                Intent(context, VolumeService::class.java).setAction(ACTION_HIDE_OVERLAY),
            )
        }

        fun refreshNotification(context: Context) {
            context.startService(
                Intent(context, VolumeService::class.java).setAction(ACTION_REFRESH_NOTIFICATION),
            )
        }
    }
}
