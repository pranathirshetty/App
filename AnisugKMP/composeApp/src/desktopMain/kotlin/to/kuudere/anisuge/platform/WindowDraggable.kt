package to.kuudere.anisuge.platform

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.WindowScope

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
