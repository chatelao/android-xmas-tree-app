package com.xmastree.remote.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xmastree.remote.protocol.MelodyId
import com.xmastree.remote.protocol.PatternId
import com.xmastree.remote.protocol.PlaybackState
import com.xmastree.remote.ui.TreeRemoteUiState
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlScreen(
    uiState: TreeRemoteUiState,
    onSelectPattern: (PatternId) -> Unit,
    onColorSelected: (Int, Int, Int) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onSelectMelody: (MelodyId) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onTempoChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lighting Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Lighting Pattern", style = MaterialTheme.typography.titleMedium)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PatternId.values().forEach { pattern ->
                        FilterChip(
                            selected = uiState.patternId == pattern,
                            onClick = { onSelectPattern(pattern) },
                            label = { Text(formatPatternName(pattern)) }
                        )
                    }
                }

                Text("Color Wheel", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ColorWheel(
                        modifier = Modifier.size(160.dp),
                        onColorPicked = onColorSelected
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(uiState.red, uiState.green, uiState.blue))
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Text(
                            text = "RGB(${uiState.red}, ${uiState.green}, ${uiState.blue})",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ColorSwatch(255, 0, 0, onColorSelected)
                            ColorSwatch(0, 200, 50, onColorSelected)
                            ColorSwatch(255, 190, 0, onColorSelected)
                            ColorSwatch(0, 140, 255, onColorSelected)
                            ColorSwatch(255, 240, 200, onColorSelected)
                        }
                    }
                }

                Text("Brightness: ${uiState.brightness}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.brightness.toFloat(),
                    onValueChange = { onBrightnessChange(it.roundToInt()) },
                    valueRange = 0f..100f
                )

                Text("Animation Speed: ${uiState.speed}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.speed.toFloat(),
                    onValueChange = { onSpeedChange(it.roundToInt()) },
                    valueRange = 1f..100f
                )
            }
        }

        // Audio / Melody Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Festive Melody Player", style = MaterialTheme.typography.titleMedium)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MelodyId.values().forEach { melody ->
                        FilterChip(
                            selected = uiState.melodyId == melody,
                            onClick = { onSelectMelody(melody) },
                            label = { Text(formatMelodyName(melody)) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPlay,
                        enabled = uiState.playbackState != PlaybackState.PLAY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        Text(" Play")
                    }
                    OutlinedButton(
                        onClick = onPause,
                        enabled = uiState.playbackState == PlaybackState.PLAY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                        Text(" Pause")
                    }
                    OutlinedButton(
                        onClick = onStop,
                        enabled = uiState.playbackState != PlaybackState.STOP,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                        Text(" Stop")
                    }
                }

                Text("Volume: ${uiState.volume}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.volume.toFloat(),
                    onValueChange = { onVolumeChange(it.roundToInt()) },
                    valueRange = 0f..100f
                )

                Text("Tempo: ${uiState.tempo}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.tempo.toFloat(),
                    onValueChange = { onTempoChange(it.roundToInt()) },
                    valueRange = 50f..200f
                )
            }
        }
    }
}

@Composable
private fun ColorWheel(
    modifier: Modifier = Modifier,
    onColorPicked: (Int, Int, Int) -> Unit
) {
    val hueColors = listOf(
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
    )

    fun pickAt(offset: Offset, sizePx: Float) {
        val center = sizePx / 2f
        val dx = offset.x - center
        val dy = offset.y - center
        val radius = center
        val dist = hypot(dx, dy).coerceAtMost(radius)
        val angleRad = atan2(dy.toDouble(), dx.toDouble())
        var angleDeg = Math.toDegrees(angleRad).toFloat()
        if (angleDeg < 0f) angleDeg += 360f
        val saturation = (dist / radius).coerceIn(0f, 1f)
        val androidColor = android.graphics.Color.HSVToColor(floatArrayOf(angleDeg, saturation, 1f))
        onColorPicked(
            android.graphics.Color.red(androidColor),
            android.graphics.Color.green(androidColor),
            android.graphics.Color.blue(androidColor)
        )
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    pickAt(pos, size.width.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    pickAt(change.position, size.width.toFloat())
                }
            }
    ) {
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.sweepGradient(hueColors),
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius
        )
        drawCircle(
            color = Color.LightGray,
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun ColorSwatch(r: Int, g: Int, b: Int, onClick: (Int, Int, Int) -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color(r, g, b))
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable { onClick(r, g, b) }
    )
}

fun formatPatternName(pattern: PatternId): String = when (pattern) {
    PatternId.OFF -> "Off"
    PatternId.STATIC -> "Static"
    PatternId.RAINBOW_WAVE -> "Rainbow"
    PatternId.SPARKLE -> "Sparkle"
    PatternId.FESTIVE_CANDLE -> "Candle"
    PatternId.TWINKLE -> "Twinkle"
}

fun formatMelodyName(melody: MelodyId): String = when (melody) {
    MelodyId.SILENT -> "Silent"
    MelodyId.JINGLE_BELLS -> "Jingle Bells"
    MelodyId.WE_WISH_YOU -> "We Wish You"
    MelodyId.SILENT_NIGHT -> "Silent Night"
    MelodyId.DECK_THE_HALLS -> "Deck the Halls"
}
