package to.kuudere.anisuge.platform

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
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
    Box(
        modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                window.setLocation(
                    window.x + dragAmount.x.toInt(),
                    window.y + dragAmount.y.toInt()
                )
            }
        }
    ) {
        content()
    }
}
