package com.example.timemanager.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun StartButton(
    onLongPressComplete: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "开始",
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    size: androidx.compose.ui.unit.Dp = 200.dp
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val trackColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer Ring Canvas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 8.dp.toPx()
            // In Canvas scope, `size` refers to the canvas size, not the Dp parameter.
            // Canvas size is provided as `Size` object, so we use `size.minDimension`.
            // But there's a name collision with the parameter `size`.
            // Let's use `this.size` to refer to DrawScope.size
            val currentSize = this.size
            val diameter = currentSize.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Only show ring when progress > 0 (i.e., user is pressing)
            if (progress.value > 0f) {
                // Track
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                // Progress
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Inner Circle / Text
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp) // Inset from ring
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // Start animating progress
                            val pressJob = scope.launch {
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                                )
                                // If completed
                                onLongPressComplete()
                                // Reset immediately after action is triggered, 
                                // so it's fresh if we return or if the action doesn't navigate away.
                                progress.snapTo(0f) 
                            }
                            try {
                                awaitRelease()
                            } finally {
                                // Reset if released early
                                pressJob.cancel()
                                // Always snap to 0 on release to ensure clean state
                                progress.snapTo(0f)
                            }
                        }
                    )
                },
            shape = CircleShape,
            color = color,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
