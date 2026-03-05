package to.kuudere.anisuge

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Anisuge",
        state = WindowState(size = DpSize(1280.dp, 800.dp)),
        undecorated = false,
    ) {
        App()
    }
}
