package to.kuudere.anisuge.platform

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun Modifier.dragToPage(pagerState: PagerState): Modifier {
    val scope = rememberCoroutineScope()
    return this.draggable(
        state = rememberDraggableState { delta ->
        },
        orientation = Orientation.Horizontal,
        onDragStopped = { velocity ->
            scope.launch {
                if (velocity < -100f) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } else if (velocity > 100f) {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }
        }
    )
}
