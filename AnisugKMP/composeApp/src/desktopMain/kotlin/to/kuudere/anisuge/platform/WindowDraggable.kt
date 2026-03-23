package to.kuudere.anisuge.platform

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowPlacement

val LocalWindowScope = staticCompositionLocalOf<WindowScope> {
    error("LocalWindowScope not provided")
}

@Composable
actual fun DraggableWindowArea(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val window = LocalWindowScope.current.window
    var dragStartPoint by remember { mutableStateOf<java.awt.Point?>(null) }

    Box(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { _ ->
                    dragStartPoint = java.awt.MouseInfo.getPointerInfo().location
                },
                onDrag = { change, _ ->
                    val currentPoint = java.awt.MouseInfo.getPointerInfo().location
                    dragStartPoint?.let { start ->
                        val dx = currentPoint.x - start.x
                        val dy = currentPoint.y - start.y
                        if (dx != 0 || dy != 0) {
                            window.setLocation(window.x + dx, window.y + dy)
                            dragStartPoint = currentPoint
                        }
                    }
                    change.consume()
                }
            )
        }
    ) {
        content()
    }
}

@Composable
actual fun WindowManagementButtons(
    onClose: () -> Unit,
    modifier: Modifier
) {
    val windowState = LocalWindowState.current

    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimize
        IconButton(
            onClick = { windowState.isMinimized = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Remove, 
                contentDescription = "Minimize", 
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Maximize
        IconButton(
            onClick = {
                windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                    WindowPlacement.Floating
                } else {
                    WindowPlacement.Maximized
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.CropSquare, 
                contentDescription = "Maximize", 
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Close
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close, 
                contentDescription = "Close", 
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
