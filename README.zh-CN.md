# 耳语音量 · Whisper Volume

[English](README.md) | 简体中文

一个面向“系统音量最低档仍然太吵”场景的 Android 小工具。它在系统媒体音量的最低档之下再加一层全局软件衰减，以 **0.5 dB** 为步进，最多再降低 **48 dB**。

> 这是对媒体音频信号做衰减，不是改写蓝牙耳机内部的 AVRCP 硬件音量档位。普通第三方 App 无法在没有 Root 或系统权限的情况下做到后者。

## 下载

从 [最新 GitHub Release](../../releases/latest) 下载 APK。

- 版本：**v1.0.0**
- 要求：**Android 9（API 28）或更高**
- APK SHA-256：`e1f276a442aee78d6bcbb6c8d48e6574227102b887ccf0a8a485b238d8aba870`
- 当前 APK 是 **debug 签名、侧载安装** 的构建，不是 Google Play 发行版。

```bash
adb install -r WhisperVolume-v1.0.0-debug.apk
```

## 功能

- 在 Android 最低媒体音量档之下再增加 **0 ～ −48 dB** 的全局衰减。
- **0.5 dB** 微调，附带“轻声 / 安静 / 耳语 / 极静”预设。
- 开启时自动把系统媒体音量降到最低可听档（可关闭）。
- 前台服务让效果在其他应用播放音频时持续生效。
- 通知栏按钮每次调整 ±3 dB。
- 可选的悬浮 `−` / `+` 调节条，在其他应用上方显示。
- 检测并显示已连接的蓝牙音频设备。
- 无法挂载全局音效、或被其他应用抢走控制权时，会明确显示错误。
- 不申请网络权限，无统计、无账号、无追踪。

## 工作原理

Android 的媒体音量是整数档。开启蓝牙绝对音量后，手机把整数档映射到 AVRCP 范围再发送给耳机，普通 App 无法再插入小数档位。

因此耳语音量处理的是系统混音后的媒体信号：

```text
播放器
    ↓
Android 媒体混音（系统音量最低档）
    ↓
DynamicsProcessing 全局输入增益（再衰减 0 ～ −48 dB）
    ↓
蓝牙编码器 → 耳机
```

核心实现位于 `audio/VolumeEngine.kt`：优先给全局输出挂载 `DynamicsProcessing` 的输入增益；不可用时回退到 `LoudnessEnhancer` 兼容模式（支持设备上最多约 30 dB）。

## 使用

1. 连接蓝牙耳机，打开耳语音量。
2. 先选一个衰减预设，建议从 **−24 dB** 开始。
3. 点“开启耳语音量”，允许通知权限。
4. 播放音乐或视频，拖动精细滑杆，或用通知栏按钮调整。
5. 需要边看其他 App 边调，打开“悬浮调节条”并授予悬浮窗权限。

App 跟随系统语言。Android 13 及以上还可以在 **设置 → 应用 → 耳语音量 → 语言** 中单独选择中文或英文。

## 兼容性与限制

- **手机通话、闹钟、键盘音和系统提示音**通常走独立音频路径，不受影响。
- 少数手机的音频架构不允许第三方 App 向全局输出挂载音效，或已有其他均衡器占用。应用会显示错误，而不是假装生效。
- 部分播放器使用 direct/offload 路径，可能绕过系统混音。
- **少数“手机 + 蓝牙耳机”组合会绕过全局音效。** 此时界面可能显示“正在运行”但听感无变化。可以换本地音乐播放器、切换蓝牙编解码器，或改用有线耳机验证。
- 如果蓝牙仍然太响：
  1. 确认系统媒体音量已经到最低档。
  2. 把额外衰减拉到 −36 或 −48 dB。
  3. 关闭其他均衡器、音量增强或“声音美化”应用。
  4. 部分手机可在开发者选项中开启 **停用绝对音量**，然后重新连接耳机。
- Bit-perfect USB 输出可能绕过效果。

## 隐私

耳语音量完全离线：

- 没有 `INTERNET` 权限；
- 无统计、广告、账号或遥测；
- 不录制、分析或上传音频；
- 设置只保存在本机。

## 从源码构建

需要 JDK 17 和 Android SDK 35。

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

# 如果 SDK 无法自动发现：
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug
```

APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 代码结构

```text
app/src/main/java/com/whispervolume/app/
├── MainActivity.kt            界面入口、权限流程
├── MainViewModel.kt           系统音量与蓝牙状态
├── WhisperApp.kt              音量引擎初始化
├── audio/VolumeEngine.kt      全局衰减与回退策略
├── data/Settings.kt           本地偏好
├── service/VolumeService.kt   前台服务与通知
├── service/VolumeOverlay.kt   悬浮调节条
├── ui/HomeScreen.kt           Compose 界面
├── ui/WhisperTheme.kt         主题
└── util/Format.kt             分贝与百分比显示
```

界面文案提供英文（`values/`）和简体中文（`values-zh/`）。

## 状态

- 源码可成功编译，并生成可安装的 debug APK。
- 尚未逐一验证所有手机和耳机组合的实际听感。应用能在全局音效挂载失败时明确报错，但无法自动检测“蓝牙链路悄悄绕过全局音效”的情况。

## 致谢

- [Android 音频效果文档](https://developer.android.com/reference/android/media/audiofx/DynamicsProcessing)
- 全局 session 方案参考了开源项目 [Quiet Dial / granular-volume](https://github.com/Rzuss/granular-volume)。
