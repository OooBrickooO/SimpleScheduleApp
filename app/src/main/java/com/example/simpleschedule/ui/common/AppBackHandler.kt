package com.example.simpleschedule.ui.common

import android.os.Build
import android.window.BackEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun AppBackHandler(enabled: Boolean, onBack: () -> Unit): Modifier {
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        var scale by remember { mutableFloatStateOf(1f) }
        var translateX by remember { mutableFloatStateOf(0f) }
        var cornerRadius by remember { mutableStateOf(0.dp) }

        PredictiveBackHandler { progress ->
            try {
                progress.collect { event ->
                    scale = 1f - (event.progress * 0.08f)
                    translateX = if (event.swipeEdge == BackEvent.EDGE_LEFT) {
                        event.progress * 56f
                    } else {
                        -event.progress * 56f
                    }
                    cornerRadius = (event.progress * 24).dp
                }
                onBack()
                scale = 1f
                translateX = 0f
                cornerRadius = 0.dp
            } catch (_: Exception) {
                scale = 1f
                translateX = 0f
                cornerRadius = 0.dp
            }
        }

        return Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = translateX
            if (cornerRadius > 0.dp) {
                clip = true
                shape = RoundedCornerShape(cornerRadius)
            }
        }
    } else {
        BackHandler(enabled = true, onBack = onBack)
        return Modifier
    }
}
