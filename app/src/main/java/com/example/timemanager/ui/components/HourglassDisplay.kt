package com.example.timemanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp

@Composable
fun HourglassDisplay(
    isRunning: Boolean,
    elapsedSeconds: Int,
    onStartClick: () -> Unit,
    onFocusClick: () -> Unit,
    onEndClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Animation State for Flip
    val rotationAnim = remember { Animatable(0f) }
    
    // Handle Flip Animation when stopping
    LaunchedEffect(isRunning) {
        if (!isRunning) {
            // Check if we are actually coming from a running state (rotation is 0)
            if (rotationAnim.value == 0f) {
                // Animate Flip
                rotationAnim.animateTo(
                    targetValue = 180f,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
                // Reset to 0 instantly (visually identical)
                rotationAnim.snapTo(0f)
            }
        } else {
            // Ensure we start at 0
            rotationAnim.snapTo(0f)
        }
    }

    // Sand Calculation
    // Assume visual capacity is 60 minutes (3600 seconds)
    val capacitySeconds = 3600f
    val progress = (elapsedSeconds % 3600) / capacitySeconds
    val topSandRatio = if (isRunning) (1f - progress).coerceIn(0f, 1f) else 1f
    val bottomSandRatio = if (isRunning) progress.coerceIn(0f, 1f) else 0f

    // Text Measurer
    val textMeasurer = rememberTextMeasurer()
    val startTextLayout = textMeasurer.measure(
        text = AnnotatedString(if (isRunning) "专注" else "开始"),
        style = MaterialTheme.typography.displayMedium.copy(color = if (isRunning) onPrimaryContainer else onPrimary)
    )
    val endTextLayout = textMeasurer.measure(
        text = AnnotatedString("结束"),
        style = MaterialTheme.typography.displayMedium.copy(
            color = if (isRunning) onErrorContainer 
                   else onSurfaceVariant.copy(alpha = 0.5f)
        )
    )

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isRunning) {
                    detectTapGestures { offset ->
                        // Standard: Top half vs Bottom half
                        // Adjust Y offset by vertical bias if needed, but here we assume center
                        if (offset.y < size.height / 2) {
                            if (isRunning) onFocusClick() else onStartClick()
                        } else {
                            if (isRunning) onEndClick()
                        }
                    }
                }
                .graphicsLayer {
                    rotationZ = rotationAnim.value
                }
        ) {
            val width = size.width
            val height = size.height
            val halfHeight = height / 2
            val gap = 10.dp.toPx() // Gap between top and bottom glass
            val glassWidth = width * 0.8f
            val centerX = width / 2
            
            // Apply a vertical offset to shift everything up slightly
            // "漏洞往上面移动一些" -> Shift content UP
            val verticalOffset = -height * 0.05f 

            // --- Draw Top Glass ---
            val topGlassPath = Path().apply {
                moveTo(centerX - glassWidth / 2, verticalOffset)
                lineTo(centerX + glassWidth / 2, verticalOffset)
                lineTo(centerX + glassWidth * 0.35f / 2, halfHeight - gap / 2 + verticalOffset)
                lineTo(centerX - glassWidth * 0.35f / 2, halfHeight - gap / 2 + verticalOffset)
                close()
            }
            
            // Fill Top Background
            drawPath(
                path = topGlassPath,
                color = if (isRunning) primaryContainer else primaryColor
            )
            
            // Draw Top Sand
            if (topSandRatio > 0) {
                // We clip the sand to the glass path
                clipPath(topGlassPath) {
                    val sandHeight = (halfHeight - gap/2) * topSandRatio
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(0f, (halfHeight - gap/2) - sandHeight + verticalOffset),
                        size = Size(width, sandHeight)
                    )
                }
            }
            
            // Draw Top Text
            val topTextOffset = Offset(
                centerX - startTextLayout.size.width / 2,
                (halfHeight - gap/2) / 2 - startTextLayout.size.height / 2 + verticalOffset
            )
            drawText(startTextLayout, topLeft = topTextOffset)


            // --- Draw Bottom Glass ---
            val bottomGlassPath = Path().apply {
                moveTo(centerX - glassWidth * 0.35f / 2, halfHeight + gap / 2 + verticalOffset)
                lineTo(centerX + glassWidth * 0.35f / 2, halfHeight + gap / 2 + verticalOffset)
                lineTo(centerX + glassWidth / 2, height + verticalOffset)
                lineTo(centerX - glassWidth / 2, height + verticalOffset)
                close()
            }

            // Fill Bottom Background
            drawPath(
                path = bottomGlassPath,
                color = if (isRunning) errorContainer else surfaceVariant
            )

            // Draw Bottom Sand
            if (bottomSandRatio > 0) {
                clipPath(bottomGlassPath) {
                    val sandHeight = (height - (halfHeight + gap/2)) * bottomSandRatio
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(0f, height - sandHeight + verticalOffset),
                        size = Size(width, sandHeight)
                    )
                }
            }

            // Draw Bottom Text
            val bottomTextOffset = Offset(
                centerX - endTextLayout.size.width / 2,
                (halfHeight + gap/2) + (height - (halfHeight + gap/2)) / 2 - endTextLayout.size.height / 2 + verticalOffset
            )
            drawText(endTextLayout, topLeft = bottomTextOffset)
            
            // --- Draw Flowing Sand ---
            if (isRunning && topSandRatio > 0) {
                val flowWidth = 4.dp.toPx()
                drawLine(
                    color = primaryColor,
                    start = Offset(centerX, halfHeight - gap / 2 + verticalOffset),
                    end = Offset(centerX, halfHeight + gap / 2 + (height - (halfHeight + gap/2)) * bottomSandRatio + verticalOffset),
                    strokeWidth = flowWidth
                )
            }
        }
    }
}
