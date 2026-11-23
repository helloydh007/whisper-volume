# Whisper Volume

English | [简体中文](README.zh-CN.md)

Whisper Volume is a small Android utility for headphones that are still too loud at the lowest system volume step. It adds a global software attenuation layer beneath the system minimum, adjustable in **0.5 dB** steps down to another **−48 dB**.

> This lowers the media signal. It does not rewrite the integer AVRCP volume steps inside a Bluetooth headset. A normal third-party app cannot do that without root or system privileges.

## Download

Get the APK from the [latest GitHub Release](../../releases/latest).

- Version: **v1.0.0**
- Requires: **Android 9 (API 28) or newer**
- APK SHA-256: `e1f276a442aee78d6bcbb6c8d48e6574227102b887ccf0a8a485b238d8aba870`
- The current APK is a **debug-signed sideload build**, not a Google Play release.

```bash
adb install -r WhisperVolume-v1.0.0-debug.apk
```

## Features

- Adds **0 to −48 dB** of extra attenuation below Android's lowest media volume step.
- **0.5 dB** fine adjustment, with Soft / Quiet / Whisper / Hush presets.
- Sets the system media volume to its lowest audible step on start (optional).
- Foreground service keeps the effect active while other apps play audio.
- Notification actions adjust the level by ±3 dB.
- Optional floating `−` / `+` strip above other apps.
- Detects and displays a connected Bluetooth audio device.
- Shows an explicit error when the global effect cannot be attached or is stolen by another app.
- No network permission, no telemetry, no account.

## How it works

Android exposes media volume as integer steps. With Bluetooth absolute volume, the phone maps those steps to the AVRCP range and sends them to the headset. A regular app cannot insert additional fractional steps.

Whisper Volume therefore processes the mixed media output:

```text
media player
    ↓
Android media mixer (lowest system step)
    ↓
DynamicsProcessing global input gain (another 0 to −48 dB)
    ↓
Bluetooth encoder → headset
```

The implementation is in `audio/VolumeEngine.kt`. It uses a global `DynamicsProcessing` input-gain stage. If that is unavailable, it falls back to `LoudnessEnhancer` in compatibility mode (up to about 30 dB on supporting devices).

## Usage

1. Connect the Bluetooth headphones and open Whisper Volume.
2. Choose an attenuation preset. Start around **−24 dB**.
3. Tap **Turn on Whisper Volume** and allow notifications.
4. Play music or video. Drag the fine slider or use the notification controls.
5. For adjustment while another app is on screen, enable **Floating controls** and grant the overlay permission.

The app follows the system language. On Android 13 and later, you can also choose English or Chinese in **Settings → Apps → Whisper Volume → Language**.

## Compatibility and limitations

- **Calls, alarms, keyboard sounds and system UI sounds** usually use separate audio paths and are not affected.
- A few phone audio stacks do not allow third-party apps to attach a global effect, or another equalizer may already own it. The app shows an error instead of pretending to work.
- Some players use direct or offloaded paths that bypass the system mixer.
- **A few phone and Bluetooth-headset combinations bypass global audio effects.** The app may show “running” while the sound does not change. Try a local music player, switch Bluetooth codecs, or test with wired headphones.
- If Bluetooth remains too loud:
  1. Confirm system media volume is at its lowest step.
  2. Move extra attenuation to −36 or −48 dB.
  3. Close other equalizer or volume-booster apps.
  4. On some phones, enable **Disable absolute volume** in Developer options and reconnect the headset.
- Bit-perfect USB output may bypass the effect.

## Privacy

Whisper Volume is fully offline:

- no `INTERNET` permission;
- no analytics, ads, accounts, or telemetry;
- no audio recording, analysis, or upload;
- settings are stored locally on the device.

## Build from source

Requirements: JDK 17 and Android SDK 35.

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

# Optional if the SDK is not discoverable:
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

```text
app/src/main/java/com/whispervolume/app/
├── MainActivity.kt            UI entry, permission flows
├── MainViewModel.kt           system volume and Bluetooth state
├── WhisperApp.kt              engine initialization
├── audio/VolumeEngine.kt      global attenuation and fallback
├── data/Settings.kt           local preferences
├── service/VolumeService.kt   foreground service and notification
├── service/VolumeOverlay.kt   floating control strip
├── ui/HomeScreen.kt           Compose UI
├── ui/WhisperTheme.kt         theme
└── util/Format.kt             dB and percentage formatting
```

UI strings are available in English (`values/`) and Simplified Chinese (`values-zh/`).

## Status

- Source builds successfully and produces an installable debug APK.
- Audible behavior has not been verified on every phone and headset combination. The app exposes an explicit error when the effect cannot be attached, but it cannot detect a Bluetooth path that silently bypasses global effects.

## Acknowledgements

- [Android audio effects documentation](https://developer.android.com/reference/android/media/audiofx/DynamicsProcessing)
- The global-session approach is inspired by the open-source [Quiet Dial / granular-volume](https://github.com/Rzuss/granular-volume) project.
