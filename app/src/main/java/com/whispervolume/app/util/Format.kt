package com.whispervolume.app.util

import java.util.Locale
import kotlin.math.pow

fun formatAttenuation(db: Float): String = when {
    db < 0.05f -> "0 dB"
    else -> String.format(Locale.US, "−%.1f dB", db)
}

fun formatAttenuationNumber(db: Float): String = when {
    db < 0.05f -> "0.0"
    else -> String.format(Locale.US, "−%.1f", db)
}

fun formatAttenuationCompact(db: Float): String = when {
    db < 0.05f -> "0 dB"
    else -> String.format(Locale.US, "−%.0f dB", db)
}

fun formatLinearPercent(db: Float): String {
    val ratio = 10f.pow(-db / 20f) * 100f
    return if (ratio >= 10f) {
        String.format(Locale.US, "%.0f%%", ratio)
    } else {
        String.format(Locale.US, "%.1f%%", ratio)
    }
}
