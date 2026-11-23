package com.whispervolume.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whispervolume.app.R
import com.whispervolume.app.SystemAudioState
import com.whispervolume.app.audio.EngineMode
import com.whispervolume.app.audio.EngineState
import com.whispervolume.app.util.formatAttenuation
import com.whispervolume.app.util.formatAttenuationCompact
import com.whispervolume.app.util.formatAttenuationNumber
import com.whispervolume.app.util.formatLinearPercent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HomeScreen(
    engine: EngineState,
    system: SystemAudioState,
    autoFloor: Boolean,
    overlayEnabled: Boolean,
    hasOverlayPermission: Boolean,
    onToggleEngine: () -> Unit,
    onAttenuationChange: (Float) -> Unit,
    onSystemVolumeChange: (Int) -> Unit,
    onFloorSystemVolume: () -> Unit,
    onAutoFloorChange: (Boolean) -> Unit,
    onOverlayChange: (Boolean) -> Unit,
) {
    var showInfo by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        WhisperPalette.BackgroundSoft,
                        WhisperPalette.Background,
                        Color(0xFF060909),
                    ),
                ),
            ),
    ) {
        AmbientGlow()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        ) {
            item { Header(onInfo = { showInfo = true }) }
            item { Spacer(Modifier.height(18.dp)) }
            item {
                DeviceAndStatus(
                    system = system,
                    engine = engine,
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                VolumeDial(
                    db = engine.attenuationDb,
                    maxDb = engine.maxDb,
                    running = engine.running,
                    hasControl = engine.hasControl,
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                FineControl(
                    engine = engine,
                    onAttenuationChange = onAttenuationChange,
                )
            }
            item { Spacer(Modifier.height(22.dp)) }
            item {
                if (engine.error != null || (engine.running && !engine.hasControl)) {
                    EffectWarning(
                        message = engine.error ?: stringResource(R.string.error_effect_occupied),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                PowerCard(
                    engine = engine,
                    autoFloor = autoFloor,
                    onClick = onToggleEngine,
                )
            }
            item { Spacer(Modifier.height(26.dp)) }
            item { SectionHeader(stringResource(R.string.section_system_volume)) }
            item { Spacer(Modifier.height(10.dp)) }
            item {
                SystemVolumeCard(
                    system = system,
                    autoFloor = autoFloor,
                    onSystemVolumeChange = onSystemVolumeChange,
                    onFloorSystemVolume = onFloorSystemVolume,
                    onAutoFloorChange = onAutoFloorChange,
                )
            }
            item { Spacer(Modifier.height(26.dp)) }
            item { SectionHeader(stringResource(R.string.section_quick_controls)) }
            item { Spacer(Modifier.height(10.dp)) }
            item {
                OverlayCard(
                    overlayEnabled = overlayEnabled,
                    hasPermission = hasOverlayPermission,
                    onOverlayChange = onOverlayChange,
                )
            }
            if (engine.running && engine.mode == EngineMode.COMPAT) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    InfoNote(
                        title = stringResource(R.string.compat_note_title),
                        body = stringResource(R.string.compat_note_body),
                        tone = WhisperPalette.Warm,
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
            item { FooterNote(onOpenInfo = { showInfo = true }) }
        }
    }

    if (showInfo) {
        InfoSheet(onDismiss = { showInfo = false })
    }
}

@Composable
private fun AmbientGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    WhisperPalette.Accent.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.82f, size.height * 0.10f),
                radius = size.width * 0.85f,
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.82f, size.height * 0.10f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    WhisperPalette.Warm.copy(alpha = 0.035f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.05f, size.height * 0.58f),
                radius = size.width * 0.7f,
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.05f, size.height * 0.58f),
        )
    }
}

@Composable
private fun Header(onInfo: () -> Unit) {
    val infoDescription = stringResource(R.string.info_content_desc)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(WhisperPalette.Accent.copy(alpha = 0.12f))
                .border(
                    1.dp,
                    WhisperPalette.Accent.copy(alpha = 0.28f),
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.logo_glyph),
                color = WhisperPalette.Accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = WhisperPalette.Text,
            )
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = WhisperPalette.TextFaint,
            )
        }

        Spacer(Modifier.weight(1f))

        Surface(
            onClick = onInfo,
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = infoDescription },
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, WhisperPalette.Line),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "i",
                    color = WhisperPalette.TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DeviceAndStatus(system: SystemAudioState, engine: EngineState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f, fill = false),
            shape = RoundedCornerShape(50),
            color = WhisperPalette.Surface.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, WhisperPalette.Line),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (system.bluetoothConnected) {
                                WhisperPalette.Accent
                            } else {
                                WhisperPalette.TextFaint
                            },
                        ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        system.bluetoothActive &&
                            !system.bluetoothName.isNullOrBlank() -> system.bluetoothName.orEmpty()
                        system.bluetoothActive -> stringResource(R.string.device_bt_active)
                        system.bluetoothConnected &&
                            !system.bluetoothName.isNullOrBlank() -> stringResource(
                            R.string.device_bt_connected_suffix,
                            system.bluetoothName.orEmpty(),
                        )

                        system.bluetoothConnected -> stringResource(R.string.device_bt_connected)
                        else -> stringResource(R.string.device_local)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = WhisperPalette.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = if (engine.running) {
                WhisperPalette.Accent.copy(alpha = 0.10f)
            } else {
                Color.Transparent
            },
            border = BorderStroke(
                1.dp,
                if (engine.running) {
                    WhisperPalette.Accent.copy(alpha = 0.22f)
                } else {
                    WhisperPalette.Line
                },
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (engine.running) WhisperPalette.Accent else WhisperPalette.TextFaint,
                        ),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (engine.running) R.string.status_on else R.string.status_off,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (engine.running) {
                        WhisperPalette.Accent
                    } else {
                        WhisperPalette.TextFaint
                    },
                )
            }
        }
    }
}

@Composable
private fun VolumeDial(
    db: Float,
    maxDb: Float,
    running: Boolean,
    hasControl: Boolean,
) {
    val animatedDb by animateFloatAsState(
        targetValue = db,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "db",
    )
    val ratio = (animatedDb / maxDb.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val activeColor = if (running && hasControl) {
        WhisperPalette.Accent
    } else {
        WhisperPalette.Accent.copy(alpha = 0.38f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(318.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2f
            val startAngle = 135f
            val sweep = 270f
            val tickCount = 61
            val tickOuter = radius * 0.94f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WhisperPalette.Accent.copy(alpha = if (running) 0.07f else 0.025f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius * 0.74f,
                ),
                radius = radius * 0.74f,
                center = center,
            )

            for (index in 0 until tickCount) {
                val fraction = index.toFloat() / (tickCount - 1)
                val angle = startAngle + sweep * fraction
                val angleRad = angle * PI.toFloat() / 180f
                val direction = Offset(cos(angleRad), sin(angleRad))
                val isActive = fraction <= ratio + 0.001f
                val tickLength = if (isActive) 9.dp.toPx() else 5.dp.toPx()
                val innerRadius = tickOuter - tickLength
                drawLine(
                    color = if (isActive) {
                        activeColor
                    } else {
                        Color.White.copy(alpha = 0.10f)
                    },
                    start = center + direction * innerRadius,
                    end = center + direction * tickOuter,
                    strokeWidth = if (isActive) 1.8.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            val arcRadius = radius * 0.79f
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

            drawArc(
                color = Color.White.copy(alpha = 0.055f),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
            )

            if (ratio > 0.001f) {
                drawArc(
                    color = activeColor,
                    startAngle = startAngle,
                    sweepAngle = sweep * ratio,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                )

                val endAngle = (startAngle + sweep * ratio) * PI.toFloat() / 180f
                val end = center + Offset(cos(endAngle), sin(endAngle)) * arcRadius
                drawCircle(
                    color = Color(0xFF0B1311),
                    radius = 7.5.dp.toPx(),
                    center = end,
                )
                drawCircle(
                    color = activeColor,
                    radius = 4.dp.toPx(),
                    center = end,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(min = 190.dp, max = 260.dp),
        ) {
            Text(
                text = stringResource(
                    if (running) R.string.attenuation_running else R.string.attenuation_idle,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = WhisperPalette.TextFaint,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatAttenuationNumber(animatedDb),
                    style = MaterialTheme.typography.displayLarge,
                    color = if (running && hasControl) {
                        WhisperPalette.Text
                    } else {
                        WhisperPalette.Text.copy(alpha = 0.82f)
                    },
                )
                Text(
                    text = " dB",
                    style = MaterialTheme.typography.titleMedium,
                    color = WhisperPalette.TextMuted,
                    modifier = Modifier.padding(bottom = 11.dp),
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = if (animatedDb < 0.05f) {
                    stringResource(R.string.original_volume)
                } else {
                    stringResource(
                        R.string.linear_gain,
                        formatLinearPercent(animatedDb),
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (running && hasControl) {
                    WhisperPalette.Accent
                } else {
                    WhisperPalette.TextMuted
                },
            )
        }
    }
}

@Composable
private fun FineControl(
    engine: EngineState,
    onAttenuationChange: (Float) -> Unit,
) {
    val sliderDescription = stringResource(R.string.slider_extra_desc)

    Slider(
        value = engine.attenuationDb.coerceIn(0f, engine.maxDb),
        onValueChange = onAttenuationChange,
        valueRange = 0f..engine.maxDb,
        steps = (engine.maxDb * 2f).roundToInt().minus(1).coerceAtLeast(0),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = sliderDescription },
        colors = SliderDefaults.colors(
            thumbColor = WhisperPalette.Accent,
            activeTrackColor = WhisperPalette.Accent,
            inactiveTrackColor = Color.White.copy(alpha = 0.09f),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.endpoint_original),
            style = MaterialTheme.typography.labelMedium,
            color = WhisperPalette.TextFaint,
        )
        Text(
            text = stringResource(
                R.string.endpoint_quietest,
                formatAttenuation(engine.maxDb),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = WhisperPalette.TextFaint,
        )
    }

    Spacer(Modifier.height(7.dp))

    Text(
        text = stringResource(R.string.fine_step_hint),
        style = MaterialTheme.typography.labelMedium,
        color = WhisperPalette.TextFaint,
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(16.dp))

    val presets = listOf(
        R.string.preset_soft to 12f,
        R.string.preset_quiet to 24f,
        R.string.preset_whisper to 36f,
        R.string.preset_hush to 48f,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { (labelRes, value) ->
            val selected = kotlin.math.abs(engine.attenuationDb - value) < 0.26f
            PresetButton(
                label = stringResource(labelRes),
                db = value,
                selected = selected,
                enabled = value <= engine.maxDb,
                onClick = { onAttenuationChange(value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    db: Float,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            WhisperPalette.Accent.copy(alpha = 0.13f)
        } else {
            WhisperPalette.Surface.copy(alpha = if (enabled) 0.72f else 0.35f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                WhisperPalette.Accent.copy(alpha = 0.52f)
            } else {
                WhisperPalette.Line
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    WhisperPalette.Accent
                } else if (enabled) {
                    WhisperPalette.TextMuted
                } else {
                    WhisperPalette.TextFaint
                },
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatAttenuationCompact(db),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    WhisperPalette.Accent.copy(alpha = 0.75f)
                } else {
                    WhisperPalette.TextFaint
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PowerCard(
    engine: EngineState,
    autoFloor: Boolean,
    onClick: () -> Unit,
) {
    val running = engine.running
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (running) WhisperPalette.SurfaceHigh else WhisperPalette.Accent,
        contentColor = if (running) WhisperPalette.Text else Color(0xFF07110D),
        border = if (running) {
            BorderStroke(1.dp, WhisperPalette.Accent.copy(alpha = 0.22f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (running) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(WhisperPalette.Accent),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(
                            if (running) R.string.power_running else R.string.power_start,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (running) {
                        if (engine.hasControl) {
                            stringResource(
                                R.string.power_running_desc,
                                formatAttenuation(engine.attenuationDb),
                            )
                        } else {
                            stringResource(R.string.power_lost_desc)
                        }
                    } else {
                        stringResource(R.string.power_idle_desc)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (running) {
                        WhisperPalette.TextMuted
                    } else {
                        Color(0xFF07110D).copy(alpha = 0.66f)
                    },
                )
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = if (running) {
                    Color.White.copy(alpha = 0.08f)
                } else {
                    Color(0xFF07110D).copy(alpha = 0.10f)
                },
            ) {
                Text(
                    text = stringResource(
                        if (running) R.string.action_stop else R.string.action_start,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (running) WhisperPalette.Accent else Color(0xFF07110D),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(WhisperPalette.TextFaint),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = stringResource(
                if (autoFloor) R.string.auto_floor_hint else R.string.system_unchanged_hint,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = WhisperPalette.TextFaint,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = WhisperPalette.TextFaint,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun SystemVolumeCard(
    system: SystemAudioState,
    autoFloor: Boolean,
    onSystemVolumeChange: (Int) -> Unit,
    onFloorSystemVolume: () -> Unit,
    onAutoFloorChange: (Boolean) -> Unit,
) {
    val sliderDescription = stringResource(R.string.slider_system_desc)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = WhisperPalette.Surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, WhisperPalette.Line),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.system_media_volume),
                        style = MaterialTheme.typography.titleMedium,
                        color = WhisperPalette.Text,
                    )
                    Text(
                        text = stringResource(R.string.system_integer_steps),
                        style = MaterialTheme.typography.labelMedium,
                        color = WhisperPalette.TextFaint,
                    )
                }
                Text(
                    text = if (system.isMuted) {
                        stringResource(R.string.muted)
                    } else {
                        stringResource(
                            R.string.volume_fraction,
                            system.volumeIndex,
                            system.maxVolume,
                        )
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (system.isMuted) WhisperPalette.Warm else WhisperPalette.Text,
                )
            }

            Spacer(Modifier.height(6.dp))

            Slider(
                value = system.volumeIndex.coerceIn(system.minVolume, system.maxVolume).toFloat(),
                onValueChange = { onSystemVolumeChange(it.roundToInt()) },
                valueRange = system.minVolume.toFloat()..system.maxVolume.toFloat(),
                steps = (system.maxVolume - system.minVolume - 1).coerceAtLeast(0),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = sliderDescription },
                colors = SliderDefaults.colors(
                    thumbColor = WhisperPalette.Text,
                    activeTrackColor = WhisperPalette.TextMuted,
                    inactiveTrackColor = Color.White.copy(alpha = 0.08f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.lowest_step,
                        system.minVolume,
                        system.maxVolume,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = WhisperPalette.TextFaint,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    onClick = onFloorSystemVolume,
                    enabled = system.volumeIndex > system.minVolume,
                    shape = RoundedCornerShape(50),
                    color = WhisperPalette.Accent.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, WhisperPalette.Accent.copy(alpha = 0.24f)),
                ) {
                    Text(
                        text = stringResource(R.string.go_to_lowest),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (system.volumeIndex > system.minVolume) {
                            WhisperPalette.Accent
                        } else {
                            WhisperPalette.TextFaint
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = WhisperPalette.Line)
            Spacer(Modifier.height(13.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.floor_on_start),
                        style = MaterialTheme.typography.labelLarge,
                        color = WhisperPalette.Text,
                    )
                    Text(
                        text = stringResource(R.string.floor_on_start_desc),
                        style = MaterialTheme.typography.labelMedium,
                        color = WhisperPalette.TextFaint,
                    )
                }
                Switch(
                    checked = autoFloor,
                    onCheckedChange = onAutoFloorChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF07110D),
                        checkedTrackColor = WhisperPalette.Accent,
                        uncheckedThumbColor = WhisperPalette.TextMuted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.06f),
                        uncheckedBorderColor = WhisperPalette.Line,
                    ),
                )
            }
        }
    }
}

@Composable
private fun OverlayCard(
    overlayEnabled: Boolean,
    hasPermission: Boolean,
    onOverlayChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = WhisperPalette.Surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, WhisperPalette.Line),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.overlay_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = WhisperPalette.Text,
                    )
                    Text(
                        text = stringResource(R.string.overlay_desc),
                        style = MaterialTheme.typography.labelMedium,
                        color = WhisperPalette.TextFaint,
                    )
                }
                Switch(
                    checked = overlayEnabled && hasPermission,
                    onCheckedChange = onOverlayChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF07110D),
                        checkedTrackColor = WhisperPalette.Accent,
                        uncheckedThumbColor = WhisperPalette.TextMuted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.06f),
                        uncheckedBorderColor = WhisperPalette.Line,
                    ),
                )
            }

            if (overlayEnabled && !hasPermission) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.overlay_permission_needed),
                    style = MaterialTheme.typography.labelMedium,
                    color = WhisperPalette.Warm,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = WhisperPalette.Line)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(WhisperPalette.AccentDeep),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(R.string.overlay_notification_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WhisperPalette.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun EffectWarning(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = WhisperPalette.ErrorContainer.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, WhisperPalette.Error.copy(alpha = 0.22f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.effect_unavailable_title),
                style = MaterialTheme.typography.labelLarge,
                color = WhisperPalette.Error,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFDAD5).copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun InfoNote(title: String, body: String, tone: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = tone.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.18f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = tone,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = WhisperPalette.TextMuted,
            )
        }
    }
}

@Composable
private fun FooterNote(onOpenInfo: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.footer_limits),
            style = MaterialTheme.typography.labelMedium,
            color = WhisperPalette.TextFaint,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.footer_learn_more),
            style = MaterialTheme.typography.labelMedium,
            color = WhisperPalette.Accent.copy(alpha = 0.82f),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onOpenInfo)
                .padding(vertical = 3.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.footer_version),
            style = MaterialTheme.typography.labelSmall,
            color = WhisperPalette.TextFaint,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(onDismiss: () -> Unit) {
    val btSteps = stringArrayResource(R.array.bt_troubleshooting_steps).toList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WhisperPalette.Surface,
        contentColor = WhisperPalette.Text,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(WhisperPalette.Line),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.about_title),
                style = MaterialTheme.typography.headlineMedium,
                color = WhisperPalette.Text,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.about_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = WhisperPalette.TextMuted,
            )

            InfoSection(
                title = stringResource(R.string.about_how_title),
                body = stringResource(R.string.about_how_body),
            )
            InfoSection(
                title = stringResource(R.string.about_scope_title),
                body = stringResource(R.string.about_scope_body),
            )
            InfoSection(
                title = stringResource(R.string.about_limits_title),
                body = stringResource(R.string.about_limits_body),
            )
            InfoSection(
                title = stringResource(R.string.about_bt_title),
                steps = btSteps,
            )
            InfoSection(
                title = stringResource(R.string.about_privacy_title),
                body = stringResource(R.string.about_privacy_body),
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = WhisperPalette.Accent,
                contentColor = Color(0xFF07110D),
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.about_close),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    body: String? = null,
    steps: List<String>? = null,
) {
    Spacer(Modifier.height(22.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = WhisperPalette.Accent,
    )
    Spacer(Modifier.height(6.dp))
    if (body != null) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = WhisperPalette.TextMuted,
        )
    }
    steps?.forEachIndexed { index, step ->
        Row(
            modifier = Modifier.padding(top = if (index == 0) 2.dp else 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(WhisperPalette.Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhisperPalette.Accent,
                    fontSize = 10.sp,
                    letterSpacing = 0.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = step,
                style = MaterialTheme.typography.bodyMedium,
                color = WhisperPalette.TextMuted,
            )
        }
    }
}
