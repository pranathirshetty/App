package to.kuudere.anisuge.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun DraggableWindowArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
