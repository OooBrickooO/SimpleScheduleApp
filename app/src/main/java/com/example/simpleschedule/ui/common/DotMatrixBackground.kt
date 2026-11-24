package com.example.simpleschedule.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DotMatrixBackground(isDark: Boolean) {
    val dotColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 16.dp.toPx()
        val dotRadius = 1.dp.toPx()
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    }
}
