package to.kuudere.anisuge.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// On Android there's no window dragging concept — just render content as-is
@Composable
actual fun DraggableWindowArea(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier) { content() }
}

@Composable
actual fun WindowManagementButtons(
    onClose: () -> Unit,
    modifier: Modifier
) {
    // No-op for Android
}
