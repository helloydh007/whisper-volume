package com.whispervolume.app

import android.app.Application
import com.whispervolume.app.audio.VolumeEngine

class WhisperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VolumeEngine.initialize(this)
    }
}
