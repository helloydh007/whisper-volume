package com.whispervolume.app.data

import android.content.Context
import android.provider.Settings as AndroidSettings

object Settings {
    private const val PREFS_NAME = "whisper_volume"
    private const val KEY_ATTENUATION_DB = "attenuation_db"
    private const val KEY_AUTO_FLOOR = "auto_floor"
    private const val KEY_OVERLAY = "overlay_enabled"

    const val DEFAULT_ATTENUATION_DB = 24f

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAttenuation(context: Context): Float =
        prefs(context).getFloat(KEY_ATTENUATION_DB, DEFAULT_ATTENUATION_DB)

    fun setAttenuation(context: Context, db: Float) {
        prefs(context).edit().putFloat(KEY_ATTENUATION_DB, db).apply()
    }

    fun isAutoFloor(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_FLOOR, true)

    fun setAutoFloor(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_FLOOR, enabled).apply()
    }

    fun isOverlayEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OVERLAY, false)

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_OVERLAY, enabled).apply()
    }

    fun canDrawOverlays(context: Context): Boolean = AndroidSettings.canDrawOverlays(context)
}
