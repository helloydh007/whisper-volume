package com.whispervolume.app.audio

import android.content.Context
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import com.whispervolume.app.R
import com.whispervolume.app.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

enum class EngineMode {
    OFF,
    DSP,
    COMPAT,
}

data class EngineState(
    val running: Boolean = false,
    val attenuationDb: Float = Settings.DEFAULT_ATTENUATION_DB,
    val maxDb: Float = VolumeEngine.MAX_DSP_DB,
    val mode: EngineMode = EngineMode.OFF,
    val hasControl: Boolean = true,
    val error: String? = null,
)

/**
 * Owns the single global audio effect used by the app.
 *
 * Android only exposes integer media-volume steps. This effect is inserted on
 * the global output mix, so it can attenuate media from any player after the
 * system's own volume has already reached its lowest step.
 */
object VolumeEngine {
    private const val TAG = "WhisperVolume"
    private const val GLOBAL_SESSION_ID = 0
    private const val EFFECT_PRIORITY = 1

    const val MAX_DSP_DB = 48f
    const val MAX_COMPAT_DB = 30f

    private var appContext: Context? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _state = MutableStateFlow(EngineState())
    val state = _state.asStateFlow()

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val saved = Settings.getAttenuation(context).coerceIn(0f, MAX_DSP_DB)
        _state.update { it.copy(attenuationDb = saved) }
    }

    @Synchronized
    fun start(): Boolean {
        val context = appContext ?: return false
        if (dynamicsProcessing != null || loudnessEnhancer != null) return true

        val requestedDb = Settings.getAttenuation(context).coerceIn(0f, MAX_DSP_DB)

        if (startDynamicsProcessing(requestedDb)) return true

        releaseEffects()
        if (startLoudnessEnhancer(requestedDb)) return true

        releaseEffects()
        _state.update {
            it.copy(
                running = false,
                mode = EngineMode.OFF,
                hasControl = false,
                error = context.getString(R.string.error_global_not_allowed),
            )
        }
        return false
    }

    @Synchronized
    fun setAttenuation(db: Float) {
        val context = appContext ?: return
        val max = _state.value.maxDb
        val rounded = ((db.coerceIn(0f, max) * 2f).roundToInt() / 2f)
        Settings.setAttenuation(context, rounded)
        _state.update { it.copy(attenuationDb = rounded) }

        if (dynamicsProcessing == null && loudnessEnhancer == null) return

        try {
            applyLevel(rounded)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to change effect level", e)
            _state.update {
                it.copy(
                    error = appContext?.getString(R.string.error_adjust_failed)
                        ?: "Could not adjust the volume.",
                )
            }
        }
    }

    fun adjust(deltaDb: Float) {
        setAttenuation(_state.value.attenuationDb + deltaDb)
    }

    @Synchronized
    fun stop() {
        releaseEffects()
        _state.update {
            it.copy(
                running = false,
                mode = EngineMode.OFF,
                hasControl = true,
                error = null,
                maxDb = MAX_DSP_DB,
            )
        }
    }

    private fun startDynamicsProcessing(db: Float): Boolean {
        var candidate: DynamicsProcessing? = null
        val occupiedMessage = appContext?.getString(R.string.error_effect_occupied)
            ?: "Another app is holding the global audio effect."
        return try {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                /* channelCount = */ 2,
                /* preEqInUse = */ false,
                /* preEqBandCount = */ 0,
                /* mbcInUse = */ false,
                /* mbcBandCount = */ 0,
                /* postEqInUse = */ false,
                /* postEqBandCount = */ 0,
                /* limiterInUse = */ false,
            ).build()

            val effect = DynamicsProcessing(EFFECT_PRIORITY, GLOBAL_SESSION_ID, config)
            candidate = effect
            effect.setControlStatusListener { _, granted ->
                _state.update {
                    it.copy(
                        hasControl = granted,
                        error = if (granted) null else occupiedMessage,
                    )
                }
            }
            effect.setInputGainAllChannelsTo(-db)
            effect.enabled = true

            dynamicsProcessing = effect
            _state.update {
                it.copy(
                    running = true,
                    mode = EngineMode.DSP,
                    maxDb = MAX_DSP_DB,
                    attenuationDb = db,
                    hasControl = effect.hasControl(),
                    error = null,
                )
            }
            Log.i(TAG, "DynamicsProcessing attached at -$db dB")
            true
        } catch (e: Exception) {
            Log.w(TAG, "DynamicsProcessing unavailable", e)
            runCatching { candidate?.release() }
            false
        }
    }

    private fun startLoudnessEnhancer(db: Float): Boolean {
        var candidate: LoudnessEnhancer? = null
        val occupiedMessage = appContext?.getString(R.string.error_effect_occupied)
            ?: "Another app is holding the global audio effect."
        return try {
            val supportedDb = db.coerceAtMost(MAX_COMPAT_DB)
            val effect = LoudnessEnhancer(GLOBAL_SESSION_ID)
            candidate = effect
            effect.setControlStatusListener { _, granted ->
                _state.update {
                    it.copy(
                        hasControl = granted,
                        error = if (granted) null else occupiedMessage,
                    )
                }
            }
            effect.setTargetGain(-(supportedDb * 100f).roundToInt())
            effect.enabled = true

            loudnessEnhancer = effect
            appContext?.let { Settings.setAttenuation(it, supportedDb) }
            _state.update {
                it.copy(
                    running = true,
                    mode = EngineMode.COMPAT,
                    maxDb = MAX_COMPAT_DB,
                    attenuationDb = supportedDb,
                    hasControl = effect.hasControl(),
                    error = null,
                )
            }
            Log.i(TAG, "LoudnessEnhancer attached at -$supportedDb dB")
            true
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer unavailable", e)
            runCatching { candidate?.release() }
            false
        }
    }

    private fun applyLevel(db: Float) {
        dynamicsProcessing?.setInputGainAllChannelsTo(-db)
        loudnessEnhancer?.setTargetGain(-(db * 100f).roundToInt())
    }

    private fun releaseEffects() {
        dynamicsProcessing?.let { effect ->
            runCatching { effect.setControlStatusListener(null) }
            runCatching { effect.enabled = false }
            runCatching { effect.release() }
        }
        dynamicsProcessing = null

        loudnessEnhancer?.let { effect ->
            runCatching { effect.setControlStatusListener(null) }
            runCatching { effect.enabled = false }
            runCatching { effect.release() }
        }
        loudnessEnhancer = null
    }
}
